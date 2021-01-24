package com.github.thebridsk.bridge.client.pages.individual.components

import com.github.thebridsk.utilities.logging.Logger

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateScore
import com.github.thebridsk.bridge.client.pages.individual.styles.IndividualStyles

/**
  * Shows the team x board table and has a totals column that shows the number of points the team has.
  *
  * To use, just code the following:
  *
  * <pre><code>
  * ViewPlayerMatchResult( routerCtl: BridgeRouter[DuplicatePage], score: MatchDuplicateScore )
  * </code></pre>
  *
  * @author werewolf
  */
object ViewPlayerMatchResult {
  case class Props(
      score: List[IndividualDuplicateScore.Place],
      useIMPs: Boolean = false
  )

  def apply(
      score: List[IndividualDuplicateScore.Place],
      useIMPs: Boolean = false
  ) = // scalafix:ok ExplicitResultTypes; ReactComponent
    Internal.component(Props(score, useIMPs))

  protected object Internal {

    val logger: Logger = Logger("bridge.ViewPlayerMatchResult")

    case class State()

    class Backend(scope: BackendScope[Props, State]) {
      import IndividualStyles._
      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        val places = props.score.sortWith((l, r) => l.place < r.place)

        <.div(
          dupStyles.viewPlayerMatchResult,
          <.table(
            ^.id := "scoreboardplayers",
            <.thead(
              <.tr(
                <.th("Place"),
                <.th(if (props.useIMPs) "IMP" else "Point"),
                <.th("Players")
              )
            ),
            <.tbody(
              places.map { place =>
                <.tr(
                  <.td(place.place.toString),
                  <.td(
                    if (props.useIMPs) f"${place.score}%.1f"
                    else f"${place.score.toInt}%d"
                  ),
                  <.td(
                    place.players.flatMap { p =>
                      List[TagMod](
                        <.br,
                        <.span(p)
                      )
                    }.tail.toTagMod
                  )
                )
              }.toTagMod
            )
          )
        )
      }

    }

    val component = ScalaComponent
      .builder[Props]("ViewPlayerMatchResults")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .build
  }

}
