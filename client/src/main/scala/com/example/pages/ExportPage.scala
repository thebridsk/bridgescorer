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
import com.example.graphql.GraphQLClient
import play.api.libs.json.JsDefined
import play.api.libs.json.JsUndefined
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.JsArray
import com.example.react.AppButtonLink

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ImportPage( ImportPage.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object ExportPage {
  import ExportPageInternal._

  case class Props( router: RouterCtl[AppPage] )

  def apply( router: RouterCtl[AppPage] ) = component( Props(router))

}

object ExportPageInternal {
  import ExportPage._

  val logger = Logger("bridge.ExportPage")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( dupids: Option[List[String]] = None,
                    chiids: Option[List[String]] = None,
                    rubids: Option[List[String]] = None,
                    selectedIds: Option[List[String]] = None
                  )

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def didMount() = Callback {
      GraphQLClient.request("""{ duplicateIds, chicagoIds, rubberIds }""").foreach { resp =>
        resp.data match {
          case Some(json) =>
            json \ "duplicateIds" match {
              case JsDefined( JsArray( array ) ) =>
                val list = array.map( s => s.toString() ).toList
                scope.withEffectsImpure.modState { s => s.copy(dupids = Some(list)) }
              case JsDefined( _ ) =>
                scope.withEffectsImpure.modState { s => s.copy(dupids = Some(List())) }
              case x: JsUndefined =>
                scope.withEffectsImpure.modState { s => s.copy(dupids = Some(List())) }
            }
            json \ "chicagoIds" match {
              case JsDefined( JsArray( array ) ) =>
                val list = array.map( s => s.toString() ).toList
                scope.withEffectsImpure.modState { s => s.copy(chiids = Some(list)) }
              case JsDefined( _ ) =>
                scope.withEffectsImpure.modState { s => s.copy(chiids = Some(List())) }
              case x: JsUndefined =>
                scope.withEffectsImpure.modState { s => s.copy(chiids = Some(List())) }
            }
            json \ "rubberIds" match {
              case JsDefined( JsArray( array ) ) =>
                val list = array.map( s => s.toString() ).toList
                scope.withEffectsImpure.modState { s => s.copy(rubids = Some(list)) }
              case JsDefined( _ ) =>
                scope.withEffectsImpure.modState { s => s.copy(rubids = Some(List())) }
              case x: JsUndefined =>
                scope.withEffectsImpure.modState { s => s.copy(rubids = Some(List())) }
            }
          case None =>
            scope.withEffectsImpure.modState { s => s.copy(dupids = Some(List()), chiids = Some(List()), rubids = Some(List())) }
        }
      }
    }

    val indent = <.span( ^.dangerouslySetInnerHtml := "&nbsp;&nbsp;&nbsp;" )

    def render( props: Props, state: State ) = {
      import BaseStyles._
      <.div(
        rootStyles.exportPageDiv,

        <.h1("Export Bridge Store"),
        <.div(
        ),

        <.div(
          AppButton( "Home", "Home",
                     props.router.setOnClick(Home)),
          {
            val filter = state.selectedIds.map(list => list.mkString("?filter=", ",", "")).getOrElse("")
            val location = document.defaultView.location
            val origin = location.origin.get
            val path = s"""${origin}/v1/export${filter}"""
            AppButtonLink( "Export", "Export", path,
                           baseStyles.appButton
            )
          }
        )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("ExportPage")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount())
                            .build
}

