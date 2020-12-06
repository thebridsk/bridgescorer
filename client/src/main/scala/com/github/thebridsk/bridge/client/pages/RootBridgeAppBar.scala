package com.github.thebridsk.bridge.client.pages

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.materialui.icons
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.Node
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.materialui.component.MyMenu
import com.github.thebridsk.materialui.MuiMenuItem
import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import japgolly.scalajs.react.vdom.VdomNode
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoModule.PlayChicago2
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoRouter.{
  ListView => ChicagoListView
}
import com.github.thebridsk.bridge.client.pages.rubber.RubberRouter.{
  ListView => RubberListView
}
import com.github.thebridsk.bridge.client.pages.rubber.RubberModule.PlayRubber
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateModule.PlayDuplicate
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.SummaryView
import com.github.thebridsk.bridge.client.routes.AppRouter.ShowDuplicateHand
import com.github.thebridsk.bridge.client.routes.AppRouter.ShowChicagoHand
import com.github.thebridsk.bridge.client.routes.AppRouter.ShowRubberHand
import com.github.thebridsk.bridge.clientcommon.pages.GotoPage
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles._
import japgolly.scalajs.react.internal.Effect
import com.github.thebridsk.materialui.AnchorOrigin
import com.github.thebridsk.materialui.AnchorOriginHorizontalValue
import com.github.thebridsk.materialui.AnchorOriginVerticalValue
import com.github.thebridsk.bridge.clientcommon.logger.Info

/**
  * The AppBar for all root pages.
  *
  * The AppBar has in the banner from left to right:
  *
  * 1. Left Buttons
  *   - Main Menu button
  *   - Home button
  * 2. Title
  * 3. Logo
  * 4. Right Buttons
  *   - Help button
  *   - Show Server URL
  *   - Dark mode selector
  *   - Fullscreen
  *   - More menu
  *
  * The more menu contains:
  * - About and info pages
  * - Debug logging pages
  * - Swagger doc pages
  * - GraphQL pages
  *
  * The Title is made up of two parts, the app title and the page title.
  * If both are given, they will be separated by a {{{" - "}}}.
  * A Main Menu is provided by this component.
  * The Home button is displayed if a page title is given.
  *
  * To use, just code the following:
  *
  * {{{
  * case class Props(
  *     title: Seq[VdomNode],
  *     helpurl: Option[String],
  *     routeCtl: BridgeRouter[AppPage],
  * )
  *
  * case class State(
  *     anchorMainEl: js.UndefOr[Element] = js.undefined,
  * }
  *
  * class Backend(scope: BackendScope[Props, State]) {
  *
  *   def handleMainClick(event: ReactEvent): Unit = {
  *     event.stopPropagation()
  *     event.extract(_.currentTarget)(currentTarget =>
  *       scope.modState(s => s.copy(anchorMainEl = currentTarget)).runNow()
  *     )
  *   }
  *   def handleMainClose( /* event: js.Object, reason: String */ ): Unit = {
  *     scope.modState(s => s.copy(anchorMainEl = js.undefined)).runNow()
  *   }
  *
  *   def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
  *     RootBridgeAppBar(
  *        title = props.title
  *        helpurl = props.helpurl.getOrElse("../help/introduction.html"),
  *        routeCtl = props.routeCtl,
  *     )(
  *     )
  *   }
  * }
  * }}}
  *
  * Note that if an item is clicked on the menu, the menu will not automatically close.
  * The onClick handler on the menu item must close the menu.
  *
  * @see See [[apply]] for the description of the arguments to instantiate the component.
  *
  * @author werewolf
  */
object RootBridgeAppBar {
  import Internal._

  case class Props(
      title: Seq[VdomNode],
      helpurl: Option[String],
      routeCtl: BridgeRouter[AppPage],
      showRightButtons: Boolean,
      showMainMenu: Boolean,
      showAPI: Boolean = false
  )

  /**
    * Instantiate the component.
    *
    * @param title - The page title.  Specify js.undefined for the Home page.
    * @param helpurl          - the URL that the help button will display.
    * The URL is relative to the page URL.
    * @param routeCtl         - the page router.
    * @param showRightButtons - show the buttons on the right of the appbar, default is true.
    * @param showMainMenu     - show the main menu, default is true.
    * @param showAPI          - show the swagger and graphql docs in the more menue, default is false.
    * @return the unmounted react component
    *
    * @see [[RootBridgeAppBar]] for usage.
    */
  def apply(
      title: Seq[VdomNode],
      helpurl: Option[String],
      routeCtl: BridgeRouter[AppPage],
      showRightButtons: Boolean = true,
      showMainMenu: Boolean = true,
      showAPI: Boolean = false
  )() = { // scalafix:ok ExplicitResultTypes; ReactComponent
    component(
      Props(title, helpurl, routeCtl, showRightButtons, showMainMenu, showAPI)
    )
  }

  protected object Internal {

    val logger: Logger = Logger("bridge.RootBridgeAppBar")

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause State to leak.
      */
    case class State(
        userSelect: Boolean = false,
        anchorMainEl: js.UndefOr[Element] = js.undefined,
        anchorMainTestHandEl: js.UndefOr[Element] = js.undefined,
    ) {

      def openMainMenu(n: Node): State =
        copy(anchorMainEl = n.asInstanceOf[Element])
      def closeMainMenu(): State = copy(anchorMainEl = js.undefined)

      def openMainTestHandMenu(n: Node): State =
        copy(anchorMainTestHandEl = n.asInstanceOf[Element])
      def closeMainTestHandMenu(): State =
        copy(anchorMainTestHandEl = js.undefined)

    }

    private val metaViewportScaling = "width=device-width"
    private val metaViewportNoScaling = "width=device-width, user-scalable=no, initial-scale=1"

    /**
      * <meta
      *   id="metaViewport"
      *   name="viewport"
      *   content="width=device-width, user-scalable=no, initial-scale=1"
      * >
      */
    private def getViewport = {
      Info.getElement("metaViewport")
    }

    def isScaling: Boolean = {
      getViewport.getAttribute("content") == metaViewportScaling
    }

    def setScaling(flag: Boolean): Unit = {
      getViewport.setAttribute(
        "content",
        if (flag) metaViewportScaling
        else metaViewportNoScaling
      )
    }

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause Backend to leak.
      */
    class Backend(scope: BackendScope[Props, State]) {

      def handleScaling(flag: Boolean)(event: ReactEvent): Unit = {
        setScaling(flag)
        handleMainClose()
      }

      def handleMainClick(event: ReactEvent): Unit = {
        event.stopPropagation()
        event.extract(_.currentTarget)(currentTarget =>
          scope.modState(s => s.openMainMenu(currentTarget)).runNow()
        )
      }
      def handleMainCloseClick(event: ReactEvent): Unit =
        scope.modState(s => s.closeMainMenu()).runNow()
      def handleMainClose( /* event: js.Object, reason: String */ ): Unit = {
        logger.fine("MainClose called")
        scope
          .modState { s =>
            s.closeMainMenu()
          }
          .runNow()
      }

      def handleTestHandClick(event: ReactEvent): Unit = {
        event.stopPropagation()
        event.extract { e =>
          e.currentTarget
        }(currentTarget =>
          scope.modState(s => s.openMainTestHandMenu(currentTarget)).runNow()
        )
      }
      def handleMainTestHandClose(
          /* event: js.Object, reason: String */
      ): Unit = scope.modState(s => s.closeMainTestHandMenu()).runNow()
      def handleMainTestHandCloseClick(event: ReactEvent): Unit =
        scope.modState(s => s.closeMainTestHandMenu()).runNow()

      def gotoPage(uri: String): Unit = {
        GotoPage.inSameWindow(uri)
      }

      def handleGotoPageClick(uri: String)(event: ReactEvent): Unit = {
        logger.info(s"""Going to page ${uri}""")
        handleMainClose()
        gotoPage(uri)
      }

      val toggleUserSelect: ReactEvent => Effect.Id[Unit] = {
        (event: ReactEvent) =>
          scope.withEffectsImpure.modState { s =>
            val newstate = s.copy(userSelect = !s.userSelect)
            val style = Info.getElement("allowSelect")
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
          }
      }

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React

        def callbackPage(page: AppPage)(e: ReactEvent) = {
          logger.info(s"""Goto page $page""")
          handleMainClose()
          props.routeCtl.set(page).runNow()
        }

        val maintitle: Seq[VdomNode] =
          List(
            MuiTypography(
              variant = TextVariant.h6,
              color = TextColor.inherit
            )(
              <.span(
                "Bridge ScoreKeeper"
              )
            )
          )

        val mainMenu: List[CtorType.ChildArg] = if (props.showMainMenu) {
          List(
            // main menu
            MyMenu(
              anchorEl = state.anchorMainEl,
              onClose = handleMainClose _,
              anchorOrigin = AnchorOrigin(
                AnchorOriginHorizontalValue.left,
                AnchorOriginVerticalValue.bottom
              ),
              transformOrigin = AnchorOrigin(
                AnchorOriginHorizontalValue.left,
                AnchorOriginVerticalValue.top
              )
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
                icons.ChevronRight(
                  classes = js.Dictionary("root" -> "mainMenuItemIcon")
                )
              ),
              {
                val check = isScaling
                MuiMenuItem(
                  id = "Scaling",
                  onClick = handleScaling(!check) _,
                  classes = js.Dictionary("root" -> "mainMenuItem")
                )(
                  "Scaling ",
                  if (check) {
                    icons.CheckBox()
                  } else {
                    icons.CheckBoxOutlineBlank()
                  }
                )
              },
              MuiMenuItem(
                id = "UserSelect",
                onClick = toggleUserSelect,
                classes = js.Dictionary("root" -> "mainMenuItem")
              )(
                "Allow Select", {
  //                      val color = if (state.userSelect) SvgColor.inherit else SvgColor.disabled
  //                      icons.Check(
  //                          color=color,
  //                          classes = js.Dictionary("root" -> "mainMenuItemIcon")
  //                      )
                  if (state.userSelect) {
                    icons.CheckBox()
                  } else {
                    icons.CheckBoxOutlineBlank()
                  }
                }
              )
            ),
            // test hand menu
            MyMenu(
              anchorEl = state.anchorMainTestHandEl,
              onClose = handleMainTestHandClose _,
              anchorOrigin = AnchorOrigin(
                AnchorOriginHorizontalValue.right,
                AnchorOriginVerticalValue.top
              ),
              transformOrigin = AnchorOrigin(
                AnchorOriginHorizontalValue.left,
                AnchorOriginVerticalValue.top
              )
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
            )
          )
        } else {
          List()
        }

        <.div(
          baseStyles.divAppBar,
          BridgeAppBar(
            handleMainClick = handleMainClick _,
            maintitle = maintitle,
            title = props.title,
            helpurl = props.helpurl.getOrElse("../help/introduction.html"),
            routeCtl = props.routeCtl,
            showHomeButton = !props.title.isEmpty,
            showRightButtons = props.showRightButtons,
            showAPI = props.showAPI
          )(
            mainMenu: _*
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

    private[pages] val component = ScalaComponent
      .builder[Props]("RootBridgeAppBar")
      .initialStateFromProps { props =>
        State()
      }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .build
  }

}
