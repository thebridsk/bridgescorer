package com.github.thebridsk.bridge.clientapi.pages

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import graphiql.GraphiQL
import com.github.thebridsk.bridge.clientapi.routes.AppRouter.AppPage
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.clientapi.routes.BridgeRouter
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles._


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

  def apply( router: BridgeRouter[AppPage] ) = component(Props(router))  // scalafix:ok ExplicitResultTypes; ReactComponent

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
    def render( props: Props, state: State ) = { // scalafix:ok ExplicitResultTypes; React
      <.div( baseStyles.divGraphiql,
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

    val didMount: Callback = Callback {
      mounted = true

    }

    val willUnmount: Callback = Callback {
      mounted = false

    }
  }

  private[pages]
  val component = ScalaComponent.builder[Props]("GraphiQLPage")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

