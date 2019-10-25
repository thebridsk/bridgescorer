package com.github.thebridsk.redux.test

import org.scalatest.AsyncFlatSpec
import org.scalatest.MustMatchers
import com.github.thebridsk.redux._
import scala.scalajs.js.annotation.ScalaJSDefined
import scala.scalajs.js
import scala.scalajs.js.JSON

class TestAddState( val value: Int = 0 ) extends js.Object {
  def copy( value: Int = this.value ) = new TestAddState(value)
}

object TestAddState {
  def apply( value: Int = 0 ) = new TestAddState(value)
}

case object TestActionInc { val x = "inc"}
case object TestActionDec { val x = "dec"}

object DelayedActions {
  def delayInc: Thunk.Function[js.Any,js.Any,TestAddState] = {

    println("Setting up delayInc")

    ( dispatch, getState, api) => {
      val wa = WrappedAction.wrapAction(TestActionInc)
      println(s"In delayInc, dispatching ${JSON.stringify(wa)}")
      val r = dispatch( wa )
      println("In delayInc, done")
      r
    }
  }

}

object TestReducers extends Reducers {
  val addOne: Reducer[TestAddState,js.Any] = (state: js.UndefOr[TestAddState], action: WrappedAction[_] ) => {
    val s = state.getOrElse(TestAddState(0))
    println( s"addOne reducer: state=${JSON.stringify(s)}, action=${JSON.stringify(action)}")
    val ns = action.reduxAction match {
      case TestActionInc => s.copy( value = s.value+1)
      case TestActionDec => s.copy( value = s.value-1)
      case _ => {
        println("No change")
        s
      }
    }
    ns
  }
}

@js.native
trait TestState extends js.Object {
  val addOne: TestAddState = js.native
}

class TestReduxFacade extends AsyncFlatSpec with MustMatchers {
  println("I'm here 1")

  behavior of "The Redux facade"

  it should "Dispatch actions" in {
    println("I'm here 2")
    val reducer = Redux.combineReducers(TestReducers)
    val store: Store[js.Any,js.Any,TestState] = Redux.createStore(reducer = reducer)

    val stateDict = store.getState().asInstanceOf[js.Dictionary[js.Any]]
    val stateJson = JSON.stringify(stateDict)

    withClue(s"State is ${stateJson}") {

      store.getState().addOne.value mustBe 0

      store.dispatch( TestActionInc )
      withClue(s"After Inc, state is ${stateJson}") {
        store.getState().addOne.value mustBe 1
      }

      store.dispatch( TestActionDec )
      store.getState().addOne.value mustBe 0

    }

  }

  it should "Dispatch action function" in {
    println("I'm here 3")
    val reducer = Redux.combineReducers(TestReducers)
    val store: Store[js.Any,js.Any,TestState] = Redux.createStore(reducer = reducer, enhancer = Redux.applyMiddleware( Thunk.withExtraArgument(js.undefined) ) )

    val stateDict = store.getState().asInstanceOf[js.Dictionary[js.Any]]
    val stateJson = JSON.stringify(stateDict)

    withClue(s"State is ${stateJson}") {

      store.getState().addOne.value mustBe 0

      store.dispatch( DelayedActions.delayInc )
      withClue(s"After Inc, state is ${stateJson}") {
        store.getState().addOne.value mustBe 1
      }
    }
  }
}
