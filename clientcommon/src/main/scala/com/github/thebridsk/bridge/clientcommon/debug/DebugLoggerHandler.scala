package com.github.thebridsk.bridge.clientcommon.debug

import com.github.thebridsk.utilities.logging.Handler
import com.github.thebridsk.utilities.logging.TraceMsg
import com.github.thebridsk.bridge.clientcommon.logger.ServerHandler
import com.github.thebridsk.bridge.clientcommon.dispatcher.Dispatcher

object DebugLoggerHandler {
  val exclude: List[String] = "bridge.Listenable"::
                "bridge.AjaxResult"::
                Nil
}

class DebugLoggerHandler extends Handler with ServerHandler {
  import DebugLoggerHandler._

  def logIt( traceMsg: TraceMsg ): Unit = {
    if (!exclude.contains(traceMsg.logger)) {
      LoggerStore.toConsole(s"sending to DebugLogger: ${traceMsg}")
      Dispatcher.log(traceMsg)
    }
  }

}
