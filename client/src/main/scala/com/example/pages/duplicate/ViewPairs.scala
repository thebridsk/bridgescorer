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
import com.example.data.duplicate.suggestion.PairsDataSummary

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
 * ViewPairs( routerCtl: RouterCtl[DuplicatePage] )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewPairs {
  import ViewPairsInternal._

  case class Props( filter: ViewPlayerFilter.Filter, showNoDataMsg: Boolean = false)

  def apply( filter: ViewPlayerFilter.Filter, showNoDataMsg: Boolean = false ) =
    component(Props(filter,showNoDataMsg))

}

object ViewPairsInternal {
  import ViewPairs._
  import DuplicateStyles._

  val logger = Logger("bridge.ViewPairs")

  val SummaryHeader = ScalaComponent.builder[(Props,State,Backend)]("PairsHeader")
                        .render_P( args => {
                          val (props,state,backend) = args
                          val sortBy = state.sortBy
                          def button( id: String, name: String, sort: SortBy ) = {
                            AppButton( id, name,
                                       ^.onClick --> backend.setSortBy(sort),
                                       sortBy==sort ?= baseStyles.buttonSelected
                                     )
                          }
                          <.thead(
                            <.tr(
                              <.th( button( "Player1", "Player 1", SortByName) ),
                              <.th( button( "Player2", "Player 2", SortByName) ),
                              <.th( button( "WonPct", "% Won", SortByWonPct) ),
                              <.th( button( "WonPtsPct", "% Won Pts", SortByWonPtsPct) ),
                              <.th( button( "ScorePct", "% Points", SortByScorePct) ),
                              <.th( button( "Won", "Won", SortByWon) ),
                              <.th( button( "WonPts", "Won Pts", SortByWonPts) ),
                              <.th( button( "Played", "Played", SortByPlayed)),
                              <.th( "Incomplete" ),
                              <.th( "Points"),
                              <.th( "Total")
                            )
                          )
                        }).build


  val SummaryRow = ScalaComponent.builder[(Props,PairData)]("PairsRow")
                      .render_P( props => {
                        val (pr,pd) = props
                        <.tr(
                          <.td( pd.player1 ),
                          <.td( pd.player2 ),
                          <.td( f"${pd.winPercent}%.2f" ),
                          <.td( f"${pd.winPtsPercent}%.2f" ),
                          <.td( f"${pd.pointsPercent}%.2f" ),
                          <.td( f"${pd.won}" ),
                          <.td( f"${pd.wonPts}%.2f" ),
                          <.td( f"${pd.played}" ),
                          <.td( f"${pd.incompleteGames}" ),
                          <.td(  Utils.toPointsString( pd.points ) ),
                          <.td( f"${pd.totalPoints}" )
                        )
                      }).build

  def compareName1( l: PairData, r: PairData ): Int = {
    l.player1.compareTo(r.player1)
  }

  def compareName2( l: PairData, r: PairData ): Int = {
    l.player2.compareTo(r.player2)
  }

  def compareWonPct( l: PairData, r: PairData ) = {
    r.winPercent.compareTo(l.winPercent)
  }

  def compareWonPtsPct( l: PairData, r: PairData ) = {
    r.winPtsPercent.compareTo(l.winPtsPercent)
  }

  def compareWon( l: PairData, r: PairData ) = {
    r.won.compareTo(l.won)
  }

  def compareWonPts( l: PairData, r: PairData ) = {
    r.wonPts.compareTo(l.wonPts)
  }

  def compareScorePct( l: PairData, r: PairData ) = {
    r.pointsPercent.compareTo(l.pointsPercent)
  }

  def comparePlayed( l: PairData, r: PairData ) = {
    r.played.compareTo(l.played)
  }

  trait SortBy {
    val compfun: List[(PairData,PairData)=>Int]
    def getPairData( lpd: List[PairData] ) = lpd.sortWith( sort _ )
    def sort( l: PairData, r: PairData ): Boolean = {
      for (c <- compfun) {
        val rc = c(l,r)
        if (rc != 0) return rc<0
      }
      false
    }
  }
  object SortByWonPct extends SortBy {
    val compfun = List(compareWonPct, compareScorePct, comparePlayed, compareName1, compareName2)
  }
  object SortByWonPtsPct extends SortBy {
    val compfun = List(compareWonPtsPct, compareScorePct, comparePlayed, compareName1, compareName2)
  }
  object SortByWon extends SortBy {
    val compfun = List(compareWon, compareScorePct, comparePlayed, compareName1, compareName2)
  }
  object SortByWonPts extends SortBy {
    val compfun = List(compareWonPts, compareScorePct, comparePlayed, compareName1, compareName2)
  }
  object SortByScorePct extends SortBy {
    val compfun = List(compareScorePct, compareWonPct, comparePlayed, compareName1, compareName2)
  }
  object SortByPlayed extends SortBy {
    val compfun = List(comparePlayed, compareWonPct, compareScorePct, compareName1, compareName2)
  }
  object SortByName extends SortBy {
    val compfun = List(compareName1, compareName2)

    override
    def getPairData( lpd: List[PairData] ) = {
      val data = lpd:::lpd.map(pd => pd.swapNames).toList
      data.sortWith( sort _ )
    }
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( sortBy: SortBy = SortByWonPct)

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def setSortBy( sortBy: SortBy ) = scope.modState { s => s.copy(sortBy=sortBy) }

    def render( props: Props, state: State ) = {

      props.filter.pairsData match {
        case Some(psd) =>
          val filter = props.filter.selectedPlayers
          val lpd = psd.data.values.filter( pd => filter.contains(pd.player1) && filter.contains(pd.player2) ).toList
          val sorted = state.sortBy.getPairData(lpd)
          <.table(
            ^.id:="Pairs",
            dupStyles.tablePairsSummary,
            SummaryHeader((props,state,this)),
            <.tbody(
              sorted.zipWithIndex.map { e =>
                val (pd,i) = e
                SummaryRow.withKey( s"PD${i}" )((props,pd))
              }.toTagMod
            )
          )
        case None =>
          <.div(
            dupStyles.divPairsGrid,
            props.showNoDataMsg ?= "Waiting for data"
          )
      }
    }
  }

  val component = ScalaComponent.builder[Props]("ViewPairs")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

