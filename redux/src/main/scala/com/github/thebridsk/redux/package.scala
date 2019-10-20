package com.github.thebridsk.redux

import scala.scalajs.js
import scala.scalajs.js.|


package object redux {

  // reducer = (state: S, action: A) => S
  type Reducer[State <: js.Any, Action <: js.Any ] = js.Function2[State,Action,State]

  /**
   * Base trait for a reducers structure
   * Subclasses must only define fields that are of type Reducer
   */
  @js.native
  trait Reducers extends js.Object

  // baseDispatch = (a: Action) => Action
  type BaseDispatch[Action <: js.Any, ReturnAction <: js.Any] = js.Function1[Action,ReturnAction]
  // dispatch = (a: Action | AsyncAction) => any
  type Dispatch[Action <: js.Any, AsyncAction <: js.Any, ReturnAction <: js.Any] = js.Function1[Action | AsyncAction, ReturnAction]

  // type ActionCreator = (...args: any) => Action | AsyncAction

  @js.native
  trait MiddlewareAPI[
      Action <: js.Any,
      AsyncAction <: js.Any,
      ReturnAction <: js.Any,
      State <: js.Any
  ] extends js.Object {
    val dispatch: Dispatch[Action,AsyncAction,ReturnAction] = js.native
    val getState: js.Function0[State] = js.native
  }

  @js.native
  type Middleware[
    Action <: js.Any,
    AsyncAction <: js.Any,
    ReturnAction <: js.Any,
    State <: js.Any
  ] = MiddlewareAPI[Action,AsyncAction,ReturnAction,State] => Dispatch[State] => Dispatch[State]

  @js.native
  trait Store[
      Action <: js.Any,
      AsyncAction <: js.Any,
      ReturnAction <: js.Any,
      State <: js.Any
  ] extends js.Object {
    def dispatch( action: Action|AsyncAction): ReturnAction = js.native
    def getState(): State = js.native
    def subscribe( listener: js.Function0[Unit]): js.Function0[Unit]  = js.native
    def replaceReducer(reducer: Reducer[State,Action,State]): Unit = js.native
  }

  // type StoreCreator = (reducer: Reducer, preloadedState: ?State) => Store
  type StoreCreator[
      Action <: js.Any,
      AsyncAction <: js.Any,
      ReturnAction <: js.Any,
      State <: js.Any
  ] = js.Function2[Reducer[State,Action], State, Store[Action,AsyncAction,ReturnAction,State]]

  // type StoreEnhancer = (next: StoreCreator) => StoreCreator
  type StoreEnhancer[
      Action <: js.Any,
      AsyncAction <: js.Any,
      ReturnAction <: js.Any,
      State <: js.Any
  ] = js.Function1[ StoreCreator[Action,AsyncAction,ReturnAction,State], StoreCreator[Action,AsyncAction,ReturnAction,State]]

}
