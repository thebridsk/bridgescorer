package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._
import org.scalajs.dom.raw.Element
//import com.github.thebridsk.materialui.util.{ JsNumber => _, _ }
import scala.language.implicitConversions

class AnchorOriginHorizontalValue(val value: String) extends AnyVal
object AnchorOriginHorizontalValue {
  val left = new AnchorOriginHorizontalValue("left")
  val center = new AnchorOriginHorizontalValue("center")
  val right = new AnchorOriginHorizontalValue("right")

  val default = left

  implicit def wrapHorizontal(v: AnchorOriginHorizontalValue) = v.value
}

class AnchorOriginVerticalValue(val value: String) extends AnyVal
object AnchorOriginVerticalValue {
  val top = new AnchorOriginVerticalValue("top")
  val center = new AnchorOriginVerticalValue("center")
  val bottom = new AnchorOriginVerticalValue("bottom")

  val default = top

  implicit def wrapVertical(v: AnchorOriginVerticalValue) = v.value
}

import js._

@js.native
protected trait AnchorOriginPrivate extends js.Object {
  @JSName("horizontal")
  val horizontalInternal: js.UndefOr[JsNumber | String] = js.native
  @JSName("vertical")
  val verticalInternal: js.UndefOr[JsNumber | String] = js.native
}

@js.native
trait AnchorOrigin
    extends js.Object
    with AnchorOriginPrivate
    with AdditionalProps {}

object AnchorOrigin {

  implicit class WrapAnchorOrigin(val p: AnchorOrigin) extends AnyVal {

    def horizontal = p.horizontalInternal.map { v =>
      val r: JsNumber | AnchorOriginHorizontalValue =
        if (js.typeOf(v.asInstanceOf[js.Any]) == "string") {
          val s: String = v.asInstanceOf[String]
          new AnchorOriginHorizontalValue(s)
        } else {
          v.asInstanceOf[JsNumber | AnchorOriginHorizontalValue]
        }
      r
    }

//    def horizontal_= (v: JsNumber) = {
//      p.horizontalInternal = v
//    }
//
//    def horizontal_= (v: AnchorOriginHorizontalValue) = {
//      p.horizontalInternal = v.value
//    }

    def vertical = p.verticalInternal.map { v =>
      val r: JsNumber | AnchorOriginVerticalValue =
        if (js.typeOf(v.asInstanceOf[js.Any]) == "string") {
          val s: String = v.asInstanceOf[String]
          new AnchorOriginVerticalValue(s)
        } else {
          v.asInstanceOf[JsNumber | AnchorOriginVerticalValue]
        }
      r
    }

//    def vertical_= (v: JsNumber) = {
//      p.verticalInternal = v
//    }
//
//    def vertical_= (v: AnchorOriginVerticalValue) = {
//      p.verticalInternal = v.value
//    }

  }

  def apply(
      horizontal: js.UndefOr[JsNumber | AnchorOriginHorizontalValue],
      vertical: js.UndefOr[JsNumber | AnchorOriginVerticalValue]
  ) = {
    val p = new js.Object().asInstanceOf[AnchorOrigin]

    horizontal.foreach(
      v => p.updateDynamic("horizontal")(v.asInstanceOf[js.Any])
    )
    vertical.foreach(v => p.updateDynamic("vertical")(v.asInstanceOf[js.Any]))

    p
  }
}

@js.native
trait AnchorPosition extends js.Object {
  val left: js.UndefOr[Double] = js.native
  val right: js.UndefOr[Double] = js.native
}
object AnchorPosition {
  def apply(
      left: js.UndefOr[Double],
      right: js.UndefOr[Double]
  ) = {
    val p = js.Dynamic.literal()

    left.foreach(p.dynamicUpdate("left")(_))
    right.foreach(p.dynamicUpdate("right")(_))

    p.asInstanceOf[AnchorPosition]
  }
}

class AnchorReference(val value: String) extends AnyVal
object AnchorReference {
  def apply(v: String) = new AnchorReference(v)
}

@js.native
protected trait PopoverPropsPrivate extends js.Any {
  @JSName("anchorEl")
  val anchorElInternal: js.UndefOr[js.Any] = js.native
  @JSName("anchorReference")
  val anchorReferenceInternal: js.UndefOr[String] = js.native
  @JSName("transitionDuration")
  val transitionDurationInternal: js.UndefOr[js.Any] = js.native
}

@js.native
trait PopoverProps extends ModalProps with PopoverPropsPrivate {
  import js._
  val action: js.UndefOr[js.Object => Unit] = js.native
  val anchorOrigin: js.UndefOr[AnchorOrigin] = js.native
  val anchorPosition: js.UndefOr[AnchorPosition] = js.native
  val elevation: js.UndefOr[Double] = js.native
  val getContentAnchorEl: js.UndefOr[() => js.Object] = js.native
  val marginThreshold: js.UndefOr[Double] = js.native
  val modalClasses: js.UndefOr[ModalProps] = js.native
  val onEnter: js.UndefOr[() => Unit] = js.native
  val onEntered: js.UndefOr[() => Unit] = js.native
  val onEntering: js.UndefOr[() => Unit] = js.native
  val onExit: js.UndefOr[() => Unit] = js.native
  val onExited: js.UndefOr[() => Unit] = js.native
  val onExiting: js.UndefOr[() => Unit] = js.native
  val paperProps: js.UndefOr[PaperProps] = js.native
  val transformOrigin: js.UndefOr[js.Object] = js.native
  val transitionComponent: js.UndefOr[js.Object] = js.native
  val transitionProps: js.UndefOr[js.Object] = js.native

//  val anchorEl: js.UndefOr[AnchorElement] = js.native
//  val anchorReference: js.UndefOr[AnchorReference] = js.native
//  val transitionDuration: js.UndefOr[JsNumber | TransitionDuration] = js.native
}
object PopoverProps extends PropsFactory[PopoverProps] {
  import js._

  implicit class WrapPopoverProps(val p: PopoverProps) extends AnyVal {

    def anchorReference =
      p.anchorReferenceInternal.map(s => new AnchorReference(s))

//    def anchorReference_= (v: js.UndefOr[AnchorReference]): Unit = {
//      v.map{ vv=>p.anchorReferenceInternal=vv.value; None }.
//        orElse{ p.anchorReferenceInternal=js.undefined; None }
//    }

    def anchorEl = p.anchorElInternal.map { v =>
      v.asInstanceOf[AnchorElement]
    }

//    def anchorEl_= (v: js.UndefOr[AnchorElement]): Unit = {
//      v.map{ vv=>p.anchorElInternal=vv.asInstanceOf[js.Any]; None }.
//        orElse{ p.anchorElInternal=js.undefined; None }
//    }

    def transitionDuration = p.transitionDurationInternal.map { v =>
      v.asInstanceOf[JsNumber | TransitionDuration]
    }

//    def transitionDuration_= (v: js.UndefOr[JsNumber|TransitionDuration]): Unit = {
//      v.map{ vv=>p.transitionDurationInternal=vv.asInstanceOf[js.Any]; None }.
//        orElse{ p.transitionDurationInternal=js.undefined; None }
//    }

  }

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
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: PopoverProps](
      props: js.UndefOr[P] = js.undefined,
      action: js.UndefOr[js.Object => Unit] = js.undefined,
      anchorEl: js.UndefOr[AnchorElement] = js.undefined,
      anchorOrigin: js.UndefOr[AnchorOrigin] = js.undefined,
      anchorPosition: js.UndefOr[AnchorPosition] = js.undefined,
      anchorReference: js.UndefOr[AnchorReference] = js.undefined,
      elevation: js.UndefOr[Double] = js.undefined,
      getContentAnchorEl: js.UndefOr[() => js.Object] = js.undefined,
      marginThreshold: js.UndefOr[Double] = js.undefined,
      modalClasses: js.UndefOr[ModalProps] = js.undefined,
      onEnter: js.UndefOr[() => Unit] = js.undefined,
      onEntered: js.UndefOr[() => Unit] = js.undefined,
      onEntering: js.UndefOr[() => Unit] = js.undefined,
      onExit: js.UndefOr[() => Unit] = js.undefined,
      onExited: js.UndefOr[() => Unit] = js.undefined,
      onExiting: js.UndefOr[() => Unit] = js.undefined,
      paperProps: js.UndefOr[PaperProps] = js.undefined,
      transformOrigin: js.UndefOr[js.Object] = js.undefined,
      transitionComponent: js.UndefOr[js.Object] = js.undefined,
      transitionDuration: js.UndefOr[JsNumber | TransitionDuration] =
        js.undefined,
      transitionProps: js.UndefOr[js.Object] = js.undefined,
      backdropComponent: js.UndefOr[js.Object] = js.undefined,
      backdropProps: js.UndefOr[BackdropProps] = js.undefined,
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
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
      onBackdropClick: js.UndefOr[() => Unit] = js.undefined,
//      onClose: js.UndefOr[js.Function2[js.Object,String,Unit]] = js.undefined,
      onClose: js.UndefOr[() => Unit] = js.undefined,
      onEscapeKeyDown: js.UndefOr[() => Unit] = js.undefined,
      onRendered: js.UndefOr[() => Unit] = js.undefined,
      open: js.UndefOr[Boolean] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ) = {
    val p: P = ModalProps(
      props,
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
      additionalProps
    )

    action.foreach(p.updateDynamic("action")(_))
    anchorEl.foreach(v => p.updateDynamic("anchorEl")(v.asInstanceOf[js.Any]))
    anchorPosition.foreach(p.updateDynamic("anchorPosition")(_))
    anchorReference.foreach(v => p.updateDynamic("anchorEl")(v.value))
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
    paperProps.foreach(p.updateDynamic("paperProps")(_))
    transformOrigin.foreach(p.updateDynamic("transformOrigin")(_))
    transitionComponent.foreach(p.updateDynamic("transitionComponent")(_))
    transitionProps.foreach(p.updateDynamic("transitionProps")(_))

    transitionDuration.foreach(
      v => p.updateDynamic("transitionDuration")(v.asInstanceOf[js.Any])
    )

    p
  }
}

object MuiPopover extends ComponentFactory[PopoverProps] {
  @js.native @JSImport("@material-ui/core/Popover", JSImport.Default) private object Popover
      extends js.Any

  protected val f = JsComponent[PopoverProps, Children.Varargs, Null](Popover)

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
    * @param additionalProps a dictionary of additional properties
    */
  def apply(
      action: js.UndefOr[js.Object => Unit] = js.undefined,
      anchorEl: js.UndefOr[AnchorElement] = js.undefined,
      anchorOrigin: js.UndefOr[AnchorOrigin] = js.undefined,
      anchorPosition: js.UndefOr[AnchorPosition] = js.undefined,
      anchorReference: js.UndefOr[AnchorReference] = js.undefined,
      elevation: js.UndefOr[Double] = js.undefined,
      getContentAnchorEl: js.UndefOr[() => js.Object] = js.undefined,
      marginThreshold: js.UndefOr[Double] = js.undefined,
      modalClasses: js.UndefOr[ModalProps] = js.undefined,
      onEnter: js.UndefOr[() => Unit] = js.undefined,
      onEntered: js.UndefOr[() => Unit] = js.undefined,
      onEntering: js.UndefOr[() => Unit] = js.undefined,
      onExit: js.UndefOr[() => Unit] = js.undefined,
      onExited: js.UndefOr[() => Unit] = js.undefined,
      onExiting: js.UndefOr[() => Unit] = js.undefined,
      paperProps: js.UndefOr[PaperProps] = js.undefined,
      transformOrigin: js.UndefOr[js.Object] = js.undefined,
      transitionComponent: js.UndefOr[js.Object] = js.undefined,
      transitionDuration: js.UndefOr[JsNumber | TransitionDuration] =
        js.undefined,
      transitionProps: js.UndefOr[js.Object] = js.undefined,
      backdropComponent: js.UndefOr[js.Object] = js.undefined,
      backdropProps: js.UndefOr[BackdropProps] = js.undefined,
      classes: js.UndefOr[js.Dictionary[String]] = js.undefined,
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
      onBackdropClick: js.UndefOr[() => Unit] = js.undefined,
//      onClose: js.UndefOr[js.Function2[js.Object,String,Unit]] = js.undefined,
      onClose: js.UndefOr[() => Unit] = js.undefined,
      onEscapeKeyDown: js.UndefOr[() => Unit] = js.undefined,
      onRendered: js.UndefOr[() => Unit] = js.undefined,
      open: js.UndefOr[Boolean] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = {
    val p: PopoverProps = PopoverProps(
      action = action,
      anchorEl = anchorEl,
      anchorOrigin = anchorOrigin,
      anchorPosition = anchorPosition,
      anchorReference = anchorReference,
      elevation = elevation,
      getContentAnchorEl = getContentAnchorEl,
      marginThreshold = marginThreshold,
      modalClasses = modalClasses,
      onEnter = onEnter,
      onEntered = onEntered,
      onEntering = onEntering,
      onExit = onExit,
      onExited = onExited,
      onExiting = onExiting,
      paperProps = paperProps,
      transformOrigin = transformOrigin,
      transitionComponent = transitionComponent,
      transitionDuration = transitionDuration,
      transitionProps = transitionProps,
      backdropComponent = backdropComponent,
      backdropProps = backdropProps,
      classes = classes,
      container = container,
      disableAutoFocus = disableAutoFocus,
      disableBackdropClick = disableBackdropClick,
      disableEnforceFocus = disableEnforceFocus,
      disableEscapeKeyDown = disableEscapeKeyDown,
      disablePortal = disablePortal,
      disableRestoreFocus = disableRestoreFocus,
      hideBackdrop = hideBackdrop,
      keepMounted = keepMounted,
      manager = manager,
      onBackdropClick = onBackdropClick,
      onClose = onClose,
      onEscapeKeyDown = onEscapeKeyDown,
      onRendered = onRendered,
      open = open,
      additionalProps = additionalProps
    )
    val x = f(p) _
    x(children)
  }
}
