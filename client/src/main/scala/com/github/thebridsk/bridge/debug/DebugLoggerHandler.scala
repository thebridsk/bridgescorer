package com.github.thebridsk.bridge.debug

import com.github.thebridsk.bridge.controller.Controller
import com.github.thebridsk.bridge.rest2.RestClientLogEntryV2

import com.github.thebridsk.utilities.logging.Handler
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.utilities.logging.TraceMsg
import com.github.thebridsk.bridge.rest2.AjaxFailure
import com.github.thebridsk.bridge.logger.ServerHandler
import com.github.thebridsk.bridge.bridge.action.BridgeDispatcher

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
