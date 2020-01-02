package com.github.thebridsk.bridge.fullserver.test.selenium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest._
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
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ScoreboardPage.PlaceEntry
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ScoreboardPage.PlaceEntry
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
import com.github.thebridsk.bridge.data.websocket.Protocol.StartMonitorDuplicate
import com.github.thebridsk.bridge.server.backend.StoreMonitor.NewParticipant
import com.github.thebridsk.bridge.server.backend.StoreMonitor.ReceivedMessage
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol
import com.github.thebridsk.bridge.server.backend.StoreMonitor.KillOneConnection
import akka.actor.Actor
import com.github.thebridsk.bridge.server.backend.StoreMonitor.NewParticipantSSEDuplicate
import com.github.thebridsk.browserpages.PageBrowser
import com.github.thebridsk.browserpages.Session
import com.github.thebridsk.bridge.server.test.selenium.TestServer

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
class DuplicateTestPages2 extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll
    with EventuallyUtils
    with CancelAfterFailure
{
  import Eventually.{ patienceConfig => _, _ }
  import ParallelUtils._

  TestStartLogging.startLogging()

  import DuplicateTestPages2._
  import DuplicateTestPages.{ testlog => _, team1 => _, team2 => _, team3 => _, team4 => _, _ }

  import scala.concurrent.duration._

  val screenshotDir = "target/DuplicateTestPages2"
  val docsScreenshotDir = "target/docs/Duplicate"

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
                      CodeBlock { SessionDirector.sessionStart(getPropOrEnv("SessionDirector")).setQuadrant(1,1024,768) },
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

    val dp = ListDuplicatePage.current
    dp.withClueAndScreenShot(screenshotDir, "NewDuplicate", "clicking NewDuplicate button") {
      dp.clickNewDuplicateButton.validate
    }
  }

  it should "create a new duplicate match" in {
    import SessionDirector._

    val curPage = NewDuplicatePage.current

    val boards = MovementsPage.getBoardsFromMovement(movement)

    testlog.info(s"Boards are $boards")

    dupid = curPage.click(boardset, movement).validate(boards).dupid
    dupid mustBe Symbol("defined")

    testlog.info(s"Duplicate id is ${dupid.get}")

    allHands.boardsets mustBe Symbol("defined")
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

  it should "allow first round to be played at table 1" in {
    tcpSleep(60)
    import SessionDirector._
    val hand = HandPage.current
    hand.getScore mustBe ( "Missing required information", "", "Enter contract tricks" )
    hand.isOkEnabled mustBe false
    hand.getInputStyle mustBe Some("Guide")
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

  it should "allow second round to be played at table 1" in {
    tcpSleep(60)
    import SessionDirector._
    val sb = ScoreboardPage.current
    val hand = sb.clickBoardToHand(4).validate
    hand.setInputStyle("Prompt")
    val board = hand.onlyEnterHand( 1, 2, 4, allHands, team1, team2)
    board.checkBoardButtons(4,true,4).checkBoardButtons(4,false, 5, 6)
    val hand2 = board.clickUnplayedBoard(5).validate
    val board2 = hand2.onlyEnterHand( EnterHand( 1, 650,0,   2,0,  0,  5,Spades,NotDoubled,North,Made,5,Vul) )
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
    def hook( actor: Actor, msg: Any ): Unit
  }

  it should "allow entering name of team 3" in {
    tcpSleep(60)
    import SessionDirector._

    PageBrowser.withClueAndScreenShot(screenshotDir, "EnterTeam3", "Entering team 3") {

      val page = TablePage.current(MissingNames).clickBoard(3, 7).asInstanceOf[TableEnterMissingNamesPage].validate
      val missing = page.getInputFieldNames
      missing must contain theSameElementsAs(List(North,South))

      page.isOKEnabled mustBe false

      page.enterPlayer(North, prefixThatMatchesNoOne)
      eventually {
        page.isPlayerSuggestionsVisible(North) mustBe true
        val sugN = page.getPlayerSuggestions(North)
        sugN.size mustBe 1
        sugN.head.text mustBe "No names matched"
      }

      page.enterPlayer(North, team3.one)

      eventually {
        page.isOKEnabled mustBe false
      }

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
    var gotJoinSSE = false
    var gotStartMonitor = false

    def process( msg: Protocol.ToServerMessage ) = {
      testlog.info(s"""withHook got $msg""")
      msg match {
        case _: StartMonitorDuplicate => gotStartMonitor = true
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
              case NewParticipantSSEDuplicate(name, dupid, subscriber) =>
                gotJoinSSE = true
              case ReceivedMessage(senderid, message) =>
                DuplexProtocol.fromString(message) match {
                  case DuplexProtocol.Send(data) => process(data)
                  case DuplexProtocol.Request(data, seq, ack) => process(data)
                  case _ =>
                }
              case x: KillOneConnection =>
                val res = new PrivateMethodExposer(actor.context.system)( Symbol("printTree"))()
                testlog.info("KillOneConnection was received, actors in system:\n"+res)
              case _ =>
            }
          }
        } ) {
          testlog.info(s"""withHook starting""")
          val storeMonitorActorRef = TestServer.getMyService.duplicateMonitor.monitor.monitor
          storeMonitorActorRef ! KillOneConnection()

          eventually {
            (gotJoin || gotJoinSSE) mustBe true
          }
          testlog.info(s"""withHook got join""")

          if (gotJoin) {
            eventually {
              gotStartMonitor mustBe true
            }
            testlog.info(s"""withHook got start monitor""")
          }
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
    import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._
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
