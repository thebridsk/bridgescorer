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
  * Voyager page to show GraphQL documentation using
  * [GraphQL Voyager](https://www.npmjs.com/package/graphql-voyager).
  *
  * To use, just code the following:
  *
  * {{{
  * val router: BridgeRouter[AppPage] = ...
  *
  * VoyagerPage(
  *   router = router,
  *   page = VoyagerViewDefault
  * )
  *
  * VoyagerPage(
  *   router = router,
  *   page = VoyagerView(Map("url" -> "https://some.graphql.endpoint.example.com/graphql"))
  * )
  * }}}
  *
  * When this page is being displayed, the **url** query parameter on the page URL may be added or modified
  * to point to a GraphQL endpoint.  The GraphQL endpoint MUST support
  * the [introspection system](https://graphql.org/learn/introspection/).
  *
  * @see See the [[apply]] method for a description of the arguments.
  * @see See https://www.npmjs.com/package/graphql-voyager for information about GraphQL Voyager
  *
  * @author werewolf
  */
object VoyagerPage {
  import Internal._

  case class Props(
    router: BridgeRouter[AppPage],
    page: VoyagerView
  )

  /**
    * Instantiate the component.
    *
    * @param router the react router
    * @param page a VoyagerView object.  The object contains a map that has an optional key, `url`.
    *             If the key is present the value, a URL, must point to a GraphQL endpoint.
    *
    * @return the unmounted react component.
    */
  def apply(
    router: BridgeRouter[AppPage],
    page: VoyagerView
  ) =
    component(Props(router,page)) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    case class State()

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

    val component = ScalaComponent
      .builder[Props]("VoyagerPage")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .componentWillUnmount(scope => scope.backend.willUnmount)
      .build
  }

}
