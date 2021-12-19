package com.github.thebridsk.redux

import scala.scalajs.js

@js.native
trait Store[S <: State] extends js.Any {

  /**
    * Returns the current state tree of your application. It is equal to the last value returned by the store's reducer.
    *
    * @return The current state tree of your application.
    */
  def getState(): S = js.native

  /**
    * Dispatches an action. This is the only way to trigger a state change.
    *
    * The store's reducing function will be called with the current getState() result and the given action synchronously.
    * Its return value will be considered the next state. It will be returned from getState() from now on,
    * and the change listeners will immediately be notified.
    *
    * If you attempt to call dispatch from inside the reducer, it will throw with an error saying “Reducers may not dispatch actions.”
    * This is similar to “Cannot dispatch in a middle of dispatch” error in Flux, but doesn't cause the problems associated with it.
    * In Flux, a dispatch is forbidden while Stores are handling the action and emitting updates.
    * This is unfortunate because it makes it impossible to dispatch actions from component lifecycle hooks or other benign places.
    *
    * In Redux, subscriptions are called after the root reducer has returned the new state, so you may dispatch in the subscription listeners.
    * You are only disallowed to dispatch inside the reducers because they must have no side effects.
    * If you want to cause a side effect in response to an action, the right place to do this is in the potentially async action creator.
    *
    * @param action A plain object describing the change that makes sense for your application.
    *               Actions are the only way to get data into the store, so any data, whether from the UI events,
    *               network callbacks, or other sources such as WebSockets needs to eventually be dispatched as actions.
    *               Actions must have a type field that indicates the type of action being performed.
    *               Types can be defined as constants and imported from another module.
    *               It's better to use strings for type than Symbols because strings are serializable.
    *               Other than type, the structure of an action object is really up to you.
    *               If you're interested, check out Flux Standard Action for recommendations on how actions could be constructed.
    * @return The dispatched action (see notes).
    */
  def dispatch(action: NativeAction[Action]): S = js.native

  def dispatch(delayedAction: thunk.Function[S]): S = js.native

  /**
    * Adds a change listener. It will be called any time an action is dispatched, and some part of the state tree may potentially have changed.
    * You may then call getState() to read the current state tree inside the callback.
    *
    * You may call dispatch() from a change listener, with the following caveats:
    * 1. The listener should only call dispatch() either in response to user actions or under specific conditions (e. g. dispatching an action when the store has a specific field).
    *    Calling dispatch() without any conditions is technically possible, however it leads to an infinite loop as every dispatch() call usually triggers the listener again.
    * 2. The subscriptions are snapshotted just before every dispatch() call.
    *    If you subscribe or unsubscribe while the listeners are being invoked, this will not have any effect on the dispatch() that is currently in progress.
    *    However, the next dispatch() call, whether nested or not, will use a more recent snapshot of the subscription list.
    * 3. The listener should not expect to see all state changes, as the state might have been updated multiple times during a nested dispatch() before the listener is called.
    *    It is, however, guaranteed that all subscribers registered before the dispatch() started will be called with the latest state by the time it exits.
    *
    * It is a low-level API. Most likely, instead of using it directly, you'll use React (or other) bindings.
    * If you commonly use the callback as a hook to react to state changes, you might want to write a custom observeStore utility.
    * The Store is also an Observable, so you can subscribe to changes with libraries like RxJS.
    *
    * To unsubscribe the change listener, invoke the function returned by subscribe.
    *
    * @param listener The callback to be invoked any time an action has been dispatched, and the state tree might have changed.
    *                 You may call getState() inside this callback to read the current state tree.
    *                 It is reasonable to expect that the store's reducer is a pure function,
    *                 so you may compare references to some deep path in the state tree to learn whether its value has changed.
    * @return the unsubscribe function
    */
  def subscribe(listener: Listener): Unsubscribe = js.native

  /**
    * Replaces the reducer currently used by the store to calculate the state.
    *
    * It is an advanced API. You might need this if your app implements code splitting, and you want to load some of the reducers dynamically.
    * You might also need this if you implement a hot reloading mechanism for Redux.
    *
    * @param nextReducer The next reducer for the store to use.
    */
  def replaceReducer(nextReducer: Reducer[S]): Unit = js.native
}
