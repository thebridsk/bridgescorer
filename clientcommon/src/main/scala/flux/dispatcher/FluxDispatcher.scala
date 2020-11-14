package flux.dispatcher

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
trait DispatchToken extends js.Object

object FluxDispatcher {
  def apply[X](): FluxDispatcher[X] = {
    new FluxDispatcher[X]()
  }
}

@JSImport("flux", "Dispatcher")
@js.native
class FluxDispatcher[X] extends js.Object {

  /**
    * Dispatches a payload to all registered callbacks.
    */
  def dispatch(message: X): Unit = js.native

  /**
    * Waits for the callbacks specified to be invoked before continuing execution of the current callback.
    * This method should only be used by a callback in response to a dispatched payload.
    */
  def waitFor(token: js.Array[DispatchToken]): Unit = js.native

  /**
    * Registers a callback to be invoked with every dispatched payload. Returns a token that can be used with waitFor().
    */
  def register(handler: js.Function1[X, Any]): DispatchToken = js.native

  /**
    * Removes a callback based on its token.
    */
  def unregister(token: DispatchToken): Unit = js.native

  /**
    * Is this Dispatcher currently dispatching.
    */
  def isDispatching(): Boolean = js.native
}
