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
import com.example.materialui.PropsFactory
import com.example.materialui.ComponentFactory
import com.example.materialui.AdditionalProps

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

@js.native
protected trait SvgIconPropsPrivate extends js.Any {
  @JSName("color")
  var colorInternal: js.UndefOr[String] = js.native
  @JSName("fontSize")
  var fontSizeInternal: js.UndefOr[String] = js.native
}

@js.native
trait SvgIconProps extends AdditionalProps with SvgIconPropsPrivate {
  var classes: js.UndefOr[js.Object] = js.native
//  var color: js.UndefOr[SvgColor] = js.native
  var component: js.UndefOr[String] = js.native
//  var fontSize: js.UndefOr[SvgFontSize] = js.native
  var nativeColor: js.UndefOr[String] = js.native
  var shapeRendering: js.UndefOr[String] = js.native
  var titleAccess: js.UndefOr[String] = js.native
  var viewBox: js.UndefOr[String] = js.native
}

object SvgIconProps extends PropsFactory[SvgIconProps] {

  implicit class WrapTypographyProps( val p: SvgIconProps ) extends AnyVal {

    def color = p.colorInternal.map( s => new SvgColor(s) )

    def color_= (v: js.UndefOr[SvgColor]) = { p.colorInternal = v.map(pp => pp.value) }

    def fontSize = p.fontSizeInternal.map( s => new SvgFontSize(s) )

    def fontSize_= (v: js.UndefOr[SvgFontSize]) = { p.fontSizeInternal = v.map(pp => pp.value) }

  }

  /**
   * @param props
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
   * @param additionalProps a dictionary of additional properties
   */
  def apply[P <: SvgIconProps](
      props: js.UndefOr[P] = js.undefined,
      classes: js.UndefOr[js.Object] = js.undefined,
      color: js.UndefOr[SvgColor] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      fontSize: js.UndefOr[SvgFontSize] = js.undefined,
      nativeColor: js.UndefOr[String] = js.undefined,
      shapeRendering: js.UndefOr[String] = js.undefined,
      titleAccess: js.UndefOr[String] = js.undefined,
      viewBox: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
      val p = get(props,additionalProps)
      p.classes = classes
      p.color = color
      p.component = component
      p.fontSize = fontSize
      p.nativeColor = nativeColor
      p.shapeRendering = shapeRendering
      p.titleAccess = titleAccess
      p.viewBox = viewBox
      p
  }
}


trait SvgIconBase extends ComponentFactory[SvgIconProps] {

  protected val f: Js.Component[SvgIconProps, Null, CtorType.PropsAndChildren]

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
   * @param additionalProps a dictionary of additional properties
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

      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = {
    val p: SvgIconProps = SvgIconProps(
      classes = classes,
      color = color,
      component = component,
      fontSize = fontSize,
      nativeColor = nativeColor,
      shapeRendering = shapeRendering,
      titleAccess = titleAccess,
      viewBox = viewBox,
      additionalProps = additionalProps
    )

    f(p)(children:_*)
  }

  @inline
  def create(
      classes: js.UndefOr[js.Object] = js.undefined,
      color: js.UndefOr[SvgColor] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      fontSize: js.UndefOr[SvgFontSize] = js.undefined,
      nativeColor: js.UndefOr[String] = js.undefined,
      shapeRendering: js.UndefOr[String] = js.undefined,
      titleAccess: js.UndefOr[String] = js.undefined,
      viewBox: js.UndefOr[String] = js.undefined,

      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ) = {

    apply(
      classes = classes,
      color = color,
      component = component,
      fontSize = fontSize,
      nativeColor = nativeColor,
      shapeRendering = shapeRendering,
      titleAccess = titleAccess,
      viewBox = viewBox,
      additionalProps = additionalProps
    )()
  }
}
