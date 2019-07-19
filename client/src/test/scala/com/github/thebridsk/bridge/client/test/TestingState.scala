//package com.github.thebridsk.bridge.client.test
//
//import japgolly.scalajs.react.vdom.html_<^._
//import japgolly.scalajs.react.test._
//import japgolly.scalajs.react._
//import org.scalajs.dom.raw.HTMLInputElement
//import japgolly.scalajs.react.extra.LogLifecycle
//import org.scalatest.FlatSpec
//import org.scalatest.MustMatchers
//import com.github.thebridsk.bridge.data.js.SystemTimeJs
//
//object TestingStateComponent {
//
//  type CallbackOk = State=>Callback
//  type CallbackCancel = Callback
//
//  case class Props( v1: String, v2: String, ok: CallbackOk, cancel: CallbackCancel )
//
//  case class State( input1: String, input2: String ) {
//    override
//    def toString() = "State("+super.toString()+","+input1+","+input2+")"
//  }
//
//  class Backend(scope: BackendScope[Props, State]) {
//
//    def set1( e: ReactEventFromInput ) = e.extract(_.target.value)( text => {
//
//      scope.modState( s => {
//        val ns = s.copy( input1 = text)
//        println("TestingState.Backend.set1: new state "+ns)
//        ns
//      } )
////      val s = scope.state
////      val ns = s.copy( input1 = e.target.value)
////      scope.setState(ns)
////      println("TestingState.Backend.set1: new state "+ns)
//    })
//
//    def set2( e: ReactEventFromInput ) = e.extract(_.target.value)( text => {
////      scope.modState( s => s.copy( input1 = e.target.value) )
//      scope.modState( s => {
//        val ns = s.copy( input2 = text)
//        println("TestingState.Backend.set2: new state "+ns)
//        ns
//      } )
////      val s = scope.state.runNow()
////      val ns = s.copy( input2 = e.target.value)
////      scope.setState(ns)
////      println("TestingState.Backend.set2: new state "+ns)
//    })
//
//    private def state = scope.state
//
//    def ok( e: ReactEventFromHtml) = {
//      val s = state.runNow()
//      println("TestingState.Backend.ok: calling callback with state "+s)
//      scope.props.runNow.ok( s )
//    }
//
//    def newok( e: ReactEventFromHtml) = scope.props >>= { p => state >>= { s => p.ok(s) }}
//
//    def cancel() = {
//      println("TestingState.Backend.cancel: calling callback")
//      scope.props.runNow().cancel
//    }
//
//    def newcancel = scope.props <| { p =>
//      println("TestingState.Backend.cancel: calling callback")
//    } >>= { p =>
//      p.cancel
//    }
//
//    def render(props: Props, state: State) = {
//      println("TestingState.componentA.render.function called, props="+props+", state="+state+", backend="+this)
//      <.div(
//        <.input( ^.value:=state.input1, ^.onChange==>set1 ),
//        <.input( ^.value:=state.input2, ^.onChange==>set2 ),
//
//        <.button( ^.onClick ==> newok, ^.cls:="ok", "ok"),
//        <.button( ^.onClick --> newcancel, ^.cls:="cancel", "Cancel")
//      )
//    }
//
//  }
//
//}
//
///**
// * @author werewolf
// */
//class TestingState extends FlatSpec with MustMatchers {
//
//  SystemTimeJs()
//
//  import TestingStateComponent._
//
//  val componentA = ScalaComponent.builder[Props]("A")
//    .initialStateFromProps( p => {
//      val s = State(p.v1, p.v2 )
//      println("TestingState.componentA.initialStateP.function: "+s)
//      s
//    })
//    .backend( backendScope => {
//      println("TestingState.componentA.backend.function: "+backendScope+", state="+backendScope.state)
//      new Backend(backendScope)
//    } )
//    .renderBackend
////    .render((props,state,backend) => {
////      println("TestingState.componentA.render.function called, props="+props+", state="+state+", backend="+backend)
////      <.div(
////        <.input( ^.value:=state.input1, ^.onChange==>backend.set1 ),
////        <.input( ^.value:=state.input2, ^.onChange==>backend.set2 ),
////
////        <.button( ^.onClick ==> backend.ok, ^.cls:="ok", "ok"),
////        <.button( ^.onClick --> backend.cancel, ^.cls:="cancel", "Cancel")
////      )
////    } )
//    .configure(LogLifecycle.verbose)
//    .build
//
//  def A( v1: String, v2: String, ok: CallbackOk, cancel: CallbackCancel ) =
//    componentA(Props(v1,v2,ok,cancel))
//
//  behavior of "TestingState in bridgescorer-client"
//
//  it should "testinput" in {
//    object data extends ReactForJQuery {
//      var v1: String = null
//      var v2: String = null
//
//      var cancelled = false
//
//      val componentx = ReactTestUtils renderIntoDocument A( "", "a", ok, cancel)
//
//      def ok( state: State ) = CallbackTo {
//        v1=state.input1
//        v2=state.input2
//        println("data.ok called: "+state)
//      }
//      def cancel() = CallbackTo {
//        cancelled = true
//        println("data.cancel called")
//      }
//    }
//
//    val buttons = data.jquery("input")
//    buttons.length mustBe 2
//
//    val b0 = buttons(0)
//    val n0 = b0
//    val i0 = n0.asInstanceOf[HTMLInputElement]
//    i0.value = "hello"
//    SimEvent.Change("hello") simulate b0
//
//    println("After ChangeEventData(hello)")
//
//    val backend = data.component.backend
//    println("State after hello: "+ data.component.backend.state)
//
//    val b1 = buttons(1)
//    val n1 = b1
//    val i1 = n1.asInstanceOf[HTMLInputElement]
//    i1.value = "goodbye"
//    SimEvent.Change("goodbye") simulate b1
//
//    println("State after goodbye: "+ data.component.state)
//
//    val buttonok = data.jquery("button.ok")(0)
//    Simulation.click run buttonok
//
//    assert(!data.cancelled, "Must not be cancelled")
//    data.v1 mustBe "hello"
//    data.v2 mustBe "goodbye"
//  }
//
//  it should "testcancel" in {
//    object data extends ReactForJQuery {
//      var v1: String = null
//      var v2: String = null
//
//      var cancelled = false
//
//      val component = ReactTestUtils renderIntoDocument A( "", "a", ok, cancel)
//
//      def ok( state: State ) = CallbackTo {
//        v1=state.input1
//        v2=state.input2
//        println("data.ok called: "+state)
//      }
//      def cancel() = CallbackTo {
//        cancelled = true
//        println("data.cancel called")
//      }
//    }
//
//    val buttons = data.jquery("input")
//    buttons.length mustBe 2
//
//    val b0 = buttons(0)
//    val n0 = b0
//    val i0 = n0.asInstanceOf[HTMLInputElement]
//    i0.value = "hello"
//    SimEvent.Change("hello") simulate b0
//
//    println("After ChangeEventData(hello)")
//
//    println("State after hello: "+ data.component.backend.state)
//
//    val b1 = buttons(1)
//    val n1 = b1
//    val i1 = n1.asInstanceOf[HTMLInputElement]
//    i1.value = "goodbye"
//    SimEvent.Change("goodbye") simulate b1
//
//    println("State after goodbye: "+ data.component.state)
//
//    val buttoncancel = data.jquery("button.cancel")(0)
//    Simulation.click run buttoncancel
//
//    assert(data.cancelled, "must be cancelled")
//  }
//}
