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
object ImportPage {
  import ImportPageInternal._

  case class Props( router: RouterCtl[AppPage] )

  def apply( router: RouterCtl[AppPage] ) = component( Props(router))

}

object ImportPageInternal {
  import ImportPage._

  val logger = Logger("bridge.ImportPage")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( existing: Option[List[String]] = None )

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def didMount() = Callback {
      GraphQLClient.request("""{ importIds }""").foreach { resp =>
        resp.data match {
          case Some(json) =>
            json \ "importIds" match {
              case JsDefined( JsArray( array ) ) =>
                val list = array.map( s => s.toString() ).toList
                scope.withEffectsImpure.modState { s => s.copy(existing = Some(list)) }
              case JsDefined( _ ) =>
                scope.withEffectsImpure.modState { s => s.copy(existing = Some(List())) }
              case x: JsUndefined =>
                scope.withEffectsImpure.modState { s => s.copy(existing = Some(List())) }
            }
          case None =>
            scope.withEffectsImpure.modState { s => s.copy(existing = Some(List())) }
        }
      }
    }

    val indent = <.span( ^.dangerouslySetInnerHtml := "&nbsp;&nbsp;&nbsp;" )

    def render( props: Props, state: State ) = {
      import BaseStyles._
      <.div(
        rootStyles.importPageDiv,

        <.h1("Import Bridge Store"),
        <.form(
          ^.action:="/v1/import?url=/public/index.html",
          ^.method:="post",
          ^.encType:="multipart/form-data",

          <.label(
            ^.`for`:="zip",
            "Zipfile to import as a bridgestore: ",
          ),
          <.input(
            ^.`type` := "file",
            ^.name := "zip",
            ^.accept := "application/zip"
          ),
          <.br,
          <.input(
            ^.`type` := "submit",
            ^.name := "submit",
            ^.value := "Submit"
          )
        ),
        <.div(
          <.p(
            state.existing match {
              case Some(list) =>
                if (!list.isEmpty) s"""Existing Ids: ${list.mkString(", ")}"""
                else "No existing Ids"
              case None =>
                "Working on getting existing Ids"
            }
          )
        ),

        <.div(
          AppButton( "Home", "Home",
                     props.router.setOnClick(Home))
        )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("ImportPage")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount())
                            .build
}

