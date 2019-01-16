package com.example.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._

class CALMouseEvent( val value: js.Any ) extends AnyVal
object CALMouseEvent {
  val onClick = new CALMouseEvent("onClick")
  val onMouseDown = new CALMouseEvent("onMouseDown")
  val onMouseUp = new CALMouseEvent("onMouseUp")
  val False = new CALMouseEvent( false )
}

class CALTouchEvent( val value: js.Any ) extends AnyVal
object CALTouchEvent {
  val onTouchStart = new CALTouchEvent("onTouchStart")
  val onTouchEnd = new CALTouchEvent("onTouchEnd")
  val False = new CALTouchEvent( false )
}

@js.native
trait ClickAwayListenerProps extends js.Object {
  val mouseEvent: js.UndefOr[CALMouseEvent] = js.native
  val onClickAway: js.UndefOr[() => Unit] = js.native
  val touchEvent: js.UndefOr[CALTouchEvent] = js.native
}
object ClickAwayListenerProps {

    /**
     * @param p the object that will become the properties object
     * @param mouseEvent The mouse event to listen to. You can disable
     *                    the listener by providing false.
     *                    Default: onMouseUp
     * @param onClickAway Callback fired when a "click away" event is detected.
     * @param touchEvent The touch event to listen to. You can disable the
     *                    listener by providing false.
     *                    Default: onTouchEnd
     */
    def apply(
        p: js.Object with js.Dynamic = js.Dynamic.literal(),
        mouseEvent: js.UndefOr[CALMouseEvent] = js.undefined,
        onClickAway: js.UndefOr[() => Unit] = js.undefined,
        touchEvent: js.UndefOr[CALTouchEvent] = js.undefined
    ): ClickAwayListenerProps = {
      mouseEvent.foreach(v => p.updateDynamic("mouseEvent")(v.value))
      onClickAway.foreach(p.updateDynamic("onClickAway")(_))
      touchEvent.foreach(v => p.updateDynamic("touchEvent")(v.value))

      p.asInstanceOf[ClickAwayListenerProps]
    }
}

object MuiClickAwayListener {
    @js.native @JSImport("@material-ui/core/ClickAwayListener", JSImport.Default) private object ClickAwayListener extends js.Any

    private val f = JsComponent[ClickAwayListenerProps, Children.Varargs, Null](ClickAwayListener)

    /**
     * @param mouseEvent The mouse event to listen to. You can disable
     *                    the listener by providing false.
     *                    Default: onMouseUp
     * @param onClickAway Callback fired when a "click away" event is detected.
     * @param touchEvent The touch event to listen to. You can disable the
     *                    listener by providing false.
     *                    Default: onTouchEnd
     */
    def apply(
        mouseEvent: js.UndefOr[CALMouseEvent] = js.undefined,
        onClickAway: js.UndefOr[() => Unit] = js.undefined,
        touchEvent: js.UndefOr[CALTouchEvent] = js.undefined
    )(
        children: CtorType.ChildArg*
    ) = {
      val p = ClickAwayListenerProps(
                mouseEvent = mouseEvent,
                onClickAway = onClickAway,
                touchEvent = touchEvent
              )
      val x = f(p) _
      x(children)
    }
}
