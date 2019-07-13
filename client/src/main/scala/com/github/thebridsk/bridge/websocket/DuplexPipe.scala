package com.github.thebridsk.bridge.websocket

import com.github.thebridsk.bridge.data.websocket.Protocol
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol
import DuplexPipe._
import scala.collection.mutable.Queue
import org.scalajs.dom.document
import com.github.thebridsk.bridge.data.SystemTime
import com.github.thebridsk.utilities.logging.Logger
import org.scalajs.dom.raw.WebSocket
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.LogEntryS
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.LogEntryV2


object DuplexPipe {
  val log = Logger("comm.DuplexPipe")

  class TimeoutException extends Exception
  class ServerException( msg: String) extends Exception

  trait Listener {
    def onMessage( msg: Protocol.ToBrowserMessage ): Unit
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

  def apply( url: String, protocol: String = "") = new DuplexPipe(url,protocol)
}

object DuplexProtocolConverter extends DuplexPipe.Converter {
  def toString( d: DuplexProtocol.DuplexMessage ): String = DuplexProtocol.toString(d)
  def fromString( s: String ): DuplexProtocol.DuplexMessage = DuplexProtocol.fromString(s)
}

/**
 * A duplex pipe for managing a Websocket.
 */
class DuplexPipe( url: String,
                  protocol: String = "") {
  val converter = DuplexProtocolConverter
  val maxBufferedAmount = 10240
  private var listeners: List[Listener] = Nil

  type Session = DuplexPipe => Unit

  private var fSession: Option[Session] = None

  /**
   * Add a listener for unsolicited messages from the server
   */
  def addListener( l: Listener ) = listeners ::= l

  /**
   * Remove a listener for unsolicited messages from the server
   */
  def removeListener( l: Listener ) = listeners.filter { listener => listener != l }

  def setSession( session: Session ) = {
    fSession = Some(session)
    session(this)
  }

  def clearSession( stop: Protocol.ToServerMessage) = {
    fSession = None
    send(stop)
  }

  /**
   * Send a request to the server, and get the response
   * @param req the request data
   * @param response the function that is called with the response.
   * @param timeoutSeconds the max time to wait for a response
   */
  def request( req: Protocol.ToServerMessage, response: Response, timeoutSeconds: Int = 60 ): Unit = {
    val seq = nextSeq()
    prepareForSending( DuplexProtocol.Request( req, seq, false ), Some(seq), Some(response) )
  }

  /**
   * Send data to the server, and receive an ack.  The data will be tried again until
   * an ack is received.
   * @param data the data to send
   * @param ack the function to call when ack is received or timeout.
   * Use DuplexPipe.ignore method to ignore the ack and timeout.
   * @param timeoutSeconds the max time to wait for a response
   */
  def send( data: Protocol.ToServerMessage, ack: Ack, timeoutSeconds: Int = 60 ): Unit = {
    val seq = nextSeq()
    prepareForSending( DuplexProtocol.Request( data, seq, true ), Some(seq), Some(ack) )
  }

  /**
   * Send data to the server.  The data is sent only once hoping for the best.
   * @param data the data to send
   */
  def send( data: Protocol.ToServerMessage ): Unit = prepareForSending( DuplexProtocol.Send(data) )

  def sendlog( data: LogEntryS ): Unit = {
    prepareForSending(data)
  }

  def sendlog( data: LogEntryV2 ): Unit = {
    prepareForSending(data)
  }

  // implementation details

  private def notifyUnsolicited( data: Protocol.ToBrowserMessage ) = listeners.foreach { l => l.onMessage(data) }

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

  private def openKick(): Unit = {
    log.info("OpenKick")
    fSession.foreach( s => s(this))
    kick()
  }

  private def kick(): Unit = {
    try {
      // no resending
      log.fine("Kick pending="+pending.size+", outstanding="+outstanding.size+" bufferedAmount="+websocket.bufferedAmount+"/"+maxBufferedAmount)

      var kickagain = false
      val currentTime = SystemTime.currentTimeMillis()
      if (!pending.isEmpty) {
        if (websocket.readyState == WebSocket.OPEN) {
          log.fine("Kick trying to send pending, pending="+pending.size+", outstanding="+outstanding.size+" bufferedAmount="+websocket.bufferedAmount+"/"+maxBufferedAmount)
          while (!pending.isEmpty && websocket.bufferedAmount < maxBufferedAmount) {
            val head = pending.dequeue()
            head.seq match {
              case Some(seq) =>
                head.sent = currentTime
                head.endwait = head.sent + head.timeoutSeconds*1000d
                outstanding += (seq->head)
              case None =>
            }
            log.fine("Sending data: "+head.data)
            websocket.sendObj(head.data)
          }
          log.fine("Kick done sending pending, pending="+pending.size+", outstanding="+outstanding.size+" bufferedAmount="+websocket.bufferedAmount+"/"+maxBufferedAmount)
          if (!pending.isEmpty) kickagain = true
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

      if (kickagain) {
        // set a timer to kick again
        log.fine("Kicking again in 5 seconds")
        document.defaultView.setTimeout(()=>{ kick() }, 5000)
      }
      log.fine("Kick return pending="+pending.size+", outstanding="+outstanding.size+" bufferedAmount="+websocket.bufferedAmount+"/"+maxBufferedAmount)
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
      case DuplexProtocol.Unsolicited(data) =>
        notifyUnsolicited(data)
      case DuplexProtocol.Send(data) =>  // ignore
      case DuplexProtocol.Request(data, seq, ack) => // ignore
      case x: DuplexProtocol.LogEntryS => // ignore
      case x: DuplexProtocol.LogEntryV2 => // ignore
    }
    log.fine("fromWebsocket pending="+pending.size+", outstanding="+outstanding.size)

  }

  def onNormalClose() = {}

  def start( force: Boolean ) = websocket.start(force)

  def close(code: Code, reason: String) = websocket.close(code, reason)

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

    override def onOpen(): Unit = { openKick() }

    def sendObj( msg: DuplexProtocol.DuplexMessage): Unit = {
      send( converter.toString(msg) )
    }

    override def onClose(wasClean: Boolean, code: Int, reason: String): Unit = {
      log.info("WS.onClose: wasClean="+wasClean+", code="+code+", reason="+reason+", codestring="+MyWebsocket.getMsgFromCode(Code(code)))
      if (code != MyWebsocket.CLOSE_NORMAL) {
        log.info("Restarting websocket")
        start(true)
      } else {
        log.info("Normal close")
        onNormalClose()
      }
    }
  }

  private val websocket = new WS(url,protocol)
}
