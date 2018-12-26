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
import com.example.bridge.action.ActionUpdateRubberList
import com.example.logger.Alerter
import com.example.bridge.action.ActionDeleteRubber
import com.example.data.MatchRubber

object RubberListStore extends ChangeListenable {
  val logger = Logger("bridge.RubberListStore")

  /**
   * Required to instantiate the store.
   */
  def init() = {}

  private var dispatchToken: Option[DispatchToken] = Some(BridgeDispatcher.register(dispatch _))

  def dispatch( msg: Any ) = Alerter.tryitWithUnit { msg match {
    case ActionUpdateRubberList(importId,summary) =>
      updateRubberList(importId,summary)
    case ActionDeleteRubber(id) =>
      if (fImportId.isEmpty) {
        fSummary = fSummary.map { a => a.filter( c => c.id != id ) }
      }

    case x =>
      // There are multiple stores, all the actions get sent to all stores
//      logger.warning("BoardSetStore: Unknown msg dispatched, "+x)
  }}

  private var fSummary: Option[Array[MatchRubber]] = None
  private var fImportId: Option[String] = None

  def getRubberSummary() = fSummary
  def getImportId = fImportId

  def updateRubberList( importId: Option[String], summary: Array[MatchRubber] ) = {
    fSummary = Option( summary )
    fImportId = importId
    logger.fine(s"Got ${summary.length} entries from import $importId")
    notifyChange()
  }

}
