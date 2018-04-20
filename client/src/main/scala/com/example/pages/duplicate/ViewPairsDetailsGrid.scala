package com.example.pages.duplicate


import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import com.example.routes.AppRouter.AppPage
import com.example.data.DuplicateSummary
import com.example.data.Id
import utils.logging.Logger
import com.example.controller.Controller
import com.example.data.SystemTime
import com.example.react.AppButton
import com.example.react.Utils._
import com.example.data.duplicate.suggestion.PairsData
import com.example.data.duplicate.suggestion.PairData
import com.example.color.Color
import com.example.react.CheckBox
import com.example.data.duplicate.suggestion.ColorBy
import com.example.data.duplicate.suggestion.ColorByWonPct
import com.example.data.duplicate.suggestion.ColorBy
import com.example.data.duplicate.suggestion.PairsDataSummary
import com.example.data.duplicate.suggestion.Stat
import com.example.data.duplicate.suggestion.ColorByWon
import com.example.data.duplicate.suggestion.ColorByWonPts
import com.example.data.duplicate.suggestion.ColorByWonPtsPct
import com.example.data.duplicate.suggestion.ColorByPointsPct
import com.example.react.PieChart
import com.example.react.PieChartOrSquareForZero
import com.example.pages.BaseStyles
import com.example.react.PieChartTable.Cell
import com.example.react.PieChartTable.Data
import com.example.react.PieChartTable.Column
import com.example.react.PieChartTable
import com.example.react.PieChartTable.Row
import com.example.react.PieChartTable.DataPieChart
import com.example.react.PieChartTable.DataTagMod

/**
 * Shows a pairs summary page.
 * Each match has a button that that shows that match, by going to the ScoreboardView(id) page.
 * There is also a button to create a new match, by going to the NewScoreboardView page.
 *
 * The data is obtained from the DuplicateStore object.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ViewPairsDetailsGrid( routerCtl: RouterCtl[DuplicatePage] )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewPairsDetailsGrid {
  import ViewPairsDetailsGridInternal._

  case class Props( filter: ViewPlayerFilter.Filter, showNoDataMsg: Boolean = false) {

    def getNames = filter.getNames
  }

  def apply( filter: ViewPlayerFilter.Filter, showNoDataMsg: Boolean = false ) =
    component(Props( filter, showNoDataMsg))

}

object ViewPairsDetailsGridInternal {
  import ViewPairsDetailsGrid._
  import DuplicateStyles._

  val logger = Logger("bridge.ViewPairsDetailsGrid")

  sealed trait DisplayType
  object DisplayDecDef extends DisplayType
  object DisplayMadeDown extends DisplayType
  object DisplayAll extends DisplayType

  def size( v: Int, vmin: Double, vmax: Double, sizemin: Int, sizemax: Int ): Int = {
    if (vmax == vmin) sizemax
    else ((v.toDouble-vmin)*(sizemax-sizemin)/(vmax-vmin) + sizemin).toInt
  }

  val DarkRed = Color.rgb( 164, 0, 0)
  val DarkGreen = Color.rgb( 0, 164, 0)
  val Red = Color.rgb( 255, 0, 0 )
  val Green = Color.rgb( 0, 255, 0 )
  val Blue = Color.rgb( 0, 0, 255 )
  val Black = Color.rgb( 0, 0, 0 )

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( displayType: DisplayType = DisplayDecDef,
                    tooltipChartSize: Int = 150,
                    maxSize: Int = 100,
                    minSize: Int = 5
                  )

  object ColorByPlayedResults extends ColorBy {
    val name = "Played"
    def value( pd: PairData ): Double = {
      val x = pd.details.map { d => d.declarer+d.defended+d.passed }.getOrElse(0)
      x
    }
    def n( pd: PairData ): Int = pd.details.map { d => d.declarer+d.defended+d.passed }.getOrElse(0)
  }

  object ColorByDeclarerResults extends ColorBy {
    val name = "Declared"
    def value( pd: PairData ): Double = {
      val x = pd.details.map { d => d.declarer }.getOrElse(0)
      x
    }
    def n( pd: PairData ): Int = pd.details.map { d => d.declarer+d.defended+d.passed }.getOrElse(0)
  }

  object ColorByDefendedResults extends ColorBy {
    val name = "Defended"
    def value( pd: PairData ): Double = {
      val x = pd.details.map { d => d.defended }.getOrElse(0)
      x
    }
    def n( pd: PairData ): Int = pd.details.map { d => d.declarer+d.defended+d.passed }.getOrElse(0)
  }

  object ColorByPassedResults extends ColorBy {
    val name = "Passed"
    def value( pd: PairData ): Double = {
      val x = pd.details.map { d => d.passed }.getOrElse(0)
      x
    }
    def n( pd: PairData ): Int = pd.details.map { d => d.declarer+d.defended+d.passed }.getOrElse(0)
  }

  val cellX = Cell( List( DataTagMod( "X" )) )
  val cellEmpty = Cell( List() )

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def setDisplayType( displayType: DisplayType ) = scope.modState { s => s.copy(displayType=displayType) }

    def render( props: Props, state: State ) = {

      props.filter.pairsData match {
        case Some(pds) if !pds.players.isEmpty =>
          val summary = new PairsDataSummary( pds, ColorByDeclarerResults, props.filter.selected, ColorByDefendedResults, ColorByPlayedResults, ColorByPassedResults )

          val statDec = summary.colorStat
          val statDef = summary.extraStats.head
          val statPlayed = summary.extraStats.tail.head
          val statPassed = summary.extraStats.tail.tail.head

          val statTotalDec = summary.colorStatPlayerTotals
          val statTotalDef = summary.extraStatsPlayerTotals.head
          val statTotalPlayed = summary.extraStatsPlayerTotals.tail.head
          val statTotalPassed = summary.extraStatsPlayerTotals.tail.tail.head

          val vMin = Math.min( statDec.min, Math.min( statDef.min, statPassed.min ) )
          val vMax = Math.max( statDec.max, Math.max( statDef.max, statPassed.max ) )
          val vTotalMin = Math.min( statTotalDec.min, Math.min( statTotalDef.min, statTotalPassed.min ) )
          val vTotalMax = Math.max( statTotalDec.max, Math.max( statTotalDef.max, statTotalPassed.max ) )

          val allPlayers = summary.playerTotals.filter { e =>
            e._2.details.map( d => d.total>0 ).getOrElse(false)
          }.map( e => e._1 ).toList.sorted
          val sortedPlayers = props.filter.selected.map( l => l ).getOrElse(allPlayers)

          def cellDecDef( pd: PairData, isTotals: Boolean ) = {
            val (vmin,vmax) = if (isTotals) (statTotalPlayed.min,statTotalPlayed.max) else (statPlayed.min,statPlayed.max)
            pd.details match {
              case Some(det) =>
                val oneOverTot = 100.0/(det.declarer+det.defended+det.passed)
                val title = TagMod( s"${pd.player1} - ${pd.player2}" )
                val legend = f"Declarer ${det.declarer} (${det.declarer*oneOverTot}%.2f%%)%nDefended ${det.defended} (${det.defended*oneOverTot}%.2f%%)%nPassed ${det.passed} (${det.passed*oneOverTot}%.2f%%)"
                DataPieChart(
                  size = size(det.total, vmin, vmax, state.minSize, state.maxSize),
                  color = Green::Red::Blue::Nil,
                  value = det.declarer.toDouble::det.defended.toDouble::det.passed.toDouble::Nil,
                ).toCellWithOneChartAndTitle(legend, state.tooltipChartSize, state.maxSize, Some(title))
              case None =>
                cellEmpty
            }
          }

          def cellMadeDown( pd: PairData, isTotals: Boolean ) = {
            val (vmin,vmax) = if (isTotals) (vTotalMin,vTotalMax) else (vMin,vMax)
            pd.details match {
              case Some(det) =>
                val sizeDec = size(det.declarer, vmin, vmax, state.minSize, state.maxSize)
                val sizeDef = size(det.defended, vmin, vmax, state.minSize, state.maxSize)
                val sizePas = size(det.passed, vmin, vmax, state.minSize, state.maxSize)
                val oneOverDec = 100.0/det.declarer
                val oneOverDef = 100.0/det.defended
                val oneOverPas = 100.0/det.passed
                val oneOverTotal = 100.0/(det.declarer+det.defended+det.passed)
                val titleDec = f"Declarer ${det.declarer} (${det.declarer*oneOverTotal}%.2f%%)\n  Made ${det.made} (${det.made*oneOverDec}%.2f%%)%n  Down ${det.down} (${det.down*oneOverDec}%.2f%%)"
                val titleDef = f"Defended ${det.defended} (${det.defended*oneOverTotal}%.2f%%)\n  Took Down ${det.tookDown} (${det.tookDown*oneOverDef}%.2f%%)%n  Allowed Made ${det.allowedMade} (${det.allowedMade*oneOverDef}%.2f%%)"
                val titlePas = f"Passed ${det.passed} (${det.passed*oneOverTotal}%.2f%%)"
                val title = TagMod( s"${pd.player1} - ${pd.player2}" )
                val data = List(
                              DataPieChart(
                                size = if (det.declarer == 0) -sizeDec else sizeDec,
                                color = Green::Red::Nil,
                                value = det.made.toDouble::det.down.toDouble::Nil,
                              ),
                              DataPieChart(
                                size = if (det.defended == 0) -sizeDef else sizeDef,
                                color = Green::Red::Nil,
                                value = det.tookDown.toDouble::det.allowedMade.toDouble::Nil,
                              ),
                              DataPieChart(
                                size = if (det.passed == 0) -sizePas else sizePas,
                                color = Blue::Nil,
                                value = det.passed.toDouble::Nil,
                              )
                            )
                Cell(
                  data = data,
                  title = Some(
                      TagMod(
                        <.div( baseStyles.tooltipTitle, title ),
                        <.div(
                          baseStyles.tooltipBody,
                          data.map( d => PieChartTable.item(if (d.size <= 0) d else d.withSize(state.maxSize)) ).toTagMod,
                          <.div( baseStyles.tooltipBody, titleDec+"\n"+titleDef+"\n"+titlePas )
                        )
                      )
                  )
                )
              case None =>
                cellEmpty
            }
          }

          def cellAll( pd: PairData, isTotals: Boolean ) = {
            val (vmin,vmax) = if (isTotals) (statTotalPlayed.min,statTotalPlayed.max) else (statPlayed.min,statPlayed.max)
            pd.details match {
              case Some(det) =>
                val sizeAll = size(det.total, vmin, vmax, state.minSize, state.maxSize)
                val oneOverDec = 100.0/det.declarer
                val oneOverDef = 100.0/det.defended
                val oneOverPas = 100.0/det.passed
                val oneOverTotal = 100.0/(det.declarer+det.defended+det.passed)
                val titleDec = f"Declarer ${det.declarer} (${det.declarer*oneOverTotal}%.2f%%)\n  Made ${det.made} (${det.made*oneOverDec}%.2f%%)%n  Down ${det.down} (${det.down*oneOverDec}%.2f%%)"
                val titleDef = f"Defended ${det.defended} (${det.defended*oneOverTotal}%.2f%%)\n  Took Down ${det.tookDown} (${det.tookDown*oneOverDef}%.2f%%)%n  Allowed Made ${det.allowedMade} (${det.allowedMade*oneOverDef}%.2f%%)"
                val titlePas = f"Passed ${det.passed} (${det.passed*oneOverTotal}%.2f%%)"
                val title = TagMod( s"${pd.player1} - ${pd.player2}" )
                DataPieChart(
                  size = if (det.total == 0) -sizeAll else sizeAll,
                  color = Green::Red::DarkRed::DarkGreen::Blue::Nil,
                  value = det.made.toDouble::det.down.toDouble::det.allowedMade.toDouble::det.tookDown.toDouble::det.passed.toDouble::Nil,
                ).toCellWithOneChartAndTitle(titleDec+"\n"+titleDef+"\n"+titlePas, state.tooltipChartSize, state.maxSize, Some(title))
              case None =>
                cellEmpty
            }
          }

          val cellFun = state.displayType match {
            case DisplayDecDef =>
              cellDecDef _
            case DisplayMadeDown =>
              cellMadeDown _
            case DisplayAll =>
              cellAll _
          }

          def getButton( displayType: DisplayType, id: String, text: String ) = {
            AppButton(
              id,
              text,
              ^.onClick-->setDisplayType(displayType),
              BaseStyles.highlight(selected = displayType==state.displayType)
            )
          }

          val shownSorted = sortedPlayers.filter( e => props.filter.isPlayerShown(e) )

          val columns = shownSorted.map( p => Column(p) ):::List(Column("Totals"))

          val rows = shownSorted.map { rowPlayer =>

            val cells = shownSorted.map { colPlayer =>
              if (colPlayer == rowPlayer) cellX
              else {
                summary.get(rowPlayer, colPlayer) match {
                  case Some(pd) =>
                    cellFun( pd, false )
                  case None =>
                    cellEmpty
                }
              }
            }

            val pdTotal = summary.playerTotals.get(rowPlayer).getOrElse(PairData(rowPlayer,"",0,0,0,0,0,0,None,0,0,0,0,0))
            val totalCell = List( cellFun(pdTotal,true) )

            Row( rowPlayer, cells:::totalCell )
          }

          <.div(
            dupStyles.divPairsDetailsGrid,
            PieChartTable(
              firstColumn = Column("Player"),
              columns = columns,
              rows = rows,
              totalRows = None,
              header = None,
              caption = Some(TagMod(
                "Hand Results ",
                getButton( DisplayDecDef, "DisplayDecDef", "Declared/Defended" ),
                getButton( DisplayMadeDown, "DisplayMadeDown", "Made Down" ),
                getButton( DisplayAll, "DisplayAll", "All" )
              )),
              footer = Some(TagMod(
                <.tr(
                  <.td(
                    ^.colSpan:=sortedPlayers.length+2,
                    state.displayType match {
                      case DisplayDecDef =>
                        TagMod(
                          "The size of the circle is proportional to the number of hands played by the pair/player. ",
                          <.br,
                          "Green, ",
                          PieChart( 15, 1.0::Nil, Green::Nil, attrs=Some(^.display := "inline-block") ),
                          ", indicates the number of times the team was declarer ",
                          <.br,
                          "Red, ",
                          PieChart( 15, 1.0::Nil, Red::Nil, attrs=Some(^.display := "inline-block") ),
                          ", indicates the number of times the team defended",
                          <.br,
                          "Blue, ",
                          PieChart( 15, 1.0::Nil, Blue::Nil, attrs=Some(^.display := "inline-block") ),
                          ", indicates passed out hands"
                        )
                      case DisplayMadeDown =>
                        TagMod(
                          "The size of the circle is proportional to the number of hands played by the pair/player. ",
                          "A black square, ",
                          PieChartOrSquareForZero( -5, Black, 1.0::Nil, Green::Nil, attrs=Some(^.display := "inline-block") ),
                          ", indicates 0",
                          <.br,
                          "The first circle is the results of hands where the team was declarer.  green, ",
                          PieChart( 15, 1.0::Nil, Green::Nil, attrs=Some(^.display := "inline-block") ),
                          ", is made, red, ",
                          PieChart( 15, 1.0::Nil, Red::Nil, attrs=Some(^.display := "inline-block") ),
                          ", is down.",
                          <.br,
                          "The second circle is the results of hands where the team was defending.  green is took down, red is allowed made.",
                          <.br,
                          "The third circle, blue, ",
                          PieChart( 15, 1.0::Nil, Blue::Nil, attrs=Some(^.display := "inline-block") ),
                          ", is the results of hands were passed out.",
                        )
                      case DisplayAll =>
                        TagMod(
                          "The size of the circle is proportional to the number of hands played by the pair/player. ",
                          <.br,
                          "Green, ",
                          PieChart( 15, 1.0::Nil, Green::Nil, attrs=Some(^.display := "inline-block") ),
                          ", is contract made as declarer, red, ",
                          PieChart( 15, 1.0::Nil, Red::Nil, attrs=Some(^.display := "inline-block") ),
                          ", is down as declarer.",
                          <.br,
                          "Dark green, ",
                          PieChart( 15, 1.0::Nil, DarkGreen::Nil, attrs=Some(^.display := "inline-block") ),
                          ", is took down as defender, dark red, ",
                          PieChart( 15, 1.0::Nil, DarkRed::Nil, attrs=Some(^.display := "inline-block") ),
                          ", is allowed made as defender.",
                          <.br,
                          "Blue, ",
                          PieChart( 15, 1.0::Nil, Blue::Nil, attrs=Some(^.display := "inline-block") ),
                          ", is passed out hands."
                        )
                    }
                  )
                )
              ))
            )
          )

        case Some(pds) =>
          <.div(
            dupStyles.divPairsDetailsGrid,
            props.showNoDataMsg ?= "No past duplicate matches were found"
          )
        case None =>
          <.div(
            dupStyles.divPairsDetailsGrid,
            props.showNoDataMsg ?= "Waiting for data"
          )
      }
    }
  }

  val component = ScalaComponent.builder[Props]("ViewPairsDetailsGrid")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

