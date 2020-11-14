package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.language.implicitConversions
import scala.scalajs.js.UndefOr

class CALMouseEvent(val value: js.Any) extends AnyVal
object CALMouseEvent {
  val onClick = new CALMouseEvent("onClick")
  val onMouseDown = new CALMouseEvent("onMouseDown")
  val onMouseUp = new CALMouseEvent("onMouseUp")
  val False = new CALMouseEvent(false)

  implicit def toJsAny(cv: CALMouseEvent): js.Any = cv.value

}

class CALTouchEvent(val value: js.Any) extends AnyVal
object CALTouchEvent {
  val onTouchStart = new CALTouchEvent("onTouchStart")
  val onTouchEnd = new CALTouchEvent("onTouchEnd")
  val False = new CALTouchEvent(false)

  implicit def toJsAny(cv: CALTouchEvent): js.Any = cv.value
}

@js.native
protected trait ClickAwayListenerPropsPrivate extends js.Any {
  @JSName("mouseEvent")
  val mouseEventInternal: js.UndefOr[js.Any] = js.native
  @JSName("touchEvent")
  val touchEventInternal: js.UndefOr[js.Any] = js.native
}

@js.native
trait ClickAwayListenerProps
    extends AdditionalProps
    with ClickAwayListenerPropsPrivate {
  val onClickAway: js.UndefOr[() => Unit] = js.native
}
object ClickAwayListenerProps extends PropsFactory[ClickAwayListenerProps] {

  implicit class WrapClickAwayListenerProps(
      private val p: ClickAwayListenerProps
  ) extends AnyVal {
    def mouseEvent: UndefOr[CALMouseEvent] =
      p.mouseEventInternal.map(s => new CALMouseEvent(s))

//    def mouseEvent_= (v: js.UndefOr[CALMouseEvent]) = { p.mouseEventInternal = v.map(pp => pp.value) }

    def touchEvent: UndefOr[CALTouchEvent] =
      p.touchEventInternal.map(s => new CALTouchEvent(s))

//    def touchEvent_= (v: js.UndefOr[CALTouchEvent]) = { p.touchEventInternal = v.map(pp => pp.value) }

  }

  /**
    * @param p the object that will become the properties object
    * @param mouseEvent The mouse event to listen to. You can disable
    *                    the listener by providing false.
    *                    Default: onMouseUp
    * @param onClickAway Callback fired when a "click away" event is detected.
    * @param touchEvent The touch event to listen to. You can disable the
    *                    listener by providing false.
    *                    Default: onTouchEnd
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: ClickAwayListenerProps](
      props: js.UndefOr[P] = js.undefined,
      mouseEvent: js.UndefOr[CALMouseEvent] = js.undefined,
      onClickAway: js.UndefOr[() => Unit] = js.undefined,
      touchEvent: js.UndefOr[CALTouchEvent] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p = get(props, additionalProps)

    mouseEvent.foreach(v => p.updateDynamic("mouseEvent")(v.value))
    onClickAway.foreach(p.updateDynamic("onClickAway")(_))
    touchEvent.foreach(v => p.updateDynamic("touchEvent")(v.value))

    p
  }
}

object MuiClickAwayListener {
  @js.native @JSImport(
    "@material-ui/core/ClickAwayListener",
    JSImport.Default
  ) private object ClickAwayListener extends js.Any

  protected val f = JsComponent[ClickAwayListenerProps, Children.Varargs, Null](
    ClickAwayListener
  ) // scalafix:ok ExplicitResultTypes; ReactComponent

  /**
    * @param mouseEvent The mouse event to listen to. You can disable
    *                    the listener by providing false.
    *                    Default: onMouseUp
    * @param onClickAway Callback fired when a "click away" event is detected.
    * @param touchEvent The touch event to listen to. You can disable the
    *                    listener by providing false.
    *                    Default: onTouchEnd
    * @param additionalProps a dictionary of additional properties
    */
  def apply(
      mouseEvent: js.UndefOr[CALMouseEvent] = js.undefined,
      onClickAway: js.UndefOr[() => Unit] = js.undefined,
      touchEvent: js.UndefOr[CALTouchEvent] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val p: ClickAwayListenerProps = ClickAwayListenerProps(
      mouseEvent = mouseEvent,
      onClickAway = onClickAway,
      touchEvent = touchEvent,
      additionalProps = additionalProps
    )
    val x = f(p) _
    x(children)
  }
}
