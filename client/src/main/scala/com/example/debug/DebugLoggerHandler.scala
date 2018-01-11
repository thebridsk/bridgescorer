package com.example.debug

import com.example.controller.Controller
import com.example.rest2.RestClientLogEntryV2

import utils.logging.Handler
import utils.logging.Logger
import utils.logging.TraceMsg
import com.example.rest2.AjaxFailure
import org.scalajs.dom.ext.AjaxException
import com.example.logger.ServerHandler
import com.example.bridge.action.BridgeDispatcher

object DebugLoggerHandler {
  val exclude = "bridge.Listenable"::
                "bridge.AjaxResult"::
                Nil
}

class DebugLoggerHandler extends Handler with ServerHandler {
  import DebugLoggerHandler._

  def logIt( traceMsg: TraceMsg ): Unit = {
    if (!exclude.contains(traceMsg.logger)) {
      BridgeDispatcher.log(traceMsg)
    }
  }

}
