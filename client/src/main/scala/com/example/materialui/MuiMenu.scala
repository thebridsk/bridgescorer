package com.example.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._
import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.Node

@js.native
trait MenuProps extends PopoverProps {

  var disableAutoFocusItem: js.UndefOr[Boolean] = js.native
  var MenuListProps: js.UndefOr[js.Object] = js.native
  var PopoverClasses: js.UndefOr[js.Object] = js.native
}
object MenuProps extends PropsFactory[MenuProps] {
  import js._
  def apply[P <: MenuProps](
      props: js.UndefOr[P] = js.undefined,

      disableAutoFocusItem: js.UndefOr[Boolean] = js.undefined,
      MenuListProps: js.UndefOr[js.Object] = js.undefined,
      PopoverClasses: js.UndefOr[js.Object] = js.undefined,

      action: js.UndefOr[js.Object=>Unit] = js.undefined,
      anchorEl: js.UndefOr[AnchorElement] = js.undefined,
      anchorOrigin: js.UndefOr[AnchorOrigin] = js.undefined,
      anchorPosition: js.UndefOr[AnchorPosition] = js.undefined,
      anchorReference: js.UndefOr[AnchorReference] = js.undefined,
      elevation: js.UndefOr[Double] = js.undefined,
      getContentAnchorEl: js.UndefOr[()=>js.Object] = js.undefined,
      marginThreshold: js.UndefOr[Double] = js.undefined,
      modalClasses: js.UndefOr[ModalProps] = js.undefined,
      onEnter: js.UndefOr[()=>Unit] = js.undefined,
      onEntered: js.UndefOr[()=>Unit] = js.undefined,
      onEntering: js.UndefOr[()=>Unit] = js.undefined,
      onExit: js.UndefOr[()=>Unit] = js.undefined,
      onExited: js.UndefOr[()=>Unit] = js.undefined,
      onExiting: js.UndefOr[()=>Unit] = js.undefined,
      paperProps: js.UndefOr[PaperProps] = js.undefined,
      transformOrigin: js.UndefOr[js.Object] = js.undefined,
      transitionComponent: js.UndefOr[js.Object] = js.undefined,
      transitionDuration: js.UndefOr[ Int | TransitionDuration ] = js.undefined,
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
      onBackdropClick: js.UndefOr[()=>Unit] = js.undefined,
//      onClose: js.UndefOr[js.Function2[js.Object,String,Unit]] = js.undefined,
      onClose: js.UndefOr[() => Unit] = js.undefined,
      onEscapeKeyDown: js.UndefOr[()=>Unit] = js.undefined,
      onRendered: js.UndefOr[()=>Unit] = js.undefined,
      open: js.UndefOr[Boolean] = js.undefined,

      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined

  ): P = {
    val p: P = PopoverProps(
        props,

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

    disableAutoFocusItem.foreach( p.updateDynamic("disableAutoFocusItem")(_))
    MenuListProps.foreach( p.updateDynamic("MenuListProps")(_))
    PopoverClasses.foreach( p.updateDynamic("PopoverClasses")(_))

    p
  }
}

object MuiMenu extends PropsFactory[MenuProps] {

  @js.native @JSImport("@material-ui/core/Menu", JSImport.Default) private object Menu extends js.Any

  protected val f = JsComponent[MenuProps, Children.Varargs, Null](Menu)

  import js._
  /**
   * @param anchorEl The DOM element used to set the position of the menu.
   * @param classes Override or extend the styles applied to the component. See CSS API below for more details.
   * @param disableAutoFocusItem If true, the selected / first menu item will not be auto focused.
   * @param MenuListProps Properties applied to the MenuList element.
   * @param onClose Callback fired when the component requests to be closed.
   *                Signature:
   *                  function(event: object, reason: string) => void
   *                event: The event source of the callback
   *                reason: Can be:"escapeKeyDown", "backdropClick", "tabKeyDown"
   * @param onEnter Callback fired before the Menu enters.
   * @param onEntered Callback fired before the Menu has entered.
   * @param onEntering Callback fired before the Menu is entering.
   * @param onExit Callback fired before the Menu exits.
   * @param onExited Callback fired before the Menu has exited.
   * @param onExiting Callback fired before the Menu is exiting.
   * @param open If true, the menu is visible.
   * @param PopoverClasses classes property applied to the Popover element.
   * @param transitionDuration The length of the transition in ms, or 'auto'
   * @param additionalProps a dictionary of additional properties
   *
   * @param children Menu contents, normally MenuItems.
   */
  def apply(

      disableAutoFocusItem: js.UndefOr[Boolean] = js.undefined,
      MenuListProps: js.UndefOr[js.Object] = js.undefined,
      PopoverClasses: js.UndefOr[js.Object] = js.undefined,

      action: js.UndefOr[js.Object=>Unit] = js.undefined,
      anchorEl: js.UndefOr[AnchorElement] = js.undefined,
      anchorOrigin: js.UndefOr[AnchorOrigin] = js.undefined,
      anchorPosition: js.UndefOr[AnchorPosition] = js.undefined,
      anchorReference: js.UndefOr[AnchorReference] = js.undefined,
      elevation: js.UndefOr[Double] = js.undefined,
      getContentAnchorEl: js.UndefOr[()=>js.Object] = js.undefined,
      marginThreshold: js.UndefOr[Double] = js.undefined,
      modalClasses: js.UndefOr[ModalProps] = js.undefined,
      onEnter: js.UndefOr[()=>Unit] = js.undefined,
      onEntered: js.UndefOr[()=>Unit] = js.undefined,
      onEntering: js.UndefOr[()=>Unit] = js.undefined,
      onExit: js.UndefOr[()=>Unit] = js.undefined,
      onExited: js.UndefOr[()=>Unit] = js.undefined,
      onExiting: js.UndefOr[()=>Unit] = js.undefined,
      paperProps: js.UndefOr[PaperProps] = js.undefined,
      transformOrigin: js.UndefOr[js.Object] = js.undefined,
      transitionComponent: js.UndefOr[js.Object] = js.undefined,
      transitionDuration: js.UndefOr[ Int | TransitionDuration ] = js.undefined,
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
      onBackdropClick: js.UndefOr[()=>Unit] = js.undefined,
//      onClose: js.UndefOr[js.Function2[js.Object,String,Unit]] = js.undefined,
      onClose: js.UndefOr[() => Unit] = js.undefined,
      onEscapeKeyDown: js.UndefOr[()=>Unit] = js.undefined,
      onRendered: js.UndefOr[()=>Unit] = js.undefined,
      open: js.UndefOr[Boolean] = js.undefined,

      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
        children: CtorType.ChildArg*
   ) = {

    val p: MenuProps = MenuProps(
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
      disableAutoFocusItem = disableAutoFocusItem,
      MenuListProps = MenuListProps,
      PopoverClasses = PopoverClasses,

      additionalProps = additionalProps
    )

    val x = f(p) _
    x(children)
  }
}
