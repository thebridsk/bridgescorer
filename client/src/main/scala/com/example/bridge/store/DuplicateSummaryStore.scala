package com.example.bridge.store

import flux.dispatcher.DispatchToken
import com.example.data.BoardSet
import com.example.data.Movement
import com.example.bridge.action.ActionUpdateBoardSet
import com.example.bridge.action.BridgeDispatcher
import com.example.bridge.action.ActionUpdateMovement
import com.example.bridge.action.ActionUpdateAllBoardSets
import com.example.bridge.action.ActionUpdateAllMovement
import utils.logging.Logger
import com.example.bridge.action.ActionUpdateDuplicateSummary
import com.example.data.DuplicateSummary
import com.example.logger.Alerter

object DuplicateSummaryStore extends ChangeListenable {
  val logger = Logger("bridge.DuplicateSummaryStore")

  /**
   * Required to instantiate the store.
   */
  def init() = {}

  private var dispatchToken: Option[DispatchToken] = Some(BridgeDispatcher.register(dispatch _))

  def dispatch( msg: Any ) = Alerter.tryitWithUnit { msg match {
    case ActionUpdateDuplicateSummary(summary) =>
      updateBoardSets(summary)
    case x =>
      // There are multiple stores, all the actions get sent to all stores
//      logger.warning("BoardSetStore: Unknown msg dispatched, "+x)
  }}

  private var fSummary: Option[List[DuplicateSummary]] = None

  def getDuplicateSummary() = fSummary

  def updateBoardSets( summary: List[DuplicateSummary] ) = {
    fSummary = Option( summary )
    notifyChange()
  }

}
