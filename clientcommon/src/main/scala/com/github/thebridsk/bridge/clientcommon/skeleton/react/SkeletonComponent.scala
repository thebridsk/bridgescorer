package com.github.thebridsk.bridge.clientcommon.skeleton.react

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate

/**
  * A skeleton component.
  *
  * To use, just code the following:
  *
  * {{{
  * SkeletonComponent( SkeletonComponent.Props( ... ) )
  * }}}
  *
  * @see See [[apply]] for a description of the arguments.
  *
  * @author werewolf
  */
object SkeletonComponent {
  import Internal._

  case class Props()

  /**
    *
    *
    * @return the unmounted react component
    *
    * @see [[SkeletonComponent$]] for usage.
    */
  def apply() =
    component(Props()) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    case class State()

    class Backend(scope: BackendScope[Props, State]) {
      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        <.div()
      }

      private var mounted = false

      val didMount: Callback = Callback {
        mounted = true

      }

      val willUnmount: Callback = Callback {
        mounted = false

      }
    }

    def didUpdate(
        cdu: ComponentDidUpdate[Props, State, Backend, Unit]
    ): Callback =
      Callback {
        val props = cdu.currentProps
        val prevProps = cdu.prevProps
        if (prevProps != props) {
          // props have change, reinitialize state
          cdu.setState(State())
        }
      }

    val component = ScalaComponent
      .builder[Props]("SkeletonComponent")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .componentDidUpdate(didUpdate)
      .build
  }

}
