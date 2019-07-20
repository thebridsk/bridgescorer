package com.github.thebridsk.bridge.clientcommon.rest2

import org.scalajs.dom.ext.Ajax.InputData
import scala.concurrent.duration.Duration
import com.github.thebridsk.bridge.source.SourcePosition
import org.scalactic.source.Position
import scala.concurrent.Promise
import com.github.thebridsk.bridge.data.RestMessage
import org.scalajs.dom
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.logger.CommAlerter
import com.github.thebridsk.bridge.clientcommon.logger.Init

trait IAjaxCall {
  def send(method: String, url: String, data: InputData, timeout: Duration,
      headers: Map[String, String], withCredentials: Boolean,
      responseType: String)( implicit pos: SourcePosition): AjaxResult[WrapperXMLHttpRequest]
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

  import play.api.libs.json._
  def processError( statusCode: Int, resp: JsValue ) = {
    resp match {
      case _: JsObject =>
        resp \ "errors" match {
          case JsDefined( JsArray( messages ) ) =>
            val e = messages.map { v =>
                       (v \ "message") match {
                         case JsDefined(JsString(msg)) => msg
                         case x => x.toString()
                       }
                     }.mkString("Errors:\n","\n","")
            log.warning(e)
            RestMessage(e)
          case JsDefined( _ ) =>
            log.warning(s"Expecting a messages, got ${resp}")
            RestMessage(s"Expecting a messages, got ${resp}")
          case x: JsUndefined =>
            resp \ "error" match {
              case JsDefined( JsString( msg ) ) =>
                val e = s"Error:\n${msg}"
                log.warning(e)
                RestMessage(e)
              case JsDefined( _ ) =>
                log.warning(s"Expecting a message, got ${resp}")
                RestMessage(s"Expecting a message, got ${resp}")
              case x: JsUndefined =>
                // no error
                RestMessage(s"Got statusCode ${statusCode} but no error or errors field: ${x}.  Response is ${resp}")
            }
        }

      case _ =>
        log.warning(s"Expecting a JsObject, got ${resp}")
        RestMessage(s"Expecting a JsObject, got ${resp}")
    }
  }

  def send(
            method: String,
            url: String,
            data: InputData,
            timeout: Duration,
            headers: Map[String, String],
            withCredentials: Boolean,
            responseType: String
          )(
            implicit
              pos: SourcePosition
          ): AjaxResult[WrapperXMLHttpRequest] = {
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

          promise.failure( new AjaxErrorReturn( req.status, resp, result ) )
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
    req.timeout = timeout.toMillis.toDouble
    req.withCredentials = withCredentials
    headers.foreach(x => req.setRequestHeader(x._1, x._2))
    Init.addClientId(req)
    if (data == null)
      req.send()
    else
      req.send(data)

    log.fine(s"${method} ${url}, sent, called from ${pos.line}")

    result
  }

}

object AjaxCallDisabled extends IAjaxCall {
  def send(method: String, url: String, data: InputData, timeout: Duration,
      headers: Map[String, String], withCredentials: Boolean,
      responseType: String)( implicit pos: SourcePosition): AjaxResult[WrapperXMLHttpRequest] = {

      sendImpl(method, url, data, timeout, headers, withCredentials, responseType, pos.pos)
  }

  // need this to log location of warning as this file, not from caller (pos)
  private def sendImpl(method: String, url: String, data: InputData, timeout: Duration,
      headers: Map[String, String], withCredentials: Boolean,
      responseType: String, pos: Position): AjaxResult[WrapperXMLHttpRequest] = {
    val wreq = new DisabledXMLHttpRequest
    val promise = Promise[WrapperXMLHttpRequest]()
    val result = new AjaxResult( wreq, url, data, promise.future, promise, pos )

    AjaxResult.log.warning(s"Ajax is disabled, ${method} ${url}, called from ${pos.line}")

    promise.failure( new AjaxDisabled( RestMessage(s""" """), result ) )

    result
  }
}
