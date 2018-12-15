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

object Controller {
  val logger = Logger("bridge.Controller")

  class AlreadyStarted extends Exception

  private var duplexPipe: Option[DuplexPipe] = None

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
      monitorMatchDuplicate(mc.id)
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

  def getDuplexPipe() = duplexPipe match {
    case Some(d) => d
    case None =>
      val url = AppRouter.hostUrl.replaceFirst("http", "ws") + "/v1/ws/"
      val d = new DuplexPipe( url, Protocol.DuplicateBridge ) {
        override
        def onNormalClose() = {
          start(true)
        }
      }
      d.addListener(new DuplexPipe.Listener {
        def onMessage( msg: Protocol.ToBrowserMessage ) = {
          processMessage(msg)
        }
      })
      duplexPipe = Some(d)
      d
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
    }
  }

  val useRest: Boolean = true;

  def updateHand( dup: MatchDuplicate, hand: DuplicateHand ) = {
    BridgeDispatcher.updateDuplicateHand(dup.id, hand)
    if (useRest) {
      val resource = RestClientDuplicate.boardResource(dup.id).handResource(hand.board)
      resource.update(hand.id, hand).recordFailure().onComplete { t =>
        if (t.isFailure) {
          Alerter.alert("Failure updating hand on server")
        }
      }
    } else {
      val msg = Protocol.UpdateDuplicateHand(dup.id, hand)
      getDuplexPipe().send(msg)
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
    if (useRest) {
      val resource = RestClientDuplicate.teamResource(dup.id)
      resource.update(team.id, team).recordFailure().onComplete { t =>
        if (t.isFailure) {
          Alerter.alert("Failure updating team on server")
        }
      }
    } else {
      val msg = Protocol.UpdateDuplicateTeam(dup.id, team)
      getDuplexPipe().send(msg)
    }
    logger.info("Update team ("+dup.id+","+team+")")
  }

  def updateTeamOld( dup: MatchDuplicate, team: Team ) = {
    BridgeDispatcher.updateTeam(dup.id, team)
    RestClientDuplicate.teamResource(dup.id).update(team.id, team).recordFailure().foreach( t => {
      logger.info("Update team ("+dup.id+","+team.id+")")
    })
  }

  def esOnMessage( me: MessageEvent ): Unit = {
    import com.example.data.websocket.DuplexProtocol
    try {
      logger.info(s"esOnMessage received ${me.data}")
      me.data match {
        case s: String =>
          if (s.equals("")) {
            // ignore this, this is a heartbeat
          } else {
            DuplexProtocol.fromString(s) match {
              case DuplexProtocol.Response(data,seq) =>
                processMessage(data)
              case DuplexProtocol.Unsolicited(data) =>
                processMessage(data)
              case x =>
                logger.severe(s"EventSource received unknown object: ${me.data}")
            }
          }
      }
    } catch {
      case x: Exception =>
        logger.severe(s"esOnMessage exception: $x", x)
    }
  }

  def esOnError( err: Event ): Unit = {
    logger.info("EventSource error")
  }

  def getEventSource( dupid: Id.MatchDuplicate ): Option[EventSource] = {
    val es = new EventSource(s"/v1/sse/duplicates/${dupid}")
    es.onmessage = esOnMessage
    es.onerror = esOnError
    Some(es)
  }

  var eventSource: Option[EventSource] = None

  def monitorMatchDuplicate( dupid: Id.MatchDuplicate ): Unit = {

    DuplicateStore.getId() match {
      case Some(mdid) =>
        if (mdid != dupid) {
          logger.info(s"""Switching MatchDuplicate monitor to ${dupid} from ${mdid}""" )
          if (useRest) {
            BridgeDispatcher.startDuplicateMatch(dupid)
            eventSource.foreach( es => es.close())
            eventSource = getEventSource(dupid)
          } else {
            getDuplexPipe().clearSession(Protocol.StopMonitor(mdid))
            BridgeDispatcher.startDuplicateMatch(dupid)
            getDuplexPipe().setSession { dp =>
              logger.info(s"""In Session: Switching MatchDuplicate monitor to ${dupid} from ${mdid}""" )
              dp.send(Protocol.StartMonitor(dupid))
            }
          }
        } else {
          // already monitoring id
          logger.info(s"""Already monitoring MatchDuplicate ${dupid}""" )
        }
      case None =>
        logger.info(s"""Starting MatchDuplicate monitor to ${dupid}""" )
        if (useRest) {
          BridgeDispatcher.startDuplicateMatch(dupid)
          eventSource.foreach( es => es.close())
          eventSource = getEventSource(dupid)
        } else {
          BridgeDispatcher.startDuplicateMatch(dupid)
          getDuplexPipe().setSession { dp =>
            logger.info(s"""In Session: Starting MatchDuplicate monitor to ${dupid}""" )
            dp.send(Protocol.StartMonitor(dupid))
          }
        }
    }
  }

  /**
   * Stop monitoring a duplicate match
   */
  def stop() = {
    if (useRest) {
      eventSource.foreach( es => es.close())
      eventSource = None
    } else {
      duplexPipe match {
        case Some(d) =>
          DuplicateStore.getId() match {
            case Some(id) =>
              d.clearSession(Protocol.StopMonitor(id))
            case None =>
          }
          BridgeDispatcher.stop()
        case _ =>

      }
    }
  }

  def getSummary(): Unit = {
    logger.finer("Sending duplicatesummaries list request to server")
    import scala.scalajs.js.timers._
    setTimeout(1) { // note the absence of () =>
      RestClientDuplicateSummary.list().recordFailure().foreach { list => Alerter.tryitWithUnit {
        logger.finer(s"DuplicateSummary got ${list.size} entries")
        BridgeDispatcher.updateDuplicateSummary(None,list.toList)
      }}
    }
  }

  def getImportSummary( importId: String ): Unit = {
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
                    }
                  case _: JsUndefined =>
                    logger.warning(s"error import duplicatesummaries ${importId}, did not find import/duplicatesummaries field")
                }
              case None =>
                logger.warning(s"error import duplicatesummaries ${importId}, ${r.getError()}")
            }
          }.recover {
            case x: Exception =>
                logger.warning(s"exception import duplicatesummaries ${importId}", x)
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
