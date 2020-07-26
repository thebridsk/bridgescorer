package com.github.thebridsk.bridge.client.pages.rubber

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.Node
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import japgolly.scalajs.react.vdom.VdomNode
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.client.pages.BridgeAppBar
import com.github.thebridsk.bridge.client.pages.ServerURLPopup


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
  ): TagMod = {
    TagMod(
      ServerURLPopup(),
      component(Props(mainMenuItems,title,helpurl,routeCtl))
    )
  }
}

object RubberPageBridgeAppBarInternal {
  import RubberPageBridgeAppBar._

  val logger: Logger = Logger("bridge.RubberPageBridgeAppBar")

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

    def openMainMenu( n: Node ): State = copy( anchorMainEl = n.asInstanceOf[Element] )
    def closeMainMenu(): State = copy( anchorMainEl = js.undefined )
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def handleMainClick( event: ReactEvent ): Unit = event.extract(_.currentTarget)(currentTarget => scope.modState(s => s.openMainMenu(currentTarget)).runNow() )
    def handleMainCloseClick( event: ReactEvent ): Unit = scope.modState(s => s.closeMainMenu()).runNow()
    def handleMainClose( /* event: js.Object, reason: String */ ): Unit = {
      logger.fine("MainClose called")
      scope.modState(s => s.closeMainMenu()).runNow()
    }

    def render( props: Props, state: State ) = { // scalafix:ok ExplicitResultTypes; React
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

    val didMount: Callback = Callback {
      mounted = true

    }

    val willUnmount: Callback = Callback {
      mounted = false

    }
  }

  private[rubber]
  val component = ScalaComponent.builder[Props]("RubberPageBridgeAppBar")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

