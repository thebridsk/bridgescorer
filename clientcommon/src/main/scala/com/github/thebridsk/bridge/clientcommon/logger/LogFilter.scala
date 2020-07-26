package com.github.thebridsk.bridge.clientcommon.logger

import com.github.thebridsk.utilities.logging.TraceMsg

object LogFilter {
  val filterlist: List[String] = "DuplexPipeForLogging.scala"::
                   "SendToServerHandler.scala"::
                   "SendToWebsocketHandler.scala"::
                   "ServerHandler.scala"::
                   "DuplexPipe"::
                   Nil

  def apply() = new LogFilter
}


class LogFilter extends com.github.thebridsk.bridge.clientcommon.websocket.LogFilter {
  override
  def isLogged(traceMsg: TraceMsg): Boolean = {
    super.isLogged(traceMsg) && ( !traceMsg.logger.startsWith("comm.")
                                  && !LogFilter.filterlist.contains(traceMsg.pos.fileName)
                                )
  }
}
