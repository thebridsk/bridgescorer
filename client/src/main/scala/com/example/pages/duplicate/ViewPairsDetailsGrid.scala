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
import org.scalajs.dom.ext.Color
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
import org.scalajs.dom.svg
import com.example.react.PieChart
import com.example.react.PieChartOrSquareForZero
import com.example.pages.BaseStyles

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

  val SummaryHeader = ScalaComponent.builder[(Props,State,Backend,List[String])]("ViewPairsDetailsGrid.Header")
                        .render_P( args => {
                          val (props,state,backend,players) = args
                          <.thead(
                            <.tr(
                              <.th("Players"),
                              players.filter( e => props.filter.isPlayerShown(e) ).map( p => <.th( p ) ).toTagMod,
                              <.th("Totals")
                            )
                          )
                        }).build

  val titleAttr    = VdomAttr("data-title")

  def size( v: Int, vmin: Int, vmax: Int, sizemin: Int, sizemax: Int ): Int = {
    if (vmax == vmin) sizemax
    else ((v.toDouble-vmin)*(sizemax-sizemin)/(vmax-vmin) + sizemin).toInt
  }

  val DarkRed = Color( 164, 0, 0)
  val DarkGreen = Color( 0, 164, 0)

  val SummaryRow = ScalaComponent.builder[(Props,State,String,List[String],Stat,Stat,Stat,PairData,Stat,Stat,Stat)]("ViewPairsDetailsGrid.Row")
                      .render_P( args => {
                        val (props,state,rowplayer, players, colorStat, sizeStat, passedStat,
                             playerTotal, colorStatPlayerTotals, playedStatPlayerTotals, passedStatPlayer) = args
                        val pds = props.filter.pairsData.get
                        val displayType = state.displayType

                        def rectangleTotal( pd: PairData, sizeSt: Stat ): TagMod = {
                          pd.details match {
                            case Some(det) if det.declarer+det.defended+det.passed != 0 =>
                              val size = sizeSt.size(pd, state.minSize, state.maxSize)
                              val oneOverTot = 100.0/(det.declarer+det.defended+det.passed)
                              val title = f"Declarer ${det.declarer} (${det.declarer*oneOverTot}%.2f%%)%nDefended ${det.defended} (${det.defended*oneOverTot}%.2f%%)%nPassed ${det.passed} (${det.passed*oneOverTot}%.2f%%)"
                              <.td( <.div(
                                titleAttr := title,
                                baseStyles.hover,
                                PieChart( size,
                                          det.declarer.toDouble::det.defended.toDouble::det.passed.toDouble::Nil,
                                          Color.Green::Color.Red::Color.Blue::Nil,
                                          None )    // Some(title)
                              ))
                            case _ =>
                              <.td()
                          }
                        }

                        def rectangle( pd: PairData, sizeSt: Stat ): TagMod = {
                          pd.details match {
                            case Some(det) if det.declarer+det.defended+det.passed != 0 =>
                              val size = sizeSt.size(pd, state.minSize, state.maxSize)
                              val oneOverTot = 100.0/(det.declarer+det.defended+det.passed)
                              val title = f"Declarer ${det.declarer} (${det.declarer*oneOverTot}%.2f%%)%nDefended ${det.defended} (${det.defended*oneOverTot}%.2f%%)%nPassed ${det.passed} (${det.passed*oneOverTot}%.2f%%)"
                              <.td( <.div(
                                titleAttr := title,
                                baseStyles.hover,
                                PieChart( size,
                                          det.declarer.toDouble::det.defended.toDouble::det.passed.toDouble::Nil,
                                          Color.Green::Color.Red::Color.Blue::Nil,
                                          None )    // Some(title)
                              ))
                            case _ =>
                              <.td()
                          }
                        }

                        def square(colPlayer: String, sizeSt: Stat ) = {
                          if (rowplayer == colPlayer) {
                            <.td("x")
                          } else {
                            pds.get(rowplayer, colPlayer) match {
                              case Some(pd) =>
                                rectangle(pd,sizeSt)
                              case None =>
                                <.td()
                            }
                          }
                        }

                        /*
                         * @param pd
                         * @param vmin min of times played as defended, declared, or passed
                         * @param vmax max of times played as defended, declared, or passed
                         */
                        def rectangleTotalHand( pd: PairData, vmin: Int, vmax: Int ): TagMod = {
                          pd.details match {
                            case Some(det) if det.declarer+det.defended+det.passed != 0 =>
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
                              <.td( <.div(
                                titleAttr := titleDec+"\n"+titleDef+"\n"+titlePas,
                                baseStyles.hover,
                                PieChartOrSquareForZero(
                                    if (det.declarer == 0) -sizeDec else sizeDec,
                                    Color.Black,
                                    det.made.toDouble::det.down.toDouble::Nil,
                                    Color.Green::Color.Red::Nil,
                                    None ), // Some(titleDec) ),
                                PieChartOrSquareForZero(
                                    if (det.defended == 0) -sizeDef else sizeDef,
                                    Color.Black,
                                    det.tookDown.toDouble::det.allowedMade.toDouble::Nil,
                                    Color.Green::Color.Red::Nil,
                                    None ), // Some(titleDef) ),
                                PieChartOrSquareForZero(
                                    if (det.passed == 0) -sizePas else sizePas,
                                    Color.Black,
                                    det.passed.toDouble::Nil,
                                    Color.Blue::Nil,
                                    None ) // Some(titlePas) )
                              ))
                            case _ =>
                              <.td()
                          }
                        }

                        /*
                         * @param pd
                         * @param vmin min of total times played
                         * @param vmax max of total times played
                         */
                        def rectangleTotalBoth( pd: PairData, vmin: Int, vmax: Int ): TagMod = {
                          pd.details match {
                            case Some(det) if det.declarer+det.defended+det.passed != 0 =>
                              val sizeAll = size(det.total, vmin, vmax, state.minSize, state.maxSize)
                              val oneOverDec = 100.0/det.declarer
                              val oneOverDef = 100.0/det.defended
                              val oneOverPas = 100.0/det.passed
                              val oneOverTotal = 100.0/(det.declarer+det.defended+det.passed)
                              val titleDec = f"Declarer ${det.declarer} (${det.declarer*oneOverTotal}%.2f%%)\n  Made ${det.made} (${det.made*oneOverDec}%.2f%%)%n  Down ${det.down} (${det.down*oneOverDec}%.2f%%)"
                              val titleDef = f"Defended ${det.defended} (${det.defended*oneOverTotal}%.2f%%)\n  Took Down ${det.tookDown} (${det.tookDown*oneOverDef}%.2f%%)%n  Allowed Made ${det.allowedMade} (${det.allowedMade*oneOverDef}%.2f%%)"
                              val titlePas = f"Passed ${det.passed} (${det.passed*oneOverTotal}%.2f%%)"
                              <.td( <.div(
                                titleAttr := titleDec+"\n"+titleDef+"\n"+titlePas,
                                baseStyles.hover,
                                PieChartOrSquareForZero(
                                    if (det.total == 0) -sizeAll else sizeAll,
                                    Color.Black,
                                    det.made.toDouble::det.down.toDouble::det.allowedMade.toDouble::det.tookDown.toDouble::det.passed.toDouble::Nil,
                                    Color.Green::Color.Red::DarkRed::DarkGreen::Color.Blue::Nil,
                                    None ), // Some(titleDec) ),
                              ))
                            case _ =>
                              <.td()
                          }
                        }

                        /*
                         * @param pd
                         * @param vmin min of times played as defended, declared, or passed
                         * @param vmax max of times played as defended, declared, or passed
                         */
                        def rectangleHand( pd: PairData, vmin: Int, vmax: Int ): TagMod = {
                          pd.details match {
                            case Some(det) if det.declarer+det.defended+det.passed != 0 =>
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
                              <.td( <.div(
                                titleAttr := titleDec+"\n"+titleDef+"\n"+titlePas,
                                baseStyles.hover,
                                PieChartOrSquareForZero(
                                    if (det.declarer == 0) -sizeDec else sizeDec,
                                    Color.Black,
                                    det.made.toDouble::det.down.toDouble::Nil,
                                    Color.Green::Color.Red::Nil,
                                    None ), // Some(titleDec) ),
                                PieChartOrSquareForZero(
                                    if (det.defended == 0) -sizeDef else sizeDef,
                                    Color.Black,
                                    det.tookDown.toDouble::det.allowedMade.toDouble::Nil,
                                    Color.Green::Color.Red::Nil,
                                    None ), // Some(titleDef) ),
                                PieChartOrSquareForZero(
                                    if (det.passed == 0) -sizePas else sizePas,
                                    Color.Black,
                                    det.passed.toDouble::Nil,
                                    Color.Blue::Nil,
                                    None ) // Some(titlePas) )
                              ))
                            case _ =>
                              <.td()
                          }
                        }

                        /*
                         * @param pd
                         * @param vmin min of total times played
                         * @param vmax max of total times played
                         */
                        def rectangleBoth( pd: PairData, vmin: Int, vmax: Int ): TagMod = {
                          pd.details match {
                            case Some(det) if det.declarer+det.defended+det.passed != 0 =>
                              val sizeAll = size(det.total, vmin, vmax, state.minSize, state.maxSize)
                              val oneOverDec = 100.0/det.declarer
                              val oneOverDef = 100.0/det.defended
                              val oneOverPas = 100.0/det.passed
                              val oneOverTotal = 100.0/(det.declarer+det.defended+det.passed)
                              val titleDec = f"Declarer ${det.declarer} (${det.declarer*oneOverTotal}%.2f%%)\n  Made ${det.made} (${det.made*oneOverDec}%.2f%%)%n  Down ${det.down} (${det.down*oneOverDec}%.2f%%)"
                              val titleDef = f"Defended ${det.defended} (${det.defended*oneOverTotal}%.2f%%)\n  Took Down ${det.tookDown} (${det.tookDown*oneOverDef}%.2f%%)%n  Allowed Made ${det.allowedMade} (${det.allowedMade*oneOverDef}%.2f%%)"
                              val titlePas = f"Passed ${det.passed} (${det.passed*oneOverTotal}%.2f%%)"
                              <.td( <.div(
                                titleAttr := titleDec+"\n"+titleDef+"\n"+titlePas,
                                baseStyles.hover,
                                PieChartOrSquareForZero(
                                    if (det.total == 0) -sizeAll else sizeAll,
                                    Color.Black,
                                    det.made.toDouble::det.down.toDouble::det.allowedMade.toDouble::det.tookDown.toDouble::det.passed.toDouble::Nil,
                                    Color.Green::Color.Red::DarkRed::DarkGreen::Color.Blue::Nil,
                                    None ), // Some(titleDec) ),
                              ))
                            case _ =>
                              <.td()
                          }
                        }

                        def squareHand(colPlayer: String, vmin: Int, vmax: Int ) = {
                          if (rowplayer == colPlayer) {
                            <.td("x")
                          } else {
                            pds.get(rowplayer, colPlayer) match {
                              case Some(pd) =>
                                rectangleHand(pd,vmin,vmax)
                              case None =>
                                <.td()
                            }
                          }
                        }

                        def squareBoth(colPlayer: String, vmin: Int, vmax: Int ) = {
                          if (rowplayer == colPlayer) {
                            <.td("x")
                          } else {
                            pds.get(rowplayer, colPlayer) match {
                              case Some(pd) =>
                                rectangleBoth(pd,vmin,vmax)
                              case None =>
                                <.td()
                            }
                          }
                        }

                        displayType match {
                          case DisplayDecDef =>
                            <.tr(
                              <.td( rowplayer ),
                              players.filter( e => props.filter.isPlayerShown(e) ).map( p => square(p,sizeStat) ).toTagMod,
                              rectangleTotal(playerTotal, playedStatPlayerTotals)
                            )
                          case DisplayMadeDown =>
                            val vmin = Math.min( passedStat.min, Math.min( sizeStat.min, colorStat.min )).toInt
                            val vmax = Math.max( passedStat.max, Math.max( sizeStat.max, colorStat.max )).toInt
                            val vminplayer = Math.min( passedStatPlayer.min, Math.min( playedStatPlayerTotals.min, colorStatPlayerTotals.min )).toInt
                            val vmaxplayer = Math.max( passedStatPlayer.max, Math.max( playedStatPlayerTotals.max, colorStatPlayerTotals.max )).toInt
                            <.tr(
                              <.td( rowplayer ),
                              players.filter( e => props.filter.isPlayerShown(e) ).map( p => squareHand(p,vmin,vmax) ).toTagMod,
                              rectangleTotalHand(playerTotal, vminplayer, vmaxplayer)
                            )
                          case DisplayAll =>
                            val vmin = Math.min( passedStat.min, Math.min( sizeStat.min, colorStat.min )).toInt
                            val vmax = Math.max( passedStat.max, Math.max( sizeStat.max, colorStat.max )).toInt
                            val vminplayer = playedStatPlayerTotals.min.toInt
                            val vmaxplayer = playedStatPlayerTotals.max.toInt
                            <.tr(
                              <.td( rowplayer ),
                              players.filter( e => props.filter.isPlayerShown(e) ).map( p => squareBoth(p,vmin,vmax) ).toTagMod,
                              rectangleTotalBoth(playerTotal, vminplayer, vmaxplayer)
                            )
                        }
                      }).build

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( displayType: DisplayType = DisplayDecDef,
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
          val summary = new PairsDataSummary( pds, ColorByDeclarerResults, props.filter.selected, if (state.displayType==DisplayMadeDown) ColorByDefendedResults else ColorByPlayedResults, ColorByPassedResults )
          val allPlayers = summary.playerTotals.filter { e =>
            e._2.details.map( d => d.declarer+d.defended+d.passed>0 ).getOrElse(false)
          }.map( e => e._1 ).toList.sorted
          val sortedPlayers = props.filter.selected.map( l => l ).getOrElse(allPlayers)

          def getButton( displayType: DisplayType, id: String, text: String ) = {
            AppButton(
              id,
              text,
              ^.onClick-->setDisplayType(displayType),
              BaseStyles.highlight(selected = displayType==state.displayType)
            )
          }

          <.div(
            dupStyles.divPairsDetailsGrid,
            <.table(
                ^.id:="PairsGrid",
                dupStyles.tablePairsDetailsGrid,
                <.caption(
                  "Hand Results ",
                  getButton( DisplayDecDef, "DisplayDecDef", "Declared/Defended" ),
                  getButton( DisplayMadeDown, "DisplayMadeDown", "Made Down" ),
                  getButton( DisplayAll, "DisplayAll", "All" )
                ),
                SummaryHeader((props,state,this,sortedPlayers)),
                <.tfoot(
                  <.tr(
                    <.td(
                      ^.colSpan:=sortedPlayers.length+2,
                      state.displayType match {
                        case DisplayDecDef =>
                          TagMod(
                            "The size of the circle is proportional to the number of hands played by the pair/player. ",
                            <.br,
                            "Green, ",
                            PieChart( 15, 1.0::Nil, Color.Green::Nil, attrs=Some(^.display := "inline-block") ),
                            ", indicates the number of times the team was declarer ",
                            <.br,
                            "Red, ",
                            PieChart( 15, 1.0::Nil, Color.Red::Nil, attrs=Some(^.display := "inline-block") ),
                            ", indicates the number of times the team defended",
                            <.br,
                            "Blue, ",
                            PieChart( 15, 1.0::Nil, Color.Blue::Nil, attrs=Some(^.display := "inline-block") ),
                            ", indicates passed out hands"
                          )
                        case DisplayMadeDown =>
                          TagMod(
                            "The size of the circle is proportional to the number of hands played by the pair/player. ",
                            "A black square, ",
                            PieChartOrSquareForZero( -5, Color.Black, 1.0::Nil, Color.Green::Nil, attrs=Some(^.display := "inline-block") ),
                            ", indicates 0",
                            <.br,
                            "The first circle is the results of hands were the team was declarer.  green, ",
                            PieChart( 15, 1.0::Nil, Color.Green::Nil, attrs=Some(^.display := "inline-block") ),
                            ", is made, red, ",
                            PieChart( 15, 1.0::Nil, Color.Red::Nil, attrs=Some(^.display := "inline-block") ),
                            ", is down.",
                            <.br,
                            "The second circle is the results of hands were the team was defending.  green is took down, red is allowed made.",
                            <.br,
                            "The third circle, blue, ",
                            PieChart( 15, 1.0::Nil, Color.Blue::Nil, attrs=Some(^.display := "inline-block") ),
                            ", is the results of hands were passed out.",
                          )
                        case DisplayAll =>
                          TagMod(
                            "The size of the circle is proportional to the number of hands played by the pair/player. ",
                            <.br,
                            "Green, ",
                            PieChart( 15, 1.0::Nil, Color.Green::Nil, attrs=Some(^.display := "inline-block") ),
                            ", is contract made as declarer, red, ",
                            PieChart( 15, 1.0::Nil, Color.Red::Nil, attrs=Some(^.display := "inline-block") ),
                            ", is down as declarer.",
                            <.br,
                            "Dark green, ",
                            PieChart( 15, 1.0::Nil, DarkGreen::Nil, attrs=Some(^.display := "inline-block") ),
                            ", is took down as defender, dark red, ",
                            PieChart( 15, 1.0::Nil, DarkRed::Nil, attrs=Some(^.display := "inline-block") ),
                            ", is allowed made as defender.",
                            <.br,
                            "Blue, ",
                            PieChart( 15, 1.0::Nil, Color.Blue::Nil, attrs=Some(^.display := "inline-block") ),
                            ", is passed out hands."
                          )
                      }
                    )
                  )
                ),
                <.tbody(
                  sortedPlayers.zipWithIndex.filter( e => props.filter.isPlayerShown(e._1) ).map { e =>
                    val (rowplayer,i) = e
                    //      SummaryRow args:        (props,state,rowplayer, players, colorStat, sizeStat)
                    SummaryRow.withKey( s"PDD${i}" )(( props,
                                                      state,
                                                      rowplayer,
                                                      sortedPlayers,
                                                      summary.colorStat,
                                                      summary.extraStats.head,
                                                      summary.extraStats.tail.head,
                                                      summary.playerTotals.get(rowplayer).getOrElse(PairData(rowplayer,"",0,0,0,0,0,0,None,0,0,0,0,0)),
                                                      summary.colorStatPlayerTotals,
                                                      summary.extraStatsPlayer.head,
                                                      summary.extraStatsPlayer.tail.head
                                                   ))
                  }.toTagMod
                )
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

