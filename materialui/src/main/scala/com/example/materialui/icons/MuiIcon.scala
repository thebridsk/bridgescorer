package com.example.materialui.icons

import scala.reflect.macros.blackbox.Context

object MuiIcon {
  import scala.language.experimental.macros

  def apply(): SvgIconBase = macro createIconComponent

  def createIconComponent(c: Context)() = {
    import c.universe._

    val term = c.internal.enclosingOwner.asTerm
    val iconName = term.name.decodedName.toString

    // does not work
    val code = s"""
        new com.example.materialui.icons.SvgIconBase {

          @scala.scalajs.js.native
          @scala.scalajs.js.annotation.JSImport(
              "@material-ui/icons/${iconName}",
              scala.scalajs.js.annotation.JSImport.Default
          )
          private object icon extends scala.scalajs.js.Any

          protected val f = japgolly.scalajs.react.JsComponent[
                                    com.example.materialui.icons.SvgIconProps,
                                    japgolly.scalajs.react.Children.Varargs,
                                    Null
                            ](icon)

        }
    """
    val tree = c.parse(code)
    q"""$tree"""
  }

}
