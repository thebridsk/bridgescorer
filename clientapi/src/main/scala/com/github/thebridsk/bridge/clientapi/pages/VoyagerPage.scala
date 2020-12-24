package com.github.thebridsk.bridge.clientapi.pages

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import graphqlvoyager.Voyager
import com.github.thebridsk.bridge.clientapi.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.clientapi.routes.BridgeRouter
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.clientapi.routes.AppRouter.VoyagerView

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

  case class Props(
    router: BridgeRouter[AppPage],
    page: VoyagerView
  )

  def apply(
    router: BridgeRouter[AppPage],
    page: VoyagerView
  ) =
    component(Props(router,page)) // scalafix:ok ExplicitResultTypes; ReactComponent

}

object VoyagerPageInternal {
  import VoyagerPage._

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause State to leak.
    */
  case class State()

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause Backend to leak.
    */
  class Backend(scope: BackendScope[Props, State]) {
    def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
      val url = props.page.query.get("url").getOrElse("/v1/graphql")
      <.div(
        RootBridgeAppBar(
          Seq(
            MuiTypography(
              variant = TextVariant.h6,
              color = TextColor.inherit
            )(
              <.span(
                " Voyager"
              )
            )
          ),
          None,
          props.router
        )(),
        <.div(
          Voyager(url)
        )
//        <.div(
//          AppButton( "Home", "Home",
//                     props.router.setOnClick(Home))
//        )
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

  private[pages] val component = ScalaComponent
    .builder[Props]("VoyagerPage")
    .initialStateFromProps { props => State() }
    .backend(new Backend(_))
    .renderBackend
    .componentDidMount(scope => scope.backend.didMount)
    .componentWillUnmount(scope => scope.backend.willUnmount)
    .build
}
