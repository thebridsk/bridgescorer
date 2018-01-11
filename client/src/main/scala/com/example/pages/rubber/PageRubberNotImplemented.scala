package com.example.pages.rubber

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PageRubberNotImplemented( PageRubberNotImplemented.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object PageRubberNotImplemented {
  import PageRubberNotImplementedInternal._

  case class Props( page: RubberPage, routerCtl: RouterCtl[RubberPage] )

  def apply( page: RubberPage, routerCtl: RouterCtl[RubberPage] ) =
    component( Props( page, routerCtl ) )

}

object PageRubberNotImplementedInternal {
  import PageRubberNotImplemented._
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
      <.div(<.h1("PageRubberNotImplemented not implemented, page="+props.page))
    }
  }

  val component = ScalaComponent.builder[Props]("PageRubberNotImplemented")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

