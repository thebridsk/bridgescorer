package com.github.thebridsk.materialui.styles

import scala.scalajs.js
import com.github.thebridsk.materialui.AdditionalProps
import com.github.thebridsk.materialui.StandardProps
import com.github.thebridsk.materialui.ComponentFactory
import japgolly.scalajs.react.Children
import japgolly.scalajs.react._

@js.native
trait ThemeProviderProps extends AdditionalProps with StandardProps {
  val theme: Theme
}


object MuiThemeProvider extends ComponentFactory[ThemeProviderProps] {
  // @js.native
  // @js.annotation.JSImport("@mui/material/styles/ThemeProvider", js.annotation.JSImport.Default)
  // private object ThemeProvider extends js.Any

  protected val f =
    JsComponent[ThemeProviderProps, Children.Varargs, Null](
      Styles.ThemeProvider
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  /**
    * @param theme  A theme object, usually the result of createTheme(). The provided theme will be merged with the default theme.
    *               You can provide a function to extend the outer theme.
    */
  def apply(
    theme: Theme
  )(
      children: CtorType.ChildArg*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val p = js.Dynamic.literal().asInstanceOf[ThemeProviderProps]
    p.updateDynamic("theme")(theme)

    val x = f(p) _
    x(children)
  }

}
