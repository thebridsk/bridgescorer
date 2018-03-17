package com.example.test.selenium

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import org.openqa.selenium.WebDriver
import org.scalatest.selenium.Firefox
import org.scalatest.BeforeAndAfterAll
import org.scalatest._
import org.openqa.selenium._
import org.scalatest.concurrent.Eventually
import java.util.concurrent.TimeUnit
import com.example.Server
import scala.concurrent.Await
import com.example.data.bridge._
import com.example.backend.BridgeServiceInMemory
import com.example.backend.BridgeService
import org.scalatest.time.Span
import org.scalatest.time.Millis
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.safari.SafariDriver
import com.example.data.bridge._
import scala.collection.JavaConversions._
import scala.util.Failure
import scala.concurrent._
import ExecutionContext.Implicits.global
import utils.logging.Logger
import java.util.logging.Level
import org.scalactic.source.Position
import com.example.data.util.Strings
import com.example.test.util.NoResultYet
import com.example.test.util.EventuallyUtils
import com.example.test.util.ParallelUtils
import org.scalatest.concurrent.Eventually
import com.example.pages.bridge.HomePage
import com.example.pages.duplicate.ListDuplicatePage
import com.example.pages.duplicate.NewDuplicatePage
import com.example.pages.duplicate.MovementsPage
import com.example.pages.duplicate.BoardSetsPage
import com.example.pages.duplicate.ScoreboardPage
import com.example.pages.duplicate.TablePage
import com.example.pages.duplicate.TablePage.EnterNames
import com.example.pages.duplicate.TableEnterScorekeeperPage
import com.example.pages.GenericPage
import com.example.pages.duplicate.HandPage
import com.example.test.TestStartLogging
import com.example.pages.duplicate.BoardPage
import com.example.pages.duplicate.TablePage.SelectNames
import com.example.pages.duplicate.TablePage.Hands
import com.example.pages.duplicate.TableSelectScorekeeperPage
import com.example.pages.duplicate.Team
import com.example.pages.Page.AnyPage
import com.example.pages.duplicate.EnterHand
import com.example.pages.duplicate.AllHandsInMatch
import com.example.pages.duplicate.HandsOnBoard
import com.example.pages.duplicate.OtherHandNotPlayed
import com.example.pages.duplicate.OtherHandPlayed
import com.example.pages.duplicate.TeamScoreboard
import com.example.pages.duplicate.HandDirectorView
import com.example.pages.duplicate.HandCompletedView
import com.example.pages.duplicate.HandTableView
import java.net.URL
import com.example.data.MatchDuplicate
import scala.io.Source
import scala.io.Codec
import com.example.backend.resource.FileIO
import com.example.pages.duplicate.PeopleRow
import com.example.data.BoardSet
import com.example.pages.duplicate.TablePage.MissingNames
import com.example.pages.duplicate.TableEnterMissingNamesPage
import com.example.test.util.MonitorTCP
import com.example.test.util.HttpUtils
import com.example.test.util.HttpUtils.ResponseFromHttp
import com.example.backend.StoreMonitor
import com.example.data.websocket.Protocol
import com.example.data.websocket.Protocol.StartMonitor
import com.example.backend.StoreMonitor.NewParticipant
import com.example.backend.StoreMonitor.ReceivedMessage
import com.example.data.websocket.DuplexProtocol
import com.example.backend.StoreMonitor.KillOneConnection
import akka.actor.Actor
import com.example.pages.duplicate.DuplicateResultPage.PlaceEntry
import com.example.pages.duplicate.DuplicateResultEditPage
import com.example.pages.duplicate.DuplicateResultPage

object DuplicateResultTest {

  val testlog = Logger[DuplicateResultTest]

  val screenshotDir = "target/screenshots/DuplicateResultTest"

  val team1 = Team( 1, " Nick", "Sam ")
  val team2 = Team( 2, " Ethan ", "Wayne")
  val team3 = Team( 3, "Ellen", "Wilma")
  val team4 = Team( 4, "Nora", "Sally")

  val teams = team1::team2::team3::team4::Nil

  val places = PlaceEntry( 1, 23, team1::Nil ) ::
               PlaceEntry( 2, 17, team2::team4::Nil ) ::
               PlaceEntry( 4, 15, team3::Nil ) ::
               Nil

  val allnames = teams.flatMap(t => t.one::t.two::Nil).map( n => n.trim() )

  val movement = "Armonk2Tables"
  val boardset = "ArmonkBoards"
}

/**
 * Test going from the table view, by hitting a board button,
 * to the names view, to the hand view.
 * @author werewolf
 */
class DuplicateResultTest extends FlatSpec with DuplicateUtils with MustMatchers with BeforeAndAfterAll with EventuallyUtils {
  import Eventually.{ patienceConfig => _, _ }
  import ParallelUtils._

  TestStartLogging.startLogging()

  import DuplicateResultTest._

  import scala.concurrent.duration._

  val SessionDirector = new DirectorSession()

//  val Session1 = new Session

  val timeoutMillis = 15000
  val intervalMillis = 500

  val backend = TestServer.backend

//  case class MyDuration( timeout: Long, units: TimeUnit )
  type MyDuration = Duration
  val MyDuration = Duration

  implicit val timeoutduration = MyDuration( 60, TimeUnit.SECONDS )

  val defaultPatienceConfig = PatienceConfig(timeout=scaled(Span(timeoutMillis, Millis)), interval=scaled(Span(intervalMillis,Millis)))
  implicit def patienceConfig = defaultPatienceConfig

  override
  def beforeAll() = {

    MonitorTCP.nextTest()
    try {
      import Session._
      // The sessions for the tables and complete is defered to the test that gets the home page url.
      waitForFutures( "Starting browser or server",
                      CodeBlock { SessionDirector.sessionStart(getPropOrEnv("SessionDirector")).setQuadrant(1) },
                      CodeBlock { TestServer.start() }
                      )
    } catch {
      case e: Throwable =>
        afterAll()
        throw e
    }
  }

  override
  def afterAll() = {
    waitForFuturesIgnoreTimeouts( "Stopping browsers and server",
                    CodeBlock { SessionDirector.sessionStop() },
                    CodeBlock { TestServer.stop() }
                    )
  }

  var dupid: Option[String] = None
  var boardSet: Option[BoardSet] = None

  behavior of "Duplicate test pages of Bridge Server"

  it should "go to the home page" in {
    import SessionDirector._

    tcpSleep(15)
    HomePage.goto.validate
  }

  it should "go to duplicate list page" in {
    import SessionDirector._

    HomePage.current.clickListDuplicateButton.validate
  }

  it should "allow creating a new duplicate match" in {
    import SessionDirector._

    ListDuplicatePage.current.clickNewDuplicateButton.validate
  }

  it should "create a new duplicate result match" in {
    import SessionDirector._

    val curPage = NewDuplicatePage.current

    curPage.clickCreateResultsOnly

    eventually {
      curPage.isCreateResultsOnly mustBe true
    }

    dupid = curPage.clickForResultsOnly(boardset, movement).validate.dupid
    dupid mustBe 'defined

    testlog.info(s"Duplicate id is ${dupid.get}")

  }

  it should "show selecting date" in {
    import SessionDirector._
    val page = DuplicateResultEditPage.current

    val played = page.findPlayed

    played.clickSelectDate

    played.getDays.isEmpty mustBe false

    page.esc
  }

  it should "enter the players and points" in {
    import SessionDirector._
    val page = DuplicateResultEditPage.current.validate

    tcpSleep(30)

    page.withClueAndScreenShot(screenshotDir, "EnterNames", "") {

      val x = page.findPlayed.value
      if (x == null || x == "") {
        testlog.severe(s"Played is blank")
        fail( "Played is blank.  It should have the current date")
      }

      page.isOKEnabled mustBe false

      places.foreach { pe =>
        pe.teams.foreach { team =>
          page.setName(0, team.teamid, 1, team.one)
          page.setName(0, team.teamid, 2, team.two)
          page.setPoints(0, team.teamid, pe.points)
        }
      }

      page.isOKEnabled mustBe true

      page.clickOK.validate
    }
  }

  it should "find a place table and validate it" in {
    import SessionDirector._
    val page = DuplicateResultPage.current

    page.findElemById("scoreboardplayers")

    page.checkPlaceTable(places:_*)
  }

  it should "go to edit page and cancel back" in {
    import SessionDirector._
    val page = DuplicateResultPage.current

    tcpSleep(30)

    val edit = page.clickEdit.validate

    edit.withClueAndScreenShot(screenshotDir, "ValidatingEdit", "See screenshot") {

      val x = edit.findPlayed.value
      if (x == null || x == "") {
        testlog.warning(s"Played is blank")
        Thread.sleep(60000)
      }

      places.foreach { pe =>
        pe.teams.foreach { team =>
          edit.getName(0, team.teamid, 1) mustBe team.one.trim()
          edit.getName(0, team.teamid, 2) mustBe team.two.trim()
          edit.getPoints(0, team.teamid) mustBe pe.points
        }
      }

      edit.isOKEnabled mustBe true

      edit.clickCancel.validate

    }
  }

  it should "go to summary page and see the game just entered" in {
    import SessionDirector._
    val page = DuplicateResultPage.current

    tcpSleep(30)

    val ld = page.clickSummary.validate

    val pg = ld.clickResult(dupid.get).validate

    pg.checkPlaceTable(places:_*)

    val ld2 = pg.clickSummary.validate
  }

}
