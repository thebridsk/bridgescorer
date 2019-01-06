package com.example.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._


class ColorVariant(val value: String) extends AnyVal
object ColorVariant {
  val default = new ColorVariant("default")
  val inherit = new ColorVariant("inherit")
  val primary = new ColorVariant("primary")
  val secondary = new ColorVariant("secondary")
  val values = List( default, inherit, primary, secondary)
}

class ItemSize( val value: String) extends AnyVal
object ItemSize {
//  ['small', 'medium', 'large']
  val small = new ItemSize("small")
  val medium = new ItemSize("medium")
  val large = new ItemSize("large")
  val values = List(small, medium, large)
}
//         * __WARNING__: `flat` and `raised` are deprecated.
//         * Instead use `text` and `contained` respectively.
//         * `fab` and `extendedFab` are deprecated.
//         * Instead use `<Fab>` and `<Fab variant="extended">`

class Variant( val value: String ) extends AnyVal
object Variant {
  val text = new Variant("text")
  val outlined = new Variant("outlined")
  val contained = new Variant("contained")

//  @deprecated("Use <Fab> element instead", "1.0")
  val fab = new Variant("fab")
//  @deprecated("Use <Fab variant=\"extended\"> element instead", "1.0")
  val extendedFab = new Variant("extendedFab")
//  @deprecated("use 'text'", "1.0")
  val flat = new Variant("flat")
//  @deprecated("use 'contained'", "1.0")
  val raised = new Variant("raised")

  val values = List(text, outlined, contained, fab, extendedFab, flat, raised)
}

object MuiButton {
    @js.native @JSImport("@material-ui/core/Button", JSImport.Default) private object Button extends js.Any

    private val f = JsComponent[js.Object, Children.Varargs, Null](Button)

    /**
     * @param p the object that will become the properties object
     * @param classes Override or extend the styles applied to the component.
     *                 See [[https://material-ui.com/api/button/#css CSS API]]
     *                 for more details.
     * @param color The color of the component. It supports those theme colors
     *               that make sense for this component.  Default: ColorVariant.default
     * @param component The component used for the root node.
     *                   Either a string to use a DOM element or a component.
     *                   Default: "button"
     * @param disabled If `true`, the button will be disabled.  Default: false
     * @param disableFocusRipple If `true`, the  keyboard focus ripple will be disabled.
     *                            `disableRipple` must also be true.
     *                            Default: "button"
     * @param disableRipple If `true`, the ripple effect will be disabled.
     * @param fullWidth If `true`, the button will take up the full width of its container.
     *                   Default: false
     * @param href The URL to link to when the button is clicked.
     *              If defined, an `a` element will be used as the root node.
     * @param mini If `true`, and `variant` is `'fab'`, will use mini floating action button styling.
     *              Default: false
     * @param size The size of the button. `small` is equivalent to the dense button styling.
     *              Default: ItemSize.medium
     * @param variant The variant to use. WARNING: flat and raised are deprecated.
     *                 Instead use text and contained respectively.
     *                 fab and extendedFab are deprecated.
     *                 Instead use <Fab> and <Fab variant="extended">
     *                 Default: Variant.text
     *
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
     */
    def set(
        pb: js.Object with js.Dynamic,
        classes:  js.UndefOr[js.Object] = js.undefined,
        color: js.UndefOr[ColorVariant] = js.undefined,
        component: js.UndefOr[String] = js.undefined,
        disabled: js.UndefOr[Boolean] = js.undefined,
        disableFocusRipple: js.UndefOr[Boolean] = js.undefined,
        disableRipple: js.UndefOr[Boolean] = js.undefined,
        fullWidth: js.UndefOr[Boolean] = js.undefined,
        href: js.UndefOr[String] = js.undefined,
        mini: js.UndefOr[Boolean] = js.undefined,
        size: js.UndefOr[ItemSize] = js.undefined,
        variant: js.UndefOr[Variant] = js.undefined,

        // from ButtonBase
        action: js.UndefOr[js.Function1[js.Object,Unit]] = js.undefined,
        buttonRef: js.UndefOr[js.Object] = js.undefined,   // js.object or js.Function0[ref]
        centerRipple: js.UndefOr[Boolean] = js.undefined,
//        classes:  js.UndefOr[js.Object] = js.undefined,
//        component: js.UndefOr[String] = js.undefined,
//        disabled: js.UndefOr[Boolean] = js.undefined,
//        disableRipple: js.UndefOr[Boolean] = js.undefined,
        disableTouchRipple: js.UndefOr[Boolean] = js.undefined,
        focusRipple: js.UndefOr[Boolean] = js.undefined,
        focusVisibleClassName: js.UndefOr[String] = js.undefined,
        onFocusVisible: js.UndefOr[js.Function0[Unit]] = js.undefined,
        TouchRippleProps: js.UndefOr[TouchRippleProps] = js.undefined,
        `type`: js.UndefOr[String] = js.undefined,

        onClick: js.UndefOr[ReactEvent => Unit] = js.undefined,
        style: js.UndefOr[js.Object] = js.undefined
    ): js.Object with js.Dynamic = {
      val p = MuiButtonBase.set(
                 pb,
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
                 style
               )

      color.foreach(c =>  p.updateDynamic("color")(c.value) )
      disableFocusRipple.foreach(p.updateDynamic("disableFocusRipple")(_))
      fullWidth.foreach(p.updateDynamic("fullWidth")(_))
      href.foreach(p.updateDynamic("href")(_))
      mini.foreach(p.updateDynamic("mini")(_))
      size.foreach(s => p.updateDynamic("size")(s.value))
      variant.foreach(v => p.updateDynamic("variant")(v.value))
      p
    }


    /**
     * @param classes Override or extend the styles applied to the component.
     *                 See [[https://material-ui.com/api/button/#css CSS API]]
     *                 for more details.
     * @param color The color of the component. It supports those theme colors
     *               that make sense for this component.  Default: ColorVariant.default
     * @param component The component used for the root node.
     *                   Either a string to use a DOM element or a component.
     *                   Default: "button"
     * @param disabled If `true`, the button will be disabled.  Default: false
     * @param disableFocusRipple If `true`, the  keyboard focus ripple will be disabled.
     *                            `disableRipple` must also be true.
     *                            Default: "button"
     * @param disableRipple If `true`, the ripple effect will be disabled.
     * @param fullWidth If `true`, the button will take up the full width of its container.
     *                   Default: false
     * @param href The URL to link to when the button is clicked.
     *              If defined, an `a` element will be used as the root node.
     * @param mini If `true`, and `variant` is `'fab'`, will use mini floating action button styling.
     *              Default: false
     * @param size The size of the button. `small` is equivalent to the dense button styling.
     *              Default: ItemSize.medium
     * @param variant The variant to use. WARNING: flat and raised are deprecated.
     *                 Instead use text and contained respectively.
     *                 fab and extendedFab are deprecated.
     *                 Instead use <Fab> and <Fab variant="extended">
     *                 Default: Variant.text
     *
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
     * @param children
     */
    def apply(
        classes:  js.UndefOr[js.Object] = js.undefined,
        color: js.UndefOr[ColorVariant] = js.undefined,
        component: js.UndefOr[String] = js.undefined,
        disabled: js.UndefOr[Boolean] = js.undefined,
        disableFocusRipple: js.UndefOr[Boolean] = js.undefined,
        disableRipple: js.UndefOr[Boolean] = js.undefined,
        fullWidth: js.UndefOr[Boolean] = js.undefined,
        href: js.UndefOr[String] = js.undefined,
        mini: js.UndefOr[Boolean] = js.undefined,
        size: js.UndefOr[ItemSize] = js.undefined,
        variant: js.UndefOr[Variant] = js.undefined,

        // from ButtonBase
        action: js.UndefOr[js.Function1[js.Object,Unit]] = js.undefined,
        buttonRef: js.UndefOr[js.Object] = js.undefined,   // js.object or js.Function0[ref]
        centerRipple: js.UndefOr[Boolean] = js.undefined,
//        classes:  js.UndefOr[js.Object] = js.undefined,
//        component: js.UndefOr[String] = js.undefined,
//        disabled: js.UndefOr[Boolean] = js.undefined,
//        disableRipple: js.UndefOr[Boolean] = js.undefined,
        disableTouchRipple: js.UndefOr[Boolean] = js.undefined,
        focusRipple: js.UndefOr[Boolean] = js.undefined,
        focusVisibleClassName: js.UndefOr[String] = js.undefined,
        onFocusVisible: js.UndefOr[js.Function0[Unit]] = js.undefined,
        TouchRippleProps: js.UndefOr[TouchRippleProps] = js.undefined,
        `type`: js.UndefOr[String] = js.undefined,

        onClick: js.UndefOr[ReactEvent => Unit] = js.undefined,
        style: js.UndefOr[js.Object] = js.undefined
    )(
        children: CtorType.ChildArg*
    ) = {
      val p = set(
                  js.Dynamic.literal(),
                  classes,
                  color,
                  component,
                  disabled,
                  disableFocusRipple,
                  disableRipple,
                  fullWidth,
                  href,
                  mini,
                  size,
                  variant,

                  action,
                  buttonRef,
                  centerRipple,
                  disableTouchRipple,
                  focusRipple,
                  focusVisibleClassName,
                  onFocusVisible,
                  TouchRippleProps,
                  `type`,
                  onClick,
                  style
              )
      val x = f(p) _
      x(children)
    }

}
