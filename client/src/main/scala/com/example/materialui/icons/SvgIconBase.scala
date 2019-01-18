package com.example.materialui.icons

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._
import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.Node
import japgolly.scalajs.react.component.Js

class SvgColor( val value: String) extends AnyVal
object SvgColor {
  val inherit = new SvgColor("inherit")
  val primary = new SvgColor("primary")
  val secondary = new SvgColor("secondary")
  val action = new SvgColor("action")
  val error = new SvgColor("error")
  val disabled = new SvgColor("disabled")
}

class SvgFontSize( val value: String) extends AnyVal
object SvgFontSize {
  val inherit = new SvgFontSize("inherit")
  val default = new SvgFontSize("default")
  val small = new SvgFontSize("small")
  val large = new SvgFontSize("large")
}

trait SvgIconBase {

  protected val f: Js.Component[js.Object, Null, CtorType.PropsAndChildren]

  import js._

  /**
   * @param classes Node passed into the SVG element.
   * @param color The color of the component. It supports those theme colors
   *               that make sense for this component. You can use the
   *               nativeColor property to apply a color attribute to the
   *               SVG element.
   *               Default: inherit
   * @param component The component used for the root node. Either a string
   *                   to use a DOM element or a component.
   *                   Default: svg
   * @param fontSize The fontSize applied to the icon. Defaults to 24px,
   *                  but can be configure to inherit font size.
   *                  Default: default
   * @param nativeColor Applies a color attribute to the SVG element.
   * @param shapeRendering The shape-rendering attribute. The behavior of the
   *                        different options is described on the MDN Web Docs.
   *                        If you are having issues with blurry icons you should
   *                        investigate this property.
   * @param titleAccess Provides a human-readable title for the element that
   *                     contains it. https://www.w3.org/TR/SVG-access/#Equivalent
   * @param viewBox Allows you to redefine what the coordinates without units mean
   *                 inside an SVG element. For example, if the SVG element is
   *                 500 (width) by 200 (height), and you pass viewBox="0 0 50 20",
   *                 this means that the coordinates inside the SVG will go from
   *                 the top left corner (0,0) to bottom right (50,20) and each
   *                 unit will be worth 10px.
   *                 Default: '0 0 24 24'
   *
   * @param children Node passed into the SVG element.
   */
  def apply(
      classes: js.UndefOr[js.Object] = js.undefined,
      color: js.UndefOr[SvgColor] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      fontSize: js.UndefOr[SvgFontSize] = js.undefined,
      nativeColor: js.UndefOr[String] = js.undefined,
      shapeRendering: js.UndefOr[String] = js.undefined,
      titleAccess: js.UndefOr[String] = js.undefined,
      viewBox: js.UndefOr[String] = js.undefined,
  )(
      children: CtorType.ChildArg*
  ) = {
    val p: js.Object with js.Dynamic = js.Dynamic.literal()

    classes.foreach(p.updateDynamic("classes")(_))
    color.foreach(v=>p.updateDynamic("color")(v.value))
    component.foreach(p.updateDynamic("component")(_))
    fontSize.foreach(v=>p.updateDynamic("fontSize")(v.value))
    nativeColor.foreach(p.updateDynamic("nativeColor")(_))
    shapeRendering.foreach(p.updateDynamic("shapeRendering")(_))
    titleAccess.foreach(p.updateDynamic("titleAccess")(_))
    viewBox.foreach(p.updateDynamic("viewBox")(_))

    f(p)(children:_*)
  }

}
