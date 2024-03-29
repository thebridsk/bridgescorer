package com.github.thebridsk.bridge.client.pages

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientServerURL
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientServerVersion
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.ServerURL
import com.github.thebridsk.bridge.data.ServerVersion
import com.github.thebridsk.bridge.client.version.VersionClient
import com.github.thebridsk.bridge.data.version.VersionShared
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles._

/**
  * A component page that shows the versions of the server and client.
  *
  * To use, just code the following:
  *
  * {{{
  * AboutPage(
  *   router = ...
  * )
  * }}}
  *
  * @author werewolf
  */
object AboutPage {
  import Internal._

  case class Props(router: BridgeRouter[AppPage])

  /**
    * Instantiate the component
    *
    * @param router
    * @return the unmounted react component
    */
  def apply(router: BridgeRouter[AppPage]) =
    component(Props(router)) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {

    val logger: Logger = Logger("bridge.AboutPage")

    case class State(serverUrl: ServerURL, serverVersion: List[ServerVersion])

    class Backend(scope: BackendScope[Props, State]) {

      val didMount: Callback = Callback {
        import com.github.thebridsk.bridge.clientcommon.BridgeExecutionContext.global
        // make AJAX rest call here
        logger.finer("HomePage: Sending serverurl request to server")
        RestClientServerURL
          .list()
          .recordFailure()
          .foreach(serverUrl =>
            scope.withEffectsImpure.modState(s =>
              s.copy(serverUrl = serverUrl(0))
            )
          )
        RestClientServerVersion
          .list()
          .recordFailure()
          .foreach(serverVersion =>
            scope.withEffectsImpure.modState(s =>
              s.copy(serverVersion = serverVersion.toList)
            )
          )
      }

      val indent =
        <.span(
          ^.dangerouslySetInnerHtml := "&nbsp;&nbsp;&nbsp;"
        ) // scalafix:ok ExplicitResultTypes; React

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        <.div(
          RootBridgeAppBar(
            Seq(
              MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit
              )(
                <.span(
                  " About"
                )
              )
            ),
            None,
            props.router
          )(),
          <.div(
            rootStyles.aboutPageDiv,
            <.div(
              ^.id := "url",
              "Server",
              <.ul(
                if (state.serverUrl.serverUrl.isEmpty) {
                  <.li("No network interfaces found")
                } else {
                  state.serverUrl.serverUrl.map { url => <.li(url) }.toTagMod
                }
              )
            ),
            <.div(
              <.p,
              "Client:",
              <.br,
              indent,
              "Client version is ",
              VersionClient.version,
              <.br,
              indent,
              "Client Build date is ",
              VersionClient.builtAtString + " UTC",
              <.br,
              indent,
              "Client Shared version is ",
              VersionShared.version,
              <.br,
              indent,
              "Client Shared Build date is ",
              VersionShared.builtAtString + " UTC",
              <.p,
              "Server:",
              state.serverVersion
                .map(sv =>
                  <.span(
                    <.br,
                    indent,
                    sv.name,
                    " version is ",
                    sv.version,
                    <.br,
                    indent,
                    sv.name,
                    " Build date is ",
                    sv.buildDate
                  )
                )
                .toTagMod
            )
          )
  //        <.div(
  //          AppButton( "OK", "OK",
  //                     props.router.setOnClick(Home))
  //        )
        )
      }
    }

    val component = ScalaComponent
      .builder[Props]("AboutPage")
      .initialStateFromProps { props =>
        State(ServerURL(Nil), List(ServerVersion("?", "?", "?")))
      }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .build
  }

}
