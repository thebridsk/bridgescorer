package com.example.pages

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import graphqlvoyater.Voyager

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * VoyagerPage( VoyagerPage.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object VoyagerPage {
  import VoyagerPageInternal._

  case class Props( )

  def apply( ) = component(Props())

}

object VoyagerPageInternal {
  import VoyagerPage._
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
      <.div(
          Voyager("/v1/graphql")
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

  val component = ScalaComponent.builder[Props]("VoyagerPage")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

