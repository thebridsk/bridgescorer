package com.github.thebridsk.bridge.bridge.store

import com.github.thebridsk.utilities.logging.Logger
import flux.dispatcher.DispatchToken
import com.github.thebridsk.bridge.bridge.action.BridgeDispatcher
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.bridge.action.ActionUpdateChicago
import com.github.thebridsk.bridge.bridge.action.ActionUpdateRubberHand
import com.github.thebridsk.bridge.bridge.action.ActionUpdateRubberNames
import com.github.thebridsk.bridge.bridge.action.ActionUpdateRubber
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.data.RubberHand
import com.github.thebridsk.bridge.bridge.action.RubberBridgeAction
import com.github.thebridsk.bridge.logger.Alerter
import com.github.thebridsk.bridge.Bridge

object RubberStore extends ChangeListenable {
  val logger = Logger("bridge.RubberStore")

  /**
   * Required to instantiate the store.
   */
  def init() = {}

  private var dispatchToken: Option[DispatchToken] = Some(BridgeDispatcher.register(dispatch _))

  def dispatch( msg: Any ) = Alerter.tryitWithUnit { msg match {
    case m: RubberBridgeAction =>
      m match {
        case ActionUpdateRubberHand(rubid,handid,hand,cb) => updateRubberHand(rubid, handid, hand,cb)
        case ActionUpdateRubberNames(rubid,north, south, east, west,firstdealer,cb) => updateRubberNames(rubid, north, south, east, west,firstdealer,cb)
        case ActionUpdateRubber(rub,cb) => updateRubber(rub, cb)
        case _ =>
      }
    case x =>
      // There are multiple stores, all the actions get sent to all stores
//      logger.warning("BoardSetStore: Unknown msg dispatched, "+x)
  }}

  private var rubber: Option[MatchRubber] = None
  private var monitoredId: Option[String] = None

  def getRubber = rubber
  def getMonitoredId = monitoredId

  def isMonitoredId( rubid: String ) = monitoredId match {
    case Some(id) => id == rubid
    case None => false
  }

  def start( id: String, rub: Option[MatchRubber] ) = {
    monitoredId = Some(id)
    rubber = rub
    notifyChange()
  }

  private def update(funName: String, rubid: String, fun: (Option[MatchRubber])=>Option[MatchRubber], callback: Option[MatchRubber=>Unit]) = {
    monitoredId match {
      case Some(id) if (id == rubid) =>
        rubber = fun(rubber)
        rubber match {
          case Some(rub) =>
            logger.info("RubberStore."+funName+": updating rubberstore id="+rub.id)
            callback.foreach( cb=>cb(rubber.get) )
            notifyChange()
            if (Bridge.isDemo) {
              scalajs.js.timers.setTimeout(1) {
                RubberListStore.dispatch(ActionUpdateRubber(rubber.get))
              }
            }
          case None =>
            logger.warning("RubberStore."+funName+": did not have rubber, monitoredId is "+monitoredId)
        }
      case _ =>
        logger.warning("RubberStore."+funName+": expecting id "+monitoredId+", got "+rubid)
    }
  }

  def updateRubber( rub: MatchRubber, callback: Option[MatchRubber=>Unit] ) = {
    update("updateRubber", rub.id, (oldrub)=>{
      Some(rub)
    },callback)
  }

  def updateRubberNames( rubid: String, north: String, south: String, east: String, west: String, firstDealer: PlayerPosition, callback: Option[MatchRubber=>Unit] ) = {
    update("updateRubberNames", rubid, (rub)=>{
      rub.map(_.setPlayers(north, south, east, west).setFirstDealer(firstDealer.pos))
    },callback)
  }

  def updateRubberHand( rubid: String, handid: String, hand: RubberHand, callback: Option[MatchRubber=>Unit] ) = {
    update("updateRubberHand", rubid, (rub)=>{
      rub match {
        case Some(mr) =>
          Some( mr.getHand(handid) match {
            case Some(h) => mr.modifyHand(hand)
            case None => mr.addHand(hand)
          })
        case None =>
          None
      }
    },callback)
  }

}
