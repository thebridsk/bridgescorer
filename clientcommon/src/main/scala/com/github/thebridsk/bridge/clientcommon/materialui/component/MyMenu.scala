package com.github.thebridsk.materialui.component

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.materialui.AnchorElement
import com.github.thebridsk.materialui.MuiMenu
import com.github.thebridsk.materialui.AnchorOrigin
import com.github.thebridsk.materialui.AnchorReference

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
  import MyMenuInternal._

  case class Props(
      // placement: js.UndefOr[PopperPlacement],
      anchorOrigin: js.UndefOr[AnchorOrigin],
      anchorEl: js.UndefOr[AnchorElement],
      onClickAway: js.UndefOr[() => Unit],
      onItemClick: js.UndefOr[ReactEvent => Unit],
//      additionalProps: js.UndefOr[js.Dictionary[js.Any]],
      children: Seq[CtorType.ChildArg]
  )

  def apply(
      // placement: js.UndefOr[PopperPlacement] = js.undefined,
      anchorOrigin: js.UndefOr[AnchorOrigin] = js.undefined,
      anchorEl: js.UndefOr[AnchorElement] = js.undefined,
      onClickAway: js.UndefOr[() => Unit] = js.undefined,
      onItemClick: js.UndefOr[ReactEvent => Unit] = js.undefined
//      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) =
    component(
      Props(anchorOrigin, anchorEl, onClickAway, onItemClick, children)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

}

object MyMenuInternal {
  import MyMenu._

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

      MuiMenu(
        anchorEl = props.anchorEl,
        anchorReference = AnchorReference.anchorEl,
        anchorOrigin = props.anchorOrigin,
        onClose = props.onClickAway,
        open = props.anchorEl.isDefined,
        // keepMounted = true,
        disablePortal = true
      )(
        props.children: _*
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
