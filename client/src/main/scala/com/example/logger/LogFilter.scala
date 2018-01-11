package com.example.logger

import utils.logging.Filter
import utils.logging.TraceMsg

object LogFilter {
  val filterlist = "DuplexPipeForLogging.scala"::
                   "SendToServerHandler.scala"::
                   "SendToWebsocketHandler.scala"::
                   "ServerHandler.scala"::
                   "DuplexPipe"::
                   Nil

  def apply() = new LogFilter
}


class LogFilter extends com.example.websocket.LogFilter {
  override
  def isLogged(traceMsg: TraceMsg) = {
    super.isLogged(traceMsg) && ( !traceMsg.logger.startsWith("comm.")
                                  && !LogFilter.filterlist.contains(traceMsg.pos.fileName)
                                )
  }
}
