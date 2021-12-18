package com.github.thebridsk.bridge.client.pages.individual.components

import com.github.thebridsk.utilities.logging.Logger

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.client.pages.individual.router.IndividualDuplicateRouter.BaseScoreboardViewWithPerspective
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateScore
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateSummaryDetails
import com.github.thebridsk.bridge.client.pages.individual.styles.IndividualStyles

/**
  * Shows detail stats for a match.
  *
  * To use, just code the following:
  *
  * {{{
  * val page: BaseScoreboardViewWithPerspective = ...
  * val score: IndividualDuplicateScore = ...
  *
  * ViewScoreboardDetails(
  *   page: BaseScoreboardViewWithPerspective,
  *   score: IndividualDuplicateScore
  * )
  * }}}
  *
  * @author werewolf
  */
object ViewScoreboardDetails {
  case class Props(
      page: BaseScoreboardViewWithPerspective,
      score: IndividualDuplicateScore
  )

  /**
    * Instantiate the react component
    *
    * @param page the page that identifies the perspective
    * @param score the score object for the current match
    *
    * @return the unmounted react component
    */
  def apply(
    page: BaseScoreboardViewWithPerspective,
    score: IndividualDuplicateScore
  ) =
    Internal.component(
      Props(page, score)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    val logger: Logger = Logger("bridge.ViewScoreboardDetails")

    case class State()

    class Backend(scope: BackendScope[Props, State]) {
      import IndividualStyles._
      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        val details = props.score.getDetails
        <.div(
          dupStyles.viewScoreboardDetails,
          <.table(
            Header(props),
            <.tbody(
              details
                .sortWith((l, r) => l.player < r.player)
                .zipWithIndex
                .map { entry =>
                  val (detail, i) = entry
                  Row.withKey(i)((props, detail))
                }
                .toTagMod
            )
          )
        )
      }

    }

    private val Header = ScalaComponent
      .builder[Props]("ViewScoreboardDetails.Header")
      .render_P { props =>
        <.thead(
          <.tr(
            <.th("Player", ^.rowSpan := 2),
            <.th("Declarer", ^.colSpan := 3),
            <.th("Defended", ^.colSpan := 3),
            <.th("Passed", ^.rowSpan := 2)
          ),
          <.tr(
            <.th("Total"),
            <.th("Made"),
            <.th("Down"),
            <.th("Total"),
            <.th("Made"),
            <.th("Down")
          )
        )
      }
      .build

    private val Row = ScalaComponent
      .builder[(Props, IndividualDuplicateSummaryDetails)]("ViewScoreboardDetails.Row")
      .render_P { args =>
        val (props, detail) = args
        <.tr(
          <.td(detail.player),
          <.td(detail.declarer),
          <.td(detail.made),
          <.td(detail.down),
          <.td(detail.defended),
          <.td(detail.allowedMade),
          <.td(detail.tookDown),
          <.td(detail.passed)
        )
      }
      .build

    val component = ScalaComponent
      .builder[Props]("ViewScoreboardDetails")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .build
  }

}
