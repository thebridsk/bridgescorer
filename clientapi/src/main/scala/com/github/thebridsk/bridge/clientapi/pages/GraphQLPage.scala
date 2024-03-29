package com.github.thebridsk.bridge.clientapi.pages

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.clientapi.routes.AppRouter.AppPage
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.clientcommon.BridgeExecutionContext.global
import play.api.libs.json.Json
import scala.util.Success
import scala.util.Failure
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxFailure
import com.github.thebridsk.bridge.clientcommon.graphql.GraphQLClient
import com.github.thebridsk.bridge.data.graphql.GraphQLProtocol.GraphQLResponse
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
  * GraphQLPage( GraphQLPage.Props( ... ) )
  * </code></pre>
  *
  * @author werewolf
  */
object GraphQLPage {
  import GraphQLPageInternal._

  case class Props(router: BridgeRouter[AppPage])

  def apply(router: BridgeRouter[AppPage]) =
    component(Props(router)) // scalafix:ok ExplicitResultTypes; ReactComponent
}

object GraphQLPageInternal {
  import GraphQLPage._

  val logger: Logger = Logger("bridge.GraphQLPage")

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause State to leak.
    */
  case class State(
      query: Option[String] = None,
      response: Option[String] = None,
      error: Option[TagMod] = None
  ) {

    def clearError(): State = copy(error = None)

    def withError(err: String): State = {
      val c = err
        .split("\n")
        .zipWithIndex
        .map { e =>
          val (line, i) = e
          if (i == 0) TagMod(line)
          else TagMod(<.br, line)
        }
        .toTagMod
      copy(error = Some(c))
    }
  }

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause Backend to leak.
    */
  class Backend(scope: BackendScope[Props, State]) {

    val didMount: Callback = Callback {}

    import com.github.thebridsk.bridge.clientcommon.react.Utils._
    def setQuery(e: ReactEventFromInput): Callback =
      e.inputText(q => scope.modState(s => s.copy(query = Some(q))))

    val cancelError: Callback = scope.modState(s => s.copy(error = None))

    val clearQuery: Callback = scope.modState(s => s.copy(query = None))
    val clearResponse: Callback = scope.modState(s => s.copy(response = None))

    val execute: Callback = scope.state >>= { state =>
      Callback {
        state.query match {
          case Some(q) =>
            val x =
              GraphQLClient.request(q) // .recordFailure()
            x.map { resp =>
              val r = Json.prettyPrint(resp.data.get)
              scope.withEffectsImpure.modState(s => s.copy(response = Some(r)))
            }.mapErrorReturn[GraphQLResponse]
              .map { error =>
                scope.withEffectsImpure.modState(s =>
                  s.withError(error.getError())
                )
              }
              .onlyExceptions
              .onComplete { tryT =>
                tryT match {
                  case Success(t) =>
                  case Failure(tr) =>
                    tr match {
                      case x: AjaxFailure =>
                        scope.withEffectsImpure
                          .modState(s => s.withError(x.msg.msg))
                      case x: Throwable =>
                        scope.withEffectsImpure
                          .modState(s => s.withError(x.getMessage))
                    }
                }
              }
          case None =>
          // should not happen
        }
      }

    }

    def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
      <.div(
        rootStyles.graphqlPageDiv,
        PopupOkCancel(state.error, None, Some(cancelError)),
        RootBridgeAppBar(
          Seq(
            MuiTypography(
              variant = TextVariant.h6,
              color = TextColor.inherit
            )(
              <.span(
                " GraphQL"
              )
            )
          ),
          None,
          props.router
        )(),
        <.div(
          <.textarea(
            ^.cols := 80,
            ^.rows := 40,
            ^.placeholder := "Enter GraphQL query",
            ^.onChange ==> setQuery,
            state.query.map(s => TagMod(s)).whenDefined
          ),
          <.div(
            baseStyles.divFooter,
            <.div(
              baseStyles.divFooterLeft,
              AppButton(
                "Execute",
                "Execute",
                baseStyles.appButton,
                ^.disabled := state.query.isEmpty,
                ^.onClick --> execute
              ),
              AppButton(
                "ClearQ",
                "Clear Query",
                baseStyles.appButton,
                ^.onClick --> clearQuery
              ),
              AppButton(
                "ClearR",
                "Clear Response",
                baseStyles.appButton,
                ^.onClick --> clearResponse
              )
            )
          ),
          <.div(
            <.code(
              <.pre(
                state.response.whenDefined
              )
            )
          )
        )
      )
    }
  }

  private[pages] val component = ScalaComponent
    .builder[Props]("GraphQLPage")
    .initialStateFromProps { props => State() }
    .backend(new Backend(_))
    .renderBackend
    .componentDidMount(scope => scope.backend.didMount)
    .build
}
