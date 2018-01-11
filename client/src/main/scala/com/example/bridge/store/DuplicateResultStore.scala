package com.example.bridge.store

import flux.dispatcher.DispatchToken
import com.example.data.BoardSet
import com.example.data.Movement
import utils.logging.Logger
import com.example.data.MatchDuplicateResult
import com.example.logger.Alerter
import com.example.bridge.action.ActionUpdateDuplicateResult
import com.example.bridge.action.BridgeDispatcher
import com.example.data.Id

object DuplicateResultStore extends ChangeListenable {
  val logger = Logger("bridge.DuplicateResultStore")

  /**
   * Required to instantiate the store.
   */
  def init() = {}

  private var dispatchToken: Option[DispatchToken] = Some(BridgeDispatcher.register(dispatch _))

  def dispatch( msg: Any ) = Alerter.tryitWithUnit { msg match {
    case ActionUpdateDuplicateResult(dr) =>
      update(dr)
    case x =>
      // There are multiple stores, all the actions get sent to all stores
//      logger.warning("BoardSetStore: Unknown msg dispatched, "+x)
  }}

  private var monitoringId: Option[Id.MatchDuplicateResult] = None
  private var duplicateResult: Option[MatchDuplicateResult] = None

  /**
   * @param id the id to monitor.  None means not monitoring anything, Some(x) means monitoring x
   */
  def monitor( id: Option[Id.MatchDuplicateResult] ) = monitoringId = id

  def getDuplicateResult() = duplicateResult

  def update( dr: MatchDuplicateResult ) = {
    monitoringId match {
      case Some(id) =>
        if (id == dr.id) {
          duplicateResult = Option( dr )
          notifyChange()
        } else {
          logger.warning(s"Unexpected duplicate result, expecting ${id}, got ${dr}")
        }
      case None =>
        logger.warning(s"Unexpected duplicate result, not monitoring any, got ${dr}")
    }
  }

}
