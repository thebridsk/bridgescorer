package com.example.materialui.icons.MuiIcons

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import japgolly.scalajs.react.JsComponent
import japgolly.scalajs.react.Children
import com.example.materialui.icons.SvgIconBase
import com.example.materialui.icons.SvgIconProps

object MoreVert extends SvgIconBase {
  @js.native @JSImport("@material-ui/icons/MoreVert", JSImport.Default)
  private object icon extends js.Any
  protected val f = JsComponent[SvgIconProps, Children.Varargs, Null](icon)
}
