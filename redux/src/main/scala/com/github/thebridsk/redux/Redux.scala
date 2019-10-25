package com.github.thebridsk.redux

import scala.scalajs.js
import scala.scalajs.js.|
import scala.scalajs.js.annotation._

import com.github.thebridsk.redux._

@js.native
@JSImport("redux", JSImport.Namespace)
object Redux extends js.Object {

  def applyMiddleware[
      Action,
      ReturnAction,
      State <: js.Any
  ]( middleware: Middleware[Action,ReturnAction,State]* ):  Enhancer[State, Action, ReturnAction] = js.native

  def combineReducers( reducers: Reducers|js.Dictionary[Reducer[js.Any,js.Any]] ): Reducer[js.Any,js.Any] = js.native

  def createStore[
      Action,
      ReturnAction,
      State <: js.Any,
      ReturnState <: js.Any
  ](
    reducer: Reducer[State,Action],
    preloadedState: State,
    enhancer: Enhancer[State,Action,ReturnAction]
  ): Store[Action,ReturnAction,State] = js.native

  def createStore[
      Action,
      ReturnAction,
      State <: js.Any,
      ReturnState <: js.Any
  ](
    reducer: Reducer[State,Action],
    preloadedState: State,
  ): Store[Action,ReturnAction,ReturnState] = js.native

  def createStore[
      Action,
      ReturnAction,
      State <: js.Any,
      ReturnState <: js.Any
  ](
    reducer: Reducer[State,Action],
  ): Store[Action,ReturnAction,ReturnState] = js.native

  type Enhancer[State <: js.Any, Action, ReturnAction] = js.Function1[js.Function, js.Function3[Reducer[State, Action], js.UndefOr[State], js.UndefOr[js.Function], Store[Action, ReturnAction, State]]]

  def createStore[
      Action,
      ReturnAction,
      State <: js.Any,
      ReturnState <: js.Any
  ](
    reducer: Reducer[State,Action],
    enhancer: Enhancer[State,Action,ReturnAction]
  ): Store[Action,ReturnAction,ReturnState] = js.native

}

@js.native
@JSImport("redux-thunk", JSImport.Default)
object Thunk extends js.Object {

  type Function[
    Action,
    ReturnAction,
    State <: js.Any
  ] = js.Function3[ js.Function1[WrappedAction[Action], WrappedAction[ReturnAction]], js.Function0[ js.UndefOr[State] ], js.UndefOr[js.Any], ReturnAction ]

  def apply(): Middleware[js.Any,js.Any,js.Any] = js.native

  val withExtraArgument: js.Function1[ js.UndefOr[js.Any], Middleware[js.Any,js.Any,js.Any] ] = js.native

}

