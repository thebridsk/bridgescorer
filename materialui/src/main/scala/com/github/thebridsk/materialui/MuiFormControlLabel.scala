package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.language.implicitConversions
import japgolly.scalajs.react.{raw => Raw}

class LabelPlacement(val value: String) extends AnyVal
object LabelPlacement {
  val bottom = new LabelPlacement("bottom")
  val end = new LabelPlacement("end")
  val start = new LabelPlacement("start")
  val top = new LabelPlacement("top")

  implicit def toJsAny(cv: LabelPlacement): String = cv.value

}

@js.native
protected trait FormControlLabelPropsPrivate extends js.Any {
  @JSName("control")
  val controlInternal: js.UndefOr[Raw.React.Node] = js.native
  @JSName("label")
  val labelInternal: js.UndefOr[Raw.React.Node] = js.native
  @JSName("labelPlacement")
  val labelPlacementInternal: js.UndefOr[String] = js.native
}

@js.native
trait FormControlLabelProps extends AdditionalProps with StandardProps with FormControlLabelPropsPrivate {
  val checked: js.UndefOr[Boolean] = js.native
  val classes: js.UndefOr[js.Dictionary[String]] = js.native
  // val control: js.UndefOr[Raw.React.Element] = js.native
  val disable: js.UndefOr[Boolean] = js.native
  val inputRef: js.UndefOr[js.Object] = js.native
  // val label: js.UndefOr[Raw.React.Element] = js.native
//  val labelPlacement: js.UndefOr[String] = js.native
  val onChange: js.UndefOr[js.Function1[js.Object,Unit]] = js.native
  val value: js.UndefOr[js.Any] = js.native

}

object FormControlLabelProps extends PropsFactory[FormControlLabelProps] {

  implicit class WrapFormControlLabelProps(private val p: FormControlLabelProps) extends AnyVal {

    def control = p.controlInternal.map( n => VdomNode(n))
    def label = p.labelInternal.map( n => VdomNode(n))
    def labelPlacement = p.labelPlacementInternal.map(s => new LabelPlacement(s))

  }

  def toRaw( v: VdomNode ) = v.rawNode.asInstanceOf[js.Any]

  /**
    * @param p the object that will become the properties object
    * @param checked If true, the component appears selected.
    * @param classes Override or extend the styles applied to the component. See CSS API below for more details.
    * @param control A control element. For instance, it can be be a Radio, a Switch or a Checkbox.
    * @param disable If true, the control will be disabled.
    * @param inputRef Pass a ref to the input element.
    * @param label The text to be used in an enclosing label element.
    * @param labelPlacement The position of the label.  Default: 'end'.
    * @param onChange Callback fired when the state is changed.
    *                   Signature:
    *                     function(event: object) => void
    *                     event: The event source of the callback. You can pull out the new checked state by accessing event.target.checked (boolean).
    * @param value The value of the component.
    * @param className css class name to add to element
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: FormControlLabelProps](
      props: js.UndefOr[P] = js.undefined,
      checked: js.UndefOr[Boolean] = js.undefined,
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      control: js.UndefOr[VdomNode] = js.undefined,
      disable: js.UndefOr[Boolean] = js.undefined,
      inputRef: js.UndefOr[js.Object] = js.undefined,
      label: js.UndefOr[VdomNode] = js.undefined,
      labelPlacement: js.UndefOr[LabelPlacement] = js.undefined,
      onChange: js.UndefOr[js.Function1[js.Object,Unit]] = js.undefined,
      value: js.UndefOr[js.Any] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p = get(props, additionalProps)

    props.foreach(p.updateDynamic("props")(_))
    checked.foreach(p.updateDynamic("checked")(_))
    classes.foreach(p.updateDynamic("classes")(_))
    control.foreach( v => p.updateDynamic("control")(toRaw(v)))
    disable.foreach(p.updateDynamic("disable")(_))
    inputRef.foreach(p.updateDynamic("inputRef")(_))
    label.foreach( v => p.updateDynamic("label")(toRaw(v)))
    labelPlacement.foreach(v => p.updateDynamic("labelPlacement")(v.value))
    onChange.foreach(p.updateDynamic("onChange")(_))
    value.foreach(p.updateDynamic("value")(_))
    className.foreach(p.updateDynamic("className")(_))

    p
  }

}

object MuiFormControlLabel extends ComponentFactory[FormControlLabelProps] {
  @js.native @JSImport("@material-ui/core/FormControlLabel", JSImport.Default) private object MList
      extends js.Any

  protected val f = JsComponent[FormControlLabelProps, Children.Varargs, Null](MList)

  /**
    * @param checked If true, the component appears selected.
    * @param classes Override or extend the styles applied to the component. See CSS API below for more details.
    * @param control A control element. For instance, it can be be a Radio, a Switch or a Checkbox.
    * @param disable If true, the control will be disabled.
    * @param inputRef Pass a ref to the input element.
    * @param label The text to be used in an enclosing label element.
    * @param labelPlacement The position of the label.  Default: 'end'.
    * @param onChange Callback fired when the state is changed.
    *                   Signature:
    *                     function(event: object) => void
    *                     event: The event source of the callback. You can pull out the new checked state by accessing event.target.checked (boolean).
    * @param value The value of the component.
    * @param className css class name to add to element
    */
  def apply(
      checked: js.UndefOr[Boolean] = js.undefined,
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      control: js.UndefOr[VdomNode] = js.undefined,
      disable: js.UndefOr[Boolean] = js.undefined,
      inputRef: js.UndefOr[js.Object] = js.undefined,
      label: js.UndefOr[VdomNode] = js.undefined,
      labelPlacement: js.UndefOr[LabelPlacement] = js.undefined,
      onChange: js.UndefOr[js.Function1[js.Object,Unit]] = js.undefined,
      value: js.UndefOr[js.Any] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = {
    val p: FormControlLabelProps = FormControlLabelProps(
      checked = checked,
      classes = classes,
      control = control,
      disable = disable,
      inputRef = inputRef,
      label = label,
      labelPlacement = labelPlacement,
      onChange = onChange,
      value = value,
      className = className,
      additionalProps = additionalProps
    )
    val x = f(p) _
    x(children)
  }
}
