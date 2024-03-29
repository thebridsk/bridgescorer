package com.github.thebridsk.bridge.client.pages

import com.github.thebridsk.bridge.client.routes.AppRouter._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
//import com.github.thebridsk.bridge.client.fastclick.FastClick
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.clientcommon.debug.DebugLoggerComponent

/**
  * A component page to view the logs and to set logging levels.
  *
  * From this page, logging can be started and stopped.  The logging level can also be set.
  *
  * Usage:
  * {{{
  * LogPage(
  *   routeCtl = ...
  * )
  * }}}
  *
  * @author werewolf
  */
object LogPage {

  case class Props(routeCtl: BridgeRouter[AppPage])

  /**
    * Instantiate the component
    *
    * @param routeCtl
    * @return the unmounted react component.
    */
  def apply(routeCtl: BridgeRouter[AppPage]) =
    Internal.component(
      Props(routeCtl)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    case class State(
    ) {}

    class Backend(scope: BackendScope[Props, State]) {

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        <.div(
          RootBridgeAppBar(
            title = Seq(
              MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit
              )(
                <.span(
                  " Logs"
                )
              )
            ),
            helpurl = Some("../help/introduction.html"),
            routeCtl = props.routeCtl
          )(),
          DebugLoggerComponent()
        )
      }

      private var mounted = false

      val didMount: Callback = Callback {
        mounted = true
        // make AJAX rest call here
      }

      val willUnmount: Callback = Callback {
        mounted = false
      }

    }

    val component = ScalaComponent
      .builder[Props]("LogPage")
      .initialStateFromProps { props => State() }
      .backend(backendScope => new Backend(backendScope))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .build

  }

}
