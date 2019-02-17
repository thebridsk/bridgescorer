package com.example.bridge

import com.example.data.DuplicateHand
import com.example.data.MatchDuplicate
import com.example.data.Id
import com.example.data.Team
import com.example.data.BoardSet
import com.example.data.Movement
import com.example.data.Round
import com.example.data.Hand
import com.example.data.MatchChicago
import com.example.data.MatchRubber
import com.example.data.bridge.PlayerPosition
import com.example.data.RubberHand
import com.example.data.DuplicateSummary
import utils.logging.TraceMsg
import com.example.data.MatchDuplicateResult

package object action {

  sealed trait BridgeAction

  sealed trait DuplicateBridgeAction extends BridgeAction

  /**
   * @param importId the import Id.  If None, then this is the main store.
   * @param summary
   */
  case class ActionUpdateDuplicateSummary( importId: Option[String], summary: List[DuplicateSummary] ) extends BridgeAction

  case class ActionUpdateDuplicateSummaryItem( importId: Option[String], summary: DuplicateSummary ) extends BridgeAction

  /**
   * @param importId the import Id.  If None, then this is the main store.
   * @param summary
   */
  case class ActionUpdateDuplicateSummaryDemoMatch( importId: Option[String], summary: List[MatchDuplicate] ) extends BridgeAction

  case class ActionUpdateDuplicateSummaryDemoMatchItem( importId: Option[String], summary: MatchDuplicate ) extends BridgeAction

  case class ActionUpdateDuplicateResult( dr: MatchDuplicateResult ) extends BridgeAction

  case class ActionStartDuplicateMatch( dupid: Id.MatchDuplicate ) extends DuplicateBridgeAction
  case class ActionStop() extends DuplicateBridgeAction
  case class ActionUpdateDuplicateMatch( duplicate: MatchDuplicate) extends DuplicateBridgeAction
  case class ActionUpdateDuplicateHand( dupid: String, hand: DuplicateHand) extends DuplicateBridgeAction
  case class ActionUpdateTeam( dupid: String, team: Team) extends DuplicateBridgeAction

  sealed trait BoardSetAction extends BridgeAction
  sealed trait MovementAction extends BridgeAction

  case class ActionUpdateBoardSet( boardSet: BoardSet ) extends BoardSetAction
  case class ActionUpdateMovement( movement: Movement ) extends MovementAction
  case class ActionUpdateAllBoardSets( boardSets: List[BoardSet] ) extends BoardSetAction
  case class ActionUpdateAllMovement( movement: List[Movement] ) extends MovementAction
  case class ActionUpdateAllBoardsetsAndMovement( boardSets: List[BoardSet], movement: List[Movement] ) extends MovementAction

  sealed trait ChicagoBridgeAction extends BridgeAction

  /**
   * @param importId the import Id.  If None, then this is the main store.
   * @param summary
   */
  case class ActionUpdateChicagoSummary( importId: Option[String], summary: Array[MatchChicago] ) extends ChicagoBridgeAction
  case class ActionDeleteChicago( chi: Id.MatchChicago ) extends ChicagoBridgeAction

  case class ActionUpdateChicago( chi: MatchChicago, callback: Option[MatchChicago=>Unit]=None ) extends ChicagoBridgeAction
  case class ActionUpdateChicagoNames( chiid: String, nplayer1: String, nplayer2: String, nplayer3: String, nplayer4: String, extra: Option[String], quintet: Boolean, simpleRotation: Boolean, callback: Option[MatchChicago=>Unit]=None ) extends ChicagoBridgeAction
  case class ActionUpdateChicagoRound( chiid: String, round: Round, callback: Option[MatchChicago=>Unit]=None ) extends ChicagoBridgeAction
  case class ActionUpdateChicagoHand( chiid: String, roundid: Int, handid: Int, hand: Hand, callback: Option[MatchChicago=>Unit]=None ) extends ChicagoBridgeAction
  case class ActionUpdateChicago5( chiid: String, extraplayer: String, callback: Option[MatchChicago=>Unit]=None ) extends ChicagoBridgeAction

  sealed trait RubberBridgeAction extends BridgeAction

  /**
   * @param importId the import Id.  If None, then this is the main store.
   * @param summary
   */
  case class ActionUpdateRubberList( importId: Option[String], summary: Array[MatchRubber] ) extends RubberBridgeAction
  case class ActionDeleteRubber( rub: String ) extends RubberBridgeAction

  case class ActionUpdateRubber( rub: MatchRubber, callback: Option[MatchRubber=>Unit]=None ) extends RubberBridgeAction
  case class ActionUpdateRubberNames( rubid: String, north: String, south: String, east: String, west: String, firstDealer: PlayerPosition, callback: Option[MatchRubber=>Unit]=None ) extends RubberBridgeAction
  case class ActionUpdateRubberHand( rubid: String, handid: String, hand: RubberHand, callback: Option[MatchRubber=>Unit]=None ) extends RubberBridgeAction

  case class PostLogEntry( traceMsg: TraceMsg ) extends BridgeAction
  case class StopLogs() extends BridgeAction
  case class StartLogs() extends BridgeAction
  case class ClearLogs() extends BridgeAction

}
