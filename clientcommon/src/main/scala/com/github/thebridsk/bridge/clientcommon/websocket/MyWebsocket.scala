package com.github.thebridsk.bridge.clientcommon.websocket

import org.scalajs.dom.raw._
import scala.scalajs.js
import org.scalajs.dom
import com.github.thebridsk.bridge.data.websocket.Protocol
import scala.scalajs.js.Array
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.logger.CommAlerter

class WebsocketClosed extends Exception

object MyWebsocket extends MyWebsocketCodes {
  val log = Logger("comm.MyWebsocket")

}

abstract class MyWebsocket(url: String, protocol: String = "") {
  import MyWebsocket._

  def onOpen(): Unit = {}
  def onError(filename: String, line: Int, col: Int, message: String): Unit = {}
  def onMessage(msg: String): Unit = {}
  def onClose(wasClean: Boolean, code: Int, reason: String): Unit = {}

  /**
   * The current state of the connection; this is one of the Ready state constants. Read
   * only.
   *
   * MDN
   */
  def readyState: Int = websocket match {
    case Some(ws) => ws.readyState
    case None => WebSocket.CLOSED
  }

  /**
   * The number of bytes of data that have been queued using calls to send() but not yet
   * transmitted to the network. This value does not reset to zero when the connection is
   * closed; if you keep calling send(), this will continue to climb. Read only.
   *
   * MDN
   */
  def bufferedAmount: Int = websocket match {
    case Some(ws) => ws.bufferedAmount
    case None => throw new WebsocketClosed
  }

  /**
   * The extensions selected by the server. This is currently only the empty string or a
   * list of extensions as negotiated by the connection.
   *
   * MDN
   */
  def extensions: String = websocket match {
    case Some(ws) => ws.extensions
    case None => throw new WebsocketClosed
  }

  /**
   * Send the data on the websocket
   * @param data
   * @throws WebsocketClosed if the websocket is closed
   */
  def send( data: String ): Unit = websocket match {
    case Some(ws) => ws.send(data)
    case None => throw new WebsocketClosed
  }

  def isClosed() = websocket.isEmpty

  private var closeCalled = false

  def wasCloseCalled = closeCalled

  def close(code: Code, reason: String) = {
    closeCalled = true
    websocket match {
      case Some(ws) =>
        ws.close(code.code, reason)
        websocket = None
      case None =>
    }
  }

  def start( force: Boolean ) = {
    websocket match {
      case Some(ws) =>
        if (force) {
          websocket = None
          log.warning("Forcing new websocket for "+url)
          ws.onopen = { (event: Event) => {} }
          ws.onclose = {(event: CloseEvent) => {} }
          ws.onerror = {(event: Event) => {} }
          ws.onmessage = {(event: MessageEvent) => {} }
          ws.close(MyWebsocket.CLOSE_NORMAL, "starting another")
          init()
        } else {
          log.warning("Websocket for "+url+" is already running")
        }
      case None =>
        log.warning("Starting new websocket for "+url)
        init()
    }
  }

  private var websocket: Option[WebSocket] = None
  init()

  private def isCurrentWebsocket( ws: WebSocket ) = websocket.map(w => w==ws).getOrElse(false)

  private def init() = {
    import js.JSConverters._
//    val prots = protocol.toJSArray
    closeCalled = false
    val ws = new WebSocket(url, protocol)
    websocket = Some(ws)
    ws.onopen = { (event: Event) => CommAlerter.tryitWithUnit {
      if (isCurrentWebsocket(ws)) {
        try {
          log.fine("Mywebsocket.onopen on "+url)
          onOpen()
        } catch {
          case t: Throwable =>
            log.severe("MyWebsocket.init.onmessage: uncaught error "+t,t)
            t.printStackTrace()
        }
      }
    }}
    ws.onerror = { (event: Event) => CommAlerter.tryitWithUnit {
      // The event does not contain any useful information
      // see http://stackoverflow.com/questions/18803971/websocket-onerror-how-to-read-error-description
      if (isCurrentWebsocket(ws)) {
        try {
//          log.severe("MyWebsocket "+event.filename+"("+event.lineno+","+event.colno+"): "+event.message)
//          onError(event.filename, event.lineno, event.colno, event.message)
          log.severe("MyWebsocket.onerror on "+url)
          onError("",0,0,"")
        } catch {
          case t: Throwable =>
            log.severe("MyWebsocket.init.onerror: uncaught error "+t,t)
            t.printStackTrace()
        }
      }
    }}
    ws.onmessage = { (event: MessageEvent) => CommAlerter.tryitWithUnit {
      if (isCurrentWebsocket(ws)) {
        try {
          onMessage(event.data.toString())
        } catch {
          case t: Throwable =>
            log.severe("MyWebsocket.init.onmessage: uncaught error "+t,t)
            t.printStackTrace()
        }
      }
    }}
    ws.onclose = { (event: CloseEvent) => CommAlerter.tryitWithUnit {
      if (isCurrentWebsocket(ws)) {
        try {
          var r = "<undefined>"
          try {
            r = event.reason
          } catch {
            case t: Throwable =>
              log.warning("onClose reason was undefined: "+t.toString())
          }
          if (event.code == CLOSE_NORMAL) {
            log.fine("Mywebsocket.onclose("+event.wasClean+","+ event.code+","+ r+") on "+url)
          } else {
            log.warning("Mywebsocket.onclose("+event.wasClean+","+ event.code+","+ r+") on "+url)
          }
          onClose(event.wasClean, event.code, r)
        } catch {
          case t: Throwable =>
            log.severe("MyWebsocket.init.onmessage: uncaught error "+t,t)
            t.printStackTrace()
        }
      }
    }}
  }
}

case class Code( val code: Int ) {
  import MyWebsocket._
  if (!((code >= CLOSE_NORMAL && code <= TLS_Handshake) ||
        (code >= 3000 && code <=3999) ||
        (code >= 4000 && code <=4999)
        )) {
    throw new IllegalArgumentException("Code is not valid: "+code)
  }
}

trait MyWebsocketCodes {

  def getMsgFromCode( code: Code ) = reasons.getOrElse(code.code, "Reason code "+code.code)

  val CLOSE_NORMAL = 1000
  val CLOSE_GOING_AWAY = 1001
  val CLOSE_PROTOCOL_ERROR = 1002
  val CLOSE_UNSUPPORTED = 1003
  val Reserved = 1004
  val CLOSE_NO_STATUS = 1005
  val CLOSE_ABNORMAL = 1006
  val Unsupported_Data = 1007
  val Policy_Violation = 1008
  val CLOSE_TOO_LARGE = 1009
  val Missing_Extension = 1010
  val Internal_Error = 1011
  val Service_Restart = 1012
  val Try_Again_Later = 1013
  val Reserved2 = 1014
  val TLS_Handshake = 1015

  private val reasons = Map(
    1000 -> "Normal closure; the connection successfully completed whatever purpose for which it was created.",
    1001 -> "The endpoint is going away, either because of a server failure or because the browser is navigating away from the page that opened the connection.",
    1002 -> "The endpoint is terminating the connection due to a protocol error.",
    1003 -> "The connection is being terminated because the endpoint received data of a type it cannot accept (for example, a text-only endpoint received binary data).",
    1004 -> "A meaning might be defined in the future.",
    1005 -> "Reserved.  Indicates that no status code was provided even though one was expected.",
    1006 -> "Reserved. Used to indicate that a connection was closed abnormally (that is, with no close frame being sent) when a status code is expected.",
    1007 -> "The endpoint is terminating the connection because a message was received that contained inconsistent data (e.g., non-UTF-8 data within a text message).",
    1008 -> "The endpoint is terminating the connection because it received a message that violates its policy. This is a generic status code, used when codes 1003 and 1009 are not suitable.",
    1009 -> "The endpoint is terminating the connection because a data frame was received that is too large.",
    1010 -> "The client is terminating the connection because it expected the server to negotiate one or more extension, but the server didn't.",
    1011 -> "The server is terminating the connection because it encountered an unexpected condition that prevented it from fulfilling the request.",
    1012 -> "The server is terminating the connection because it is restarting. [Ref]",
    1013 -> "The server is terminating the connection due to a temporary condition, e.g. it is overloaded and is casting off some of its clients. [Ref]",
    1014 -> "for future use by the WebSocket standard.",
    1015 -> "Reserved. Indicates that the connection was closed due to a failure to perform a TLS handshake (e.g., the server certificate can't be verified).")
}

