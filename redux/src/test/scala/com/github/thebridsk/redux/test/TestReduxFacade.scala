package com.github.thebridsk.redux.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.redux._
import scala.scalajs.js
import scala.scalajs.js.JSON

import com.github.thebridsk.redux._
import com.github.thebridsk.redux.thunk.Thunk
import com.github.thebridsk.redux.thunk.ThunkWithArgs
import com.github.thebridsk.bridge.clientcommon.javascript.ObjectToString
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.utilities.logging.js.JsConsoleHandler

object TestReduxFacade {
  JsConsoleHandler.init()
  val log: Logger = Logger("test.redux")
}
import TestReduxFacade.log
case class TestAddState( val value: Int = 0 ) {
  // def copy( value: Int = this.value ) = new TestAddState(value)
}

object TestAddState {
  val reducer: Reducer[TestAddState] = (state: js.UndefOr[TestAddState], action: NativeAction[Action]) => {
    val s = state.getOrElse(TestAddState(0))
    log.info( s"addOne reducer: state=${s}, action=${JSON.stringify(action)}")
    val ad = action.asInstanceOf[js.Dynamic]
    log.info(s"Action=${com.github.thebridsk.bridge.clientcommon.javascript.ObjectToString.dynToString(ad, "  ")}")
    val ns = action.actiontype match {
      case TestActionInc.actiontype =>
        log.info("Doing inc")
        s.copy( value = s.value+1)
      case TestActionDec.actiontype =>
        log.info("Doing dec")
        s.copy( value = s.value-1)
      case _ =>
        action.action.map { a =>
          a match {
            case TestActionIncN(n,_) =>
              log.info("Doing incN")
              s.copy( value = s.value+n)
            case TestActionDecN(n,_) =>
              log.info("Doing decN")
              s.copy( value = s.value-n)
            case _ =>
              log.info("No change")
              s
          }
        }.getOrElse(s)
    }
    ns
  }

}

object TestActionInc extends Action { val actiontype = "inc"}
object TestActionDec extends Action { val actiontype = "dec"}

case class TestActionIncN(val n: Int, actiontype: String = "incN") extends Action
case class TestActionDecN(val n: Int, actiontype: String = "decN") extends Action

object DelayedActions {
  def delayInc: com.github.thebridsk.redux.thunk.Function[TestState] = {

    log.info("Setting up delayInc")

    ( dispatch, getState, extraArgs) => {
      val wa = TestActionInc
      log.info(s"extraArgs is ${ObjectToString.objToString(extraArgs, "  ")}")
      log.info(s"In delayInc, dispatching ${wa}")
      val r = dispatch( wa )
      log.info("In delayInc, done")

    }
  }

}

object TestReducers extends js.Object {
  val addOne = TestAddState.reducer
}

@js.native
trait TestState extends State {
  val addOne: TestAddState = js.native
}
object TestState {
  def apply(as: TestAddState): TestState = {
    js.Dictionary(
      "addOne" -> as
    ).asInstanceOf[TestState]
  }
}

class TestReduxFacade extends AnyFlatSpec with Matchers {

  val actionInc = NativeAction(TestActionInc)
  val actionDec = NativeAction(TestActionDec)

  log.info("I'm here 0")

  behavior of "The Redux facade"

  it should "Dispatch actions" in {
    log.info("I'm here 1")
    val reducer = Redux.combineReducers[TestState](TestReducers.asInstanceOf[js.Dictionary[Reducer[State]]])
    val store: Store[TestState] = Redux.createStore(
      reducer = reducer,
      enhancer = Redux.applyMiddleware(StateLogger.logger[TestState])
    )

    val stateDict = store.getState().asInstanceOf[js.Dictionary[js.Any]]
    val stateJson = JSON.stringify(stateDict)

    withClue(s"State is ${stateJson}") {

      store.getState().addOne.value mustBe 0

      store.dispatch( TestActionIncN(2) )
      withClue(s"After Inc, state is ${stateJson}") {
        store.getState().addOne.value mustBe 2
      }

      store.dispatch( TestActionDec )
      store.getState().addOne.value mustBe 1

    }

  }

  it should "Listen for Store changes" in {
    log.info("I'm here 2")
    val reducer = Redux.combineReducers[TestState](TestReducers.asInstanceOf[js.Dictionary[Reducer[State]]])
    val store: Store[TestState] = Redux.createStore(reducer = reducer, preloadedState = TestState(TestAddState(2)))

    var listenCalled = false
    var listenState: Option[TestState] = None
    def listener(): Unit = {
      listenCalled = true
      listenState = Option(store.getState())
    }

    val unsubscribe = store.subscribe(listener _)

    val stateDict = store.getState().asInstanceOf[js.Dictionary[js.Any]]
    val stateJson = JSON.stringify(stateDict)



    withClue(s"State is ${stateJson}") {

      store.getState().addOne.value mustBe 2

      store.dispatch( TestActionDecN(2) )
      withClue(s"After Inc, state is ${stateJson}") {
        store.getState().addOne.value mustBe 0
      }

      listenCalled mustBe true
      listenState.get.addOne.value mustBe 0

    }

  }

  it should "Dispatch action function" in {
    log.info("I'm here 3")
    val reducer = Redux.combineReducers[TestState](TestReducers.asInstanceOf[js.Dictionary[Reducer[State]]])
    log.info(s"thunk is ${ObjectToString.objToString(Thunk.thunk)}")
    val store: Store[TestState] = Redux.createStore(
      reducer = reducer,
      enhancer = Redux.applyMiddleware[TestState](
        StateLogger.logger[TestState],
        Thunk.thunk,
      )
    )

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

  it should "Dispatch action function with args" in {
    log.info("I'm here 4")
    val reducer = Redux.combineReducers[TestState](TestReducers.asInstanceOf[js.Dictionary[Reducer[State]]])

    log.info("I'm here 5")
    try {

      val store: Store[TestState] = Redux.createStore(
        reducer = reducer,
        enhancer = Redux.applyMiddleware[TestState](
          ThunkWithArgs.withExtraArgument(js.Dictionary[js.Any]("xxx"->"yyy"))
        )
      )

      val stateDict = store.getState().asInstanceOf[js.Dictionary[js.Any]]
      val stateJson = JSON.stringify(stateDict)

      withClue(s"State is ${stateJson}") {

        store.getState().addOne.value mustBe 0

        store.dispatch( DelayedActions.delayInc )
        withClue(s"After Inc, state is ${stateJson}") {
          store.getState().addOne.value mustBe 1
        }
      }
    } catch {
      case x: Exception =>
        log.info(s"Exception in thunking with args: ${x}")
        // x.printStackTrace()
        fail(s"Exception in thunking with args: ${x}")
    }
  }
}
