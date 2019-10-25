package com.github.thebridsk

import scala.scalajs.js
import scala.scalajs.js.|
import scala.language.implicitConversions

/**
 * Inspired by https://github.com/vegansk/scalajs-redux
 */
package object redux {
import scala.concurrent.Future

  // reducer = (state: S, action: A) => S
  type Reducer[State <: js.Any, Action ] = js.Function2[js.UndefOr[State],WrappedAction[Action],State]

  /**
    *
    */
  type Reducers = js.Object

  // baseDispatch = (a: Action) => Action
  type BaseDispatch[Action, ReturnAction] = js.Function1[WrappedAction[Action],WrappedAction[ReturnAction]]
  // dispatch = (a: Action | AsyncAction) => any
  type Dispatch[Action, ReturnAction] = js.Function1[WrappedAction[Action], WrappedAction[ReturnAction]]

  // type ActionCreator = (...args: any) => Action | AsyncAction

  @js.native
  trait WrappedAction[Action] extends js.Object {
    @js.annotation.JSName("type")
    val actionType: String = js.native
    val reduxAction: Action = js.native
  }

  object WrappedAction {

    implicit def wrapAction[Action]( act: Action ) = js.Dynamic.literal(
      `type` = "Action",
      reduxAction = act.asInstanceOf[js.Any]
    ).asInstanceOf[WrappedAction[js.Any]]

    implicit def wrapFutureAction[Action,ReturnAction,State <: js.Any]( act: Thunk.Function[Action,ReturnAction,State] ) = act.asInstanceOf[WrappedAction[js.Any]]
  }

  @js.native
  trait MiddlewareAPI[
      Action,
      ReturnAction,
      State <: js.Any
  ] extends js.Object {
    val dispatch: Dispatch[Action,ReturnAction] = js.native
    val getState: js.Function0[State] = js.native
  }

  @js.native
  type Middleware[
    Action,
    ReturnAction,
    State <: js.Any
  ] = js.Function1[MiddlewareAPI[Action,ReturnAction,State], js.Function1[Dispatch[Action,ReturnAction],Dispatch[Action,ReturnAction]] ]

  type Listener = js.Function0[Unit]
  type RemoveListener = js.Function0[Unit]

  @js.native
  trait Store[
      Action,
      ReturnAction,
      State <: js.Any
  ] extends js.Object {
    def dispatch( action: WrappedAction[Action]): WrappedAction[ReturnAction] = js.native
    def getState(): State = js.native
    def subscribe( listener: Listener): RemoveListener  = js.native
    def replaceReducer(reducer: Reducer[State,ReturnAction]): Unit = js.native
  }

  // type StoreCreator = (reducer: Reducer, preloadedState: ?State) => Store
  type StoreCreator[
      Action,
      ReturnAction,
      State <: js.Any
  ] = js.Function2[Reducer[State,Action], State, Store[Action,ReturnAction,State]]

  // type StoreEnhancer = (next: StoreCreator) => StoreCreator
  type StoreEnhancer[
      Action,
      ReturnAction,
      State <: js.Any
  ] = js.Function1[ StoreCreator[Action,ReturnAction,State], StoreCreator[Action,ReturnAction,State]]

}
