package com.github.thebridsk.bridge.pages

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import graphiql.GraphiQL
import japgolly.scalajs.react.extra.router.RouterCtl
import com.github.thebridsk.bridge.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.react.AppButton
import com.github.thebridsk.bridge.routes.AppRouter.Home
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.routes.BridgeRouter

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * GraphiQLPage( GraphiQLPage.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object GraphiQLPage {
  import GraphiQLPageInternal._

  case class Props(  router: BridgeRouter[AppPage] )

  def apply( router: BridgeRouter[AppPage] ) = component(Props(router))

}

object GraphiQLPageInternal {
  import GraphiQLPage._
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
      <.div( BaseStyles.baseStyles.divGraphiql,
        RootBridgeAppBar(
            Seq(MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      " GraphiQL",
                    )
                )
            ),
            None,
            props.router
        )(),
//        <.div(
//          AppButton( "Home", "Home",
//                     props.router.setOnClick(Home))
//        ),
        GraphiQL("/v1/graphql")
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

  val component = ScalaComponent.builder[Props]("GraphiQLPage")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

