package com.github.thebridsk.bridge.client.controller

import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientRubber
import com.github.thebridsk.bridge.client.bridge.store.RubberStore
import com.github.thebridsk.bridge.client.bridge.action.BridgeDispatcher
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.data.RubberHand
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxResult
import scala.concurrent.ExecutionContext
import com.github.thebridsk.bridge.clientcommon.rest2.ResultObject
import com.github.thebridsk.bridge.clientcommon.rest2.RestResult
import org.scalactic.source.Position
import scala.concurrent.Future
import com.github.thebridsk.bridge.clientcommon.rest2.WrapperXMLHttpRequest
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import com.github.thebridsk.bridge.clientcommon.graphql.GraphQLClient
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsDefined
import com.github.thebridsk.bridge.data.rest.JsonSupport
import play.api.libs.json.Json
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import play.api.libs.json.JsUndefined
import com.github.thebridsk.bridge.client.bridge.store.RubberListStore
import scala.util.Success
import scala.util.Failure
import com.github.thebridsk.bridge.data.websocket.Protocol
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo
import com.github.thebridsk.bridge.clientcommon.rest2.Result

object RubberController {
  val logger: Logger = Logger("bridge.RubberController")

  class CreateResultMatchRubber(
      ajaxResult: AjaxResult[WrapperXMLHttpRequest],
      future: Future[MatchRubber]
  )(implicit
      pos: Position,
      executor: ExecutionContext
  ) extends CreateResult[MatchRubber](ajaxResult, future) {

    def this(
        result: RestResult[MatchRubber]
    )(implicit executor: ExecutionContext) = {
      this(result.ajaxResult, result.future)
    }

    def updateStore(mc: MatchRubber): MatchRubber = {
      showMatch(mc)
      logger.info(s"Created new rubber game: ${mc.id}")
      mc
    }

  }

  import scala.concurrent.ExecutionContext.Implicits.global

  private var currentId = 0

  def createMatch(): Result[MatchRubber] = {
    AjaxResult.isEnabled
      .orElse(Some(true))
      .map(e => e && !BridgeDemo.isDemo) match {
      case Some(true) | None =>
        // enabled - Some(true)
        // mocked - None
        logger.info("Sending create rubber to server")
        val rub = MatchRubber(MatchRubber.idNul, "", "", "", "", "", Nil)
        val r = RestClientRubber.create(rub).recordFailure()
        new CreateResultMatchRubber(r)
      case Some(false) =>
        // disabled
        currentId = currentId + 1
        val created =
          MatchRubber(MatchRubber.id(currentId), "", "", "", "", "", Nil)
        logger.info("PageRubber: created new local rubber game: " + created.id)
        showMatch(created)
        new ResultObject(created)
    }
  }

  def showMatch(rub: MatchRubber): Unit = {
    RubberStore.start(rub.id, Some(rub))
    logger.fine("calling callback with " + rub.id)
    BridgeDispatcher.updateRubber(rub)
  }

  def ensureMatch(rubid: MatchRubber.Id): Result[MatchRubber] = {
    if (!RubberStore.isMonitoredId(rubid)) {
      val result = RestClientRubber.get(rubid).recordFailure()
      result.foreach(created => {
        logger.info("PageRubber: got rubber game: " + created.id)
        showMatch(created)
      })
      result
    } else {
      new ResultObject(RubberStore.getRubber.get)
    }
  }

  def updateMatch(rub: MatchRubber): Unit = {
    logger.info("dispatrubng an update to MatchRubber " + rub.id)
    BridgeDispatcher.updateRubber(rub, Some(updateServer))
  }

  def updateServer(rub: MatchRubber): Unit = {
    if (!BridgeDemo.isDemo) {
      RestClientRubber
        .update(rub.id, rub)
        .recordFailure()
        .foreach(updated => {
          logger.fine("PageRubber: Updated rubber game: " + rub.id)
          // the BridgeDispatcher.updateRubber causes a timing problem.
          // if two updates are done one right after the other, then the second
          // update will be lost.
          //      BridgeDispatcher.updateRubber(updated)
        })
    }
  }

  def updateRubberNames(
      rubid: MatchRubber.Id,
      north: String,
      south: String,
      east: String,
      west: String,
      firstDealer: PlayerPosition
  ): Unit = {
    BridgeDispatcher.updateRubberNames(
      rubid,
      north,
      south,
      east,
      west,
      firstDealer,
      Some(updateServer)
    )
  }

  def updateRubberHand(
      rubid: MatchRubber.Id,
      handid: String,
      hand: RubberHand
  ): Unit = {
    BridgeDispatcher.updateRubberHand(rubid, handid, hand, Some(updateServer))
  }

  def getSummary(error: () => Unit): Unit = {
    logger.finer("Sending rubbers list request to server")
    import scala.scalajs.js.timers._
    setTimeout(1) { // note the absence of () =>
      if (BridgeDemo.isDemo) {
        val x = RubberListStore.getRubberSummary().getOrElse(Array())
        BridgeDispatcher.updateRubberList(None, x)
      } else {
        RestClientRubber.list().recordFailure().onComplete { trylist =>
          Alerter.tryitWithUnit {
            trylist match {
              case Success(list) =>
                logger.finer(s"RubberList got ${list.size} entries")
                BridgeDispatcher.updateRubberList(None, list)
              case Failure(err) =>
                error()
            }
          }
        }
      }
    }
  }

  def getImportSummary(importId: String, error: () => Unit): Unit = {
    logger.finer(
      s"Sending import rubbersummaries ${importId} list request to server"
    )
    import scala.scalajs.js.timers._
    setTimeout(1) { // note the absence of () =>
      GraphQLClient
        .request(
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
          Some(JsObject(Seq("importId" -> JsString(importId)))),
          Some("importRubbers")
        )
        .map { r =>
          r.data match {
            case Some(data) =>
              data \ "import" \ "rubbers" match {
                case JsDefined(jds) =>
                  import JsonSupport._
                  Json.fromJson[List[MatchRubber]](jds) match {
                    case JsSuccess(ds, path) =>
                      logger.finer(
                        s"Import(${importId})/rubbers got ${ds.size} entries"
                      )
                      BridgeDispatcher.updateRubberList(
                        Some(importId),
                        ds.toArray
                      )
                    case JsError(err) =>
                      logger.warning(
                        s"Import(${importId})/chicagos, JSON error: ${JsError.toJson(err)}"
                      )
                      error()
                  }
                case _: JsUndefined =>
                  logger.warning(
                    s"error import rubber list ${importId}, did not find import/rubbers field"
                  )
                  error()
              }
            case None =>
              logger.warning(
                s"error import rubber list ${importId}, ${r.getError()}"
              )
              error()
          }
        }
        .recover {
          case x: Exception =>
            logger.warning(s"exception import rubber list ${importId}", x)
            error()
        }
        .foreach { x => }
    }
  }

  def deleteRubber(id: MatchRubber.Id): Any = {
    BridgeDispatcher.deleteRubber(id)
    if (!BridgeDemo.isDemo) RestClientRubber.delete(id).recordFailure()
  }

  private var sseConnection: ServerEventConnection[MatchRubber.Id] = null

  private var useSSEFromServer: Boolean = true;

  def setUseSSEFromServer(b: Boolean): Unit = {
    if (b != useSSEFromServer) {
      useSSEFromServer = b
      setServerEventConnection()
    }
  }

  setServerEventConnection()

  private def setServerEventConnection(): Unit = {
    sseConnection = new SSE[MatchRubber.Id]("/v1/sse/rubbers/", Listener)
  }

  object Listener extends SECListener[MatchRubber.Id] {
    def handleStart(rubid: MatchRubber.Id): Unit = {}
    def handleStop(rubid: MatchRubber.Id): Unit = {}

    def processMessage(msg: Protocol.ToBrowserMessage): Unit = {
      msg match {
        case Protocol.MonitorJoined(id, members)       =>
        case Protocol.MonitorLeft(id, members)         =>
        case Protocol.UpdateDuplicate(matchDuplicate)  =>
        case Protocol.UpdateDuplicateHand(rubid, hand) =>
        case Protocol.UpdateDuplicateTeam(rubid, team) =>
        case _: Protocol.UpdateDuplicatePicture        =>
        case _: Protocol.UpdateDuplicatePictures       =>
        case _: Protocol.UpdateIndividualDuplicate     =>
        case _: Protocol.UpdateIndividualDuplicateHand =>
        case _: Protocol.UpdateIndividualDuplicatePicture        =>
        case _: Protocol.UpdateIndividualDuplicatePictures       =>
        case Protocol.NoData(_)                        =>
        case Protocol.UpdateChicago(mc)                =>
        case Protocol.UpdateChicagoRound(mc, r)        =>
        case Protocol.UpdateChicagoHand(mc, r, hand)   =>
        case Protocol.UpdateRubber(mr) =>
          BridgeDispatcher.updateRubber(mr)
        case Protocol.UpdateRubberHand(mrid, hand) =>
          BridgeDispatcher.updateRubberHand(mrid, hand.id, hand)
      }
    }
  }

  def monitor(rubid: MatchRubber.Id, restart: Boolean = false): Unit = {

    if (AjaxResult.isEnabled.getOrElse(false)) {
      RubberStore.getMonitoredId match {
        case Some(mdid) =>
          sseConnection.cancelStop()
          if (restart || mdid != rubid || !sseConnection.isConnected) {
            logger.info(
              s"""Switching MatchChicago monitor to ${rubid} from ${mdid}"""
            )
            RubberStore.start(rubid, None)
            sseConnection.monitor(rubid, restart)
          } else {
            // already monitoring id
            logger.info(s"""Already monitoring MatchChicago ${rubid}""")
          }
        case None =>
          logger.info(s"""Starting MatchChicago monitor to ${rubid}""")
          RubberStore.start(rubid, None)
          sseConnection.monitor(rubid, restart)
      }
    } else {}

  }

  /**
    * Stop monitoring a duplicate match
    */
  def delayStop(): Unit = {
    logger.fine(s"Controller.delayStop ${RubberStore.getMonitoredId}")
    sseConnection.delayStop()
  }

  /**
    * Stop monitoring a duplicate match
    */
  def stop(): Unit = {
    logger.fine(s"Controller.stop ${RubberStore.getMonitoredId}")
    sseConnection.stop()
  }

}
