package com.example.pages.duplicate

import com.example.data.Id
import com.example.data.Team
import com.example.data.bridge.PerspectiveComplete
import com.example.data.bridge.PerspectiveDirector
import com.example.data.bridge.PerspectiveTable
import utils.logging.Logger

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.example.data.bridge.MatchDuplicateScore
import com.example.pages.duplicate.DuplicateRouter.BaseScoreboardViewWithPerspective
import com.example.pages.duplicate.DuplicateRouter.TableRoundScoreboardView
import com.example.react.Utils._
import com.example.data.DuplicateSummaryDetails

/**
 * Shows the team x board table and has a totals column that shows the number of points the team has.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ViewScoreboardDetails( routerCtl: RouterCtl[DuplicatePage], score: MatchDuplicateScore )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewScoreboardDetails {
 case class Props( page: BaseScoreboardViewWithPerspective, md: MatchDuplicateScore )

  def apply( page: BaseScoreboardViewWithPerspective, md: MatchDuplicateScore  ) = ViewScoreboardDetailsInternal.component(Props(page,md))

}

object ViewScoreboardDetailsInternal {
  import ViewScoreboardDetails._

  val logger = Logger("bridge.ViewScoreboardDetails")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State()

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {
    import DuplicateStyles._
    def render( props: Props, state: State ) = {
      val details = props.md.getDetails()
      <.div(
        dupStyles.divScoreboardDetails,
        <.table(
          Header(props),
          <.tbody(
            details.sortWith((l,r) => Id.idComparer(l.team, r.team)<0).zipWithIndex.map { entry =>
              val (detail, i) = entry
              Row.withKey(i)((props,detail))
            }.toTagMod
          )
        )
      )
    }

  }

  val Header = ScalaComponent.builder[Props]("ViewScoreboardDetails.Header")
                      .render_P { props =>
                        <.thead(
                          <.tr(
                            <.th("Team", ^.rowSpan:=2),
                            <.th("Players", ^.rowSpan:=2),
                            <.th("Declarer", ^.colSpan:=3),
                            <.th("Defended", ^.colSpan:=3),
                            <.th("Passed", ^.rowSpan:=2)
                          ),
                          <.tr(
                            <.th("Total"),
                            <.th("Made"),
                            <.th("Down"),
                            <.th("Total"),
                            <.th("Made"),
                            <.th("Down"),
                          )
                        )
                      }.build

  val Row = ScalaComponent.builder[(Props, DuplicateSummaryDetails)]("ViewScoreboardDetails.Row")
                      .render_P { args =>
                        val (props,detail) = args
                        val (p1,p2) = props.md.getTeam(detail.team).map( t => (t.player1,t.player2) ).getOrElse(("",""))
                        <.tr(
                          <.td( Id.teamIdToTeamNumber(detail.team)),
                          <.td( s"${p1} ${p2}" ),
                          <.td( detail.declarer),
                          <.td( detail.made),
                          <.td( detail.down),
                          <.td( detail.defended),
                          <.td( detail.allowedMade),
                          <.td( detail.tookDown),
                          <.td( detail.passed)
                        )
                      }.build

  val component = ScalaComponent.builder[Props]("ViewScoreboardDetails")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

