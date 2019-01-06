package com.example.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._
import org.scalajs.dom.raw.Element
//import com.example.materialui.util.{ JsNumber => _, _ }
import scala.language.implicitConversions

class AnchorOriginHorizontalValue( val value: String ) extends AnyVal
object AnchorOriginHorizontalValue {
  val left = new AnchorOriginHorizontalValue("left")
  val center = new AnchorOriginHorizontalValue("center")
  val right = new AnchorOriginHorizontalValue("right")

  val default = left

  implicit def wrapHorizontal( v: AnchorOriginHorizontalValue ) = v.value
}

class AnchorOriginVerticalValue( val value: String ) extends AnyVal
object AnchorOriginVerticalValue {
  val top = new AnchorOriginVerticalValue("top")
  val center = new AnchorOriginVerticalValue("center")
  val bottom = new AnchorOriginVerticalValue("bottom")

  val default = top

  implicit def wrapVertical( v: AnchorOriginVerticalValue ) = v.value
}

import js._

@js.native
trait AnchorOrigin extends js.Object {
    var horizontal: js.UndefOr[JsNumber|AnchorOriginHorizontalValue] = js.native
    var vertical: js.UndefOr[JsNumber|AnchorOriginHorizontalValue] = js.native
}

object AnchorOrigin {
  def apply(
           horizontal: js.UndefOr[JsNumber|AnchorOriginHorizontalValue],
           vertical: js.UndefOr[JsNumber|AnchorOriginHorizontalValue]
      ) = {
    val p = js.Dynamic.literal()

    val r = p.asInstanceOf[AnchorOrigin]

    horizontal.foreach( r.horizontal = _)
    vertical.foreach( r.vertical = _)
    r
  }
}

@js.native
trait AnchorPosition extends js.Object {
    val left: js.UndefOr[Double]
    val right: js.UndefOr[Double]
}
object AnchorPosition {
  def apply(
             left: js.UndefOr[Double],
             right: js.UndefOr[Double]
  ) = {
    val p = js.Dynamic.literal()

    left.foreach( p.dynamicUpdate("left")(_))
    right.foreach( p.dynamicUpdate("right")(_))

    p.asInstanceOf[AnchorPosition]
  }
}

class AnchorReference( val value: String ) extends AnyVal
object AnchorReference {
  def apply( v: String ) = new AnchorReference(v)
}

@js.native
trait PopoverProps extends ModalProps {
  import js._
  val action: js.UndefOr[js.Function1[js.Object,Unit]] = js.native
  var anchorEl: js.UndefOr[ js.Object | js.Function0[Element] ] = js.native
  val anchorOrigin: js.UndefOr[AnchorOrigin] = js.native
  val anchorPosition: js.UndefOr[AnchorPosition] = js.native
  val anchorReference: js.UndefOr[AnchorReference] = js.native
  val elevation: js.UndefOr[Double] = js.native
  val getContentAnchorEl: js.UndefOr[js.Function0[js.Object]] = js.native
  val marginThreshold: js.UndefOr[Double] = js.native
  val modalClasses: js.UndefOr[ModalProps] = js.native
  val onEnter: js.UndefOr[js.Function0[Unit]] = js.native
  val onEntered: js.UndefOr[js.Function0[Unit]] = js.native
  val onEntering: js.UndefOr[js.Function0[Unit]] = js.native
  val onExit: js.UndefOr[js.Function0[Unit]] = js.native
  val onExited: js.UndefOr[js.Function0[Unit]] = js.native
  val onExiting: js.UndefOr[js.Function0[Unit]] = js.native
  val paperProps: js.UndefOr[PaperProps] = js.native
  val transformOrigin: js.UndefOr[js.Object] = js.native
  val transitionComponent: js.UndefOr[js.Object] = js.native
  var transitionDuration: js.UndefOr[JsNumber | TransitionDuration] = js.native
  val transitionProps: js.UndefOr[js.Object] = js.native
}
object PopoverProps {
  import js._
  /**
   * @param p the object that will become the properties object
   *
   * @param action This is callback property. It's called by the component on mount.
   *                This is useful when you want to trigger an action programmatically.
   *                It currently only supports updatePosition() action.
   *                Signature:
   *                  function(actions: object) => void
   *                  actions: This object contains all possible actions that
   *                           can be triggered programmatically.
   * @param anchorEl This is the DOM element, or a function that returns the DOM element,
   *                  that may be used to set the position of the popover.
   * @param anchorOrigin This is the point on the anchor where the popover's anchorEl will
   *                      attach to. This is not used when the anchorReference is 'anchorPosition'.
   *                      Options: vertical: [top, center, bottom];
   *                               horizontal: [left, center, right].
   *                      Default: { vertical: 'top', horizontal: 'left'}
   * @param anchorPosition This is the position that may be used to set the position of the popover.
   *                        The coordinates are relative to the application's client area.
   * @param anchorReference Default: anchorEl
   * @param elevation The elevation of the popover.  Default: 8
   * @param getContentAnchorEl This function is called in order to retrieve the content
   *                            anchor element. It's the opposite of the anchorEl property.
   *                            The content anchor element should be an element inside the
   *                            popover. It's used to correctly scroll and set the position
   *                            of the popover. The positioning strategy tries to make the
   *                            content anchor element just above the anchor element.
   * @param marginThreshold classes property applied to the Modal element.
   * @param onEnter Callback fired before the component is entering.
   * @param onEntered Callback fired when the component has entered.
   * @param onEntering Callback fired when the component is entering.
   * @param onExit Callback fired before the component is exiting.
   * @param onExited Callback fired when the component has exited.
   * @param onExiting Callback fired when the component is exiting.
   * @param PaperProps Properties applied to the Paper element.
   * @param transformOrigin This is the point on the popover which will attach to the
   *                         anchor's origin. Options:
   *                           vertical: [top, center, bottom, x(px)];
   *                           horizontal: [left, center, right, x(px)].
   *                         Default {vertical: 'top', horizontal: 'left'}
   * @param TransitionComponent The component used for the transition.
   *                             Default: Grow
   * @param transitionDuration Set to 'auto' to automatically calculate transition
   *                            time based on height.
   *                            Default: 'auto'
   * @param TransitionProps Properties applied to the Transition element.
   *
   * From MuiModel
   *
   * @param BackdropComponent A backdrop component. This property enables custom
   *                           backdrop rendering.
   *                           Default: Backdrop
   * @param BackdropProps Properties applied to the Backdrop element.
   * @param classes Override or extend the styles applied to the component.
   *                 See CSS API below for more details.
   * @param container A node, component instance, or function that returns either.
   *                   The container will have the portal children appended to it.
   * @param disableAutoFocus If true, the modal will not automatically shift focus
   *                          to itself when it opens, and replace it to the last focused
   *                          element when it closes. This also works correctly with any
   *                          modal children that have the disableAutoFocus prop.
   *                          Generally this should never be set to true as it makes the
   *                          modal less accessible to assistive technologies, like screen readers.
   *                          Default: false
   * @param disableBackdropClick If true, clicking the backdrop will not fire any callback.
   *                              Default: false
   * @param disableEnforceFocus If true, the modal will not prevent focus from leaving the modal
   *                             while open.  Generally this should never be set to true as it
   *                             makes the modal less accessible to assistive technologies,
   *                             like screen readers.
   *                             Default: false
   * @param disableEscapeKeyDown If true, hitting escape will not fire any callback.
   *                               Default: false
   * @param disablePortal Disable the portal behavior. The children stay within it's
   *                       parent DOM hierarchy.
   *                       Default: false
   * @param disableRestoreFocus If true, the modal will not restore focus to previously
   *                             focused element once modal is hidden.
   *                             Default: false
   * @param hideBackdrop If true, the backdrop is not rendered.
   *                      Default: false
   * @param keepMounted Always keep the children in the DOM. This property can be useful
   *                     in SEO situation or when you want to maximize the responsiveness
   *                     of the Modal.
   *                     Default: false
   * @param manager A modal manager used to track and manage the state of open Modals.
   *                 This enables customizing how modals interact within a container.
   *                 Default: new ModalManager()
   * @param onBackdropClick Callback fired when the backdrop is clicked.
   * @param onClose Callback fired when the component requests to be closed. The reason
   *                 parameter can optionally be used to control the response to onClose.
   *                 Signature:
   *                   function(event: object, reason: string) => void
   *                   event: The event source of the callback
   *                   reason: Can be:"escapeKeyDown", "backdropClick"
   * @param onEscapeKeyDown Callback fired when the escape key is pressed,
   *                         disableEscapeKeyDown is false and the modal is in focus.
   * @param onRendered Callback fired once the children has been mounted into the container.
   *                    It signals that the open={true} property took effect.
   * @param open If true, the modal is open.
   */
  def apply(
      p: js.Object with js.Dynamic = js.Dynamic.literal(),

      action: js.UndefOr[js.Function1[js.Object,Unit]] = js.undefined,
      anchorEl: js.UndefOr[ Element | js.Function0[Element] ] = js.undefined,
      anchorOrigin: js.UndefOr[AnchorOrigin] = js.undefined,
      anchorPosition: js.UndefOr[AnchorPosition] = js.undefined,
      anchorReference: js.UndefOr[AnchorReference] = js.undefined,
      elevation: js.UndefOr[Double] = js.undefined,
      getContentAnchorEl: js.UndefOr[js.Function0[js.Object]] = js.undefined,
      marginThreshold: js.UndefOr[Double] = js.undefined,
      modalClasses: js.UndefOr[ModalProps] = js.undefined,
      onEnter: js.UndefOr[js.Function0[Unit]] = js.undefined,
      onEntered: js.UndefOr[js.Function0[Unit]] = js.undefined,
      onEntering: js.UndefOr[js.Function0[Unit]] = js.undefined,
      onExit: js.UndefOr[js.Function0[Unit]] = js.undefined,
      onExited: js.UndefOr[js.Function0[Unit]] = js.undefined,
      onExiting: js.UndefOr[js.Function0[Unit]] = js.undefined,
      paperProps: js.UndefOr[PaperProps] = js.undefined,
      transformOrigin: js.UndefOr[js.Object] = js.undefined,
      transitionComponent: js.UndefOr[js.Object] = js.undefined,
      transitionDuration: js.UndefOr[ JsNumber | TransitionDuration ] = js.undefined,
      transitionProps: js.UndefOr[js.Object] = js.undefined,

      backdropComponent: js.UndefOr[js.Object] = js.undefined,
      backdropProps: js.UndefOr[BackdropProps] = js.undefined,
      classes: js.UndefOr[js.Object] = js.undefined,
      container: js.UndefOr[js.Object] = js.undefined,
      disableAutoFocus: js.UndefOr[Boolean] = js.undefined,
      disableBackdropClick: js.UndefOr[Boolean] = js.undefined,
      disableEnforceFocus: js.UndefOr[Boolean] = js.undefined,
      disableEscapeKeyDown: js.UndefOr[Boolean] = js.undefined,
      disablePortal: js.UndefOr[Boolean] = js.undefined,
      disableRestoreFocus: js.UndefOr[Boolean] = js.undefined,
      hideBackdrop: js.UndefOr[Boolean] = js.undefined,
      keepMounted: js.UndefOr[Boolean] = js.undefined,
      manager: js.UndefOr[js.Object] = js.undefined,
      onBackdropClick: js.UndefOr[js.Function0[Unit]] = js.undefined,
//      onClose: js.UndefOr[js.Function2[js.Object,String,Unit]] = js.undefined,
      onClose: js.UndefOr[() => Unit] = js.undefined,
      onEscapeKeyDown: js.UndefOr[js.Function0[Unit]] = js.undefined,
      onRendered: js.UndefOr[js.Function0[Unit]] = js.undefined,
      open: js.UndefOr[Boolean] = js.undefined,

  ) = {
    val mp = ModalProps(
        p,
        backdropComponent,
        backdropProps,
        classes,
        container,
        disableAutoFocus,
        disableBackdropClick,
        disableEnforceFocus,
        disableEscapeKeyDown,
        disablePortal,
        disableRestoreFocus,
        hideBackdrop,
        keepMounted,
        manager,
        onBackdropClick,
        onClose,
        onEscapeKeyDown,
        onRendered,
        open
    )

    action.foreach(p.updateDynamic("action")(_))
    anchorEl.foreach(v => v.asInstanceOf[Any] match {
      case e: Element => p.updateDynamic("anchorEl")(_)
      case o: js.Object => p.updateDynamic("anchorEl")(_)
    })
    anchorOrigin.foreach(p.updateDynamic("anchorOrigin")(_))
    anchorPosition.foreach(p.updateDynamic("anchorPosition")(_))
    anchorReference.foreach(v => p.updateDynamic("anchorReference")(v.value))
    elevation.foreach(p.updateDynamic("elevation")(_))
    getContentAnchorEl.foreach(p.updateDynamic("getContentAnchorEl")(_))
    marginThreshold.foreach(p.updateDynamic("marginThreshold")(_))
    modalClasses.foreach(p.updateDynamic("modalClasses")(_))
    onEnter.foreach(p.updateDynamic("onEnter")(_))
    onEntered.foreach(p.updateDynamic("onEntered")(_))
    onEntering.foreach(p.updateDynamic("onEntering")(_))
    onExit.foreach(p.updateDynamic("onExit")(_))
    onExited.foreach(p.updateDynamic("onExited")(_))
    onExiting.foreach(p.updateDynamic("onExiting")(_))
    paperProps.foreach(p.updateDynamic("PaperProps")(_))
    transformOrigin.foreach(p.updateDynamic("transformOrigin")(_))
    transitionComponent.foreach(p.updateDynamic("TransitionComponent")(_))
    transitionProps.foreach(p.updateDynamic("TransitionProps")(_))

    val r = p.asInstanceOf[PopoverProps]
    transitionDuration.foreach( r.transitionDuration=_ )
    anchorEl.foreach( r.anchorEl = _ )

    r
  }
}

object MuiPopover {
    @js.native @JSImport("@material-ui/core/Popover", JSImport.Default) private object Popover extends js.Any

    private val f = JsComponent[js.Object, Children.Varargs, Null](Popover)

    /**
     * @param BackdropComponent A backdrop component. This property enables custom
     *                           backdrop rendering.
     *                           Default: Backdrop
     * @param BackdropProps Properties applied to the Backdrop element.
     * @param classes Override or extend the styles applied to the component.
     *                 See CSS API below for more details.
     * @param container A node, component instance, or function that returns either.
     *                   The container will have the portal children appended to it.
     * @param disableAutoFocus If true, the modal will not automatically shift focus
     *                          to itself when it opens, and replace it to the last focused
     *                          element when it closes. This also works correctly with any
     *                          modal children that have the disableAutoFocus prop.
     *                          Generally this should never be set to true as it makes the
     *                          modal less accessible to assistive technologies, like screen readers.
     *                          Default: false
     * @param disableBackdropClick If true, clicking the backdrop will not fire any callback.
     *                              Default: false
     * @param disableEnforceFocus If true, the modal will not prevent focus from leaving the modal
     *                             while open.  Generally this should never be set to true as it
     *                             makes the modal less accessible to assistive technologies,
     *                             like screen readers.
     *                             Default: false
     * @paramdisableEscapeKeyDown If true, hitting escape will not fire any callback.
     *                               Default: false
     * @param disablePortal Disable the portal behavior. The children stay within it's
     *                       parent DOM hierarchy.
     *                       Default: false
     * @param disableRestoreFocus If true, the modal will not restore focus to previously
     *                             focused element once modal is hidden.
     *                             Default: false
     * @param hideBackdrop If true, the backdrop is not rendered.
     *                      Default: false
     * @param keepMounted Always keep the children in the DOM. This property can be useful
     *                     in SEO situation or when you want to maximize the responsiveness
     *                     of the Modal.
     *                     Default: false
     * @param manager A modal manager used to track and manage the state of open Modals.
     *                 This enables customizing how modals interact within a container.
     *                 Default: new ModalManager()
     * @param onBackdropClick Callback fired when the backdrop is clicked.
     * @param onClose Callback fired when the component requests to be closed. The reason
     *                 parameter can optionally be used to control the response to onClose.
     *                 Signature:
     *                   function(event: object, reason: string) => void
     *                   event: The event source of the callback
     *                   reason: Can be:"escapeKeyDown", "backdropClick"
     * @param onEscapeKeyDown Callback fired when the escape key is pressed,
     *                         disableEscapeKeyDown is false and the modal is in focus.
     * @param onRendered Callback fired once the children has been mounted into the container.
     *                    It signals that the open={true} property took effect.
     * @param open If true, the modal is open.
     */
    def apply(

      action: js.UndefOr[js.Function1[js.Object,Unit]] = js.undefined,
      anchorEl: js.UndefOr[ Element | js.Function0[Element] ] = js.undefined,
      anchorOrigin: js.UndefOr[AnchorOrigin] = js.undefined,
      anchorPosition: js.UndefOr[AnchorPosition] = js.undefined,
      anchorReference: js.UndefOr[AnchorReference] = js.undefined,
      elevation: js.UndefOr[Double] = js.undefined,
      getContentAnchorEl: js.UndefOr[js.Function0[js.Object]] = js.undefined,
      marginThreshold: js.UndefOr[Double] = js.undefined,
      modalClasses: js.UndefOr[ModalProps] = js.undefined,
      onEnter: js.UndefOr[js.Function0[Unit]] = js.undefined,
      onEntered: js.UndefOr[js.Function0[Unit]] = js.undefined,
      onEntering: js.UndefOr[js.Function0[Unit]] = js.undefined,
      onExit: js.UndefOr[js.Function0[Unit]] = js.undefined,
      onExited: js.UndefOr[js.Function0[Unit]] = js.undefined,
      onExiting: js.UndefOr[js.Function0[Unit]] = js.undefined,
      paperProps: js.UndefOr[PaperProps] = js.undefined,
      transformOrigin: js.UndefOr[js.Object] = js.undefined,
      transitionComponent: js.UndefOr[js.Object] = js.undefined,
      transitionDuration: js.UndefOr[ JsNumber | TransitionDuration ] = js.undefined,
      transitionProps: js.UndefOr[js.Object] = js.undefined,

      backdropComponent: js.UndefOr[js.Object] = js.undefined,
      backdropProps: js.UndefOr[BackdropProps] = js.undefined,
      classes: js.UndefOr[js.Object] = js.undefined,
      container: js.UndefOr[js.Object] = js.undefined,
      disableAutoFocus: js.UndefOr[Boolean] = js.undefined,
      disableBackdropClick: js.UndefOr[Boolean] = js.undefined,
      disableEnforceFocus: js.UndefOr[Boolean] = js.undefined,
      disableEscapeKeyDown: js.UndefOr[Boolean] = js.undefined,
      disablePortal: js.UndefOr[Boolean] = js.undefined,
      disableRestoreFocus: js.UndefOr[Boolean] = js.undefined,
      hideBackdrop: js.UndefOr[Boolean] = js.undefined,
      keepMounted: js.UndefOr[Boolean] = js.undefined,
      manager: js.UndefOr[js.Object] = js.undefined,
      onBackdropClick: js.UndefOr[js.Function0[Unit]] = js.undefined,
//      onClose: js.UndefOr[js.Function2[js.Object,String,Unit]] = js.undefined,
      onClose: js.UndefOr[() => Unit] = js.undefined,
      onEscapeKeyDown: js.UndefOr[js.Function0[Unit]] = js.undefined,
      onRendered: js.UndefOr[js.Function0[Unit]] = js.undefined,
      open: js.UndefOr[Boolean] = js.undefined,
    )(
        children: CtorType.ChildArg*
    ) = {
      val p = PopoverProps(
                  js.Dynamic.literal(),

                  action,
                  anchorEl,
                  anchorOrigin,
                  anchorPosition,
                  anchorReference,
                  elevation,
                  getContentAnchorEl,
                  marginThreshold,
                  modalClasses,
                  onEnter,
                  onEntered,
                  onEntering,
                  onExit,
                  onExited,
                  onExiting,
                  paperProps,
                  transformOrigin,
                  transitionComponent,
                  transitionDuration,
                  transitionProps,

                  backdropComponent,
                  backdropProps,
                  classes,
                  container,
                  disableAutoFocus,
                  disableBackdropClick,
                  disableEnforceFocus,
                  disableEscapeKeyDown,
                  disablePortal,
                  disableRestoreFocus,
                  hideBackdrop,
                  keepMounted,
                  manager,
                  onBackdropClick,
                  onClose,
                  onEscapeKeyDown,
                  onRendered,
                  open,
              )
      val x = f(p) _
      x(children)
    }
}
