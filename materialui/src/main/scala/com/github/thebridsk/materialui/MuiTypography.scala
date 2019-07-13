package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._
import org.scalajs.dom.raw.Element
//import com.github.thebridsk.materialui.util.{ JsNumber => _, _ }
import scala.language.implicitConversions

import js._

class TextAlign(val value: String) extends AnyVal {
  override def toString() = value
}

object TextAlign {
  val inherit = new TextAlign("inherit")
  val left = new TextAlign("left")
  val center = new TextAlign("center")
  val right = new TextAlign("right")
  val justify = new TextAlign("justify")

  val default = inherit
}

class TextVariant(val value: String) extends AnyVal {
  override def toString() = value
}

object TextVariant {
  val h1 = new TextVariant("h1")
  val h2 = new TextVariant("h2")
  val h3 = new TextVariant("h3")
  val h4 = new TextVariant("h4")
  val h5 = new TextVariant("h5")
  val h6 = new TextVariant("h6")
  val subtitle1 = new TextVariant("subtitle1")
  val subtitle2 = new TextVariant("subtitle2")
  val body1 = new TextVariant("body1")
  val body2 = new TextVariant("body2")
  val caption = new TextVariant("caption")
  val button = new TextVariant("button")
  val overline = new TextVariant("overline")
  val srOnly = new TextVariant("srOnly")
  val inherit = new TextVariant("inherit")
  @deprecated("use h1", "0.1")
  val display1 = new TextVariant("display1")
  @deprecated("use h1", "0.1")
  val display2 = new TextVariant("display2")
  @deprecated("use h1", "0.1")
  val display3 = new TextVariant("display3")
  @deprecated("use h1", "0.1")
  val display4 = new TextVariant("display4")
  @deprecated("use h1", "0.1")
  val headline = new TextVariant("headline")
  @deprecated("use h2", "0.1")
  val title = new TextVariant("title")
  @deprecated("use h3", "0.1")
  val subheading = new TextVariant("subheading")
}

class TextColor(val value: String) extends AnyVal {
  override def toString() = value
}

object TextColor {
  val default = new TextColor("default")
  val error = new TextColor("error")
  val inherit = new TextColor("inherit")
  val primary = new TextColor("primary")
  val secondary = new TextColor("secondary")
  val textPrimary = new TextColor("textPrimary")
  val textSecondary = new TextColor("textSecondary")
}

import js._

@js.native
trait TypographyPropsPrivate extends js.Any {
  @JSName("align")
  val alignInternal: js.UndefOr[String] = js.native
  @JSName("color")
  val colorInternal: js.UndefOr[String] = js.native
  @JSName("variant")
  val variantInternal: js.UndefOr[String] = js.native
}

@js.native
trait TypographyProps extends AdditionalProps with TypographyPropsPrivate {
//      val align: js.UndefOr[TextAlign] = js.native
  val classes: js.UndefOr[js.Any] = js.native
  val className: js.UndefOr[String] = js.native
//      val color: js.UndefOr[TextColor] = js.native
  val component: js.UndefOr[String] = js.native
  val gutterBottom: js.UndefOr[Boolean] = js.native
  val headlineMapping: js.UndefOr[Map[String, String]] = js.native
  val inline: js.UndefOr[Boolean] = js.native
  val nowrap: js.UndefOr[Boolean] = js.native
  val paragraph: js.UndefOr[Boolean] = js.native
//      val variant: js.UndefOr[TextVariant] = js.native
}
object TypographyProps extends PropsFactory[TypographyProps] {
  import js._

  implicit class WrapTypographyProps(val p: TypographyProps) extends AnyVal {

    def align = p.alignInternal.map(s => new TextAlign(s))

//    def align_= (v: js.UndefOr[TextAlign]): Unit = {
//      v.map{ vv=>p.alignInternal=vv.value; None }.
//        orElse{ p.alignInternal=js.undefined; None }
//    }

    def color = p.colorInternal.map(s => new TextColor(s))

//    def color_= (v: js.UndefOr[TextColor]): Unit = {
//      v.map{ vv=>p.colorInternal=vv.value; None }.
//        orElse{ p.colorInternal=js.undefined; None }
//    }

    def variant = p.variantInternal.map(s => new TextVariant(s))

//    def variant_= (v: js.UndefOr[TextVariant]): Unit = {
//      v.map{ vv=>p.variantInternal=vv.value; None }.
//        orElse{ p.variantInternal=js.undefined; None }
//    }

  }

  /**
    * @param p the object that will become the properties object
    *
    * @param align Set the text-align on the component.
    *               Default: inherit
    * @param classes Override or extend the styles applied to the component.
    *                 See CSS API below for more details.
    * @param color The color of the component. It supports those theme colors
    *               that make sense for this component.
    *               Default: default
    * @param component The component used for the root node. Either a string
    *                   to use a DOM element or a component. By default, it
    *                   maps the variant to a good default headline component.
    * @param gutterBottom If true, the text will have a bottom margin.
    *                      default: false
    * @param headlineMapping We are empirically mapping the variant property
    *                         to a range of different DOM element types. For instance,
    *                         subtitle1 to <h6>. If you wish to change that mapping, you
    *                         can provide your own. Alternatively, you can use the component
    *                         property. The default mapping is the following:
    *                         { h1: 'h1', h2: 'h2', h3: 'h3', h4: 'h4', h5: 'h5', h6: 'h6', subtitle1: 'h6', subtitle2: 'h6', body1: 'p', body2: 'p',
    *                         // deprecated display4: 'h1', display3: 'h1', display2: 'h1', display1: 'h1', headline: 'h1', title: 'h2', subheading: 'h3',}
    * @param inline Controls whether the Typography is inline or not.
    *                Default: false
    * @param nowrap If true, the text will not wrap, but instead will truncate with an ellipsis.
    *                Default: false
    * @param paragraph If true, the text will have a bottom margin.
    *                   Default: false
    * @param variant Applies the theme typography styles. Use body1 as the default value
    *                 with the legacy implementation and body2 with the new one.
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: TypographyProps](
      props: js.UndefOr[P] = js.undefined,
      align: js.UndefOr[TextAlign] = js.undefined,
      classes: js.UndefOr[js.Any] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      color: js.UndefOr[TextColor] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      gutterBottom: js.UndefOr[Boolean] = js.undefined,
      headlineMapping: js.UndefOr[Map[String, String]] = js.undefined,
      inline: js.UndefOr[Boolean] = js.undefined,
      nowrap: js.UndefOr[Boolean] = js.undefined,
      paragraph: js.UndefOr[Boolean] = js.undefined,
      variant: js.UndefOr[TextVariant] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p = get(props, additionalProps)

    align.foreach(v => p.updateDynamic("align")(v.value))
    classes.foreach(p.updateDynamic("classes")(_))
    className.foreach(p.updateDynamic("className")(_))
    color.foreach(v => p.updateDynamic("color")(v.value))
    component.foreach(p.updateDynamic("component")(_))
    gutterBottom.foreach(p.updateDynamic("gutterBottom")(_))
    headlineMapping.foreach(p.updateDynamic("headlineMapping")(_))
    inline.foreach(p.updateDynamic("inline")(_))
    nowrap.foreach(p.updateDynamic("nowrap")(_))
    paragraph.foreach(p.updateDynamic("paragraph")(_))
    variant.foreach(v => p.updateDynamic("variant")(v.value))

    p
  }
}

object MuiTypography extends ComponentFactory[TypographyProps] {
  @js.native @JSImport("@material-ui/core/Typography", JSImport.Default) private object Typography
      extends js.Any

  protected val f =
    JsComponent[TypographyProps, Children.Varargs, Null](Typography)

  /**
    * @param align Set the text-align on the component.
    *               Default: inherit
    * @param classes Override or extend the styles applied to the component.
    *                 See CSS API below for more details.
    * @param color The color of the component. It supports those theme colors
    *               that make sense for this component.
    *               Default: default
    * @param component The component used for the root node. Either a string
    *                   to use a DOM element or a component. By default, it
    *                   maps the variant to a good default headline component.
    * @param gutterBottom If true, the text will have a bottom margin.
    *                      default: false
    * @param headlineMapping We are empirically mapping the variant property
    *                         to a range of different DOM element types. For instance,
    *                         subtitle1 to <h6>. If you wish to change that mapping, you
    *                         can provide your own. Alternatively, you can use the component
    *                         property. The default mapping is the following:
    *                         { h1: 'h1', h2: 'h2', h3: 'h3', h4: 'h4', h5: 'h5', h6: 'h6', subtitle1: 'h6', subtitle2: 'h6', body1: 'p', body2: 'p',
    *                         // deprecated display4: 'h1', display3: 'h1', display2: 'h1', display1: 'h1', headline: 'h1', title: 'h2', subheading: 'h3',}
    * @param inline Controls whether the Typography is inline or not.
    *                Default: false
    * @param nowrap If true, the text will not wrap, but instead will truncate with an ellipsis.
    *                Default: false
    * @param paragraph If true, the text will have a bottom margin.
    *                   Default: false
    * @param variant Applies the theme typography styles. Use body1 as the default value
    *                 with the legacy implementation and body2 with the new one.
    * @param additionalProps a dictionary of additional properties
    */
  def apply(
      align: js.UndefOr[TextAlign] = js.undefined,
      classes: js.UndefOr[js.Any] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      color: js.UndefOr[TextColor] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      gutterBottom: js.UndefOr[Boolean] = js.undefined,
      headlineMapping: js.UndefOr[Map[String, String]] = js.undefined,
      inline: js.UndefOr[Boolean] = js.undefined,
      nowrap: js.UndefOr[Boolean] = js.undefined,
      paragraph: js.UndefOr[Boolean] = js.undefined,
      variant: js.UndefOr[TextVariant] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = {
    val p: TypographyProps = TypographyProps(
      align = align,
      classes = classes,
      className = className,
      color = color,
      component = component,
      gutterBottom = gutterBottom,
      headlineMapping = headlineMapping,
      inline = inline,
      nowrap = nowrap,
      paragraph = paragraph,
      variant = variant,
      additionalProps = additionalProps
    )
    val x = f(p) _
    x(children)
  }
}
