package com.github.thebridsk.bridge.clienttest.routes

import AppRouter.AppPage
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.extra.router.Resolution
import japgolly.scalajs.react.vdom.VdomElement
import com.github.thebridsk.materialui.MuiAppBar
import com.github.thebridsk.materialui.Position
import com.github.thebridsk.materialui.ColorVariant
import com.github.thebridsk.materialui.MuiToolbar
import com.github.thebridsk.materialui.MuiIconButton
import com.github.thebridsk.materialui.ItemSize
import com.github.thebridsk.materialui.ItemEdge
import com.github.thebridsk.materialui.icons.Menu
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import scala.scalajs.js
import com.github.thebridsk.materialui.styles.StyleImplicits._
import com.github.thebridsk.bridge.clientcommon.pages.TitleSuits
import com.github.thebridsk.materialui.icons.Brightness7
import com.github.thebridsk.materialui.icons.Brightness4
import com.github.thebridsk.materialui.icons.Refresh
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.materialui.MuiBox
import com.github.thebridsk.bridge.clienttest.store.AppRoot
import com.github.thebridsk.materialui.MuiMenu
import org.scalajs.dom.Element
import org.scalajs.dom.Node
import com.github.thebridsk.materialui.MuiMenuItem
import com.github.thebridsk.materialui.icons.MoreVert

/**
  * @author werewolf
  */
object Navigator {

  case class Props(selectedPage: Resolution[AppPage], routeCtl: BridgeRouter[AppPage])

  def apply(
      selectedPage: Resolution[AppPage],
      routeCtl: BridgeRouter[AppPage]
  ): VdomElement = {
    Internal.component(Props(selectedPage, routeCtl))
  }

  // val ThemeContext = React.createContext[Theme]("theme",Theme())

  protected object Internal {

    val logger: Logger = Logger("bridge.Navigator")

    case class State(
      mode: String = "light",
      anchorMainEl: js.UndefOr[Element] = js.undefined,
      anchorMoreEl: js.UndefOr[Element] = js.undefined
    ) {

      def toggleDarkMode(): State =
        copy(mode = (if (mode=="light") "dark" else "light"))

      def openMainMenu(n: Node): State =
        copy(anchorMainEl = n.asInstanceOf[Element])
      def closeMainMenu(): State = copy(anchorMainEl = js.undefined)

      def openMoreMenu(n: Node): State =
        copy(anchorMoreEl = n.asInstanceOf[Element])
      def closeMoreMenu(): State = copy(anchorMoreEl = js.undefined)
    }

    class Backend(scope: BackendScope[Props, State]) {

      def toggleLightDark(event: ReactEvent): Unit = {
        logger.fine("toggleLightDark called")
        scope.modState(s => s.toggleDarkMode()).runNow()
      }

      def refresh(event: ReactEvent): Unit =
        scope.forceUpdate.runNow()

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        AppRoot(
          page(props,state)
        )
      }

      def handleMainClick(event: ReactEvent): Unit = {
        event.stopPropagation()
        event.extract(_.currentTarget)(currentTarget =>
          scope.modState(s => s.openMainMenu(currentTarget)).runNow()
        )
      }
      def handleMainClose(/*event: ReactEvent*/): Unit =
        scope.modState(s => s.closeMainMenu()).runNow()

      def handleMoreClick(event: ReactEvent): Unit = {
        event.stopPropagation()
        event.extract(_.currentTarget)(currentTarget =>
          scope.modState(s => s.openMoreMenu(currentTarget)).runNow()
        )
      }
      def handleMoreClose( /* event: js.Object, reason: String */ ): Unit = {
        logger.fine("MoreClose called")
        scope.modState(s => s.closeMoreMenu()).runNow()
      }

      def page(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        val p = props.selectedPage.render()
        val appPage = props.selectedPage.page

        def gotoAboutPage(e: ReactEvent) = props.routeCtl.toAbout
        def gotoInfoPage(e: ReactEvent) = props.routeCtl.toInfo

        def callbackPage(page: AppPage)(e: ReactEvent) =
          props.routeCtl.toRootPage(page)

        <.div(
          MuiAppBar(
            position = Position.sticky,
            color = ColorVariant.secondary,
            enableColorOnDark = true,
            sx = js.Dictionary(
              "height" -> "50px",
              "justifyContent" -> "center"
            )
          )(
            MuiToolbar()(
              MuiIconButton(
                size = ItemSize.large,
                edge = ItemEdge.start,
                color = ColorVariant.inherit,
                onClick = handleMainClick _
              )(
                Menu()
              ),
              MuiMenu(
                anchorEl = state.anchorMainEl,
                open = state.anchorMainEl.isDefined,
                onClose = handleMainClose _,
              )(
                MuiMenuItem(
                  id = "Duplicate",
                  onClick = callbackPage(AppRouter.Home) _
                )(
                  "Duplicate"
                ),
              ),

              MuiTypography(
                variant = TextVariant.h6,
                component = "div",
                color = TextColor.inherit,
                sx = js.Dictionary(
                  "flexGrow" -> 1
                )
              )(
                "Bridge"
              ),
              MuiTypography(
                variant = TextVariant.h2,
                color = TextColor.inherit,
                sx = js.Dictionary(
                  "textShadow" -> "-1px 0px 0 #fff, 1px 0px 0 #fff, 0px 1px 0 #fff, 0px -1px 0 #fff",
                  "fontSize" -> "2.5rem"
                )
              )(
                TitleSuits.suitspan
              ),
              MuiIconButton(
                onClick = toggleLightDark _,
                color = ColorVariant.inherit
              )(
                if (state.mode == "dark") Brightness7()
                else Brightness4()
              ),
              MuiIconButton(
                onClick = refresh _,
                color = ColorVariant.inherit
              )(
                Refresh()
              ),
              MuiIconButton(
                size = ItemSize.large,
                edge = ItemEdge.start,
                color = ColorVariant.inherit,
                onClick = handleMoreClick _
              )(
                MoreVert()
              ),
              MuiMenu(
                anchorEl = state.anchorMoreEl,
                open = state.anchorMoreEl.isDefined,
                onClose = handleMoreClose _,
              )(
                MuiMenuItem(
                  id = "Duplicate",
                  onClick = callbackPage(AppRouter.Home) _
                )(
                  "Duplicate"
                ),
              ),
            )
          ),
          MuiBox(
            sx = js.Dictionary(
              "padding" -> "8px",
              "overflow" -> "auto"
            )
          )(
            p
          )
        )
      }
    }

    val component = ScalaComponent
      .builder[Props]("Navigator")
      .initialStateFromProps(props => new State)
      .backend(backendScope => new Backend(backendScope))
      .renderBackend
      .build

  }

}
