package com.example.rest2

import scala.concurrent._
import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.typedarray._
import scala.scalajs.js.typedarray.TypedArrayBufferOps._
import org.scalajs.dom
import org.scalajs.dom.{html, raw}
import org.scalajs.dom.raw.Blob
import java.nio.ByteBuffer
import org.scalajs.dom.ext.AjaxException
import scala.scalajs.js.annotation.ScalaJSDefined
import org.scalajs.dom.raw.XMLHttpRequest
import com.example.data.RestMessage
import scala.concurrent.duration.Duration
import scala.concurrent.CanAwait
import scala.util.Try
import org.scalactic.source.Position
import utils.logging.Logger
import org.scalajs.dom.ext.Ajax.InputData
import scala.util.Success
import com.example.logger.Alerter
import com.example.source.SourcePosition
import com.example.logger.CommAlerter
import scala.util.Failure

class AjaxFailure( val msg: RestMessage, val result: AjaxResult )(implicit val pos: Position) extends Exception(msg.msg) {
  override
  def toString() = {
    getClass.getName+" "+msg.msg
  }
}

class RequestCancelled( msg: RestMessage, result: AjaxResult )(implicit pos: Position) extends AjaxFailure(msg,result)

class AjaxDisabled( msg: RestMessage, result: AjaxResult )(implicit pos: Position) extends AjaxFailure(msg,result)

trait ResultRecorder {
  def record( ex: Throwable )( implicit pos: Position ): Unit
}

object ResultRecorder extends ResultRecorder {
  val log = Logger("bridge.ResultRecorder")

  import com.example.source._

  def logException( x: Exception, url: Option[String] = None, reqbody: Option[InputData] = None ) = {
    x match {
      case ex: AjaxDisabled =>
        log.info(s"ResultRecorder.logException: Ajax is disabled, called from ${ex.pos.line}")
      case ex: AjaxFailure =>
        val req = ex.result.req
        log.warning("ResultRecorder.logException: "+req.status+"  "+req.responseText+", readyState="+req.readyState+", statusText="+req.statusText+", called from "+ex.pos.line)
        log.warning("ResultRecorder.logException: for "+url.getOrElse("<unknown>")+" "+reqbody.getOrElse("<none>"))
        log.warning("ResultRecorder.logException: responseType="+req.responseType+" responseText="+req.responseText)
      case ex: AjaxException =>
        val req = ex.xhr
        log.warning("ResultRecorder.logException: "+req.status+"  "+req.responseText+", readyState="+req.readyState+", statusText="+req.statusText)
        log.warning("ResultRecorder.logException: for "+url.getOrElse("<unknown>")+" "+reqbody.getOrElse("<none>"))
        log.warning("ResultRecorder.logException: responseType="+req.responseType+" responseText="+req.responseText)
    }
  }

  def record( ex: Throwable )( implicit pos: Position ): Unit = {
    ex match {
      case ex: AjaxDisabled =>
        // ignore it
      case x: RequestCancelled =>
        log.info(s"ResultRecorder.record: Request from ${x.result.pos.fileName}:${x.result.pos.lineNumber} was cancelled, failure recorded from ${pos.fileName}:${pos.lineNumber}", x)
        logException(x, Some( x.result.url ), Some( x.result.reqbody ) )
      case x: AjaxFailure =>
        log.info(s"ResultRecorder.record: Request from ${x.result.pos.fileName}:${x.result.pos.lineNumber} failed ${x}, failure recorded from ${pos.fileName}:${pos.lineNumber}")
        logException(x, Some( x.result.url ), Some( x.result.reqbody ) )
      case x: AjaxException =>
        log.info(s"ResultRecorder.record: Request failed ${x}, failure recorded from ${pos.fileName}:${pos.lineNumber}")
        logException(x)
    }
  }

}

trait CancellableFuture[T] extends Future[T] {
  /**
   * calls Promise.failure( RequestCancelled ) if successfully cancelled
   * returns true if cancelled, false otherwise
   */
  def cancel(): Boolean
}

class ResultHolder[T] {
  private var result: Option[CancellableFuture[T]] = None

  def set( r: CancellableFuture[T] )(implicit executor: ExecutionContext) = {
    result=Some(r)
    r.onComplete(t => result=None)
  }

  def isRunning = result.isDefined

  def cancel() = result match {
    case Some(r) =>
      result = None
      r.cancel()
    case None =>
      false
  }
}

object ResultHolder {
  def apply[T]() = new ResultHolder[T]()
}

trait WrapperXMLHttpRequest {

  /**
   * The status of the response to the request. This is the HTTP result code (for example,
   * status is 200 for a successful request).
   *
   * MDN
   */
  def status: Int

  /**
   * The state of the request: Value State Description 0 UNSENT open()has not been
   * called yet. 1 OPENED send()has not been called yet. 2 HEADERS_RECEIVED send() has
   * been called, and headers and status are available. 3 LOADING Downloading;
   * responseText holds partial data. 4 DONE The operation is complete.
   *
   * MDN
   */
  def readyState: Int

  /**
   * The response to the request as text, or null if the request was unsuccessful or has
   * not yet been sent.
   *
   * MDN
   */
  def responseText: String

  /**
   * Returns the serialized URL of the response or the empty string if the URL is null. If
   * the URL is returned, URL fragment if present in the URL will be stripped away. The
   * value of responseURL will be the final URL obtained after any redirects.
   *
   * This property should be a String, but it isn't implemented by IE, even as new as IE11,
   * hence it must be UndefOr.
   *
   * MDN
   */
  def responseURL: js.UndefOr[String]

  /**
   * The response string returned by the HTTP server. Unlike status, this includes the
   * entire text of the response message ("200 OK", for example).
   *
   * MDN
   */
  def statusText: String

  /**
   * Aborts the request if it has already been sent.
   *
   * MDN
   */
  def abort(): Unit

  def responseType: String

  def getAllResponseHeaders(): String

  def getResponseHeader(header: String): String

}

class WrapperXMLHttpRequestImpl( val req: XMLHttpRequest ) extends WrapperXMLHttpRequest {

  /**
   * The status of the response to the request. This is the HTTP result code (for example,
   * status is 200 for a successful request).
   *
   * MDN
   */
  def status: Int = req.status

  /**
   * The state of the request: Value State Description 0 UNSENT open()has not been
   * called yet. 1 OPENED send()has not been called yet. 2 HEADERS_RECEIVED send() has
   * been called, and headers and status are available. 3 LOADING Downloading;
   * responseText holds partial data. 4 DONE The operation is complete.
   *
   * MDN
   */
  def readyState: Int = req.readyState

  /**
   * The response to the request as text, or null if the request was unsuccessful or has
   * not yet been sent.
   *
   * MDN
   */
  def responseText: String = req.responseText

  /**
   * Returns the serialized URL of the response or the empty string if the URL is null. If
   * the URL is returned, URL fragment if present in the URL will be stripped away. The
   * value of responseURL will be the final URL obtained after any redirects.
   *
   * This property should be a String, but it isn't implemented by IE, even as new as IE11,
   * hence it must be UndefOr.
   *
   * MDN
   */
  def responseURL: js.UndefOr[String] = req.responseURL

  /**
   * The response string returned by the HTTP server. Unlike status, this includes the
   * entire text of the response message ("200 OK", for example).
   *
   * MDN
   */
  def statusText: String = req.statusText

  /**
   * Aborts the request if it has already been sent.
   *
   * MDN
   */
  def abort(): Unit = req.abort

  def responseType: String = req.responseType

  def getAllResponseHeaders(): String = req.getAllResponseHeaders()

  def getResponseHeader(header: String): String = req.getResponseHeader(header)
}

class AjaxResult( val req: WrapperXMLHttpRequest, val url: String, val reqbody: InputData, future: Future[WrapperXMLHttpRequest], promise: Promise[WrapperXMLHttpRequest], val pos: Position ) extends CancellableFuture[WrapperXMLHttpRequest] {
  /**
   * calls Promise.failure( RequestCancelled ) if successfully cancelled
   * returns true if cancelled, false otherwise
   */
  def cancel(): Boolean = {
    req.abort()
    if (req.readyState == XMLHttpRequest.UNSENT) {
      try {
        promise.failure( new RequestCancelled( RestMessage("Cancelled"), this ) )
        true
      } catch {
        case x: IllegalStateException =>
          false
      }
    } else {
      false
    }
  }

  // Members declared in scala.concurrent.Awaitable
  def ready(atMost: Duration)(implicit permit: CanAwait) = {
    future.ready(atMost)
    this
  }
  def result(atMost: Duration)(implicit permit: CanAwait): WrapperXMLHttpRequest = future.result(atMost)

  def defaultTry[T]( msg: String ): Try[T] = Failure( new Exception(msg) )
  def defaultFuture[T]( msg: String ): Future[T] = Future.failed(new Exception(msg))

  // Members declared in scala.concurrent.Future
  def isCompleted: Boolean = future.isCompleted
  def onComplete[U](f: Try[WrapperXMLHttpRequest] => U)(implicit executor: ExecutionContext): Unit = future.onComplete(CommAlerter.tryitfunr(null.asInstanceOf[U])(f))
  def transform[S](f: Try[WrapperXMLHttpRequest] => Try[S])(implicit executor: ExecutionContext): Future[S] = future.transform( CommAlerter.tryitfunr[Try[WrapperXMLHttpRequest],Try[S]](defaultTry("transform"))(f))
  def transformWith[S](f: Try[WrapperXMLHttpRequest] => Future[S])(implicit executor: ExecutionContext): Future[S] = future.transformWith(CommAlerter.tryitfunr[Try[WrapperXMLHttpRequest],Future[S]](defaultFuture("transformWith"))(f))
  def value: Option[Try[WrapperXMLHttpRequest]] = future.value

  def recordFailure( rec: ResultRecorder = ResultRecorder )(implicit executor: ExecutionContext, pos: Position) = {
    failed.foreach( Alerter.tryit { rec.record(_) })
    this
  }

}

/**
 * Wraps an XMLHttpRequest to provide an easy one-line way of making
 * an Ajax call, returning a Future.
 */
object AjaxResult {

  val log = Logger("bridge.AjaxResult")

  import scala.concurrent.duration._
  import scala.language.postfixOps
  val defaultTimeout = 30 seconds

  private var fAjaxCall: IAjaxCall = AjaxCall

  def setEnabled( f: Boolean ) = {
    if (f) {
      fAjaxCall = AjaxCall
    } else {
      fAjaxCall = AjaxCallDisabled
    }
    log.info("Setting RestClient.enabled to "+f)
  }

  def setAjaxCall( ac: IAjaxCall ) = {
    fAjaxCall = ac
  }

  def ajaxCall: IAjaxCall = fAjaxCall

  /**
   * @return Some(true) if enabled and sending to server
   *          Some(false) if disabled
   *          None if a mock test has been set
   */
  def isEnabled: Option[Boolean] = {
    fAjaxCall match {
      case `AjaxCall` => Some(true)
      case `AjaxCallDisabled` => Some(false)
      case _ => None
    }
  }

  def get(url: String, data: InputData = null, timeout: Duration = defaultTimeout,
      headers: Map[String, String] = Map.empty,
      withCredentials: Boolean = false, responseType: String = "")( implicit pos: Position) = {
    apply("GET", url, data, timeout, headers, withCredentials, responseType)(pos)
  }

  def post(url: String, data: InputData = null, timeout: Duration = defaultTimeout,
      headers: Map[String, String] = Map.empty,
      withCredentials: Boolean = false, responseType: String = "")( implicit pos: Position) = {
    apply("POST", url, data, timeout, headers, withCredentials, responseType)(pos)
  }

  def put(url: String, data: InputData = null, timeout: Duration = defaultTimeout,
      headers: Map[String, String] = Map.empty,
      withCredentials: Boolean = false, responseType: String = "")( implicit pos: Position) = {
    apply("PUT", url, data, timeout, headers, withCredentials, responseType)(pos)
  }

  def delete(url: String, data: InputData = null, timeout: Duration = defaultTimeout,
      headers: Map[String, String] = Map.empty,
      withCredentials: Boolean = false, responseType: String = "")( implicit pos: Position) = {
    apply("DELETE", url, data, timeout, headers, withCredentials, responseType)(pos)
  }

  def apply(method: String, url: String, data: InputData, timeout: Duration,
      headers: Map[String, String], withCredentials: Boolean,
      responseType: String)( implicit pos: Position): AjaxResult = {
    ajaxCall.send(method, url, data, timeout, headers, withCredentials, responseType)
  }
}

trait IAjaxCall {
  def send(method: String, url: String, data: InputData, timeout: Duration,
      headers: Map[String, String], withCredentials: Boolean,
      responseType: String)( implicit pos: SourcePosition): AjaxResult
}

object AjaxCall extends IAjaxCall {
  val log = Logger("comm.AjaxCall")

  val Done = dom.raw.XMLHttpRequest.DONE
  val Loading = dom.raw.XMLHttpRequest.LOADING
  val Unsent = dom.raw.XMLHttpRequest.UNSENT
  val Opened = dom.raw.XMLHttpRequest.OPENED
  val HeadersReceived = dom.raw.XMLHttpRequest.HEADERS_RECEIVED

  def readyStateToString( readyState: Int ) = {
    val s = readyState match {
      case Done => "done"
      case Loading => "loading"
      case Unsent => "unsent"
      case Opened => "opened"
      case HeadersReceived => "headersReceived"
      case _ => "unknown"
    }
    s+s"(${readyState})"
  }

  def send(method: String, url: String, data: InputData, timeout: Duration,
      headers: Map[String, String], withCredentials: Boolean,
      responseType: String)( implicit pos: SourcePosition): AjaxResult = {
    val req = new dom.XMLHttpRequest()
    val wreq = new WrapperXMLHttpRequestImpl(req)
    val promise = Promise[WrapperXMLHttpRequest]()
    val result = new AjaxResult( wreq, url, data, promise.future, promise, pos.pos )

    req.onreadystatechange = CommAlerter.tryitfun { (e: dom.Event) =>
      log.fine(s"${method} ${url}, readyState ${readyStateToString(req.readyState)}, status ${req.status}, called from ${pos.line}")
      if (req.readyState == Done) {
        if ((req.status >= 200 && req.status < 300) || req.status == 304) {
          log.fine(s"${method} ${url}, status ${req.status} called from ${pos.line}")
          try {
            promise.success(wreq)
          } catch {
            case x: IllegalStateException =>
              // ignore this, means that promise is already complete
          }
        } else {
          log.warning(s"${method} ${url}, status ${req.status} called from ${pos.line}")
          val resp = req.responseText
          import play.api.libs.json._
          import com.example.data.rest.JsonSupport._
          val msg = try {
            readJson[RestMessage](resp)
          } catch {
            case x: IllegalArgumentException =>
              RestMessage("")
          }
          try {
            promise.failure(new AjaxFailure(msg,result))
          } catch {
            case x: IllegalStateException =>
              // ignore this, means that promise is already complete
          }
        }
      }
    }
    req.ontimeout = CommAlerter.tryitfun { (e: dom.Event) =>
      log.warning(s"${method} ${url}, timeout status ${req.status} called from ${pos.line}")
      try {
        promise.failure(new AjaxFailure(RestMessage("timed out"),result))
      } catch {
        case x: IllegalStateException =>
          // ignore this, means that promise is already complete
      }
    }
    req.open(method, url, true)
    req.responseType = "text" // responseType
    req.timeout = timeout.toMillis
    req.withCredentials = withCredentials
    headers.foreach(x => req.setRequestHeader(x._1, x._2))
    if (data == null)
      req.send()
    else
      req.send(data)

    log.fine(s"${method} ${url}, sent, called from ${pos.line}")

    result
  }

}
class DisabledXMLHttpRequest extends WrapperXMLHttpRequest {
  def status: Int = 0
  def readyState: Int = 0
  def responseText: String = null
  def responseURL: js.UndefOr[String] = js.undefined
  def statusText: String = null
  def abort(): Unit = {}
  def responseType: String = null
  def getAllResponseHeaders(): String = null
  def getResponseHeader(header: String): String = null
}

object AjaxCallDisabled extends IAjaxCall {
  def send(method: String, url: String, data: InputData, timeout: Duration,
      headers: Map[String, String], withCredentials: Boolean,
      responseType: String)( implicit pos: SourcePosition): AjaxResult = {

      sendImpl(method, url, data, timeout, headers, withCredentials, responseType, pos.pos)
  }

  // need this to log location of warning as this file, not from caller (pos)
  private def sendImpl(method: String, url: String, data: InputData, timeout: Duration,
      headers: Map[String, String], withCredentials: Boolean,
      responseType: String, pos: Position): AjaxResult = {
    val wreq = new DisabledXMLHttpRequest
    val promise = Promise[WrapperXMLHttpRequest]()
    val result = new AjaxResult( wreq, url, data, promise.future, promise, pos )

    AjaxResult.log.warning(s"Ajax is disabled, ${method} ${url}, called from ${pos.line}")

    promise.failure( new AjaxDisabled( RestMessage(s""" """), result ) )

    result
  }
}
