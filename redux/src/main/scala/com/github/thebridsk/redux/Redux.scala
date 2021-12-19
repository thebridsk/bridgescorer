package com.github.thebridsk.redux

import scala.scalajs.js
import scala.scalajs.js.annotation._

import com.github.thebridsk.redux._

@js.native
@JSImport("redux", JSImport.Namespace)
object Redux extends js.Object {

  /**
    * Creates a Redux store that holds the complete state tree of your app. There should only be a single store in your app.
    *
    * @param reducer A reducing function that returns the next state tree, given the current state tree and an action to handle.
    * @param preloadedState The initial state. You may optionally specify it to hydrate the state from the server in universal apps,
    *                       or to restore a previously serialized user session. If you produced reducer with combineReducers,
    *                       this must be a plain object with the same shape as the keys passed to it.
    *                       Otherwise, you are free to pass anything that your reducer can understand.
    * @param enhancer The store enhancer. You may optionally specify it to enhance the store with third-party capabilities such as middleware,
    *                 time travel, persistence, etc. The only store enhancer that ships with Redux is applyMiddleware().
    * @return An object that holds the complete state of your app. The only way to change its state is by dispatching actions.
    *         You may also subscribe to the changes to its state to update the UI.
    */
  def createStore[S <: State](
    reducer: Reducer[S],
    preloadedState: js.UndefOr[S] = js.undefined,
    enhancer: js.UndefOr[Enhancer[S]] = js.undefined
  ): Store[S] = js.native

  /**
    * As your app grows more complex, you'll want to split your reducing function into separate functions, each managing independent parts of the state.
    *
    * The combineReducers helper function turns an object whose values are different reducing functions into a single reducing function you can pass to createStore.
    *
    * The resulting reducer calls every child reducer, and gathers their results into a single state object.
    * The state produced by combineReducers() namespaces the states of each reducer under their keys as passed to combineReducers()
    *
    * @param reducers An object whose values correspond to different reducing functions that need to be combined into one.
    *                 See the notes below for some rules every passed reducer must follow.
    * @return A reducer that invokes every reducer inside the reducers object, and constructs a state object with the same shape.
    *
    * Notes​
    *
    * This function is mildly opinionated and is skewed towards helping beginners avoid common pitfalls.
    * This is why it attempts to enforce some rules that you don't have to follow if you write the root reducer manually.
    *
    * Any reducer passed to combineReducers must satisfy these rules:
    * - For any action that is not recognized, it must return the state given to it as the first argument.
    * - It must never return undefined. It is too easy to do this by mistake via an early return statement,
    *   so combineReducers throws if you do that instead of letting the error manifest itself somewhere else.
    * - If the state given to it is undefined, it must return the initial state for this specific reducer.
    *   According to the previous rule, the initial state must not be undefined either.
    *   It is handy to specify it with ES6 optional arguments syntax, but you can also explicitly check the first argument for being undefined.
    *
    * While combineReducers attempts to check that your reducers conform to some of these rules, you should remember them,
    * and do your best to follow them. combineReducers will check your reducers by passing undefined to them;
    * this is done even if you specify initial state to Redux.createStore(combineReducers(...), initialState).
    * Therefore, you must ensure your reducers work properly when receiving undefined as state, even if you never
    * intend for them to actually receive undefined in your own code.
    */
  def combineReducers[S <: State](
    reducers: js.Dictionary[Reducer[State]]
  ): Reducer[S] = js.native

  def applyMiddleware[S <: State](
    middleware: Middleware[S]*
  ): Enhancer[S] = js.native
}

