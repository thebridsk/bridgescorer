package com.example.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._
import js._
import org.scalajs.dom.raw.Element

class PopperPlacement( val value: String ) extends AnyVal
object PopperPlacement {
  val autoEnd = new PopperPlacement("auto-end")
  val autoStart = new PopperPlacement("auto-start")
  val auto = new PopperPlacement("auto")
  val bottomEnd = new PopperPlacement("bottom-end")
  val bottomStart = new PopperPlacement("bottom-start")
  val bottom = new PopperPlacement("bottom")
  val leftEnd = new PopperPlacement("left-end")
  val leftStart = new PopperPlacement("left-start")
  val left = new PopperPlacement("left")
  val rightEnd = new PopperPlacement("right-end")
  val rightStart = new PopperPlacement("right-start")
  val right = new PopperPlacement("right")
  val topEnd = new PopperPlacement("top-end")
  val topStart = new PopperPlacement("top-start")
  val top = new PopperPlacement("top")
}

@js.native
trait PopperProps extends js.Object {
  val anchorEl: js.UndefOr[ js.Object | js.Function0[Element] ] = js.native
  val container: js.UndefOr[ js.Object | js.Function0[Object] ] = js.native
  val disablePortal: js.UndefOr[Boolean] = js.native
  val keepMounted: js.UndefOr[Boolean] = js.native
  val modifiers: js.UndefOr[js.Object] = js.native
  val open: js.UndefOr[Boolean] = js.native
  val placement: js.UndefOr[PopperPlacement] = js.native
  val popperOptions: js.UndefOr[js.Object] = js.native
  val transition: js.UndefOr[Boolean] = js.native
}
object PopperProps {

    /**
     * @param p the object that will become the properties object
     * @param anchorEl This is the DOM element, or a function that returns the DOM element,
     *                  that may be used to set the position of the popover. The return
     *                  value will passed as the reference object of the Popper instance.
     * @param container A node, component instance, or function that returns either. The
     *                   container will passed to the Modal component. By default, it uses
     *                   the body of the anchorEl's top-level document object, so it's
     *                   simply document.body most of the time.
     * @param disablePortal Disable the portal behavior. The children stay within it's
     *                        parent DOM hierarchy.
     *                       Default: false
     * @param keepMounted Always keep the children in the DOM. This property can be useful
     *                     in SEO situation or when you want to maximize the responsiveness
     *                     of the Popper.
     * @param modifiers Popper.js is based on a "plugin-like" architecture, most of its
     *                   features are fully encapsulated "modifiers".
     *                   A modifier is a function that is called each time Popper.js needs
     *                   to compute the position of the popper. For this reason, modifiers
     *                   should be very performant to avoid bottlenecks. To learn how to
     *                   create a modifier, read the modifiers documentation.
     * @param open If true, the popper is visible.
     * @param placement Popper placement.
     *                   Default: bottom
     * @param popperOptions Options provided to the popper.js instance.
     * @param transition Help supporting a react-transition-group/Transition component.
     *                    Default: false
     */
    def apply(
        p: js.Object with js.Dynamic = js.Dynamic.literal(),
        anchorEl: js.UndefOr[ js.Object | js.Function0[Element] ] = js.undefined,
        container: js.UndefOr[ js.Object | js.Function0[Object] ] = js.undefined,
        disablePortal: js.UndefOr[Boolean] = js.undefined,
        keepMounted: js.UndefOr[Boolean] = js.undefined,
        modifiers: js.UndefOr[js.Object] = js.undefined,
        open: js.UndefOr[Boolean] = js.undefined,
        placement: js.UndefOr[PopperPlacement] = js.undefined,
        popperOptions: js.UndefOr[js.Object] = js.undefined,
        transition: js.UndefOr[Boolean] = js.undefined
    ): PopperProps = {
      anchorEl.foreach(v => p.updateDynamic("anchorEl")(v.asInstanceOf[js.Any]))
      container.foreach(v => p.updateDynamic("container")(v.asInstanceOf[js.Any]))
      disablePortal.foreach(p.updateDynamic("disablePortal")(_))
      keepMounted.foreach(p.updateDynamic("keepMounted")(_))
      modifiers.foreach(p.updateDynamic("modifiers")(_))
      open.foreach(p.updateDynamic("open")(_))
      placement.foreach(v => p.updateDynamic("placement")(v.value))
      popperOptions.foreach(p.updateDynamic("popperOptions")(_))
      transition.foreach(p.updateDynamic("transition")(_))

      p.asInstanceOf[PopperProps]
    }
}

object MuiPopper {
    @js.native @JSImport("@material-ui/core/Popper", JSImport.Default) private object Popper extends js.Any

    private val f = JsComponent[PopperProps, Children.Varargs, Null](Popper)

    /**
     * @param anchorEl This is the DOM element, or a function that returns the DOM element,
     *                  that may be used to set the position of the popover. The return
     *                  value will passed as the reference object of the Popper instance.
     * @param container A node, component instance, or function that returns either. The
     *                   container will passed to the Modal component. By default, it uses
     *                   the body of the anchorEl's top-level document object, so it's
     *                   simply document.body most of the time.
     * @param disablePortal Disable the portal behavior. The children stay within it's
     *                        parent DOM hierarchy.
     *                       Default: false
     * @param keepMounted Always keep the children in the DOM. This property can be useful
     *                     in SEO situation or when you want to maximize the responsiveness
     *                     of the Popper.
     * @param modifiers Popper.js is based on a "plugin-like" architecture, most of its
     *                   features are fully encapsulated "modifiers".
     *                   A modifier is a function that is called each time Popper.js needs
     *                   to compute the position of the popper. For this reason, modifiers
     *                   should be very performant to avoid bottlenecks. To learn how to
     *                   create a modifier, read the modifiers documentation.
     * @param open If true, the popper is visible.
     * @param placement Popper placement.
     *                   Default: bottom
     * @param popperOptions Options provided to the popper.js instance.
     * @param transition Help supporting a react-transition-group/Transition component.
     *                    Default: false
     */
    def apply(
        anchorEl: js.UndefOr[ js.Object | js.Function0[Element] ] = js.undefined,
        container: js.UndefOr[ js.Object | js.Function0[Object] ] = js.undefined,
        disablePortal: js.UndefOr[Boolean] = js.undefined,
        keepMounted: js.UndefOr[Boolean] = js.undefined,
        modifiers: js.UndefOr[js.Object] = js.undefined,
        open: js.UndefOr[Boolean] = js.undefined,
        placement: js.UndefOr[PopperPlacement] = js.undefined,
        popperOptions: js.UndefOr[js.Object] = js.undefined,
        transition: js.UndefOr[Boolean] = js.undefined
    )(
        children: CtorType.ChildArg*
    ) = {
      val p = PopperProps(
                anchorEl = anchorEl,
                container = container,
                disablePortal = disablePortal,
                keepMounted = keepMounted,
                modifiers = modifiers,
                open = open,
                placement = placement,
                popperOptions = popperOptions,
                transition = transition
              )
      val x = f(p) _
      x(children)
    }
}
