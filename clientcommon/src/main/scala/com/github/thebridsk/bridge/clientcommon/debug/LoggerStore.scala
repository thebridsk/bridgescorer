package com.github.thebridsk.bridge.clientcommon.debug

import com.github.thebridsk.bridge.clientcommon.dispatcher.ChangeListenable
import com.github.thebridsk.utilities.logging.TraceMsg
import flux.dispatcher.DispatchToken
import com.github.thebridsk.bridge.clientcommon.dispatcher.Dispatcher
import com.github.thebridsk.bridge.clientcommon.dispatcher.PostLogEntry
import scala.collection.mutable.ListBuffer
import com.github.thebridsk.bridge.clientcommon.dispatcher.ClearLogs
import com.github.thebridsk.bridge.clientcommon.dispatcher.StopLogs
import com.github.thebridsk.bridge.clientcommon.dispatcher.StartLogs
import scala.scalajs.js.timers.SetTimeoutHandle
import org.scalajs.dom

object LoggerStore extends ChangeListenable {

  baseclass: ChangeListenable =>

  val MaxSize = 200

  /**
    * Required to instantiate the store.
    */
  def init(): Option[Unit] = {
    dispatchToken.map { x =>
      toConsole("LoggerStore registered")
    }
  }

  private var dispatchToken: Option[DispatchToken] = {
    val tok = Some(Dispatcher.register(dispatch _))
    toConsole(s"""LoggerStore registered on dispatcher: ${tok}""")
    tok
  }

  case class MessageEntry(i: Long, traceMsg: TraceMsg)

  private var messages: ListBuffer[MessageEntry] = ListBuffer()
  private var enabled: Boolean = false
  private var counter: Long = 0

  /**
    * Get the trace messages in the store
    * @return the messages in reverse chronological order, returned object must not be changed
    */
  def getMessages() = messages

  def isEnabled() = enabled

  def dispatch(msg: Any): Any =
    msg match {
      case PostLogEntry(tracemsg) =>
        counter = counter + 1
        if (enabled) {
          logToConsole(tracemsg)
          messages.insert(0, MessageEntry(counter, tracemsg))
          if (messages.size > MaxSize) messages = messages.take(MaxSize)
          notifyLogChange()
        }
      case _: ClearLogs =>
        messages.clear()
        notifyLogChange()
      case _: StopLogs =>
        toConsole("Stopping logging")
        enabled = false
        notifyLogChange()
      case _: StartLogs =>
        toConsole("Starting logging")
        enabled = true
        notifyLogChange()
      case x =>
      // There are multiple stores, all the actions get sent to all stores
//      logger.warning("BoardSetStore: Unknown msg dispatched, "+x)
    }

  def notifyLogChange(): SetTimeoutHandle = {
    import scala.scalajs.js.timers._

    setTimeout(0) { // note the absence of () =>
      notifyChange()
    }
  }

  var debug = true

  def logToConsole(msg: => TraceMsg): Unit = {
    if (debug) {
      dom.window.console.log(msg.toString)
    }
  }

  def toConsole(msg: => String): Unit = {
    if (debug) {
      dom.window.console.log(msg)
    }
  }
}
