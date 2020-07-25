package com.github.thebridsk.bridge.client.controller

import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientChicago
import com.github.thebridsk.bridge.client.bridge.store.ChicagoStore
import com.github.thebridsk.bridge.client.bridge.action.BridgeDispatcher
import com.github.thebridsk.bridge.data.Round
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.clientcommon.rest2.RestResult
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.github.thebridsk.bridge.clientcommon.rest2.ResultObject
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxResult
import org.scalactic.source.Position
import com.github.thebridsk.bridge.clientcommon.rest2.WrapperXMLHttpRequest
import com.github.thebridsk.bridge.clientcommon.graphql.GraphQLClient
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsDefined
import play.api.libs.json.Json
import com.github.thebridsk.bridge.data.rest.JsonSupport._
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import play.api.libs.json.JsUndefined
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import com.github.thebridsk.bridge.client.bridge.store.ChicagoSummaryStore
import scala.util.Success
import scala.util.Failure
import com.github.thebridsk.bridge.data.websocket.Protocol
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo
import com.github.thebridsk.bridge.clientcommon.pages.LocalStorage
import scala.scalajs.js
import org.scalajs.dom.raw.StorageEvent

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
    if (BridgeDemo.isDemo) {
      currentId = currentId + 1
      val chi = MatchChicago( MatchChicago.id(currentId),List("","","",""),Nil,0,false)
      val data = writeJson(chi)
      logger.fine(s"saving as lastChicago, id=${chi.id}: ${chi}")
      LocalStorage.setItem(lastChicagoStorageKey,data)
      new CreateResultMatchChicago(null, Future(chi))
    } else {
      val chi = MatchChicago(MatchChicago.idNul,List("","","",""),Nil,0,false)
      val result = RestClientChicago.create(chi).recordFailure()
      new CreateResultMatchChicago(result)
    }
  }

  def showMatch( chi: MatchChicago ) = {
    ChicagoStore.start(chi.id, Some(chi))
    logger.fine("calling callback with "+chi.id)
    BridgeDispatcher.updateChicago(chi)
  }

  def ensureMatch( chiid: MatchChicago.Id ) = {
    monitor(chiid)
  }

  def ensureMatchOld( chiid: MatchChicago.Id ) = {
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

  private val lastChicagoStorageKey = "thebridsk:bridge:lastChicago"

  private val localStorageListender: js.Function1[StorageEvent, js.Any] = { se =>
    Option(se.key) match {
      case Some(key) if key == lastChicagoStorageKey =>
        logger.fine("Updating MatchChicago from store")
        Option(se.newValue).map { smc =>
          val mc = readJson[MatchChicago](smc)
          BridgeDispatcher.updateChicago(mc)
        }.getOrElse {
          Option(se.oldValue).map { smc =>
            val mc = readJson[MatchChicago](smc)
            BridgeDispatcher.deleteChicago(mc.id)
          }
        }
      case Some(key) =>
        // ignore other keys
      case None =>
        // this gets invoked when the storage is cleared using the clear() method.
        ChicagoStore.getMonitoredId.foreach { id =>
          BridgeDispatcher.deleteChicago(id)
        }
    }
    0
  }

  def updateServer( chi: MatchChicago ) = {
    if (!BridgeDemo.isDemo) {
      RestClientChicago.update(chi.id, chi).recordFailure().foreach( updated => {
        logger.fine(s"PageChicago: Updated chicago game: ${chi.id}")
        // the BridgeDispatcher.updateChicago causes a timing problem.
        // if two updates are done one right after the other, then the second
        // update will be lost.
  //      BridgeDispatcher.updateChicago(updated)
      })
    } else {
      // demo
      val data = writeJson(chi)
      logger.fine(s"saving as lastChicago, id=${chi.id}: ${chi}")
      LocalStorage.setItem(lastChicagoStorageKey,data)
    }
  }

  def updateChicagoNames( chiid: MatchChicago.Id, nplayer1: String, nplayer2: String, nplayer3: String, nplayer4: String, extra: Option[String], quintet: Boolean, simpleRotation: Boolean ) = {
    BridgeDispatcher.updateChicagoNames(chiid, nplayer1, nplayer2, nplayer3, nplayer4, extra, quintet, simpleRotation, Some(updateServer))
  }

  def updateChicago5( chiid: MatchChicago.Id, extraPlayer: String ) = {
    BridgeDispatcher.updateChicago5(chiid, extraPlayer, Some(updateServer))
  }

  def updateChicagoRound( chiid: MatchChicago.Id, round: Round ) = {
    BridgeDispatcher.updateChicagoRound(chiid, round, Some( updateServer ))
  }

  def updateChicagoHand( chiid: MatchChicago.Id, roundid: Int, handid: Int, hand: Hand ) = {
    BridgeDispatcher.updateChicagoHand(chiid, roundid, handid, hand, Some( updateServer ))
  }

  def getSummaryFromLocalStorage: Array[MatchChicago] = {
    LocalStorage.item(lastChicagoStorageKey).map{ s => readJson[MatchChicago](s)}.toArray
  }

  def getSummary(error: ()=>Unit): Unit = {
    logger.finer("Sending duplicatesummaries list request to server")
    import scala.scalajs.js.timers._
    setTimeout(1) { // note the absence of () =>
      if (BridgeDemo.isDemo) {
        val x = ChicagoSummaryStore.getChicagoSummary().getOrElse(getSummaryFromLocalStorage)
        logger.fine(s"Updating chicago summary from lastChicago: ${x}")
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

  def deleteChicago( id: MatchChicago.Id) = {
    BridgeDispatcher.deleteChicago(id)
    if (!BridgeDemo.isDemo) RestClientChicago.delete(id).recordFailure()
    else {
      logger.fine(s"Deleting match chicago from local storage, id=${id}")
      LocalStorage.item(lastChicagoStorageKey).map(s=>readJson[MatchChicago](s)).filter(mc=>mc.id == id).foreach { mc =>
        logger.fine(s"Actually deleting match chicago from local storage, id=${id}")
        LocalStorage.removeItem(lastChicagoStorageKey)
      }
    }
  }


  private var sseConnection: ServerEventConnection[MatchChicago.Id] = null

  private var useSSEFromServer: Boolean = true;

  def setUseSSEFromServer( b: Boolean ) = {
    if (b != useSSEFromServer) {
      useSSEFromServer = b
      setServerEventConnection()
    }
  }

  setServerEventConnection()

  private def setServerEventConnection(): Unit = {
    sseConnection = new SSE[MatchChicago.Id]( "/v1/sse/chicagos/", Listener)
  }

  object Listener extends SECListener[MatchChicago.Id] {
    def handleStart( dupid: MatchChicago.Id) = {
    }
    def handleStop( dupid: MatchChicago.Id) = {
    }

    def processMessage( msg: Protocol.ToBrowserMessage ) = {
      msg match {
        case Protocol.MonitorJoined(id,members) =>
        case Protocol.MonitorLeft(id,members) =>
        case Protocol.UpdateDuplicate(matchDuplicate) =>
        case Protocol.UpdateDuplicateHand(dupid, hand) =>
        case Protocol.UpdateDuplicateTeam(dupid,team) =>
        case _: Protocol.UpdateDuplicatePicture =>
        case _: Protocol.UpdateDuplicatePictures =>
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

  def monitor( dupid: MatchChicago.Id, restart: Boolean = false ): Unit = {

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

      if (BridgeDemo.isDemo) {
        LocalStorage.onstorage(localStorageListender)
        val amc = ChicagoSummaryStore.getChicagoSummary() match {
          case Some(value) =>
            logger.fine(s"Already have latest in summary store")
            value
          case None =>
            val array = getSummaryFromLocalStorage
            logger.fine(s"Updating chicago summary from lastChicago: ${array}")
            BridgeDispatcher.updateChicagoSummary(None, array)
            array.headOption.map( chi => BridgeDispatcher.updateChicago(chi) )
            array
        }
        val mc = ChicagoStore.getChicago.filter( _.id==dupid ).orElse {
          amc.find( _.id == dupid)
        }
        ChicagoStore.start(dupid,mc)
      }

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
