package com.github.thebridsk.bridge.fullserver.test.selenium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import com.github.thebridsk.utilities.logging.Logger
import org.scalatest.time.Millis
import org.scalatest.time.Span
import java.util.concurrent.TimeUnit
import com.github.thebridsk.bridge.server.test.util.MonitorTCP
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.EnterNamesPage
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.HandPage
import org.scalactic.source.Position
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.SummaryPage
import org.scalatest.CancelAfterFailure
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage
import org.openqa.selenium.WebDriver
import com.github.thebridsk.browserpages.Session
import com.github.thebridsk.browserpages.PageBrowser
import scala.jdk.CollectionConverters._
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.ChicagoMatchTypeFour

object ChicagoDemoTestPages {

  val log: Logger = Logger[ChicagoDemoTestPages]()

  val player1 = "Naomi"
  val player2 = "Sebastion"
  val player3 = "Eloise"
  val player4 = "Wyatt"

  val players: List[String] = player1::player2::player3::player4::Nil

}

class ChicagoDemoTestPages extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll
    with CancelAfterFailure
{
  import com.github.thebridsk.browserpages.PageBrowser._
  import Eventually.{ patienceConfig => _, _ }
  import ChicagoDemoTestPages._

  import scala.concurrent.duration._

  val screenshotDir = "target/screenshots/ChicagoDemoTestPages"

  TestStartLogging.startLogging()

  val Session1 = new Session

  val SessionWatcher = new Session("watcher")

  val timeoutMillis = 15000
  val intervalMillis = 500

  val backend = TestServer.backend

  implicit val itimeout: PatienceConfig = PatienceConfig(timeout=scaled(Span(timeoutMillis, Millis)), interval=scaled(Span(intervalMillis,Millis)))

  val chicagoListURL: Option[String] = None
  val chicagoToListId: Option[String] = Some("Quit")

  implicit val timeoutduration: FiniteDuration = Duration( 60, TimeUnit.SECONDS )

  override
  def beforeAll(): Unit = {
    import com.github.thebridsk.bridge.server.test.util.ParallelUtils._

    MonitorTCP.nextTest()

    try {
      waitForFutures("Starting a browser or server",
                     CodeBlock { Session1.sessionStart().setPositionRelative(0,0).setSize(1100, 800)},
//                     CodeBlock { SessionWatcher.sessionStart().setQuadrant(2, 1100, 800)},
                     CodeBlock { TestServer.start() } )
    } catch {
      case e: Throwable =>
        afterAll()
        throw e
    }

  }

  override
  def afterAll(): Unit = {
    import com.github.thebridsk.bridge.server.test.util.ParallelUtils._

    waitForFuturesIgnoreTimeouts(
      "Stopping a browser or server",
      CodeBlock {
        watcherTab.foreach{ t =>
          log.fine("closing watcher tab")
          Session1.switchTo().window(t).close
        }
        mainTab.foreach { t =>
          log.fine("switching to main tab")
          Session1.switchTo().window(t)
        }
        log.fine("stopping session")
        Session1.sessionStop()
      },
      // CodeBlock { SessionWatcher.sessionStop() },
      CodeBlock { TestServer.stop() }
    )

  }

  var chicagoId: Option[String] = None   // eventually this will be obtained dynamically

  var mainTab: Option[String] = None
  var watcherTab: Option[String] = None

  behavior of "Chicago test of Bridge Server"

  it should "return a root page that has a title of \"The Bridge ScoreKeeper\"" in {
    import Session1._

    HomePage.demo.validate

    mainTab = Option(Session1.getWindowHandle())
    withClue("Determining main tab window handle") {
      mainTab.isDefined mustBe true
    }
  }

  it should "allow us to score a Chicago match" in {
    import Session1._

    chicagoId = Some( HomePage.current.clickNewChicagoButton.validate.chiid )

  }

  it should "start the watcher session on the created match" in {
    import Session1._

    val url = SummaryPage.getDemoUrl(chicagoId.get, None)
    val beforetabs = Session1.getWindowHandles().asScala
    PageBrowser.executeScript(s"window.open('$url','_blank');");
    val tabs = Session1.getWindowHandles().asScala --= beforetabs
    watcherTab = tabs.headOption
    withClue("Determining watcher tab window handle") {
      watcherTab.isDefined mustBe true
    }
    Session1.switchTo().window(mainTab.get)
  }

  it should "allow player names to be entered" in {
    import Session1._

    val p = EnterNamesPage.current
    p.enterPlayer(North, player1, true).
      enterPlayer(South, player2, true).
      enterPlayer(East, player3, true).
      enterPlayer(West, player4, true)

    p.setDealer(North)

    val h = p.clickOK.validate

  }

  /**
   * Enter the contract and click OK.
   * @param contractTricks
   * @param contractSuit
   * @param contractDoubled
   * @param declarer
   * @param madeOrDown
   * @param tricks
   * @param score check the score line
   * @param dealer check for dealer
   * @param patienceConfig
   * @param pos
   * @return the next page
   */
  def enterHand(
        h: HandPage,
        contractTricks: Int,
        contractSuit: ContractSuit,
        contractDoubled: ContractDoubled,
        declarer: PlayerPosition,
        madeOrDown: MadeOrDown,
        tricks: Int,
        nsVul: Vulnerability,
        ewVul: Vulnerability,
        score: String,
        dealer: String,
        round: Int,
        hand: Int,
        handScores: List[Int],
        roundScores: List[Int],
        totals: List[Int]
      )(implicit
          webDriver: WebDriver,
          pos: Position
      ): SummaryPage = {

    withClueAndScreenShot(screenshotDir, s"EnterHandRound${round}Hand${hand}", s"round ${round} hand ${hand}") {
      h.validate
      val sp = h.enterHand(contractTricks, contractSuit, contractDoubled, declarer, madeOrDown, tricks, nsVul, ewVul, Some(score), Some(dealer))
      sp.validate
      val scores = handScores.map{ s =>
        if (s<0) {
          "x"
        } else if (s==0) {
          ""
        } else {
          s.toString()
        }
      }
      val roundS = roundScores.map(s => s.toString())
      val totalsS = totals.map(s => s.toString())
      sp.checkHandScore(hand, players, scores, roundS)
      sp.checkTotalScore(round, players, roundS, totalsS)
      sp
    }
  }

  it should "play a round of 4 hands" in {
    import Session1._

    val h = HandPage.current(ChicagoMatchTypeFour)

    // N player1   S player2   E player3   W player4

    val sp = enterHand(h,4,Spades,NotDoubled,North,Made,4, NotVul, NotVul,
                       s"420 ${player1}-${player2}", player1, 0, 0,
                       List(420,420,0,0), List(420,420,0,0), List(420,420,0,0) )

  }

  it should "See the same on second browser" in {
    import Session1._
    Session1.switchTo().window(watcherTab.get)

    val handScores = List(420,420,0,0)
    val scores = handScores.map{ s =>
      if (s<0) {
        "x"
      } else if (s==0) {
        ""
      } else {
        s.toString()
      }
    }
    val roundScores = List(420,420,0,0)
    val totals = List(420,420,0,0)

    val roundS = roundScores.map(s => s.toString())
    val totalsS = totals.map(s => s.toString())

    val sp = SummaryPage.current(ChicagoMatchTypeFour).validate

    sp.checkHandScore(0, players, scores, roundS)
    sp.checkTotalScore(0, players, roundS, totalsS)

    log.fine("Done with test")
  }

  it should "Start second Chicago match and see names from first match" in {
    import Session1._
    Session1.switchTo().window(mainTab.get)

    val sp = SummaryPage.current(ChicagoMatchTypeFour).validate
    val hp = sp.clickHome.validate

    val np = hp.clickNewChicagoButton.validate

    val combo = np.getPlayerCombobox(North)
    combo.clickCaret
    eventually {
      val suggested = combo.suggestions.map(_.text)
      suggested must (contain.allOf(player1,player2,player3,player4) )
    }

  }

}
