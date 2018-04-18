package com.example.pages.duplicate


import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
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
import com.example.data.duplicate.suggestion.ColorByPlayed
import com.example.pages.BaseStyles
import com.example.react.ColorBar
import com.example.data.duplicate.suggestion.CalculationType
import com.example.data.duplicate.suggestion.CalculationAsPlayed
import com.example.data.duplicate.suggestion.ColorByIMP
import com.example.data.duplicate.suggestion.CalculationMP
import com.example.data.duplicate.suggestion.CalculationIMP
import com.example.react.StatsTable
import com.example.react.PieChartTable
import com.example.react.PieChartTable.Column
import com.example.react.PieChartTable.Row
import com.example.react.PieChartTable.Data
import com.example.react.PieChartTable.Cell
import com.example.react.PieChartTable.DataBar
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
 * ViewPairsGrid( routerCtl: RouterCtl[DuplicatePage] )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewPairsGrid {
  import ViewPairsGridInternal._

  case class Props( filter: ViewPlayerFilter.Filter, showNoDataMsg: Boolean = false) {

    def getNames = filter.getNames
  }

  def apply( filter: ViewPlayerFilter.Filter, showNoDataMsg: Boolean = false ) =
    component(Props( filter, showNoDataMsg))

}

object ViewPairsGridInternal {
  import ViewPairsGrid._
  import DuplicateStyles._

  val logger = Logger("bridge.ViewPairsGrid")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State(
                    calc: CalculationType,
                    initialCalc: CalculationType,
                    colorBy: ColorBy = ColorByWonPct,
                    maxSize: Int = 60,
                    minSize: Int = 5
                  )

  /**
   * @param stat
   * @param n the number of steps
   * @return Tuple3( titlesBelow, titleMiddle, titlesAbove )
   */
  def stepTitles( stat: Stat, n: Int ) = {
    val min = stat.min
    val ave = stat.ave
    val max = stat.max

    val stepB = (ave-min)/n
    val below = (0 until n).map { i =>
      f"${min+stepB*i}%.2f%%"
    }.toList

    val middle = f"${ave}%.2f%%"

    val stepA = (max-ave)/n
    val above = (1 to n).map { i =>
      f"${ave+stepA*i}%.2f%%"
    }.toList
    (below,middle,above)
  }

//  case class Data[+TColor](
//    size: Int,
//    color: List[TColor],
//    value: List[Double],
//    title: Option[String] = None,
//    size2: Int = 0,
//    showX: Boolean = false
//  )


  /**
   * @param pd
   * @param sizeSt the stat for determining size of bar
   * @param colorSt the stat for determining the color of the bar
   * @param colorAbove the hue of the color to use if pd is above average
   * @param colorBelow the hue of the color to use if pd is below average
   * @param state
   * @param sizeMultiplier multiply the size to make the bar bigger. (1 - player,player, 2 - total column)
   * @return a tuple2, the data and the title.
   */
  def getData( pd: PairData, sizeSt: Stat, colorSt: Stat, colorAbove: Double, colorBelow: Double, state: State, sizeMultiplier: Int ): (Data,String) = {
    val (bcolor, light) = colorSt.sizeAveAsFraction(pd)
    val lightness = light*75.0+25.0
    val size = sizeSt.size(pd, state.minSize*sizeMultiplier, state.maxSize*sizeMultiplier)
    val (color,scolor) = if (bcolor) {
      // above average, green
      (Color.hsl( colorAbove, 100, lightness ), f"hsl(${colorAbove},100,${lightness}%.2f)" )
    } else {
      // below average, red
      (Color.hsl( colorBelow, 100, lightness ), f"hsl(${colorBelow},100,${lightness}%.2f)" )
    }
    val title = f"""Played ${pd.played},
                   |Won ${pd.won+pd.wonImp} (${pd.winPercent}%.2f%%),
                   |WonPoints ${pd.wonPts+pd.wonImpPts}%.2f (${pd.winPtsPercent}%.2f%%)""".stripMargin
    val titleMP = if (state.calc != CalculationIMP) {
                    f""",
                       |Points ${pd.points}%.0f (${pd.pointsPercent}%.2f%%)""".stripMargin
    } else {
      ""
    }
    val titleIMP = if (state.calc != CalculationMP) {
                    f""",
                       |IMP ${pd.avgIMP}%.2f""".stripMargin
    } else {
      ""
    }

    (DataBar( size, List( color ), List(1.0), None, None, 20 ), title+titleMP+titleIMP)
  }

  val cellX = Cell( List( DataTagMod( "X" )) )

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def setColorBy( colorBy: ColorBy ) = scope.modState { s => s.copy(colorBy=colorBy) }

    def setCalc( calc: CalculationType ) = scope.modState { s =>
      val colorBy = calc match {
        case CalculationAsPlayed =>
          s.colorBy
        case CalculationMP =>
          if (s.colorBy == ColorByIMP) ColorByPointsPct
          else s.colorBy
        case CalculationIMP =>
          if (s.colorBy == ColorByPointsPct) ColorByIMP
          else s.colorBy
      }
      s.copy(colorBy=colorBy, calc=calc)
    }

    def render( props: Props, state: State ) = {

      props.filter.pairsData match {
        case Some(rawpds) if !rawpds.players.isEmpty =>
          val pds = {
            if (rawpds.calc == state.calc) rawpds
            else PairsData( rawpds.pastgames, state.calc )
          }
          val summary = new PairsDataSummary( pds, state.colorBy, props.filter.selected, ColorByPlayed )
          val allPlayers = summary.players.sorted
          val sortedPlayers = summary.playerFilter.sorted

          def getButton( colorBy: ColorBy, id: String, text: String ) = {
            AppButton(
              id,
              text,
              ^.onClick-->setColorBy(colorBy),
              BaseStyles.highlight(selected = colorBy==state.colorBy)
            )
          }

          val statColor = summary.colorStat
          val statSize = summary.extraStats.head

          val statTotalColor = summary.colorStatPlayerTotals
          val statTotalSize = summary.extraStatsPlayerTotals.head

          val n = 10
          val (titlesBelow, titleWhite, titlesAbove) = stepTitles( statColor, n )
          val (titlesTBelow, titleTWhite, titlesTAbove) = stepTitles( statTotalColor, n )

          val shownSortedPlayers = sortedPlayers.filter( p => props.filter.isPlayerShown(p) )

          val columns = sortedPlayers.map( p => Column(p) ):::List( Column( "Totals" ) )

          val rows = shownSortedPlayers.map { rowplayer =>
            val data: List[Cell] = shownSortedPlayers.map { colPlayer =>
              val d = if (rowplayer == colPlayer) {
                cellX
              } else {
                pds.get(rowplayer, colPlayer) match {
                  case Some(pd) =>
                    val (data,title) = getData(pd, statSize, statColor, 120, 0, state, 1)
                    Cell(List( data ), Some(<.div(title)))
                  case None =>
                    Cell(List())
                }
              }
              d
            }
            val playerTotals = summary.playerTotals.get(rowplayer).getOrElse(PairData(rowplayer,"",0,0,0,0,0,0,None,0,0,0,0,0))

            val (totalData, totalTitle) = getData(playerTotals, statTotalSize, statTotalColor, 240, 60, state, 2)

            val totalDataList = List(Cell(List(totalData), Some(<.div(totalTitle))))

            Row( rowplayer, data:::totalDataList)
          }

          <.div(
            dupStyles.divPairsGrid,
            PieChartTable(
              firstColumn = Column("Player"),
              columns = columns,
              rows = rows,
              header = None,
              footer = Some(
                TagMod(
                  <.tr(
                    <.td(
                      ^.colSpan:=sortedPlayers.length+1,
                      f"Average for ${state.colorBy.name} is ${summary.colorStat.ave}%.2f"
                    ),
                    <.td( f"${summary.colorStatPlayerTotals.ave}%.2f" )
                  ),
                  <.tr(
                    <.td(
                      ^.colSpan:=sortedPlayers.length+2,
                      "The width of the box is relative to the number of times the pair has played together. ",
                      <.br,
                      "The color indicates how well, dark green is well above average, light green is above average, ",
                      <.br,
                      "white is average, ",
                      "light red is below average, dark red is well below average",
                      ColorBar( 0, 25.0, 120, 25.0, n, titlesBelow, titlesAbove, titleWhite ),
                      "For Totals, the width is relative to the number of times the player has played",
                      <.br,
                      "blue is above average and yellow is below average.",
                      ColorBar( 60, 25.0, 240, 25.0, n, titlesTBelow, titlesTAbove, titleTWhite )
                    )
                  )
                )
              ),
              totalRows = None,
              caption = Some(
                TagMod(
                  "Results ",
                  AppButton(
                      "CalcPlayed",
                      "as played",
                      BaseStyles.highlight(selected = state.calc == CalculationAsPlayed),
                      ^.onClick --> setCalc( CalculationAsPlayed )
                  ),
                  AppButton(
                      "CalcMP",
                      "by MP",
                      BaseStyles.highlight(selected = state.calc == CalculationMP),
                      ^.onClick --> setCalc( CalculationMP )
                  ),
                  AppButton(
                      "CalcIMP",
                      "by IMP",
                      BaseStyles.highlight(selected = state.calc == CalculationIMP),
                      ^.onClick --> setCalc( CalculationIMP )
                  ),
                  " color by ",
                  getButton( ColorByWonPct, "ColorByWonPct", "Won %" ),
                  getButton( ColorByWonPtsPct, "ColorByWonPtsPct", "WonPoints %" ),
                  if (state.calc != CalculationIMP) getButton( ColorByPointsPct, "ColorByPointsPct", "Points %" )
                  else TagMod(),
                  if (state.calc != CalculationMP) getButton( ColorByIMP, "ColorByIMP", "IMP" )
                  else TagMod()
                )
              )
            )
          )
        case Some(pds) =>
          <.div(
            dupStyles.divPairsGrid,
            props.showNoDataMsg ?= "No past duplicate matches were found"
          )
        case None =>
          <.div(
            dupStyles.divPairsGrid,
            props.showNoDataMsg ?= "Waiting for data"
          )
      }
    }
  }

  val component = ScalaComponent.builder[Props]("ViewPairsGrid")
                            .initialStateFromProps { props =>
                              props.filter.pairsData match {
                                case Some(pd) =>
                                  val anyMP = pd.pastgames.find( ds => ds.isMP ).isDefined
                                  val anyIMP = pd.pastgames.find( ds => ds.isIMP ).isDefined
                                  val calc: CalculationType =
                                    if (anyMP == anyIMP) CalculationAsPlayed
                                    else if (anyMP) CalculationMP
                                    else CalculationIMP
                                  State( calc = calc, initialCalc = calc )
                                case None =>
                                  val calc: CalculationType = CalculationMP
                                  State( calc = calc, initialCalc = calc )
                              }
                            }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

