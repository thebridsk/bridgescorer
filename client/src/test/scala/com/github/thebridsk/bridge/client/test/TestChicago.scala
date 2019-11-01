//package com.github.thebridsk.bridge.client.test
//
//import japgolly.scalajs.react.vdom.html_<^._
//import japgolly.scalajs.react.test._
//import com.github.thebridsk.bridge.pages.hand.ViewVulnerability
//import com.github.thebridsk.bridge.data.bridge.Vulnerability
//import com.github.thebridsk.bridge.data.bridge.NotVul
//import com.github.thebridsk.bridge.data.bridge.Vul
//import org.scalajs.jquery.{ jQuery => _, _ }
//import com.github.thebridsk.bridge.client.test.utils.jQuery
//import com.github.thebridsk.bridge.data.Round
//import japgolly.scalajs.react._
//import com.github.thebridsk.bridge.data.bridge._
//import com.github.thebridsk.bridge.rest.RestClient
//import com.github.thebridsk.bridge.routes.AppRouter.AppPage
//import com.github.thebridsk.bridge.pages.HomePage
//import org.scalatest.FlatSpec
//import org.scalatest.MustMatchers
//import com.github.thebridsk.bridge.data.js.SystemTimeJs
//
///**
// * @author werewolf
// */
//class TestChicago extends FlatSpec with MustMatchers {
//
//  SystemTimeJs()
//  RestClient.setEnabled(false)      // disable server communications when running this test suite
//
//  behavior of "TestChicago in bridgescorer-client"
//
//  it should "be ignored" in {
//    println("TestChicago is being skipped since the code was reworked to use Router")
//  }
//
////  it should "work in PageChicago" in {
////    implicit object view extends ReactForJQuery {
////      var doneCalled = false
////
////      val component = ReactTestUtils renderIntoDocument PageChicago( done, "", false )
////
////      def done() = CallbackTo {doneCalled = true}
////    }
////    println("Created PageChicago")
////
////    import view._
////
////    try {
////      val reactdom = ReactDOM
////      println("reactdom is "+reactdom)
////      val domNode = reactdom.findDOMNode( view.component )
////      println("domNode is "+domNode )
////
////      val h1elem = view.jquery("h1")
////      myassert( h1elem(1).innerHTML, "Enter players and identify first dealer", "Enter players page shows")
////
////      show(ReactDOM.findDOMNode(component), "PageChicago ")
////    } catch {
////      case e: Throwable =>
////        println("PageChicago oops "+e.toString())
////        e.printStackTrace(System.out)
////        throw e
////    }
////    println("PageChicago Checking Ok is disabled")
//////    find(id("Ok")).get.isEnabled mustBe false
////    assert( findTagById("button", "Ok").disabled.getOrElse(false) )
//////    assert( !find(id("Ok")).get.isEnabled, "must be disabled")
////    println("PageChicago Ok is disabled")
////
////    show(ReactDOM.findDOMNode(component), "PageChicago Before entering names")
////
////    textField("North").value = "Nancy"
////    textField("South").value = "Sam"
////    textField("East").value = "Ellen"
////    textField("West").value = "Wayne"
////
//////    find(id("Ok")).get.isEnabled mustBe true
////    assert( !findTagById("button", "Ok").disabled.getOrElse(false) )
//////    assert( find(id("Ok")).get.isEnabled, "must be enabled")
////    println("Created PageChicago Ok is enabled")
////
////    println("Clicking on Ok")
////    click on id("Ok")
////    println("Clicked on Ok")
////
////    // PageHand is now displaying the first hand of the first round.
////
////    checkPlayer( "td", "North", "Nancy", view)
////    checkPlayer( "td", "South", "Sam", view)
////    checkPlayer( "td", "East", "Ellen", view)
////    checkPlayer( "td", "West", "Wayne", view)
////
////    find(id("North")).get.text mustBe "Nancy"
////
////    myassert( view.jquery("#nsVul").text(), "Not Vul", "NS is not vulnerable")
////    myassert( view.jquery("#ewVul").text(), "Not Vul", "EW is not vulnerable")
////    myassert( view.jquery("#nsVul").is(":disabled"), true, "NS vulnerability can't be change")
////    myassert( view.jquery("#ewVul").is(":disabled"), true, "EW vulnerability can't be change")
////
////    myassert( view.jquery("#North").text() , "Nancy", "Checking that North is Nancy" )
////    myassert( view.jquery("#South").text() , "Sam", "Checking that South is Sam" )
////    myassert( view.jquery("#East").text() , "Ellen", "Checking that East is Ellen" )
////    myassert( view.jquery("#West").text() , "Wayne", "Checking that West is Wayne" )
////
////    view.show()
////
////    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne" ) _
////    enterHand(4,Spades,NotDoubled,North,Made,4)  // NS score 420
////    assertScore( Seq( 420, 420, 0, 0 ))
////    click on id("NextHand")
////    enterHand(4,Hearts,NotDoubled,East,Made,4)  // EW score 620
////    assertScore( Seq( 420, 420, 620, 620 ))
////    click on id("NextHand")
////    enterHand(5,Diamonds,NotDoubled,South,Made,5)  // NS score 600
////    assertScore( Seq( 1020, 1020, 620, 620 ))
////    click on id("NextHand")
////    enterHand(3,Clubs,NotDoubled,West,Made,4)  // EW score 130
////    assertScore( Seq( 1020, 1020, 750, 750 ))
////    click on id("NewRound")
////
////    click on id("West2")
////
////    click on id("PlayerEFirstDealer")
////    click on id("Ok")
////
//////      val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne" ) _
////    enterHand(4,Spades,NotDoubled,North,Made,4)  // NS score 420
////    assertScore( Seq( 1440, 1020, 1170, 750 ))
////    click on id("NextHand")
////    enterHand(4,Hearts,NotDoubled,East,Made,4)  // EW score 420
////    assertScore( Seq( 1440, 1440, 1170, 1170 ))
////    click on id("NextHand")
////    enterHand(5,Diamonds,NotDoubled,South,Made,5)  // NS score 400
////    assertScore( Seq( 1840, 1440, 1570, 1170 ))
////    click on id("NextHand")
////    enterHand(3,Clubs,NotDoubled,West,Made,4)  // EW score 130
////    assertScore( Seq( 1840, 1570, 1570, 1300 ))
////    click on id("NewRound")
////
////    click on id("PlayerWFirstDealer")
////    click on id("Ok")
////
////    find(id("North")).get.text mustBe "Nancy"
////    find(id("South")).get.text mustBe "Wayne"
////    find(id("East")).get.text mustBe "Ellen"
////    find(id("West")).get.text mustBe "Sam"
////
//////      val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne" ) _
////    enterHand(4,Spades,NotDoubled,North,Made,4)  // NS score 420
////    assertScore( Seq( 2260, 1570, 1570, 1720 ))
////    click on id("NextHand")
////    enterHand(4,Hearts,NotDoubled,East,Made,4)  // EW score 420
////    assertScore( Seq( 2260, 1990, 1990, 1720 ))
////    click on id("NextHand")
////    enterHand(5,Diamonds,NotDoubled,South,Made,5)  // NS score 400
////    assertScore( Seq( 2660, 1990, 1990, 2120 ))
////    click on id("NextHand")
////    enterHand(3,Clubs,NotDoubled,West,Made,4)  // EW score 130
////    assertScore( Seq( 2660, 2120, 2120, 2120 ))
////    click on id("NewRound")
////
////    click on id("South1")
////    click on id("East3")
////    click on id("PlayerSFirstDealer")
////    click on id("Ok")
////
////    find(id("North")).get.text mustBe "Nancy"
////    find(id("South")).get.text mustBe "Ellen"
////    find(id("East")).get.text mustBe "Wayne"
////    find(id("West")).get.text mustBe "Sam"
////
//////      val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne" ) _
////    enterHand(4,Spades,NotDoubled,North,Made,4)  // NS score 420
////    assertScore( Seq( 3080, 2120, 2540, 2120 ))
////    click on id("NextHand")
////    enterHand(4,Hearts,NotDoubled,East,Made,4)  // EW score 620
////    assertScore( Seq( 3080, 2740, 2540, 2740 ))
////    click on id("NextHand")
////    enterHand(5,Diamonds,NotDoubled,South,Made,5)  // NS score 600
////    assertScore( Seq( 3680, 2740, 3140, 2740 ))
////    click on id("NextHand")
////    enterHand(3,Clubs,NotDoubled,West,Made,4)  // EW score 130
////    assertScore( Seq( 3680, 2870, 3140, 2870 ))
////    click on id("NewRound")
////
////    println("MyTest:PageChicagoTest Done")
////  }
//
//  def assertTotals( player1: String, player2: String, player3: String, player4: String)
//                  ( score1: Int, score2: Int, score3: Int, score4: Int)
//                  ( implicit view: ReactForJQuery ) = {
//
//      myassert( view.jquery("#Player1").text(), player1, "Player 1 is "+player1)
//      myassert( view.jquery("#Player2").text(), player2, "Player 2 is "+player2)
//      myassert( view.jquery("#Player3").text(), player3, "Player 3 is "+player3)
//      myassert( view.jquery("#Player4").text(), player4, "Player 4 is "+player4)
//
//      myassert( view.jquery("#Total1").text(), score1.toString(), "Player 1 ("+player1+") gets 420")
//      myassert( view.jquery("#Total2").text(), score2.toString(), "Player 2 ("+player2+") gets 420")
//      myassert( view.jquery("#Total3").text(), score3.toString(), "Player 3 ("+player3+") gets 0")
//      myassert( view.jquery("#Total4").text(), score4.toString(), "Player 4 ("+player4+") gets 0")
//
//  }
//
//  def enterHand( contractTricks: ContractTricks,
//                 contractSuit: ContractSuit,
//                 contractDoubled: ContractDoubled,
//                 declarer: PlayerPosition,
//                 madeOrDown: MadeOrDown,
//                 tricks: Int
//               )(
//                 implicit view: ReactForJQuery
//               ) = {
//    import view._
//    myassert( view.jquery("#VerifySectionHeader").text(), "Bridge Scorer:", "Must have PageHand showing")
//
//    println("EnterHand: Entering")
//
//    contractTricks match {
//      case ContractTricks(0) =>
//        click on id("CTPassed")
//      case _ =>
//        println("EnterHand: clicking on contract")
//        click on id("CS"+contractSuit.suit)
//        click on id("CT"+contractTricks.tricks)
//        click on id("Dec"+declarer.pos)
//        click on id("Doubled"+contractDoubled.doubled)
//        click on id(madeOrDown.forScore)
//        click on id("T"+tricks)
//    }
//
//    println("EnterHand: clicking ok")
//    click on id("Ok")
//  }
//
//  def checkPlayer( tag: String, pos: String, name: String, view: ReactForJQuery ) = {
//      val north = view.findTagById(tag, pos)
//      val northval = north.innerHTML
//      assert( northval == name)
//  }
//
//  def myassert[T]( got: T, expect: T, msg: String) = {
//    withClue(msg) {
//      got mustBe expect
//    }
////    try {
////      assert( got == expect )
////    } catch {
////      case ae: AssertionError =>
////        println("******** ERROR ********** "+msg+": got=<"+got+">, expected=<"+expect+">")
////        throw ae
////      case x: Throwable => throw x
////    }
//  }
//}
