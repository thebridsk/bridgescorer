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
import play.api.libs.json.JsString
import play.api.libs.json.JsObject
import play.api.libs.json.JsNull
import play.api.libs.json.JsTrue
import com.example.react.PopupOkCancel
import com.example.data.SystemTime.Timestamp
import play.api.libs.json.Reads
import play.api.libs.json.Json
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import com.example.react.DateUtils
import com.example.pages.duplicate.DuplicateRouter.ImportSummaryView
import com.example.pages.duplicate.DuplicateModule.PlayDuplicate

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ImportsListPage( ImportsListPage.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object ImportsListPage {
  import ImportsListPageInternal._

  case class Props( router: RouterCtl[AppPage], page: AppPage )

  def apply( router: RouterCtl[AppPage], page: AppPage ) = component( Props(router,page))

}

object ImportMethods {
  import ImportsListPageInternal._
//  {
//    "import" : {
//      "delete" : true   // true if deleted, false if not
//    }
//  }
// or
//  {
//    "import" : null     // import store not found
//  }

  def delete( id: String ) = {

    val vars = JsObject( Seq("mdid" -> JsString(id)))
    val query =
       """
         mutation DeleteImport($mdid: ImportId!) {
           import(id: $mdid) {
             delete
           }
         }
       """
    val operation = Some("DeleteImport")

    GraphQLClient.request(query, Some(vars), operation ).map { resp =>
      resp.data match {
        case Some( d: JsObject ) =>
          d \ "import" match {
            case JsDefined( JsNull ) =>
              logger.warning( s"deleteImport(${id}) return: not found")
              Left(s"Import store ${id} not found")
            case JsDefined( di: JsObject ) =>
              if (di \ "delete" == JsDefined(JsTrue)) {
                Right(true)
              } else {
                Left(s"Import store ${id} was not deleted" )
              }
            case _ =>
              logger.warning( s"Unexpected response on deleteImport(${id}): ${resp}")
              Left("Internal error")
          }
        case _ =>
          logger.warning( s"Error on deleteImport(${id}): ${resp}")
          Left("Internal error")
      }
    }

  }

  case class ImportStore( id: String, date: Timestamp )
  case class ImportsList( imports: List[ImportStore] )

  implicit val readsImportStore = Json.reads[ImportStore]
  implicit val readsImportsList = Json.reads[ImportsList]

  def list() = {


    val vars = None
    val query =
       """
         |{
         |  imports {
         |    id
         |    date
         |  }
         |}
         |""".stripMargin
    val operation = None

    GraphQLClient.request(query, vars, operation ).map { resp =>
      resp.data match {
        case Some( d: JsObject ) =>
          Json.fromJson[ImportsList](d) match {
            case JsSuccess( t, path ) =>
              Right(t)
            case err: JsError =>
              logger.warning( s"Error processing return data from Imports list: ${JsError.toJson(err)}" )
              Left("Error processing returned data")
          }
        case _ =>
          logger.warning( s"Error on Imports list: ${resp}")
          Left("Internal error")
      }
    }.recover {
      case x: Exception =>
        logger.warning( s"Error on Imports list", x)
        Left("Internal error")
    }

  }
}

object ImportsListPageInternal {
  import ImportsListPage._
  import ImportMethods.ImportsList

  val logger = Logger("bridge.ImportsListPage")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State(
      stores: Option[ImportsList] = None,
      selectedForImport: Option[String] = None,
      error: Option[TagMod] = None
  ) {

    def clearError() = copy(error=None)

    def withError( s: String ) = copy( error = Some(s) )

    def withError( t: TagMod ) = copy( error = Some(t) )
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    val didMount = Callback {
      refresh()
    }

    /**
     * called from threads
     */
    def refresh() = {
      ImportMethods.list().foreach { rlist =>
        rlist match {
          case Right(list) =>
            scope.withEffectsImpure.modState { s => s.copy(stores = Some(list)) }
          case Left(err) =>
            scope.withEffectsImpure.modState { s => s.withError(err) }
        }
      }
    }

    def error( err: String ) = scope.modState( s => s.withError(err) )

    val clearError = scope.modState( s => s.clearError() )

    def setSelected( data: ReactEventFromInput) = data.extract(_.target.files){ files =>
      scope.modState { s =>
        if (files.length == 1) {
          val full = files(0).name
          val i = full.lastIndexOf("\\")
          val f = if (i > 0) full.substring(i+1)
                  else {
                    val j = full.lastIndexOf("/")
                    if (j > 0) full.substring(j+1)
                    else full
                  }
          s.copy( selectedForImport = Some(f) )
        } else {
          s.copy( selectedForImport = None )
        }
      }
    }

    def delete( id: String ) = scope.modState { s =>
      ImportMethods.delete(id).foreach { result =>
        scope.withEffectsImpure.modState { state =>
          result match {
            case Right(r) =>
              refresh()
              state.clearError()
            case Left(err) =>
              state.withError(err)
          }
        }
      }
      s.withError(s"Working on deleting ${id}")
    }

    def render( props: Props, state: State ) = {
      import BaseStyles._
      val importFileText = state.selectedForImport.map( f => s"Selected ${f}" ).getOrElse( "Zipfile to import as a bridgestore" )
      val returnUrl = props.router.urlFor( props.page ).value.replace("#", "%23")
      <.div(
        rootStyles.importsListPageDiv,
        PopupOkCancel( state.error, None, Some(clearError) ),

        <.h1("Import Bridge Store"),
        <.table(
          <.thead(
            <.tr(
              <.th(
                ^.colSpan := 2,
                <.form(
                  ^.action:=s"/v1/import?url=${returnUrl}",
                  ^.method:="post",
                  ^.encType:="multipart/form-data",

                  <.label(
                    importFileText,
                    <.input(
                      ^.`type` := "file",
                      ^.name := "zip",
                      ^.accept := "application/zip",
                      ^.onChange ==> setSelected _
                    )
                  ),
                  <.input(
                    ^.`type` := "submit",
                    ^.name := "submit",
                    ^.value := "Import"
                  )
                ),
              ),
              <.th( "Actions" )
            )
          ),
          <.tbody(
            if (state.stores.isEmpty) {
              <.tr(
                <.td( "Working" )
              )
            } else {
              state.stores.get.imports.zipWithIndex.map { entry =>
                val (store,i) = entry
                SummaryRow.withKey( s"Import${i}" )((props,state,this,i,store))
              }.toTagMod
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

  val SummaryRow = ScalaComponent.builder[(Props,State,Backend,Int,ImportMethods.ImportStore)]("SuggestionRow")
                      .render_P( args => {
                        // row is zero based
                        val (props,state,backend,row,store) = args
                        val storeid = store.id
                        <.tr(
                          <.td( store.id ),
                          <.td( DateUtils.formatDate(store.date) ),
                          <.td(
                            AppButton( s"Duplicate${row}", "Duplicate", props.router.setOnClick(PlayDuplicate(ImportSummaryView(storeid))) ),
                            AppButton( s"Delete${row}", "Delete", ^.onClick --> backend.delete(storeid) )
                          )
                        )
                      }).build

  val component = ScalaComponent.builder[Props]("ImportsListPage")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .build
}

