package com.example.bridge.store

import flux.dispatcher.DispatchToken
import com.example.bridge.action.BridgeDispatcher
import com.example.bridge.action.ActionUpdateChicagoHand
import com.example.bridge.action.ActionUpdateChicagoNames
import com.example.bridge.action.ActionUpdateChicagoRound
import com.example.data.MatchChicago
import com.example.data.Round
import com.example.data.Hand
import com.example.bridge.action.ActionUpdateChicago
import com.example.bridge.action.ActionUpdateChicago5
import com.example.bridge.action.ChicagoBridgeAction
import utils.logging.Logger
import com.example.logger.Alerter

object ChicagoStore extends ChangeListenable {
  val logger = Logger("bridge.ChicagoStore")

  /**
   * Required to instantiate the store.
   */
  def init() = {}

  private var dispatchToken: Option[DispatchToken] = Some(BridgeDispatcher.register(dispatch _))

  def dispatch( msg: Any ) = Alerter.tryitWithUnit { msg match {
    case m: ChicagoBridgeAction =>
      m match {
        case ActionUpdateChicagoHand(chiid,roundid,handid,hand,cb) => updateChicagoHand(chiid, roundid, handid, hand,cb)
        case ActionUpdateChicagoNames(chiid,nplayer1,nplayer2,nplayer3,nplayer4,extra,quintet,simple,cb) => updateChicagoNames(chiid, nplayer1, nplayer2, nplayer3, nplayer4,extra,quintet,simple,cb)
        case ActionUpdateChicago5(chiid,extraPlayer,cb) => updateChicago5(chiid, extraPlayer, cb)
        case ActionUpdateChicagoRound(chiid,round,cb) => updateChicagoRound(chiid, round,cb)
        case ActionUpdateChicago(chi,cb) => updateChicago(chi, cb)
      }
    case x =>
      // There are multiple stores, all the actions get sent to all stores
//      logger.warning("BoardSetStore: Unknown msg dispatched, "+x)
  }}

  private var chicago: Option[MatchChicago] = None
  private var monitoredId: Option[String] = None

  def getChicago = chicago
  def getMonitoredId = monitoredId

  def isMonitoredId( chiid: String ) = monitoredId match {
    case Some(id) => id == chiid
    case None => false
  }

  def start( id: String, chi: MatchChicago ) = {
    monitoredId = Some(id)
    chicago = Some(chi)
    notifyChange()
  }

  private def update(funName: String, chiid: String, fun: (MatchChicago)=>Unit, callback: Option[MatchChicago=>Unit]) = {
    monitoredId match {
      case Some(id) if (id == chiid) =>
        chicago match {
          case Some(chi) =>
            logger.info("ChicagoStore."+funName+": updating chicagostore id="+chi.id)
            fun(chi)
            callback.foreach( cb=>cb(chicago.get) )
            notifyChange()
          case None =>
            logger.warning("ChicagoStore."+funName+": did not have chicago, monitoredId is "+monitoredId)
        }
      case _ =>
        logger.warning("ChicagoStore."+funName+": expecting id "+monitoredId+", got "+chiid)
    }
  }

  def updateChicago( chi: MatchChicago, callback: Option[MatchChicago=>Unit] ) = {
    update("updateChicago", chi.id, (oldchi)=>{
      chicago = Some(chi)
    },callback)
  }

  def updateChicagoNames( chiid: String, nplayer1: String, nplayer2: String, nplayer3: String, nplayer4: String, extra: Option[String], quintet: Boolean, simpleRotation: Boolean, callback: Option[MatchChicago=>Unit] ) = {
    update("updateChicagoNames", chiid, (chi)=>{
      chicago = Some( extra match {
        case Some(e) =>
          val nchi = if (chi.isConvertableToChicago5) {
            chi.setPlayers(nplayer1, nplayer2, nplayer3, nplayer4).playChicago5(e)
          } else if (chi.players.size == 5) {
            chi.setPlayers(nplayer1, nplayer2, nplayer3, nplayer4,e)
          } else {
            // not valid
            logger.severe("Not valid to set 5 names on chicago match "+chi.id)
            chi
          }
          if (quintet) {
            if (nchi.gamesPerRound == 0 && nchi.rounds.isEmpty) {
              nchi.setQuintet(simpleRotation)
            } else {
              logger.severe("Setting Quintet is not valid if gamesPerRound is not 0 or rounds is not empty")
              nchi
            }
          } else {
            nchi
          }
        case _ => chi.setPlayers(nplayer1, nplayer2, nplayer3, nplayer4)
      })
    },callback)
  }

  def updateChicago5( chiid: String, extraPlayer: String, callback: Option[MatchChicago=>Unit] ) = {
    update("updateChicagoNames", chiid, (chi)=>{
      chicago = Some( chi.playChicago5(extraPlayer) )
    },callback)
  }

  def updateChicagoRound( chiid: String, round: Round, callback: Option[MatchChicago=>Unit] ) = {
    update("updateChicagoRound", chiid, (chi)=>{
      chicago = Some( chi.modifyRound(round) )
    },callback)
  }

  def updateChicagoHand( chiid: String, roundid: Int, handid: Int, hand: Hand, callback: Option[MatchChicago=>Unit] ) = {
    update("updateChicagoHand", chiid, (chi)=>{
      chicago = Some( chi.modifyHand(roundid, handid, hand) )
    },callback)
  }

}
