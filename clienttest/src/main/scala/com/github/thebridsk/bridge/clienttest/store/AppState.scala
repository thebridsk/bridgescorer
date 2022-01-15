package com.github.thebridsk.bridge.clienttest.store

import scala.scalajs.js
import com.github.thebridsk.redux.State
import com.github.thebridsk.redux.Reducer
import com.github.thebridsk.redux.Store
import com.github.thebridsk.redux.Redux
import com.github.thebridsk.redux.StateLogger



@js.native
trait AppState extends State {
  val base: BaseState = js.native
}

object AppReducers extends State {
  def reducer(): js.Dictionary[Reducer[State]] =
    AppReducers.asInstanceOf[js.Dictionary[Reducer[State]]]

  val base: Reducer[BaseState] = BaseState.reducer _
}

object AppStore {

  val reducer = Redux.combineReducers[AppState](AppReducers.reducer())
  val store: Store[AppState] = Redux.createStore(
    reducer = reducer,
    enhancer = Redux.applyMiddleware(StateLogger.logger[AppState])
  )

}
