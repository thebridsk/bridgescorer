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

  class CreateResultMatchDuplicate( result: Result[MatchDuplicate])(implicit executor: ExecutionContext)
        extends CreateResult[MatchDuplicate](result) {

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
      })
      duplexPipe = Some(d)
      d
  }

  def updateHand( dup: MatchDuplicate, hand: DuplicateHand ) = {
    BridgeDispatcher.updateDuplicateHand(dup.id, hand)
    val msg = Protocol.UpdateDuplicateHand(dup.id, hand)
    getDuplexPipe().send(msg)
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
    val msg = Protocol.UpdateDuplicateTeam(dup.id, team)
    getDuplexPipe().send(msg)
    logger.info("Update team ("+dup.id+","+team+")")
  }

  def updateTeamOld( dup: MatchDuplicate, team: Team ) = {
    BridgeDispatcher.updateTeam(dup.id, team)
    RestClientDuplicate.teamResource(dup.id).update(team.id, team).recordFailure().foreach( t => {
      logger.info("Update team ("+dup.id+","+team.id+")")
    })
  }

  def monitorMatchDuplicate( dupid: Id.MatchDuplicate ): Unit = {
    DuplicateStore.getId() match {
      case Some(mdid) =>
        if (mdid != dupid) {
          logger.info(s"""Switching MatchDuplicate monitor to ${dupid} from ${mdid}""" )
          getDuplexPipe().clearSession(Protocol.StopMonitor(mdid))
          BridgeDispatcher.startDuplicateMatch(dupid)
          getDuplexPipe().setSession { dp =>
            logger.info(s"""In Session: Switching MatchDuplicate monitor to ${dupid} from ${mdid}""" )
            dp.send(Protocol.StartMonitor(dupid))
          }
        } else {
          // already monitoring id
          logger.info(s"""Already monitoring MatchDuplicate ${dupid}""" )
        }
      case None =>
        logger.info(s"""Starting MatchDuplicate monitor to ${dupid}""" )
        BridgeDispatcher.startDuplicateMatch(dupid)
        getDuplexPipe().setSession { dp =>
          logger.info(s"""In Session: Starting MatchDuplicate monitor to ${dupid}""" )
          dp.send(Protocol.StartMonitor(dupid))
        }
    }
  }

  /**
   * Stop monitoring a duplicate match
   */
  def stop() = duplexPipe match {
    case Some(d) =>
      DuplicateStore.getId() match {
        case Some(id) =>
          d.clearSession(Protocol.StopMonitor(id))
        case None =>
      }
      BridgeDispatcher.stop()
    case _ =>

  }

  def getSummary(): Unit = {
    logger.finer("Sending duplicatesummaries list request to server")
    import scala.scalajs.js.timers._
    setTimeout(1) { // note the absence of () =>
      RestClientDuplicateSummary.list().recordFailure().foreach { list => Alerter.tryitWithUnit {
        logger.finer(s"DuplicateSummary got ${list.size} entries")
        BridgeDispatcher.updateDuplicateSummary(list.toList)
      }}
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
