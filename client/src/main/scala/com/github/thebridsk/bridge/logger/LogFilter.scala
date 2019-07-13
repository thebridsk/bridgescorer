package com.github.thebridsk.bridge.logger

import com.github.thebridsk.utilities.logging.Filter
import com.github.thebridsk.utilities.logging.TraceMsg

object LogFilter {
  val filterlist = "DuplexPipeForLogging.scala"::
                   "SendToServerHandler.scala"::
                   "SendToWebsocketHandler.scala"::
                   "ServerHandler.scala"::
                   "DuplexPipe"::
                   Nil

  def apply() = new LogFilter
}


class LogFilter extends com.github.thebridsk.bridge.websocket.LogFilter {
  override
  def isLogged(traceMsg: TraceMsg) = {
    super.isLogged(traceMsg) && ( !traceMsg.logger.startsWith("comm.")
                                  && !LogFilter.filterlist.contains(traceMsg.pos.fileName)
                                )
  }
}
