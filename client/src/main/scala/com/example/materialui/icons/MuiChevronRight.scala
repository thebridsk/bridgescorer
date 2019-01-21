package com.example.materialui.icons

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._
import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.Node

object MuiChevronRightIcon extends SvgIconBase {

  @js.native @JSImport("@material-ui/icons/ChevronRight", JSImport.Default)
  private object icon extends js.Any

  protected val f = JsComponent[SvgIconProps, Children.Varargs, Null](icon)

}
