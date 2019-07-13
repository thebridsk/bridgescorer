package com.github.thebridsk.bridge.websocket

import com.github.thebridsk.utilities.logging.Filter
import com.github.thebridsk.utilities.logging.TraceMsg

object LogFilter {
  val filterlist = "DuplexPipe.scala"::
                   "MyWebsocket.scala"::
                   "BridgeWebsocket.scala"::
                   Nil

  def apply() = new LogFilter
}


class LogFilter extends Filter {
  def isLogged(traceMsg: TraceMsg) = {
    !LogFilter.filterlist.contains(traceMsg.pos.fileName) && !traceMsg.logger.startsWith("comm.")
  }
}
