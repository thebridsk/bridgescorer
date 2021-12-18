package com.github.thebridsk.materialui

import scala.scalajs.js
import japgolly.scalajs.react._

@js.native
trait CssBaselineProps extends AdditionalProps with StandardProps {
  val enableColorScheme: js.UndefOr[Boolean] = js.native
  val component: js.UndefOr[String] = js.native
}
object CssBaselineProps extends PropsFactory[CssBaselineProps] {

  /**
    * @param p the object that will become the properties object
    * @param enableColorScheme Enable color-scheme CSS property to use theme.palette.mode.
    *                          For more details, check out
    *                          https://developer.mozilla.org/en-US/docs/Web/CSS/color-scheme For browser support, check out https://caniuse.com/?search=color-scheme
    *                          Default: false
    * @param component The component used for the root node. Either a
    *                   string to use a DOM element or a component.
    *                   Default: div
    * @param sx      The system prop that allows defining system overrides as well as additional CSS styles.
    * @param className css class name to add to element
    * @param onClick the click handler
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: CssBaselineProps](
      props: js.UndefOr[P] = js.undefined,
      enableColorScheme: js.UndefOr[Boolean] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      sx: js.UndefOr[js.Dictionary[js.Any]] = js.undefined,
      onClick: js.UndefOr[ReactEvent => Unit] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p = get(props, additionalProps, onClick, sx)
    enableColorScheme.foreach(p.updateDynamic("enableColorScheme")(_))
    component.foreach(p.updateDynamic("component")(_))
    className.map(p.updateDynamic("className")(_))
    p
  }
}

object MuiCssBaseline extends ComponentFactory[CssBaselineProps] {
  @js.native @js.annotation.JSImport(
    "@mui/material/CssBaseline",
    js.annotation.JSImport.Default
  ) private object CssBaseline extends js.Any

  protected val f =
    JsComponent[CssBaselineProps, Children.Varargs, Null](
      CssBaseline
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  /**
    * @param enableColorScheme Enable color-scheme CSS property to use theme.palette.mode.
    *                          For more details, check out
    *                          https://developer.mozilla.org/en-US/docs/Web/CSS/color-scheme For browser support, check out https://caniuse.com/?search=color-scheme
    *                          Default: false
    * @param component The component used for the root node. Either a
    *                   string to use a DOM element or a component.
    *                   Default: div
    * @param sx      The system prop that allows defining system overrides as well as additional CSS styles.
    * @param className css class name to add to element
    * @param onClick the click handler
    * @param additionalProps a dictionary of additional properties
    */
  def apply(
      enableColorScheme: js.UndefOr[Boolean] = js.undefined,
      component: js.UndefOr[String] = js.undefined,
      sx: js.UndefOr[js.Dictionary[js.Any]] = js.undefined,
      onClick: js.UndefOr[ReactEvent => Unit] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val p: CssBaselineProps = CssBaselineProps(
      enableColorScheme = enableColorScheme,
      component = component,
      sx = sx,
      onClick = onClick,
      className = className,
      additionalProps = additionalProps
    )
    val x = f(p) _
    x(children)
  }
}
