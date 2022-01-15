package com.github.thebridsk.bridge.clienttest.store

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.materialui.styles.Styles
import com.github.thebridsk.materialui.styles.Theme
import com.github.thebridsk.materialui.styles.Palette
import com.github.thebridsk.materialui.styles.MuiColor
import com.github.thebridsk.materialui.styles.PaletteColor
import com.github.thebridsk.materialui.styles.StyleImplicits._
import com.github.thebridsk.materialui.styles.MuiThemeProvider
import com.github.thebridsk.materialui.MuiCssBaseline
import com.github.thebridsk.bridge.clienttest.routes.StateProvider
import com.github.thebridsk.bridge.clientcommon.javascript.ObjectToString
import com.github.thebridsk.color.Color

object AppRoot {
  import Internal._

  @js.native
  trait Props extends js.Object {
    val base: BaseState = js.native
  }

  private val emptyProps = js.Dictionary("xxx" -> "emptyProps").asInstanceOf[Props]

  private val mapStateToProps: js.Function1[AppState, js.Object] = state => {
    val p = js.Dictionary("base" -> state.base)
    p.asInstanceOf[js.Object]
  }

  def apply(
    child: CtorType.ChildArg
  ) = {
    val c = component.toJsComponent.raw.asInstanceOf[js.Function1[Props,facade.React.ComponentClassP[Props]]]
    logger.info(s"AppRoot c=${ObjectToString.anyToString(c)}")
    val x = StateProvider.connectWithChildren[AppState,Props](
      mapStateToProps
    )(
      c
      // (p: Props) => {
      //   logger.info(s"in connectWithComponent: p=${ObjectToString.anyToString(p)} child=${child}")
      //   (c: CtorType.ChildArg) => component(p)(c).rawElement
      // }
    )
    // x(emptyProps)(child)
    x(emptyProps)(child)
  }

  val ThemeContext = React.createContext[Theme]("theme",Theme())

  protected object Internal {

    val logger: Logger = Logger("bridge.AppRoot")

    class Backend(scope: BackendScope[Props, Unit]) {

      def render(props: Props, propsChildren: PropsChildren) = { // scalafix:ok ExplicitResultTypes; React

        val theme = Styles.createTheme(
          Theme(
            palette = Palette(
              primary = MuiColor.red,
              secondary = PaletteColor(main = Color.rgb(64, 128, 64)),
              mode = props.base.mode,
            ),
          )
        )

        StateProvider.provide(AppStore.store)(
          ThemeContext.provide(theme)(
            MuiThemeProvider(theme)(
              MuiCssBaseline(
                enableColorScheme = true
              )(
                propsChildren.only()
              )
            )
          )
        )

      }
    }

    val component = ScalaComponent
      .builder[Props]("AppRoot")
      .stateless
      .backend(new Backend(_))
      .renderBackendWithChildren
      .build

  }
}
