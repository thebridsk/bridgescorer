//package com.github.thebridsk.bridge.test
//
//import japgolly.scalajs.react.vdom.html_<^._
//import japgolly.scalajs.react.test._
//import japgolly.scalajs.react._
//import com.github.thebridsk.bridge.pages.hand.ViewVulnerability
//import com.github.thebridsk.bridge.data.bridge.Vulnerability
//import com.github.thebridsk.bridge.data.bridge.NotVul
//import com.github.thebridsk.bridge.data.bridge.Vul
//import japgolly.scalajs.react.ReactDOM
//import japgolly.scalajs.react.Callback
//import japgolly.scalajs.react.CallbackTo
//import org.scalatest.FlatSpec
//import org.scalatest.MustMatchers
//
//object WorkingThingsOutComponents {
//
//  val componentA = ScalaComponent.builder[Int]("A")
//    .initialStateFromProps ( p => p )
//    .render_P(n => <.div(
//      (0 to n).map(j => componentB(s"$j² = ${j*j}")).toTagMod
//    ))
//    .build
//
//  val componentB = ScalaComponent.builder[String]("B")
//    .stateless
//    .render_P(s => <.div("Input is ", s))
//    .build
//
//}
//
///**
// * @author werewolf
// */
//class WorkingThingsOut extends FlatSpec with MustMatchers {
//  import WorkingThingsOutComponents._
//
//  behavior of "WorkingThingsOut in bridgescorer-client"
//
//  it should "vultest" in {
//    object callbacks extends ReactForJQuery {
//      var nsVul: Vulnerability = NotVul
//      var ewVul: Vulnerability = NotVul
//
//      val component = ReactTestUtils renderIntoDocument ViewVulnerability( nsVul, ewVul, Some(setNS), Some(setEW) )
//
//      def setNS( vul: Vulnerability ) = CallbackTo {nsVul = vul}
//      def setEW( vul: Vulnerability ) = CallbackTo {ewVul = vul}
//    }
//
//    import callbacks._
//    val buttons = callbacks.jquery("button")
//    assert( buttons.length == 2 )
//
//    buttons(0).innerHTML mustBe "Not Vul"
//    buttons(1).innerHTML mustBe "Not Vul"
//
//    assert( callbacks.nsVul == NotVul)
//    assert( callbacks.ewVul == NotVul)
//
//    val b = buttons(0)
//    Simulation.click run b
//
//    assert( callbacks.nsVul == Vul)
//    assert( callbacks.ewVul == NotVul)
//
//    // the following doesn't happen since the button only calls the callback
//    // there is no internal state in the ViewVulnerability component
////    val buttons2 = callbacks.jquery("button")
////    assert( buttons2.length == 2)
//
////    buttons2(0).innerHTML mustBe "Vul"
////    buttons2(1).innerHTML mustBe "Not Vul"
//  }
//
//  it should "vultest2" in {
//    object callbacks extends ReactForJQuery {
//      var nsVul: Vulnerability = Vul
//      var ewVul: Vulnerability = NotVul
//
//      val component = ReactTestUtils renderIntoDocument ViewVulnerability( nsVul, ewVul, Some(setNS), Some(setEW) )
//
//      def setNS( vul: Vulnerability ) = CallbackTo {nsVul = vul}
//      def setEW( vul: Vulnerability ) = CallbackTo {ewVul = vul}
//    }
//
//    import callbacks._
//    val buttons = callbacks.jquery("button")
//    assert( buttons.length == 2 )
//
//    buttons(0).innerHTML mustBe "Vul"
//    buttons(1).innerHTML mustBe "Not Vul"
//
//    assert( callbacks.nsVul == Vul)
//    assert( callbacks.ewVul == NotVul)
//
//    val b = buttons(0)
//    Simulation.click run b
//
//    assert( callbacks.nsVul == NotVul)
//    assert( callbacks.ewVul == NotVul)
//
//    // the following doesn't happen since the button only calls the callback
//    // there is no internal state in the ViewVulnerability component
////    val buttons2 = callbacks.jquery("button")
////    assert( buttons2.length == 2)
//
////    buttons2(0).innerHTML mustBe "Not Vul"
////    buttons2(1).innerHTML mustBe "Not Vul"
//  }
//}
