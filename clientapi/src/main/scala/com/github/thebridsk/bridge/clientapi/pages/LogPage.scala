package com.github.thebridsk.bridge.clientapi.pages

import com.github.thebridsk.bridge.clientapi.routes.AppRouter._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
//import com.github.thebridsk.bridge.clientapi.fastclick.FastClick
import com.github.thebridsk.bridge.clientapi.routes.BridgeRouter
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.clientcommon.debug.DebugLoggerComponent


/**
 * @author werewolf
 */
object LogPage {

  var debugging = false

  case class Props( routeCtl: BridgeRouter[AppPage])

  case class State(
  ) {
  }

  class Backend( scope: BackendScope[Props, State]) {

    def render( props: Props, state: State ) = { // scalafix:ok ExplicitResultTypes; React
      <.div(
        RootBridgeAppBar(
            title = Seq(MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit,
            )(
                <.span(
                  " Logs",
                )
            ) ),
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

  private val component = ScalaComponent.builder[Props]("LogPage")
        .initialStateFromProps { props => State() }
        .backend( backendScope => new Backend(backendScope))
        .renderBackend
        .componentDidMount( scope => scope.backend.didMount)
        .componentWillUnmount( scope => scope.backend.willUnmount )
        .build

  def apply( routeCtl: BridgeRouter[AppPage] ) = component(Props(routeCtl))  // scalafix:ok ExplicitResultTypes; ReactComponent

}
