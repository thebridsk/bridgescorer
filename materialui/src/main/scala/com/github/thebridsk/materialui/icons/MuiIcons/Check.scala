package com.github.thebridsk.materialui.icons.MuiIcons

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import japgolly.scalajs.react.JsComponent
import japgolly.scalajs.react.Children
import com.github.thebridsk.materialui.icons.SvgIconBase
import com.github.thebridsk.materialui.icons.SvgIconProps

object Check extends SvgIconBase {
  @js.native @JSImport("@material-ui/icons/Check", JSImport.Default)
  private object icon extends js.Any
  protected val f = JsComponent[SvgIconProps, Children.Varargs, Null](icon)
}
