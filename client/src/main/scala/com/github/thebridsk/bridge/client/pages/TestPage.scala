package com.github.thebridsk.bridge.client.pages

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.client.pages.duplicate.components.EnterScorekeeper

/**
  * a Test page component.
  *
  * Used for development of components to help develop the CSS for the component.
  *
  * Properties:
  *   None
  *
  * Usage:
  *   <pre><code>
  *   val router: BridgeRouter[AppPage]
  *
  *   TestPage(router)
  *   </code></pre>
  *
  * @author werewolf
  */
object TestPage {
  import TestPageInternal._

  case class Props(router: BridgeRouter[AppPage])

  def apply(router: BridgeRouter[AppPage]) =
    component(Props(router)) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object TestPageInternal {

    val logger: Logger = Logger("bridge.TestPage")

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause State to leak.
      */
    case class State(
      name: String = "",
      selected: Option[PlayerPosition] = None
    )

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause Backend to leak.
      */
    class Backend(scope: BackendScope[Props, State]) {

      val didMount: Callback = Callback {
      }

      def setScoreKeeperPosition( p: PlayerPosition ) = scope.modState { state => state.copy(selected = Some(p)) }
      def setScoreKeeper( n: String ) = scope.modState { state => state.copy(name = n) }

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        <.div(
          RootBridgeAppBar(
            Seq(
              MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit
              )(
                <.span(
                  " Test"
                )
              )
            ),
            None,
            props.router
          )(),
          <.div(
            ^.id := "TestPage",
            <.div(
              EnterScorekeeper(
                name = state.name,
                selected = state.selected,
                tabIndex = -1,
                setScoreKeeper = setScoreKeeper,
                setScoreKeeperPosition = setScoreKeeperPosition
              )
            )
          )
        )
      }
    }

    private[pages] val component = ScalaComponent
      .builder[Props]("TestPage")
      .initialStateFromProps { props =>
        State()
      }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .build
  }

}
