package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
trait ButtonBaseProps extends AdditionalProps {
  val action: js.UndefOr[js.Object => Unit] = js.native
  val buttonRef
      : js.UndefOr[js.Object] = js.native // js.object or js.Function0[ref]
  val centerRipple: js.UndefOr[Boolean] = js.native
  val classes: js.UndefOr[js.Dictionary[String]] = js.native
  val component: js.UndefOr[String] = js.native
  val disabled: js.UndefOr[Boolean] = js.native
  val disableRipple: js.UndefOr[Boolean] = js.native
  val disableTouchRipple: js.UndefOr[Boolean] = js.native
  val focusRipple: js.UndefOr[Boolean] = js.native
  val focusVisibleClassName: js.UndefOr[String] = js.native
  val onFocusVisible: js.UndefOr[() => Unit] = js.native
  val TouchRippleProps: js.UndefOr[TouchRippleProps] = js.native
  val `type`: js.UndefOr[String] = js.native

  val onClick: js.UndefOr[ReactEvent => Unit] = js.native
  val style: js.UndefOr[js.Object] = js.native

  val id: js.UndefOr[String] = js.native
}

object ButtonBaseProps extends PropsFactory[ButtonBaseProps] {

  /**
    * @param p the object that will become the properties object
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
    * @param classes Override or extend the styles applied to the component.
    *                 See [[https://material-ui.com/api/button/#css CSS API]]
    *                 for more details.
    * @param component The component used for the root node.
    *                   Either a string to use a DOM element or a component.
    *                   Default: "button"
    * @param disabled If `true`, the button will be disabled.
    * @param disableRipple If `true`, the ripple effect will be disabled.  Default: false
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
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: ButtonBaseProps](
      props: js.UndefOr[P] = js.undefined,
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
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p = get(props, additionalProps)

    action.foreach(p.updateDynamic("action")(_))
    buttonRef.foreach(p.updateDynamic("buttonRef")(_))
    centerRipple.foreach(p.updateDynamic("centerRipple")(_))
    classes.foreach(p.updateDynamic("classes")(_))
    component.foreach(p.updateDynamic("component")(_))
    disabled.foreach(p.updateDynamic("disabled")(_))
    disableRipple.foreach(p.updateDynamic("disableRipple")(_))
    disableTouchRipple.foreach(p.updateDynamic("disableTouchRipple")(_))
    focusRipple.foreach(p.updateDynamic("focusRipple")(_))
    focusVisibleClassName.foreach(p.updateDynamic("focusVisibleClassName")(_))
    onFocusVisible.foreach(p.updateDynamic("onFocusVisible")(_))
    TouchRippleProps.foreach(p.updateDynamic("TouchRippleProps")(_))
    `type`.foreach(p.updateDynamic("type")(_))
    onClick.foreach(p.updateDynamic("onClick")(_))
    style.foreach(p.updateDynamic("style")(_))
    id.foreach(p.updateDynamic("id")(_))
    title.foreach(p.updateDynamic("title")(_))
    p
  }

}

object MuiButtonBase extends ComponentFactory[ButtonBaseProps] {
  @js.native @JSImport("@material-ui/core/Button", JSImport.Default) private object ButtonBase
      extends js.Any

  protected val f =
    JsComponent[ButtonBaseProps, Children.Varargs, Null](ButtonBase)

  /**
    * @param p the object that will become the properties object
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
    * @param classes Override or extend the styles applied to the component.
    *                 See [[https://material-ui.com/api/button/#css CSS API]]
    *                 for more details.
    * @param component The component used for the root node.
    *                   Either a string to use a DOM element or a component.
    *                   Default: "button"
    * @param disabled If `true`, the button will be disabled.
    * @param disableRipple If `true`, the ripple effect will be disabled.  Default: false
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
    * @param additionalProps a dictionary of additional properties
    */
  def apply(
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
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = {
    val p: ButtonBaseProps = ButtonBaseProps(
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
      additionalProps = additionalProps
    )
    val x = f(p) _
    x(children)
  }
}
