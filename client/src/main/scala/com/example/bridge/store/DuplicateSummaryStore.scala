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
import com.example.bridge.action.ActionUpdateDuplicateSummaryItem
import com.example.bridge.action.ActionUpdateDuplicateSummaryDemoMatchItem
import com.example.bridge.action.ActionUpdateDuplicateSummaryDemoMatch
import com.example.data.MatchDuplicate
import com.example.Bridge
import japgolly.scalajs.react.Callback
import com.example.data.SystemTime

object DuplicateSummaryStore extends ChangeListenable {
  val logger = Logger("bridge.DuplicateSummaryStore")

  /**
   * Required to instantiate the store.
   */
  def init() = {}

  private var dispatchToken: Option[DispatchToken] = Some(BridgeDispatcher.register(dispatch _))

  def dispatch( msg: Any ) = Alerter.tryitWithUnit { msg match {
    case ActionUpdateDuplicateSummary(importId,summary) =>
      updateDuplicateSummary(importId,summary)
    case ActionUpdateDuplicateSummaryItem(importId,summary) =>
      updateDuplicateSummaryItem(importId,summary)
    case ActionUpdateDuplicateSummaryDemoMatch(importId,summary) =>
      updateDuplicateSummaryDemoMatch(importId,summary)
    case ActionUpdateDuplicateSummaryDemoMatchItem(importId,summary) =>
      updateDuplicateSummaryDemoMatchItem(importId,summary)
    case x =>
      // There are multiple stores, all the actions get sent to all stores
//      logger.warning("BoardSetStore: Unknown msg dispatched, "+x)
  }}

  private var fMatchSummary: Option[List[MatchDuplicate]] = None
  private var fSummary: Option[List[DuplicateSummary]] = None
  private var fImportId: Option[String] = None
  private var fCalled: Boolean = false
  private var fLastCalledImportId = 0.0

  private val maxTimeLastCalledImportId = 1000.0

  override
  def noListener() = {
    fCalled = false;
  }

  def getDuplicateMatchSummary() = fMatchSummary
  def getDuplicateSummary() = fSummary
  def getImportId = fImportId

  def getCalledImportId = {
    val currentTime = SystemTime.currentTimeMillis()
    val rc = if (fCalled) {
      if (currentTime - fLastCalledImportId < maxTimeLastCalledImportId) {
        Right(fImportId)
      }
      else {
        Left("within timeout")
      }
    }
    else {
      Left("never called")
    }
    fLastCalledImportId = currentTime
    rc
  }

  def updateDuplicateSummary( importId: Option[String], summary: List[DuplicateSummary] ) = {
    logger.fine(s"""Update DuplicateSummaryStore from ${importId}: ${summary}""")
    fSummary = Option( summary )
    fImportId = importId
    fCalled = true;
    notifyChange()
  }

  def updateDuplicateSummaryItem( importId: Option[String], summary: DuplicateSummary ) = {
    logger.fine(s"""Update DuplicateSummaryStore from ${importId}: ${summary}""")
    if (importId == fImportId) {
      fSummary = fSummary.map { list =>
        list.map { ds =>
          if (ds.id == summary.id) summary
          else ds
        }
      }
    } else {
      fSummary = Some( List(summary) )
      fImportId = importId
    }
    logger.fine(s"""Updated DuplicateSummaryStore from ${fImportId}: ${fSummary}""")
    notifyChange()
  }

  def updateDuplicateSummaryDemoMatch( importId: Option[String], summary: List[MatchDuplicate] ) = {
    if (Bridge.isDemo) {
      logger.fine(s"""Update DuplicateSummaryStore from ${importId}: ${summary}""")
      if (importId == fImportId) {
        fMatchSummary = Option(summary)
        fImportId = importId
        fSummary = Option( summary.map( md => DuplicateSummary.create(md)))
      }
      notifyChange()
    }
  }

  def updateDuplicateSummaryDemoMatchItem( importId: Option[String], summary: MatchDuplicate ) = {
    if (Bridge.isDemo) {
      logger.fine(s"""Update DuplicateSummaryStore from ${importId}: ${summary}""")
      if (importId == fImportId) {
        fMatchSummary = fMatchSummary.map { list =>
          list.map { ds =>
            if (ds.id == summary.id) summary
            else ds
          }
        }
      } else {
        fMatchSummary = Some( List(summary) )
        fImportId = importId
      }
      logger.fine(s"""Updated DuplicateSummaryStore from ${fImportId}: ${fSummary}""")
      notifyChange()
    }
  }

}
