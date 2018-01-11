package com.example.logger

import com.example.controller.Controller
import utils.logging.Handler
import utils.logging.Logger
import utils.logging.TraceMsg
import com.example.routes.AppRouter
import com.example.data.websocket.DuplexProtocol

object SendToWebsocketHandler {
  val log = Logger("comm.SendToWebsocketHandler")
}

class SendToWebsocketHandler extends Handler with ServerHandler {

  var duplexPipe: Option[DuplexPipeForLogging] = None

  def getDuplexPipe() = duplexPipe match {
    case Some(d) => d
    case None =>
      val url = AppRouter.hostUrl.replaceFirst("http", "ws") + "/v1/logger/ws"
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
