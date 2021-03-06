package com.github.thebridsk.bridge.client.bridge.store

import com.github.thebridsk.utilities.logging.Logger
import flux.dispatcher.DispatchToken
import com.github.thebridsk.bridge.client.bridge.action.BridgeDispatcher
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateRubberHand
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateRubberNames
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateRubber
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.data.RubberHand
import com.github.thebridsk.bridge.client.bridge.action.RubberBridgeAction
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo

object RubberStore extends ChangeListenable {
  val logger: Logger = Logger("bridge.RubberStore")

  /**
    * Required to instantiate the store.
    */
  def init(): Unit = {}

  private var dispatchToken: Option[DispatchToken] = Some(
    BridgeDispatcher.register(dispatch _)
  )

  def dispatch(msg: Any): Unit =
    Alerter.tryitWithUnit {
      msg match {
        case m: RubberBridgeAction =>
          m match {
            case ActionUpdateRubberHand(rubid, handid, hand, cb) =>
              updateRubberHand(rubid, handid, hand, cb)
            case ActionUpdateRubberNames(
                  rubid,
                  north,
                  south,
                  east,
                  west,
                  firstdealer,
                  cb
                ) =>
              updateRubberNames(
                rubid,
                north,
                south,
                east,
                west,
                firstdealer,
                cb
              )
            case ActionUpdateRubber(rub, cb) => updateRubber(rub, cb)
            case _                           =>
          }
        case x =>
        // There are multiple stores, all the actions get sent to all stores
//      logger.warning("BoardSetStore: Unknown msg dispatched, "+x)
      }
    }

  private var rubber: Option[MatchRubber] = None
  private var monitoredId: Option[MatchRubber.Id] = None

  def getRubber = rubber
  def getMonitoredId = monitoredId

  def isMonitoredId(rubid: MatchRubber.Id): Boolean =
    monitoredId match {
      case Some(id) => id == rubid
      case None     => false
    }

  def start(id: MatchRubber.Id, rub: Option[MatchRubber]): Unit = {
    monitoredId = Some(id)
    rubber = rub
    notifyChange()
  }

  private def update(
      funName: String,
      rubid: MatchRubber.Id,
      fun: (Option[MatchRubber]) => Option[MatchRubber],
      callback: Option[MatchRubber => Unit]
  ) = {
    monitoredId match {
      case Some(id) if (id == rubid) =>
        rubber = fun(rubber)
        rubber match {
          case Some(rub) =>
            logger.info(
              "RubberStore." + funName + ": updating rubberstore id=" + rub.id
            )
            callback.foreach(cb => cb(rubber.get))
            notifyChange()
            if (BridgeDemo.isDemo) {
              scalajs.js.timers.setTimeout(1) {
                RubberListStore.dispatch(ActionUpdateRubber(rubber.get))
              }
            }
          case None =>
            logger.warning(
              "RubberStore." + funName + ": did not have rubber, monitoredId is " + monitoredId
            )
        }
      case _ =>
        logger.warning(
          "RubberStore." + funName + ": expecting id " + monitoredId + ", got " + rubid
        )
    }
  }

  def updateRubber(
      rub: MatchRubber,
      callback: Option[MatchRubber => Unit]
  ): Any = {
    update(
      "updateRubber",
      rub.id,
      (oldrub) => {
        Some(rub)
      },
      callback
    )
  }

  def updateRubberNames(
      rubid: MatchRubber.Id,
      north: String,
      south: String,
      east: String,
      west: String,
      firstDealer: PlayerPosition,
      callback: Option[MatchRubber => Unit]
  ): Any = {
    update(
      "updateRubberNames",
      rubid,
      (rub) => {
        rub.map(
          _.setPlayers(north, south, east, west).setFirstDealer(firstDealer.pos)
        )
      },
      callback
    )
  }

  def updateRubberHand(
      rubid: MatchRubber.Id,
      handid: String,
      hand: RubberHand,
      callback: Option[MatchRubber => Unit]
  ): Any = {
    update(
      "updateRubberHand",
      rubid,
      (rub) => {
        rub match {
          case Some(mr) =>
            Some(mr.getHand(handid) match {
              case Some(h) => mr.modifyHand(hand)
              case None    => mr.addHand(hand)
            })
          case None =>
            None
        }
      },
      callback
    )
  }

}
