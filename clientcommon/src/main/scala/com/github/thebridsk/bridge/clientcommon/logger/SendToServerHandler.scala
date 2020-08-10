package com.github.thebridsk.bridge.clientcommon.logger

import com.github.thebridsk.bridge.clientcommon.rest2.RestClientLogEntryV2

import com.github.thebridsk.utilities.logging.Handler
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.utilities.logging.TraceMsg
import org.scalajs.dom.ext.AjaxException

object SendToServerHandler {

  val logger: Logger = Logger("comm.SendToServerHandler")

}

class SendToServerHandler extends Handler with ServerHandler {

  def logIt(traceMsg: TraceMsg): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
//    logger.info("sending to server: "+traceMsg )
    RestClientLogEntryV2
      .update("entry", traceMsgToLogEntryV2(traceMsg))
      .failed
      .foreach(t =>
        t match {
          case x: AjaxException =>
            val status = x.xhr.status
            val resp = x.xhr.responseText
            val readyState = x.xhr.readyState
            SendToServerHandler.logger.warning(
              "Got back an error in rest api for loggingV2: " + status + "  " + resp + " " + readyState
            )
          case _ =>
            SendToServerHandler.logger
              .warning("Unknown exception sending logs for loggingV2: " + t)
        }
      )
  }

}
