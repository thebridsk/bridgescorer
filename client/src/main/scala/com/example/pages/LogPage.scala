package com.example.pages

import com.example.routes.AppRouter._
import org.scalajs.dom.document
import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import utils.logging.Logger
import com.example.data.ServerURL
import com.example.rest2.RestClientServerURL
import com.example.data.ServerVersion
import com.example.controller.Controller
import com.example.controller.RubberController
import com.example.version.VersionClient
import com.example.version.VersionShared
import scala.util.Success
import scala.util.Failure
import com.example.react.AppButton
import com.example.routes.AppRouter.About
import com.example.pages.chicagos.ChicagoModule.PlayChicago2
import com.example.pages.chicagos.ChicagoRouter.ListView
import com.example.pages.duplicate.DuplicateModule.PlayDuplicate
import com.example.pages.duplicate.DuplicateRouter.SummaryView
import com.example.pages.duplicate.DuplicateRouter.NewDuplicateView
import com.example.pages.rubber.RubberModule.PlayRubber
import com.example.pages.rubber.RubberRouter.{ ListView => RubberListView}
import com.example.pages.chicagos.ChicagoRouter.NamesView
import com.example.pages.rubber.RubberRouter.RubberMatchNamesView
import com.example.pages.duplicate.DuplicateRouter.CompleteScoreboardView
import com.example.controller.ChicagoController
import com.example.react.Utils._
import com.example.react.Popup
import com.example.rest2.RestResult
import com.example.data.MatchChicago
import com.example.rest2.Result
import com.example.rest2.ResultHolder
import com.example.rest2.RequestCancelled
import scala.concurrent.ExecutionContext.Implicits.global
import com.example.rest2.RestClient
import com.example.data.RestMessage
import com.example.rest2.AjaxResult
import com.example.rest2.AjaxFailure
import com.example.rest2.WrapperXMLHttpRequest
import com.example.react.AppButtonLink
//import com.example.fastclick.FastClick
import com.example.react.PopupOkCancel
import com.example.pages.duplicate.DuplicateRouter.SelectMatchView
import japgolly.scalajs.react.extra.router.RouterCtl
import com.example.routes.BridgeRouter
import com.example.react.AppButtonLinkNewWindow
import com.example.react.HelpButton
import com.example.Bridge
import com.example.materialui.MuiButton
import com.example.materialui.ColorVariant
import com.example.materialui.MuiMenu
import com.example.materialui.MuiMenuItem
import org.scalajs.dom.raw.Node
import org.scalajs.dom.raw.Element
import com.example.materialui.Variant
import com.example.materialui.Style
import com.example.materialui.Position
import com.example.materialui.MuiAppBar
import com.example.materialui.MuiToolbar
import com.example.materialui.ColorVariant
import com.example.materialui.MuiTypography
import com.example.materialui.TextVariant
import com.example.materialui.TextColor
import com.example.materialui.Style
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport
import japgolly.scalajs.react.vdom.HtmlStyles
import com.example.materialui.MuiMenuList
import com.example.materialui.component.MyMenu
import com.example.materialui.PopperPlacement
import com.example.routes.AppRouter
import com.example.debug.DebugLoggerComponent

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
