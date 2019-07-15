package com.github.thebridsk.bridge.pages

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import graphqlvoyager.Voyager
import japgolly.scalajs.react.extra.router.RouterCtl
import com.github.thebridsk.bridge.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.react.AppButton
import com.github.thebridsk.bridge.routes.AppRouter.Home
import com.github.thebridsk.bridge.routes.BridgeRouter
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor

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

  case class Props(  router: BridgeRouter[AppPage] )

  def apply( router: BridgeRouter[AppPage] ) = component(Props(router))

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
        RootBridgeAppBar(
            Seq(MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      " Voyager",
                    )
                )
            ),
            None,
            props.router
        )(),
        <.div(
            Voyager("/v1/graphql")
        ),
//        <.div(
//          AppButton( "Home", "Home",
//                     props.router.setOnClick(Home))
//        )
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

