package com.github.thebridsk.bridge.client.pages

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.materialui.MuiAppBar
import com.github.thebridsk.materialui.Position
import com.github.thebridsk.materialui.MuiToolbar
import com.github.thebridsk.materialui.icons.MuiIcons
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.ColorVariant
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.Node
import com.github.thebridsk.utilities.logging.Logger
import japgolly.scalajs.react.vdom.HtmlStyles
import com.github.thebridsk.materialui.component.MyMenu
import com.github.thebridsk.materialui.MuiMenuItem
import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.bridge.client.routes.AppRouter.About
import com.github.thebridsk.bridge.clientcommon.react.AppButtonLinkNewWindow
import org.scalajs.dom.document
import japgolly.scalajs.react.vdom.VdomNode
import com.github.thebridsk.bridge.client.routes.AppRouter.Home
import com.github.thebridsk.bridge.client.routes.AppRouter.Info
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoModule.PlayChicago2
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoRouter.{ ListView => ChicagoListView }
import com.github.thebridsk.bridge.client.pages.rubber.RubberRouter.{ ListView => RubberListView }
import com.github.thebridsk.bridge.client.pages.rubber.RubberModule.PlayRubber
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateModule.PlayDuplicate
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.SummaryView
import com.github.thebridsk.bridge.client.Bridge
import com.github.thebridsk.materialui.PopperPlacement
import com.github.thebridsk.bridge.client.routes.AppRouter.ShowDuplicateHand
import com.github.thebridsk.bridge.client.routes.AppRouter.ShowChicagoHand
import com.github.thebridsk.bridge.client.routes.AppRouter.ShowRubberHand
import com.github.thebridsk.materialui.icons.SvgColor
import com.github.thebridsk.bridge.clientcommon.pages.GotoPage

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
  )() = {
    TagMod(
      ServerURLPopup(),
      component(Props(title,helpurl,routeCtl))
    )
  }
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
                MuiMenuItem(
                    onClick = handleTestHandClick _,
                    classes = js.Dictionary("root" -> "mainMenuItem")
                )(
                    "Test Hands",
                    MuiIcons.ChevronRight(
                        classes = js.Dictionary("root" -> "mainMenuItemIcon")
                    )
                ),
                {
                  val path = GotoPage.currentURL
                  val (newp,color,check) = if (path.indexOf("indexNoScale") >= 0) {
                    ("""index.html""", SvgColor.disabled, false)
                  } else {
                    ("""indexNoScale.html""", SvgColor.inherit, true)
                  }
                  val newpath = if (path.endsWith(".gz")) {
                    s"""${newp}.gz"""
                  } else {
                    newp
                  }
                  MuiMenuItem(
                      id = "NoScaling",
                      onClick = handleGotoPageClick(newp) _,
                      classes = js.Dictionary("root" -> "mainMenuItem")

                  )(
                      "Scaling ",
//                      MuiIcons.Check(
//                          color=color,
//                          classes = js.Dictionary("root" -> "mainMenuItemIcon")
//                      )
                      if (check) {
                        MuiIcons.CheckBox()
                      } else {
                        MuiIcons.CheckBoxOutlineBlank()
                      }
                  )
                },
                MuiMenuItem(
                    id = "UserSelect",
                    onClick = toggleUserSelect,
                    classes = js.Dictionary("root" -> "mainMenuItem")

                )(
                    "Allow Select",
                    {
//                      val color = if (state.userSelect) SvgColor.inherit else SvgColor.disabled
//                      MuiIcons.Check(
//                          color=color,
//                          classes = js.Dictionary("root" -> "mainMenuItemIcon")
//                      )
                      if (state.userSelect) {
                        MuiIcons.CheckBox()
                      } else {
                        MuiIcons.CheckBoxOutlineBlank()
                      }
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
