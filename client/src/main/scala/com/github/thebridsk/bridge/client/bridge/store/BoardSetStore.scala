package com.github.thebridsk.bridge.client.bridge.store

import flux.dispatcher.DispatchToken
import scala.collection.mutable
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateBoardSet
import com.github.thebridsk.bridge.client.bridge.action.BridgeDispatcher
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateMovement
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateAllBoardSets
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateAllMovement
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateAllBoardsetsAndMovement
import com.github.thebridsk.bridge.client.bridge.action.ActionCreateBoardSet
import com.github.thebridsk.bridge.client.bridge.action.ActionDeleteBoardSet
import com.github.thebridsk.bridge.client.bridge.action.ActionCreateMovement
import com.github.thebridsk.bridge.client.bridge.action.ActionDeleteMovement

object BoardSetStore extends ChangeListenable {
  val logger: Logger = Logger("bridge.BoardSetStore")

  /**
   * Required to instantiate the store.
   */
  def init(): Unit = {}

  private var dispatchToken: Option[DispatchToken] = Some(BridgeDispatcher.register(dispatch _))

  def dispatch( msg: Any ): Unit = Alerter.tryitWithUnit  {
    msg match {
      case ActionUpdateAllBoardSets(boardSets) =>
        updateBoardSets(true, boardSets: _*)
      case ActionUpdateAllMovement(movement) =>
        updateMovement(true, movement: _*)
      case ActionCreateBoardSet(boardSet) =>
        updateBoardSets(false, boardSet)
      case ActionDeleteBoardSet(boardSet) =>
        deleteBoardSets(boardSet)
      case ActionUpdateBoardSet(boardSet) =>
        updateBoardSets(false, boardSet)
      case ActionCreateMovement(movement) =>
        updateMovement(false, movement)
      case ActionDeleteMovement(movement) =>
        deleteMovement(movement)
      case ActionUpdateMovement(movement) =>
        updateMovement(false, movement)
      case ActionUpdateAllBoardsetsAndMovement(newboardSets,newmovements) =>
        updateBoardSetsNoNotify(true, newboardSets: _*)
        updateMovementNoNotify(true, newmovements: _*)
        notifyChange()
      case x =>
        // There are multiple stores, all the actions get sent to all stores
        // logger.warning("BoardSetStore: Unknown msg dispatched, "+x)
    }
  }

  private val boardSets = mutable.Map[BoardSet.Id,BoardSet]()
  private val movements = mutable.Map[Movement.Id,Movement]()

  def getBoardSets(): Map[BoardSet.Id,BoardSet] = Map( boardSets.toList: _*)
  def getBoardSet( name: BoardSet.Id ): Option[BoardSet] = boardSets.get(name)

  def getMovement(): Map[Movement.Id,Movement] = Map( movements.toList: _*)
  def getMovement( name: Movement.Id ): Option[Movement] = movements.get(name)

  def updateBoardSetsNoNotify( deleteMissing: Boolean, newboardSets: BoardSet* ): Unit = {
    val keys = mutable.Set( boardSets.keySet.toSeq: _* )
    newboardSets.foreach { bs => {
      val name = bs.name
      keys -= name
      boardSets += name->bs
    }}
    if (deleteMissing) keys.foreach { key => boardSets.remove(key) }
  }

  def updateBoardSets( deleteMissing: Boolean, newboardSets: BoardSet* ): Unit = {
    logger.fine("updating all boardsets")
    updateBoardSetsNoNotify(deleteMissing, newboardSets: _*)
    notifyChange()
  }

  def deleteBoardSets( id: BoardSet.Id ): Unit = {
    boardSets.remove(id)
    notifyChange()
  }

  def updateMovementNoNotify( deleteMissing: Boolean, newMovement: Movement* ): Unit = {
    val keys = mutable.Set( movements.keySet.toSeq: _* )
    newMovement.foreach { bs => {
      val name = bs.name
      keys -= name
      movements += name->bs
    }}
    if (deleteMissing) keys.foreach { key => movements.remove(key) }
  }

  def updateMovement( deleteMissing: Boolean, newMovement: Movement* ): Unit = {
    updateMovementNoNotify(deleteMissing, newMovement: _*)
    notifyChange()
  }

  def deleteMovement( id: Movement.Id ): Unit = {
    movements.remove(id)
    notifyChange()
  }

}
