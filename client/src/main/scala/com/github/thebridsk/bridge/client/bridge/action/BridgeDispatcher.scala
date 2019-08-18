package com.github.thebridsk.bridge.client.bridge.action

import flux.dispatcher.FluxDispatcher
import scala.scalajs.js.annotation.ScalaJSDefined
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.Id
import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import flux.dispatcher.DispatchToken
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.data.Round
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.data.RubberHand
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.utilities.logging.TraceMsg
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.data.ServerURL

object BridgeDispatcher {
  val dispatcher: FluxDispatcher[BridgeAction] = FluxDispatcher() // new FluxDispatcher()
  val logger = Logger("bridge.BridgeDispatcher")

  def startDuplicateMatch( dupid: Id.MatchDuplicate ) = {
    logger.info("Setting up store for MatchDuplicate "+dupid )
    dispatcher.dispatch( ActionStartDuplicateMatch(dupid))
  }

  def stop() = dispatcher.dispatch( ActionStop() )

  def updateDuplicateSummary( importId: Option[String], summary: List[DuplicateSummary] ) = dispatcher.dispatch( ActionUpdateDuplicateSummary(importId,summary))

  def updateDuplicateSummaryItem( importId: Option[String], summary: DuplicateSummary ) = dispatcher.dispatch( ActionUpdateDuplicateSummaryItem(importId,summary))

  def updateDuplicateSummaryDemoMatch( importId: Option[String], summary: List[MatchDuplicate] ) = dispatcher.dispatch( ActionUpdateDuplicateSummaryDemoMatch(importId,summary))

  def updateDuplicateSummaryDemoMatchItem( importId: Option[String], summary: MatchDuplicate ) = dispatcher.dispatch( ActionUpdateDuplicateSummaryDemoMatchItem(importId,summary))

  def updateDuplicateResult( dr: MatchDuplicateResult ) = dispatcher.dispatch(ActionUpdateDuplicateResult(dr))

  def updateDuplicateMatch( duplicate: MatchDuplicate ) = dispatcher.dispatch( ActionUpdateDuplicateMatch( duplicate) )

  def updateDuplicateHand( dupid: Id.MatchDuplicate, hand: DuplicateHand ) = dispatcher.dispatch( ActionUpdateDuplicateHand( dupid, hand ))

  def updateTeam( dupid: Id.MatchDuplicate, team: Team ) = dispatcher.dispatch( ActionUpdateTeam( dupid, team ))

  def updateBoardSet( boardSet: BoardSet ) = dispatcher.dispatch( ActionUpdateBoardSet(boardSet))
  def updateAllBoardSet( boardSets: List[BoardSet] ) = dispatcher.dispatch( ActionUpdateAllBoardSets(boardSets))

  def updateMovement( boardSet: Movement ) = dispatcher.dispatch( ActionUpdateMovement(boardSet))
  def updateAllMovement( boardSets: List[Movement] ) = dispatcher.dispatch( ActionUpdateAllMovement(boardSets))

  def updateAllBoardSetAndMovements( boardSets: List[BoardSet], movements: List[Movement] ) = dispatcher.dispatch( ActionUpdateAllBoardsetsAndMovement(boardSets,movements))

  def updateChicagoSummary( importId: Option[String], summary: Array[MatchChicago] ) = dispatcher.dispatch( ActionUpdateChicagoSummary(importId,summary))

  def deleteChicago( id: Id.MatchChicago ) = dispatcher.dispatch( ActionDeleteChicago(id))

  def updateChicago( chi: MatchChicago, callback: Option[MatchChicago=>Unit]=None ) =
    dispatcher.dispatch( ActionUpdateChicago( chi, callback ))
  def updateChicagoNames( chiid: String, nplayer1: String, nplayer2: String, nplayer3: String, nplayer4: String, extra: Option[String], quintet: Boolean, simpleRotation: Boolean, callback: Option[MatchChicago=>Unit]=None ) = {
    logger.info("BridgeDispatcher.updateChicagoNames")
    dispatcher.dispatch( ActionUpdateChicagoNames( chiid, nplayer1, nplayer2, nplayer3, nplayer4, extra, quintet, simpleRotation, callback ))
  }
  def updateChicago5( chiid: String, extraPlayer: String, callback: Option[MatchChicago=>Unit]=None ) =
    dispatcher.dispatch(ActionUpdateChicago5(chiid,extraPlayer,callback))

  def updateChicagoRound( chiid: String, round: Round, callback: Option[MatchChicago=>Unit]=None ) = {
    logger.info("BridgeDispatcher.updateChicagoRound")
    dispatcher.dispatch( ActionUpdateChicagoRound( chiid, round, callback ))
  }
  def updateChicagoHand( chiid: String, roundid: Int, handid: Int, hand: Hand, callback: Option[MatchChicago=>Unit]=None ) =
    dispatcher.dispatch( ActionUpdateChicagoHand( chiid, roundid, handid, hand, callback ))

  def updateRubberList( importId: Option[String], summary: Array[MatchRubber] ) = dispatcher.dispatch( ActionUpdateRubberList(importId,summary))

  def deleteRubber( id: String ) = dispatcher.dispatch( ActionDeleteRubber(id))

  def updateRubber( chi: MatchRubber, callback: Option[MatchRubber=>Unit]=None ) =
    dispatcher.dispatch( ActionUpdateRubber( chi, callback ))
  def updateRubberNames( rubid: String, north: String, south: String, east: String, west: String, firstDealer: PlayerPosition, callback: Option[MatchRubber=>Unit]=None ) =
    dispatcher.dispatch( ActionUpdateRubberNames( rubid, north, south, east, west, firstDealer, callback ))
  def updateRubberHand( rubid: String, handid: String, hand: RubberHand, callback: Option[MatchRubber=>Unit]=None ) =
    dispatcher.dispatch( ActionUpdateRubberHand( rubid, handid, hand, callback ))

  def log( msg: TraceMsg ) = {
    if (dispatcher.isDispatching()) {
      import scala.scalajs.js.timers._

      setTimeout(0) { // note the absence of () =>
        dispatcher.dispatch( PostLogEntry(msg) )
      }
    } else {
      dispatcher.dispatch( PostLogEntry(msg) )
    }
  }
  def stopLogs() = dispatcher.dispatch(StopLogs())
  def startLogs() = dispatcher.dispatch(StartLogs())
  def clearLogs() = dispatcher.dispatch(ClearLogs())

  def updateServerURL( urls: ServerURL ) = dispatcher.dispatch( ActionUpdateServerURLs(urls) )

  /**
   * Waits for the callbacks specified to be invoked before continuing execution of the current callback.
   * This method should only be used by a callback in response to a dispatched payload.
   */
  def waitFor(token: js.Array[DispatchToken]): Unit = dispatcher.waitFor(token)
  /**
   * Registers a callback to be invoked with every dispatched payload. Returns a token that can be used with waitFor().
   */
  def register(handler: js.Function1[BridgeAction,Any]): DispatchToken = dispatcher.register(handler)
  /**
   * Removes a callback based on its token.
   */
  def unregister( token: DispatchToken ): Unit = dispatcher.unregister(token)
  /**
   * Is this Dispatcher currently dispatching.
   */
  def isDispatching(): Boolean = dispatcher.isDispatching()

}
