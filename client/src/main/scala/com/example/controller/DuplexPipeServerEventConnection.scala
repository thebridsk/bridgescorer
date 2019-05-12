package com.example.controller

import com.example.data.websocket.Protocol
import utils.logging.Logger
import japgolly.scalajs.react.Callback
import com.example.routes.AppRouter
import japgolly.scalajs.react.CallbackTo
import scala.concurrent.ExecutionContext
import com.example.logger.Alerter
import scala.concurrent.Future
import com.example.rest2.AjaxResult
import org.scalactic.source.Position
import com.example.rest2.WrapperXMLHttpRequest
import org.scalajs.dom.raw.EventSource
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.raw.Event
import scala.scalajs.js.timers.SetTimeoutHandle
import scala.concurrent.duration.Duration
import com.example.data.websocket.Protocol.ToBrowserMessage

object DuplexPipeServerEventConnection {
  private val logger = Logger("bridge.DuplexPipeServerEventConnection")

}

import DuplexPipeServerEventConnection.logger
import com.example.websocket.DuplexPipe
import com.example.data.websocket.Protocol.ToBrowserMessage
import com.example.data.websocket.Protocol.ToServerMessage
import com.example.bridge.action.BridgeDispatcher

/**
 *
 * @tparam T the type of the identifier of the monitored object.
 *            The toString method must generate the string for the identifies
 *            used in a URL.
 * @constructor
 * @param urlprefix - the monitoring URL without the identifier
 */
abstract class DuplexPipeServerEventConnection[T]( urlprefix: String, listener: SECListener[T] ) extends ServerEventConnection[T](listener) {

  def isConnected = duplexPipe.isDefined

  private var duplexPipe: Option[DuplexPipe] = None

  def getDuplexPipe() = duplexPipe match {
    case Some(d) => d
    case None =>
      val url = AppRouter.hostUrl.replaceFirst("http", "ws") + "/v1/ws/"
      val d = new DuplexPipe( url, Protocol.DuplicateBridge ) {
        override
        def onNormalClose() = {
          start(true)
        }
      }
      d.addListener(new DuplexPipe.Listener {
        def onMessage( msg: Protocol.ToBrowserMessage ) = {
          listener.processMessage(msg)
        }
      })
      duplexPipe = Some(d)
      d
  }

  def actionStartMonitor( mdid: T ): ToServerMessage
  def actionStopMonitor( mdid: T ): ToServerMessage

  def monitor( dupid: T, restart: Boolean = false ): Unit = {

    if (AjaxResult.isEnabled.getOrElse(false)) {
      monitoredId match {
        case Some(mdid) =>
          cancelStop()
          if (restart || mdid != dupid) {
            logger.info(s"""Switching MatchDuplicate monitor to ${dupid} from ${mdid}""" )
            listener.handleStart(dupid)
            getDuplexPipe().clearSession(actionStopMonitor(mdid))
            listener.handleStart(dupid)
            getDuplexPipe().setSession { dp =>
              logger.info(s"""In Session: Switching MatchDuplicate monitor to ${dupid} from ${mdid}""" )
              dp.send(actionStartMonitor(dupid))
            }
          } else {
            // already monitoring id
            logger.info(s"""Already monitoring ${dupid}""" )
          }
        case None =>
          logger.info(s"""Starting MatchDuplicate monitor to ${dupid}""" )
          listener.handleStart(dupid)
          listener.handleStart(dupid)
          getDuplexPipe().setSession { dp =>
            logger.info(s"""In Session: Starting MatchDuplicate monitor to ${dupid}""" )
            dp.send(actionStartMonitor(dupid))
          }
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
        duplexPipe match {
          case Some(d) =>
            monitoredId match {
              case Some(id) =>
                d.clearSession(actionStopMonitor(id))
              case None =>
            }
            BridgeDispatcher.stop()
          case _ =>

        }
        listener.handleStop(id)
      case None =>
    }
  }

  def send( msg: ToServerMessage ) = getDuplexPipe().send(msg)

  def getDuplexPipeServerEventConnection(): Option[DuplexPipeServerEventConnection[T]] = Some(this)

}
