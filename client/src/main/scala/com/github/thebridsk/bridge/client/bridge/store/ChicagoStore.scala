package com.github.thebridsk.bridge.client.bridge.store

import flux.dispatcher.DispatchToken
import com.github.thebridsk.bridge.client.bridge.action.BridgeDispatcher
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateChicagoHand
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateChicagoNames
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateChicagoRound
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.Round
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateChicago
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateChicago5
import com.github.thebridsk.bridge.client.bridge.action.ChicagoBridgeAction
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo

object ChicagoStore extends ChangeListenable {
  val logger: Logger = Logger("bridge.ChicagoStore")

  /**
   * Required to instantiate the store.
   */
  def init(): Unit = {}

  private var dispatchToken: Option[DispatchToken] = Some(BridgeDispatcher.register(dispatch _))

  def dispatch( msg: Any ): Unit = Alerter.tryitWithUnit { msg match {
    case m: ChicagoBridgeAction =>
      m match {
        case ActionUpdateChicagoHand(chiid,roundid,handid,hand,cb) => updateChicagoHand(chiid, roundid, handid, hand,cb)
        case ActionUpdateChicagoNames(chiid,nplayer1,nplayer2,nplayer3,nplayer4,extra,quintet,simple,cb) => updateChicagoNames(chiid, nplayer1, nplayer2, nplayer3, nplayer4,extra,quintet,simple,cb)
        case ActionUpdateChicago5(chiid,extraPlayer,cb) => updateChicago5(chiid, extraPlayer, cb)
        case ActionUpdateChicagoRound(chiid,round,cb) => updateChicagoRound(chiid, round,cb)
        case ActionUpdateChicago(chi,cb) => updateChicago(chi, cb)
        case _ =>
      }
    case x =>
      // There are multiple stores, all the actions get sent to all stores
//      logger.warning("BoardSetStore: Unknown msg dispatched, "+x)
  }}

  private var chicago: Option[MatchChicago] = None
  private var monitoredId: Option[MatchChicago.Id] = None

  def getChicago = chicago
  def getMonitoredId = monitoredId

  def isMonitoredId( chiid: MatchChicago.Id ): Boolean = monitoredId match {
    case Some(id) => id == chiid
    case None => false
  }

  def start( id: MatchChicago.Id, chi: Option[MatchChicago] ): Unit = {
    monitoredId = Some(id)
    chicago = chi
    notifyChange()
  }

  private def update(funName: String, chiid: MatchChicago.Id, fun: (Option[MatchChicago])=>Option[MatchChicago], callback: Option[MatchChicago=>Unit]) = {
    monitoredId match {
      case Some(id) if (id == chiid) =>
        logger.info("ChicagoStore."+funName+": updating chicagostore id="+id)
        chicago = fun(chicago)
        chicago match {
          case Some(chi) =>
            callback.foreach( cb=>cb(chicago.get) )
            notifyChange()
            if (BridgeDemo.isDemo) {
              scalajs.js.timers.setTimeout(1) {
                ChicagoSummaryStore.dispatch(ActionUpdateChicago(chi))
              }
            }
          case None =>
            logger.warning("ChicagoStore."+funName+": did not have chicago, monitoredId is "+monitoredId)
        }
      case _ =>
        logger.warning("ChicagoStore."+funName+": expecting id "+monitoredId+", got "+chiid)
    }
  }

  def updateChicago( chi: MatchChicago, callback: Option[MatchChicago=>Unit] ): Any = {
    update("updateChicago", chi.id, (oldchi)=>{
      Some(chi)
    },callback)
  }

  def updateChicagoNames( chiid: MatchChicago.Id, nplayer1: String, nplayer2: String, nplayer3: String, nplayer4: String, extra: Option[String], quintet: Boolean, simpleRotation: Boolean, callback: Option[MatchChicago=>Unit] ): Any = {
    update("updateChicagoNames", chiid, (chi)=>{
      chi match {
        case Some(mc) =>
          extra match {
            case Some(e) =>
              val nchi = if (mc.isConvertableToChicago5) {
                mc.setPlayers(nplayer1, nplayer2, nplayer3, nplayer4).playChicago5(e)
              } else if (mc.players.size == 5) {
                mc.setPlayers(nplayer1, nplayer2, nplayer3, nplayer4,e)
              } else {
                // not valid
                logger.severe("Not valid to set 5 names on chicago match "+mc.id)
                mc
              }
              Some(if (quintet) {
                if (nchi.gamesPerRound == 0 && nchi.rounds.isEmpty) {
                  nchi.setQuintet(simpleRotation)
                } else {
                  logger.severe("Setting Quintet is not valid if gamesPerRound is not 0 or rounds is not empty")
                  nchi
                }
              } else {
                nchi
              })
            case _ => Some(mc.setPlayers(nplayer1, nplayer2, nplayer3, nplayer4))
          }
        case None =>
          None
      }
    },callback)
  }

  def updateChicago5( chiid: MatchChicago.Id, extraPlayer: String, callback: Option[MatchChicago=>Unit] ): Any = {
    update("updateChicagoNames", chiid, (chi)=>{
      chi.map(_.playChicago5(extraPlayer))
    },callback)
  }

  def updateChicagoRound( chiid: MatchChicago.Id, round: Round, callback: Option[MatchChicago=>Unit] ): Any = {
    update("updateChicagoRound", chiid, (chi)=>{
      chi.map(_.modifyRound(round))
    },callback)
  }

  def updateChicagoHand( chiid: MatchChicago.Id, roundid: Int, handid: Int, hand: Hand, callback: Option[MatchChicago=>Unit] ): Any = {
    update("updateChicagoHand", chiid, (chi)=>{
      chi.map(_.modifyHand(roundid, handid, hand))
    },callback)
  }

}
