package com.github.thebridsk.bridge.client.bridge

import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.data.Round
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.data.RubberHand
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.utilities.logging.TraceMsg
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.data.ServerURL
import com.github.thebridsk.bridge.clientcommon.dispatcher.Action

package object action {
import _root_.com.github.thebridsk.bridge.data.DuplicatePicture

  sealed trait BridgeAction extends Action

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
  case class ActionUpdatePicture( dupid: String, boardid: String, picture: Option[DuplicatePicture]) extends DuplicateBridgeAction
  case class ActionUpdatePictures( dupid: String, picture: List[DuplicatePicture]) extends DuplicateBridgeAction

  sealed trait BoardSetAction extends BridgeAction
  sealed trait MovementAction extends BridgeAction

  case class ActionCreateBoardSet( boardSet: BoardSet ) extends BoardSetAction
  case class ActionDeleteBoardSet( id: String ) extends BoardSetAction
  case class ActionUpdateBoardSet( boardSet: BoardSet ) extends BoardSetAction
  case class ActionCreateMovement( movement: Movement ) extends MovementAction
  case class ActionDeleteMovement( id: String ) extends MovementAction
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

  case class ActionUpdateServerURLs( urls: ServerURL ) extends BridgeAction

}
