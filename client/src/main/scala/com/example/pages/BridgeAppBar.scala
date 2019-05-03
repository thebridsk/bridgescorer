package com.example.pages

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.materialui.MuiAppBar
import com.example.materialui.Position
import com.example.materialui.MuiToolbar
import com.example.materialui.MuiIconButton
import com.example.materialui.icons.MuiIcons
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
import org.scalajs.dom.experimental.URL
import com.example.Bridge
import com.example.react.Utils._
import com.example.routes.AppRouter.GraphQLAppPage
import com.example.routes.AppRouter.GraphiQLView
import com.example.routes.AppRouter.VoyagerView
import com.example.routes.AppRouter.PageTest
import com.example.routes.AppRouter.ColorView
import com.example.routes.AppRouter.LogView
import com.example.bridge.action.BridgeDispatcher
import com.example.logger.Init

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
      handleMainClick: ReactEvent=>Unit,
      maintitle: Seq[VdomNode],
      title: Seq[CtorType.ChildArg],
      helpurl: String,
      routeCtl: BridgeRouter[_],
      showHomeButton: Boolean
  )

  def apply(
      handleMainClick: ReactEvent=>Unit,
      maintitle: Seq[VdomNode],
      title: Seq[VdomNode],
      helpurl: String,
      routeCtl: BridgeRouter[_],
      showHomeButton: Boolean = true
  )(
      mainMenu: CtorType.ChildArg*,
  ) =
    component(Props(mainMenu,handleMainClick,maintitle,title,helpurl,routeCtl,showHomeButton))

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

    def openMoreMenu( n: Node ) = copy( anchorMoreEl = n.asInstanceOf[Element] )
    def closeMoreMenu() = copy( anchorMoreEl = js.undefined )
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def handleMoreClick( event: ReactEvent ) = event.extract(_.currentTarget)(currentTarget => scope.modState(s => s.openMoreMenu(currentTarget)).runNow() )
    def handleMoreCloseClick( event: ReactEvent ) = scope.modState(s => s.closeMoreMenu()).runNow()
    def handleMoreClose( /* event: js.Object, reason: String */ ) = {
      logger.fine("MoreClose called")
      scope.modState(s => s.closeMoreMenu()).runNow()
    }

    def gotoPage( uri: String ) = {
      GotoPage.inNewWindow(uri)
    }

    def handleHelpGotoPageClick(uri: String)( event: ReactEvent ) = {
      logger.info(s"""Going to page ${uri}""")
//      handleMoreClose()

      gotoPage(uri)
    }

    def startLog( event: ReactEvent ) = {
      Init.startMaybeDebugLogging(true)
      BridgeDispatcher.startLogs()
    }
    def stopLog( event: ReactEvent ) = BridgeDispatcher.stopLogs()

    def render( props: Props, state: State ) = {
      import BaseStyles._

      def gotoHomePage(e: ReactEvent) = props.routeCtl.toHome
      def gotoAboutPage(e: ReactEvent) = props.routeCtl.toAbout
      def gotoInfoPage(e: ReactEvent) = {
        logger.info("going to infopage")
        props.routeCtl.toInfo
      }

      def callbackPage(page: AppPage)(e: ReactEvent) = props.routeCtl.toRootPage(page)

      val rightButton =
        List[CtorType.ChildArg](
          MuiIconButton(
                            id = "Help",
                            onClick = handleHelpGotoPageClick(props.helpurl) _,
                            color=ColorVariant.inherit
                        )(
                            MuiIcons.Help()()
                        ),

          MuiIconButton(
                            id = "MoreMenu",
                            onClick = handleMoreClick _,
                            color=ColorVariant.inherit
                        )(
                            MuiIcons.MoreVert()()
                        )
        )

      val toolbarSuits = TitleSuits.suits

      val toolbarContentTail: List[CtorType.ChildArg] = List(
                                                          toolbarSuits,
                                                          <.div(
                                                              rightButton:_*
                                                          )
                                                        )

      val toolbarFront: List[CtorType.ChildArg] =
          {
            val demo = if (Bridge.isDemo) {
              List[TagMod](
                MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.error,
                )(
                    <.span("DEMO",<.span(^.dangerouslySetInnerHtml:="&nbsp;"))
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
                    color=ColorVariant.inherit
                )(
                  MuiIcons.Menu()()
                ),
                if (props.showHomeButton) {
                  MuiIconButton(
                      id = "Home",
                      onClick = gotoHomePage _,
                      color=ColorVariant.inherit
                  )(
                      MuiIcons.Home()()
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
                        color = TextColor.inherit,
                    )(
                        <.span(^.dangerouslySetInnerHtml:="&nbsp;-&nbsp;")
                    )
                  ) ::: props.title.toList
                })):_*
              )
            )
          }

      val toolbarContent = toolbarFront ::: toolbarContentTail

      <.div(
        (
          (
            MuiAppBar(
                position=Position.static
            )(
                MuiToolbar(
                    classes = js.Dictionary("root"->"muiToolbar")
                )(
                    toolbarContent:_*
                )
            )::
            // more menu
            MyMenu(
                anchorEl=state.anchorMoreEl,
                onClickAway = handleMoreClose _,
                onItemClick = handleMoreCloseClick _,
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
                    id = "TestPage",
                    onClick = callbackPage(PageTest) _
                )(
                    "Test Page"
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
                ),
            )::Nil
          ).map(_.vdomElement):::
          props.mainMenu.toList
        ): _*
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

  val component = ScalaComponent.builder[Props]("BridgeAppBar")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

