package com.example.controller

import com.example.bridge.action.BridgeDispatcher
import com.example.data.MatchDuplicate
import scala.scalajs.js.annotation.ScalaJSDefined
import com.example.data.websocket.Protocol
import com.example.rest2.RestClientDuplicate
import utils.logging.Logger
import com.example.bridge.store.DuplicateStore
import com.example.data.Id
import japgolly.scalajs.react.Callback
import com.example.routes.AppRouter
import japgolly.scalajs.react.CallbackTo
import com.example.data.DuplicateHand
import com.example.data.Team
import com.example.websocket.DuplexPipe
import com.example.data.websocket.DuplexProtocol.LogEntryS
import com.example.data.websocket.DuplexProtocol.LogEntryV2
import com.example.rest2.Result
import scala.concurrent.ExecutionContext
import com.example.rest2.RestResult
import com.example.data.RestMessage
import com.example.rest2.RestClientDuplicateSummary
import com.example.logger.Alerter
import com.example.bridge.store.DuplicateResultStore
import com.example.rest2.RestClientDuplicateResult
import scala.concurrent.Future
import com.example.rest2.AjaxResult
import org.scalactic.source.Position
import com.example.rest2.WrapperXMLHttpRequest
import com.example.graphql.GraphQLClient
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsDefined
import play.api.libs.json.JsError
import play.api.libs.json.JsUndefined
import play.api.libs.json.Json
import com.example.data.DuplicateSummary
import com.example.data.rest.JsonSupport
import play.api.libs.json.JsSuccess
import org.scalajs.dom.raw.EventSource
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.raw.Event
import scala.scalajs.js.timers.SetTimeoutHandle
import com.example.Bridge
import com.example.bridge.store.DuplicateSummaryStore
import scala.util.Success
import scala.util.Failure
import com.example.rest2.AjaxCall
import scala.concurrent.duration.Duration
import com.example.data.websocket.Protocol.ToBrowserMessage
import com.example.data.websocket.Protocol.ToServerMessage

object Controller extends  {
  val logger = Logger("bridge.Controller")

  class AlreadyStarted extends Exception

  private var sseConnection: ServerEventConnection[Id.MatchDuplicate] = null

  private var duplexPipe: Option[DuplexPipe] = None

  var useRestToServer: Boolean = true;
  private var useSSEFromServer: Boolean = true;

  def setUseSSEFromServer( b: Boolean ) = {
    if (b != useSSEFromServer) {
      useSSEFromServer = b
      setServerEventConnection()
    }
  }

  setServerEventConnection()

  private def setServerEventConnection(): Unit = {
    sseConnection = if (useSSEFromServer) {
      new SSE[Id.MatchDuplicate]( "/v1/sse/duplicates/", Listener)
    } else {
      new DuplexPipeServerEventConnection( "", Listener ) {

        def actionStartMonitor( mdid: Id.MatchDuplicate ): ToServerMessage = {
          Protocol.StartMonitorDuplicate(mdid)
        }
        def actionStopMonitor( mdid: Id.MatchDuplicate ): ToServerMessage = {
          Protocol.StopMonitorDuplicate(mdid)
        }

      }
    }
  }

  def log( entry: LogEntryS ) = {
    // This can't create a duplexPipe, we haven't setup all the info
    duplexPipe match {
      case Some(dp) => dp.sendlog(entry)
      case None =>
    }
  }

  def log( entry: LogEntryV2 ) = {
    // This can't create a duplexPipe, we haven't setup all the info
    duplexPipe match {
      case Some(dp) => dp.sendlog(entry)
      case None =>
    }
  }

  class CreateResultMatchDuplicate(
                                    ajaxResult: AjaxResult[WrapperXMLHttpRequest],
                                    future: Future[MatchDuplicate]
                                  )(
                                    implicit
                                      pos: Position,
                                      executor: ExecutionContext
                                  ) extends CreateResult[MatchDuplicate](ajaxResult,future) {

    def this( result: RestResult[MatchDuplicate])(implicit executor: ExecutionContext) = {
      this(result.ajaxResult, result.future )
    }

    def updateStore( mc: MatchDuplicate ): MatchDuplicate = {
      monitor(mc.id)
      BridgeDispatcher.updateDuplicateMatch(mc)
      logger.info(s"Created new chicago game: ${mc.id}")
      mc
    }

  }

  import scala.concurrent.ExecutionContext.Implicits.global

  /**
   * Create a duplicate match and start monitoring it
   * @param url the base URL of the server, no trailing slash.
   */
  def createMatchDuplicate( default: Boolean = true,
                            boards: Option[String] = None,
                            movement: Option[String] = None,
                            test: Boolean = false ): Result[MatchDuplicate] = {
    val result = RestClientDuplicate.createMatchDuplicate(MatchDuplicate.create(), default=default, boards=boards, movement=movement, test=test).recordFailure()
    new CreateResultMatchDuplicate(result)
  }

  def getMatchDuplicate( id: Id.MatchDuplicate ): Result[MatchDuplicate] = {
    RestClientDuplicate.get(id).recordFailure()
  }

  def deleteMatchDuplicate( id: Id.MatchDuplicate ): Result[Unit] = {
    logger.info("Deleting MatchDuplicate with id "+id)
    stop()
    val result = RestClientDuplicate.delete(id).recordFailure()
    result.foreach( msg => {
      logger.info("Controller: Deleted MatchDuplicate with id "+id)
    })
    result
  }

  object Listener extends SECListener[Id.MatchDuplicate] {
    def handleStart( dupid: Id.MatchDuplicate) = {
      BridgeDispatcher.startDuplicateMatch(dupid)
    }
    def handleStop( dupid: Id.MatchDuplicate) = {
      BridgeDispatcher.stop()
    }

    def processMessage( msg: Protocol.ToBrowserMessage ) = {
      msg match {
        case Protocol.MonitorJoined(id,members) =>
        case Protocol.MonitorLeft(id,members) =>
        case Protocol.UpdateDuplicate(matchDuplicate) =>
          BridgeDispatcher.updateDuplicateMatch(matchDuplicate)
        case Protocol.UpdateDuplicateHand(dupid, hand) =>
          BridgeDispatcher.updateDuplicateHand(dupid,hand)
        case Protocol.UpdateDuplicateTeam(dupid,team) =>
          BridgeDispatcher.updateTeam(dupid, team)
        case Protocol.NoData(_) =>
        case Protocol.UpdateChicago(_) =>
        case Protocol.UpdateRubber(_) =>
      }
    }
  }

  def send( msg: ToServerMessage ): Unit = {
    sseConnection.getDuplexPipeServerEventConnection().map( c => c.send(msg))
  }

  def updateMatch( dup: MatchDuplicate ) = {
    BridgeDispatcher.updateDuplicateMatch(dup)
    if (!Bridge.isDemo) {
      if (useRestToServer) {
        val resource = RestClientDuplicate.update(dup.id, dup).recordFailure().onComplete { t =>
          if (t.isFailure) {
            Alerter.alert("Failure updating hand on server")
          }
        }
      } else {
        val msg = Protocol.UpdateDuplicate(dup)
        send(msg)
      }
    }
    logger.info("Update hand ("+dup.id+","+dup+")")
  }

  def updateHand( dup: MatchDuplicate, hand: DuplicateHand ) = {
    BridgeDispatcher.updateDuplicateHand(dup.id, hand)
    if (!Bridge.isDemo) {
      if (useRestToServer) {
        val resource = RestClientDuplicate.boardResource(dup.id).handResource(hand.board)
        resource.update(hand.id, hand).recordFailure().onComplete { t =>
          if (t.isFailure) {
            Alerter.alert(s"Failure updating hand on server\n${hand}")
          }
        }
      } else {
        val msg = Protocol.UpdateDuplicateHand(dup.id, hand)
        send(msg)
      }
    }
    logger.info("Update hand ("+dup.id+","+hand.board+","+hand.id+")")
  }

  def updateHandOld( dup: MatchDuplicate, hand: DuplicateHand ) = {
    BridgeDispatcher.updateDuplicateHand(dup.id, hand)
    RestClientDuplicate.boardResource(dup.id).handResource(hand.board).update(hand.id, hand).recordFailure().foreach( h => {
      logger.info("Update hand ("+dup.id+","+hand.board+","+hand.id+")")
    })
  }

  def updateTeam( dup: MatchDuplicate, team: Team ) = {
    BridgeDispatcher.updateTeam(dup.id, team)
    if (!Bridge.isDemo) {
      if (useRestToServer) {
        val resource = RestClientDuplicate.teamResource(dup.id)
        resource.update(team.id, team).recordFailure().onComplete { t =>
          if (t.isFailure) {
            Alerter.alert("Failure updating team on server")
          }
        }
      } else {
        val msg = Protocol.UpdateDuplicateTeam(dup.id, team)
        send(msg)
      }
    }
    logger.info("Update team ("+dup.id+","+team+")")
  }

  def updateTeamOld( dup: MatchDuplicate, team: Team ) = {
    BridgeDispatcher.updateTeam(dup.id, team)
    RestClientDuplicate.teamResource(dup.id).update(team.id, team).recordFailure().foreach( t => {
      logger.info("Update team ("+dup.id+","+team.id+")")
    })
  }

  def monitor( dupid: Id.MatchDuplicate, restart: Boolean = false ): Unit = {

    if (AjaxResult.isEnabled.getOrElse(false)) {
      DuplicateStore.getId() match {
        case Some(mdid) =>
          sseConnection.cancelStop()
          if (restart || mdid != dupid || !sseConnection.isConnected) {
            logger.info(s"""Switching MatchDuplicate monitor to ${dupid} from ${mdid}""" )
            sseConnection.monitor(dupid, restart)
          } else {
            // already monitoring id
            logger.info(s"""Already monitoring MatchDuplicate ${dupid}""" )
          }
        case None =>
          logger.info(s"""Starting MatchDuplicate monitor to ${dupid}""" )
          sseConnection.monitor(dupid, restart)
      }
    } else {
      BridgeDispatcher.startDuplicateMatch(dupid)
    }

  }

  /**
   * Stop monitoring a duplicate match
   */
  def delayStop() = {
    logger.fine(s"Controller.delayStop ${DuplicateStore.getId()}")
    sseConnection.delayStop()
  }

  /**
   * Stop monitoring a duplicate match
   */
  def stop() = {
    logger.fine(s"Controller.stop ${DuplicateStore.getId()}")
    sseConnection.stop()
  }

  private def getDemoSummary( error: ()=>Unit ): Unit = {
    val x = AjaxCall.send(
      method = "GET",
      url = "demo/demoMatchDuplicates.json",
      data = null,
      timeout = Duration("30s"),
      headers = Map[String, String](),
      withCredentials = false,
      responseType = "text/plain"
    )
    x.onComplete{ t =>
      t match {
        case Success(wxhr) =>
          val r = wxhr.responseText
          import JsonSupport._
          val bm = JsonSupport.readJson[List[MatchDuplicate]](r)
          BridgeDispatcher.updateDuplicateSummaryDemoMatch(None,bm)
        case Failure(err) =>
          error()
      }
    }
  }

  def getSummary( error: ()=>Unit /* = ()=>{} */ ): Unit = {
    logger.finer("Sending duplicatesummaries list request to server")
    import scala.scalajs.js.timers._
    setTimeout(1) { // note the absence of () =>
      if (Bridge.isDemo) {
        if (DuplicateSummaryStore.getDuplicateSummary().isEmpty) {
          getDemoSummary(error)
        }
      } else {
        RestClientDuplicateSummary.list().recordFailure().onComplete { trylist =>
          trylist match {
            case Success(list) =>
              Alerter.tryitWithUnit {
                logger.finer(s"DuplicateSummary got ${list.size} entries")
                BridgeDispatcher.updateDuplicateSummary(None,list.toList)
              }
            case Failure(err) =>
              error()
          }
        }
      }
    }
  }

  def getImportSummary( importId: String, error: ()=>Unit ): Unit = {
    logger.finer(s"Sending import duplicatesummaries ${importId} list request to server")
    import scala.scalajs.js.timers._
    setTimeout(1) { // note the absence of () =>
      GraphQLClient.request(
          """query importDuplicates( $importId: ImportId! ) {
             |  import( id: $importId ) {
             |    duplicatesummaries {
             |      id
             |      finished
             |      teams {
             |        id
             |        team {
             |          id
             |          player1
             |          player2
             |          created
             |          updated
             |        }
             |        result
             |        place
             |      }
             |      boards
             |      tables
             |      onlyresult
             |      created
             |      updated
             |      bestMatch {
             |        id
             |        sameness
             |        differences
             |      }
             |    }
             |  }
             |}
             |""".stripMargin,
          Some(JsObject( Seq( "importId" -> JsString(importId) ) )),
          Some("importDuplicates") ).map { r =>
            r.data match {
              case Some(data) =>
                data \ "import" \ "duplicatesummaries" match {
                  case JsDefined( jds ) =>
                    import JsonSupport._
                    Json.fromJson[List[DuplicateSummary]](jds) match {
                      case JsSuccess(ds,path) =>
                        logger.finer(s"Import(${importId})/DuplicateSummary got ${ds.size} entries")
                        BridgeDispatcher.updateDuplicateSummary(Some(importId),ds)
                      case JsError(err) =>
                        logger.warning(s"Import(${importId})/DuplicateSummary, JSON error: ${JsError.toJson(err)}")
                        error()
                    }
                  case _: JsUndefined =>
                    logger.warning(s"error import duplicatesummaries ${importId}, did not find import/duplicatesummaries field")
                    error()
                }
              case None =>
                logger.warning(s"error import duplicatesummaries ${importId}, ${r.getError()}")
                error()
            }
          }.recover {
            case x: Exception =>
                logger.warning(s"exception import duplicatesummaries ${importId}", x)
                error()
          }.foreach { x => }
    }
  }

  def getDuplicateResult( id: Id.MatchDuplicateResult ) = {
    RestClientDuplicateResult.get(id).recordFailure()
  }

  def monitorDuplicateResult( id: Id.MatchDuplicateResult ): Unit = {
    DuplicateResultStore.monitor( Some(id) )
    RestClientDuplicateResult.get(id).recordFailure().foreach { dr => Alerter.tryitWithUnit {
      BridgeDispatcher.updateDuplicateResult(dr)
    }}
  }

  def stopMonitoringDuplicateResult() = {
    DuplicateResultStore.monitor( None )
  }
}
