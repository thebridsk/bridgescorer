package com.github.thebridsk.materialui.component

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.materialui.MuiPopper
import com.github.thebridsk.materialui.MuiClickAwayListener
import com.github.thebridsk.materialui.MuiPaper
import com.github.thebridsk.materialui.PopperPlacement
import com.github.thebridsk.materialui.AnchorElement


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
      placement: js.UndefOr[PopperPlacement],
      anchorEl: js.UndefOr[AnchorElement],
      onClickAway: js.UndefOr[ ()=>Unit ],
      onItemClick: js.UndefOr[ReactEvent=>Unit],
//      additionalProps: js.UndefOr[js.Dictionary[js.Any]],
      children: Seq[CtorType.ChildArg]
  )

  def apply(
      placement: js.UndefOr[PopperPlacement] = js.undefined,
      anchorEl: js.UndefOr[AnchorElement] = js.undefined,
      onClickAway: js.UndefOr[ ()=>Unit ] = js.undefined,
      onItemClick: js.UndefOr[ReactEvent=>Unit] = js.undefined,
//      additionalProps: js.UndefOr[js.Dictionary[js.Any]] = js.undefined
  )(
      children: CtorType.ChildArg*
  ) = component(Props(placement,anchorEl,onClickAway,onItemClick,children))  // scalafix:ok ExplicitResultTypes; ReactComponent

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

    def render( props: Props, state: State ) = { // scalafix:ok ExplicitResultTypes; React

//      val additionalProps = js.Dictionary[js.Any]()
//      var foundClasses = false
//      props.additionalProps.foreach { ap =>
//        ap.foreach{ e =>
//          val (key,value) = e
//          if (key == "class") {
//            val clss = value.asInstanceOf[String]
//            if (clss.indexOf("popupMenu")<0) additionalProps.update(key, value+" popupMenu")
//            else additionalProps.update(key, value)
//          } else {
//            additionalProps.update(key, value)
//          }
//        }
//      }

      <.div(
          ^.cls := "popupMenu",
          MuiPopper(
              placement = props.placement,
              open=props.anchorEl.isDefined,
              anchorEl=props.anchorEl,
//              additionalProps = props.additionalProps,
              disablePortal = true
          )(
              MuiPaper(
                  onClick = props.onItemClick,
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

  private[component]
  val component = ScalaComponent.builder[Props]("MyMenu")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}
