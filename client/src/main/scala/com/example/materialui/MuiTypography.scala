package com.example.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._
import org.scalajs.dom.raw.Element
//import com.example.materialui.util.{ JsNumber => _, _ }
import scala.language.implicitConversions

import js._

class TextAlign( val value: String ) extends AnyVal
object TextAlign {
  val inherit = new TextAlign("inherit")
  val left = new TextAlign("left")
  val center = new TextAlign("center")
  val right = new TextAlign("right")
  val justify = new TextAlign("justify")

  val default = inherit

  implicit def wrapHorizontal( v: TextAlign ) = js.Object( v.value )
}

class TextVariant( val value: String ) extends AnyVal
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

  implicit def wrapHorizontal( v: TextVariant ) = js.Object( v.value )
}

class TextColor( val value: String ) extends AnyVal
object TextColor {
  val default = new TextColor("default")
  val error = new TextColor("error")
  val inherit = new TextColor("inherit")
  val primary = new TextColor("primary")
  val secondary = new TextColor("secondary")
  val textPrimary = new TextColor("textPrimary")
  val textSecondary = new TextColor("textSecondary")

  implicit def wrapHorizontal( v: TextColor ) = js.Object( v.value )
}

import js._

@js.native
trait TypographyProps extends js.Object {
  val action: js.UndefOr[js.Function1[js.Object,Unit]] = js.native
}
object TypographyProps {
  import js._
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
   */
  def apply(
      p: js.Object with js.Dynamic = js.Dynamic.literal(),

      align: js.UndefOr[TextAlign] = js.undefined,
      classes: js.UndefOr[js.Any] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      color: js.UndefOr[TextColor] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      gutterBottom: js.UndefOr[Boolean] = js.undefined,
      headlineMapping: js.UndefOr[Map[String,String]] = js.undefined,
      inline: js.UndefOr[Boolean] = js.undefined,
      nowrap: js.UndefOr[Boolean] = js.undefined,
      paragraph: js.UndefOr[Boolean] = js.undefined,
      variant: js.UndefOr[TextVariant] = js.undefined,

  ) = {

    align.foreach(p.updateDynamic("action")(_))
    classes.foreach(p.updateDynamic("classes")(_))
    className.foreach(p.updateDynamic("className")(_))
    color.foreach(p.updateDynamic("color")(_))
    component.foreach(p.updateDynamic("component")(_))
    gutterBottom.foreach(p.updateDynamic("gutterBottom")(_))
    headlineMapping.foreach(p.updateDynamic("headlineMapping")(_))
    inline.foreach(p.updateDynamic("inline")(_))
    nowrap.foreach(p.updateDynamic("nowrap")(_))
    paragraph.foreach(p.updateDynamic("paragraph")(_))
    variant.foreach(p.updateDynamic("variant")(_))

    val r = p.asInstanceOf[TypographyProps]

    r
  }
}

object MuiTypography {
    @js.native @JSImport("@material-ui/core/Typography", JSImport.Default) private object Typography extends js.Any

    private val f = JsComponent[TypographyProps, Children.Varargs, Null](Typography)

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
     */
    def apply(
      align: js.UndefOr[TextAlign] = js.undefined,
      classes: js.UndefOr[js.Any] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      color: js.UndefOr[TextColor] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      gutterBottom: js.UndefOr[Boolean] = js.undefined,
      headlineMapping: js.UndefOr[Map[String,String]] = js.undefined,
      inline: js.UndefOr[Boolean] = js.undefined,
      nowrap: js.UndefOr[Boolean] = js.undefined,
      paragraph: js.UndefOr[Boolean] = js.undefined,
      variant: js.UndefOr[TextVariant] = js.undefined,
    )(
        children: CtorType.ChildArg*
    ) = {
      val p = TypographyProps(
                  js.Dynamic.literal(),

                  align,
                  classes,
                  className,
                  color,
                  component,
                  gutterBottom,
                  headlineMapping,
                  inline,
                  nowrap,
                  paragraph,
                  variant,
              )
      val x = f(p) _
      x(children)
    }
}
