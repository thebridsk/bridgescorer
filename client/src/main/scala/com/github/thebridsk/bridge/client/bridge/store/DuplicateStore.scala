package com.github.thebridsk.bridge.client.bridge.store

import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateDuplicateHand
import com.github.thebridsk.bridge.client.bridge.action.BridgeDispatcher
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.utilities.logging.Logger
import flux.dispatcher.DispatchToken
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateDuplicateMatch
import com.github.thebridsk.bridge.client.bridge.action.ActionStartDuplicateMatch
import com.github.thebridsk.bridge.client.bridge.action.ActionStop
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore
import com.github.thebridsk.bridge.data.bridge.PerspectiveDirector
import com.github.thebridsk.bridge.data.bridge.PerspectiveComplete
import com.github.thebridsk.bridge.data.bridge.DuplicateViewPerspective
import com.github.thebridsk.bridge.data.bridge.PerspectiveTable
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateTeam
import com.github.thebridsk.bridge.client.bridge.action.DuplicateBridgeAction
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import japgolly.scalajs.react.Callback
import com.github.thebridsk.bridge.clientcommon.react.BeepComponent
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo
import com.github.thebridsk.bridge.data.DuplicatePicture
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdatePicture
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdatePictures
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.DuplicateHandV2

object DuplicateStore extends ChangeListenable {
  val logger: Logger = Logger("bridge.DuplicateStore")

  /**
    * Required to instantiate the store.
    */
  def init(): Unit = {}

  private var monitoredId: Option[MatchDuplicate.Id] = None
  private var bridgeMatch: Option[MatchDuplicate] = None
  private var directorsView: Option[MatchDuplicateScore] = None
  private var completeView: Option[MatchDuplicateScore] = None
  private var teamsView = Map[(Team.Id, Team.Id), MatchDuplicateScore]()

  private var pictures = Map[(Board.Id, Team.Id), DuplicatePicture]()

  def getId() = monitoredId
  def getMatch(): Option[MatchDuplicate] = {
    logger.fine(s"getMatch returning bridgeMatch=$bridgeMatch")
    bridgeMatch
  }

  def getBoardsFromRound(table: Table.Id, round: Int): List[DuplicateHandV2] = {
    bridgeMatch match {
      case Some(md) => md.getHandsInRound(table, round)
      case None     => Nil
    }
  }

  def getTablePerspectiveFromRound(
      table: Table.Id,
      round: Int
  ): Option[DuplicateViewPerspective] = {
    bridgeMatch match {
      case Some(md) =>
        import scala.util.control.Breaks._
        var result: Option[DuplicateViewPerspective] = None
        breakable {
          for (b <- md.boards) {
            for (h <- b.hands) {
              if (h.table == table && h.round == round) {
                result = Some(PerspectiveTable(h.nsTeam, h.ewTeam))
                break()
              }
            }
          }
        }
        result
      case None => None
    }
  }

  def getView(
      perspective: DuplicateViewPerspective
  ): Option[MatchDuplicateScore] =
    perspective match {
      case PerspectiveDirector      => getDirectorsView()
      case PerspectiveComplete      => getCompleteView()
      case PerspectiveTable(t1, t2) => getTeamsView(t1, t2)
    }

  def getDirectorsView(): Option[MatchDuplicateScore] =
    directorsView match {
      case Some(dv) => Some(dv)
      case None =>
        directorsView = bridgeMatch match {
          case Some(md) => Some(MatchDuplicateScore(md, PerspectiveDirector))
          case None     => None
        }
        directorsView
    }
  def getCompleteView(): Option[MatchDuplicateScore] =
    completeView match {
      case Some(dv) => Some(dv)
      case None =>
        completeView = bridgeMatch match {
          case Some(md) => Some(MatchDuplicateScore(md, PerspectiveComplete))
          case None     => None
        }
        completeView
    }
  def getTeamsView(
      team1: Team.Id,
      team2: Team.Id
  ): Option[MatchDuplicateScore] = {
    val teams = if (team1 > team2) (team2, team1) else (team1, team2)
    teamsView.get(teams) match {
      case Some(dv) => Some(dv)
      case None =>
        bridgeMatch match {
          case Some(md) =>
            val s =
              MatchDuplicateScore(md, PerspectiveTable(teams._1, teams._2))
            teamsView = teamsView + (teams -> s)
            Some(s)
          case None => None
        }
    }
  }

  def getPicture(
      dupid: MatchDuplicate.Id,
      boardId: Board.Id,
      handId: Team.Id
  ): Option[DuplicatePicture] = {
    monitoredId.filter(dup => dup == dupid).flatMap { dup =>
      pictures.get((boardId, handId))
    }
  }

  def getPicture(
      dupid: MatchDuplicate.Id,
      boardId: Board.Id
  ): List[DuplicatePicture] = {
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
        case m: DuplicateBridgeAction =>
          m match {
            case ActionStartDuplicateMatch(dupid) =>
              start(dupid)
            case ActionStop() =>
              stop()
              notifyChange()
            case ActionUpdateDuplicateMatch(duplicate) =>
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
                    if (BridgeDemo.isDemo) {
                      scalajs.js.timers.setTimeout(1) {
                        BridgeDispatcher.updateDuplicateSummaryDemoMatchItem(
                          None,
                          duplicate
                        )
                        BridgeDispatcher.updateDuplicateSummaryItem(
                          None,
                          DuplicateSummary.create(duplicate)
                        )
                      }
                    }
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

            case ActionUpdateDuplicateHand(dupid, hand) =>
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
                      if (BridgeDemo.isDemo) {
                        scalajs.js.timers.setTimeout(1) {
                          BridgeDispatcher.updateDuplicateSummaryDemoMatchItem(
                            None,
                            md
                          )
                          BridgeDispatcher.updateDuplicateSummaryItem(
                            None,
                            DuplicateSummary.create(md)
                          )
                        }
                      }
                    }
                  } else {
                    logger.severe(
                      "Duplicate IDs don't match, working with " + md.id + " got " + dupid
                    )
                  }
                case _ =>
                  logger.severe(
                    "Got a hand update and don't have a MatchDuplicate"
                  )
              }
            case ActionUpdateTeam(dupid, team) =>
              bridgeMatch match {
                case Some(md) =>
                  if (md.id == dupid) {
                    logger.info("Updating team in " + md.id + ": " + team)
                    (md.getTeam(team.id) match {
                      case Some(oldteam) =>
                        if (!oldteam.equalsIgnoreModifyTime(team)) {
                          bridgeMatch = Some(md.updateTeam(team))
                          logger.info(
                            s"Updated team duplicate match $dupid, bridgeMatch=$bridgeMatch"
                          )
                          resetViews()
                          notifyChange()
                          bridgeMatch
                        } else {
                          None
                        }
                      case None =>
                        bridgeMatch = Some(md.updateTeam(team))
                        logger.info(
                          s"Updated team duplicate match $dupid, bridgeMatch=$bridgeMatch"
                        )
                        resetViews()
                        notifyChange()
                        bridgeMatch
                    }).map { md =>
                      if (BridgeDemo.isDemo) {
                        scalajs.js.timers.setTimeout(1) {
                          BridgeDispatcher.updateDuplicateSummaryDemoMatchItem(
                            None,
                            md
                          )
                          BridgeDispatcher.updateDuplicateSummaryItem(
                            None,
                            DuplicateSummary.create(md)
                          )
                        }
                      }
                    }
                  } else {
                    logger.severe(
                      "Duplicate IDs don't match, working with " + md.id + " got " + dupid
                    )
                  }
                case _ =>
                  logger.severe(
                    "Got a team update and don't have a MatchDuplicate"
                  )
              }
            case ActionUpdatePicture(dupid, boardid, handid, picture) =>
              val key = (boardid, handid)
              logger.fine(s"Trying to update picture on ${monitoredId} with ${msg}")
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
            case ActionUpdatePictures(dupid, picts) =>
              logger.fine(s"Trying to update pictures on ${monitoredId} with ${msg}")
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

  def start(dupid: MatchDuplicate.Id): Unit = {
    monitoredId match {
      case Some(mid) if mid != dupid =>
        bridgeMatch = None
        stop()
      case _ =>
    }
    logger.info(s"Starting to monitor $dupid, bridgeMatch=$bridgeMatch")
    monitoredId = Some(dupid)
    if (BridgeDemo.isDemo) {
      if (bridgeMatch.isEmpty)
        bridgeMatch =
          DuplicateSummaryStore.getDuplicateMatchSummary.flatMap(list =>
            list.find(md => md.id == dupid)
          )
      logger.info(s"monitoring bridgeMatch=$bridgeMatch")
    }
    notifyChange()
  }

  private def resetViews() = {
    directorsView = None
    completeView = None
    teamsView = Map()
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
    lastRoundComplete = getCompleteView() match {
      case Some(cv) =>
        val last = cv.tables.values.map(rounds =>
          rounds.filter(r => !r.allUnplayedOnTable).lastOption
        )
        val lastcompleterounds = last.map(or =>
          or.filter(r => r.complete).map(r => r.round).getOrElse(-1)
        )
        val lcr = lastcompleterounds.headOption.getOrElse(-1)
        val lrc = if (lcr != -1) {
          lastcompleterounds.find(r => r != lcr).isEmpty
        } else {
          false
        }
        logger.fine(
          s"lrc=${lrc}, lastRoundComplete=${lastRoundComplete}, Last complete rounds on all tables is ${lastcompleterounds}, last=${last}"
        )
        if (lrc && !lastRoundComplete) BeepComponent.beep()
        lrc
      case None => false
    }
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
