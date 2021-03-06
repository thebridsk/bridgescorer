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
import com.github.thebridsk.materialui.component.MyMenu
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
import com.github.thebridsk.bridge.clientcommon.material.icons.LightDark
import com.github.thebridsk.bridge.clientcommon.pages.ColorThemeStorage
import com.github.thebridsk.bridge.clientcommon.debug.DebugLoggerComponent
import com.github.thebridsk.bridge.clientcommon.fullscreen.Values
import com.github.thebridsk.bridge.clientcommon.fullscreen.Fullscreen

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
  * BridgeAppBar( BridgeAppBar.Props( ... ) )
  * </code></pre>
  *
  * @author werewolf
  */
object BridgeAppBar {
  import BridgeAppBarInternal._

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

}

object BridgeAppBarInternal {
  import BridgeAppBar._

  val logger: Logger = Logger("bridge.BridgeAppBar")

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause State to leak.
    */
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

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause Backend to leak.
    */
  class Backend(scope: BackendScope[Props, State]) {

    def handleMoreClick(event: ReactEvent): Unit =
      event.extract(_.currentTarget)(currentTarget =>
        scope.modState(s => s.openMoreMenu(currentTarget)).runNow()
      )
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

    def serverUrlClick(event: ReactEvent): Unit = {
      logger.fine("Requesting to show server URL popup")
      ServerURLPopup.setShowServerURLPopup(true)
    }

    // data-theme="dark"
    def toggleLightDark(event: ReactEvent): Unit = {
      logger.fine("toggle light dark")
      val ntheme = ColorThemeStorage.getColorTheme() match {
        case Some(curtheme) =>
          LightDark.nextTheme(curtheme)
        case None =>
          "medium"
      }
      ColorThemeStorage.setColorTheme(ntheme)
    }

    def isFullscreenEnabledI: Boolean = {
      import com.github.thebridsk.bridge.clientcommon.fullscreen.Implicits._
      val doc = document
      logger.info(s"browser fullscreenEnabled: ${doc.myFullscreenEnabled}")
      val e = doc.myFullscreenEnabled
      if (!e) {
        logger.info("fullscreenEnabled = false")
      }
      e
    }

    def isFullscreen: Boolean = {
      import com.github.thebridsk.bridge.clientcommon.fullscreen.Implicits._
      val doc = document
      logger.info(s"browser fullscreenEnabled: ${doc.myFullscreenEnabled}")
      if (isFullscreenEnabledI) {
        val fe = doc.myFullscreenElement
        val r = !js.isUndefined(fe) && fe != null
        logger.fine(s"browser isfullscreen: $r")
        if (r) {
          val elem = doc.myFullscreenElement
          logger.info(s"browser fullscreen element is ${elem.nodeName}")
        }
        r
      } else {
        false
      }
    }

    def toggleFullscreen(event: ReactEvent): Unit = {
      import com.github.thebridsk.bridge.clientcommon.fullscreen.Implicits._
      val body = document.documentElement
      val doc = document
      if (isFullscreenEnabled) {
        val isfullscreen = isFullscreen
        if (isfullscreen) {
          logger.fine(s"browser exiting fullscreen")
          doc.myExitFullscreen()
        } else {
          logger.fine(s"browser requesting fullscreen on body")
          body.requestFullscreen()
        }
        scalajs.js.timers.setTimeout(500) {
          scope.withEffectsImpure.forceUpdate
        }
      } else {
        logger.info(s"fullscreen is disabled")
      }
    }

    def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React

      def gotoHomePage(e: ReactEvent) = props.routeCtl.toHome
      def gotoAboutPage(e: ReactEvent) = props.routeCtl.toAbout
      def gotoInfoPage(e: ReactEvent) = {
        logger.fine("going to infopage")
        props.routeCtl.toInfo
      }

      def callbackPage(page: AppPage)(e: ReactEvent) =
        props.routeCtl.toRootPage(page)

      val buttonStyle = js.Dictionary("root" -> "toolbarIcon")

      val fullscreenEnabled = isFullscreenEnabledI
      val isfullscreen = isFullscreen

      val rightButton = if (props.showRightButtons) {
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
          MuiIconButton(
            id = "ServerURL",
            onClick = serverUrlClick _,
            title = "Show server URLs",
            color = ColorVariant.inherit,
            classes = buttonStyle
          )(
            icons.Place()
          ),
          MuiIconButton(
            id = "LightDark",
            onClick = toggleLightDark _,
            title = "Change color mode",
            color = ColorVariant.inherit,
            classes = buttonStyle
          )(
            LightDark()
          ),
          MuiIconButton(
            id = "Fullscreen",
            onClick = toggleFullscreen _,
            title = if (isfullscreen) "Exit fullscreen" else "Go to fullscreen",
            color = ColorVariant.inherit,
            classes = buttonStyle
          )(
            if (isfullscreen) {
              icons.FullscreenExit()
            } else {
              icons.Fullscreen()
            }
          ),
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

      val toolbarSuits = TitleSuits.suits

      val toolbarContentTail: List[CtorType.ChildArg] = List(
        toolbarSuits,
        <.div(
          rightButton: _*
        )
      )

      val toolbarFront: List[CtorType.ChildArg] = {
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
        List(
          <.div(
            baseStyles.appBarTitle,
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
          ),
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
        )
      }

      val toolbarContent = toolbarFront ::: toolbarContentTail

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
                onClickAway = handleMoreClose _,
                onItemClick = handleMoreCloseClick _
              )(
                moremenu: _*
              ) :: Nil
          ).map(_.vdomElement) :::
            props.mainMenu.toList
        ): _*
      )

      bar
    }

    val fullscreenCB = scope.forceUpdate

    private var mounted = false

    val didMount: Callback = Callback {
      mounted = true
      Fullscreen.addListener("fullscreenchange", fullscreenCB)
    }

    val willUnmount: Callback = Callback {
      mounted = false
      Fullscreen.removeListener("fullscreenchange", fullscreenCB)
    }
  }

  private[pages] val component = ScalaComponent
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
