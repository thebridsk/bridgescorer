package com.github.thebridsk.bridge.clientapi.pages

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.materialui.MuiAppBar
import com.github.thebridsk.materialui.Position
import com.github.thebridsk.materialui.MuiToolbar
import com.github.thebridsk.materialui.MuiIconButton
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
import com.github.thebridsk.bridge.clientapi.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.clientapi.routes.BridgeRouter
import com.github.thebridsk.bridge.clientapi.routes.AppRouter.About
import com.github.thebridsk.bridge.clientcommon.react.AppButtonLinkNewWindow
import org.scalajs.dom.document
import japgolly.scalajs.react.vdom.VdomNode
import com.github.thebridsk.bridge.clientapi.routes.AppRouter.Home
import org.scalajs.dom.experimental.URL
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.bridge.clientapi.routes.AppRouter.GraphQLAppPage
import com.github.thebridsk.bridge.clientapi.routes.AppRouter.GraphiQLView
import com.github.thebridsk.bridge.clientapi.routes.AppRouter.VoyagerView
import com.github.thebridsk.bridge.clientapi.routes.AppRouter.PageTest
import com.github.thebridsk.bridge.clientapi.routes.AppRouter.ColorView
import com.github.thebridsk.bridge.clientapi.routes.AppRouter.LogView
import com.github.thebridsk.bridge.clientcommon.dispatcher.Dispatcher
import com.github.thebridsk.bridge.clientcommon.logger.Init
import com.github.thebridsk.materialui.MuiButton
import com.github.thebridsk.bridge.clientcommon.pages.TitleSuits

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
      showHomeButton: Boolean
  )

  def apply(
      handleMainClick: ReactEvent => Unit,
      maintitle: Seq[VdomNode],
      title: Seq[VdomNode],
      helpurl: String,
      routeCtl: BridgeRouter[_],
      showHomeButton: Boolean = true
  )(
      mainMenu: CtorType.ChildArg*
  ) =
    component(
      Props(
        mainMenu,
        handleMainClick,
        maintitle,
        title,
        helpurl,
        routeCtl,
        showHomeButton
      )
    )

}

object BridgeAppBarInternal {
  import BridgeAppBar._

  val logger = Logger("bridge.BridgeAppBar")

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause State to leak.
    *
    */
  case class State(
      anchorMoreEl: js.UndefOr[Element] = js.undefined
  ) {

    def openMoreMenu(n: Node) = copy(anchorMoreEl = n.asInstanceOf[Element])
    def closeMoreMenu() = copy(anchorMoreEl = js.undefined)
  }

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause Backend to leak.
    *
    */
  class Backend(scope: BackendScope[Props, State]) {

    def handleMoreClick(event: ReactEvent) =
      event.extract(_.currentTarget)(
        currentTarget =>
          scope.modState(s => s.openMoreMenu(currentTarget)).runNow()
      )
    def handleMoreCloseClick(event: ReactEvent) =
      scope.modState(s => s.closeMoreMenu()).runNow()
    def handleMoreClose( /* event: js.Object, reason: String */ ) = {
      logger.fine("MoreClose called")
      scope.modState(s => s.closeMoreMenu()).runNow()
    }

    def gotoPage(uri: String) = {
      GotoPage.inNewWindow(uri)
    }

    def handleHelpGotoPageClick(uri: String)(event: ReactEvent) = {
      logger.info(s"""Going to page ${uri}""")
//      handleMoreClose()

      gotoPage(uri)
    }

    def startLog(event: ReactEvent) = {
      Init.startMaybeDebugLogging(true)
      Dispatcher.startLogs()
    }
    def stopLog(event: ReactEvent) = Dispatcher.stopLogs()

    def serverUrlClick(event: ReactEvent) = {
      logger.info("Requesting to show server URL popup")
      ServerURLPopup.setShowServerURLPopup(true)
    }

    def render(props: Props, state: State) = {
      import BaseStyles._

      def gotoHomePage(e: ReactEvent) = props.routeCtl.toHome
      def gotoAboutPage(e: ReactEvent) = props.routeCtl.toAbout
      def gotoInfoPage(e: ReactEvent) = {
        logger.info("going to infopage")
        props.routeCtl.toInfo
      }

      def callbackPage(page: AppPage)(e: ReactEvent) =
        props.routeCtl.toRootPage(page)

      val rightButton =
        List[CtorType.ChildArg](
          MuiIconButton(
            id = "Help",
            onClick = handleHelpGotoPageClick(props.helpurl) _,
            title = "Help",
            color = ColorVariant.inherit
          )(
            MuiIcons.Help()
          ),
          MuiIconButton(
            id = "ServerURL",
            onClick = serverUrlClick _,
            title = "Show server URLs",
            color = ColorVariant.inherit
          )(
            MuiIcons.Place()
            // MuiTypography(
            //   variant = TextVariant.h6,
            // )(
            //   "Server"
            // )
          ),
          MuiIconButton(
            id = "MoreMenu",
            onClick = handleMoreClick _,
            title = "Developer Menu",
            color = ColorVariant.inherit
          )(
            MuiIcons.MoreVert()
          )
        )

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
            !props.mainMenu.isEmpty ?= MuiIconButton(
              id = "MainMenu",
              onClick = props.handleMainClick,
              title = "Menu",
              color = ColorVariant.inherit
            )(
              MuiIcons.Menu()
            ),
            if (props.showHomeButton) {
              MuiIconButton(
                id = "Home",
                onClick = gotoHomePage _,
                title = "Home",
                color = ColorVariant.inherit
              )(
                MuiIcons.Home()
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

      val bar = <.div(
        (
          (
            MuiAppBar(
              position = Position.static
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
                MuiMenuItem(
                  id = "About",
                  onClick = gotoAboutPage _
                )(
                  "About"
                ),
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
                  id = "Info",
                  onClick = gotoInfoPage _
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
              ) :: Nil
          ).map(_.vdomElement) :::
            props.mainMenu.toList
        ): _*
      )

      bar
    }

    private var mounted = false

    val didMount = Callback {
      mounted = true

    }

    val willUnmount = Callback {
      mounted = false

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
