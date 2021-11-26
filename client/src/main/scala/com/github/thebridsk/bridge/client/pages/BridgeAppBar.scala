package com.github.thebridsk.bridge.client.pages

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.materialui.MuiAppBar
import com.github.thebridsk.materialui.Position
import com.github.thebridsk.materialui.MuiToolbar
import com.github.thebridsk.materialui.MuiIconButton
import com.github.thebridsk.materialui.icons
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.ColorVariant
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.Node
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.component.MyMenu
import com.github.thebridsk.materialui.MuiMenuItem
import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import org.scalajs.dom.document
import japgolly.scalajs.react.vdom.VdomNode
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.client.routes.AppRouter.LogView
import com.github.thebridsk.bridge.client.bridge.action.BridgeDispatcher
import com.github.thebridsk.bridge.clientcommon.pages.GotoPage
import com.github.thebridsk.bridge.clientcommon.pages.TitleSuits
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles._
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo
import com.github.thebridsk.bridge.clientcommon.debug.DebugLoggerComponent
import com.github.thebridsk.bridge.clientcommon.fullscreen.Values
import com.github.thebridsk.materialui.AnchorOrigin
import com.github.thebridsk.materialui.AnchorOriginHorizontalValue
import com.github.thebridsk.materialui.AnchorOriginVerticalValue
import com.github.thebridsk.bridge.clientcommon.component.LightDarkButton
import com.github.thebridsk.bridge.clientcommon.component.FullscreenButton
import com.github.thebridsk.bridge.clientcommon.component.ServerURLButton
import com.github.thebridsk.bridge.clientcommon.fullscreen.Fullscreen

/**
  * Base component for the AppBar for all pages.
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
  * The Main Menu button is only displayed if there is an main menu item.
  * The Home button is optionally displayed.
  * Provide popup for Server URLs.
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
  *     BridgeAppBar(
  *        handleMainClick = handleMainClick _,
  *        maintitle = List("App title"),
  *        title = props.title
  *        helpurl = props.helpurl.getOrElse("../help/introduction.html"),
  *        routeCtl = props.routeCtl,
  *     )(
  *       MyMain(
  *           anchorEl = state.anchorMainEl,
  *           onClose = handleMainClose _,
  *           anchorOrigin = AnchorOrigin(
  *              AnchorOriginHorizontalValue.left,
  *              AnchorOriginVerticalValue.bottom
  *           ),
  *           transformOrigin = AnchorOrigin(
  *              AnchorOriginHorizontalValue.left,
  *              AnchorOriginVerticalValue.top
  *           )
  *       )(
  *         MuiMenuItem(
  *           onClick = ...
  *         )( "item label" )
  *       )
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
object BridgeAppBar {
  import Internal._

  case class Props(
      mainMenu: Seq[CtorType.ChildArg],
      handleMainClick: ReactEvent => Unit,
      maintitle: Seq[VdomNode],
      title: Seq[CtorType.ChildArg],
      helpurl: String,
      routeCtl: BridgeRouter[_],
      showHomeButton: Boolean,
      showRightButtons: Boolean,
      showAPI: Boolean = false
  )

  /**
    * Instantiate the AppBar.
    *
    * @param handleMainClick  - event handler when the main button is hit.
    * This should open the mainMenu
    * @param maintitle        - the main part of the title that is always shown.
    * @param title            - the page title.  If the empty seq, then the page title is not displayed.
    * @param helpurl          - the URL that the help button will display.
    *                           The URL is relative to the page URL.
    * @param routeCtl         - the page router.
    * @param showHomeButton   - the home button is displayed.
    * @param showRightButtons - show the buttons on the right of the appbar.
    * @param showAPI          - show the swagger and graphql docs in the more menue.
    * @param mainMenu         - the menu items that are shown when the main menu button is hit.
    * If the empty sequence, the home button is not displayed.
    * This is typically a MyMenu or MuiMenu object.
    *
    * @return the unmounted react component
    *
    * @see [[BridgeAppBar]] for usage.
    */
  def apply(
      handleMainClick: ReactEvent => Unit,
      maintitle: Seq[VdomNode],
      title: Seq[VdomNode],
      helpurl: String,
      routeCtl: BridgeRouter[_],
      showHomeButton: Boolean = true,
      showRightButtons: Boolean = true,
      showAPI: Boolean = false
  )(
      mainMenu: CtorType.ChildArg*
  ) = // scalafix:ok ExplicitResultTypes; ReactComponent
    component(
      Props(
        mainMenu,
        handleMainClick,
        maintitle,
        title,
        helpurl,
        routeCtl,
        showHomeButton,
        showRightButtons,
        showAPI
      )
    )

  protected object Internal {

    val logger: Logger = Logger("bridge.BridgeAppBar")

    case class State(
        anchorMoreEl: js.UndefOr[Element] = js.undefined
    ) {

      def openMoreMenu(n: Node): State =
        copy(anchorMoreEl = n.asInstanceOf[Element])
      def closeMoreMenu(): State = copy(anchorMoreEl = js.undefined)
    }

    val apiPageURL: String = {
      val window = document.defaultView
      val curUrl = window.location.href
      if (curUrl.contains("fastopt")) "/public/index-api-fastopt.html"
      else "/public/index-api.html"
    }

    def getApiPageUrl(page: String): String = {
      val u = s"${apiPageURL}#${page}"
  //    logger.fine(s"Going to URL ${u}")
      u
    }

    val buttonStyle = js.Dictionary("root" -> "toolbarIcon")

    class Backend(scope: BackendScope[Props, State]) {

      def handleMoreClick(event: ReactEvent): Unit = {
        event.stopPropagation()
        event.extract(_.currentTarget)(currentTarget =>
          scope.modState(s => s.openMoreMenu(currentTarget)).runNow()
        )
      }
      def handleMoreCloseClick(event: ReactEvent): Unit =
        scope.modState(s => s.closeMoreMenu()).runNow()
      def handleMoreClose( /* event: js.Object, reason: String */ ): Unit = {
        logger.fine("MoreClose called")
        scope.modState(s => s.closeMoreMenu()).runNow()
      }

      def gotoPage(uri: String): Unit = {
        GotoPage.inNewWindow(uri)
      }

      def handleHelpGotoPageClick(uri: String)(event: ReactEvent): Unit = {
        logger.fine(s"""Going to page ${uri}""")
  //      handleMoreClose()

        gotoPage(uri)
      }

      def startLog(event: ReactEvent): Unit = {
        logger.fine(s"""BridgeAppBar start logging""")
        DebugLoggerComponent.init()
        scalajs.js.timers.setTimeout(1) {
          BridgeDispatcher.startLogs()
        }
      }
      def stopLog(event: ReactEvent): Unit = {
        logger.fine(s"""BridgeAppBar start logging""")
        scalajs.js.timers.setTimeout(1) {
          BridgeDispatcher.stopLogs()
        }
      }

      private def leftButtons(props: Props, state: State, isfullscreen: Boolean) = {

        def gotoHomePage(e: ReactEvent) = props.routeCtl.toHome

        <.div(
          baseStyles.appBarTitle,
          // this is to take up space on the left side of the app bar
          // when running in fullscreen mode on an iPad.
          // this is done because iOS Safari adds an "X" button on the
          // upper left of the screen.
          <.div(baseStyles.appBarTitleWhenFullscreen)
            .when(isfullscreen && Values.isIpad),
          !props.mainMenu.isEmpty ?= MuiIconButton(
            id = "MainMenu",
            onClick = props.handleMainClick,
            title = "Menu",
            color = ColorVariant.inherit,
            classes = buttonStyle
          )(
            icons.Menu()
          ),
          if (props.showHomeButton) {
            MuiIconButton(
              id = "Home",
              onClick = gotoHomePage _,
              title = "Home",
              color = ColorVariant.inherit,
              classes = buttonStyle
            )(
              icons.Home()
            )
          } else {
            TagMod.empty
          }
        )
      }

      private def title(props: Props, state: State) = {
        val demo = if (BridgeDemo.isDemo) {
          List[TagMod](
            MuiTypography(
              variant = TextVariant.h6,
              color = TextColor.error
            )(
              <.span("DEMO", <.span(^.dangerouslySetInnerHtml := "&nbsp;"))
            )
          )
        } else {
          List()
        }
        <.div(
          (demo :::
            props.maintitle.toList :::
            (if (props.title.isEmpty) {
              List()
            } else {
              List[TagMod](
                MuiTypography(
                  variant = TextVariant.h6,
                  color = TextColor.inherit
                )(
                  <.span(^.dangerouslySetInnerHtml := "&nbsp;-&nbsp;")
                )
              ) ::: props.title.toList
            })): _*
        )
      }

      private def rightButtons(props: Props, state: State, isfullscreen: Boolean) = {
        if (props.showRightButtons) {
          List[CtorType.ChildArg](
            MuiIconButton(
              id = "Help",
              onClick = handleHelpGotoPageClick(props.helpurl) _,
              title = "Help",
              color = ColorVariant.inherit,
              classes = buttonStyle
            )(
              icons.Help()
            ),
            ServerURLButton(classes = buttonStyle),
            LightDarkButton(classes = buttonStyle),
            FullscreenButton(classes = buttonStyle),
            MuiIconButton(
              id = "MoreMenu",
              onClick = handleMoreClick _,
              title = "Developer Menu",
              color = ColorVariant.inherit,
              classes = buttonStyle
            )(
              icons.MoreVert()
            )
          )
        } else {
          List[CtorType.ChildArg]()
        }
      }

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React

        def gotoAboutPage(e: ReactEvent) = props.routeCtl.toAbout
        def gotoInfoPage(e: ReactEvent) = props.routeCtl.toInfo

        def callbackPage(page: AppPage)(e: ReactEvent) =
          props.routeCtl.toRootPage(page)

        val isfullscreen = FullscreenButton.isFullscreen

        val toolbarContent: List[CtorType.ChildArg] = {
          List(
            leftButtons(props,state,isfullscreen),
            title(props,state),
            TitleSuits.suits,
            <.div(
              rightButtons(props, state, isfullscreen): _*
            )
          )
        }

        val moremenu =
          List[CtorType.ChildArg](
            MuiMenuItem(
              id = "About",
              onClick = gotoAboutPage _
            )(
              "About"
            ),
            MuiMenuItem(
              id = "Info",
              onClick = gotoInfoPage _
            )(
              "Info"
            ),
            MuiMenuItem(
              id = "Log",
              onClick = callbackPage(LogView) _
            )(
              "Show Logs"
            ),
            MuiMenuItem(
              id = "StartLog",
              onClick = startLog _
            )(
              "Start Logging"
            ),
            MuiMenuItem(
              id = "StopLog",
              onClick = stopLog _
            )(
              "Stop Logging"
            )
          ) :::
            (if (props.showAPI) {
              List[CtorType.ChildArg](
                MuiMenuItem(
                  id = "SwaggerDocs",
                  onClick = handleHelpGotoPageClick("/v1/docs") _
                )(
                  "Swagger Docs"
                ),
                MuiMenuItem(
                  id = "SwaggerDocs2",
                  onClick = handleHelpGotoPageClick("/public/apidocs.html") _
                )(
                  "Swagger API Docs"
                ),
                MuiMenuItem(
                  id = "GraphQL",
                  onClick = handleHelpGotoPageClick(
                    getApiPageUrl("graphql")
                  ) _ // callbackPage(GraphQLAppPage) _
                )(
                  "GraphQL"
                ),
                MuiMenuItem(
                  id = "GraphiQL",
                  onClick = handleHelpGotoPageClick(
                    getApiPageUrl("graphiql")
                  ) _ // callbackPage(GraphiQLView) _
                )(
                  "GraphiQL"
                ),
                MuiMenuItem(
                  id = "Voyager",
                  onClick = handleHelpGotoPageClick(
                    getApiPageUrl("voyager")
                  ) _ // callbackPage(VoyagerView) _
                )(
                  "Voyager"
                ),
                MuiMenuItem(
                  id = "Color",
                  onClick = handleHelpGotoPageClick(
                    getApiPageUrl("color")
                  ) _ // callbackPage(ColorView) _
                )(
                  "Color"
                )
              )
            } else {
              Nil
            })

        val bar = <.div(
          (
            (
              MuiAppBar(
                position = Position.static,
                classes = js.Dictionary("root" -> "muiAppBar")
              )(
                MuiToolbar(
                  classes = js.Dictionary("root" -> "muiToolbar")
                )(
                  toolbarContent: _*
                )
              ) ::
                // more menu
                MyMenu(
                  anchorEl = state.anchorMoreEl,
                  onClose = handleMoreClose _,
                  anchorOrigin = AnchorOrigin(
                    AnchorOriginHorizontalValue.right,
                    AnchorOriginVerticalValue.bottom
                  ),
                  transformOrigin = AnchorOrigin(
                    AnchorOriginHorizontalValue.right,
                    AnchorOriginVerticalValue.top
                  )
                )(
                  moremenu: _*
                ) :: Nil
            ).map(_.vdomElement) :::
              props.mainMenu.toList
          ): _*
        )

        bar
      }

      private var mounted = false

      val fullscreenCB = scope.forceUpdate

      val didMount: Callback = Callback {
        mounted = true
        Fullscreen.addListener("fullscreenchange", fullscreenCB)
      }

      val willUnmount: Callback = Callback {
        mounted = false
        Fullscreen.removeListener("fullscreenchange", fullscreenCB)
      }
    }

    val component = ScalaComponent
      .builder[Props]("BridgeAppBar")
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
