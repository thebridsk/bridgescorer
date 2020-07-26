package com.github.thebridsk.bridge.clientcommon.logger

import com.github.thebridsk.utilities.logging.Handler
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.utilities.logging.TraceMsg
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol

object SendToWebsocketHandler {
  val log: Logger = Logger("comm.SendToWebsocketHandler")
}

class SendToWebsocketHandler extends Handler with ServerHandler {

  var duplexPipe: Option[DuplexPipeForLogging] = None

  def getDuplexPipe(): DuplexPipeForLogging = duplexPipe match {
    case Some(d) => d
    case None =>
      val url = Info.hostUrl.replaceFirst("http", "ws") + "/v1/logger/ws"
      val d = DuplexPipeForLogging( url );
      d.addListener(new DuplexPipeForLogging.Listener {
        def onMessage( msg: DuplexProtocol.DuplexMessage ) = {
          msg match {
            case DuplexProtocol.ErrorResponse(data, seq ) =>
              SendToWebsocketHandler.log.severe("Unexpected data on logger websocket: "+data)
            case x =>
              SendToWebsocketHandler.log.severe("Unexpected data on logger websocket: "+x)
          }
        }
      })
      duplexPipe = Some(d)
      d
  }

  def logIt( traceMsg: TraceMsg ): Unit = {
    getDuplexPipe().sendlog( traceMsgToLogEntryV2(traceMsg))
  }

}
