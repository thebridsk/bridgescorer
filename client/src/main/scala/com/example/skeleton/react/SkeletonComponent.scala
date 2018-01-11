package com.example.skeleton.react

import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
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

  type Callback = ()=>Unit

  case class Props( callback: Callback )

  def apply( props: Props ) = component(props)

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
    def render( props: Props, state: State ) = {
      <.div()
    }
  }

  val component = ScalaComponent.builder[Props]("SkeletonComponent")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

