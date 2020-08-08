package com.github.thebridsk.bridge.client.websocket

import com.github.thebridsk.bridge.data.websocket.Protocol
import com.github.thebridsk.bridge.clientcommon.websocket.MyWebsocket
import com.github.thebridsk.bridge.clientcommon.websocket.MyWebsocketCodes
import com.github.thebridsk.bridge.clientcommon.websocket.Code

abstract class BridgeWebsocket(url: String, protocol: String)
    extends MyWebsocket(url, protocol) {

  def onObject(msg: Protocol.ToBrowserMessage): Unit

  final override def onMessage(data: String): Unit = {
    val msg = Protocol.fromStringToBrowserMessage(data)
    onObject(msg)
  }

  def sendObj(msg: Protocol.ToServerMessage): Unit = {
    send(Protocol.toStringToServerMessage(msg))
  }
}

object BridgeWebsocket extends MyWebsocketCodes {

  override def getMsgFromCode(code: Code): String =
    reasons.getOrElse(code.code, MyWebsocket.getMsgFromCode(code))

  val BRIDGE_CLOSE_NORMAL = 4000

  private val reasons = Map(
    4000 -> "Normal closure."
  )

}
