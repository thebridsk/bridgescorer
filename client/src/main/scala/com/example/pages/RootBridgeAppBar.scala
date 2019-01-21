package com.example.pages

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.materialui.MuiAppBar
import com.example.materialui.Position
import com.example.materialui.MuiToolbar
import com.example.materialui.MuiIconButton
import com.example.materialui.icons.MuiMenuIcon
import com.example.materialui.MuiTypography
import com.example.materialui.ColorVariant
import com.example.materialui.TextVariant
import com.example.materialui.TextColor
import com.example.materialui.icons.MuiHomeIcon
import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.Node
import utils.logging.Logger
import com.example.materialui.icons.MuiHelpIcon
import japgolly.scalajs.react.vdom.HtmlStyles
import com.example.materialui.component.MyMenu
import com.example.materialui.MuiMenuItem
import com.example.routes.AppRouter.AppPage
import com.example.routes.BridgeRouter
import com.example.routes.AppRouter.About
import com.example.react.AppButtonLinkNewWindow
import org.scalajs.dom.document
import japgolly.scalajs.react.vdom.VdomNode
import com.example.routes.AppRouter.Home
import com.example.routes.AppRouter.Info
import com.example.routes.AppRouter.GraphQLAppPage
import com.example.routes.AppRouter.GraphiQLView
import com.example.routes.AppRouter.VoyagerView
import com.example.pages.chicagos.ChicagoModule.PlayChicago2
import com.example.pages.chicagos.ChicagoRouter.{ ListView => ChicagoListView }
import com.example.pages.rubber.RubberRouter.{ ListView => RubberListView }
import com.example.pages.rubber.RubberModule.PlayRubber
import com.example.pages.duplicate.DuplicateModule.PlayDuplicate
import com.example.pages.duplicate.DuplicateRouter.SummaryView
import com.example.routes.AppRouter.ColorView

/**
 * A simple AppBar for the Bridge client.
 *
 * It can be used for all pages but the home page.
 *
 * The AppBar has in the banner from left to right:
 *
 * <ol>
 * <li>Page Menu button
 * <li>Home button
 * <li>title
 * <li>Help button
 * </ol>
 *
 * To use, just code the following:
 *
 * <pre><code>
 * RootBridgeAppBar( RootBridgeAppBar.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object RootBridgeAppBar {
  import RootBridgeAppBarInternal._

  case class Props(
      title: Seq[VdomNode],
      helpurl: Option[String],
      routeCtl: BridgeRouter[AppPage]
  )

  def apply(
      title: Seq[VdomNode],
      helpurl: Option[String],
      routeCtl: BridgeRouter[AppPage]
  )() =
    component(Props(title,helpurl,routeCtl))()

}

object RootBridgeAppBarInternal {
  import RootBridgeAppBar._

  val logger = Logger("bridge.RootBridgeAppBar")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State(
      anchorMainEl: js.UndefOr[Element] = js.undefined,
  ) {

    def openMainMenu( n: Node ) = copy( anchorMainEl = n.asInstanceOf[Element] )
    def closeMainMenu() = copy( anchorMainEl = js.undefined )
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def handleMainClick( event: ReactEvent ) = event.extract(_.currentTarget)(currentTarget => scope.modState(s => s.openMainMenu(currentTarget)).runNow() )
    def handleMainCloseClick( event: ReactEvent ) = scope.modState(s => s.closeMainMenu()).runNow()
    def handleMainClose( /* event: js.Object, reason: String */ ) = {
      logger.fine("MainClose called")
      scope.modState(s => s.closeMainMenu()).runNow()
    }

    def gotoPage( uri: String ) = {
      val location = document.defaultView.location
      val origin = location.origin.get
      val helppath = s"""${origin}${uri}"""

      AppButtonLinkNewWindow.topage(helppath)
    }

    def handleHelpGotoPageClick(uri: String)( event: ReactEvent ) = {
      logger.info(s"""Going to page ${uri}""")
//      handleHelpClose()

      gotoPage(uri)
    }

    def render( props: Props, state: State ) = {
      import BaseStyles._

      def callbackPage(page: AppPage)(e: ReactEvent) = props.routeCtl.set(page).runNow()

      <.div(

          BridgeAppBar(
              handleMainClick _,
              props.title,
              props.helpurl.getOrElse("/help/introduction.html"),
              props.routeCtl
          )(

            // main menu
            MyMenu(
                anchorEl=state.anchorMainEl,
                onClickAway = handleMainClose _,
                onItemClick = handleMainCloseClick _,
                className = Some("popupMenu")
            )(
                MuiMenuItem(
                    id = "Duplicate",
                    onClick = callbackPage(PlayDuplicate(SummaryView)) _
                )(
                    "Duplicate"
                ),
                MuiMenuItem(
                    id = "Chicago",
                    onClick = callbackPage(PlayChicago2(ChicagoListView)) _
                )(
                    "Chicago"
                ),
                MuiMenuItem(
                    id = "Rubber",
                    onClick = callbackPage(PlayRubber(RubberListView)) _
                )(
                    "Rubber"
                ),
                MuiMenuItem(
                    id = "Info",
                    onClick = callbackPage(Info) _
                )(
                    "Info"
                ),
                MuiMenuItem(
                    id = "GraphQL",
                    onClick = callbackPage(GraphQLAppPage) _
                )(
                    "GraphQL"
                ),
                MuiMenuItem(
                    id = "GraphiQL",
                    onClick = callbackPage(GraphiQLView) _
                )(
                    "GraphiQL"
                ),
                MuiMenuItem(
                    id = "Voyager",
                    onClick = callbackPage(VoyagerView) _
                )(
                    "Voyager"
                ),
                MuiMenuItem(
                    id = "Color",
                    onClick = callbackPage(ColorView) _
                )(
                    "Color"
                ),
            )
          )
      )
    }

    private var mounted = false

    val didMount = Callback {
      mounted = true

    }

    val willUnmount = Callback {
      mounted = false

    }
  }

  val component = ScalaComponent.builder[Props]("RootBridgeAppBar")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}
