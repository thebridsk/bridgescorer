package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js
import scala.scalajs.js.annotation._
import japgolly.scalajs.react.{raw => Raw}
import scala.scalajs.js.UndefOr

@js.native
protected trait RadioPropsPrivate extends js.Any {
  @JSName("checkedIcon")
  val checkedIconInternal: js.UndefOr[Raw.React.Node] = js.native
  @JSName("icon")
  val iconInternal: js.UndefOr[Raw.React.Node] = js.native
}

@js.native
trait RadioProps extends AdditionalProps with StandardProps with RadioPropsPrivate {
  val checked: js.UndefOr[Boolean] = js.native
  // val checkedIcon: js.UndefOr[Raw.React.Element] = js.native
  val classes: js.UndefOr[js.Dictionary[String]] = js.native
  val color: js.UndefOr[ColorVariant] = js.native
  val disable: js.UndefOr[Boolean] = js.native
  val disableRipple: js.UndefOr[Boolean] = js.native
  // val icon: js.UndefOr[Raw.React.Element] = js.native
  val id: js.UndefOr[String] = js.native
  val inputProps: js.UndefOr[js.Dictionary[String]] = js.native
  val inputRef: js.UndefOr[js.Object] = js.native
  val onChange: js.UndefOr[js.Function1[js.Object,Unit]] = js.native
  val required: js.UndefOr[Boolean] = js.native
  val size: js.UndefOr[ItemSize] = js.native
  val value: js.UndefOr[js.Any] = js.native

}

object RadioProps extends PropsFactory[RadioProps] {

  implicit class WrapRadioProps(private val p: RadioProps) extends AnyVal {

    def checkedIcon: UndefOr[VdomNode] = p.checkedIconInternal.map( n => VdomNode(n))
    def icon: UndefOr[VdomNode] = p.iconInternal.map( n => VdomNode(n))

  }

  def toRaw( v: VdomNode ): js.Any = v.rawNode.asInstanceOf[js.Any]

  /**
    * @param p the object that will become the properties object
    * @param checked If true, the component is checked.
    * @param checkedIcon The icon to display when the component is checked.
    *                    default: <CheckBoxIcon />
    * @param classes Override or extend the styles applied to the component. See CSS API below for more details.
    * @param color The color of the component. It supports those theme colors that make sense for this component.
    *              default: 'secondary'
    * @param disable If true, the checkbox will be disabled.
    * @param disableRipple If true, the ripple effect will be disabled.
    * @param icon The icon to display when the component is unchecked.
    *             default: <CheckBoxOutlineBlankIcon />
    * @param id The id of the input element.
    * @param inputProps Attributes applied to the input element.
    * @param inputRef Pass a ref to the input element.
    * @param name the name field
    * @param onChange Callback fired when the state is changed.
    *                         Signature:
    *                           function(event: object) => void
    *                           event: The event source of the callback. You can pull out the new checked state by accessing event.target.checked (boolean).
    * @param required If true, the input element will be required.
    * @param size The size of the checkbox. small is equivalent to the dense checkbox styling.
    *             default: 'medium'
    * @param value The value of the component. The DOM API casts this to a string. The browser uses "on" as the default value.
    * @param className css class name to add to element
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: RadioProps](
      props: js.UndefOr[P] = js.undefined,
      checked: js.UndefOr[Boolean] = js.undefined,
      checkedIcon: js.UndefOr[VdomNode] = js.undefined,
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      color: js.UndefOr[ColorVariant] = js.undefined,
      disable: js.UndefOr[Boolean] = js.undefined,
      disableRipple: js.UndefOr[Boolean] = js.undefined,
      icon: js.UndefOr[VdomNode] = js.undefined,
      id: js.UndefOr[String] = js.undefined,
      inputProps: js.UndefOr[js.Dictionary[String]] = js.undefined,
      inputRef: js.UndefOr[js.Object] = js.undefined,
      name: js.UndefOr[String] = js.undefined,
      onChange: js.UndefOr[js.Function1[js.Object,Unit]] = js.undefined,
      required: js.UndefOr[Boolean] = js.undefined,
      size: js.UndefOr[ItemSize] = js.undefined,
      value: js.UndefOr[js.Any] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p = get(props, additionalProps)

    props.foreach(p.updateDynamic("props")(_))
    checked.foreach(p.updateDynamic("checked")(_))
    checkedIcon.foreach( v => p.updateDynamic("checkedIcon")(toRaw(v)))
    classes.foreach(p.updateDynamic("classes")(_))
    color.foreach(p.updateDynamic("color")(_))
    disable.foreach(p.updateDynamic("disable")(_))
    disableRipple.foreach(p.updateDynamic("disableRipple")(_))
    icon.foreach( v => p.updateDynamic("icon")(toRaw(v)))
    id.foreach(p.updateDynamic("id")(_))
    inputProps.foreach(p.updateDynamic("inputProps")(_))
    inputRef.foreach(p.updateDynamic("inputRef")(_))
    name.foreach(p.updateDynamic("name")(_))
    onChange.foreach(p.updateDynamic("onChange")(_))
    required.foreach(p.updateDynamic("required")(_))
    size.foreach(p.updateDynamic("size")(_))
    value.foreach(p.updateDynamic("value")(_))
    className.foreach(p.updateDynamic("className")(_))

    p
  }

}

object MuiRadio extends ComponentFactory[RadioProps] {
  @js.native @JSImport("@material-ui/core/Radio", JSImport.Default) private object MList
      extends js.Any

  protected val f = JsComponent[RadioProps, Children.Varargs, Null](MList)  // scalafix:ok ExplicitResultTypes; ReactComponent

  /**
    * @param checked If true, the component is checked.
    * @param checkedIcon The icon to display when the component is checked.
    *                    default: <CheckBoxIcon />
    * @param classes Override or extend the styles applied to the component. See CSS API below for more details.
    * @param color The color of the component. It supports those theme colors that make sense for this component.
    *              default: 'secondary'
    * @param disable If true, the checkbox will be disabled.
    * @param disableRipple If true, the ripple effect will be disabled.
    * @param icon The icon to display when the component is unchecked.
    *             default: <CheckBoxOutlineBlankIcon />
    * @param id The id of the input element.
    * @param indeterminate If true, the component appears indeterminate. This does not set the native input element
    *                      to indeterminate due to inconsistent behavior across browsers. However, we set a
    *                      data-indeterminate attribute on the input.
    *                      default: false
    * @param indeterminateIcon The icon to display when the component is indeterminate.
    *                          default: <IndeterminateCheckBoxIcon />
    * @param inputProps Attributes applied to the input element.
    * @param inputRef Pass a ref to the input element.
    * @param name the name field
    * @param onChangeCallback fired when the state is changed.
    *                         Signature:
    *                           function(event: object) => void
    *                           event: The event source of the callback. You can pull out the new checked state by accessing event.target.checked (boolean).
    * @param required If true, the input element will be required.
    * @param size The size of the checkbox. small is equivalent to the dense checkbox styling.
    *             default: 'medium'
    * @param value The value of the component. The DOM API casts this to a string. The browser uses "on" as the default value.
    */
  def apply(
      checked: js.UndefOr[Boolean] = js.undefined,
      checkedIcon: js.UndefOr[VdomNode] = js.undefined,
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      color: js.UndefOr[ColorVariant] = js.undefined,
      disable: js.UndefOr[Boolean] = js.undefined,
      disableRipple: js.UndefOr[Boolean] = js.undefined,
      icon: js.UndefOr[VdomNode] = js.undefined,
      id: js.UndefOr[String] = js.undefined,
      inputProps: js.UndefOr[js.Dictionary[String]] = js.undefined,
      inputRef: js.UndefOr[js.Object] = js.undefined,
      name: js.UndefOr[String] = js.undefined,
      onChange: js.UndefOr[js.Function1[js.Object,Unit]] = js.undefined,
      required: js.UndefOr[Boolean] = js.undefined,
      size: js.UndefOr[ItemSize] = js.undefined,
      value: js.UndefOr[js.Any] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = {  // scalafix:ok ExplicitResultTypes; ReactComponent
    val p: RadioProps = RadioProps(
      checked = checked,
      checkedIcon = checkedIcon,
      classes = classes,
      color = color,
      disable = disable,
      disableRipple = disableRipple,
      icon = icon,
      id = id,
      inputProps = inputProps,
      inputRef = inputRef,
      name = name,
      onChange = onChange,
      required = required,
      size = size,
      value = value,
      className = className,
      additionalProps = additionalProps
    )
    val x = f(p) _
    x(children)
  }
}
