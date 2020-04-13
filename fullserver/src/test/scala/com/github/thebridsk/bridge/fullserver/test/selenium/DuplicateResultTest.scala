package com.github.thebridsk.bridge.fullserver.test.selenium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.BeforeAndAfterAll
import org.openqa.selenium._
import org.scalatest.concurrent.Eventually
import java.util.concurrent.TimeUnit
import com.github.thebridsk.bridge.server.Server
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.server.backend.BridgeServiceInMemory
import com.github.thebridsk.bridge.server.backend.BridgeService
import org.scalatest.time.Span
import org.scalatest.time.Millis
import com.github.thebridsk.bridge.data.bridge._
import scala.jdk.CollectionConverters._
import scala.util.Failure
import scala.concurrent._
import ExecutionContext.Implicits.global
import com.github.thebridsk.utilities.logging.Logger
import java.util.logging.Level
import org.scalactic.source.Position
import com.github.thebridsk.bridge.data.util.Strings
import com.github.thebridsk.bridge.server.test.util.NoResultYet
import com.github.thebridsk.bridge.server.test.util.EventuallyUtils
import com.github.thebridsk.bridge.server.test.util.ParallelUtils
import org.scalatest.concurrent.Eventually
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ListDuplicatePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.NewDuplicatePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.MovementsPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.BoardSetsPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ScoreboardPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage.EnterNames
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TableEnterScorekeeperPage
import com.github.thebridsk.browserpages.GenericPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.HandPage
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.BoardPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage.SelectNames
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage.Hands
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TableSelectScorekeeperPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.Team
import com.github.thebridsk.browserpages.Page.AnyPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.EnterHand
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.AllHandsInMatch
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.HandsOnBoard
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.OtherHandNotPlayed
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.OtherHandPlayed
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TeamScoreboard
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.HandDirectorView
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.HandCompletedView
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.HandTableView
import java.net.URL
import com.github.thebridsk.bridge.data.MatchDuplicate
import scala.io.Source
import scala.io.Codec
import com.github.thebridsk.utilities.file.FileIO
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.PeopleRow
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage.MissingNames
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TableEnterMissingNamesPage
import com.github.thebridsk.bridge.server.test.util.MonitorTCP
import com.github.thebridsk.bridge.server.test.util.HttpUtils
import com.github.thebridsk.bridge.server.test.util.HttpUtils.ResponseFromHttp
import com.github.thebridsk.bridge.server.backend.StoreMonitor
import com.github.thebridsk.bridge.data.websocket.Protocol
import com.github.thebridsk.bridge.server.backend.StoreMonitor.NewParticipant
import com.github.thebridsk.bridge.server.backend.StoreMonitor.ReceivedMessage
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol
import com.github.thebridsk.bridge.server.backend.StoreMonitor.KillOneConnection
import akka.actor.Actor
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.DuplicateResultPage.PlaceEntry
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.DuplicateResultEditPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.DuplicateResultPage
import com.github.thebridsk.browserpages.Session
import com.github.thebridsk.bridge.server.test.util.TestServer

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

  val movement = "2TablesArmonk"
  val boardset = "ArmonkBoards"
}

/**
 * Test going from the table view, by hitting a board button,
 * to the names view, to the hand view.
 * @author werewolf
 */
class DuplicateResultTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll with EventuallyUtils {
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
                      CodeBlock { SessionDirector.sessionStart(getPropOrEnv("SessionDirector")).setPositionRelative(0,0).setSize(1024, 800) },
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
    dupid mustBe Symbol("defined")

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