package com.github.thebridsk.bridge.clientcommon.skeleton.react

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._


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
object SkeletonComponent {
  import SkeletonComponentInternal._

  case class Props( )

  def apply( ) = component(Props())  // scalafix:ok ExplicitResultTypes; ReactComponent

}

object SkeletonComponentInternal {
  import SkeletonComponent._
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

  private[react]
  val component = ScalaComponent.builder[Props]("SkeletonComponent")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

