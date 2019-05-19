package com.example.controller

import utils.logging.Logger
import japgolly.scalajs.react._
import com.example.data.MatchRubber
import com.example.rest2.RestClientRubber
import com.example.bridge.store.RubberStore
import com.example.bridge.action.BridgeDispatcher
import com.example.data.Round
import com.example.data.Hand
import com.example.data.bridge.PlayerPosition
import com.example.data.RubberHand
import com.example.rest2.AjaxResult
import com.example.rest2.Result
import scala.concurrent.ExecutionContext
import com.example.rest2.ResultObject
import com.example.rest2.RestResult
import org.scalactic.source.Position
import scala.concurrent.Future
import com.example.rest2.WrapperXMLHttpRequest
import com.example.logger.Alerter
import com.example.graphql.GraphQLClient
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsDefined
import com.example.data.rest.JsonSupport
import play.api.libs.json.Json
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import play.api.libs.json.JsUndefined
import com.example.Bridge
import com.example.bridge.store.RubberListStore
import scala.util.Success
import scala.util.Failure
import com.example.data.websocket.Protocol

object RubberController {
  val logger = Logger("bridge.RubberController")

  class CreateResultMatchRubber(
                                ajaxResult: AjaxResult[WrapperXMLHttpRequest],
                                future: Future[MatchRubber]
                               )(
                                 implicit
                                   pos: Position,
                                   executor: ExecutionContext
                               ) extends CreateResult[MatchRubber](ajaxResult,future) {

    def this( result: RestResult[MatchRubber])(implicit executor: ExecutionContext) = {
      this(result.ajaxResult, result.future )
    }

    def updateStore( mc: MatchRubber ): MatchRubber = {
      showMatch(mc)
      logger.info(s"Created new rubber game: ${mc.id}")
      mc
    }

  }

  import scala.concurrent.ExecutionContext.Implicits.global

  private var currentId = 0

  def createMatch() = {
    AjaxResult.isEnabled.orElse(Some(true)).map( e => e && !Bridge.isDemo ) match {
      case Some(true) | None =>
        // enabled - Some(true)
        // mocked - None
        logger.info("Sending create rubber to server")
        val rub = MatchRubber("","","","","","",Nil)
        val r = RestClientRubber.create(rub).recordFailure()
        new CreateResultMatchRubber(r)
      case Some(false) =>
        // disabled
        currentId = currentId + 1
        val created = MatchRubber(s"R$currentId","","","","","",Nil)
        logger.info("PageRubber: created new local rubber game: "+created.id)
        showMatch( created )
        new ResultObject(created)
    }
  }

  def showMatch( rub: MatchRubber ) = {
    RubberStore.start(rub.id, rub)
    logger.fine("calling callback with "+rub.id)
    BridgeDispatcher.updateRubber(rub)
  }

  def ensureMatch( rubid: String ) = {
    if (!RubberStore.isMonitoredId(rubid)) {
      val result = RestClientRubber.get(rubid).recordFailure()
      result.foreach( created => {
        logger.info("PageRubber: got rubber game: "+created.id)
        showMatch( created )
      })
      result
    } else {
      new ResultObject( RubberStore.getRubber.get )
    }
  }

  def updateMatch( rub: MatchRubber ) = {
    logger.info("dispatrubng an update to MatchRubber "+rub.id )
    BridgeDispatcher.updateRubber(rub, Some(updateServer))
  }

  def updateServer( rub: MatchRubber ) = {
    if (!Bridge.isDemo) {
      RestClientRubber.update(rub.id, rub).recordFailure().foreach( updated => {
        logger.fine("PageRubber: Updated rubber game: "+rub.id)
        // the BridgeDispatcher.updateRubber causes a timing problem.
        // if two updates are done one right after the other, then the second
        // update will be lost.
  //      BridgeDispatcher.updateRubber(updated)
      })
    }
  }

  def updateRubberNames( rubid: String, north: String, south: String, east: String, west: String, firstDealer: PlayerPosition ) = {
    BridgeDispatcher.updateRubberNames(rubid, north, south, east, west, firstDealer, Some(updateServer))
  }

  def updateRubberHand( rubid: String, handid: String, hand: RubberHand ) = {
    BridgeDispatcher.updateRubberHand(rubid, handid, hand, Some( updateServer ))
  }

  def getSummary(error: ()=>Unit): Unit = {
    logger.finer("Sending rubbers list request to server")
    import scala.scalajs.js.timers._
    setTimeout(1) { // note the absence of () =>
      if (Bridge.isDemo) {
        val x = RubberListStore.getRubberSummary().getOrElse(Array())
        BridgeDispatcher.updateRubberList(None,x)
      } else {
        RestClientRubber.list().recordFailure().onComplete { trylist =>
          Alerter.tryitWithUnit {
            trylist match {
              case Success(list) =>
                logger.finer(s"RubberList got ${list.size} entries")
                BridgeDispatcher.updateRubberList(None,list)
              case Failure(err) =>
                error()
            }
          }
        }
      }
    }
  }

  def getImportSummary( importId: String, error: ()=>Unit ): Unit = {
    logger.finer(s"Sending import rubbersummaries ${importId} list request to server")
    import scala.scalajs.js.timers._
    setTimeout(1) { // note the absence of () =>
      GraphQLClient.request(
          """query importRubbers( $importId: ImportId! ) {
             |  import( id: $importId ) {
             |    rubbers {
             |      id
             |      north
             |      south
             |      east
             |      west
             |      dealerFirstHand
             |      hands {
             |        id
             |        hand {
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
             |        honors
             |        honorsPlayer
             |        created
             |        updated
             |      }
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
          Some("importRubbers") ).map { r =>
            r.data match {
              case Some(data) =>
                data \ "import" \ "rubbers" match {
                  case JsDefined( jds ) =>
                    import JsonSupport._
                    Json.fromJson[List[MatchRubber]](jds) match {
                      case JsSuccess(ds,path) =>
                        logger.finer(s"Import(${importId})/rubbers got ${ds.size} entries")
                        BridgeDispatcher.updateRubberList(Some(importId),ds.toArray)
                      case JsError(err) =>
                        logger.warning(s"Import(${importId})/chicagos, JSON error: ${JsError.toJson(err)}")
                        error()
                    }
                  case _: JsUndefined =>
                    logger.warning(s"error import rubber list ${importId}, did not find import/rubbers field")
                    error()
                }
              case None =>
                logger.warning(s"error import rubber list ${importId}, ${r.getError()}")
                error()
            }
          }.recover {
            case x: Exception =>
                logger.warning(s"exception import rubber list ${importId}", x)
                error()
          }.foreach { x => }
    }
  }

  def deleteRubber( id: String) = {
    BridgeDispatcher.deleteRubber(id)
    if (!Bridge.isDemo) RestClientRubber.delete(id).recordFailure()
  }

  private var sseConnection: ServerEventConnection[String] = null

  private var useSSEFromServer: Boolean = true;

  def setUseSSEFromServer( b: Boolean ) = {
    if (b != useSSEFromServer) {
      useSSEFromServer = b
      setServerEventConnection()
    }
  }

  setServerEventConnection()

  private def setServerEventConnection(): Unit = {
    sseConnection = new SSE[String]( "/v1/sse/rubbers/", Listener)
  }

  object Listener extends SECListener[String] {
    def handleStart( dupid: String) = {
    }
    def handleStop( dupid: String) = {
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
        case Protocol.UpdateChicagoRound(mc,r) =>
        case Protocol.UpdateChicagoHand(mc,r,hand) =>
        case Protocol.UpdateRubber(mr) =>
          BridgeDispatcher.updateRubber(mr)
        case Protocol.UpdateRubberHand(mrid,hand) =>
          BridgeDispatcher.updateRubberHand(mrid,hand.id,hand)
      }
    }
  }

  def monitor( dupid: String, restart: Boolean = false ): Unit = {

    if (AjaxResult.isEnabled.getOrElse(false)) {
      RubberStore.getMonitoredId match {
        case Some(mdid) =>
          sseConnection.cancelStop()
          if (restart || mdid != dupid || !sseConnection.isConnected) {
            logger.info(s"""Switching MatchChicago monitor to ${dupid} from ${mdid}""" )
            sseConnection.monitor(dupid, restart)
          } else {
            // already monitoring id
            logger.info(s"""Already monitoring MatchChicago ${dupid}""" )
          }
        case None =>
          logger.info(s"""Starting MatchChicago monitor to ${dupid}""" )
          sseConnection.monitor(dupid, restart)
      }
    } else {

    }

  }

  /**
   * Stop monitoring a duplicate match
   */
  def delayStop() = {
    logger.fine(s"Controller.delayStop ${RubberStore.getMonitoredId}")
    sseConnection.delayStop()
  }

  /**
   * Stop monitoring a duplicate match
   */
  def stop() = {
    logger.fine(s"Controller.stop ${RubberStore.getMonitoredId}")
    sseConnection.stop()
  }

}
