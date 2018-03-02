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
import com.example.data.DuplicateSummaryDetails

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
 * ViewPeopleTableDetails( routerCtl: RouterCtl[DuplicatePage] )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewPeopleTableDetails {
  import ViewPeopleTableDetailsInternal._

  case class Props( filter: ViewPlayerFilter.Filter, showNoDataMsg: Boolean = false)

  def apply(  filter: ViewPlayerFilter.Filter, showNoDataMsg: Boolean = false ) =
    component(Props(filter,showNoDataMsg))

}

object ViewPeopleTableDetailsInternal {
  import ViewPeopleTableDetails._
  import DuplicateStyles._
  import PairsDetailsSorting._

  val logger = Logger("bridge.ViewPeopleTableDetails")

  val SummaryHeader = ScalaComponent.builder[(Props,State,Backend)]("ViewPeopleTableDetails.Header")
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
                              <.th( button( "DeclarerPct", "% Declarer", SortByDeclarer) ),
                              <.th( button( "DefendedPct", "% Defended", SortByDefended) ),
                              <.th( button( "PassedPct", "% Passed", SortByPassed) ),
                              <.th( button( "MadePct", "% Made", SortByMade) ),
                              <.th( button( "DownPct", "% Down", SortByDown) ),
                              <.th( button( "AllowedMadePct", "% Allowed Made", SortByAllowedMade) ),
                              <.th( button( "TookDownPct", "% Took Down", SortByTookDown) ),
                              <.th( "Total" )
                            )
                          )
                        }).build


  val SummaryRow = ScalaComponent.builder[(Props,String,PairData)]("ViewPeopleTableDetails.Row")
                      .render_P( props => {
                        val (pr,playername,pd) = props
                        <.tr(
                          <.td( playername ),
                          pd.details match {
                            case Some(det) =>
                              TagMod(
                                <.td( f"${det.percentDeclared}%.2f" ),
                                <.td( f"${det.percentDefended}%.2f" ),
                                <.td( f"${det.percentPassed}%.2f" ),
                                <.td( f"${det.percentMade}%.2f" ),
                                <.td( f"${det.percentDown}%.2f" ),
                                <.td( f"${det.percentAllowedMade}%.2f" ),
                                <.td( f"${det.percentTookDown}%.2f" ),
                                <.td( s"${det.total}" )
                              )
                            case None =>
                              TagMod(
                                <.td(),
                                <.td(),
                                <.td(),
                                <.td(),
                                <.td(),
                                <.td(),
                                <.td(),
                                <.td()
                              )
                          }
                        )
                      }).build


  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( sortBy: SortBy = SortByDeclarer)

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

          val sorted = pds.sortWith( state.sortBy.sort _ )

          if (sorted.isEmpty) {
            <.div()
          } else {
            val tpd = sorted.map( pd => pd.details ).foldLeft( DuplicateSummaryDetails.zero("Totals")) { (ac,v) =>
              v.map( vv => ac.add(vv) ).getOrElse(ac)
            }
            val totalpd = sorted.head.copy( player1="Totals", player2="", details = Some(tpd) )

            <.table(
              ^.id:="Players",
              dupStyles.tablePeopleSummary,
              SummaryHeader((props,state,this)),
              <.tbody(
                sorted.zipWithIndex.map { e =>
                  val (pd,i) = e
                  SummaryRow.withKey( i )((props,pd.player1,pd))
                }.toTagMod,
                SummaryRow((props,"Totals", totalpd ))
              )
            )
          }

        case None =>
          <.div(
            dupStyles.divPairsGrid,
            props.showNoDataMsg ?= "Waiting for data"
          )
      }
    }
  }

  val component = ScalaComponent.builder[Props]("ViewPeopleTableDetails")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

