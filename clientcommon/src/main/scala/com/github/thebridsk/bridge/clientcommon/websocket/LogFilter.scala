package com.github.thebridsk.bridge.clientcommon.websocket

import com.github.thebridsk.utilities.logging.Filter
import com.github.thebridsk.utilities.logging.TraceMsg

object LogFilter {
  val filterlist: List[String] = "DuplexPipe.scala" ::
    "MyWebsocket.scala" ::
    "BridgeWebsocket.scala" ::
    Nil

  def apply() = new LogFilter
}

class LogFilter extends Filter {
  def isLogged(traceMsg: TraceMsg): Boolean = {
    !LogFilter.filterlist.contains(traceMsg.pos.fileName) &&
      !traceMsg.logger.startsWith("comm.")
  }
}
