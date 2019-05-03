package com.example.pages.rubber

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.materialui.MuiAppBar
import com.example.materialui.Position
import com.example.materialui.MuiToolbar
import com.example.materialui.MuiTypography
import com.example.materialui.ColorVariant
import com.example.materialui.TextVariant
import com.example.materialui.TextColor
import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.Node
import utils.logging.Logger
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
import com.example.pages.BaseStyles
import com.example.data.Id
import com.example.pages.BridgeAppBar
import com.example.pages.HomePage
import com.example.materialui.icons.SvgColor

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
 * RubberPageBridgeAppBar( RubberPageBridgeAppBar.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object RubberPageBridgeAppBar {
  import RubberPageBridgeAppBarInternal._

  case class Props(
      pageMenuItems: Seq[CtorType.ChildArg],
      title: Seq[CtorType.ChildArg],
      helpurl: String,
      routeCtl: BridgeRouter[RubberPage]
  )

  def apply(
      title: Seq[CtorType.ChildArg],
      helpurl: String,
      routeCtl: BridgeRouter[RubberPage]
  )(
      mainMenuItems: CtorType.ChildArg*,
  ) =
    component(Props(mainMenuItems,title,helpurl,routeCtl))

}

object RubberPageBridgeAppBarInternal {
  import RubberPageBridgeAppBar._

  val logger = Logger("bridge.RubberPageBridgeAppBar")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State(
      anchorMainEl: js.UndefOr[Element] = js.undefined
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

    def render( props: Props, state: State ) = {
      import BaseStyles._

      def handleGotoHome(e: ReactEvent) = props.routeCtl.toHome
      def handleGotoAbout(e: ReactEvent) = props.routeCtl.toAbout

      def callbackPage(page: RubberPage)(e: ReactEvent) = props.routeCtl.set(page).runNow()

      <.div(
          baseStyles.divAppBar,
          BridgeAppBar(
            handleMainClick = handleMainClick _,
            maintitle =
              List[VdomNode](
                MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      "Rubber Bridge",
                    )
                )
              ),
            title = props.title,
            helpurl = props.helpurl,
            routeCtl = props.routeCtl
          )(
              // main menu
//              MyMenu(
//                  anchorEl=state.anchorMainEl,
//                  onClickAway = handleMainClose _,
//                  onItemClick = handleMainCloseClick _,
//              )(
//                MuiMenuItem(
//                    id = "FastClick",
//                    onClick = ( (e: ReactEvent) => (HomePage.fastclickToggle>>scope.forceUpdate).runNow() ),
//                    classes = js.Dictionary("root" -> "mainMenuItem").asInstanceOf[js.Object]
//
//                )(
//                    "FastClick ",
//                    MuiCheckIcon(
//                        color= (if (HomePage.isFastclickOn) SvgColor.inherit else SvgColor.disabled),
//                        classes = js.Dictionary("root" -> "mainMenuItemIcon").asInstanceOf[js.Object]
//                    )()
//                ),
//              )
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

  val component = ScalaComponent.builder[Props]("RubberPageBridgeAppBar")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

