package com.github.thebridsk.bridge.rest2

import org.scalactic.source.Position
import com.github.thebridsk.utilities.logging.Logger
import org.scalajs.dom.ext.Ajax.InputData
import org.scalajs.dom.ext.AjaxException

trait ResultRecorder {
  def record( ex: Throwable )( implicit pos: Position ): Unit
}

object ResultRecorder extends ResultRecorder {
  val log = Logger("bridge.ResultRecorder")

  import com.github.thebridsk.bridge.source._

  def logException( x: Exception, url: Option[String] = None, reqbody: Option[InputData] = None ) = {
    x match {
      case ex: AjaxDisabled =>
        log.info(s"ResultRecorder.logException: Ajax is disabled, called from ${ex.pos.line}")
      case ex: AjaxFailureException =>
        val req = ex.result.req
        log.warning("ResultRecorder.logException: "+req.status+"  "+req.responseText+", readyState="+req.readyState+", statusText="+req.statusText+", called from "+ex.pos.line)
        log.warning("ResultRecorder.logException: for "+url.getOrElse("<unknown>")+" "+reqbody.getOrElse("<none>"))
        log.warning("ResultRecorder.logException: responseType="+req.responseType+" responseText="+req.responseText)
      case ex: AjaxException =>
        val req = ex.xhr
        log.warning("ResultRecorder.logException: "+req.status+"  "+req.responseText+", readyState="+req.readyState+", statusText="+req.statusText)
        log.warning("ResultRecorder.logException: for "+url.getOrElse("<unknown>")+" "+reqbody.getOrElse("<none>"))
        log.warning("ResultRecorder.logException: responseType="+req.responseType+" responseText="+req.responseText)
      case x: Exception =>
        log.warning("ResultRecorder.record: Unknown exception", x)
    }
  }

  def record( ex: Throwable )( implicit pos: Position ): Unit = {
    ex match {
      case ex: AjaxDisabled =>
        // ignore it
      case x: RequestCancelled =>
        log.info(s"ResultRecorder.record: Request from ${x.result.pos.fileName}:${x.result.pos.lineNumber} was cancelled, failure recorded from ${pos.fileName}:${pos.lineNumber}", x)
        logException(x, Some( x.result.url ), Some( x.result.reqbody ) )
      case x: AjaxFailureException =>
        log.info(s"ResultRecorder.record: Request from ${x.result.pos.fileName}:${x.result.pos.lineNumber} failed ${x}, failure recorded from ${pos.fileName}:${pos.lineNumber}", x)
        logException(x, Some( x.result.url ), Some( x.result.reqbody ) )
      case x: AjaxException =>
        log.info(s"ResultRecorder.record: Request failed ${x}, failure recorded from ${pos.fileName}:${pos.lineNumber}", x)
        logException(x)
      case x: Exception =>
        log.warning(s"ResultRecorder.record: Unknown exception, failure recorded from ${pos.fileName}:${pos.lineNumber}", x)
    }
  }

}
