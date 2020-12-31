package com.github.thebridsk.bridge.client.bridge.store

import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateIndividualDuplicateHand
import com.github.thebridsk.bridge.client.bridge.action.BridgeDispatcher
import com.github.thebridsk.bridge.data.IndividualDuplicate
import com.github.thebridsk.utilities.logging.Logger
import flux.dispatcher.DispatchToken
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateIndividualDuplicate
import com.github.thebridsk.bridge.client.bridge.action.ActionStartIndividualDuplicate
import com.github.thebridsk.bridge.client.bridge.action.ActionStopIndividualDuplicate
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateScore
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateViewPerspective
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateViewPerspective.PerspectiveDirector
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateViewPerspective.PerspectiveComplete
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateViewPerspective.PerspectiveTable
import com.github.thebridsk.bridge.data.IndividualBoard
import com.github.thebridsk.bridge.client.bridge.action.IndividualDuplicateBridgeAction
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import japgolly.scalajs.react.Callback
import com.github.thebridsk.bridge.clientcommon.react.BeepComponent
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo
import com.github.thebridsk.bridge.data.IndividualDuplicatePicture
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateIndividualPicture
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateIndividualPictures
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.data.IndividualDuplicateHand

object IndividualDuplicateStore extends ChangeListenable {
  val logger: Logger = Logger("bridge.IndividualDuplicateStore")

  /**
    * Required to instantiate the store.
    */
  def init(): Unit = {}

  private var monitoredId: Option[IndividualDuplicate.Id] = None
  private var bridgeMatch: Option[IndividualDuplicate] = None
  private var directorsView: Option[IndividualDuplicateScore] = None
  private var completeView: Option[IndividualDuplicateScore] = None
  private var tableViews = Map[(Table.Id, Int), IndividualDuplicateScore]()

  private var pictures = Map[(IndividualBoard.Id, IndividualDuplicateHand.Id), IndividualDuplicatePicture]()

  def getId() = monitoredId
  def getMatch(): Option[IndividualDuplicate] = {
    logger.fine(s"getMatch returning bridgeMatch=$bridgeMatch")
    bridgeMatch
  }

  def getBoardsFromRound(table: Table.Id, round: Int): List[IndividualDuplicateHand] = {
    bridgeMatch match {
      case Some(md) => md.getHandsInRound(table, round)
      case None     => Nil
    }
  }

  def getTablePerspectiveFromRound(
      table: Table.Id,
      round: Int
  ): Option[IndividualDuplicateViewPerspective] = {
    Some(PerspectiveTable(table,round))
  }

  def getView(
      perspective: IndividualDuplicateViewPerspective
  ): Option[IndividualDuplicateScore] =
    perspective match {
      case PerspectiveDirector      => getDirectorsView()
      case PerspectiveComplete      => getCompleteView()
      case PerspectiveTable(t1, t2) => getTableView(t1, t2)
    }

  def getDirectorsView(): Option[IndividualDuplicateScore] =
    directorsView match {
      case Some(dv) => Some(dv)
      case None =>
        directorsView = bridgeMatch.map(PerspectiveDirector.score)
        directorsView
    }

  def getCompleteView(): Option[IndividualDuplicateScore] =
    completeView match {
      case Some(dv) => Some(dv)
      case None =>
        completeView = bridgeMatch.map(PerspectiveComplete.score)
        completeView
    }

  def getTableView(
      table: Table.Id,
      round: Int
  ): Option[IndividualDuplicateScore] = {
    tableViews.get((table,round))
      .orElse {
        bridgeMatch
          .map(PerspectiveTable(table,round).score)
          .map { sc =>
            tableViews = tableViews + ((table,round) -> sc)
            sc
          }
      }
  }

  def getPicture(
      dupid: IndividualDuplicate.Id,
      boardId: IndividualBoard.Id,
      handId: IndividualDuplicateHand.Id
  ): Option[IndividualDuplicatePicture] = {
    monitoredId.filter(dup => dup == dupid).flatMap { dup =>
      pictures.get((boardId, handId))
    }
  }

  def getPicture(
      dupid: IndividualDuplicate.Id,
      boardId: IndividualBoard.Id
  ): List[IndividualDuplicatePicture] = {
    monitoredId
      .filter(dup => dup == dupid)
      .map { dup =>
        pictures.flatMap { e =>
          val ((bid, hid), dp) = e
          if (bid == boardId) dp :: Nil
          else Nil
        }.toList
      }
      .getOrElse(List())
  }

  private var dispatchToken: Option[DispatchToken] = Some(
    BridgeDispatcher.register(dispatch _)
  )

  def dispatch(msg: Any): Unit =
    Alerter.tryitWithUnit {
      msg match {
        case m: IndividualDuplicateBridgeAction =>
          m match {
            case ActionStartIndividualDuplicate(dupid) =>
              start(dupid)
            case ActionStopIndividualDuplicate() =>
              stop()
              notifyChange()
            case ActionUpdateIndividualDuplicate(duplicate) =>
              monitoredId match {
                case Some(mid) =>
                  if (mid == duplicate.id) {
                    logger.info(s"Updating duplicate match $mid")
                    bridgeMatch = Some(duplicate)
                    logger.info(
                      s"Updated duplicate match $mid, bridgeMatch=$bridgeMatch"
                    )
                    resetViews()
                    notifyChange()
                    // if (BridgeDemo.isDemo) {
                    //   scalajs.js.timers.setTimeout(1) {
                    //     BridgeDispatcher.updateDuplicateSummaryDemoMatchItem(
                    //       None,
                    //       duplicate
                    //     )
                    //     BridgeDispatcher.updateDuplicateSummaryItem(
                    //       None,
                    //       DuplicateSummary.create(duplicate)
                    //     )
                    //   }
                    // }
                  } else {
                    logger.severe(
                      "Duplicate IDs don't match, working with " + mid + " got " + duplicate.id
                    )
                  }
                case _ =>
                  logger.severe(
                    "Got an update when no longer monitoring: " + duplicate
                  )
              }

            case ActionUpdateIndividualDuplicateHand(dupid, hand) =>
              bridgeMatch match {
                case Some(md) =>
                  if (md.id == dupid) {
                    logger.info("Updating hand in " + md.id + ": " + hand)
                    (md.getHand(hand.board, hand.id) match {
                      case Some(oldhand) =>
                        if (!oldhand.equalsIgnoreModifyTime(hand)) {
                          logger.fine(
                            "Updating hand in DuplicateStore: " + hand
                          )
                          bridgeMatch = Some(md.updateHand(hand))
                          logger.info(
                            s"Updated hand duplicate match $dupid, bridgeMatch=$bridgeMatch"
                          )
                          resetViews()
                          notifyChange()
                          bridgeMatch
                        } else {
                          logger.fine(
                            "Not updating hand in DuplicateStore: " + hand + ", oldhand: " + oldhand
                          )
                          None
                        }
                      case None =>
                        bridgeMatch = Some(md.updateHand(hand))
                        logger.info(
                          s"Updated hand duplicate match $dupid, bridgeMatch=$bridgeMatch"
                        )
                        resetViews()
                        notifyChange()
                        bridgeMatch
                    }).map { md =>
                      // if (BridgeDemo.isDemo) {
                      //   scalajs.js.timers.setTimeout(1) {
                      //     BridgeDispatcher.updateDuplicateSummaryDemoMatchItem(
                      //       None,
                      //       md
                      //     )
                      //     BridgeDispatcher.updateDuplicateSummaryItem(
                      //       None,
                      //       DuplicateSummary.create(md)
                      //     )
                      //   }
                      // }
                    }
                  } else {
                    logger.severe(
                      "Duplicate IDs don't match, working with " + md.id + " got " + dupid
                    )
                  }
                case _ =>
                  logger.severe(
                    "Got a hand update and don't have a IndividualDuplicate"
                  )
              }
            case ActionUpdateIndividualPicture(dupid, boardid, handid, picture) =>
              val key = (boardid, handid)
              monitoredId.foreach { monitored =>
                if (dupid == monitored) {
                  picture match {
                    case Some(p) =>
                      pictures = pictures + (key -> p)
                    case None =>
                      pictures = pictures - key
                  }
                  notifyChange()
                }
              }
            case ActionUpdateIndividualPictures(dupid, picts) =>
              monitoredId.foreach { monitored =>
                if (dupid == monitored) {
                  pictures = picts.map { p =>
                    p.key -> p
                  }.toMap
                  notifyChange()
                }
              }
            case x =>
            // There are multiple stores, all the actions get sent to all stores
//          logger.fine("Ignoring unknown action: "+action)
          }
        case action =>
        // There are multiple stores, all the actions get sent to all stores
//      logger.fine("Ignoring unknown action: "+action)
      }
    }

  def start(dupid: IndividualDuplicate.Id): Unit = {
    monitoredId match {
      case Some(mid) if mid != dupid =>
        bridgeMatch = None
        stop()
      case _ =>
    }
    logger.info(s"Starting to monitor $dupid, bridgeMatch=$bridgeMatch")
    monitoredId = Some(dupid)
    // if (BridgeDemo.isDemo) {
    //   if (bridgeMatch.isEmpty)
    //     bridgeMatch =
    //       DuplicateSummaryStore.getDuplicateMatchSummary.flatMap(list =>
    //         list.find(md => md.id == dupid)
    //       )
    //   logger.info(s"monitoring bridgeMatch=$bridgeMatch")
    // }
    notifyChange()
  }

  private def resetViews() = {
    directorsView = None
    completeView = None
    tableViews = Map()
  }

  def stop(): Unit = {
    if (BridgeDemo.isDemo) {
      // keep the match
    } else {
      monitoredId match {
        case Some(id) =>
          logger.info("Stopping monitor " + id)
          monitoredId = None
          bridgeMatch = None
          logger.info("Stopped monitor " + id)
          resetViews()
          pictures = Map()
          //        removeAllListener(ChangeListenable.event)
          notifyChange()
        case _ =>
          logger.info("Stopping monitor")
          bridgeMatch = None
          logger.info("Stopped monitor")
      }
    }
  }

  private var lastRoundComplete: Boolean = false

  val monitorRoundEnd: Callback = Callback {
    lastRoundComplete = getCompleteView().map { v =>
      val f = v.isAtRoundEnd
      if (f && !lastRoundComplete) BeepComponent.beep()
      f
    }.getOrElse(false)
  }

  def startMonitorRoundEnd(): Unit = {
    addChangeListener(monitorRoundEnd)
  }

  def stopMonitorRoundEnd(): Unit = {
    removeChangeListener(monitorRoundEnd)
  }

  startMonitorRoundEnd()

  override def addChangeListener(cb: Callback): Unit = {
    super.addChangeListener(cb)
    if (BridgeDemo.isDemo) {
      scalajs.js.timers.setTimeout(1) {
        notifyChange()
      }
    }
  }

}
