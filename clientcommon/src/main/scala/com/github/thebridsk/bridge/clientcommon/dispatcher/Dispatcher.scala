package com.github.thebridsk.bridge.clientcommon.dispatcher

import flux.dispatcher.FluxDispatcher
import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import flux.dispatcher.DispatchToken
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.utilities.logging.TraceMsg

object Dispatcher extends Dispatcher {
}

object InternalDispatcher {
  val fluxdispatcher: FluxDispatcher[Action] = FluxDispatcher() // new FluxDispatcher()
}

trait Dispatcher {

  val dispatcher: FluxDispatcher[Action] = InternalDispatcher.fluxdispatcher

  def log( msg: TraceMsg ) = {
    if (dispatcher.isDispatching()) {
      import scala.scalajs.js.timers._

      setTimeout(0) { // note the absence of () =>
        dispatcher.dispatch( PostLogEntry(msg) )
      }
    } else {
      dispatcher.dispatch( PostLogEntry(msg) )
    }
  }
  def stopLogs() = dispatcher.dispatch(StopLogs())
  def startLogs() = dispatcher.dispatch(StartLogs())
  def clearLogs() = dispatcher.dispatch(ClearLogs())

  /**
   * Waits for the callbacks specified to be invoked before continuing execution of the current callback.
   * This method should only be used by a callback in response to a dispatched payload.
   */
  def waitFor(token: js.Array[DispatchToken]): Unit = dispatcher.waitFor(token)
  /**
   * Registers a callback to be invoked with every dispatched payload. Returns a token that can be used with waitFor().
   */
  def register(handler: js.Function1[Action,Any]): DispatchToken = dispatcher.register(handler)
  /**
   * Removes a callback based on its token.
   */
  def unregister( token: DispatchToken ): Unit = dispatcher.unregister(token)
  /**
   * Is this Dispatcher currently dispatching.
   */
  def isDispatching(): Boolean = dispatcher.isDispatching()

}
