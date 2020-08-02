package com.github.thebridsk.materialui

import japgolly.scalajs.react._
import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
trait ModalProps extends AdditionalProps with StandardProps {

  val backdropComponent: js.UndefOr[js.Object] = js.native
  val backdropProps: js.UndefOr[BackdropProps] = js.native
  val classes: js.UndefOr[js.Dictionary[String]] = js.native
  val container: js.UndefOr[js.Object] = js.native
  val disableAutoFocus: js.UndefOr[Boolean] = js.native
  val disableBackdropClick: js.UndefOr[Boolean] = js.native
  val disableEnforceFocus: js.UndefOr[Boolean] = js.native
  val disableEscapeKeyDown: js.UndefOr[Boolean] = js.native
  val disablePortal: js.UndefOr[Boolean] = js.native
  val disableRestoreFocus: js.UndefOr[Boolean] = js.native
  val hideBackdrop: js.UndefOr[Boolean] = js.native
  val keepMounted: js.UndefOr[Boolean] = js.native
  val manager: js.UndefOr[js.Object] = js.native
  val onBackdropClick: js.UndefOr[() => Unit] = js.native
  val onClose: js.UndefOr[() => Unit] = js.native
  val onEscapeKeyDown: js.UndefOr[() => Unit] = js.native
  val onRendered: js.UndefOr[() => Unit] = js.native
  val open: js.UndefOr[Boolean] = js.native
}
object ModalProps extends PropsFactory[ModalProps] {

  /**
    * @param p the object that will become the properties object
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
    * @param className css class name to add to element
    * @param additionalProps a dictionary of additional properties
    */
  def apply[P <: ModalProps](
      props: js.UndefOr[P] = js.undefined,
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
//        onClose: js.UndefOr[js.Function2[js.Object,String,Unit]] = js.undefined,
      onClose: js.UndefOr[() => Unit] = js.undefined,
      onEscapeKeyDown: js.UndefOr[() => Unit] = js.undefined,
      onRendered: js.UndefOr[() => Unit] = js.undefined,
      open: js.UndefOr[Boolean] = js.undefined,
      className: js.UndefOr[String] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  ): P = {
    val p = get(props, additionalProps)

    backdropComponent.foreach(p.updateDynamic("backdropComponent")(_))
    backdropProps.foreach(p.updateDynamic("backdropProps")(_))
    classes.foreach(p.updateDynamic("classes")(_))
    container.foreach(p.updateDynamic("container")(_))
    disableAutoFocus.foreach(p.updateDynamic("disableAutoFocus")(_))
    disableBackdropClick.foreach(p.updateDynamic("disableBackdropClick")(_))
    disableEnforceFocus.foreach(p.updateDynamic("disableEnforceFocus")(_))
    disableEscapeKeyDown.foreach(p.updateDynamic("disableEscapeKeyDown")(_))
    disablePortal.foreach(p.updateDynamic("disablePortal")(_))
    disableRestoreFocus.foreach(p.updateDynamic("disableRestoreFocus")(_))
    hideBackdrop.foreach(p.updateDynamic("hideBackdrop")(_))
    keepMounted.foreach(p.updateDynamic("keepMounted")(_))
    manager.foreach(p.updateDynamic("manager")(_))
    onBackdropClick.foreach(p.updateDynamic("onBackdropClick")(_))
    onClose.foreach(p.updateDynamic("onClose")(_))
    onEscapeKeyDown.foreach(p.updateDynamic("onEscapeKeyDown")(_))
    onRendered.foreach(p.updateDynamic("onRendered")(_))
    open.foreach(p.updateDynamic("open")(_))
    className.foreach(p.updateDynamic("className")(_))

    p
  }
}

object MuiModal extends ComponentFactory[ModalProps] {
  @js.native @JSImport("@material-ui/core/Modal", JSImport.Default) private object Modal
      extends js.Any

  protected val f = JsComponent[ModalProps, Children.Varargs, Null](Modal) // scalafix:ok ExplicitResultTypes; ReactComponent

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
//        onClose: js.UndefOr[js.Function2[js.Object,String,Unit]] = js.undefined,
      onClose: js.UndefOr[() => Unit] = js.undefined,
      onEscapeKeyDown: js.UndefOr[() => Unit] = js.undefined,
      onRendered: js.UndefOr[() => Unit] = js.undefined,
      open: js.UndefOr[Boolean] = js.undefined,
      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val p: ModalProps = ModalProps(
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
