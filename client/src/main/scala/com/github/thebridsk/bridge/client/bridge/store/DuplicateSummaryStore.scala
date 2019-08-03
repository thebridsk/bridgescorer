package com.github.thebridsk.bridge.client.bridge.store

import flux.dispatcher.DispatchToken
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateBoardSet
import com.github.thebridsk.bridge.client.bridge.action.BridgeDispatcher
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateMovement
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateAllBoardSets
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateAllMovement
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateDuplicateSummary
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateDuplicateSummaryItem
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateDuplicateSummaryDemoMatchItem
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateDuplicateSummaryDemoMatch
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.client.Bridge
import japgolly.scalajs.react.Callback
import com.github.thebridsk.bridge.data.SystemTime
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo

object DuplicateSummaryStore extends ChangeListenable {
  val logger = Logger("bridge.DuplicateSummaryStore")

  /**
   * Required to instantiate the store.
   */
  def init() = {}

  private var dispatchToken: Option[DispatchToken] = Some(BridgeDispatcher.register(dispatch _))

  def dispatch( msg: Any ) = Alerter.tryitWithUnit {
//    logger.info(s"DuplicateSummaryStore.dispatch $msg")
    msg match {
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
//        logger.warning("BoardSetStore: Unknown msg dispatched, "+x)
    }
  }

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
    if (BridgeDemo.isDemo) {
//      logger.info(s"""Update DuplicateSummaryStore from ${importId}: ${summary}""")
      if (importId == fImportId) {
        fMatchSummary = Option(summary)
        fImportId = importId
        fCalled = true;
        fSummary = Option( summary.map( md => DuplicateSummary.create(md)))
      }
      notifyChange()
    }
  }

  def updateDuplicateSummaryDemoMatchItem( importId: Option[String], summary: MatchDuplicate ) = {
    if (BridgeDemo.isDemo) {
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
