package com.example.test.selenium

import com.example.data.bridge.ContractTricks
import com.example.data.bridge.ContractSuit
import com.example.data.bridge.PlayerPosition
import com.example.data.bridge.MadeOrDown
import com.example.data.bridge.ContractDoubled
import org.scalactic.source.Position
import org.openqa.selenium.WebDriver
import utils.logging.Logger

trait ChicagoUtils {
  import org.scalatest.MustMatchers._
  import com.example.test.util.EventuallyUtils._
  import com.example.test.util.ParallelUtils._
  import com.example.test.pages.PageBrowser._
  import org.scalatest.concurrent.Eventually
  import Eventually.{ patienceConfig => _, _ }

  def assertTotals( players: String* )( scores: Int*)
                  (implicit webDriver: WebDriver, config: PatienceConfig, pos: Position)= {

    players.length mustBe scores.length

    for ( p <- 1 to players.length ) {
      withClue("Testing Player"+p) {
        val name = withClue("Failed to get name") {
          eventually( find(id("Player"+p)).text )
        }
        val total = withClue("Player"+p+" is "+name) {
           eventually( find(id("Total"+p)).text )
        }

        val entry = (players zip scores).find( e => e._1==name)
        withClue("Looking for "+name+" with total "+total) {
          withClue("Not found") { entry mustBe 'defined }
          withClue("Not correct total") { entry.get._2.toString() mustBe total }
        }

      }
    }

  }

  def eventuallyFindAndClickButton( bid: String )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position) = eventually {
    click on id(bid)
  }

  def enterHand( contractTricks: ContractTricks,
                 contractSuit: ContractSuit,
                 contractDoubled: ContractDoubled,
                 declarer: PlayerPosition,
                 madeOrDown: MadeOrDown,
                 tricks: Int,
                 dealer: Option[String] = None
               )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position) = {
    eventually { find(xpath("//h6[3]/span")).text mustBe "Enter Hand" }

    dealer.foreach( dealerName => eventually { find(id("Dealer")).text } mustBe dealerName )

    contractTricks match {
      case ContractTricks(0) =>
        eventuallyFindAndClickButton("CTPassed")
      case _ =>
        eventuallyFindAndClickButton("CT"+contractTricks.tricks)
        eventuallyFindAndClickButton("CS"+contractSuit.suit)
        eventuallyFindAndClickButton("Doubled"+contractDoubled.doubled)
        eventuallyFindAndClickButton("Dec"+declarer.pos)
        eventuallyFindAndClickButton(madeOrDown.forScore)
        eventuallyFindAndClickButton("T"+tricks)
    }
    tcpSleep(1)
    eventuallyFindAndClickButton("Ok")
  }

  val urlprefix = TestServer.getAppPageUrl("chicago/")

  /**
   * @return the duplicate id of the match
   */
  def checkForChicago()(implicit webDriver: WebDriver): Option[String] = {
    val cUrl = currentUrl
    ChicagoUtils.testlog.fine( "currentUrl: "+cUrl )
    ChicagoUtils.testlog.fine( "prefix    : "+urlprefix)
    if (cUrl.startsWith(urlprefix)) {
      val idm = cUrl.substring(urlprefix.length())
      val i = idm.indexOf('/')
      val id = if (i<0) idm
               else idm.substring(0, i)
      if (id.startsWith("C")) Some( id )
      else None
    } else {
      None
    }
  }

}

object ChicagoUtils extends ChicagoUtils {

  private val testlog = Logger[ChicagoUtils]

}
