package com.github.thebridsk.bridge.bridge.store

import flux.dispatcher.DispatchToken
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.bridge.action.ActionUpdateBoardSet
import com.github.thebridsk.bridge.bridge.action.BridgeDispatcher
import com.github.thebridsk.bridge.bridge.action.ActionUpdateMovement
import com.github.thebridsk.bridge.bridge.action.ActionUpdateAllBoardSets
import com.github.thebridsk.bridge.bridge.action.ActionUpdateAllMovement
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.bridge.action.ActionUpdateRubberList
import com.github.thebridsk.bridge.logger.Alerter
import com.github.thebridsk.bridge.bridge.action.ActionDeleteRubber
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.Bridge
import com.github.thebridsk.bridge.bridge.action.ActionUpdateRubber

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

    case ActionUpdateRubber(rub,cb) =>
      if (Bridge.isDemo) {
        val n = fSummary.map { a =>
          val r = a.filter( c => rub.id != c.id )
          r :+ rub
        }.orElse(Some(Array(rub)))
        fSummary = n
        // does not call callback, that is for RubberStore
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
