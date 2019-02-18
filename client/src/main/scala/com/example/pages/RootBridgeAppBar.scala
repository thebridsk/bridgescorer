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
import com.example.materialui.icons.MuiChevronRightIcon
import com.example.materialui.icons.MuiCheckIcon
import com.example.Bridge
import com.example.materialui.PopperPlacement
import com.example.routes.AppRouter.ShowDuplicateHand
import com.example.routes.AppRouter.ShowChicagoHand
import com.example.routes.AppRouter.ShowRubberHand
import com.example.routes.AppRouter.PageTest
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
    component(Props(title,helpurl,routeCtl))

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
      userSelect: Boolean = false,
      anchorMainEl: js.UndefOr[Element] = js.undefined,
      anchorMainTestHandEl: js.UndefOr[Element] = js.undefined,
  ) {

    def openMainMenu( n: Node ) = copy( anchorMainEl = n.asInstanceOf[Element] )
    def closeMainMenu() = copy( anchorMainEl = js.undefined )

    def openMainTestHandMenu( n: Node ) = copy( anchorMainTestHandEl = n.asInstanceOf[Element] )
    def closeMainTestHandMenu() = copy( anchorMainTestHandEl = js.undefined )
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
      scope.modState { s => s.closeMainMenu() }.runNow()
    }

    def handleTestHandClick( event: ReactEvent ) = {
      event.extract{ e =>
//        e.preventDefault()
        e.currentTarget
      }(
         currentTarget =>
           scope.modState( s => s.openMainTestHandMenu(currentTarget)).runNow()
     )
    }
    def handleMainTestHandClose( /* event: js.Object, reason: String */ ) = scope.modState(s => s.closeMainTestHandMenu()).runNow()
    def handleMainTestHandCloseClick( event: ReactEvent ) = scope.modState(s => s.closeMainTestHandMenu()).runNow()

    def gotoPage( uri: String ) = {
      GotoPage.inSameWindow(uri)
    }

    def handleGotoPageClick(uri: String)( event: ReactEvent ) = {
      logger.info(s"""Going to page ${uri}""")
      handleMainClose()
      gotoPage(uri)
    }

    val toggleUserSelect = { (event: ReactEvent) => scope.withEffectsImpure.modState { s =>
      val newstate = s.copy( userSelect = !s.userSelect )
      val style = Bridge.getElement("allowSelect")
      if (newstate.userSelect) {
        style.innerHTML = """
           |* {
           |  user-select: text;
           |}
           |""".stripMargin
      } else {
        style.innerHTML = ""
      }
      newstate
    }}

    def render( props: Props, state: State ) = {
      import BaseStyles._

      def callbackPage(page: AppPage)(e: ReactEvent) = {
        logger.info(s"""Goto page $page""")
        handleMainClose()
        props.routeCtl.set(page).runNow()
      }

      val maintitle: Seq[VdomNode] =
        List(
          MuiTypography(
              variant = TextVariant.h6,
              color = TextColor.inherit,
          )(
              <.span(
                "Bridge ScoreKeeper",
              )
          )
        )

      <.div(
          baseStyles.divAppBar,
          BridgeAppBar(
              handleMainClick = handleMainClick _,
              maintitle = maintitle,
              title = props.title,
              helpurl = props.helpurl.getOrElse("../help/introduction.html"),
              routeCtl = props.routeCtl,
              showHomeButton = !props.title.isEmpty
          )(

            // main menu
            MyMenu(
                anchorEl=state.anchorMainEl,
                onClickAway = handleMainClose _,
//                onItemClick = handleMainCloseClick _,
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
                MuiMenuItem(
                    onClick = handleTestHandClick _,
                    classes = js.Dictionary("root" -> "mainMenuItem").asInstanceOf[js.Object]
                )(
                    "Test Hands",
                    MuiChevronRightIcon(
                        classes = js.Dictionary("root" -> "mainMenuItemIcon").asInstanceOf[js.Object]
                    )()
                ),
                {
                  val path = GotoPage.currentURL
                  val (newp,color) = if (path.indexOf("indexNoScale") >= 0) {
                    ("""index.html""", SvgColor.disabled)
                  } else {
                    ("""indexNoScale.html""", SvgColor.inherit)
                  }
                  val newpath = if (path.endsWith(".gz")) {
                    s"""${newp}.gz"""
                  } else {
                    newp
                  }
                  MuiMenuItem(
                      id = "NoScaling",
                      onClick = handleGotoPageClick(newp) _,
                      classes = js.Dictionary("root" -> "mainMenuItem").asInstanceOf[js.Object]

                  )(
                      "Scaling ",
                      MuiCheckIcon(
                          color=color,
                          classes = js.Dictionary("root" -> "mainMenuItemIcon").asInstanceOf[js.Object]
                      )()
                  )
                },
                MuiMenuItem(
                    id = "UserSelect",
                    onClick = toggleUserSelect,
                    classes = js.Dictionary("root" -> "mainMenuItem").asInstanceOf[js.Object]

                )(
                    "Allow Select",
                    {
                      val color = if (state.userSelect) SvgColor.inherit else SvgColor.disabled
                      MuiCheckIcon(
                          color=color,
                          classes = js.Dictionary("root" -> "mainMenuItemIcon").asInstanceOf[js.Object]
                      )()
                    }
                ),
            ),

            // test hand menu
            MyMenu(
                anchorEl=state.anchorMainTestHandEl,
                onClickAway = handleMainTestHandClose _,
                onItemClick = handleMainTestHandCloseClick _,
                placement = PopperPlacement.rightStart,
            )(
                MuiMenuItem(
                    onClick = callbackPage(ShowDuplicateHand) _
                )(
                    "Duplicate"
                ),
                MuiMenuItem(
                    onClick = callbackPage(ShowChicagoHand) _
                )(
                    "Chicago"
                ),
                MuiMenuItem(
                    onClick = callbackPage(ShowRubberHand) _
                )(
                    "Rubber"
                )
            ),
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

