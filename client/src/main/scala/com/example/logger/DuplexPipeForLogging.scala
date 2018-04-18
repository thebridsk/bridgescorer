package com.example.logger

import scala.collection.mutable.Queue

import org.scalajs.dom.raw.WebSocket

import com.example.data.SystemTime
import com.example.data.websocket.DuplexProtocol
import com.example.data.websocket.DuplexProtocol.LogEntryV2
import com.example.data.websocket.Protocol
import com.example.websocket.Code
import com.example.websocket.MyWebsocket

import utils.logging.Logger


object DuplexPipeForLogging {
  val log = Logger("comm.DuplexPipeForLogging")

  class TimeoutException extends Exception
  class ServerException( msg: String) extends Exception(msg)

  trait Listener {
    def onMessage( msg: DuplexProtocol.DuplexMessage ): Unit
  }

  type Response = (Either[Protocol.ToBrowserMessage,Exception] ) => Unit

  type Ack = ( Option[Exception] ) => Unit

  trait Converter {
    def toString( d: DuplexProtocol.DuplexMessage ): String
    def fromString( s: String ): DuplexProtocol.DuplexMessage
  }

  def ignore(ack: Option[Exception] ): Unit = {}

  import scala.language.implicitConversions
  implicit def ackToResponse( ack: Ack ) = (resp: Either[Protocol.ToBrowserMessage,Exception] ) => resp match {
    case Left(d) => ack(None)
    case Right(e) => ack(Some(e))
  }

  def apply( url: String, protocol: String = Protocol.Logging) = new DuplexPipeForLogging(url,protocol)

}

object DuplexProtocolConverter extends DuplexPipeForLogging.Converter {
  def toString( d: DuplexProtocol.DuplexMessage ): String = DuplexProtocol.toString(d)
  def fromString( s: String ): DuplexProtocol.DuplexMessage = DuplexProtocol.fromString(s)
}

import DuplexPipeForLogging._
import scala.scalajs.js.timers.SetTimeoutHandle

/**
 * A duplex pipe for managing a Websocket.
 */
class DuplexPipeForLogging( url: String, protocol: String = "" ) {
  val converter = DuplexProtocolConverter
  val maxBufferedAmount = 4096 // 10240
  private var listeners: List[Listener] = Nil

  /**
   * Add a listener for unsolicited messages from the server
   */
  def addListener( l: Listener ) = listeners ::= l

  /**
   * Remove a listener for unsolicited messages from the server
   */
  def removeListener( l: Listener ) = listeners.filter { listener => listener != l }

  def sendlog( data: LogEntryV2 ): Unit = {
    prepareForSending(data)
  }

  // implementation details

  private def notifyUnsolicited( data: DuplexProtocol.DuplexMessage ) = listeners.foreach { l => l.onMessage(data) }

  private class Pending(val data: DuplexProtocol.DuplexMessage, val seq: Option[Int], val resp: Option[Response], val timeoutSeconds: Int ) {
    var sent: Double = 0
    var endwait: Double = 0
  }

  private var pending: Queue[Pending] = Queue()

  private var outstanding = scala.collection.mutable.Map[Int, Pending]()

  private def prepareForSending( data: DuplexProtocol.DuplexMessage, seq: Option[Int] = None, resp: Option[Response] = None, timeoutSeconds: Int = 60 ) = {
    log.fine("PrepareForSending seq="+seq+" timeout="+timeoutSeconds+": "+data)
    val p = new Pending(data,seq,resp,timeoutSeconds)
    pending.enqueue(p)

    kick()
  }

  var handle: Option[SetTimeoutHandle] = None

  private def kick( delay: Double = 5 ): Unit = {
    import scala.scalajs.js.timers._
    handle match {
      case Some(h) => clearTimeout(h)
      case None =>
    }
    handle = Some(setTimeout(delay) { // note the absence of () =>
      CommAlerter.tryitWithUnit {
        kick2()
      }
    })
  }

  private def kick2(): Unit = {

    try {
      // no resending
      var bufferedAmount = websocket.bufferedAmount
      log.fine("Kick pending="+pending.size+", outstanding="+outstanding.size+" bufferedAmount="+bufferedAmount+"/"+maxBufferedAmount)

      var kickagain = 0.0
      val currentTime = SystemTime.currentTimeMillis()
      if (!pending.isEmpty) {
        if (websocket.readyState == WebSocket.OPEN) {
          log.fine("Kick trying to send pending, pending="+pending.size+", outstanding="+outstanding.size+" bufferedAmount="+bufferedAmount+"/"+maxBufferedAmount)
          if (!pending.isEmpty && bufferedAmount < maxBufferedAmount) {
            val head = pending.dequeue()
            head.seq match {
              case Some(seq) =>
                head.sent = currentTime
                head.endwait = head.sent + head.timeoutSeconds*1000d
                outstanding += (seq->head)
              case None =>
            }
            log.fine("Sending data: "+head.data)
            val l = websocket.send(head.data)
            bufferedAmount += l
            log.fine("Kick after send, pending="+pending.size+", outstanding="+outstanding.size+" bufferedAmount="+bufferedAmount+"/"+maxBufferedAmount)
            if (!pending.isEmpty) kickagain = 1.0
          } else {
            if (!pending.isEmpty) kickagain = 5000.0
          }
          log.fine("Kick done sending pending, pending="+pending.size+", outstanding="+outstanding.size+" bufferedAmount="+bufferedAmount+"("+websocket.bufferedAmount+")/"+maxBufferedAmount)
        } else {
          log.warning("Kick found websocket was not open: "+websocket.readyState)
        }
      }

      if (!outstanding.isEmpty) {
        log.fine("Checking for timeouts")
        var timedout: List[Response] = Nil
        outstanding = outstanding.filter { case (seq, pending) =>
          if (pending.endwait < currentTime) {
            pending.resp match {
              case Some(r) => timedout = r::timedout
              case None =>
            }
            false
          } else {
            true
          }
        }
        log.fine("timedout: "+timedout.mkString(", "))
        val to = Right(new TimeoutException())
        for (r <- timedout) r( to )
      }

      if (kickagain > 0) {
        // set a timer to kick again
        log.fine("Kicking again in 5 seconds")
        import scala.scalajs.js.timers._
        kick(kickagain)
      }
      log.fine("Kick return pending="+pending.size+", outstanding="+outstanding.size+" bufferedAmount="+bufferedAmount+"("+websocket.bufferedAmount+")/"+maxBufferedAmount)
    } catch {
      case t: Throwable =>
        log.severe("Kick got exception: "+t.getMessage,t)
        t.printStackTrace()
    }
  }

  private def fromWebsocket( msg: DuplexProtocol.DuplexMessage ) = {
    msg match {
      case DuplexProtocol.Response(data, seq ) =>
        outstanding.remove(seq) match {
          case Some(pending) =>
            pending.resp match {
              case Some(r) => r( Left(data) )
              case None =>
            }
          case None =>
        }
      case DuplexProtocol.ErrorResponse(data, seq ) =>
        outstanding.remove(seq) match {
          case Some(pending) =>
            pending.resp match {
              case Some(r) => r( Right(new ServerException(data)) )
              case None =>
            }
          case None =>
        }
        notifyUnsolicited(msg)
      case DuplexProtocol.Unsolicited(data) =>
      case DuplexProtocol.Send(data) =>  // ignore
      case DuplexProtocol.Request(data, seq, ack) => // ignore
      case DuplexProtocol.LogEntryS(json) => // ignore
      case DuplexProtocol.LogEntryV2( pos, logger, timestamp, level, url, message, cause, args) => // ignore
    }
    log.fine("fromWebsocket pending="+pending.size+", outstanding="+outstanding.size)

  }

  private var seqCounter = 0

  private def nextSeq() = {
    seqCounter = seqCounter + 1
    seqCounter
  }

  private class WS(url: String, protocol: String) extends MyWebsocket(url,protocol) {

    override final def onMessage( data: String): Unit = {
      log.fine("WS.onMessage: "+data)
      val msg = converter.fromString(data)
      fromWebsocket(msg)
    }

    override def onOpen(): Unit = { kick() }

    def send( msg: DuplexProtocol.DuplexMessage): Int = {
      val s = converter.toString(msg)
      send( s )
      s.length()
    }

    override def onClose(wasClean: Boolean, code: Int, reason: String): Unit = {
      log.info("WS.onClose: wasClean="+wasClean+", code="+code+", reason="+reason+", codestring="+MyWebsocket.getMsgFromCode(Code(code)))
      if (code != MyWebsocket.CLOSE_NORMAL) {
        log.info("Restarting websocket")
        start(true)
      }
    }
  }

  private val websocket = new WS(url,protocol)
}
