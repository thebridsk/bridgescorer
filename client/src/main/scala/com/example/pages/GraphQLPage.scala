package com.example.pages

import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
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
import com.example.react.PopupOkCancel
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import scala.util.Success
import scala.util.Failure
import com.example.rest2.AjaxFailure
import com.example.graphql.GraphQLClient
import com.example.data.graphql.GraphQLProtocol.GraphQLResponse

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * GraphQLPage( GraphQLPage.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object GraphQLPage {
  import GraphQLPageInternal._

  case class Props( router: RouterCtl[AppPage] )

  def apply( router: RouterCtl[AppPage] ) = component( Props(router))
}

object GraphQLPageInternal {
  import GraphQLPage._

  val logger = Logger("bridge.GraphQLPage")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( query: Option[String]=None, response: Option[String]=None, error: Option[TagMod]=None ) {

    def clearError() = copy( error = None )

    def withError( err: String ) = {
      val c = err.split("\n").zipWithIndex.map { e =>
        val (line,i) = e
        if (i==0) TagMod(line)
        else TagMod(<.br,line)
      }.toTagMod
      copy( error = Some(c) )
    }
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def didMount() = Callback {
    }

    import com.example.react.Utils._
    def setQuery( e: ReactEventFromInput ) = e.inputText( q =>
      scope.modState( s => s.copy(query = Some(q)))
    )

    def cancelError() = scope.modState( s => s.copy(error=None))

    def clearQuery() = scope.modState( s => s.copy(query=None))
    def clearResponse() = scope.modState( s => s.copy(response=None))

    def execute() = scope.state >>= { state => Callback {
      state.query match {
        case Some(q) =>
          val x =
            GraphQLClient.request(q) // .recordFailure()
          x.map { resp =>
            val r = Json.prettyPrint(resp.data.get)
            scope.withEffectsImpure.modState( s => s.copy( response=Some(r) ) )
          }
          .mapErrorReturn[GraphQLResponse]
          .map { error =>
            scope.withEffectsImpure.modState( s => s.withError( error.getError() ) )
          }
          .onlyExceptions
          .onComplete { tryT =>
            tryT match {
              case Success(t) =>
              case Failure(tr) =>
                tr match {
                  case x: AjaxFailure =>
                    scope.withEffectsImpure.modState( s => s.withError( x.msg.msg ) )
                  case x: Throwable =>
                    scope.withEffectsImpure.modState( s => s.withError( x.getMessage) )
                }
            }
          }
        case None =>
          // should not happen
      }
    }

    }

    def render( props: Props, state: State ) = {
      import BaseStyles._
      <.div(
        rootStyles.graphqlPageDiv,
        PopupOkCancel( state.error, None, Some(cancelError()) ),
        <.textarea(
          ^.cols := 80,
          ^.rows := 40,
          ^.placeholder := "Enter GraphQL query",
          ^.onChange ==> setQuery,
          state.query.map( s => TagMod(s) ).whenDefined
        ),
        <.div(
          baseStyles.divFooter,
          <.div(
            baseStyles.divFooterLeft,
            AppButton( "Execute", "Execute",
                       baseStyles.appButton,
                       ^.disabled := state.query.isEmpty,
                       ^.onClick --> execute
            ),
            AppButton( "ClearQ", "Clear Query",
                       baseStyles.appButton,
                       ^.onClick --> clearQuery
            ),
            AppButton( "ClearR", "Clear Response",
                       baseStyles.appButton,
                       ^.onClick --> clearResponse
            )
          )
        ),
        <.div(
          <.code( <.pre(
            state.response.whenDefined
          ))
        )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("GraphQLPage")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount())
                            .build
}

