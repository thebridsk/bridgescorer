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

  val disableAutoFocusItem: js.UndefOr[Boolean] = js.native
  val MenuListProps: js.UndefOr[js.Object] = js.native
  val PopoverClasses: js.UndefOr[js.Object] = js.native
}
object MenuProps {
  import js._
  def apply(
      p: js.Object with js.Dynamic = js.Dynamic.literal(),

      disableAutoFocusItem: js.UndefOr[Boolean] = js.undefined,
      MenuListProps: js.UndefOr[js.Object] = js.undefined,
      PopoverClasses: js.UndefOr[js.Object] = js.undefined,

      action: js.UndefOr[js.Function1[js.Object,Unit]] = js.undefined,
      anchorEl: js.UndefOr[ js.Object | js.Function0[Element] ] = js.undefined,
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
      onBackdropClick: js.UndefOr[js.Function0[Unit]] = js.undefined,
//      onClose: js.UndefOr[js.Function2[js.Object,String,Unit]] = js.undefined,
      onClose: js.UndefOr[() => Unit] = js.undefined,
      onEscapeKeyDown: js.UndefOr[js.Function0[Unit]] = js.undefined,
      onRendered: js.UndefOr[js.Function0[Unit]] = js.undefined,
      open: js.UndefOr[Boolean] = js.undefined,

  ) = {

  }
}

object MuiMenu {

  @js.native @JSImport("@material-ui/core/Menu", JSImport.Default) private object Menu extends js.Any

  private val f = JsComponent[js.Object, Children.Varargs, Null](Menu)

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
   *
   * @param children Menu contents, normally MenuItems.
   */
  def apply(

      disableAutoFocusItem: js.UndefOr[Boolean] = js.undefined,
      MenuListProps: js.UndefOr[js.Object] = js.undefined,
      PopoverClasses: js.UndefOr[js.Object] = js.undefined,

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
      onBackdropClick: js.UndefOr[js.Function0[Unit]] = js.undefined,
//      onClose: js.UndefOr[js.Function2[js.Object,String,Unit]] = js.undefined,
      onClose: js.UndefOr[() => Unit] = js.undefined,
      onEscapeKeyDown: js.UndefOr[js.Function0[Unit]] = js.undefined,
      onRendered: js.UndefOr[js.Function0[Unit]] = js.undefined,
      open: js.UndefOr[Boolean] = js.undefined,
  )(
        children: CtorType.ChildArg*
   ) = {
    val p: js.Object with js.Dynamic = js.Dynamic.literal()

    PopoverProps(
      p,

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

    disableAutoFocusItem.foreach( p.updateDynamic("disableAutoFocusItem")(_))
    MenuListProps.foreach( p.updateDynamic("MenuListProps")(_))
    PopoverClasses.foreach( p.updateDynamic("PopoverClasses")(_))

    val x = f(p) _
    x(children)
  }
}
