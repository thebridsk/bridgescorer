package com.github.thebridsk.bridge.controller

import com.github.thebridsk.utilities.logging.Logger
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.rest2.RestClientChicago
import com.github.thebridsk.bridge.bridge.store.ChicagoStore
import com.github.thebridsk.bridge.bridge.action.BridgeDispatcher
import com.github.thebridsk.bridge.data.Round
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.rest2.RestResult
import com.github.thebridsk.bridge.rest2.Result
import com.github.thebridsk.bridge.rest2.ResultRecorder
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.concurrent.CanAwait
import scala.util.Try
import scala.concurrent.Future
import com.github.thebridsk.bridge.rest2.ResultObject
import com.github.thebridsk.bridge.rest2.AjaxResult
import org.scalactic.source.Position
import com.github.thebridsk.bridge.rest2.WrapperXMLHttpRequest
import com.github.thebridsk.bridge.graphql.GraphQLClient
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsDefined
import play.api.libs.json.Json
import com.github.thebridsk.bridge.data.rest.JsonSupport
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import play.api.libs.json.JsUndefined
import com.github.thebridsk.bridge.logger.Alerter
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.Bridge
import com.github.thebridsk.bridge.bridge.store.ChicagoSummaryStore
import scala.util.Success
import scala.util.Failure
import com.github.thebridsk.bridge.data.websocket.Protocol

object ChicagoController {
  val logger = Logger("bridge.ChicagoController")

  class CreateResultMatchChicago(
                                  ajaxResult: AjaxResult[WrapperXMLHttpRequest],
                                  future: Future[MatchChicago]
                                )(
                                  implicit
                                    pos: Position,
                                    executor: ExecutionContext
                                ) extends CreateResult[MatchChicago](ajaxResult,future) {

    def this( result: RestResult[MatchChicago])(implicit executor: ExecutionContext) = {
      this(result.ajaxResult, result.future )
    }

    def updateStore( mc: MatchChicago ): MatchChicago = {
      showMatch(mc)
      logger.info(s"Created new chicago game: ${mc.id}")
      mc
    }

  }

  import scala.concurrent.ExecutionContext.Implicits.global

  private var currentId = 0

  def createMatch() = {
    logger.info("Sending create chicago to server")
    if (Bridge.isDemo) {
      currentId = currentId + 1
      val chi = MatchChicago(s"C$currentId",List("","","",""),Nil,0,false)
      new CreateResultMatchChicago(null, Future(chi))
    } else {
      val chi = MatchChicago("",List("","","",""),Nil,0,false)
      val result = RestClientChicago.create(chi).recordFailure()
      new CreateResultMatchChicago(result)
    }
  }

  def showMatch( chi: MatchChicago ) = {
    ChicagoStore.start(chi.id, Some(chi))
    logger.fine("calling callback with "+chi.id)
    BridgeDispatcher.updateChicago(chi)
  }

  def ensureMatch( chiid: String ) = {
    if (!ChicagoStore.isMonitoredId(chiid)) {
      ChicagoStore.start(chiid,None)
      val result = RestClientChicago.get(chiid).recordFailure()
      result.foreach( created=>{
        logger.info(s"PageChicago: got chicago game: ${created.id}")
        showMatch( created )
      })
      result
    } else {
      new ResultObject(ChicagoStore.getChicago.get)
    }
  }

  def updateMatch( chi: MatchChicago ) = {
    logger.info("dispatching an update to MatchChicago "+chi.id )
    BridgeDispatcher.updateChicago(chi, Some(updateServer))
  }

  def updateServer( chi: MatchChicago ) = {
    if (!Bridge.isDemo) {
      RestClientChicago.update(chi.id, chi).recordFailure().foreach( updated => {
        logger.fine(s"PageChicago: Updated chicago game: ${chi.id}")
        // the BridgeDispatcher.updateChicago causes a timing problem.
        // if two updates are done one right after the other, then the second
        // update will be lost.
  //      BridgeDispatcher.updateChicago(updated)
      })
    }
  }

  def updateChicagoNames( chiid: String, nplayer1: String, nplayer2: String, nplayer3: String, nplayer4: String, extra: Option[String], quintet: Boolean, simpleRotation: Boolean ) = {
    BridgeDispatcher.updateChicagoNames(chiid, nplayer1, nplayer2, nplayer3, nplayer4, extra, quintet, simpleRotation, Some(updateServer))
  }

  def updateChicago5( chiid: String, extraPlayer: String ) = {
    BridgeDispatcher.updateChicago5(chiid, extraPlayer, Some(updateServer))
  }

  def updateChicagoRound( chiid: String, round: Round ) = {
    BridgeDispatcher.updateChicagoRound(chiid, round, Some( updateServer ))
  }

  def updateChicagoHand( chiid: String, roundid: Int, handid: Int, hand: Hand ) = {
    BridgeDispatcher.updateChicagoHand(chiid, roundid, handid, hand, Some( updateServer ))
  }

  def getSummary(error: ()=>Unit): Unit = {
    logger.finer("Sending duplicatesummaries list request to server")
    import scala.scalajs.js.timers._
    setTimeout(1) { // note the absence of () =>
      if (Bridge.isDemo) {
        val x = ChicagoSummaryStore.getChicagoSummary().getOrElse(Array())
        BridgeDispatcher.updateChicagoSummary(None, x)
      } else {
        RestClientChicago.list().recordFailure().onComplete { trylist =>
          Alerter.tryitWithUnit {
            trylist match {
              case Success(list) =>
                logger.finer(s"ChicagoSummary got ${list.size} entries")
                BridgeDispatcher.updateChicagoSummary(None,list)
              case Failure(err) =>
                error()
            }
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
             |    chicagos {
             |      id
             |      players
             |      rounds {
             |        id
             |        north
             |        south
             |        east
             |        west
             |        dealerFirstRound
             |        hands {
             |          id
             |          contractTricks
             |          contractSuit
             |          contractDoubled
             |          declarer
             |          nsVul
             |          ewVul
             |          madeContract
             |          tricks
             |          created
             |          updated
             |        }
             |        created
             |        updated
             |      }
             |      gamesPerRound
             |      simpleRotation
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
                data \ "import" \ "chicagos" match {
                  case JsDefined( jds ) =>
                    import JsonSupport._
                    Json.fromJson[List[MatchChicago]](jds) match {
                      case JsSuccess(ds,path) =>
                        logger.finer(s"Import(${importId})/chicagos got ${ds.size} entries")
                        BridgeDispatcher.updateChicagoSummary(Some(importId),ds.toArray)
                      case JsError(err) =>
                        logger.warning(s"Import(${importId})/chicagos, JSON error: ${JsError.toJson(err)}")
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

  def deleteChicago( id: Id.MatchChicago) = {
    BridgeDispatcher.deleteChicago(id)
    if (!Bridge.isDemo) RestClientChicago.delete(id).recordFailure()
  }


  private var sseConnection: ServerEventConnection[Id.MatchChicago] = null

  private var useSSEFromServer: Boolean = true;

  def setUseSSEFromServer( b: Boolean ) = {
    if (b != useSSEFromServer) {
      useSSEFromServer = b
      setServerEventConnection()
    }
  }

  setServerEventConnection()

  private def setServerEventConnection(): Unit = {
    sseConnection = new SSE[Id.MatchChicago]( "/v1/sse/chicagos/", Listener)
  }

  object Listener extends SECListener[Id.MatchChicago] {
    def handleStart( dupid: Id.MatchChicago) = {
    }
    def handleStop( dupid: Id.MatchChicago) = {
    }

    def processMessage( msg: Protocol.ToBrowserMessage ) = {
      msg match {
        case Protocol.MonitorJoined(id,members) =>
        case Protocol.MonitorLeft(id,members) =>
        case Protocol.UpdateDuplicate(matchDuplicate) =>
        case Protocol.UpdateDuplicateHand(dupid, hand) =>
        case Protocol.UpdateDuplicateTeam(dupid,team) =>
        case Protocol.NoData(_) =>
        case Protocol.UpdateChicago(mc) =>
          BridgeDispatcher.updateChicago(mc)
        case Protocol.UpdateChicagoRound(mc,r) =>
          BridgeDispatcher.updateChicagoRound(mc, r)
        case Protocol.UpdateChicagoHand(mc,r,hand) =>
          BridgeDispatcher.updateChicagoHand(mc,r.toInt,hand.id.toInt,hand)
        case Protocol.UpdateRubber(_) =>
        case _: Protocol.UpdateRubberHand =>
      }
    }
  }

  def monitor( dupid: Id.MatchChicago, restart: Boolean = false ): Unit = {

    if (AjaxResult.isEnabled.getOrElse(false)) {
      ChicagoStore.getMonitoredId match {
        case Some(mdid) =>
          sseConnection.cancelStop()
          if (restart || mdid != dupid || !sseConnection.isConnected) {
            logger.info(s"""Switching MatchChicago monitor to ${dupid} from ${mdid}""" )
            ChicagoStore.start(dupid,None)
            sseConnection.monitor(dupid, restart)
          } else {
            // already monitoring id
            logger.info(s"""Already monitoring MatchChicago ${dupid}""" )
          }
        case None =>
          logger.info(s"""Starting MatchChicago monitor to ${dupid}""" )
          ChicagoStore.start(dupid,None)
          sseConnection.monitor(dupid, restart)
      }
    } else {

    }

  }

  /**
   * Stop monitoring a duplicate match
   */
  def delayStop() = {
    logger.fine(s"Controller.delayStop ${ChicagoStore.getMonitoredId}")
    sseConnection.delayStop()
  }

  /**
   * Stop monitoring a duplicate match
   */
  def stop() = {
    logger.fine(s"Controller.stop ${ChicagoStore.getMonitoredId}")
    sseConnection.stop()
  }

}
