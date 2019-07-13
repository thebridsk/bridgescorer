package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._

class ToolbarVariant(val value: String) extends AnyVal
object ToolbarVariant {
  val regular = new ToolbarVariant("regular")
  val dense = new ToolbarVariant("dense")

  val values = List(regular, dense)
}

@js.native
protected trait ToolbarPropsPrivate extends js.Any {
  @JSName("variant")
  val variantInternal: js.UndefOr[String] = js.native
}

@js.native
trait ToolbarProps extends AdditionalProps with ToolbarPropsPrivate {
  val classes: js.UndefOr[js.Dictionary[String]] = js.native
  val disableGutters: js.UndefOr[Boolean] = js.native
//  var variant: js.UndefOr[ToolbarVariant] = js.native
}
object ToolbarProps extends PropsFactory[ToolbarProps] {

  implicit class WrapToolbarProps(val p: ToolbarProps) extends AnyVal {

    def variant = p.variantInternal.map(s => new ToolbarVariant(s))

//    def variant_= (v: js.UndefOr[ToolbarVariant]): Unit = {
//      v.map{ vv=>p.variantInternal=vv.value; None }.
//        orElse{ p.variantInternal=js.undefined; None }
//    }

  }

  /**
    * @param p the object that will become the properties object
    * @param classes Override or extend the styles applied to the component.
    *                 See CSS API below for more details.
    * @param disableGutters If true, disables gutter padding.
    *                        Default: false
    * @param variant The variant to use.
    *                Default: regular
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: ToolbarProps](
      props: js.UndefOr[P] = js.undefined,
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      disableGutters: js.UndefOr[Boolean] = js.undefined,
      variant: js.UndefOr[ToolbarVariant] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p = get(props, additionalProps)

    classes.foreach(p.updateDynamic("classes")(_))
    disableGutters.foreach(p.updateDynamic("disableGutters")(_))
    variant.foreach(v => p.updateDynamic("variant")(v.value))

    p
  }
}

object MuiToolbar {
  @js.native @JSImport("@material-ui/core/Toolbar", JSImport.Default) private object Toolbar
      extends js.Any

  protected val f = JsComponent[ToolbarProps, Children.Varargs, Null](Toolbar)

  /**
    * @param classes Override or extend the styles applied to the component.
    *                 See CSS API below for more details.
    * @param disableGutters If true, disables gutter padding.
    *                        Default: false
    * @param variant The variant to use.
    *                Default: regular
    * @param additionalProps a dictionary of additional properties
    */
  def apply(
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      disableGutters: js.UndefOr[Boolean] = js.undefined,
      variant: js.UndefOr[ToolbarVariant] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = {
    val p: ToolbarProps = ToolbarProps(
      classes = classes,
      disableGutters = disableGutters,
      variant = variant,
      additionalProps = additionalProps
    )
    val x = f(p) _
    x(children)
  }
}
