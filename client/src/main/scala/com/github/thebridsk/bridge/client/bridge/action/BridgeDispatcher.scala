package com.github.thebridsk.bridge.client.bridge.action

import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.DuplicateHand
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
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.clientcommon.dispatcher.Dispatcher
import com.github.thebridsk.bridge.data.DuplicatePicture
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.IndividualDuplicatePicture
import com.github.thebridsk.bridge.data.IndividualDuplicate
import com.github.thebridsk.bridge.data.IndividualDuplicateHand
import com.github.thebridsk.bridge.data.IndividualBoard
import com.github.thebridsk.bridge.data.bridge.individual.{ DuplicateSummary => IDuplicateSummary }
import com.github.thebridsk.bridge.data.IndividualMovement

object BridgeDispatcher extends Dispatcher {
  val logger: Logger = Logger("bridge.BridgeDispatcher")

  def startDuplicateMatch(dupid: MatchDuplicate.Id): Unit = {
    logger.info("Setting up store for MatchDuplicate " + dupid)
    dispatcher.dispatch(ActionStartDuplicateMatch(dupid))
  }

  def stop(): Unit = dispatcher.dispatch(ActionStop())

  def startIndividualDuplicate(dupid: IndividualDuplicate.Id): Unit = {
    logger.info("Setting up store for IndividualDuplicate " + dupid)
    dispatcher.dispatch(ActionStartIndividualDuplicate(dupid))
  }

  def stopIndividualDuplicate(): Unit = dispatcher.dispatch(ActionStopIndividualDuplicate())

  def updateDuplicateSummary(
      importId: Option[String],
      summary: List[DuplicateSummary]
  ): Unit = dispatcher.dispatch(ActionUpdateDuplicateSummary(importId, summary))

  def updateDuplicateSummaryItem(
      importId: Option[String],
      summary: DuplicateSummary
  ): Unit =
    dispatcher.dispatch(ActionUpdateDuplicateSummaryItem(importId, summary))

  def updateDuplicateSummaryDemoMatch(
      importId: Option[String],
      summary: List[MatchDuplicate]
  ): Unit =
    dispatcher.dispatch(
      ActionUpdateDuplicateSummaryDemoMatch(importId, summary)
    )

  def updateDuplicateSummaryDemoMatchItem(
      importId: Option[String],
      summary: MatchDuplicate
  ): Unit =
    dispatcher.dispatch(
      ActionUpdateDuplicateSummaryDemoMatchItem(importId, summary)
    )

  def updateDuplicateResult(dr: MatchDuplicateResult): Unit =
    dispatcher.dispatch(ActionUpdateDuplicateResult(dr))

  def updateDuplicateMatch(duplicate: MatchDuplicate): Unit =
    dispatcher.dispatch(ActionUpdateDuplicateMatch(duplicate))

  def updateDuplicateHand(dupid: MatchDuplicate.Id, hand: DuplicateHand): Unit =
    dispatcher.dispatch(ActionUpdateDuplicateHand(dupid, hand))

  def updateTeam(dupid: MatchDuplicate.Id, team: Team): Unit =
    dispatcher.dispatch(ActionUpdateTeam(dupid, team))
  def updatePicture(
      dupid: MatchDuplicate.Id,
      boardid: Board.Id,
      handid: Team.Id,
      picture: Option[DuplicatePicture]
  ): Unit =
    dispatcher.dispatch(ActionUpdatePicture(dupid, boardid, handid, picture))
  def updatePictures(
      dupid: MatchDuplicate.Id,
      pictures: List[DuplicatePicture]
  ): Unit = dispatcher.dispatch(ActionUpdatePictures(dupid, pictures))

  def updateIndividualDuplicateSummary(
      importId: Option[String],
      summary: List[IDuplicateSummary]
  ): Unit = dispatcher.dispatch(ActionUpdateIndividualDuplicateSummary(importId, summary))

  def updateIndividualDuplicateSummaryItem(
      importId: Option[String],
      summary: IDuplicateSummary
  ): Unit =
    dispatcher.dispatch(ActionUpdateIndividualDuplicateSummaryItem(importId, summary))

  def updateIndividualDuplicateSummaryDemoMatch(
      importId: Option[String],
      summary: List[MatchDuplicate]
  ): Unit =
    dispatcher.dispatch(
      ActionUpdateIndividualDuplicateSummaryDemoMatch(importId, summary)
    )

  def updateIndividualDuplicateSummaryDemoMatchItem(
      importId: Option[String],
      summary: MatchDuplicate
  ): Unit =
    dispatcher.dispatch(
      ActionUpdateIndividualDuplicateSummaryDemoMatchItem(importId, summary)
    )

  def updateIndividualDuplicate(duplicate: IndividualDuplicate): Unit =
    dispatcher.dispatch(ActionUpdateIndividualDuplicate(duplicate))
  def updateIndividualDuplicateHand(dupid: IndividualDuplicate.Id, hand: IndividualDuplicateHand): Unit =
    dispatcher.dispatch(ActionUpdateIndividualDuplicateHand(dupid, hand))
  def updateIndividualPicture(
      dupid: IndividualDuplicate.Id,
      boardid: IndividualBoard.Id,
      handid: IndividualDuplicateHand.Id,
      picture: Option[IndividualDuplicatePicture]
  ): Unit =
    dispatcher.dispatch(ActionUpdateIndividualPicture(dupid, boardid, handid, picture))
  def updateIndividualPictures(
      dupid: IndividualDuplicate.Id,
      pictures: List[IndividualDuplicatePicture]
  ): Unit = dispatcher.dispatch(ActionUpdateIndividualPictures(dupid, pictures))

  def createBoardSet(boardSet: BoardSet): Unit =
    dispatcher.dispatch(ActionCreateBoardSet(boardSet))
  def deleteBoardSet(boardSetId: BoardSet.Id): Unit =
    dispatcher.dispatch(ActionDeleteBoardSet(boardSetId))
  def updateBoardSet(boardSet: BoardSet): Unit =
    dispatcher.dispatch(ActionUpdateBoardSet(boardSet))
  def updateAllBoardSet(boardSets: List[BoardSet]): Unit =
    dispatcher.dispatch(ActionUpdateAllBoardSets(boardSets))

  def createMovement(movement: Movement): Unit =
    dispatcher.dispatch(ActionCreateMovement(movement))
  def deleteMovement(movementId: Movement.Id): Unit =
    dispatcher.dispatch(ActionDeleteMovement(movementId))
  def updateMovement(movement: Movement): Unit =
    dispatcher.dispatch(ActionUpdateMovement(movement))
  def updateAllMovement(movements: List[Movement]): Unit =
    dispatcher.dispatch(ActionUpdateAllMovement(movements))

  def createIndividualMovement(movement: IndividualMovement): Unit =
    dispatcher.dispatch(ActionCreateIndividualMovement(movement))
  def deleteIndividualMovement(movementId: IndividualMovement.Id): Unit =
    dispatcher.dispatch(ActionDeleteIndividualMovement(movementId))
  def updateIndividualMovement(movement: IndividualMovement): Unit =
    dispatcher.dispatch(ActionUpdateIndividualMovement(movement))
  def updateAllIndividualMovement(movements: List[IndividualMovement]): Unit =
    dispatcher.dispatch(ActionUpdateAllIndividualMovement(movements))

  def updateAllBoardSetAndMovements(
      boardSets: List[BoardSet],
      movements: List[Movement],
      individualmovements: List[IndividualMovement]
  ): Unit =
    dispatcher.dispatch(
      ActionUpdateAllBoardsetsAndMovement(boardSets, movements, individualmovements)
    )

  def updateChicagoSummary(
      importId: Option[String],
      summary: Array[MatchChicago]
  ): Unit = dispatcher.dispatch(ActionUpdateChicagoSummary(importId, summary))

  def deleteChicago(id: MatchChicago.Id): Unit =
    dispatcher.dispatch(ActionDeleteChicago(id))

  def updateChicago(
      chi: MatchChicago,
      callback: Option[MatchChicago => Unit] = None
  ): Unit =
    dispatcher.dispatch(ActionUpdateChicago(chi, callback))
  def updateChicagoNames(
      chiid: MatchChicago.Id,
      nplayer1: String,
      nplayer2: String,
      nplayer3: String,
      nplayer4: String,
      extra: Option[String],
      quintet: Boolean,
      simpleRotation: Boolean,
      callback: Option[MatchChicago => Unit] = None
  ): Unit = {
    logger.info("BridgeDispatcher.updateChicagoNames")
    dispatcher.dispatch(
      ActionUpdateChicagoNames(
        chiid,
        nplayer1,
        nplayer2,
        nplayer3,
        nplayer4,
        extra,
        quintet,
        simpleRotation,
        callback
      )
    )
  }
  def updateChicago5(
      chiid: MatchChicago.Id,
      extraPlayer: String,
      callback: Option[MatchChicago => Unit] = None
  ): Unit =
    dispatcher.dispatch(ActionUpdateChicago5(chiid, extraPlayer, callback))

  def updateChicagoRound(
      chiid: MatchChicago.Id,
      round: Round,
      callback: Option[MatchChicago => Unit] = None
  ): Unit = {
    logger.info("BridgeDispatcher.updateChicagoRound")
    dispatcher.dispatch(ActionUpdateChicagoRound(chiid, round, callback))
  }
  def updateChicagoHand(
      chiid: MatchChicago.Id,
      roundid: Int,
      handid: Int,
      hand: Hand,
      callback: Option[MatchChicago => Unit] = None
  ): Unit =
    dispatcher.dispatch(
      ActionUpdateChicagoHand(chiid, roundid, handid, hand, callback)
    )

  def updateRubberList(
      importId: Option[String],
      summary: Array[MatchRubber]
  ): Unit = dispatcher.dispatch(ActionUpdateRubberList(importId, summary))

  def deleteRubber(id: MatchRubber.Id): Unit =
    dispatcher.dispatch(ActionDeleteRubber(id))

  def updateRubber(
      chi: MatchRubber,
      callback: Option[MatchRubber => Unit] = None
  ): Unit =
    dispatcher.dispatch(ActionUpdateRubber(chi, callback))
  def updateRubberNames(
      rubid: MatchRubber.Id,
      north: String,
      south: String,
      east: String,
      west: String,
      firstDealer: PlayerPosition,
      callback: Option[MatchRubber => Unit] = None
  ): Unit =
    dispatcher.dispatch(
      ActionUpdateRubberNames(
        rubid,
        north,
        south,
        east,
        west,
        firstDealer,
        callback
      )
    )
  def updateRubberHand(
      rubid: MatchRubber.Id,
      handid: String,
      hand: RubberHand,
      callback: Option[MatchRubber => Unit] = None
  ): Unit =
    dispatcher.dispatch(ActionUpdateRubberHand(rubid, handid, hand, callback))

}
