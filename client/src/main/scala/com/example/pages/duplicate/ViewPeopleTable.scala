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
import com.example.data.duplicate.suggestion.PairsDataSummary
import com.example.data.duplicate.suggestion.ColorByWonPct
import com.example.data.duplicate.suggestion.PairData
import scala.annotation.tailrec

/**
 * Shows a summary page of all duplicate matches from the database.
 * Each match has a button that that shows that match, by going to the ScoreboardView(id) page.
 * There is also a button to create a new match, by going to the NewScoreboardView page.
 *
 * The data is obtained from the DuplicateStore object.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ViewPeopleTable( routerCtl: RouterCtl[DuplicatePage] )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewPeopleTable {
  import ViewPeopleTableInternal._

  case class Props( filter: ViewPlayerFilter.Filter, showNoDataMsg: Boolean = false)

  def apply(  filter: ViewPlayerFilter.Filter, showNoDataMsg: Boolean = false ) =
    component(Props(filter,showNoDataMsg))

}

object ViewPeopleTableInternal {
  import ViewPeopleTable._
  import DuplicateStyles._

  val logger = Logger("bridge.ViewPeopleTable")

  val SummaryHeader = ScalaComponent.builder[(Props,State,Backend)]("PeopleSummaryHeader")
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
                              <.th( button( "Player", "Player", SortByName) ),
                              <.th( button( "WonPct", "% Won", SortByWonPct) ),
                              <.th( button( "WonPts", "% WonPoints", SortByWonPts) ),
                              <.th( button( "ScorePct", "% Points", SortByScorePct) ),
                              <.th( "Won"),
                              <.th( "WonPoints"),
                              <.th( "Played"),
                              <.th( "Incomplete"),
                              <.th( "Points"),
                              <.th( "Total")
                            )
                          )
                        }).build


  val SummaryRow = ScalaComponent.builder[(Props,String,PairData)]("PeopleSummaryRow")
                      .render_P( props => {
                        val (pr,playername,pd) = props
                        <.tr(
                          <.td( playername ),
                          <.td( f"${pd.winPercent}%.2f" ),
                          <.td( f"${pd.winPtsPercent}%.2f" ),
                          <.td( f"${pd.pointsPercent}%.2f" ),
                          <.td( s"${pd.won}" ),
                          <.td( f"${pd.wonPts}%.2f" ),
                          <.td( s"${pd.played}" ),
                          <.td( s"${pd.incompleteGames}" ),
                          <.td( Utils.toPointsString(pd.points) ),
                          <.td( pd.totalPoints )
                        )
                      }).build

  trait SortBy {
    val sortList: List[SortBy]
    def isLessThen( pd1: PairData, pd2: PairData ): Boolean = {

      @tailrec
      def compare( list: List[SortBy] ): Boolean = {
        list.headOption match {
          case Some(h) =>
            val rc = h.compare(pd1, pd2)
            if ( rc != 0) {
              rc < 0
            } else {
              compare( list.tail )
            }
          case None =>
            false
        }
      }

      compare(sortList)
    }
    def compare( pd1: PairData, pd2: PairData ): Int
  }
  object SortByName extends SortBy {
    val sortList = this::Nil
    def compare( pd1: PairData, pd2: PairData ): Int = {
      pd1.player1.compareTo(pd2.player1)
    }
  }
  object SortByWonPct extends SortBy {
    val sortList = this::SortByScorePct::SortByName::Nil
    def compare( pd1: PairData, pd2: PairData ): Int = {
      -pd1.winPercent.compareTo(pd2.winPercent)
    }
  }
  object SortByWonPts extends SortBy {
    val sortList = {
      val x: SortBy = this
      val a: SortBy = SortByScorePct
      val b: SortBy = SortByName
      x::a::b::Nil
    }
    def compare( pd1: PairData, pd2: PairData ): Int = {
      -pd1.winPtsPercent.compareTo(pd2.winPtsPercent)
    }
  }
  object SortByScorePct extends SortBy {
    val sortList = this::SortByWonPts::SortByName::Nil
    def compare( pd1: PairData, pd2: PairData ): Int = {
      -pd1.pointsPercent.compareTo(pd2.pointsPercent)
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
        case Some(pd) =>
          val summary = new PairsDataSummary(pd, ColorByWonPct, props.filter.selected)
          val pds = summary.playerTotals.values.toList

          val sorted = pds.sortWith( state.sortBy.isLessThen _ )

          <.table(
            ^.id:="Players",
            dupStyles.tablePeopleSummary,
            SummaryHeader((props,state,this)),
            <.tbody(
              sorted.zipWithIndex.map { e =>
                val (pd,i) = e
                SummaryRow.withKey( i )((props,pd.player1,pd))
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

  val component = ScalaComponent.builder[Props]("ViewPeopleTable")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

