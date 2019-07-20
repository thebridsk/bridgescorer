package com.github.thebridsk.bridge.client.websocket

import scala.scalajs.js
import com.github.thebridsk.bridge.data.websocket.Protocol
import scala.scalajs.js.Array
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.websocket.MyWebsocket
import com.github.thebridsk.bridge.clientcommon.websocket.MyWebsocketCodes
import com.github.thebridsk.bridge.clientcommon.websocket.Code

abstract class BridgeWebsocket(url: String, protocol: String) extends MyWebsocket(url,protocol) {

  def onObject(msg: Protocol.ToBrowserMessage): Unit

  override final def onMessage( data: String): Unit = {
    val msg = Protocol.fromStringToBrowserMessage(data)
    onObject(msg)
  }

  def sendObj( msg: Protocol.ToServerMessage ): Unit = {
    send( Protocol.toStringToServerMessage(msg) )
  }
}

object BridgeWebsocket extends MyWebsocketCodes {

  override def getMsgFromCode( code: Code ) = reasons.getOrElse(code.code, MyWebsocket.getMsgFromCode(code))

  val BRIDGE_CLOSE_NORMAL = 4000

  private val reasons = Map(
    4000 -> "Normal closure."
  )

}
