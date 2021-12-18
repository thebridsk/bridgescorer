package com.github.thebridsk.bridge.clienttest.routes

import AppRouter.AppPage
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.extra.router.Resolution
import japgolly.scalajs.react.vdom.VdomElement
import com.github.thebridsk.materialui.styles.Styles
import com.github.thebridsk.materialui.styles.Theme
import com.github.thebridsk.materialui.styles.Palette
import com.github.thebridsk.materialui.styles.MuiColor
import com.github.thebridsk.materialui.styles.PaletteColor
import com.github.thebridsk.color.Color
import com.github.thebridsk.materialui.styles.MuiThemeProvider
import com.github.thebridsk.materialui.MuiCssBaseline
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

/**
  * @author werewolf
  */
object Navigator {

  case class Props(selectedPage: Resolution[AppPage], ctrl: RouterCtl[AppPage])

  def apply(
      selectedPage: Resolution[AppPage],
      ctrl: RouterCtl[AppPage]
  ): VdomElement = {
    Internal.component(Props(selectedPage, ctrl))
  }

  val ThemeContext = React.createContext[Theme]("theme",Theme())

  protected object Internal {

    val logger: Logger = Logger("bridge.Navigator")

    case class State(
      mode: String = "light"
    ) {

      def toggleDarkMode(): State =
        copy(mode = (if (mode=="light") "dark" else "light"))

    }

    class Backend(scope: BackendScope[Props, State]) {

      def toggleLightDark(event: ReactEvent): Unit = {
        logger.fine("toggleLightDark called")
        scope.modState(s => s.toggleDarkMode()).runNow()
      }

      def refresh(event: ReactEvent): Unit =
        scope.forceUpdate.runNow()

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        val p = props.selectedPage.render()

        val theme = Styles.createTheme(
          Theme(
            palette = Palette(
              primary = MuiColor.red,
              secondary = PaletteColor(main = Color.rgb(64, 128, 64)),
              mode = state.mode,
            ),
          )
        )

        ThemeContext.provide(theme)(
          MuiThemeProvider(theme)(
            MuiCssBaseline(
              enableColorScheme = true
            )(
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
                    color = ColorVariant.inherit
                  )(
                    Menu()
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
                  )
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
