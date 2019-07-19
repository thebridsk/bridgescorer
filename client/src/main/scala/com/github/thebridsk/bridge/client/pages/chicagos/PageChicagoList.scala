package com.github.thebridsk.bridge.client.pages.chicagos

import scala.scalajs.js

import com.github.thebridsk.bridge.client.controller.ChicagoController
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.SystemTime
import com.github.thebridsk.bridge.data.chicago.ChicagoScoring
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.logging.LogLifecycleToServer
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientChicago
import com.github.thebridsk.bridge.client.routes.BridgeRouter
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.DateUtils
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoRouter.NamesView
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoRouter.SummaryView
import com.github.thebridsk.utilities.logging.Level
import com.github.thebridsk.bridge.clientcommon.rest2.ResultHolder
import com.github.thebridsk.bridge.clientcommon.rest2.RequestCancelled
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import com.github.thebridsk.bridge.clientcommon.react.HelpButton
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoRouter.ListViewBase
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoRouter.ImportListView
import com.github.thebridsk.bridge.client.bridge.store.ChicagoSummaryStore
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoRouter.ListView
import com.github.thebridsk.bridge.clientcommon.react.Tooltip
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import com.github.thebridsk.bridge.clientcommon.graphql.GraphQLClient
import play.api.libs.json.JsDefined
import play.api.libs.json.JsUndefined
import com.github.thebridsk.bridge.data.graphql.GraphQLProtocol.GraphQLResponse
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.materialui.TextVariant
import com.github.thebridsk.materialui.TextColor
import com.github.thebridsk.materialui.MuiTypography

/**
  * @author werewolf
  */
object PageChicagoList {
  import PageChicagoListInternal._

  type CallbackDone = Callback
  type ShowCallback = ( /* id: */ String) => Callback

  case class Props(routerCtl: BridgeRouter[ChicagoPage], page: ListViewBase)

  def apply(routerCtl: BridgeRouter[ChicagoPage], page: ListViewBase) =
    component(Props(routerCtl, page))

}

object PageChicagoListInternal {
  import PageChicagoList._
  import ChicagoStyles._

  implicit val logger = Logger("bridge.PageChicagoList")
  implicit val defaultTraceLevelForReactComponents = Level.FINER

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause State to leak.
    *
    * @constructor
    * @param askingToDelete The Id of rubber match being deleted.  None if not deleting.
    * @param popupMsg show message in popup if not None.
    */
  case class State(
      askingToDelete: Option[String] = None,
      popupMsg: Option[String] = None,
      info: Boolean = false
  )

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause Backend to leak.
    *
    */
  class Backend(scope: BackendScope[Props, State]) {

    def delete(id: String) =
      scope.modState(s => s.copy(askingToDelete = Some(id)))

    val deleteOK = scope.modState { s =>
      s.askingToDelete
        .map { ids =>
          val id = ids.asInstanceOf[Id.MatchChicago]
          ChicagoController.deleteChicago(id)
          s.copy(askingToDelete = None)
        }
        .getOrElse {
          s.copy(popupMsg = Some("Not ready to delete a chicago match"))
        }

    }

    val deleteCancel = scope.modState(s => s.copy(askingToDelete = None))

    val resultChicago = ResultHolder[MatchChicago]()
    val resultGraphQL = ResultHolder[GraphQLResponse]()

    val cancel = Callback {
      resultChicago.cancel()
      resultGraphQL.cancel()
    } >> scope.modState(s => s.copy(popupMsg = None))

    val newChicago = {
      import scala.concurrent.ExecutionContext.Implicits.global
      scope.modState(
        s => s.copy(popupMsg = Some("Creating a new Chicago match...")),
        Callback {
          logger.info(s"Creating new chicago.  HomePage.mounted=${mounted}")
          val result = ChicagoController.createMatch()
          resultChicago.set(result)
          result.foreach { created =>
            logger.info(
              s"Got new chicago ${created.id}.  HomePage.mounted=${mounted}"
            )
            if (mounted) {
              scope.withEffectsImpure.props.routerCtl
                .set(NamesView(created.id, 0))
                .runNow()
            }
          }
          result.failed.foreach(t => {
            t match {
              case x: RequestCancelled =>
              case _ =>
                scope.withEffectsImpure.modState(
                  s =>
                    s.copy(
                      popupMsg = Some("Failed to create a new Chicago match")
                    )
                )
            }
          })
        }
      )

    }
//      scope.modState( s => s.copy( workingOnNew = Some("Creating new...") )) >> ChicagoController.createMatch(
//        created=> {
//          logger.info("Got new chicago "+created.id)
//          scope.props.runNow.routerCtl.set(NamesView(created.id,0)).runNow()
//        }
//      )

    def showChicago(chi: MatchChicago) =
      Callback {
        ChicagoController.showMatch(chi)
      } >> scope.props >>= { props =>
        props.routerCtl.set(SummaryView(chi.id))
      }

    def render(props: Props, state: State) = {

      val (bok, bcancel) = if (state.info) {
        (Some(cancel), None)
      } else {
        (None, Some(cancel))
      }
      val (msg, funOk, funCancel) = state.popupMsg
        .map(msg => (Some(msg), bok, bcancel))
        .getOrElse(
          (
            state.askingToDelete.map(
              id => s"Are you sure you want to delete Chicago match ${id}"
            ),
            Some(deleteOK),
            Some(deleteCancel)
          )
        )
      <.div(
        chiStyles.chicagoListPage,
        PopupOkCancel(msg.map(s => s), funOk, funCancel),
        ChicagoPageBridgeAppBar(
          title = Seq[CtorType.ChildArg](
            MuiTypography(
              variant = TextVariant.h6,
              color = TextColor.inherit
            )(
              <.span("List")
            )
          ),
          helpurl = "../help/chicago/list.html",
          routeCtl = props.routerCtl
        )(),
        <.div(
          ChicagoSummaryStore.getChicagoSummary() match {
            case Some(chicagosRaw) =>
              val importId = props.page match {
                case ilv: ImportListView => Some(ilv.getDecodedId)
                case _                   => None
              }
              val chicagos =
                chicagosRaw.sortWith((l, r) => Id.idComparer(l.id, r.id) > 0)
              val maxplayers =
                chicagos.map(mc => mc.players.length).foldLeft(4) {
                  case (m, i) => math.max(m, i)
                }
              <.table(
                <.thead(
                  <.tr(
                    <.th("Id"),
                    importId.map { id =>
                      TagMod(
                        <.th("Import from"),
                        <.th("Best Match")
                      )
                    }.whenDefined,
                    <.th("Created", <.br(), "Updated"),
                    <.th("Players - Scores", ^.colSpan := maxplayers),
                    <.th("")
                  ),
                  ChicagoRowFirst.withKey("New")(
                    (this, props, state, maxplayers, importId)
                  )
                ),
                <.tbody(
                  (0 until chicagos.length).map { i =>
                    val key = "Game" + i
                    val chicago = ChicagoScoring(chicagos(i))
                    ChicagoRow.withKey(key)(
                      (this, props, state, i, maxplayers, chicago, importId)
                    )
                  }.toTagMod
                )
              )
            case None =>
              <.h1("Loading ...")
          }
        )
      )
    }

    def setMessage(msg: String, info: Boolean = false) =
      scope.withEffectsImpure.modState(
        s => s.copy(popupMsg = Some(msg), info = info)
      )

    def importChicago(importId: String, id: String) =
      scope.modState(
        s =>
          s.copy(
            popupMsg =
              Some(s"Importing Chicago Match ${id} from import ${importId}")
          ),
        Callback {
          val query =
            """mutation importChicago( $importId: ImportId!, $chiId: ChicagoId! ) {
              |  import( id: $importId ) {
              |    importchicago( id: $chiId ) {
              |      id
              |    }
              |  }
              |}
              |""".stripMargin
          val vars = JsObject(
            Seq("importId" -> JsString(importId), "chiId" -> JsString(id))
          )
          val op = Some("importChicago")
          val result = GraphQLClient.request(query, Some(vars), op)
          resultGraphQL.set(result)
          result
            .map {
              gr =>
                gr.data match {
                  case Some(data) =>
                    data \ "import" \ "importchicago" \ "id" match {
                      case JsDefined(JsString(newid)) =>
                        setMessage(
                          s"import chicago ${id} from ${importId}, new ID ${newid}",
                          true
                        )
                      case JsDefined(x) =>
                        setMessage(
                          s"expecting string on import chicago ${id} from ${importId}, got ${x}"
                        )
                      case _: JsUndefined =>
                        setMessage(
                          s"error import chicago ${id} from ${importId}, did not find import/importchicago/id field"
                        )
                    }
                  case None =>
                    setMessage(
                      s"error import chicago ${id} from ${importId}, ${gr.getError()}"
                    )
                }
            }
            .recover {
              case x: Exception =>
                logger.warning(
                  s"exception import chicago ${id} from ${importId}",
                  x
                )
                setMessage(s"exception import chicago ${id} from ${importId}")
            }
            .foreach { x =>
            }
        }
      )

    private var mounted = false

    val storeCallback = scope.forceUpdate

    def summaryError() =
      scope.withEffectsImpure.modState(
        s => s.copy(popupMsg = Some("Error getting duplicate summary"))
      )

    val didMount = scope.props >>= { (p) =>
      Callback {
        mounted = true

        // make AJAX rest call here
        logger.finer("PageChicagoList: Sending chicagos list request to server")
        ChicagoSummaryStore.addChangeListener(storeCallback)
        p.page match {
          case isv: ImportListView =>
            val importId = isv.getDecodedId
            ChicagoController.getImportSummary(importId, summaryError _)
          case ListView =>
            ChicagoController.getSummary(summaryError _)
        }

      }
    }

    val willUnmount = Callback {
      mounted = false
    }

  }

  val ChicagoRowFirst = ScalaComponent
    .builder[(Backend, Props, State, Int, Option[String])]("ChicagoRowFirst")
    .stateless
    .render_P { args =>
      val (backend, props, state, maxplayers, importId) = args
      <.tr(
        <.th(""),
        importId
          .map { id =>
            TagMod(
              <.th(id),
              <.th(""),
              <.th("")
            )
          }
          .getOrElse(
            <.th(
              AppButton("New", "New", ^.onClick --> backend.newChicago)
            )
          ),
        <.th(^.colSpan := maxplayers, ""),
        <.th("")
      )
    }
    .build

  val ChicagoRow = ScalaComponent
    .builder[(Backend, Props, State, Int, Int, ChicagoScoring, Option[String])](
      "ChicagoRow"
    )
    .stateless
    .render_P(args => {
      val (backend, props, state, game, maxplayers, chicago, importId) = args
      val id = chicago.chicago.id
      val date = id
      val created = DateUtils.showDate(chicago.chicago.created)
      val updated = DateUtils.showDate(chicago.chicago.updated)

      val (players, scores) = chicago.sortedResults()

      <.tr(
        <.td(
          AppButton(
            "Chicago" + id,
            id,
            baseStyles.appButton100,
            ^.onClick --> backend.showChicago(chicago.chicago),
            importId.map { id =>
              ^.disabled := true
            }.whenDefined
          )
        ),
        importId.whenDefined {
          iid =>
            TagMod(
              <.td(
                AppButton(
                  "ImportChicago_" + id,
                  "Import",
                  baseStyles.appButton100,
                  ^.onClick --> backend.importChicago(iid, id)
                )
              ),
              <.td(
                chicago.chicago.bestMatch.map { bm =>
                  if (bm.id.isDefined && bm.sameness > 90) {
                    val title = bm.htmlTitle
                    TagMod(
                      Tooltip(
                        f"""${bm.id.get} ${bm.sameness}%.2f%%""",
                        <.div(title)
                      )
                    )
                  } else {
                    TagMod()
                  }
                }.whenDefined
              )
            )
        },
        <.td(created, <.br(), updated),
        (0 until players.length).map { i =>
          <.td(players(i) + " - " + scores(i).toString)
        }.toTagMod,
        (players.length until maxplayers).map { i =>
          <.td()
        }.toTagMod,
        <.td(
          importId.isEmpty ?= AppButton(
            "Delete",
            "Delete",
            ^.onClick --> backend.delete(id)
          )
        )
      )
    })
    .build

  val component = ScalaComponent
    .builder[Props]("PageChicagoList")
    .initialStateFromProps { props =>
      State()
    }
    .backend(new Backend(_))
    .renderBackend
//                            .configure(LogLifecycleToServer.verbose)     // logs lifecycle events
    .componentDidMount(scope => scope.backend.didMount)
    .componentWillUnmount(scope => scope.backend.willUnmount)
    .build
}
