package com.github.thebridsk.bridge.client.bridge.store

import flux.dispatcher.DispatchToken
import com.github.thebridsk.bridge.client.bridge.action.BridgeDispatcher
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateChicagoSummary
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.client.bridge.action.ActionDeleteChicago
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateChicago
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo

object ChicagoSummaryStore extends ChangeListenable {
  val logger: Logger = Logger("bridge.ChicagoSummaryStore")

  /**
    * Required to instantiate the store.
    */
  def init(): Unit = {}

//  logger.warning("ChicagoSummaryStore initialized")

  private var dispatchToken: Option[DispatchToken] = Some(
    BridgeDispatcher.register(dispatch _)
  )

  def dispatch(msg: Any): Unit =
    Alerter.tryitWithUnit {
//    logger.info(s"Received $msg")
      msg match {
        case ActionUpdateChicagoSummary(importId, summary) =>
          updateChicagoSummary(importId, summary)
        case ActionDeleteChicago(id) =>
          if (fImportId.isEmpty) {
            fSummary = fSummary.map { a => a.filter(c => c.id != id) }
          }

        case ActionUpdateChicago(chi, cb) =>
          if (BridgeDemo.isDemo) {
            logger.info("Updating MatchChicago for demo")
            val n = fSummary
              .map { a =>
                val r = a.filter(c => chi.id != c.id)
                r :+ chi
              }
              .orElse(Some(Array(chi)))
            fSummary = n
            logger.info(s"""Chicago summary is now ${n.get.mkString("\n")}""")
            // does not call callback, that is for ChicagoStore
          }

        case x =>
        // There are multiple stores, all the actions get sent to all stores
        //      logger.warning("BoardSetStore: Unknown msg dispatched, "+x)
      }
    }

  private var fSummary: Option[Array[MatchChicago]] = None
  private var fImportId: Option[String] = None

  def getChicagoSummary() = fSummary
  def getImportId = fImportId

  def updateChicagoSummary(
      importId: Option[String],
      summary: Array[MatchChicago]
  ): Unit = {
    fSummary = Option(summary)
    fImportId = importId
    notifyChange()
  }

}
