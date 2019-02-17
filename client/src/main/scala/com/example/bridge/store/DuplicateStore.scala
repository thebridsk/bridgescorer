package com.example.bridge.store

import com.example.bridge.action.ActionUpdateDuplicateHand
import com.example.bridge.action.BridgeDispatcher
import com.example.data.MatchDuplicate
import com.example.bridge.action.ActionUpdateDuplicateMatch
import utils.logging.Logger
import flux.dispatcher.DispatchToken
import com.example.bridge.action.ActionUpdateDuplicateMatch
import com.example.bridge.action.ActionStartDuplicateMatch
import com.example.bridge.action.ActionStop
import com.example.data.Id
import com.example.data.bridge.MatchDuplicateScore
import com.example.data.bridge.PerspectiveDirector
import com.example.data.bridge.PerspectiveComplete
import com.example.data.bridge.PerspectiveTable
import com.example.data.bridge.DuplicateViewPerspective
import com.example.data.bridge.PerspectiveTable
import com.example.data.Board
import com.example.data.DuplicateHand
import com.example.bridge.action.ActionUpdateTeam
import com.example.bridge.action.DuplicateBridgeAction
import com.example.logger.Alerter
import japgolly.scalajs.react.Callback
import com.example.skeleton.react.BeepComponent
import com.example.Bridge
import com.example.data.DuplicateSummary

object DuplicateStore extends ChangeListenable {
  val logger = Logger("bridge.DuplicateStore")

  /**
   * Required to instantiate the store.
   */
  def init() = {}

  private var monitoredId: Option[Id.MatchDuplicate] = None
  private var bridgeMatch: Option[MatchDuplicate] = None
  private var directorsView: Option[MatchDuplicateScore] = None
  private var completeView: Option[MatchDuplicateScore] = None
  private var teamsView = Map[(Id.Team,Id.Team),MatchDuplicateScore]()

  def getId() = monitoredId
  def getMatch() = bridgeMatch

  def getBoardsFromRound( table: String, round: Int ) = {
    bridgeMatch match {
      case Some(md) => md.getHandsInRound(table, round)
      case None => Nil
    }
  }

  def getTablePerspectiveFromRound( table: String, round: Int ): Option[DuplicateViewPerspective] = {
    bridgeMatch match {
      case Some(md) =>
        import scala.util.control.Breaks._
        var result: Option[DuplicateViewPerspective] = None
        breakable { for (b <- md.boards) {
          for (h <- b.hands) {
            if (h.table == table && h.round == round) {
              result = Some(PerspectiveTable(h.nsTeam,h.ewTeam))
              break;
            }
          }
        }}
        result
      case None => None
    }
  }

  def getView( perspective: DuplicateViewPerspective ) = perspective match {
    case PerspectiveDirector => getDirectorsView()
    case PerspectiveComplete => getCompleteView()
    case PerspectiveTable(t1,t2) => getTeamsView(t1, t2)
  }

  def getDirectorsView(): Option[MatchDuplicateScore] = directorsView match {
    case Some(dv) => Some(dv)
    case None =>
      directorsView = bridgeMatch match {
        case Some(md) => Some(MatchDuplicateScore(md,PerspectiveDirector))
        case None => None
      }
      directorsView
  }
  def getCompleteView(): Option[MatchDuplicateScore] = completeView match {
    case Some(dv) => Some(dv)
    case None =>
      completeView = bridgeMatch match {
        case Some(md) => Some(MatchDuplicateScore(md,PerspectiveComplete))
        case None => None
      }
      completeView
  }
  def getTeamsView(team1: Id.Team, team2: Id.Team): Option[MatchDuplicateScore] = {
    val teams = if (team1>team2) (team2,team1) else (team1,team2)
    teamsView.get(teams) match {
    case Some(dv) => Some(dv)
    case None =>
      bridgeMatch match {
        case Some(md) =>
          val s = MatchDuplicateScore(md,PerspectiveTable(teams._1,teams._2))
          teamsView = teamsView + (teams->s)
          Some(s)
        case None => None
      }
    }
  }

  private var dispatchToken: Option[DispatchToken] = Some(BridgeDispatcher.register(dispatch _))

  def dispatch( msg: Any ) = Alerter.tryitWithUnit { msg match {
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
                logger.info("Updating duplicate match "+mid)
                bridgeMatch = Some(duplicate)
                resetViews()
                notifyChange()
                if (Bridge.isDemo) {
                  scalajs.js.timers.setTimeout(1) {
                    BridgeDispatcher.updateDuplicateSummaryDemoMatchItem(None, duplicate)
                    BridgeDispatcher.updateDuplicateSummaryItem(None, DuplicateSummary.create(duplicate))
                  }
                }
              } else {
                logger.severe("Duplicate IDs don't match, working with "+mid+" got "+duplicate.id)
              }
            case _ =>
              logger.severe("Got an update when no longer monitoring: "+duplicate)
          }

        case ActionUpdateDuplicateHand(dupid, hand) =>
          bridgeMatch match {
            case Some(md) =>
              if (md.id == dupid) {
                logger.info("Updating hand in "+md.id+": "+hand)
                (md.getHand(hand.board, hand.id) match {
                  case Some(oldhand) =>
                    if (!oldhand.equalsIgnoreModifyTime(hand)) {
                      logger.fine("Updating hand in DuplicateStore: "+hand)
                      bridgeMatch = Some( md.updateHand(hand) )
                      resetViews()
                      notifyChange()
                      bridgeMatch
                    } else {
                      logger.fine("Not updating hand in DuplicateStore: "+hand+", oldhand: "+oldhand)
                      None
                    }
                  case None =>
                    bridgeMatch = Some( md.updateHand(hand) )
                    resetViews()
                    notifyChange()
                    bridgeMatch
                }).map {md =>
                  if (Bridge.isDemo) {
                    scalajs.js.timers.setTimeout(1) {
                      BridgeDispatcher.updateDuplicateSummaryDemoMatchItem(None, md)
                      BridgeDispatcher.updateDuplicateSummaryItem(None, DuplicateSummary.create(md))
                    }
                  }
                }
              } else {
                logger.severe("Duplicate IDs don't match, working with "+md.id+" got "+dupid)
              }
            case _ =>
              logger.severe("Got a hand update and don't have a MatchDuplicate")
          }
        case ActionUpdateTeam(dupid,team) =>
          bridgeMatch match {
            case Some(md) =>
              if (md.id == dupid) {
                logger.info("Updating team in "+md.id+": "+team)
                (md.getTeam(team.id) match {
                  case Some(oldteam) =>
                    if (!oldteam.equalsIgnoreModifyTime(team)) {
                      bridgeMatch = Some( md.updateTeam(team) )
                      resetViews()
                      notifyChange()
                      bridgeMatch
                    } else {
                      None
                    }
                  case None =>
                    bridgeMatch = Some( md.updateTeam(team) )
                    resetViews()
                    notifyChange()
                    bridgeMatch
                }).map {md =>
                  if (Bridge.isDemo) {
                    scalajs.js.timers.setTimeout(1) {
                      BridgeDispatcher.updateDuplicateSummaryDemoMatchItem(None, md)
                      BridgeDispatcher.updateDuplicateSummaryItem(None, DuplicateSummary.create(md))
                    }
                  }
                }
              } else {
                logger.severe("Duplicate IDs don't match, working with "+md.id+" got "+dupid)
              }
            case _ =>
              logger.severe("Got a team update and don't have a MatchDuplicate")
          }
        case x =>
          // There are multiple stores, all the actions get sent to all stores
//          logger.fine("Ignoring unknown action: "+action)
      }
    case action =>
      // There are multiple stores, all the actions get sent to all stores
//      logger.fine("Ignoring unknown action: "+action)
  }}

  def start( dupid: Id.MatchDuplicate ) = {
    monitoredId match {
      case Some(mid) if mid!=dupid =>
        bridgeMatch = None
        stop()
      case _ =>
    }
    logger.info("Starting to monitor "+dupid)
    monitoredId = Some(dupid)
    if (Bridge.isDemo) {
      bridgeMatch = DuplicateSummaryStore.getDuplicateMatchSummary().flatMap( list => list.find(md => md.id == dupid))
    }
    notifyChange()
  }

  private def resetViews() = {
    directorsView = None
    completeView = None
    teamsView = Map()
  }

  def stop() = {
    if (Bridge.isDemo) {
      // keep the match
    } else {
      monitoredId match {
        case Some(id) =>
          logger.info("Stopping monitor "+id)
          monitoredId = None
          bridgeMatch = None
          resetViews()
  //        removeAllListener(ChangeListenable.event)
          notifyChange()
        case _ =>
          logger.info("Stopping monitor")
          bridgeMatch = None
      }
    }
  }

  private var lastRoundComplete: Boolean = false

  val monitorRoundEnd = Callback {
    lastRoundComplete = getCompleteView() match {
      case Some(cv) =>
        val last = cv.tables.values.map( rounds => rounds.filter(r => !r.allUnplayedOnTable).lastOption)
        val lastcompleterounds = last.map( or => or.filter(r=>r.complete).map(r=>r.round).getOrElse(-1))
        val lcr = lastcompleterounds.headOption.getOrElse(-1)
        val lrc = if (lcr != -1) {
          lastcompleterounds.find( r => r!=lcr).isEmpty
        } else {
          false
        }
        logger.fine(s"lrc=${lrc}, lastRoundComplete=${lastRoundComplete}, Last complete rounds on all tables is ${lastcompleterounds}, last=${last}")
        if (lrc && !lastRoundComplete) BeepComponent.beep()
        lrc
      case None => false
    }
  }

  def startMonitorRoundEnd() = {
    addChangeListener(monitorRoundEnd)
  }

  def stopMonitorRoundEnd() = {
    removeChangeListener(monitorRoundEnd)
  }

  startMonitorRoundEnd()

  override
  def addChangeListener( cb: Callback ) = {
    super.addChangeListener(cb)
    if (Bridge.isDemo) {
      scalajs.js.timers.setTimeout(1) {
        notifyChange()
      }
    }
  }

}
