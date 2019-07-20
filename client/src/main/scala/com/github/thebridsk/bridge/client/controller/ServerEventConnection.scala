package com.github.thebridsk.bridge.client.controller

import com.github.thebridsk.bridge.data.websocket.Protocol
import com.github.thebridsk.utilities.logging.Logger
import japgolly.scalajs.react.Callback
import com.github.thebridsk.bridge.client.routes.AppRouter
import japgolly.scalajs.react.CallbackTo
import scala.concurrent.ExecutionContext
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import scala.concurrent.Future
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxResult
import org.scalactic.source.Position
import com.github.thebridsk.bridge.clientcommon.rest2.WrapperXMLHttpRequest
import org.scalajs.dom.raw.EventSource
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.raw.Event
import scala.scalajs.js.timers.SetTimeoutHandle
import scala.concurrent.duration.Duration
import com.github.thebridsk.bridge.data.websocket.Protocol.ToBrowserMessage

object ServerEventConnection {
  private val logger = Logger("bridge.ServerEventConnection")

}

import ServerEventConnection.logger

trait SECListener[T] {

  /**
   * Called when the monitoring is started or restarted
   * @param id
   */
  def handleStart( id: T ): Unit
  /**
   * Called when the monitoring is stopped
   * @param id
   */
  def handleStop( id: T ): Unit

  /**
   * Called when data has been received
   * @param data
   */
  def processMessage( data: ToBrowserMessage ): Unit

}

abstract class ServerEventConnection[T]( val listener: SECListener[T] ) {

  /**
   * @return the monitored ID, None is returned if not monitoring anything.
   */
  def getMonitoredId = monitoredId

  def isConnected: Boolean

  protected var monitoredId: Option[T] = None

  def monitor( dupid: T, restart: Boolean = false ): Unit

  private var delayHandle: Option[SetTimeoutHandle] = None

  def cancelStop() = {
      import scala.scalajs.js.timers._

      logger.fine(s"CancelStop: Cancelling stop: ${monitoredId}")

      delayHandle.foreach( h => clearTimeout(h))
      delayHandle = None
  }

  /**
   * In 30 seconds stop monitoring a match
   */
  def delayStop() = {
      import scala.scalajs.js.timers._

      logger.fine(s"DelayStop: Requesting stop monitoring duplicate match on server in 30 seconds: ${monitoredId}")
      if (monitoredId.isDefined) {
        delayHandle.foreach( h => clearTimeout(h))     // cancel old timer if it exists
        delayHandle = Some( setTimeout(30000) { // note the absence of () =>
          delayHandle = None
          logger.fine(s"DelayStop: Stopping monitoring duplicate match on server: ${monitoredId}")
          stop()
        })
      }
  }

  /**
   * Immediately stop monitoring a match
   */
  def stop(): Unit

  def getDuplexPipeServerEventConnection(): Option[DuplexPipeServerEventConnection[T]]
}
