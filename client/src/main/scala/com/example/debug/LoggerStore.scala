package com.example.debug

import com.example.bridge.store.ChangeListenable
import utils.logging.TraceMsg
import flux.dispatcher.DispatchToken
import com.example.bridge.action.BridgeDispatcher
import com.example.bridge.action.PostLogEntry
import scala.collection.mutable.ListBuffer
import com.example.bridge.action.ClearLogs
import com.example.bridge.action.StopLogs
import com.example.bridge.action.StartLogs

object LoggerStore extends ChangeListenable {

  val MaxSize = 100

  /**
   * Required to instantiate the store.
   */
  def init() = {}

  private var dispatchToken: Option[DispatchToken] = Some(BridgeDispatcher.register(dispatch _))

  case class MessageEntry( i: Long, traceMsg: TraceMsg )

  private var messages: ListBuffer[MessageEntry] = ListBuffer()
  private var enabled: Boolean = true
  private var counter: Long = 0

  /**
   * Get the trace messages in the store
   * @return the messages in reverse chronological order, returned object must not be changed
   */
  def getMessages() = messages

  def isEnabled() = enabled

  def dispatch( msg: Any ) = msg match {
    case PostLogEntry(tracemsg) =>
      counter = counter + 1
      if (enabled) {
        messages.insert(0, MessageEntry(counter,tracemsg))
        if (messages.size > MaxSize) messages = messages.take(MaxSize)
        notifyChange()
      }
    case _: ClearLogs =>
      messages.clear()
      notifyChange()
    case _: StopLogs =>
      enabled = false
      notifyChange()
    case _: StartLogs =>
      enabled = true
      notifyChange()
    case x =>
      // There are multiple stores, all the actions get sent to all stores
//      logger.warning("BoardSetStore: Unknown msg dispatched, "+x)
  }

}
