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
import com.github.thebridsk.bridge.data.Id

object SSE {
  private val logger = Logger("bridge.SSE")

}

import SSE.logger

/**
 *
 * @tparam T the type of the identifier of the monitored object.
 *            The toString method must generate the string for the identifies
 *            used in a URL.
 * @constructor
 * @param urlprefix - the monitoring URL without the identifier
 */
class SSE[T <: Id[_]]( urlprefix: String, listener: SECListener[T] ) extends ServerEventConnection[T](listener) {

  val heartbeatTimeout = 20000   // ms  20s
  val restartTimeout = 10*60*1000   // ms 10m   // TODO find good timeout for restart
  val defaultErrorBackoff = 1000   // ms  1s
  val limitErrorBackoff = 60000 // ms  1m

  def isConnected = eventSource.isDefined

  private var currentESTimeout: Option[SetTimeoutHandle] = None

  private var currentESRestartTimeout: Option[SetTimeoutHandle] = None

  private def resetESConnection( dupid: T ) = {
    eventSource.map { es =>
      logger.fine(s"EventSource reseting connection to $dupid")
      monitor(dupid, true)
    }.getOrElse(
      logger.fine(s"EventSource no connection to reset")
    )
  }

  private def resetESTimeout( dupid: T ) = {
    eventSource.map { es =>
      clearESTimeout()

      logger.fine(s"EventSource setting heartbeat timeout to ${heartbeatTimeout} ms")
      import scala.scalajs.js.timers._
      currentESTimeout = Some( setTimeout(heartbeatTimeout) {
        logger.info(s"EventSource heartbeat timeout fired ${heartbeatTimeout} ms, reseting connection")
        resetESConnection(dupid)
      })
    }.getOrElse(
      logger.fine(s"EventSource no connection to reset")
    )
  }

  private def clearESTimeout() = {
    import scala.scalajs.js.timers._
    logger.fine(s"EventSource clearing timeout")
    currentESTimeout.foreach( clearTimeout(_))
    currentESTimeout = None
  }

  private def resetESRestartTimeout( dupid: T ) = {
    eventSource.map { es =>
      clearESRestartTimeout()

      logger.fine(s"EventSource restart timeout to ${restartTimeout} ms")
      import scala.scalajs.js.timers._
      currentESRestartTimeout = Some( setTimeout(restartTimeout) {
        logger.fine(s"EventSource restart timeout fired ${restartTimeout} ms, reseting connection")
        resetESConnection(dupid)
      })
    }.getOrElse(
      logger.fine(s"EventSource no connection to restart")
    )
  }

  private def clearESRestartTimeout() = {
    import scala.scalajs.js.timers._
    logger.fine(s"EventSource restart clear timeout")
    currentESRestartTimeout.foreach( clearTimeout(_))
    currentESRestartTimeout = None
  }

  private def esOnMessage( dupid: T )( me: MessageEvent ): Unit = {
    import com.github.thebridsk.bridge.data.websocket.DuplexProtocol
    try {
      logger.fine(s"esOnMessage received ${me.data}")
      resetESTimeout(dupid)
      me.data match {
        case s: String =>
          if (s.equals("")) {
            // ignore this, this is a heartbeat
            logger.fine(s"esOnMessage received heartbeat")
          } else {
            DuplexProtocol.fromString(s) match {
              case DuplexProtocol.Response(data,seq) =>
                listener.processMessage(data)
              case DuplexProtocol.Unsolicited(data) =>
                listener.processMessage(data)
              case x =>
                logger.severe(s"EventSource received unknown object: ${me.data}")
            }
          }
      }
    } catch {
      case x: Exception =>
        logger.severe(s"esOnMessage exception: $x", x)
    }
  }

  private def esOnOpen( dupid: T )( e: Event ): Unit = {
    errorBackoff = defaultErrorBackoff
  }

  private var errorBackoff = defaultErrorBackoff

  private def esOnError( dupid: T )( err: Event ): Unit = {
    logger.severe(s"EventSource error monitoring ${dupid}: $err")

    eventSource.foreach { es =>
      if (es.readyState == EventSource.CLOSED) {
        import scala.scalajs.js.timers._

        logger.severe(s"EventSource error while connecting to server monitoring ${dupid}, connection closed: $err")
        if (errorBackoff > defaultErrorBackoff) {
          Alerter.alert(s"EventSource error while connecting to server monitoring ${dupid}, trying to restart: $err")
        }

        setTimeout(errorBackoff) {
          logger.warning(s"EventSource error monitoring ${dupid} backoff timer $errorBackoff ms fired")
          if (errorBackoff < limitErrorBackoff) errorBackoff*=2
          eventSource.foreach { es =>
            monitor(dupid, true)
          }
        }
      } else if (es.readyState == EventSource.CONNECTING) {
        logger.warning(s"EventSource error while connecting to server monitoring ${dupid}, browser is trying to reconnect: $err")
      }
    }
  }

  private def getEventSource( dupid: T ): Option[EventSource] = {
    val es = new EventSource(s"${urlprefix}${dupid.id}")
    es.onopen = esOnOpen(dupid)
    es.onmessage = esOnMessage(dupid)
    es.onerror = esOnError(dupid)
    Some(es)
  }

  private var eventSource: Option[EventSource] = None

  def monitor( dupid: T, restart: Boolean = false ): Unit = {

    if (AjaxResult.isEnabled.getOrElse(false)) {
      monitoredId match {
        case Some(mdid) =>
          cancelStop()
          if (restart || mdid != dupid || eventSource.isEmpty) {
            logger.info(s"""Switching MatchDuplicate monitor to ${dupid} from ${mdid}""" )
            listener.handleStart(dupid)
            eventSource.foreach( es => es.close())
            monitoredId = Some(dupid)
            eventSource = getEventSource(dupid)
            resetESTimeout(dupid)
            resetESRestartTimeout(dupid)
          } else {
            // already monitoring id
            logger.info(s"""Already monitoring ${dupid}""" )
          }
        case None =>
          logger.info(s"""Starting MatchDuplicate monitor to ${dupid}""" )
          listener.handleStart(dupid)
          eventSource.foreach( es => es.close())
          monitoredId = Some(dupid)
          eventSource = getEventSource(dupid)
          resetESTimeout(dupid)
          resetESRestartTimeout(dupid)
      }
    } else {
      listener.handleStart(dupid)
    }

  }

  /**
   * Immediately stop monitoring a match
   */
  def stop() = {
    logger.fine(s"Controller.stop ${monitoredId}")
    monitoredId match {
      case Some(id) =>
        monitoredId = None
        clearESTimeout()
        clearESRestartTimeout()
        eventSource.foreach( es => es.close())
        eventSource = None
        clearESTimeout()
        clearESRestartTimeout()
        listener.handleStop(id)
      case None =>
    }
  }

  def getDuplexPipeServerEventConnection(): Option[DuplexPipeServerEventConnection[T]] = None

}
