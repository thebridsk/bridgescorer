package com.github.thebridsk.bridge.clientcommon.react

import com.github.thebridsk.color.Color
import japgolly.scalajs.react.vdom.Attr.ValueType

object ReactColor {

  /**
   * This implicit value allows the direct assignment to an attribute.
   * For example:
   *
   *     ^.color := Color("red")
   */
  implicit val vdomAttrColor: ValueType[Color, String] =
    ValueType((b, a) => b(a.toAttrValue))

}
