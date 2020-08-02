package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.scalajs.js.UndefOr

class ToolbarVariant(val value: String) extends AnyVal
object ToolbarVariant {
  val regular = new ToolbarVariant("regular")
  val dense = new ToolbarVariant("dense")

  val values: List[ToolbarVariant] = List(regular, dense)
}

@js.native
protected trait ToolbarPropsPrivate extends js.Any {
  @JSName("variant")
  val variantInternal: js.UndefOr[String] = js.native
}

@js.native
trait ToolbarProps
    extends AdditionalProps
    with ToolbarPropsPrivate
    with StandardProps {
  val classes: js.UndefOr[js.Dictionary[String]] = js.native
  val component: js.UndefOr[String] = js.native
  val disableGutters: js.UndefOr[Boolean] = js.native
//  var variant: js.UndefOr[ToolbarVariant] = js.native
}
object ToolbarProps extends PropsFactory[ToolbarProps] {

  implicit class WrapToolbarProps(private val p: ToolbarProps) extends AnyVal {

    def variant: UndefOr[ToolbarVariant] =
      p.variantInternal.map(s => new ToolbarVariant(s))

//    def variant_= (v: js.UndefOr[ToolbarVariant]): Unit = {
//      v.map{ vv=>p.variantInternal=vv.value; None }.
//        orElse{ p.variantInternal=js.undefined; None }
//    }

  }

  /**
    * @param p the object that will become the properties object
    * @param classes Override or extend the styles applied to the component.
    *                 See CSS API below for more details.
    * @param component The component used for the root node. Either a string to use a DOM element or a component.
    *                  default: div
    * @param disableGutters If true, disables gutter padding.
    *                        Default: false
    * @param variant The variant to use.
    *                Default: regular
    * @param className css class name to add to element
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: ToolbarProps](
      props: js.UndefOr[P] = js.undefined,
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      disableGutters: js.UndefOr[Boolean] = js.undefined,
      variant: js.UndefOr[ToolbarVariant] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p = get(props, additionalProps)

    classes.foreach(p.updateDynamic("classes")(_))
    component.foreach(p.updateDynamic("component")(_))
    disableGutters.foreach(p.updateDynamic("disableGutters")(_))
    variant.foreach(v => p.updateDynamic("variant")(v.value))
    className.foreach(p.updateDynamic("className")(_))

    p
  }
}

object MuiToolbar {
  @js.native @JSImport("@material-ui/core/Toolbar", JSImport.Default) private object Toolbar
      extends js.Any

  protected val f = JsComponent[ToolbarProps, Children.Varargs, Null](Toolbar) // scalafix:ok ExplicitResultTypes; ReactComponent

  /**
    * @param classes Override or extend the styles applied to the component.
    *                 See CSS API below for more details.
    * @param component The component used for the root node. Either a string to use a DOM element or a component.
    *                  default: div
    * @param disableGutters If true, disables gutter padding.
    *                        Default: false
    * @param variant The variant to use.
    *                Default: regular
    * @param className css class name to add to element
    * @param additionalProps a dictionary of additional properties
    */
  def apply(
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      disableGutters: js.UndefOr[Boolean] = js.undefined,
      variant: js.UndefOr[ToolbarVariant] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val p: ToolbarProps = ToolbarProps(
      classes = classes,
      component = component,
      disableGutters = disableGutters,
      variant = variant,
      className = className,
      additionalProps = additionalProps
    )
    val x = f(p) _
    x(children)
  }
}
