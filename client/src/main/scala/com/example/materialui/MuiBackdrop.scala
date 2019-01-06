package com.example.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
trait BackdropProps extends js.Object {
  /**
   * Override or extend the styles applied to the
   * component. See CSS API below for more details.
   */
  val classes:  js.UndefOr[js.Object] = js.native

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

object BackdropProps {
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
   */
  def apply(
      p: js.Object with js.Dynamic = js.Dynamic.literal(),
      classes:  js.UndefOr[js.Object] = js.undefined,
      invisible: js.UndefOr[Boolean] = js.undefined,
      open: js.UndefOr[Boolean] = js.undefined,
      transitionDuration: js.UndefOr[js.Object] = js.undefined
  ) = {
    classes.foreach(p.updateDynamic("classes")(_))
    invisible.foreach(p.updateDynamic("invisible")(_))
    open.foreach(p.updateDynamic("open")(_))
    transitionDuration.foreach(p.updateDynamic("transitionDuration")(_))

    p.asInstanceOf[BackdropProps]
  }
}

object MuiBackdrop {
  @js.native @JSImport("@material-ui/core/Backdrop", JSImport.Default) private object Backdrop extends js.Any

  private val f = JsComponent[js.Object, Children.Varargs, Null](Backdrop)

  /**
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
   */
  def set(
      p: js.Object with js.Dynamic = js.Dynamic.literal(),
      classes:  js.UndefOr[js.Object] = js.undefined,
      invisible: js.UndefOr[Boolean] = js.undefined,
      open: js.UndefOr[Boolean] = js.undefined,
      transitionDuration: js.UndefOr[js.Object] = js.undefined
  ): BackdropProps = {
    BackdropProps(p,classes,invisible,open,transitionDuration)
  }

  /**
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
   */
  def apply(
      classes:  js.UndefOr[js.Object] = js.undefined,
      invisible: js.UndefOr[Boolean] = js.undefined,
      open: js.UndefOr[Boolean] = js.undefined,
      transitionDuration: js.UndefOr[js.Object] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = {
    val p = set( classes=classes, invisible=invisible, open=open, transitionDuration=transitionDuration )
    val x = f(p) _
    x(children)
  }
}
