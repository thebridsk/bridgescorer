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
      title: Seq[CtorType.ChildArg],
      helpurl: String,
      routeCtl: BridgeRouter[AppPage]
  )

  def apply(
      handleMainClick: ReactEvent=>Unit,
      title: Seq[VdomNode],
      helpurl: String,
      routeCtl: BridgeRouter[AppPage]
  )(
      mainMenu: CtorType.ChildArg*,
  ) =
    component(Props(mainMenu,handleMainClick,title,helpurl,routeCtl))()

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
      anchorHelpEl: js.UndefOr[Element] = js.undefined
  ) {

    def openHelpMenu( n: Node ) = copy( anchorHelpEl = n.asInstanceOf[Element] )
    def closeHelpMenu() = copy( anchorHelpEl = js.undefined )
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def handleHelpClick( event: ReactEvent ) = event.extract(_.currentTarget)(currentTarget => scope.modState(s => s.openHelpMenu(currentTarget)).runNow() )
    def handleHelpCloseClick( event: ReactEvent ) = scope.modState(s => s.closeHelpMenu()).runNow()
    def handleHelpClose( /* event: js.Object, reason: String */ ) = {
      logger.fine("HelpClose called")
      scope.modState(s => s.closeHelpMenu()).runNow()
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

      val helpButton =
        MuiIconButton(
                          id = "HelpMenu",
                          onClick = handleHelpClick _,
                          color=ColorVariant.inherit
                      )(
                          MuiHelpIcon()()
                      )

      val toolbarSuits =
                    MuiTypography(
                        variant = TextVariant.h6,
                        color = TextColor.inherit,
                        classes = js.Dictionary( "root" -> "homePageTitle")
                    )(
                        <.span(
                          <.span(
                              HtmlStyles.color.black,
                              rootStyles.headerSuitSize,
                              ^.dangerouslySetInnerHtml := " &spades;"
                          ),
                          <.span(
                              HtmlStyles.color.red,
                              rootStyles.headerSuitSize,
                              ^.dangerouslySetInnerHtml := " &hearts;"
                          ),
                          <.span(
                              HtmlStyles.color.red,
                              rootStyles.headerSuitSize,
                              ^.dangerouslySetInnerHtml := " &diams;"
                          ),
                          <.span(
                              HtmlStyles.color.black,
                              rootStyles.headerSuitSize,
                              ^.dangerouslySetInnerHtml := " &clubs;"
                          ),
                        )
                    )

      val toolbarContentTail = toolbarSuits::helpButton::Nil

      val toolbarFront =
                MuiIconButton(
                    id = "MainMenu",
                    onClick = props.handleMainClick,
                    color=ColorVariant.inherit
                )(
                  MuiMenuIcon()()
                ) ::
                MuiIconButton(
                    id = "Home",
                    onClick = callbackPage(Home) _,
                    color=ColorVariant.inherit
                )(
                    MuiHomeIcon()()
                ) ::
                MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      "Bridge ScoreKeeper -",
                    )
                ) ::
                Nil

      val toolbarContent = toolbarFront.map(_.vdomElement).toList ::: props.title.toList ::: toolbarContentTail.map(_.vdomElement)

      <.div(
        (
          (
            MuiAppBar(
                position=Position.static
            )(
                MuiToolbar()(
                    toolbarContent:_*
                )
            )::
            // help menu
            MyMenu(
                anchorEl=state.anchorHelpEl,
                onClickAway = handleHelpClose _,
                onItemClick = handleHelpCloseClick _,
                className = Some("popupMenu")
            )(
                MuiMenuItem(
                    id = "Help",
                    onClick = handleHelpGotoPageClick(props.helpurl) _
                )(
                    "Help"
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
                    id = "About",
                    onClick = callbackPage(About) _
                )(
                    "About"
                )
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

