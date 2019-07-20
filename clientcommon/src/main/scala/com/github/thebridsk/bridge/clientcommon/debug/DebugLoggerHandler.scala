package com.github.thebridsk.bridge.clientcommon.debug

import com.github.thebridsk.utilities.logging.Handler
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.utilities.logging.TraceMsg
import com.github.thebridsk.bridge.clientcommon.logger.ServerHandler
import com.github.thebridsk.bridge.clientcommon.dispatcher.Dispatcher

object DebugLoggerHandler {
  val exclude = "bridge.Listenable"::
                "bridge.AjaxResult"::
                Nil
}

class DebugLoggerHandler extends Handler with ServerHandler {
  import DebugLoggerHandler._

  def logIt( traceMsg: TraceMsg ): Unit = {
    if (!exclude.contains(traceMsg.logger)) {
      Dispatcher.log(traceMsg)
    }
  }

}
