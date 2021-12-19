// package com.github.thebridsk.bridge.client.test

// import com.github.thebridsk.utilities.time.js.SystemTimeJs
// import org.scalatest.flatspec.AnyFlatSpec
// import org.scalatest.matchers.must.Matchers
// import com.github.thebridsk.bridge.client.routes.AppRouter
// import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoModule
// import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateModule
// import com.github.thebridsk.bridge.client.pages.rubber.RubberModule
// import japgolly.scalajs.react.test.ReactTestUtils
// import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo

// class TestRouter extends AnyFlatSpec with Matchers {

//   SystemTimeJs()

//   behavior of "TestRouter in bridgescorer-client"

//   it should "Create a router" in {

//     BridgeDemo.setDemo(true)
//     val router = AppRouter(ChicagoModule, DuplicateModule, RubberModule).router()
//     ReactTestUtils.withRenderedIntoDocument( router ) { r =>
//       val e = r.toString // r.getDOMNode.toHtml.get.outerHTML
//       println( s"${e}")
//     }
//   }
// }
