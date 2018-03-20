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
import com.example.data.duplicate.suggestion.ColorByPlayed
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

  val SummaryHeader = ScalaComponent.builder[(Props,State,Backend,List[String])]("PairsHeader")
                        .render_P( args => {
                          val (props,state,backend,players) = args
                          def getButton( colorBy: ColorBy, id: String, text: String ) = {
                            AppButton( id,
                                       text,
                                       ^.onClick-->backend.setColorBy(colorBy),
                                       BaseStyles.highlight(selected = colorBy==state.colorBy)
                                       )

                          }
                          <.thead(
                            <.tr(
                              <.th(
                                ^.colSpan:=players.length+2,
                                getButton( ColorByWon, "ColorByWon", "Color by won" ),
                                getButton( ColorByWonPct, "ColorByWonPct", "Color by Won%" ),
                                getButton( ColorByWonPts, "ColorByWonPts", "Color by WonPoints" ),
                                getButton( ColorByWonPtsPct, "ColorByWonPtsPct", "Color by WonPoints%" ),
                                getButton( ColorByPointsPct, "ColorByPointsPct", "Color by Points%" )
                              )
                            ),
                            <.tr(
                              <.th("Players"),
                              players.filter( e => props.filter.isPlayerShown(e) ).map( p => <.th( p ) ).toTagMod,
                              <.th("Totals")
                            )
                          )
                        }).build

  val titleAttr    = VdomAttr("data-title")

  val SummaryRow = ScalaComponent.builder[(Props,State,String,List[String],Stat,Stat,PairData,Stat,Stat)]("PairsRow")
                      .render_P( args => {
                        val (props,state,rowplayer, players, colorStat, sizeStat,
                             playerTotal,playedStatPlayerTotals,colorStatPlayerTotals) = args
                        val pds = props.filter.pairsData.get
                        val colorBy = state.colorBy

                        def rectangleTotal( pd: PairData, sizeSt: Stat, colorSt: Stat ) = {
                          if (pd.played == 0) EmptyVdom
                          else {
                            val (bcolor, intense) = colorSt.sizeAve(pd, 0, 255)
                            val size = sizeSt.size(pd, state.minSize*2, state.maxSize*2)
                            val color = if (bcolor) {
                              // above average, blue
                              Color( 255-intense, 255-intense, 255 )    // r, g, b
                            } else {
                              // below average, red
                              Color( 255, 255, intense )    // r, g, b
                            }
                            val title = f"""Played ${pd.played},\nWon ${pd.won} (${pd.winPercent}%.2f%%),\nWonPoints ${pd.wonPts}%.2f (${pd.winPtsPercent}%.2f%%),\nPoints ${pd.points}%.1f (${pd.pointsPercent}%.2f%%)"""
                            TagMod(
                              <.div( ^.backgroundColor:=color.toHex,
                                     ^.width:=size.px,
                                     ^.height:=20.px
                                   ),
                              titleAttr:=title,
                              baseStyles.hover
                            )
                          }
                        }

                        def rectangle( pd: PairData, sizeSt: Stat, colorSt: Stat ) = {
                          val (bcolor, intense) = colorSt.sizeAve(pd, 0, 255)
                          val size = sizeSt.size(pd, state.minSize, state.maxSize)
                          val color = if (bcolor) {
                            // above average, green
                            Color( 255-intense, 255, 255-intense )    // r, g, b
                          } else {
                            // below average, red
                            Color( 255, intense, intense )    // r, g, b
                          }
                          val title = f"""Played ${pd.played},\nWon ${pd.won} (${pd.winPercent}%.2f%%),\nWonPoints ${pd.wonPts}%.2f (${pd.winPtsPercent}%.2f%%),\nPoints ${pd.points}%.1f (${pd.pointsPercent}%.2f%%)"""
                          TagMod(
                            <.div( ^.backgroundColor:=color.toHex,
                                   ^.width:=size.px,
                                   ^.height:=20.px
                                 ),
                            titleAttr:=title,
                            baseStyles.hover
                          )
                        }

                        def square(colPlayer: String ) = {
                          if (rowplayer == colPlayer) {
                            TagMod("x")
                          } else {
                            pds.get(rowplayer, colPlayer) match {
                              case Some(pd) =>
                                rectangle(pd,sizeStat,colorStat)
                              case None =>
                                EmptyVdom
                            }
                          }
                        }

                        <.tr(
                          <.td( rowplayer ),
                          players.filter( e => props.filter.isPlayerShown(e) ).map( p => <.td( square(p) ) ).toTagMod,
                          <.td( rectangleTotal(playerTotal, playedStatPlayerTotals, colorStatPlayerTotals) )
                        )
                      }).build

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( colorBy: ColorBy = ColorByWonPct,
                    maxSize: Int = 60,
                    minSize: Int = 2
                  )

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def setColorBy( colorBy: ColorBy ) = scope.modState { s => s.copy(colorBy=colorBy) }

    def render( props: Props, state: State ) = {

      props.filter.pairsData match {
        case Some(pds) if !pds.players.isEmpty =>
          val summary = new PairsDataSummary( pds, state.colorBy, props.filter.selected, ColorByPlayed )
          val allPlayers = summary.players.sorted
          val sortedPlayers = summary.playerFilter.sorted

          <.div(
            dupStyles.divPairsGrid,
            <.table(
                ^.id:="PairsGrid",
                dupStyles.tablePairsGrid,
                SummaryHeader((props,state,this,sortedPlayers)),
                <.tbody(
                  sortedPlayers.zipWithIndex.filter( e => props.filter.isPlayerShown(e._1) ).map { e =>
                    val (rowplayer,i) = e
                    //      SummaryRow args:        (props,state,rowplayer, players, colorStat, sizeStat)
                    SummaryRow.withKey( s"PD${i}" )(( props,
                                                      state,
                                                      rowplayer,
                                                      sortedPlayers,
                                                      summary.colorStat,
                                                      summary.extraStats.head,
                                                      summary.playerTotals.get(rowplayer).getOrElse(PairData(rowplayer,"",0,0,0,0,0,0,None,0,0,0,0,0)),
                                                      summary.extraStatsPlayer.head,
                                                      summary.colorStatPlayerTotals
                                                   ))
                  }.toTagMod
                ),
                <.tfoot(
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
                      <.br,
                      <.br,
                      "For Totals, the width is relative to the number of times the player has played",
                      <.br,
                      "blue is above average and yellow is below average."
                    )
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
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

