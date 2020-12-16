package com.github.thebridsk.bridge.clientcommon.component

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.materialui.AnchorElement
import com.github.thebridsk.materialui.AnchorOrigin
import com.github.thebridsk.materialui.AnchorReference
import com.github.thebridsk.materialui.MuiPopover
import com.github.thebridsk.materialui.MuiClickAwayListener
import com.github.thebridsk.materialui.MuiMenuList
import com.github.thebridsk.materialui.MenuVariant
import com.github.thebridsk.utilities.logging.Logger

/**
  * A menu component.
  *
  * This component is functionally the same as the MuiMenu component,
  * but it allows the popup menu to be attached to the element in any way possible.
  *
  * To use, just code the following:
  *
  * {{{
  * case class State {
  *   anchorEl: js.UndefOr[AnchorElement] = js.undefined
  * }
  * def onClose(): Unit = {
  *   scope.modState(_.copy(anchorEl = js.undefined))
  * }
  * MyMenu(
  *   onClose = onClose _,
  *   anchorOrigin = AnchorOrigin(
  *     AnchorOriginHorizontalValue.left,
  *     AnchorOriginVerticalValue.bottom
  *   ),
  *   transformOrigin = AnchorOrigin(
  *     AnchorOriginHorizontalValue.left,
  *     AnchorOriginVerticalValue.top
  *   )
  * )(
  *   MuiMenuItem(...),
  *   ...
  * )
  * }}}
  *
  * If an onClick event opens this menu, then the handler must invoke event.stopPropogate() on the onClick event.
  * If this is not done, then the ClickAwayListener in this component gets the click also, which immediately closes the menu.
  *
  * @see [[apply]] for a description of the properties to the component.
  *
  * @author werewolf
  */
object MyMenu {
  import Internal._

  case class Props(
      anchorOrigin: js.UndefOr[AnchorOrigin],
      transformOrigin: js.UndefOr[AnchorOrigin],
      anchorEl: js.UndefOr[AnchorElement],
      onClose: js.UndefOr[() => Unit],
      children: Seq[CtorType.ChildArg]
  )

  /**
    * Instantiate the component
    *
    * @param onClose - callback that gets called when the menu closes.
    * @param anchorOrigin - the point on the anchor element where the menu is attached.
    * @param transformOrigin - the point on the menu element where the menu is attacted.
    * @param anchorEl - the anchor element.  if js.undefined, then the menu is not displayed.
    * @param children - the menu items.
    * @return the unmounted react component
    *
    * @see [[MyMenu]] for usage information.
    */
  def apply(
      onClose: js.UndefOr[() => Unit],
      anchorOrigin: js.UndefOr[AnchorOrigin] = js.undefined,
      transformOrigin: js.UndefOr[AnchorOrigin] = js.undefined,
      anchorEl: js.UndefOr[AnchorElement] = js.undefined,
  )(
      children: CtorType.ChildArg*
  ) =
    component(
      Props(anchorOrigin, transformOrigin, anchorEl, onClose, children)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent


  protected object Internal {

    val log = Logger("bridge.MyMenu")

    case class State()

    class Backend(scope: BackendScope[Props, State]) {

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React

        def popoverClose() = {
          log.fine("Close called from popover")
          props.onClose.foreach(f => f())
        }

        def popoverClick(event: ReactEvent) = {
          log.fine("Click called from popover")
          props.onClose.foreach(f => f())
        }

        def clickawayClose() = {
          log.fine("Close called from clickAway")
          props.onClose.foreach(f => f())
        }

        MuiPopover(
          anchorEl = props.anchorEl,
          anchorOrigin = props.anchorOrigin,
          anchorReference = AnchorReference.anchorEl,
          disablePortal = true,
          open = props.anchorEl.isDefined,
          onClose = popoverClose _,
          onClick = popoverClick _,
          transformOrigin = props.transformOrigin,
        )(
          MuiClickAwayListener(
            onClickAway = clickawayClose _
          )(
            MuiMenuList(
              variant = MenuVariant.menu
            )(
              props.children: _*
            )
          )
        )

      }

      private var mounted = false

      val didMount: Callback = Callback {
        mounted = true

      }

      val willUnmount: Callback = Callback {
        mounted = false

      }
    }

    private[component] val component = ScalaComponent
      .builder[Props]("MyMenu")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .build
  }

}
