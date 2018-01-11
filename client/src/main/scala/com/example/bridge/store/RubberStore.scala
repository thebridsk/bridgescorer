package com.example.bridge.store

import utils.logging.Logger
import flux.dispatcher.DispatchToken
import com.example.bridge.action.BridgeDispatcher
import com.example.data.Hand
import com.example.bridge.action.ActionUpdateChicago
import com.example.bridge.action.ActionUpdateRubberHand
import com.example.bridge.action.ActionUpdateRubberNames
import com.example.bridge.action.ActionUpdateRubber
import com.example.data.MatchRubber
import com.example.data.bridge.PlayerPosition
import com.example.data.RubberHand
import com.example.bridge.action.RubberBridgeAction
import com.example.logger.Alerter

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

  def start( id: String, rub: MatchRubber ) = {
    monitoredId = Some(id)
    rubber = Some(rub)
    notifyChange()
  }

  private def update(funName: String, rubid: String, fun: (MatchRubber)=>Unit, callback: Option[MatchRubber=>Unit]) = {
    monitoredId match {
      case Some(id) if (id == rubid) =>
        rubber match {
          case Some(rub) =>
            logger.info("RubberStore."+funName+": updating rubberstore id="+rub.id)
            fun(rub)
            callback.foreach( cb=>cb(rubber.get) )
            notifyChange()
          case None =>
            logger.warning("RubberStore."+funName+": did not have rubber, monitoredId is "+monitoredId)
        }
      case _ =>
        logger.warning("RubberStore."+funName+": expecting id "+monitoredId+", got "+rubid)
    }
  }

  def updateRubber( rub: MatchRubber, callback: Option[MatchRubber=>Unit] ) = {
    update("updateRubber", rub.id, (oldrub)=>{
      rubber = Some(rub)
    },callback)
  }

  def updateRubberNames( rubid: String, north: String, south: String, east: String, west: String, firstDealer: PlayerPosition, callback: Option[MatchRubber=>Unit] ) = {
    update("updateRubberNames", rubid, (rub)=>{
      rubber = Some( rub.setPlayers(north, south, east, west).setFirstDealer(firstDealer.pos) )
    },callback)
  }

  def updateRubberHand( rubid: String, handid: String, hand: RubberHand, callback: Option[MatchRubber=>Unit] ) = {
    update("updateRubberHand", rubid, (rub)=>{
      rubber = Some(rub.getHand(handid) match {
        case Some(h) => rub.modifyHand(hand)
        case None => rub.addHand(hand)
      })
    },callback)
  }

}
