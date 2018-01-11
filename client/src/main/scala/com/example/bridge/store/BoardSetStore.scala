package com.example.bridge.store

import flux.dispatcher.DispatchToken
import scala.collection.mutable
import com.example.data.BoardSet
import com.example.data.Movement
import com.example.bridge.action.ActionUpdateBoardSet
import com.example.bridge.action.BridgeDispatcher
import com.example.bridge.action.ActionUpdateMovement
import com.example.bridge.action.ActionUpdateAllBoardSets
import com.example.bridge.action.ActionUpdateAllMovement
import utils.logging.Logger
import com.example.logger.Alerter
import com.example.bridge.action.ActionUpdateAllBoardsetsAndMovement

object BoardSetStore extends ChangeListenable {
  val logger = Logger("bridge.BoardSetStore")

  /**
   * Required to instantiate the store.
   */
  def init() = {}

  private var dispatchToken: Option[DispatchToken] = Some(BridgeDispatcher.register(dispatch _))

  def dispatch( msg: Any ) = Alerter.tryitWithUnit  {
    msg match {
      case ActionUpdateAllBoardSets(boardSets) =>
        updateBoardSets(true, boardSets: _*)
      case ActionUpdateAllMovement(movement) =>
        updateMovement(true, movement: _*)
      case ActionUpdateBoardSet(boardSet) =>
        updateBoardSets(false, boardSet)
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

  private val boardSets = mutable.Map[String,BoardSet]()
  private val movements = mutable.Map[String,Movement]()

  def getBoardSets() = Map( boardSets.toList: _*)
  def getBoardSet( name: String ) = boardSets(name)

  def getMovement() = Map( movements.toList: _*)
  def getMovement( name: String ) = movements(name)

  def updateBoardSetsNoNotify( deleteMissing: Boolean, newboardSets: BoardSet* ) = {
    val keys = mutable.Set( boardSets.keySet.toSeq: _* )
    newboardSets.foreach { bs => {
      val name = bs.name
      keys -= name
      boardSets += name->bs
    }}
    if (deleteMissing) keys.foreach { key => boardSets.remove(key) }
  }

  def updateBoardSets( deleteMissing: Boolean, newboardSets: BoardSet* ) = {
    updateBoardSetsNoNotify(deleteMissing, newboardSets: _*)
    notifyChange()
  }

  def updateMovementNoNotify( deleteMissing: Boolean, newMovement: Movement* ) = {
    val keys = mutable.Set( movements.keySet.toSeq: _* )
    newMovement.foreach { bs => {
      val name = bs.name
      keys -= name
      movements += name->bs
    }}
    if (deleteMissing) keys.foreach { key => movements.remove(key) }
  }

  def updateMovement( deleteMissing: Boolean, newMovement: Movement* ) = {
    updateMovementNoNotify(deleteMissing, newMovement: _*)
    notifyChange()
  }
}
