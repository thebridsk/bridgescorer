package com.github.thebridsk.bridge.server.backend.resource

trait Updator[V, T, R] {
  val changeContext: ChangeContext = new ChangeContext()

  /**
    * called by update method in Resources.
    * The implementation of this method must update the specified value to the new value.
    * @param value the current value of the resource to be updated
    * @return a Result of a tuple2.  The first value in the tuple2 is the new value of the resource.
    * The second value is a object that will be passed to the responder method.
    */
  def updater(value: V): Result[(V, T)]

  /**
    * called by the update method in Resources.
    * The implementation of this method must generate a response from the resp argument.  The response
    * will be returned by the update method in Resources.
    * @param resp the value that was returned by the updater method.
    * @return the value that will be returned to the to the caller of the update method in Resources.
    */
  def responder(resp: Result[(V, T)]): Result[R]

  /**
    * Add a change context to the request.
    * @param change
    */
  def prepend(change: ChangeContextData) = changeContext.prepend(change)
}
