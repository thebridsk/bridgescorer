package com.github.thebridsk.bridge.client.pages

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientServerURL
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientServerVersion
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.ServerURL
import com.github.thebridsk.bridge.data.ServerVersion
import com.github.thebridsk.bridge.client.version.VersionClient
import com.github.thebridsk.bridge.version.VersionShared
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.client.routes.AppRouter.Home
import com.github.thebridsk.bridge.clientcommon.graphql.GraphQLClient
import play.api.libs.json.JsDefined
import play.api.libs.json.JsUndefined
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import play.api.libs.json.JsObject
import play.api.libs.json.JsNull
import play.api.libs.json.JsTrue
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.data.SystemTime.Timestamp
import play.api.libs.json.Reads
import play.api.libs.json.Json
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import com.github.thebridsk.bridge.clientcommon.react.DateUtils
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateRouter.ImportSummaryView
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateModule.PlayDuplicate
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoRouter
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoModule.PlayChicago2
import com.github.thebridsk.bridge.client.pages.rubber.RubberModule.PlayRubber
import com.github.thebridsk.bridge.client.pages.rubber.RubberRouter
import com.github.thebridsk.materialui.MuiTypography
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.bridge.client.routes.BridgeRouter

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

  case class Props( router: BridgeRouter[AppPage], page: AppPage )

  def apply( router: BridgeRouter[AppPage], page: AppPage ) = component( Props(router,page))

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

  case class ImportStore( id: String, date: Timestamp, duplicatesCount: Int, duplicateresultsCount: Int, chicagosCount: Int, rubbersCount: Int )
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
         |    duplicatesCount
         |    duplicateresultsCount
         |    chicagosCount
         |    rubbersCount
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
        RootBridgeAppBar(
            Seq(MuiTypography(
                    variant = TextVariant.h6,
                    color = TextColor.inherit,
                )(
                    <.span(
                      "Import Bridge Store",
                    )
                )
            ),
            None,
            props.router
        )(),
        <.div(
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
          )
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
                            AppButton( s"Duplicate${row}", "Duplicate", props.router.setOnClick(PlayDuplicate(ImportSummaryView(storeid))) ).when(store.duplicatesCount+store.duplicateresultsCount>0),
                            AppButton( s"Chicago${row}", "Chicago", props.router.setOnClick(PlayChicago2(ChicagoRouter.ImportListView(storeid))) ).when(store.chicagosCount>0),
                            AppButton( s"Rubber${row}", "Rubber", props.router.setOnClick(PlayRubber(RubberRouter.ImportListView(storeid))) ).when(store.rubbersCount>0),
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
