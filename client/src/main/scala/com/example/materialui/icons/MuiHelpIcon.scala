package com.example.materialui.icons

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._
import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.Node

object MuiHelpIcon {

  @js.native @JSImport("@material-ui/icons/Help", JSImport.Default)
  private object icon extends js.Any

  private val f = JsComponent[js.Object, Children.None, Null](icon)

  import js._

  def apply() = {
    val p = js.Object()

    f(p)
  }

}
