package com.example.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._


@js.native
trait TouchRippleProps extends js.Object {
  /**
   * If true, the ripple starts at the center of the
   * component rather than at the point of interaction.
   */
  val center: js.UndefOr[Boolean] = js.native

  /**
   * Override or extend the styles applied to the
   * component. See CSS API below for more details.
   */
  val classes:  js.UndefOr[js.Object] = js.native
}

object TouchRippleProps {
  /**
   * create a TouchRipple object
   *
   * @param center If true, the ripple starts at the center of the
   *               component rather than at the point of interaction.
   * @param classes Override or extend the styles applied to the
   *                component. See CSS API below for more details.
   */
  def apply(
      center: js.UndefOr[Boolean] = js.undefined,
      classes:  js.UndefOr[js.Object] = js.undefined
  ) = {
    val p = js.Dynamic.literal()

    center.foreach(p.updateDynamic("center")(_))
    classes.foreach(p.updateDynamic("classes")(_))

//    Combobox.logger.info("ComboboxComponentMessagesProperty: msgEmptyList="+msgEmptyList+", msgEmptyFilter="+msgEmptyFilter)

    p.asInstanceOf[TouchRippleProps]
  }
}

object MuiTouchRipple {
  @js.native @JSImport("@material-ui/core/TouchRipple", JSImport.Default) private object TouchRipple extends js.Any

  private val f = JsComponent[js.Object, Children.Varargs, Null](TouchRipple)

  /**
   *
   * @param center If true, the ripple starts at the center of the
   *               component rather than at the point of interaction.
   * @param classes Override or extend the styles applied to the
   *                component. See CSS API below for more details.
   */
  def set(
    center: js.UndefOr[Boolean] = js.undefined,
    classes:  js.UndefOr[js.Object] = js.undefined
  ): TouchRippleProps = {
    TouchRippleProps(center,classes)
  }

  /**
   *
   * @param center If true, the ripple starts at the center of the
   *               component rather than at the point of interaction.
   * @param classes Override or extend the styles applied to the
   *                component. See CSS API below for more details.
   */
  def apply(
    center: js.UndefOr[Boolean] = js.undefined,
    classes:  js.UndefOr[js.Object] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = {
    val p = set( center, classes )
    val x = f(p) _
    x(children)
  }
}
