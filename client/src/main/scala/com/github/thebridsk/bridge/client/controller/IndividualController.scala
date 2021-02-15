package com.github.thebridsk.bridge.client.controller

import com.github.thebridsk.bridge.client.bridge.action.BridgeDispatcher
import com.github.thebridsk.bridge.data.IndividualDuplicate
import com.github.thebridsk.bridge.data.websocket.Protocol
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.bridge.store.DuplicateStore
import com.github.thebridsk.bridge.clientcommon.websocket.DuplexPipe
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.LogEntryS
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.LogEntryV2
import com.github.thebridsk.bridge.clientcommon.rest2.Result
import scala.concurrent.ExecutionContext
import com.github.thebridsk.bridge.clientcommon.rest2.RestResult
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import scala.concurrent.Future
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxResult
import org.scalactic.source.Position
import com.github.thebridsk.bridge.clientcommon.rest2.WrapperXMLHttpRequest
import com.github.thebridsk.bridge.data.websocket.Protocol.ToServerMessage
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientIndividualDuplicate
import com.github.thebridsk.bridge.data.IndividualDuplicateHand
import com.github.thebridsk.bridge.data.IndividualMovement
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import com.github.thebridsk.bridge.data.rest.JsonSupport._
import com.github.thebridsk.bridge.clientcommon.graphql.GraphQLClient
import play.api.libs.json.JsDefined
import play.api.libs.json.JsUndefined
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import com.github.thebridsk.bridge.clientcommon.rest2.ResultObject

object IndividualController extends {
  val logger: Logger = Logger("bridge.IndividualController")

  class AlreadyStarted extends Exception

  private var sseConnection: ServerEventConnection[IndividualDuplicate.Id] = null

  private var duplexPipe: Option[DuplexPipe] = None

  var useRestToServer: Boolean = true;
  private var useSSEFromServer: Boolean = true;

  def setUseSSEFromServer(b: Boolean): Unit = {
    if (b != useSSEFromServer) {
      useSSEFromServer = b
      setServerEventConnection()
    }
  }

  setServerEventConnection()

  private def setServerEventConnection(): Unit = {
    sseConnection = if (useSSEFromServer) {
      logger.fine(s"Creating SSE for individual duplicates")
      new SSE[IndividualDuplicate.Id]("/v1/sse/individualduplicates/", Listener)
    } else {
      new DuplexPipeServerEventConnection("", Listener) {

        def actionStartMonitor(mdid: IndividualDuplicate.Id): ToServerMessage = {
          Protocol.StartMonitorIndividualDuplicate(mdid)
        }
        def actionStopMonitor(mdid: IndividualDuplicate.Id): ToServerMessage = {
          Protocol.StopMonitorIndividualDuplicate(mdid)
        }

      }
    }
  }

  def log(entry: LogEntryS): Unit = {
    // This can't create a duplexPipe, we haven't setup all the info
    duplexPipe match {
      case Some(dp) => dp.sendlog(entry)
      case None     =>
    }
  }

  def log(entry: LogEntryV2): Unit = {
    // This can't create a duplexPipe, we haven't setup all the info
    duplexPipe match {
      case Some(dp) => dp.sendlog(entry)
      case None     =>
    }
  }

  class CreateResultIndividualDuplicate(
      ajaxResult: AjaxResult[WrapperXMLHttpRequest],
      future: Future[IndividualDuplicate]
  )(implicit
      pos: Position,
      executor: ExecutionContext
  ) extends CreateResult[IndividualDuplicate](ajaxResult, future) {

    def this(
        result: RestResult[IndividualDuplicate]
    )(implicit executor: ExecutionContext) = {
      this(result.ajaxResult, result.future)
    }

    def updateStore(mc: IndividualDuplicate): IndividualDuplicate = {
      monitor(mc.id)
      BridgeDispatcher.updateIndividualDuplicate(mc)
      logger.info(s"Created new duplicate game: ${mc.id}")
      mc
    }

  }

  import scala.concurrent.ExecutionContext.Implicits.global

  /**
    * Create a duplicate match and start monitoring it
    * @param url the base URL of the server, no trailing slash.
    */
  def createIndividualDuplicate(
      default: Boolean = true,
      boards: Option[BoardSet.Id] = None,
      movement: Option[IndividualMovement.Id] = None
  ): Result[IndividualDuplicate] = {
    val result = RestClientIndividualDuplicate
      .createIndividualDuplicate(
        IndividualDuplicate.create(),
        default = default,
        boards = boards,
        movement = movement
      )
      .recordFailure()
    new CreateResultIndividualDuplicate(result)
  }

  def getIndividualDuplicate(id: IndividualDuplicate.Id): Result[IndividualDuplicate] = {
    RestClientIndividualDuplicate.get(id).recordFailure()
  }

  def deleteIndividualDuplicate(id: IndividualDuplicate.Id): Result[Unit] = {
    logger.info("Deleting IndividualDuplicate with id " + id)
    stop()
    val result = RestClientIndividualDuplicate.delete(id).recordFailure()
    result.foreach(msg => {
      logger.info("Controller: Deleted IndividualDuplicate with id " + id)
    })
    result
  }

  object Listener extends SECListener[IndividualDuplicate.Id] {
    def handleStart(dupid: IndividualDuplicate.Id): Unit = {
      BridgeDispatcher.startIndividualDuplicate(dupid)
    }
    def handleStop(dupid: IndividualDuplicate.Id): Unit = {
      BridgeDispatcher.stop()
    }

    def processMessage(msg: Protocol.ToBrowserMessage): Unit = {
      msg match {
        case Protocol.MonitorJoined(id, members) =>
        case Protocol.MonitorLeft(id, members)   =>
        case Protocol.UpdateDuplicate(matchDuplicate) =>
        case Protocol.UpdateDuplicateHand(dupid, hand) =>
        case Protocol.UpdateDuplicateTeam(dupid, team) =>
        case Protocol.UpdateDuplicatePicture(dupid, boardid, handid, picture) =>
        case Protocol.UpdateDuplicatePictures(dupid, pictures) =>

        case Protocol.UpdateIndividualDuplicate(matchDuplicate) =>
          logger.fine(s"got an update to individual duplicate: $matchDuplicate")
          BridgeDispatcher.updateIndividualDuplicate(matchDuplicate)
        case Protocol.UpdateIndividualDuplicateHand(dupid, hand) =>
          BridgeDispatcher.updateIndividualDuplicateHand(dupid, hand)
        case Protocol.UpdateIndividualDuplicatePicture(dupid, boardid, handid, picture) =>
          BridgeDispatcher.updateIndividualPicture(dupid, boardid, handid, picture)
        case Protocol.UpdateIndividualDuplicatePictures(dupid, pictures) =>
          BridgeDispatcher.updateIndividualPictures(dupid, pictures)

        case Protocol.NoData(_)                  =>
        case Protocol.UpdateChicago(_)           =>
        case Protocol.UpdateChicagoRound(_, _)   =>
        case Protocol.UpdateChicagoHand(_, _, _) =>
        case Protocol.UpdateRubber(_)            =>
        case _: Protocol.UpdateRubberHand        =>
      }
    }
  }

  def send(msg: ToServerMessage): Unit = {
    sseConnection.getDuplexPipeServerEventConnection().map(c => c.send(msg))
  }

  def updateMatch(dup: IndividualDuplicate): Unit = {
    BridgeDispatcher.updateIndividualDuplicate(dup)
    if (!BridgeDemo.isDemo) {
      if (useRestToServer) {
        val resource =
          RestClientIndividualDuplicate.update(dup.id, dup).recordFailure().onComplete {
            t =>
              if (t.isFailure) {
                Alerter.alert("Failure updating hand on server")
              }
          }
      } else {
        val msg = Protocol.UpdateIndividualDuplicate(dup)
        send(msg)
      }
    }
    logger.info("Update hand (" + dup.id + "," + dup + ")")
  }

  def updateHand(dup: IndividualDuplicate, hand: IndividualDuplicateHand): Unit = {
    BridgeDispatcher.updateIndividualDuplicateHand(dup.id, hand)
    if (!BridgeDemo.isDemo) {
      if (useRestToServer) {
        val resource =
          RestClientIndividualDuplicate.boardResource(dup.id).handResource(hand.board)
        resource.update(hand.id, hand).recordFailure().onComplete { t =>
          if (t.isFailure) {
            Alerter.alert(s"Failure updating hand on server\n${hand}")
          }
        }
      } else {
        val msg = Protocol.UpdateIndividualDuplicateHand(dup.id, hand)
        send(msg)
      }
    }
    logger.info(
      "Update hand (" + dup.id + "," + hand.board + "," + hand.id + ")"
    )
  }

  def updateHandOld(dup: IndividualDuplicate, hand: IndividualDuplicateHand): Unit = {
    BridgeDispatcher.updateIndividualDuplicateHand(dup.id, hand)
    RestClientIndividualDuplicate
      .boardResource(dup.id)
      .handResource(hand.board)
      .update(hand.id, hand)
      .recordFailure()
      .foreach(h => {
        logger.info(
          "Update hand (" + dup.id + "," + hand.board + "," + hand.id + ")"
        )
      })
  }

  def updatePlayers(
    dup: IndividualDuplicate,
    newplayers: Map[Int, String]
  ): Result[List[String]] = {
    if (AjaxResult.isEnabled.getOrElse(false)) {
      serverUpdatePlayers(dup, newplayers)
    } else {
      new ResultObject(
        Future {
          val md = newplayers.foldLeft(dup) { (ac, e) =>
            val (ip, name) = e
            ac.updatePlayer(ip, name)
          }
          updateMatch(md)
          md.players
        }
      )
    }
  }

  case class Player(i: Int, name: String)
  case class Players(players: List[Player])

  implicit val PlayerFormat = Json.format[Player]
  implicit val PlayersFormat = Json.format[Players]

  case class ResponseUpdatePlayers(
    updatePlayers: List[String]
  )

  implicit val ResponseUpdatePlayersFormat = Json.format[ResponseUpdatePlayers]

  private def serverUpdatePlayers(
    dup: IndividualDuplicate,
    newplayers: Map[Int, String]
  ): AjaxResult[List[String]] = {
    // val query =
    //   """mutation updatePlayers( $dupId: IndividualDuplicateId!, $players: Players! ) {
    //     |  individualduplicate( id: $dupId ) {
    //     |     players( players: {
    //     |      players: [
    //     |        {
    //     |           i: 1
    //     |           name: "Uno"
    //     |        },
    //     |        {
    //     |          i:2
    //     |          name: "Two"
    //     |        }
    //     |      ]
    //     |    })
    //     |   }
    //     |}""".stripMargin
    val query =
      """mutation updatePlayers( $dupId: IndividualDuplicateId!, $players: Players! ) {
        |  individualduplicate( id: $dupId ) {
        |     updatePlayers( players: $players )
        |   }
        |}""".stripMargin
    val p = Json.toJson(
      Players(
        newplayers.map { e =>
          val (k, v) = e
          Player(k, v)
        }.toList
      )
    )
    val vars = JsObject(
      Seq("dupId" -> Json.toJson(dup.id), "players" -> p)
    )
    val op = Some("updatePlayers")
    val result = GraphQLClient.request(query, Some(vars), op)
    // resultGraphQL.set(result)
    result
      .map { gr =>
        gr.data match {
          case Some(data) =>
            data \ "individualduplicate" match {
              case JsDefined(x: JsObject) =>
                Json.fromJson[ResponseUpdatePlayers](x) match {
                  case JsSuccess(y, _) =>
                    logger.fine(s"updatePlayers return ${y}")
                    y.updatePlayers
                  case JsError(e) =>
                    logger.fine(s"unexpected value from updatePlayers, got ${x}: ${e}")
                    throw new Exception(s"unexpected value from updatePlayers, got ${x}")
                }
              case JsDefined(x) =>
                throw new Exception(s"unexpected value from updatePlayers, got ${x}")
              case _: JsUndefined =>
                throw new Exception(
                  s"error updating players, did not find individualduplicate field, got ${Json.prettyPrint(data)}"
                )
            }
          case None =>
            throw new Exception(
              s"error updating players, ${gr.getError()}"
            )
        }
      }
      .recover {
        case x: Exception =>
          logger.warning(
            s"exception updating players",
            x
          )
          throw new Exception(s"exception updating players", x)
      }

  }

  def monitor(dupid: IndividualDuplicate.Id, restart: Boolean = false): Unit = {

    if (AjaxResult.isEnabled.getOrElse(false)) {
      DuplicateStore.getId() match {
        case Some(mdid) =>
          sseConnection.cancelStop()
          if (restart || mdid != dupid || !sseConnection.isConnected) {
            logger.info(
              s"""Switching IndividualDuplicate monitor to ${dupid} from ${mdid}"""
            )
            sseConnection.monitor(dupid, restart)
          } else {
            // already monitoring id
            logger.info(s"""Already monitoring IndividualDuplicate ${dupid}""")
          }
        case None =>
          logger.info(s"""Starting IndividualDuplicate monitor to ${dupid}""")
          sseConnection.monitor(dupid, restart)
      }
    } else {
      BridgeDispatcher.startIndividualDuplicate(dupid)
    }

  }

  /**
    * Stop monitoring a duplicate match
    */
  def delayStop(): Unit = {
    logger.fine(s"Controller.delayStop ${DuplicateStore.getId()}")
    sseConnection.delayStop()
  }

  /**
    * Stop monitoring a duplicate match
    */
  def stop(): Unit = {
    logger.fine(s"Controller.stop ${DuplicateStore.getId()}")
    sseConnection.stop()
  }
}
