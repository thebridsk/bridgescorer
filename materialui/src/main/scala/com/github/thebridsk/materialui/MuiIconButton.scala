package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.language.implicitConversions

class ItemEdge(val value: js.Any) extends AnyVal
object ItemEdge {
  val start = new ItemEdge("start")
  val end = new ItemEdge("end")
  val False = new ItemEdge(false)
  val values = List(start,end,False)

  implicit def toJsAny(cv: ItemEdge): js.Any = cv.value
}


@js.native
protected trait IconButtonPropsPrivate extends js.Any {
  @JSName("color")
  val colorInternal: js.UndefOr[String] = js.native
  @JSName("edge")
  val edgeInternal: js.UndefOr[js.Any] = js.native
  @JSName("size")
  val sizeInternal: js.UndefOr[String] = js.native
  @JSName("variant")
  val variantInternal: js.UndefOr[String] = js.native
}

@js.native
trait IconButtonProps extends ButtonBaseProps with IconButtonPropsPrivate {
  val disableFocusRipple: js.UndefOr[Boolean] = js.native
  val fullWidth: js.UndefOr[Boolean] = js.native
  val href: js.UndefOr[String] = js.native

}
object IconButtonProps extends PropsFactory[IconButtonProps] {
  import js._

  implicit class WrapButtonProps(val p: IconButtonProps) extends AnyVal {

    def color = p.colorInternal.map(s => new ColorVariant(s))

    def edge = p.edgeInternal.map( s => new ItemEdge(s))

//    def color_= (v: js.UndefOr[ColorVariant]) = { p.colorInternal = v.map(pp => pp.value) }

    def size = p.sizeInternal.map(s => new ItemSize(s))

//    def size_= (v: js.UndefOr[ItemSize]) = { p.sizeInternal = v.map(pp => pp.value) }

    def variant = p.variantInternal.map(s => new Variant(s))

//    def variant_= (v: js.UndefOr[Variant]) = { p.variantInternal = v.map(pp => pp.value) }

  }

  /**
    * @param p the object that will become the properties object
    * @param classes Override or extend the styles applied to the component.
    *                 See [[https://material-ui.com/api/button/#css CSS API]]
    *                 for more details.
    * @param color The color of the component. It supports those theme colors
    *               that make sense for this component.  Default: ColorVariant.default
    * @param edge  If given, uses a negative margin to counteract the padding on one side
    *              (this is often helpful for aligning the left or right side of the icon
    *              with content above or below, without ruining the border size and shape).
    *              Values: "start", "end", false.  default: false
    * @param size The size of the button. `small` is equivalent to the dense button styling.
    *              Default: ItemSize.medium
    * @param action Callback fired when the component mounts. This is useful when
    *                you want to trigger an action programmatically. It currently
    *                only supports focusVisible() action.
    *                Signature:
    *                  function(actions: object) => void
    *                  actions: This object contains all possible actions that
    *                           can be triggered programmatically.
    * @param buttonRef Use that property to pass a ref callback to the native button component.
    * @param centerRipple If true, the ripples will be centered. They won't start at the cursor interaction position.
    *                      Default: false
    * @param disableTouchRipple If true, the touch ripple effect will be disabled.  Default: false
    * @param focusRipple If true, the base button will have a keyboard focus ripple. disableRipple must also be false
    *                     Default: false
    * @param focusVisibleClassName This property can help a person know which element has
    *                               the keyboard focus. The class name will be applied when
    *                               the element gain the focus through a keyboard interaction.
    *                               It's a polyfill for the CSS :focus-visible feature.
    *                               The rational for using this feature is explain here.
    * @param onFocusVisible Callback fired when the component is focused with a keyboard. We trigger a onFocus callback too.
    * @param TouchRippleProps Properties applied to the TouchRipple element.
    * @param type Used to control the button's purpose. This property passes the value to the type attribute of the native
    *              button component. Valid property values include button, submit, and reset.
    *              Default: "button"
    * @param id the value of the id attribute
    * @param title the value of the title attribute
    * @param className css class name to add to element
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: IconButtonProps](
      props: js.UndefOr[P] = js.undefined,
      color: js.UndefOr[ColorVariant] = js.undefined,
      edge: js.UndefOr[ItemEdge] = js.undefined,
      size: js.UndefOr[ItemSize] = js.undefined,
      // from ButtonBase
      action: js.UndefOr[js.Object => Unit] = js.undefined,
      buttonRef: js.UndefOr[js.Object] = js.undefined, // js.object or js.Function0[ref]
      centerRipple: js.UndefOr[Boolean] = js.undefined,
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      disabled: js.UndefOr[Boolean] = js.undefined,
      disableRipple: js.UndefOr[Boolean] = js.undefined,
      disableTouchRipple: js.UndefOr[Boolean] = js.undefined,
      focusRipple: js.UndefOr[Boolean] = js.undefined,
      focusVisibleClassName: js.UndefOr[String] = js.undefined,
      onFocusVisible: js.UndefOr[() => Unit] = js.undefined,
      TouchRippleProps: js.UndefOr[TouchRippleProps] = js.undefined,
      `type`: js.UndefOr[String] = js.undefined,
      onClick: js.UndefOr[ReactEvent => Unit] = js.undefined,
      style: js.UndefOr[js.Object] = js.undefined,
      id: js.UndefOr[String] = js.undefined,
      title: js.UndefOr[String] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p: P = ButtonBaseProps(
      props,
      action,
      buttonRef,
      centerRipple,
      classes,
      component,
      disabled,
      disableRipple,
      disableTouchRipple,
      focusRipple,
      focusVisibleClassName,
      onFocusVisible,
      TouchRippleProps,
      `type`,
      onClick,
      style,
      id,
      title,
      className,
      additionalProps
    )

    color.foreach(v => p.updateDynamic("color")(v.value))
    edge.foreach(v => p.updateDynamic("edge")(v.value))
    size.foreach(v => p.updateDynamic("size")(v.value))

    p
  }

}

object MuiIconButton extends ComponentFactory[IconButtonProps] {
  @js.native @JSImport("@material-ui/core/IconButton", JSImport.Default) private object IconButton
      extends js.Any

  protected val f =
    JsComponent[IconButtonProps, Children.Varargs, Null](IconButton)

  /**
    * @param classes Override or extend the styles applied to the component.
    *                 See [[https://material-ui.com/api/button/#css CSS API]]
    *                 for more details.
    * @param color The color of the component. It supports those theme colors
    *               that make sense for this component.  Default: ColorVariant.default
    * @param edge  If given, uses a negative margin to counteract the padding on one side
    *              (this is often helpful for aligning the left or right side of the icon
    *              with content above or below, without ruining the border size and shape).
    *              Values: "start", "end", false.  default: false
    * @param size The size of the button. `small` is equivalent to the dense button styling.
    *              Default: ItemSize.medium
    * @param action Callback fired when the component mounts. This is useful when
    *                you want to trigger an action programmatically. It currently
    *                only supports focusVisible() action.
    *                Signature:
    *                  function(actions: object) => void
    *                  actions: This object contains all possible actions that
    *                           can be triggered programmatically.
    * @param buttonRef Use that property to pass a ref callback to the native button component.
    * @param centerRipple If true, the ripples will be centered. They won't start at the cursor interaction position.
    *                      Default: false
    * @param disableTouchRipple If true, the touch ripple effect will be disabled.  Default: false
    * @param focusRipple If true, the base button will have a keyboard focus ripple. disableRipple must also be false
    *                     Default: false
    * @param focusVisibleClassName This property can help a person know which element has
    *                               the keyboard focus. The class name will be applied when
    *                               the element gain the focus through a keyboard interaction.
    *                               It's a polyfill for the CSS :focus-visible feature.
    *                               The rational for using this feature is explain here.
    * @param onFocusVisible Callback fired when the component is focused with a keyboard. We trigger a onFocus callback too.
    * @param TouchRippleProps Properties applied to the TouchRipple element.
    * @param type Used to control the button's purpose. This property passes the value to the type attribute of the native
    *              button component. Valid property values include button, submit, and reset.
    *              Default: "button"
    * @param id the value of the id attribute
    * @param title the value of the title attribute
    * @param className css class name to add to element
    * @param additionalProps a dictionary of additional properties
    */
  def apply(
      color: js.UndefOr[ColorVariant] = js.undefined,
      edge: js.UndefOr[ItemEdge] = js.undefined,
      size: js.UndefOr[ItemSize] = js.undefined,
      // from ButtonBase
      action: js.UndefOr[js.Object => Unit] = js.undefined,
      buttonRef: js.UndefOr[js.Object] = js.undefined, // js.object or js.Function0[ref]
      centerRipple: js.UndefOr[Boolean] = js.undefined,
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      disabled: js.UndefOr[Boolean] = js.undefined,
      disableRipple: js.UndefOr[Boolean] = js.undefined,
      disableTouchRipple: js.UndefOr[Boolean] = js.undefined,
      focusRipple: js.UndefOr[Boolean] = js.undefined,
      focusVisibleClassName: js.UndefOr[String] = js.undefined,
      onFocusVisible: js.UndefOr[() => Unit] = js.undefined,
      TouchRippleProps: js.UndefOr[TouchRippleProps] = js.undefined,
      `type`: js.UndefOr[String] = js.undefined,
      onClick: js.UndefOr[ReactEvent => Unit] = js.undefined,
      style: js.UndefOr[js.Object] = js.undefined,
      id: js.UndefOr[String] = js.undefined,
      title: js.UndefOr[String] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = {
    val p: IconButtonProps = IconButtonProps(
      color = color,
      edge = edge,
      size = size,
      // from ButtonBase
      action = action,
      buttonRef = buttonRef,
      centerRipple = centerRipple,
      classes = classes,
      component = component,
      disabled = disabled,
      disableRipple = disableRipple,
      disableTouchRipple = disableTouchRipple,
      focusRipple = focusRipple,
      focusVisibleClassName = focusVisibleClassName,
      onFocusVisible = onFocusVisible,
      TouchRippleProps = TouchRippleProps,
      `type` = `type`,
      onClick = onClick,
      style = style,
      id = id,
      title = title,
      className = className,
      additionalProps = additionalProps
    )
    val x = f(p) _
    x(children)
  }

}
