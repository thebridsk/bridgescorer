package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.language.implicitConversions
import scala.scalajs.js.UndefOr

class ColorVariant(val value: String) extends AnyVal
object ColorVariant {
  val default = new ColorVariant("default")
  val inherit = new ColorVariant("inherit")
  val primary = new ColorVariant("primary")
  val secondary = new ColorVariant("secondary")
  val values: List[ColorVariant] = List(default, inherit, primary, secondary)

  implicit def toJsAny(cv: ColorVariant): js.Any = cv.value
}

class ItemSize(val value: String) extends AnyVal
object ItemSize {
//  ['small', 'medium', 'large']
  val small = new ItemSize("small")
  val medium = new ItemSize("medium")
  val large = new ItemSize("large")
  val values: List[ItemSize] = List(small, medium, large)

  implicit def toJsAny(cv: ItemSize): js.Any = cv.value
}
//         * __WARNING__: `flat` and `raised` are deprecated.
//         * Instead use `text` and `contained` respectively.
//         * `fab` and `extendedFab` are deprecated.
//         * Instead use `<Fab>` and `<Fab variant="extended">`

class Variant(val value: String) extends AnyVal
object Variant {
  val text = new Variant("text")
  val outlined = new Variant("outlined")
  val contained = new Variant("contained")

  val values: List[Variant] = List(text, outlined, contained)

  implicit def toJsAny(cv: Variant): js.Any = cv.value
}

@js.native
protected trait ButtonPropsPrivate extends js.Any {
  @JSName("color")
  val colorInternal: js.UndefOr[String] = js.native
  @JSName("size")
  val sizeInternal: js.UndefOr[String] = js.native
  @JSName("variant")
  val variantInternal: js.UndefOr[String] = js.native
}

@js.native
trait ButtonProps extends ButtonBaseProps with ButtonPropsPrivate {
  val disableFocusRipple: js.UndefOr[Boolean] = js.native
  val fullWidth: js.UndefOr[Boolean] = js.native
  val href: js.UndefOr[String] = js.native
  val mini: js.UndefOr[Boolean] = js.native

}

object ButtonProps extends PropsFactory[ButtonProps] {

  implicit class WrapButtonProps(private val p: ButtonProps) extends AnyVal {

    def color: UndefOr[ColorVariant] =
      p.colorInternal.map(s => new ColorVariant(s))

//    def color_= (v: js.UndefOr[ColorVariant]) = { p.colorInternal = v.map(pp => pp.value) }

    def size: UndefOr[ItemSize] = p.sizeInternal.map(s => new ItemSize(s))

//    def size_= (v: js.UndefOr[ItemSize]) = { p.sizeInternal = v.map(pp => pp.value) }

    def variant: UndefOr[Variant] = p.variantInternal.map(s => new Variant(s))

//    def variant_= (v: js.UndefOr[Variant]) = { p.variantInternal = v.map(pp => pp.value) }

  }

  /**
    * @param p the object that will become the properties object
    * @param color The color of the component. It supports those theme colors
    *               that make sense for this component.  Default: ColorVariant.default
    * @param fullWidth If `true`, the button will take up the full width of its container.
    *                   Default: false
    * @param href The URL to link to when the button is clicked.
    *              If defined, an `a` element will be used as the root node.
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
    * @param classes Override or extend the styles applied to the component.
    *                 See [[https://material-ui.com/api/button/#css CSS API]]
    *                 for more details.
    * @param component The component used for the root node.
    *                   Either a string to use a DOM element or a component.
    *                   Default: "button"
    * @param disabled If `true`, the button will be disabled.  Default: false
    * @param disableFocusRipple If `true`, the  keyboard focus ripple will be disabled.
    *                            `disableRipple` must also be true.
    *                            Default: "button"
    * @param disableRipple If `true`, the ripple effect will be disabled.
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
  def apply[P <: ButtonProps](
      props: js.UndefOr[P] = js.undefined,
      color: js.UndefOr[ColorVariant] = js.undefined,
      fullWidth: js.UndefOr[Boolean] = js.undefined,
      href: js.UndefOr[String] = js.undefined,
      size: js.UndefOr[ItemSize] = js.undefined,
      variant: js.UndefOr[Variant] = js.undefined,
      // from ButtonBase
      action: js.UndefOr[js.Object => Unit] = js.undefined,
      buttonRef: js.UndefOr[js.Object] =
        js.undefined, // js.object or js.Function0[ref]
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

    color.foreach(p.updateDynamic("color")(_))
    fullWidth.foreach(p.updateDynamic("fullWidth")(_))
    href.foreach(p.updateDynamic("href")(_))
    size.foreach(p.updateDynamic("size")(_))
    variant.foreach(p.updateDynamic("variant")(_))

    p
  }

}

object MuiButton extends ComponentFactory[ButtonProps] {
  @js.native @JSImport(
    "@material-ui/core/Button",
    JSImport.Default
  ) private object Button extends js.Any

  protected val f =
    JsComponent[ButtonProps, Children.Varargs, Null](
      Button
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

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
    * @param id the value of the id attribute
    * @param title the value of the title attribute
    * @param className css class name to add to element
    * @param additionalProps a dictionary of additional properties
    * @param children
    */
  def apply(
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      color: js.UndefOr[ColorVariant] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      disabled: js.UndefOr[Boolean] = js.undefined,
      disableFocusRipple: js.UndefOr[Boolean] = js.undefined,
      disableRipple: js.UndefOr[Boolean] = js.undefined,
      fullWidth: js.UndefOr[Boolean] = js.undefined,
      href: js.UndefOr[String] = js.undefined,
      size: js.UndefOr[ItemSize] = js.undefined,
      variant: js.UndefOr[Variant] = js.undefined,
      // from ButtonBase
      action: js.UndefOr[js.Object => Unit] = js.undefined,
      buttonRef: js.UndefOr[js.Object] =
        js.undefined, // js.object or js.Function0[ref]
      centerRipple: js.UndefOr[Boolean] = js.undefined,
//        classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
//        component: js.UndefOr[String] = js.undefined,
//        disabled: js.UndefOr[Boolean] = js.undefined,
//        disableRipple: js.UndefOr[Boolean] = js.undefined,
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
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val p: ButtonProps = ButtonProps(
      js.undefined,
      color,
      fullWidth,
      href,
      size,
      variant,
      // from ButtonBase
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
    val x = f(p) _
    x(children)
  }

}
