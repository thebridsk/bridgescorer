package com.github.thebridsk.materialui.component

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
  * A skeleton component.
  *
  * To use, just code the following:
  *
  * <pre><code>
  * SkeletonComponent( SkeletonComponent.Props( ... ) )
  * </code></pre>
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

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause State to leak.
      */
    case class State()

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause Backend to leak.
      */
    class Backend(scope: BackendScope[Props, State]) {

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React

        def popoverClose() = {
          log.fine("Close called from popover")
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
