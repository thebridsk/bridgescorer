package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
trait BackdropProps extends AdditionalProps with StandardProps {

  /**
    * Override or extend the styles applied to the
    * component. See CSS API below for more details.
    */
  val classes: js.UndefOr[js.Dictionary[String]] = js.native

  /**
    * If true, the backdrop is invisible. It can be used when rendering a popover
    * or a custom select component.
    * Default false
    */
  val invisible: js.UndefOr[Boolean] = js.native

  /**
    * If true, the backdrop is open.
    */
  val open: js.UndefOr[Boolean] = js.native

  /**
    * The duration for the transition, in milliseconds. You may specify a single
    * timeout for all transitions, or individually with an object.
    * Either a Double or a TransitionDuration object.
    */
  val transitionDuration: js.UndefOr[js.Object] = js.native

}

object BackdropProps extends PropsFactory[BackdropProps] {

  /**
    * create a Backdrop object
    *
    * @param classes Override or extend the styles applied to the
    *                component. See CSS API below for more details.
    * @param center If true, the ripple starts at the center of the
    *               component rather than at the point of interaction.
    * @param invisible If true, the backdrop is invisible. It can be used
    *                   when rendering a popover or a custom select component.
    *                   Default false
    * @param open If true, the backdrop is open.
    * @param transitionDuration The duration for the transition, in milliseconds.
    *                            You may specify a single timeout for all transitions,
    *                            or individually with an object. Either a Double or a
    *                            TransitionDuration object.
    * @param className css class name to add to element
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: BackdropProps](
      props: js.UndefOr[P] = js.undefined,
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      invisible: js.UndefOr[Boolean] = js.undefined,
      open: js.UndefOr[Boolean] = js.undefined,
      transitionDuration: js.UndefOr[js.Object] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p: P = get(props, additionalProps)
    classes.foreach(p.updateDynamic("classes")(_))
    invisible.foreach(p.updateDynamic("invisible")(_))
    open.foreach(p.updateDynamic("open")(_))
    className.foreach(p.updateDynamic("className")(_))
    transitionDuration.foreach(p.updateDynamic("transitionDuration")(_))

    p
  }
}

object MuiBackdrop extends ComponentFactory[BackdropProps] {
  @js.native @JSImport(
    "@mui/material/Backdrop",
    JSImport.Default
  ) private object Backdrop extends js.Any

  protected val f =
    JsComponent[BackdropProps, Children.Varargs, Null](
      Backdrop
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  /**
    * @param classes Override or extend the styles applied to the
    *                component. See CSS API below for more details.
    * @param center If true, the ripple starts at the center of the
    *               component rather than at the point of interaction.
    * @param invisible If true, the backdrop is invisible. It can be used
    *                   when rendering a popover or a custom select component.
    *                   Default false
    * @param open If true, the backdrop is open.
    * @param transitionDuration The duration for the transition, in milliseconds.
    *                            You may specify a single timeout for all transitions,
    *                            or individually with an object. Either a Double or a
    *                            TransitionDuration object.
    * @param className css class name to add to element
    * @param additionalProps a dictionary of additional properties
    */
  def apply(
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
      invisible: js.UndefOr[Boolean] = js.undefined,
      open: js.UndefOr[Boolean] = js.undefined,
      transitionDuration: js.UndefOr[js.Object] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val p: BackdropProps = BackdropProps(
      classes = classes,
      invisible = invisible,
      open = open,
      transitionDuration = transitionDuration,
      className = className,
      additionalProps = additionalProps
    )
    val x = f(p) _
    x(children)
  }
}
