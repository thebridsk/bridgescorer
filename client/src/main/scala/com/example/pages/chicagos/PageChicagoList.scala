package com.example.pages.chicagos

import scala.scalajs.js

import com.example.controller.ChicagoController
import com.example.data.MatchChicago
import com.example.data.SystemTime
import com.example.data.chicago.ChicagoScoring
import utils.logging.Logger
import com.example.logging.LogLifecycleToServer
import com.example.rest2.RestClientChicago
import com.example.routes.BridgeRouter
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import com.example.react.AppButton
import com.example.react.DateUtils
import com.example.pages.chicagos.ChicagoRouter.NamesView
import com.example.pages.chicagos.ChicagoRouter.SummaryView
import utils.logging.Level
import com.example.rest2.ResultHolder
import com.example.rest2.RequestCancelled
import scala.concurrent.ExecutionContext.Implicits.global
import com.example.data.Id
import com.example.react.PopupOkCancel
import com.example.logger.Alerter
import com.example.react.HelpButton
import com.example.pages.chicagos.ChicagoRouter.ListViewBase
import com.example.pages.chicagos.ChicagoRouter.ImportListView
import com.example.bridge.store.ChicagoSummaryStore
import com.example.pages.chicagos.ChicagoRouter.ListView
import com.example.react.Tooltip
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import com.example.graphql.GraphQLClient
import play.api.libs.json.JsDefined
import play.api.libs.json.JsUndefined
import com.example.data.graphql.GraphQLProtocol.GraphQLResponse
import com.example.react.Utils._

/**
 * @author werewolf
 */
object PageChicagoList {
  import PageChicagoListInternal._

  type CallbackDone = Callback
  type ShowCallback = (/* id: */ String)=>Callback

  case class Props( routerCtl: BridgeRouter[ChicagoPage], page: ListViewBase )

  def apply( routerCtl: BridgeRouter[ChicagoPage], page: ListViewBase ) = component(Props(routerCtl,page))

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
   */
  case class State( workingOnNew: Option[String], askingToDelete: Option[String], popupMsg: Option[String] )

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def delete( id: String ) = scope.modState(s => s.copy( askingToDelete = Some(id)))

    val deleteOK = scope.modState{ s =>
      s.askingToDelete.map { ids =>
        val id = ids.asInstanceOf[Id.MatchChicago]
        ChicagoController.deleteChicago(id)
        s.copy( askingToDelete = None )
      }.getOrElse {
        s.copy( workingOnNew = Some("Not ready to delete a chicago match") )
      }

    }

    val deleteCancel = scope.modState(s => s.copy( askingToDelete = None))

    val resultChicago = ResultHolder[MatchChicago]()
    val resultGraphQL = ResultHolder[GraphQLResponse]()

    val cancel = Callback {
      resultChicago.cancel()
      resultGraphQL.cancel()
    } >> scope.modState( s => s.copy(workingOnNew=None, popupMsg=None))

    val newChicago = {
      import scala.concurrent.ExecutionContext.Implicits.global
      scope.modState( s => s.copy(workingOnNew=Some("Creating a new Chicago match...")), Callback {
        logger.info(s"Creating new chicago.  HomePage.mounted=${mounted}")
        val result = ChicagoController.createMatch()
        resultChicago.set(result)
        result.foreach { created =>
          logger.info(s"Got new chicago ${created.id}.  HomePage.mounted=${mounted}")
          if (mounted) {
            scope.withEffectsImpure.props.routerCtl.set(NamesView(created.id,0)).runNow()
          }
        }
        result.failed.foreach( t => {
          t match {
            case x: RequestCancelled =>
            case _ =>
              scope.withEffectsImpure.modState( s => s.copy(workingOnNew=Some("Failed to create a new Chicago match")))
          }
        })
      })

    }
//      scope.modState( s => s.copy( workingOnNew = Some("Creating new...") )) >> ChicagoController.createMatch(
//        created=> {
//          logger.info("Got new chicago "+created.id)
//          scope.props.runNow.routerCtl.set(NamesView(created.id,0)).runNow()
//        }
//      )

    def showChicago( chi: MatchChicago ) = Callback {
      ChicagoController.showMatch( chi )
    } >> scope.props >>= { props => props.routerCtl.set(SummaryView(chi.id)) }


    def render(props: Props, state:State) = {
      ChicagoSummaryStore.getChicagoSummary() match {
        case Some(chicagosRaw) =>
          val importId = props.page match {
            case ilv: ImportListView => Some(ilv.getDecodedId)
            case _ => None
          }
          val chicagos = chicagosRaw.sortWith((l,r) => Id.idComparer(l.id, r.id) > 0)
          val maxplayers = chicagos.map( mc => mc.players.length ).foldLeft(4){case (m, i) => math.max(m,i)}
          val (msg,funOk,funCancel) = state.popupMsg.map( msg => (Some(msg),None,Some(cancel))).
                                         getOrElse(
                                           (
                                             state.askingToDelete.map(id => s"Are you sure you want to delete Chicago match ${id}"),
                                             Some(deleteOK),
                                             Some(deleteCancel)
                                           )
                                         )
          <.div( chiStyles.chicagoListPage,
              PopupOkCancel(msg.map(s=>s),funOk,funCancel),
              <.table(
                  <.thead(
                    <.tr(
                      <.th( "Id"),
                      importId.map { id =>
                        TagMod(
                          <.th("Import from"),
                          <.th("Best Match")
                        )
                      }.whenDefined,
                      <.th( "Created", <.br(), "Updated"),
                      <.th( "Players - Scores", ^.colSpan:=maxplayers),
                      <.th( "")
                  )),
                  <.tbody(
                      ChicagoRowFirst.withKey("New")((this,props,state,maxplayers,importId)),
                      (0 until chicagos.length).map { i =>
                        val key="Game"+i
                        val chicago = ChicagoScoring(chicagos(i))
                        ChicagoRow.withKey(key)((this,props,state,i,maxplayers,chicago,importId))
                      }.toTagMod
                  )
              ),
              <.div(
                baseStyles.divFooter,
                <.div(
                  baseStyles.divFooterLeft,
                  AppButton( "Home", "Home", props.routerCtl.home )
                ),
                <.div(
                  baseStyles.divFooterLeft,
                  HelpButton("/help/chicago/list.html")
                )
              )
          )
        case None =>
          <.div(
              chiStyles.chicagoListPage,
              "Loading ..."
          )
      }
    }

    def setMessage( msg: String ) = scope.withEffectsImpure.modState( s => s.copy( popupMsg = Some(msg)) )

    def importChicago( importId: String, id: String ) =
      scope.modState( s => s.copy(popupMsg=Some(s"Importing Chicago Match ${id} from import ${importId}")), Callback {
        val query = """mutation importChicago( $importId: ImportId!, $chiId: ChicagoId! ) {
                      |  import( id: $importId ) {
                      |    importchicago( id: $chiId ) {
                      |      id
                      |    }
                      |  }
                      |}
                      |""".stripMargin
        val vars = JsObject( Seq( "importId" -> JsString(importId), "chiId" -> JsString(id) ) )
        val op = Some("importChicago")
        val result = GraphQLClient.request(query, Some(vars), op)
        resultGraphQL.set(result)
        result.map { gr =>
          gr.data match {
            case Some(data) =>
              data \ "import" \ "importchicago" \ "id" match {
                case JsDefined( JsString( newid ) ) =>
                  setMessage(s"import chicago ${id} from ${importId}, new ID ${newid}" )
                case JsDefined( x ) =>
                  setMessage(s"expecting string on import chicago ${id} from ${importId}, got ${x}")
                case _: JsUndefined =>
                  setMessage(s"error import chicago ${id} from ${importId}, did not find import/importchicago/id field")
              }
            case None =>
              setMessage(s"error import chicago ${id} from ${importId}, ${gr.getError()}")
          }
        }.recover {
          case x: Exception =>
              logger.warning(s"exception import chicago ${id} from ${importId}", x)
              setMessage(s"exception import chicago ${id} from ${importId}")
        }.foreach { x => }
      })

    private var mounted = false

    val storeCallback = scope.forceUpdate

    val didMount = scope.props >>= { (p) => Callback {
      mounted = true

      // make AJAX rest call here
      logger.finer("PageChicagoList: Sending chicagos list request to server")
      ChicagoSummaryStore.addChangeListener(storeCallback)
      p.page match {
        case isv: ImportListView =>
          val importId = isv.getDecodedId
          ChicagoController.getImportSummary(importId)
        case ListView =>
          ChicagoController.getSummary()
      }

    }}

    val willUnmount = Callback {
      mounted = false
    }

  }

  val ChicagoRowFirst = ScalaComponent.builder[(Backend, Props, State, Int, Option[String])]("ChicagoRowFirst")
    .stateless
    .render_P { args =>
      val (backend, props, state, maxplayers, importId) = args
      <.tr(
          <.td( "" ),
          importId.map { id =>
            TagMod(
              <.th(id),
              <.th( "" ),
              <.th( "" )
            )
          }.getOrElse(
            <.td(
              state.workingOnNew match {
                case Some(msg) =>
                  <.span(
                    msg,
                    " ",
                    AppButton(
                      "PopupCancel", "Cancel",
                      ^.onClick --> backend.cancel
                    )
                  )
                case None =>
                  AppButton( "New", "New", ^.onClick --> backend.newChicago)
              }
            )
          ),
          <.td( ^.colSpan:=maxplayers,"" ),
          <.td( "")
          )
    }.build

  val ChicagoRow = ScalaComponent.builder[(Backend, Props, State, Int, Int, ChicagoScoring, Option[String])]("ChicagoRow")
    .stateless
    .render_P( args => {
      val (backend, props, state, game, maxplayers, chicago, importId) = args
      val id = chicago.chicago.id
      val date = id
      val created = DateUtils.showDate(chicago.chicago.created)
      val updated = DateUtils.showDate(chicago.chicago.updated)

      val (players,scores) = chicago.sortedResults()

      <.tr(
          <.td(
                AppButton( "Chicago"+id, id,
                           baseStyles.appButton100,
                           ^.onClick --> backend.showChicago(chicago.chicago),
                           importId.map { id => ^.disabled := true }.whenDefined
                         )
              ),
          importId.whenDefined { iid =>
            TagMod(
              <.td(
                AppButton( "ImportChicago_"+id, "Import",
                           baseStyles.appButton100,
                           ^.onClick --> backend.importChicago(iid,id)
                         )
              ),
              <.td(
                chicago.chicago.bestMatch.map { bm =>
                  if (bm.id.isDefined && bm.sameness > 90) {
                    val title = bm.htmlTitle
                    TagMod(Tooltip(
                      f"""${bm.id.get} ${bm.sameness}%.2f%%""",
                      <.div( title )
                    ))
                  } else {
                    TagMod()
                  }
                }.whenDefined
              )
            )
          },
          <.td( created,<.br(),updated),
          (0 until players.length).map { i =>
            <.td( players(i)+" - "+scores(i).toString)
          }.toTagMod,
          (players.length until maxplayers ).map { i =>
            <.td( )
          }.toTagMod,
          <.td(
              importId.isEmpty ?= AppButton( "Delete", "Delete", ^.onClick --> backend.delete(id) )
          )
        )
    }).build

  val component = ScalaComponent.builder[Props]("PageChicagoList")
                            .initialStateFromProps { props => State(None, None, None) }
                            .backend(new Backend(_))
                            .renderBackend
//                            .configure(LogLifecycleToServer.verbose)     // logs lifecycle events
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

