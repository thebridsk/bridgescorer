package com.github.thebridsk.redux

import scala.scalajs.js
import scala.scalajs.js.annotation._


@js.native
@JSImport("redux", JSImport.Namespace)
object Redux extends js.Object {

  def applyMiddleware[
      Action <: js.Any,
      AsyncAction <: js.Any,
      ReturnAction <: js.Any,
      State <: js.Any
  ]: (Middleware*) => js.Function1[StoreCreator[Action,AsyncAction,ReturnAction,State],StoreCreator[Action,AsyncAction,ReturnAction,State]] = js.native

  def combineReducers( reducers: Reducers|js.Dictionary[js.Any] ): Reducer[js.Any,js.Any] = js.native

  def createStore[
      Action <: js.Any,
      AsyncAction <: js.Any,
      ReturnAction <: js.Any,
      State <: js.Any
  ](
    reducer: Reducer[State,Action],
    preloadedState: State,
    enhancer: (Middleware*) => js.Function1[StoreCreator[Action,AsyncAction,ReturnAction,State],StoreCreator[Action,AsyncAction,ReturnAction,State]] = js.native
  ): Store[Action,AsyncAction,ReturnAction,State] = js.native

  def createStore[
      Action <: js.Any,
      AsyncAction <: js.Any,
      ReturnAction <: js.Any,
      State <: js.Any
  ](
    reducer: Reducer[State,Action],
    preloadedState: State,
  ): Store[Action,AsyncAction,ReturnAction,State] = js.native

  def createStore[
      Action <: js.Any,
      AsyncAction <: js.Any,
      ReturnAction <: js.Any,
      State <: js.Any
  ](
    reducer: Reducer[State,Action],
  ): Store[Action,AsyncAction,ReturnAction,State] = js.native

  def createStore[
      Action <: js.Any,
      AsyncAction <: js.Any,
      ReturnAction <: js.Any,
      State <: js.Any
  ](
    reducer: Reducer[State,Action],
    enhancer: (Middleware*) => js.Function1[StoreCreator[Action,AsyncAction,ReturnAction,State],StoreCreator[Action,AsyncAction,ReturnAction,State]] = js.native
  ): Store[Action,AsyncAction,ReturnAction,State] = js.native

}
