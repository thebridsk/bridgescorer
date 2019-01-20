package com.example.materialui.component

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.materialui.MuiPopper
import org.scalajs.dom.raw.Element
import com.example.materialui.MuiClickAwayListener
import com.example.materialui.MuiPaper
import com.example.materialui.PopperPlacement
import com.example.materialui.AnchorElement

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

  import js._
  case class Props(
      placement: js.UndefOr[PopperPlacement],
      anchorEl: js.UndefOr[AnchorElement],
      onClickAway: js.UndefOr[ ()=>Unit ],
      children: Seq[CtorType.ChildArg]
  )

  def apply(
      placement: js.UndefOr[PopperPlacement] = js.undefined,
      anchorEl: js.UndefOr[AnchorElement] = js.undefined,
      onClickAway: js.UndefOr[ ()=>Unit ] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = component(Props(placement,anchorEl,onClickAway,children))

}

object MyMenuInternal {
  import MyMenu._
  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State()

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {
    def render( props: Props, state: State ) = {
      MuiPopper(
          placement = props.placement,
          open=props.anchorEl.isDefined,
          anchorEl=props.anchorEl
      )(
          MuiPaper(
          )(
              MuiClickAwayListener(
                  onClickAway = props.onClickAway
              )(
                  <.div(
                      props.children:_*
                  )
              )
          )
      )
    }

    private var mounted = false

    val didMount = Callback {
      mounted = true

    }

    val willUnmount = Callback {
      mounted = false

    }
  }

  val component = ScalaComponent.builder[Props]("MyMenu")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

