package com.github.thebridsk.materialui.icons

import japgolly.scalajs.react._
import scala.scalajs.js
import scala.scalajs.js.annotation._
import japgolly.scalajs.react.component.Js
import com.github.thebridsk.materialui.PropsFactory
import com.github.thebridsk.materialui.ComponentFactory
import com.github.thebridsk.materialui.AdditionalProps
import scala.scalajs.js.UndefOr

class SvgColor(val value: String) extends AnyVal
object SvgColor {
  val inherit = new SvgColor("inherit")
  val primary = new SvgColor("primary")
  val secondary = new SvgColor("secondary")
  val action = new SvgColor("action")
  val error = new SvgColor("error")
  val disabled = new SvgColor("disabled")
}

class SvgFontSize(val value: String) extends AnyVal
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
  var classes: js.UndefOr[js.Dictionary[String]] = js.native
//  var color: js.UndefOr[SvgColor] = js.native
  var component: js.UndefOr[String] = js.native
//  var fontSize: js.UndefOr[SvgFontSize] = js.native
  var htmlColor: js.UndefOr[String] = js.native
  var shapeRendering: js.UndefOr[String] = js.native
  var titleAccess: js.UndefOr[String] = js.native
  var viewBox: js.UndefOr[String] = js.native
}

object SvgIconProps extends PropsFactory[SvgIconProps] {

  implicit class WrapTypographyProps(private val p: SvgIconProps)
      extends AnyVal {

    def color: UndefOr[SvgColor] = p.colorInternal.map(s => new SvgColor(s))

    def color_=(v: js.UndefOr[SvgColor]) = {
      p.colorInternal = v.map(pp => pp.value)
    }

    def fontSize: UndefOr[SvgFontSize] =
      p.fontSizeInternal.map(s => new SvgFontSize(s))

    def fontSize_=(v: js.UndefOr[SvgFontSize]) = {
      p.fontSizeInternal = v.map(pp => pp.value)
    }

  }

  /**
    * @param props
    * @param classes Node passed into the SVG element.
    * @param color The color of the component. It supports those theme colors
    *               that make sense for this component. You can use the
    *               htmlColor property to apply a color attribute to the
    *               SVG element.
    *               Default: inherit
    * @param component The component used for the root node. Either a string
    *                   to use a DOM element or a component.
    *                   Default: svg
    * @param fontSize The fontSize applied to the icon. Defaults to 24px,
    *                  but can be configure to inherit font size.
    *                  Default: default
    * @param htmlColor Applies a color attribute to the SVG element.
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
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      color: js.UndefOr[SvgColor] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      fontSize: js.UndefOr[SvgFontSize] = js.undefined,
      htmlColor: js.UndefOr[String] = js.undefined,
      shapeRendering: js.UndefOr[String] = js.undefined,
      titleAccess: js.UndefOr[String] = js.undefined,
      viewBox: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p = get(props, additionalProps)
    p.classes = classes
    p.color = color
    p.component = component
    p.fontSize = fontSize
    p.htmlColor = htmlColor
    p.shapeRendering = shapeRendering
    p.titleAccess = titleAccess
    p.viewBox = viewBox
    p
  }
}

trait SvgIconBase extends ComponentFactory[SvgIconProps] {

  protected val f: Js.Component[SvgIconProps, Null, CtorType.PropsAndChildren]

  /**
    * @param classes Node passed into the SVG element.
    * @param color The color of the component. It supports those theme colors
    *               that make sense for this component. You can use the
    *               htmlColor property to apply a color attribute to the
    *               SVG element.
    *               Default: inherit
    * @param component The component used for the root node. Either a string
    *                   to use a DOM element or a component.
    *                   Default: svg
    * @param fontSize The fontSize applied to the icon. Defaults to 24px,
    *                  but can be configure to inherit font size.
    *                  Default: default
    * @param htmlColor Applies a color attribute to the SVG element.
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
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      color: js.UndefOr[SvgColor] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      fontSize: js.UndefOr[SvgFontSize] = js.undefined,
      htmlColor: js.UndefOr[String] = js.undefined,
      shapeRendering: js.UndefOr[String] = js.undefined,
      titleAccess: js.UndefOr[String] = js.undefined,
      viewBox: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): Js.UnmountedWithRawType[SvgIconProps, Null, Js.RawMounted[
    SvgIconProps,
    Null
  ]] = {
    val p: SvgIconProps = SvgIconProps(
      classes = classes,
      color = color,
      component = component,
      fontSize = fontSize,
      htmlColor = htmlColor,
      shapeRendering = shapeRendering,
      titleAccess = titleAccess,
      viewBox = viewBox,
      additionalProps = additionalProps
    )

    f(p)()
  }

  @inline
  def create(
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      color: js.UndefOr[SvgColor] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      fontSize: js.UndefOr[SvgFontSize] = js.undefined,
      htmlColor: js.UndefOr[String] = js.undefined,
      shapeRendering: js.UndefOr[String] = js.undefined,
      titleAccess: js.UndefOr[String] = js.undefined,
      viewBox: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): Js.UnmountedWithRawType[SvgIconProps, Null, Js.RawMounted[
    SvgIconProps,
    Null
  ]] = {

    apply(
      classes = classes,
      color = color,
      component = component,
      fontSize = fontSize,
      htmlColor = htmlColor,
      shapeRendering = shapeRendering,
      titleAccess = titleAccess,
      viewBox = viewBox,
      additionalProps = additionalProps
    )
  }
}

object MuiSvgIcon extends ComponentFactory[SvgIconProps] {
  @js.native @JSImport("@material-ui/core/SvgIcon", JSImport.Default) private object SvgIcon
      extends js.Any

  protected val f = JsComponent[SvgIconProps, Children.Varargs, Null](SvgIcon) // scalafix:ok ExplicitResultTypes; ReactComponent

  /**
    * @param classes Node passed into the SVG element.
    * @param color The color of the component. It supports those theme colors
    *               that make sense for this component. You can use the
    *               htmlColor property to apply a color attribute to the
    *               SVG element.
    *               Default: inherit
    * @param component The component used for the root node. Either a string
    *                   to use a DOM element or a component.
    *                   Default: svg
    * @param fontSize The fontSize applied to the icon. Defaults to 24px,
    *                  but can be configure to inherit font size.
    *                  Default: default
    * @param htmlColor Applies a color attribute to the SVG element.
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
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      color: js.UndefOr[SvgColor] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      fontSize: js.UndefOr[SvgFontSize] = js.undefined,
      htmlColor: js.UndefOr[String] = js.undefined,
      shapeRendering: js.UndefOr[String] = js.undefined,
      titleAccess: js.UndefOr[String] = js.undefined,
      viewBox: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val p: SvgIconProps = SvgIconProps(
      classes = classes,
      color = color,
      component = component,
      fontSize = fontSize,
      htmlColor = htmlColor,
      shapeRendering = shapeRendering,
      titleAccess = titleAccess,
      viewBox = viewBox,
      additionalProps = additionalProps
    )

    val x = f(p) _
    x(children)
  }
}
