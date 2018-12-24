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
import com.example.bridge.action.ActionUpdateChicagoSummary
import com.example.logger.Alerter
import com.example.data.MatchChicago
import com.example.bridge.action.ActionDeleteChicago

object ChicagoSummaryStore extends ChangeListenable {
  val logger = Logger("bridge.ChicagoSummaryStore")

  /**
   * Required to instantiate the store.
   */
  def init() = {}

  private var dispatchToken: Option[DispatchToken] = Some(BridgeDispatcher.register(dispatch _))

  def dispatch( msg: Any ) = Alerter.tryitWithUnit { msg match {
    case ActionUpdateChicagoSummary(importId,summary) =>
      updateChicagoSummary(importId,summary)
    case ActionDeleteChicago(id) =>
      if (fImportId.isEmpty) {
        fSummary = fSummary.map { a => a.filter( c => c.id != id ) }
      }

    case x =>
      // There are multiple stores, all the actions get sent to all stores
//      logger.warning("BoardSetStore: Unknown msg dispatched, "+x)
  }}

  private var fSummary: Option[Array[MatchChicago]] = None
  private var fImportId: Option[String] = None

  def getChicagoSummary() = fSummary
  def getImportId = fImportId

  def updateChicagoSummary( importId: Option[String], summary: Array[MatchChicago] ) = {
    fSummary = Option( summary )
    fImportId = importId
    notifyChange()
  }

}
