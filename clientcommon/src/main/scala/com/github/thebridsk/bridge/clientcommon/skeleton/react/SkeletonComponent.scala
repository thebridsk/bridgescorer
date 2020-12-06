package com.github.thebridsk.bridge.clientcommon.skeleton.react

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._

/**
  * A skeleton component.
  *
  * To use, just code the following:
  *
  * {{{
  * SkeletonComponent( SkeletonComponent.Props( ... ) )
  * }}}
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
    * @see [[SkeletonComponent]] for usage.
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

    private[react] val component = ScalaComponent
      .builder[Props]("SkeletonComponent")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .build
  }

}
