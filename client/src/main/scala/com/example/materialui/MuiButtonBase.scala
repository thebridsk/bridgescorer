package com.example.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._

object MuiButtonBase {
    @js.native @JSImport("@material-ui/core/Button", JSImport.Default) private object ButtonBase extends js.Any

    private val f = JsComponent[js.Object, Children.Varargs, Null](ButtonBase)

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
     */
    def set(
        p: js.Object with js.Dynamic,
        action: js.UndefOr[js.Function1[js.Object,Unit]] = js.undefined,
        buttonRef: js.UndefOr[js.Object] = js.undefined,   // js.object or js.Function0[ref]
        centerRipple: js.UndefOr[Boolean] = js.undefined,
        classes:  js.UndefOr[js.Object] = js.undefined,
        component: js.UndefOr[String] = js.undefined,
        disabled: js.UndefOr[Boolean] = js.undefined,
        disableRipple: js.UndefOr[Boolean] = js.undefined,
        disableTouchRipple: js.UndefOr[Boolean] = js.undefined,
        focusRipple: js.UndefOr[Boolean] = js.undefined,
        focusVisibleClassName: js.UndefOr[String] = js.undefined,
        onFocusVisible: js.UndefOr[js.Function0[Unit]] = js.undefined,
        TouchRippleProps: js.UndefOr[TouchRippleProps] = js.undefined,
        `type`: js.UndefOr[String] = js.undefined,

        onClick: js.UndefOr[ReactEvent => Unit] = js.undefined,
        style: js.UndefOr[js.Object] = js.undefined,

        id: js.UndefOr[String] = js.undefined
    ): js.Object with js.Dynamic = {
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
      p
    }

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
     */
    def apply(
        action: js.UndefOr[js.Function1[js.Object,Unit]] = js.undefined,
        buttonRef: js.UndefOr[js.Object] = js.undefined,   // js.object or js.Function0[ref]
        centerRipple: js.UndefOr[Boolean] = js.undefined,
        classes:  js.UndefOr[js.Object] = js.undefined,
        component: js.UndefOr[String] = js.undefined,
        disabled: js.UndefOr[Boolean] = js.undefined,
        disableRipple: js.UndefOr[Boolean] = js.undefined,
        disableTouchRipple: js.UndefOr[Boolean] = js.undefined,
        focusRipple: js.UndefOr[Boolean] = js.undefined,
        focusVisibleClassName: js.UndefOr[String] = js.undefined,
        onFocusVisible: js.UndefOr[js.Function0[Unit]] = js.undefined,
        TouchRippleProps: js.UndefOr[TouchRippleProps] = js.undefined,
        `type`: js.UndefOr[String] = js.undefined,

        onClick: js.UndefOr[ReactEvent => Unit] = js.undefined,
        style: js.UndefOr[js.Object] = js.undefined,
        id: js.UndefOr[String] = js.undefined
    )(
        children: CtorType.ChildArg*
    ) = {
      val p = set(
                  js.Dynamic.literal(),
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
                  id
              )
      val x = f(p) _
      x(children)
    }
}
