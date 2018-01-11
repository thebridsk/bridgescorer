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
import com.example.pages.HomePage
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
import com.example.pages.duplicate.ScoreboardPage.PlaceEntry
import com.example.pages.duplicate.ScoreboardPage.PlaceEntry
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

object DuplicateTestPages2 {

  val testlog = Logger[DuplicateTestPages2]

  val team1 = Team( 1, " Nick", "Sam ")
  val team2 = Team( 2, " Ethan ", "Wayne")
  val team3 = Team( 3, "Ellen", "Wilma")
  val team4 = Team( 4, "Nora", "Sally")

  val allnames = (team1::team2::team3::team4::Nil).flatMap(t => t.one::t.two::Nil).map( n => n.trim() )
}

/**
 * Test going from the table view, by hitting a board button,
 * to the names view, to the hand view.
 * @author werewolf
 */
class DuplicateTestPages2 extends FlatSpec with DuplicateUtils with MustMatchers with BeforeAndAfterAll with EventuallyUtils with ParallelUtils {
  import Eventually.{ patienceConfig => _, _ }

  TestStartLogging.startLogging()

  import DuplicateTestPages2._
  import DuplicateTestPages.{ testlog => _, team1 => _, team2 => _, team3 => _, team4 => _, _ }

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
                      Future { SessionDirector.sessionStart(getPropOrEnv("SessionDirector")).setQuadrant(1) },
                      Future { TestServer.start() }
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
                    Future { SessionDirector.sessionStop() },
                    Future { TestServer.stop() }
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

  it should "create a new duplicate match" in {
    import SessionDirector._

    val curPage = NewDuplicatePage.current

    val boards = MovementsPage.getBoardsFromMovement(movement)

    testlog.info(s"Boards are $boards")

    dupid = curPage.click(boardset, movement).validate(boards).dupid
    dupid mustBe 'defined

    testlog.info(s"Duplicate id is ${dupid.get}")

    allHands.boardsets mustBe 'defined
  }

  var rounds: List[Int] = Nil

  it should "start on table 1" in {
    import SessionDirector._

    rounds = MovementsPage.getRoundsFromMovement(movement)

    ScoreboardPage.current.validate.clickTableButton(1).validate

  }

  it should "allow players names to be entered at both tables" in {
    tcpSleep(60)
    import SessionDirector._
    var sk = TablePage.current(EnterNames).validate(rounds).clickBoard(1,1).asInstanceOf[TableEnterScorekeeperPage].validate
    sk.isOKEnabled mustBe false
    sk = sk.enterScorekeeper(team1.one).esc.clickPos(North)
    sk.isOKEnabled mustBe true
    sk.findSelectedPos mustBe Some(North)
    var en = sk.clickOK.validate
    en.isOKEnabled mustBe false
    en = en.enterPlayer(South, team1.two).enterPlayer(East, team2.one)
    en.isOKEnabled mustBe false
    en = en.enterPlayer(West, team2.two).esc
    en.isOKEnabled mustBe true
    val hand = en.clickOK.asInstanceOf[HandPage].validate
  }

  it should "allow first round to be played at both tables" in {
    tcpSleep(60)
    import SessionDirector._
    val hand = HandPage.current
    hand.getScore mustBe ( "Missing required information", "", "Enter contract tricks" )
    hand.isOkEnabled mustBe false
    hand.getInputStyle mustBe Some("Yellow")
    val board = hand.enterHand( 1, 1, 1, allHands, team1, team2)
    board.checkBoardButtons(1,true,1).checkBoardButtons(1,false, 2, 3)
    val hand2 = board.clickUnplayedBoard(2).validate
    val board2 = hand2.enterHand( 1, 1, 2, allHands, team1, team2)
    board2.checkBoardButtons(2,true,1,2).checkBoardButtons(2,false, 3)
    val hand3 = board2.clickUnplayedBoard(3).validate
    val board3 = hand3.enterHand( 1, 1, 3, allHands, team1, team2)
    board3.checkBoardButtons(3,true,1,2,3).checkBoardButtons(3,false).clickScoreboard.validate
  }

  def selectScorekeeper( currentPage: ScoreboardPage,
                         table: Int, round: Int,
                         ns: Team, ew: Team,
                         scorekeeper: PlayerPosition,
                         mustswap: Boolean
                       )( implicit
                           webDriver: WebDriver
                       ) = {

    val tp = currentPage.clickTableButton(table).validate.setTarget(SelectNames)
    val ss = tp.clickRound(round).asInstanceOf[TableSelectScorekeeperPage].validate

    val sn = ss.verifyAndSelectScorekeeper(ns.one, ns.two, ew.one, ew.two, scorekeeper)
    sn.verifyNamesAndSelect(ns.teamid, ew.teamid, ns.one, ns.two, ew.one, ew.two, scorekeeper, mustswap).asInstanceOf[ScoreboardPage]
  }

  it should "allow selecting players for round 2" in {
    tcpSleep(10)
    import SessionDirector._
    val sb = selectScorekeeper(ScoreboardPage.current,1,2, team1, team2, East, false )
  }

  it should "allow second round to be played at both tables" in {
    tcpSleep(60)
    import SessionDirector._
    val sb = ScoreboardPage.current
    val hand = sb.clickBoardToHand(4).validate
    hand.setInputStyle("Prompt")
    val board = hand.onlyEnterHand( 1, 2, 4, allHands, team1, team2)
    board.checkBoardButtons(4,true,4).checkBoardButtons(4,false, 5, 6)
    val hand2 = board.clickUnplayedBoard(5).validate
    val board2 = hand2.onlyEnterHand( EnterHand( 1, 650,0,   2,0,   5,Spades,NotDoubled,North,Made,5,Vul) )
    board2.checkBoardButtons(5,true,4,5).checkBoardButtons(5,false, 6)
    val hand3 = board2.clickUnplayedBoard(6).validate
    val board3 = hand3.onlyEnterHand( 1, 2, 6, allHands, team1, team2)
    board3.checkBoardButtons(6,true,4,5,6).checkBoardButtons(6,false).clickScoreboard.validate.clickTableButton(1).validate
  }

  def withHook[T]( hook: Hook )( f: => T ): T = {
    class HookM( hook: Hook ) {
      StoreMonitor.setTestHook(hook.hook _)
      def close() = StoreMonitor.unsetTestHook()
    }
    val h = new HookM(hook)
    try {
      f
    } catch {
      case x: Throwable =>
        testlog.severe("withHook execution exception", x)
        throw x
    } finally {
      h.close()
    }
  }

  trait Hook {
    def hook( actor: Actor, msg: Any )
  }

  it should "allow entering name of team 3" in {
    tcpSleep(60)
    import SessionDirector._

    val page = TablePage.current(MissingNames).clickBoard(3, 7).asInstanceOf[TableEnterMissingNamesPage].validate
    val missing = page.getInputFieldNames
    missing must contain theSameElementsAs(List(North,South))

    page.isOKEnabled mustBe false

    page.enterPlayer(North, prefixThatMatchesNoOne)
    page.isPlayerSuggestionsVisible(North) mustBe true
    val sugN = page.getPlayerSuggestions(North)
    sugN.size mustBe 1
    sugN.head.text mustBe "No names matched"

    page.enterPlayer(North, team3.one)

    page.isOKEnabled mustBe false

    page.enterPlayer(South, prefixThatMatchesSomeNames)

    eventually {
      page.isPlayerSuggestionsVisible(South) mustBe true
      val suggestions = page.getPlayerSuggestions(South)
      val sugNames = suggestions.map(e=>e.text)
//      sugNames must contain allElementsOf matchedNames
//      sugNames.size must be >= matchedNames.size
      sugNames.size must be > 0
      sugNames.foreach(e => e.toLowerCase().startsWith(prefixThatMatchesSomeNames))
      suggestions
    }
    page.enterPlayer(South, team3.two)

    page.esc

    val pg = page.clickReset.validate
    pg.enterPlayer(North, team3.one)
    pg.esc
    pg.enterPlayer(South, team3.two)
    pg.esc

    pg.isOKEnabled mustBe true

    val ss = page.clickOK.validate

    val sn = ss.verifyAndSelectScorekeeper(team3.one, team3.two, team1.one, team1.two, East)
    val handpage = sn.verifyNamesAndSelect(team3.teamid, team1.teamid, team3.one, team3.two, team1.one, team1.two, East, false).asInstanceOf[HandPage].validate

  }

  def runWithLogging[T](name: String)( f: => T ): T = {
    try {
      testlog.info(s"Starting ${name} test")
      f
    } finally {
      testlog.info(s"Ending ${name} test")
    }
  }

  private object ItVerbStringTest {
    // can't extend AnyVal, ItVerbString is a nested class of trait FlatSpecLike
    implicit class ItVerbStringWrapper( val itVerb: ItVerbString ) {
      def whenTestServerIsRunInTest(testFun: => Any /* Assertion */)(implicit pos: Position): Unit = {
        if (TestServer.isServerStartedByTest) itVerb.in(testFun)
        else itVerb.ignore(testFun)
      }
    }
  }

  import ItVerbStringTest._

  it should "see a restart of the websocket" whenTestServerIsRunInTest {
    var gotJoin = false
    var gotStartMonitor = false

    def process( msg: Protocol.ToServerMessage ) = {
      testlog.info(s"""withHook got $msg""")
      msg match {
        case _: StartMonitor => gotStartMonitor = true
        case _ =>
      }
    }

    if (TestServer.isServerStartedByTest) {
      runWithLogging("withHookBlock") {
        withHook( new Hook {
          def hook( actor: Actor, msg: Any ) = {
            msg match {
              case NewParticipant(name, subscriber) =>
                gotJoin = true
              case ReceivedMessage(senderid, message) =>
                DuplexProtocol.fromString(message) match {
                  case DuplexProtocol.Send(data) => process(data)
                  case DuplexProtocol.Request(data, seq, ack) => process(data)
                  case _ =>
                }
              case x: KillOneConnection =>
                val res = new PrivateMethodExposer(actor.context.system)('printTree)()
                testlog.info("KillOneConnection was received, actors in system:\n"+res)
              case _ =>
            }
          }
        } ) {
          testlog.info(s"""withHook starting""")
          val storeMonitorActorRef = TestServer.getMyService.monitor.monitor.monitor
          storeMonitorActorRef ! KillOneConnection()

          eventually {
            gotJoin mustBe true
          }
          testlog.info(s"""withHook got join""")

          eventually {
            gotStartMonitor mustBe true
          }
          testlog.info(s"""withHook got start monitor""")
        }
      }
    }
  }

  it should "cancel the current hand" in {
    import SessionDirector._
    val handpage = HandPage.current.validate.clickCancel.validate

//    Thread.tcpSleep( 600000L )
  }

  behavior of "Names resource"

  it should "show the names without leading and trailing spaces" in {
    import com.example.rest.UtilsPlayJson._
    val rnames: ResponseFromHttp[Option[Array[String]]] = HttpUtils.getHttpObject( new URL(TestServer.hosturl+"v1/rest/names") )

    rnames.data match {
      case Some(names) =>
        withClue( s"""the names ${names.mkString("['", "', '", "']")} must not contain leading or trailing spaces""" ) {
          names.foreach{ n =>
            withClue( s"""in name "$n" """ ) {
              n.charAt(0) must not be ' '
              n.last must not be ' '
            }
          }
        }
      case None =>
        fail( "did not get names from server" )
    }
  }
}
