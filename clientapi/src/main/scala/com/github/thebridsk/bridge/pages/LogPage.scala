package com.github.thebridsk.bridge.pages

import com.github.thebridsk.bridge.routes.AppRouter._
import org.scalajs.dom.document
import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.ServerURL
import com.github.thebridsk.bridge.rest2.RestClientServerURL
import com.github.thebridsk.bridge.data.ServerVersion
import com.github.thebridsk.bridge.version.VersionClient
import com.github.thebridsk.bridge.version.VersionShared
import scala.util.Success
import scala.util.Failure
import com.github.thebridsk.bridge.react.AppButton
import com.github.thebridsk.bridge.routes.AppRouter.About
import com.github.thebridsk.bridge.react.Utils._
import com.github.thebridsk.bridge.react.Popup
import com.github.thebridsk.bridge.rest2.RestResult
import com.github.thebridsk.bridge.rest2.Result
import com.github.thebridsk.bridge.rest2.ResultHolder
import com.github.thebridsk.bridge.rest2.RequestCancelled
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.rest2.RestClient
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.rest2.AjaxResult
import com.github.thebridsk.bridge.rest2.AjaxFailure
import com.github.thebridsk.bridge.rest2.WrapperXMLHttpRequest
import com.github.thebridsk.bridge.react.AppButtonLink
//import com.github.thebridsk.bridge.fastclick.FastClick
import com.github.thebridsk.bridge.react.PopupOkCancel
import japgolly.scalajs.react.extra.router.RouterCtl
import com.github.thebridsk.bridge.routes.BridgeRouter
import com.github.thebridsk.bridge.react.AppButtonLinkNewWindow
import com.github.thebridsk.bridge.react.HelpButton
import com.github.thebridsk.materialui.MuiButton
import com.github.thebridsk.materialui.ColorVariant
import com.github.thebridsk.materialui.MuiMenu
import com.github.thebridsk.materialui.MuiMenuItem
import org.scalajs.dom.raw.Node
import org.scalajs.dom.raw.Element
import com.github.thebridsk.materialui.Variant
import com.github.thebridsk.materialui.Style
import com.github.thebridsk.materialui.Position
import com.github.thebridsk.materialui.MuiAppBar
import com.github.thebridsk.materialui.MuiToolbar
import com.github.thebridsk.materialui.ColorVariant
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.materialui.Style
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport
import japgolly.scalajs.react.vdom.HtmlStyles
import com.github.thebridsk.materialui.MuiMenuList
import com.github.thebridsk.materialui.component.MyMenu
import com.github.thebridsk.materialui.PopperPlacement
import com.github.thebridsk.bridge.routes.AppRouter
import com.github.thebridsk.bridge.debug.DebugLoggerComponent

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

    def render( props: Props, state: State ) = {
      import BaseStyles._
      <.div(
        rootStyles.logDiv,
        RootBridgeAppBar(
            title = Seq("Logs"),
            helpurl = Some("../help/introduction.html"),
            routeCtl = props.routeCtl
        )(),
        DebugLoggerComponent()
      )
    }

    private var mounted = false

    val didMount = Callback {
      mounted = true
      // make AJAX rest call here
    }

    val willUnmount = Callback {
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

  def apply( routeCtl: BridgeRouter[AppPage] ) = component(Props(routeCtl))

}
