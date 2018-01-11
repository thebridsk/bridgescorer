package com.example.logger

import com.example.controller.Controller
import com.example.rest2.RestClientLogEntryV2

import utils.logging.Handler
import utils.logging.Logger
import utils.logging.TraceMsg
import com.example.rest2.AjaxFailure
import org.scalajs.dom.ext.AjaxException

object SendToServerHandler {

  val logger = Logger("comm.SendToServerHandler")

}

class SendToServerHandler extends Handler with ServerHandler {

  def logIt( traceMsg: TraceMsg ): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
//    logger.info("sending to server: "+traceMsg )
    RestClientLogEntryV2.update("entry",traceMsgToLogEntryV2(traceMsg)).failed.foreach( t => t match {
      case x: AjaxException =>
        val status = x.xhr.status
        val resp = x.xhr.responseText
        val readyState = x.xhr.readyState
        SendToServerHandler.logger.warning("Got back an error in rest api for loggingV2: "+status+"  "+resp+" "+readyState)
      case _ =>
        SendToServerHandler.logger.warning("Unknown exception sending logs for loggingV2: "+t )
    })
  }

}
