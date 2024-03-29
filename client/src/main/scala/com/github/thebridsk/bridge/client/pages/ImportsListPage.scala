package com.github.thebridsk.bridge.client.pages

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.graphql.GraphQLClient
import play.api.libs.json.JsDefined
import com.github.thebridsk.bridge.clientcommon.BridgeExecutionContext.global
import play.api.libs.json.JsString
import play.api.libs.json.JsObject
import play.api.libs.json.JsNull
import play.api.libs.json.JsTrue
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.data.SystemTime.Timestamp
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
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles._
import com.github.thebridsk.bridge.data.ImportStoreConstants
import com.github.thebridsk.bridge.clientcommon.pages.ColorThemeStorage
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.clientcommon.react.Utils
import org.scalajs.dom.FileList
import org.scalajs.dom.FormData
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxResult
import scala.util.Success
import scala.util.Failure
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxDisabled
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxErrorReturn
import com.github.thebridsk.bridge.data.rest.JsonSupport
import com.github.thebridsk.bridge.data.RestMessage

import org.scalajs.dom.File
import play.api.libs.json.Reads
import scala.util.matching.Regex

/**
  * A component page to list all imports, and to allow importing.
  *
  * To use, just code the following:
  *
  * {{{
  * ImportsListPage(
  *   router = ...,
  *   page = ImportsList,
  * )
  * }}}
  *
  * @author werewolf
  */
object ImportsListPage {
  import Internal._

  case class Props(router: BridgeRouter[AppPage], page: AppPage)


  /**
    * Instantiate the component
    *
    * @param router
    * @param page - a ImportsList object
    * @return the unmounted react component
    */
  def apply(router: BridgeRouter[AppPage], page: AppPage) =
    component(
      Props(router, page)
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

  protected object Internal {
    import ImportGraphQL.ImportsList

    val logger: Logger = Logger("bridge.ImportsListPage")

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause State to leak.
      */
    case class State(
        stores: Option[ImportsList] = None,
        error: Option[TagMod] = None
    ) {

      def clearError: State = copy(error = None)

      def withError(s: String): State = copy(error = Some(s))

      def withError(t: TagMod): State = copy(error = Some(t))
    }

    val patternName: Regex = """(?:.*[\\/])([^\\/]+)""".r

    /**
      * Internal state for rendering the component.
      *
      * I'd like this class to be private, but the instantiation of component
      * will cause Backend to leak.
      */
    class Backend(scope: BackendScope[Props, State]) {

      val refreshCB: Callback = Callback {
        refresh()
      }

      val didMount = refreshCB

      /**
        * called from threads
        */
      def refresh(): Unit = {
        ImportGraphQL.list().foreach { rlist =>
          rlist match {
            case Right(list) =>
              scope.withEffectsImpure.modState { s =>
                s.copy(stores = Some(list)).clearError
              }
            case Left(err) =>
              scope.withEffectsImpure.modState { s => s.withError(err) }
          }
        }
      }

      def error(err: String): Callback = scope.modState(s => s.withError(err))

      val clearError: Callback = scope.modState(s => s.clearError)

      def getFile(filelist: FileList): Option[(String, File)] = {
        if (filelist.length == 1) {
          val file = filelist(0)
          file.name match {
            case patternName(f) => Some((f, file))
            case f              => Some((f, file))
          }
        } else {
          None
        }
      }

      import Utils._
      def doInput(returnUrl: String, theme: String)(
          e: ReactEventFromInput
      ): Callback =
        e.preventDefaultAction.inputFiles { filelist =>
          getFile(filelist) match {
            case Some((name, file)) =>
              scope.modState(
                s => s.withError(s"Importing $name ..."),
                Callback {
                  val formData = new FormData
                  formData.append("zip", file)
                  AjaxResult
                    .post(s"/v1/import?url=${returnUrl}${theme}", formData)
                    .recordFailure()
                    .onComplete { twx =>
                      twx match {
                        case Success(value) =>
                          scope.withEffectsImpure.modState(
                            s =>
                              s.withError(
                                "Import successful, retrieving information"
                              ),
                            refreshCB
                          )
                        case Failure(x) =>
                          val errmsg = x match {
                            case ex: AjaxDisabled =>
                              "Import failed, not connected to server"
                            case ex: AjaxErrorReturn =>
                              import JsonSupport._
                              val errmsg = readJson[RestMessage](ex.body)
                              errmsg.msg
                            case x: Exception =>
                              "Import failed, error on server"
                          }
                          scope.withEffectsImpure
                            .modState(s => s.withError(errmsg))
                      }
                    }
                }
              )
            case None =>
              scope.modState(s => s.withError("No file was selected"))
          }
        }

      def delete(id: String): Callback =
        scope.modState { s =>
          ImportGraphQL.delete(id).foreach { result =>
            scope.withEffectsImpure.modState { state =>
              result match {
                case Right(r) =>
                  refresh()
                  state.clearError
                case Left(err) =>
                  state.withError(err)
              }
            }
          }
          s.withError(s"Working on deleting ${id}")
        }

      def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
        val importFileText = "Select Bridgestore file"
        val returnUrl = props.router.urlFor(props.page).value.replace("#", "%23")
        val theme = ColorThemeStorage
          .getColorThemeFromBody()
          .map(t => s"""&theme=$t""")
          .getOrElse("")
        <.div(
          rootStyles.importsListPageDiv,
          PopupOkCancel(state.error, None, Some(clearError)),
          RootBridgeAppBar(
            Seq(
              MuiTypography(
                variant = TextVariant.h6,
                color = TextColor.inherit
              )(
                <.span(
                  "Import Bridge Store"
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
                  <.th("Id"),
                  <.th("Created"),
                  <.th("Actions")
                )
              ),
              <.tbody(
                <.tr(
                  <.td(
                    <.label(
                      importFileText,
                      BaseStyles.baseStyles.required,
                      <.input(
                        ^.`type` := "file",
                        ^.name := "zip",
                        ^.accept := s".${ImportStoreConstants.importStoreFileExtension},application/zip",
                        ^.value := "",
                        ^.onChange ==> doInput(returnUrl, theme) _
                      )
                    )
                  ),
                  <.td(),
                  <.td(
                  )
                ),
                if (state.stores.isEmpty) {
                  <.tr(
                    <.td("Working")
                  )
                } else {
                  state.stores.get.imports.zipWithIndex.map { entry =>
                    val (store, i) = entry
                    SummaryRow.withKey(s"Import${i}")(
                      (props, state, this, i, store)
                    )
                  }.toTagMod
                }
              )
            )
          )
        )
      }
    }

    val SummaryRow = ScalaComponent
      .builder[(Props, State, Backend, Int, ImportGraphQL.ImportStore)](
        "SuggestionRow"
      )
      .render_P(args => {
        // row is zero based
        val (props, state, backend, row, store) = args
        val storeid = store.id
        <.tr(
          <.td(store.id),
          <.td(DateUtils.formatDate(store.date)),
          <.td(
            AppButton(
              s"Duplicate${row}",
              "Duplicate",
              props.router.setOnClick(PlayDuplicate(ImportSummaryView(storeid)))
            ).when(store.duplicatesCount + store.duplicateresultsCount > 0),
            AppButton(
              s"Chicago${row}",
              "Chicago",
              props.router.setOnClick(
                PlayChicago2(ChicagoRouter.ImportListView(storeid))
              )
            ).when(store.chicagosCount > 0),
            AppButton(
              s"Rubber${row}",
              "Rubber",
              props.router.setOnClick(
                PlayRubber(RubberRouter.ImportListView(storeid))
              )
            ).when(store.rubbersCount > 0),
            AppButton(
              s"Delete${row}",
              "Delete",
              ^.onClick --> backend.delete(storeid)
            )
          )
        )
      })
      .build

    val component = ScalaComponent
      .builder[Props]("ImportsListPage")
      .initialStateFromProps { props => State() }
      .backend(new Backend(_))
      .renderBackend
      .componentDidMount(scope => scope.backend.didMount)
      .build
  }

}

object ImportGraphQL {

  val logger: Logger = Logger("bridge.ImportsListPage.ImportMethods")

  /**
    * Delete an import object.
    *
    * Usage:
    * {{{
    * ImportGraphQL.delete(id).onComplete { t =>
    *   t match {
    *     case Success(v) =>
    *       v match {
    *         case Right(b) =>
    *         case Left(errmsg) =>
    *       }
    *     case Failure(ex) =>
    *   }
    * }
    * }}}
    *
    * @param id
    * @return An AjaxResult object.  This object extends the Future interface.
    *         The future that is returned will return a Right(true) if the import was deleted,
    *         and a Left(errmsg) for failures.
    */
  def delete(id: String): AjaxResult[Either[String, Boolean]] = {

    val vars = JsObject(Seq("iid" -> JsString(id)))
    val query =
      """
         mutation DeleteImport($iid: ImportId!) {
           import(id: $iid) {
             delete
           }
         }
       """
    val operation = Some("DeleteImport")

    GraphQLClient.request(query, Some(vars), operation).map { resp =>
      resp.data match {
        case Some(d: JsObject) =>
          d \ "import" match {
            case JsDefined(JsNull) =>
              logger.warning(s"deleteImport(${id}) return: not found")
              Left(s"Import store ${id} not found")
            case JsDefined(di: JsObject) =>
              if (di \ "delete" == JsDefined(JsTrue)) {
                Right(true)
              } else {
                Left(s"Import store ${id} was not deleted")
              }
            case _ =>
              logger
                .warning(s"Unexpected response on deleteImport(${id}): ${resp}")
              Left("Internal error")
          }
        case _ =>
          logger.warning(s"Error on deleteImport(${id}): ${resp}")
          Left("Internal error")
      }
    }

  }

  case class ImportStore(
      id: String,
      date: Timestamp,
      duplicatesCount: Int,
      duplicateresultsCount: Int,
      chicagosCount: Int,
      rubbersCount: Int
  )
  case class ImportsList(imports: List[ImportStore])

  implicit val readsImportStore: Reads[ImportStore] = Json.reads[ImportStore]
  implicit val readsImportsList: Reads[ImportsList] = Json.reads[ImportsList]


  /**
    * Get information on all import objects.
    *
    * Usage:
    * {{{
    * ImportGraphQL.list().onComplete { t =>
    *   t match {
    *     case Success(v) =>
    *       v match {
    *         case Right(importsList) =>
    *           importsList.imports.foreach { i => ... }
    *         case Left(errmsg) =>
    *       }
    *     case Failure(ex) =>
    *   }
    * }
    * }}}
    *
    * @return An AjaxResult object.  This object extends the Future interface.
    *         The future that is returned will return a Right(importsList) if no error occurred,
    *         and a Left(errmsg) for failures.
    */
  def list(): AjaxResult[Either[String, ImportsList]] = {

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

    GraphQLClient
      .request(query, vars, operation)
      .map { resp =>
        resp.data match {
          case Some(d: JsObject) =>
            Json.fromJson[ImportsList](d) match {
              case JsSuccess(t, path) =>
                Right(t)
              case err: JsError =>
                logger.warning(
                  s"Error processing return data from Imports list: ${JsError.toJson(err)}"
                )
                Left("Error processing returned data")
            }
          case _ =>
            logger.warning(s"Error on Imports list: ${resp}")
            Left("Internal error")
        }
      }
      .recover {
        case x: Exception =>
          logger.warning(s"Error on Imports list", x)
          Left("Internal error")
      }

  }
}
