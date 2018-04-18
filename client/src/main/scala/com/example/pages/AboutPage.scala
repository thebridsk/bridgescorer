package com.example.pages

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import com.example.routes.AppRouter.AppPage
import com.example.rest2.RestClientServerURL
import com.example.rest2.RestClientServerVersion
import utils.logging.Logger
import com.example.data.ServerURL
import com.example.data.ServerVersion
import com.example.version.VersionClient
import com.example.version.VersionShared
import com.example.react.AppButton
import com.example.routes.AppRouter.Home

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * AboutPage( AboutPage.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object AboutPage {
  import AboutPageInternal._

  case class Props( router: RouterCtl[AppPage] )

  def apply( router: RouterCtl[AppPage] ) = component( Props(router))

}

object AboutPageInternal {
  import AboutPage._

  val logger = Logger("bridge.AboutPage")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( serverUrl: ServerURL, serverVersion: List[ServerVersion] )

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    val didMount = Callback {
      import scala.concurrent.ExecutionContext.Implicits.global
      // make AJAX rest call here
      logger.finer("HomePage: Sending serverurl request to server")
      RestClientServerURL.list().recordFailure().foreach( serverUrl => scope.withEffectsImpure.modState( s => s.copy(serverUrl=serverUrl(0))) )
      RestClientServerVersion.list().recordFailure().foreach( serverVersion => scope.withEffectsImpure.modState( s => s.copy(serverVersion=serverVersion.toList)) )
    }

    val indent = <.span( ^.dangerouslySetInnerHtml := "&nbsp;&nbsp;&nbsp;" )

    def render( props: Props, state: State ) = {
      import BaseStyles._
      <.div(
        rootStyles.aboutPageDiv,
        <.div(
          ^.id:="url",
          "Server",
          <.ul(
            if (state.serverUrl.serverUrl.isEmpty) {
              <.li("No network interfaces found")
            } else {
              state.serverUrl.serverUrl.map{ url => <.li(url) }.toTagMod
            }
          )
        ),
        <.div(
          <.p,
          "Client:",
          <.br, indent,
          "Client version is ", VersionClient.version,
          <.br, indent,
          "Client Build date is ", VersionClient.builtAtString+" UTC",
          <.br, indent,
          "Client Shared version is ", VersionShared.version,
          <.br, indent,
          "Client Shared Build date is ", VersionShared.builtAtString+" UTC",
          <.p,
          "Server:",
          state.serverVersion.map(sv => <.span(
            <.br, indent,
            sv.name, " version is ", sv.version,
            <.br, indent,
            sv.name, " Build date is ", sv.buildDate
          ) ).toTagMod
        ),
        <.div(
          AppButton( "OK", "OK",
                     props.router.setOnClick(Home))
        )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("AboutPage")
                            .initialStateFromProps { props => State(ServerURL(Nil), List(ServerVersion("?","?","?"))) }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .build
}

