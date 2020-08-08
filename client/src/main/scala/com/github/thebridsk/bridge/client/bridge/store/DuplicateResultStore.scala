package com.github.thebridsk.bridge.client.bridge.store

import flux.dispatcher.DispatchToken
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateDuplicateResult
import com.github.thebridsk.bridge.client.bridge.action.BridgeDispatcher

object DuplicateResultStore extends ChangeListenable {
  val logger: Logger = Logger("bridge.DuplicateResultStore")

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
        case ActionUpdateDuplicateResult(dr) =>
          update(dr)
        case x =>
        // There are multiple stores, all the actions get sent to all stores
//      logger.warning("BoardSetStore: Unknown msg dispatched, "+x)
      }
    }

  private var monitoringId: Option[MatchDuplicateResult.Id] = None
  private var duplicateResult: Option[MatchDuplicateResult] = None

  /**
    * @param id the id to monitor.  None means not monitoring anything, Some(x) means monitoring x
    */
  def monitor(id: Option[MatchDuplicateResult.Id]): Unit = monitoringId = id

  def getDuplicateResult() = duplicateResult

  def update(dr: MatchDuplicateResult): Unit = {
    monitoringId match {
      case Some(id) =>
        if (id == dr.id) {
          duplicateResult = Option(dr)
          notifyChange()
        } else {
          logger.warning(
            s"Unexpected duplicate result, expecting ${id}, got ${dr}"
          )
        }
      case None =>
        logger.warning(
          s"Unexpected duplicate result, not monitoring any, got ${dr}"
        )
    }
  }

}
