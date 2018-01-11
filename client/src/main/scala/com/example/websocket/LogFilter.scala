package com.example.websocket

import utils.logging.Filter
import utils.logging.TraceMsg

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
