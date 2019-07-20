package com.github.thebridsk.bridge.client.test

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.test._
import com.github.thebridsk.bridge.client.pages.hand.ViewVulnerability
import com.github.thebridsk.bridge.data.bridge.Vulnerability
import com.github.thebridsk.bridge.data.bridge.NotVul
import com.github.thebridsk.bridge.data.bridge.Vul
//import org.scalajs.jquery.{ jQuery => _, _ }
import com.github.thebridsk.bridge.client.test.utils.jQuery
import com.github.thebridsk.bridge.data.Round
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.client.routes.AppRouter.AppPage
import com.github.thebridsk.bridge.client.pages.HomePage
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoModule.PlayChicago2
import com.github.thebridsk.bridge.client.pages.chicagos.ChicagoRouter.ListView
import com.github.thebridsk.bridge.data.js.SystemTimeJs
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxResult
import com.github.thebridsk.bridge.client.test.utils.StartLogging
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.source.SourcePosition
import japgolly.scalajs.react.extra.router.BaseUrl
import com.github.thebridsk.bridge.client.routes.TestBridgeRouter
import japgolly.scalajs.react.extra.router.Path
import com.github.thebridsk.bridge.client.routes.AppRouter.Home
import com.github.thebridsk.bridge.client.pages.duplicate.PageNewDuplicate
import com.github.thebridsk.bridge.client.pages.duplicate.PageNewDuplicateInternal

object MyTest {
  val log = Logger("bridge.MyTest")
}

/**
 * @author werewolf
 */
class MyTest extends FlatSpec with MustMatchers {
  import MyTest._
  com.github.thebridsk.bridge.client.modules.Loader.init
  com.github.thebridsk.bridge.client.test.utils.Loader.init

  SystemTimeJs()
  AjaxResult.setEnabled(false)      // disable server communications when running this test suite

  StartLogging.start()

  behavior of "MyTest in bridgescorer-client"

  it should "show that Ajax is disabled" in {
    AjaxResult.isEnabled mustBe Some(false)
  }

  /**
   * A BridgeRouter for testing
   * @param base the base url, example: http://localhost:8080/html/index-fastopt.html
   */
  class TestPageBridgeRouter( base: BaseUrl ) extends TestBridgeRouter[AppPage](base) {

    private var callRefresh: Boolean = false
    private var callSet: Option[AppPage] = None

    def isRefreshCalled = callRefresh
    def getSetPage = callSet

    def resetRouter = {
      callRefresh = false
      callSet = None
    }

    def home: TagMod = setOnClick(Home)

    def refresh: Callback = Callback {
      callRefresh = true
    }
    def set( page: AppPage ): Callback = Callback {
      callSet = Some(page)
    }

    def pathFor(target: AppPage): Path = target match {
      case Home => Path("")
      // TODO implement other pages
      case _ => ???
    }
  }

  it should "work with jQuery" in {

    val router = new TestPageBridgeRouter( BaseUrl("http://test.example.com/index.html") )

    def selectedPage = router.getSetPage

    log.warning("Rendering homepage into document")
    val component = ReactTestUtils renderIntoDocument HomePage(router)
    log.warning("Done rendering homepage into document")
    val y = component.getDOMNode.asMounted().asElement()
    log.info("TestJQuery: ReactDOM.findDOMNode(component) is "+y)
    val jq = jQuery
    log.info("TestJQuery: jquery is "+jQuery)
    val xx = jq(y)
  }

  it should "work with HomePage" in {

    val router = new TestPageBridgeRouter( BaseUrl("http://test.example.com/index.html") )

    def selectedPage = router.getSetPage

    ReactTestUtils.withRenderedIntoDocument(HomePage( router ) ) { m =>
      val e = m.getDOMNode.asMounted().asElement()

      val jv = new ReactForJQuery(e)

      jv.show()  // (view.jqueryComponent.context)

      val buttons = jv.jquery("#ChicagoList2")
      assert( buttons.length == 1 )

      val b = buttons(0)
      Simulation.click run b

      myassert( selectedPage.get, PlayChicago2(ListView), "Chicago button was not hit")

    }
  }

  it should "work with ViewVulnerability" in {
    object view {
      var nsVul: Vulnerability = NotVul
      var ewVul: Vulnerability = Vul

      def setNS( vul: Vulnerability ) = Callback {nsVul = vul}
      def setEW( vul: Vulnerability ) = Callback {ewVul = vul}
    }

    ReactTestUtils.withRenderedIntoDocument( ViewVulnerability( view.nsVul, view.ewVul, Some(view.setNS), Some(view.setEW) ) ) { m =>
      val jv = new ReactForJQuery(m.getDOMNode.asMounted().asElement())

      jv.show() // view.jqueryComponent.context)

      myassert( jv.jquery("#nsVul").text(), "Not Vul", "NS is not vulnerable")
      myassert( jv.jquery("#ewVul").text(), "Vul", "EW is vulnerable")

      myassert( jv.jquery("#nsVul").attr("class").getOrElse(""), "", "NS vulnerable button is not highlighted")
      myassert( jv.jquery("#ewVul").attr("class").getOrElse(""), "handVulnerable", "EW vulnerable button is highlighted")

      val buttons = jv.jquery("button")
      assert( buttons.length == 2 )

      myassert( buttons(0).innerHTML , "Not Vul", "first test for button 0")
      myassert( buttons(1).innerHTML , "Vul", "first test for button 1")

      myassert( view.nsVul, NotVul, "Initial state of nsVul is NotVul")
      myassert( view.ewVul, Vul, "Initial state of ewVul is Vul")

      val b = buttons(0)
      Simulation.click run b

      myassert( view.nsVul, Vul, "Final state of nsVul is Vul")
      myassert( view.ewVul, Vul, "Final state of ewVul is Vul")

    }
  }

//  it should "work with ViewPlayersTest" in {
//    object view extends ReactForJQuery {
//      var cancelCalled = false
//      var playerState: PlayerState = null
//      val rounds = Array[Round]()
//
//      val component = ReactTestUtils renderIntoDocument ViewPlayers( rounds, ok, cancel )
//
//      def ok( ps: PlayerState) = CallbackTo {
//        playerState = ps
//        log.info("Got players: "+ps)
//      }
//      def cancel() = CallbackTo {cancelCalled = true}
//    }
//
//    val h1elem = view.jquery("h1")
//    myassert( h1elem(1).innerHTML, "Enter players and identify first dealer", "Enter players page shows")
//
//    myassert(view.jquery("#Ok").is(":disabled"),true, "Check for OK disabled")
//    view.assertTrue(view.isDisabledTagById("button", "Ok"), "OK should be disabled until players are entered")
//    view.setInputFieldByName("North","Nancy")
//    view.setInputFieldByName("South","Sam")
//    view.setInputFieldByName("East","Ellen")
//    view.setInputFieldByName("West","Wayne")
//
////      val state = view.component.backend.getState()
////      log.info("State is "+state)
//
//    view.show()
//
//    myassert( view.jquery("input[name=\"North\"]").attr("value"), "Nancy", "North is Nancy")
//    myassert( view.jquery("input[name=\"South\"]").attr("value"), "Sam", "South is Sam")
//    myassert( view.jquery("input[name=\"East\"]").attr("value"), "Ellen", "East is Ellen")
//    myassert( view.jquery("input[name=\"West\"]").attr("value"), "Wayne", "West is Wayne")
//
//    myassert[Boolean](jQuery("#Ok").is(":disabled"),false, "Check for OK enabled")
//    view.assertTrue(!view.isDisabledTagById("button", "Ok"), "OK should not be disabled, players have been entered")
//
//    view.clickTagById("button", "Ok")
//
////      assert( view.playerState != null )
//    myassert( view.playerState.north , "Nancy", "Checking that north player is Nancy")
//    myassert( view.playerState.south , "Sam", "Checking that south player is Sam")
//    myassert( view.playerState.east , "Ellen", "Checking that east player is Ellen")
//    myassert( view.playerState.west , "Wayne", "Checking that west player is Wayne")
//
//  }

  def myassert[T]( got: T, expect: T, msg: String)( implicit pos: SourcePosition ) = {
    withClue(msg+" called from "+pos.line) {
      got mustBe expect
    }
//    try {
//      got mustBe expect
//    } catch {
//      case ae: AssertionError =>
//        log.info("******** ERROR ********** "+msg+": got=<"+got+">, expected=<"+expect+">")
//        throw ae
//      case x: Throwable => throw x
//    }
  }

  it should "convert a list of int to a string with ranges" in {
    val list = 2::3::4::Nil
    PageNewDuplicateInternal.intToString(list) mustBe "2-4"
  }

  it should "convert another list of int to a string with ranges" in {
    val list2 = 1::2::3::4::7::9::10::12::14::16::17::19::20::Nil
    PageNewDuplicateInternal.intToString(list2) mustBe "1-4, 7, 9-10, 12, 14, 16-17, 19-20"

  }
}
