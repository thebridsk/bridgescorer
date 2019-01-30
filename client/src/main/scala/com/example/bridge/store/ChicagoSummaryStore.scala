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
import com.example.bridge.action.ActionUpdateChicago
import com.example.Bridge

object ChicagoSummaryStore extends ChangeListenable {
  val logger = Logger("bridge.ChicagoSummaryStore")

  /**
   * Required to instantiate the store.
   */
  def init() = {}

//  logger.warning("ChicagoSummaryStore initialized")

  private var dispatchToken: Option[DispatchToken] = Some(BridgeDispatcher.register(dispatch _))

  def dispatch( msg: Any ) = Alerter.tryitWithUnit {
//    logger.info(s"Received $msg")
    msg match {
      case ActionUpdateChicagoSummary(importId,summary) =>
        updateChicagoSummary(importId,summary)
      case ActionDeleteChicago(id) =>
        if (fImportId.isEmpty) {
          fSummary = fSummary.map { a => a.filter( c => c.id != id ) }
        }

      case ActionUpdateChicago(chi,cb) =>
        if (Bridge.isDemo) {
          logger.info("Updating MatchChicago for demo")
          val n = fSummary.map { a =>
            val r = a.filter( c => chi.id != c.id )
            r :+ chi
          }.orElse(Some(Array(chi)))
          fSummary = n
          logger.info(s"""Chicago summary is now $n""")
          // does not call callback, that is for ChicagoStore
        }

      case x =>
        // There are multiple stores, all the actions get sent to all stores
  //      logger.warning("BoardSetStore: Unknown msg dispatched, "+x)
    }
  }

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
