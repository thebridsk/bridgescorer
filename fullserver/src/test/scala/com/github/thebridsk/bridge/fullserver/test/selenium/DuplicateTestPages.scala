package com.github.thebridsk.bridge.fullserver.test.selenium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest._
import org.openqa.selenium._
import java.util.concurrent.TimeUnit
import com.github.thebridsk.bridge.data
import org.scalatest.time.Span
import org.scalatest.time.Millis
import com.github.thebridsk.bridge.data.bridge._
import scala.concurrent._
import ExecutionContext.Implicits.global
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.util.Strings
import com.github.thebridsk.bridge.server.test.util.EventuallyUtils
import com.github.thebridsk.bridge.server.test.util.ParallelUtils
import org.scalatest.concurrent.Eventually
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ListDuplicatePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.NewDuplicatePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.MovementsPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.BoardSetsPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ScoreboardPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage.EnterNames
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TableEnterScorekeeperPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.HandPage
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.BoardPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage.SelectNames
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage.Hands
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TableSelectScorekeeperPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.Team
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
import java.net.URL
import com.github.thebridsk.bridge.data.MatchDuplicate
import scala.io.Source
import scala.io.Codec
import com.github.thebridsk.utilities.file.FileIO
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.server.test.util.MonitorTCP
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStoreConverters
import com.github.thebridsk.bridge.server.backend.MatchDuplicateCacheStoreSupport
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage
import java.util.zip.ZipFile
import scala.reflect.io.File
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.PeopleRowMP
import com.github.thebridsk.browserpages.PageBrowser
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.SuggestionPage
import java.io.OutputStreamWriter
import java.io.InputStream
import play.api.libs.json.Json
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import com.github.thebridsk.bridge.server.test.util.GraphQLUtils
import com.github.thebridsk.browserpages.Session
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.StatisticsPage
import com.github.thebridsk.bridge.fullserver.test.pages.LightDarkAddOn
import scala.util.Using
import scala.math.Ordering.Double.TotalOrdering
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.browserpages.Checkbox

object DuplicateTestPages {

  val testlog: Logger = Logger[DuplicateTestPages]()

  val screenshotDir = "target/DuplicateTestPages"
  val docsScreenshotDir = "target/docs/Duplicate"
  val finalMatchDir = "target/FinalMatches"

  TestStartLogging.startLogging()

  val cm = Strings.checkmark
  val bl = ""
  val zr = "0"

  val team1original: Team = Team( 1, "Fred", "Sam")

  val team1: Team = Team( 1, "Nick", "Sam")
  val team2: Team = Team( 2, "Ethan", "Wayne")
  val team3: Team = Team( 3, "Ellen", "Wilma")
  val team4: Team = Team( 4, "Nora", "Sally")

  val prefixThatMatchesSomeNames = "e"
  lazy val matchedNames: List[String] = allHands.teams.flatMap{ t => List(t.one,t.two).filter(n=> n.toLowerCase().startsWith(prefixThatMatchesSomeNames))}
  val prefixThatMatchesNoOne = "asdf"

  val movement = "2TablesArmonk"
  val boardset = "ArmonkBoards"

  lazy val allHands: AllHandsInMatch = new AllHandsInMatch( List(
      HandsOnBoard( 1, 1, 1, EnterHand( 1,110,0,  2,0,  0,  1,Spades,NotDoubled,North,Made,2,NotVul), OtherHandNotPlayed(2,2,1)),
      HandsOnBoard( 2, 2, 1, EnterHand( 4, 80,0,  3,2, -1,  1,Spades,NotDoubled,North,Made,1,NotVul), OtherHandPlayed(1,1,1, 2, 0, 1, -1)),
      HandsOnBoard( 1, 1, 2, EnterHand( 1,110,0,  2,0,  0,  2,Spades,NotDoubled,North,Made,2,Vul), OtherHandNotPlayed(2,2,2)),
      HandsOnBoard( 2, 2, 2, EnterHand( 4,140,2,  3,0,  1,  2,Spades,NotDoubled,North,Made,3,Vul), OtherHandPlayed(1,1,2, 0, 2, -1, 1)),
      HandsOnBoard( 1, 1, 3, EnterHand( 1,140,0,  2,0,  0,  3,Spades,NotDoubled,North,Made,3,Vul), OtherHandNotPlayed(2,2,3)),
      HandsOnBoard( 2, 2, 3, EnterHand( 4,140,1,  3,1,  0,  3,Spades,NotDoubled,North,Made,3,Vul), OtherHandPlayed(1,1,3, 1, 1, 0, 0)),

      HandsOnBoard( 1, 2, 4, EnterHand( 1, 420,1,   2,1,  0,  4,Spades,NotDoubled,North,Made,4,NotVul), OtherHandPlayed(2,1,4, 1, 1, 0, 0)),
      HandsOnBoard( 2, 1, 4, EnterHand( 3, 420,0,   4,0,  0,  4,Spades,NotDoubled,North,Made,4,NotVul), OtherHandNotPlayed(1,2,4)),
      HandsOnBoard( 1, 2, 5, EnterHand( 1,   0,0,   2,2, -12, 0,Spades,NotDoubled,North,Made,5,Vul),    OtherHandPlayed(2,1,5, 2, 0, 12,-12)),
      HandsOnBoard( 2, 1, 5, EnterHand( 3, 650,0,   4,0,  0,  5,Spades,NotDoubled,North,Made,5,Vul),    OtherHandNotPlayed(1,2,5)),
      HandsOnBoard( 1, 2, 6, EnterHand( 1,1010,2,   2,0,  1,  6,Spades,NotDoubled,North,Made,7,NotVul), OtherHandPlayed(2,1,6, 0, 2, -1, 1)),
      HandsOnBoard( 2, 1, 6, EnterHand( 3, 980,0,   4,0,  0,  6,Spades,NotDoubled,North,Made,6,NotVul), OtherHandNotPlayed(1,2,6)),

      HandsOnBoard( 1, 3, 7, EnterHand( 3,720,0,    1,0,  0,  1,Hearts,Redoubled,North,Made,1,Vul), OtherHandNotPlayed(2,4,7)),
      HandsOnBoard( 2, 4, 7, EnterHand( 4,720,1,    2,1,  0,  1,Hearts,Redoubled,North,Made,1,Vul), OtherHandPlayed(1,3,7, 1, 1, 0, 0)),
      HandsOnBoard( 1, 3, 8, EnterHand( 3,470,0,    1,0,  0,  2,Hearts,Doubled,North,Made,2,NotVul), OtherHandNotPlayed(2,4,8)),
      HandsOnBoard( 2, 4, 8, EnterHand( 4,470,1,    2,1,  0,  2,Hearts,Doubled,North,Made,2,NotVul), OtherHandPlayed(1,3,8, 1, 1, 0, 0)),
      HandsOnBoard( 1, 3, 9, EnterHand( 3,140,0,    1,0,  0,  3,Hearts,NotDoubled,North,Made,3,NotVul), OtherHandNotPlayed(2,4,9)),
      HandsOnBoard( 2, 4, 9, EnterHand( 4,140,1,    2,1,  0,  3,Hearts,NotDoubled,North,Made,3,NotVul), OtherHandPlayed(1,3,9, 1, 1, 0, 0)),

      HandsOnBoard( 1, 4, 10, EnterHand( 3,630,0,    1,2, -1,  4,NoTrump,NotDoubled,North,Made,4,Vul), OtherHandPlayed(2,3,10, 2, 0, 1, -1)),
      HandsOnBoard( 2, 3, 10, EnterHand( 2,660,0,    4,0,  0,  4,NoTrump,NotDoubled,North,Made,5,Vul), OtherHandNotPlayed(1,4,10)),
      HandsOnBoard( 1, 4, 11, EnterHand( 3,460,1,    1,1,  0,  5,NoTrump,NotDoubled,North,Made,5,NotVul), OtherHandPlayed(2,3,11,1,1, 0, 0)),
      HandsOnBoard( 2, 3, 11, EnterHand( 2,460,0,    4,0,  0,  5,NoTrump,NotDoubled,North,Made,5,NotVul), OtherHandNotPlayed(1,4,11)),
      HandsOnBoard( 1, 4, 12, EnterHand( 3,2220,1,   1,1,  0,  7,NoTrump,NotDoubled,North,Made,7,Vul), OtherHandPlayed(2,3,12, 1, 1, 0, 0)),
      HandsOnBoard( 2, 3, 12, EnterHand( 2,2220,0,   4,0,  0,  7,NoTrump,NotDoubled,North,Made,7,Vul), OtherHandNotPlayed(1,4,12)),

      HandsOnBoard( 1, 5, 13, EnterHand( 2, 70,0,   3,0,  0,  1,Diamonds,NotDoubled,North,Made,1,Vul), OtherHandNotPlayed(2,6,13)),
      HandsOnBoard( 2, 6, 13, EnterHand( 4, 70,1,   1,1,  0,  1,Diamonds,NotDoubled,North,Made,1,Vul), OtherHandPlayed(1,5,13, 1, 1, 0, 0)),
      HandsOnBoard( 1, 5, 14, EnterHand( 2, 90,0,   3,0,  0,  2,Diamonds,NotDoubled,North,Made,2,NotVul), OtherHandNotPlayed(2,6,14)),
      HandsOnBoard( 2, 6, 14, EnterHand( 4, 90,1,   1,1,  0,  2,Diamonds,NotDoubled,North,Made,2,NotVul), OtherHandPlayed(1,5,14, 1, 1, 0, 0)),
      HandsOnBoard( 1, 5, 15, EnterHand( 2,110,0,   3,0,  0,  3,Diamonds,NotDoubled,North,Made,3,Vul), OtherHandNotPlayed(2,6,15)),
      HandsOnBoard( 2, 6, 15, EnterHand( 4,110,1,   1,1,  0,  3,Diamonds,NotDoubled,North,Made,3,Vul), OtherHandPlayed(1,5,15, 1, 1, 0, 0)),

      HandsOnBoard( 1, 6, 16, EnterHand( 2,-100,0,    3,2, -2,  4,Clubs,NotDoubled,North,Down,2,NotVul), OtherHandPlayed(2,5,16, 2, 0, 2, -2)),
      HandsOnBoard( 2, 5, 16, EnterHand( 1, -50,0,    4,0,  0,  4,Clubs,NotDoubled,North,Down,1,NotVul), OtherHandNotPlayed(1,6,16)),
      HandsOnBoard( 1, 6, 17, EnterHand( 2,-150,1,    3,1,  0,  5,Clubs,NotDoubled,North,Down,3,NotVul), OtherHandPlayed(2,5,17, 1, 1, 0, 0)),
      HandsOnBoard( 2, 5, 17, EnterHand( 1,-150,0,    4,0,  0,  5,Clubs,NotDoubled,North,Down,3,NotVul), OtherHandNotPlayed(1,6,17)),
      HandsOnBoard( 1, 6, 18, EnterHand( 2,-100,1,    3,1,  0,  6,Clubs,NotDoubled,North,Down,1,Vul), OtherHandPlayed(2,5,18, 1, 1, 0, 0)),
      HandsOnBoard( 2, 5, 18, EnterHand( 1,-100,0,    4,0,  0,  6,Clubs,NotDoubled,North,Down,1,Vul), OtherHandNotPlayed(1,6,18))
    ),
    List(team1,team2,team3,team4),
    BoardSetsPage.getBoardSet(boardset),
    MovementsPage.getMovement(movement)
  ).checkFixHands


  val listDuplicateResult: List[String] = List(
        team1.one+"\n1\n20",
        team1.two+"\n1\n20",
        team2.one+"\n2\n18",
        team2.two+"\n2\n18",
        team3.one+"\n2\n18",
        team3.two+"\n2\n18",
        team4.one+"\n4\n16",
        team4.two+"\n4\n16"
      )

  val peopleResult: List[PeopleRowMP] = List(
        PeopleRowMP(team1.one,"100.00%","100.00%","55.56%","1","1.00","1","0","55.56% (20.0/36)","20","36"),
        PeopleRowMP(team1.two,"100.00%","100.00%","55.56%","1","1.00","1","0","55.56% (20.0/36)","20","36"),
        PeopleRowMP(team2.one,"0.00%","0.00%","50.00%","0","0.00","1","0","50.00% (18.0/36)","18","36"),
        PeopleRowMP(team2.two,"0.00%","0.00%","50.00%","0","0.00","1","0","50.00% (18.0/36)","18","36"),
        PeopleRowMP(team3.one,"0.00%","0.00%","50.00%","0","0.00","1","0","50.00% (18.0/36)","18","36"),
        PeopleRowMP(team3.two,"0.00%","0.00%","50.00%","0","0.00","1","0","50.00% (18.0/36)","18","36"),
        PeopleRowMP(team4.one,"0.00%","0.00%","44.44%","0","0.00","1","0","44.44% (16.0/36)","16","36"),
        PeopleRowMP(team4.two,"0.00%","0.00%","44.44%","0","0.00","1","0","44.44% (16.0/36)","16","36")
      )

  // this is here to validate the AllHandsInMatch.getScoreToRound call
  val resultAfterOneRoundCheckMark: List[TeamScoreboard] = List(
        TeamScoreboard(team1original, 0, "0", List(cm,cm,cm,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl)),
        TeamScoreboard(team2, 0, "0", List(cm,cm,cm,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl)),
        TeamScoreboard(team3, 0, "0", List(bl,bl,bl,cm,cm,cm,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl)),
        TeamScoreboard(team4, 0, "0", List(bl,bl,bl,cm,cm,cm,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl))
      )

  val resultAfterOneRoundZero: List[TeamScoreboard] = List(
        TeamScoreboard(team1original, 0, "0", List(zr,zr,zr,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl)),
        TeamScoreboard(team2, 0, "0", List(zr,zr,zr,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl)),
        TeamScoreboard(team3, 0, "0", List(bl,bl,bl,zr,zr,zr,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl)),
        TeamScoreboard(team4, 0, "0", List(bl,bl,bl,zr,zr,zr,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl))
      )
}

/**
 * Test going from the table view, by hitting a board button,
 * to the names view, to the hand view.
 * @author werewolf
 */
class DuplicateTestPages extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll
    with EventuallyUtils
    with CancelAfterFailure {
  import Eventually.{ patienceConfig => _, _ }
  import ParallelUtils._

  import DuplicateTestPages._

  import scala.concurrent.duration._

  val SessionDirector = new DirectorSession()
  val SessionComplete = new CompleteSession()
  val SessionTable1 = new TableSession("1")
  val SessionTable2 = new TableSession("2")

//  val Session1 = new Session

  val timeoutMillis = 15000
  val intervalMillis = 1000

  val backend = TestServer.backend

//  case class MyDuration( timeout: Long, units: TimeUnit )
  type MyDuration = Duration
  val MyDuration = Duration

  implicit val timeoutduration: FiniteDuration = MyDuration( 60, TimeUnit.SECONDS )

  val defaultPatienceConfig: PatienceConfig = PatienceConfig(timeout=scaled(Span(timeoutMillis, Millis)), interval=scaled(Span(intervalMillis,Millis)))
  implicit def patienceConfig: PatienceConfig = defaultPatienceConfig

  override
  def beforeAll(): Unit = {

    testlog.fine( s"DuplicateTestPages patienceConfig=${patienceConfig}" )

    MonitorTCP.nextTest()
    TestStartLogging.startLogging()
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
  def afterAll(): Unit = {
    waitForFuturesIgnoreTimeouts( "Stopping browsers and server",
                    CodeBlock { SessionTable1.sessionStop() },
                    CodeBlock { SessionTable2.sessionStop() },
                    CodeBlock { SessionComplete.sessionStop() },
                    CodeBlock { SessionDirector.sessionStop() },
                    CodeBlock { TestServer.stop() }
                    )
  }

  var dupid: Option[String] = None
  var boardSet: Option[BoardSet] = None

  behavior of "Duplicate test pages of Bridge Server"

  it should "go to the home page" in {
    tcpSleep(15)
    import Session._
    waitForFutures(
      "Starting browsers",
      CodeBlock { SessionTable1.sessionStart(getPropOrEnv("SessionTable1")).setQuadrant(4,1024,768) },
      CodeBlock { SessionTable2.sessionStart(getPropOrEnv("SessionTable2")).setQuadrant(3,1024,768) },
      CodeBlock { SessionComplete.sessionStart(getPropOrEnv("SessionComplete")).setQuadrant(2,1024,768) },
      CodeBlock {
        import SessionDirector._
        HomePage.goto.validate.takeScreenshot(docsScreenshotDir, "HomePage")
      }
    )

  }

  it should "go to duplicate list page" in {
    import SessionDirector._

    HomePage.current.clickListDuplicateButton.validate
  }

  it should "go to boardsets page" in {
    import SessionDirector._

    val lp = ListDuplicatePage.current.validate.clickMainMenu.validate
    eventually {
      lp.findElemById("BoardSets")
    }
    lp.clickBoardSets.validate.click(BoardSetsPage.boardsets.head).validate.clickOK.validate
  }

  it should "go to movements page" in {
    import SessionDirector._

    val lp = ListDuplicatePage.current.validate.clickMainMenu.validate
    eventually {
      lp.findElemById("Movements")
    }
    lp.withClueAndScreenShot(screenshotDir, "Movement", "trying to click first movement") {
      val mp = lp.clickMovements.validate.takeScreenshot(screenshotDir, "MovementBefore")
      val mp1 = mp.click(MovementsPage.movements.head).validate
      mp1.clickOK.validate
    }
  }

  it should "allow creating a new duplicate match" in {
    import SessionDirector._

    val dp = ListDuplicatePage.current
    dp.withClueAndScreenShot(screenshotDir, "NewDuplicate", "clicking NewDuplicate button") {
      dp.clickNewDuplicateButton.validate.takeScreenshot(docsScreenshotDir, "NewDuplicate")
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

  it should "go to duplicate match game in complete, table 1, and table 2 browsers" in {
    tcpSleep(60)

    rounds = MovementsPage.getRoundsFromMovement(movement)

    waitForFutures(
      "Starting browsers",
      CodeBlock {
        import SessionDirector._
        val menu = ScoreboardPage.current.clickMainMenu.validate
        eventually {
          menu.findElemById("Director")
        }
        menu.clickDirectorButton.validate
      },
      CodeBlock {
        import SessionTable1._
        ScoreboardPage.goto(dupid.get).
                       takeScreenshot(docsScreenshotDir, "ScoreboardFromTable").
                       validate.
                       clickTableButton(1).
                       validate(rounds).
                       takeScreenshot(docsScreenshotDir, "TableRound1")
      },
      CodeBlock {
        import SessionTable2._
        TablePage.goto(dupid.get,"2", EnterNames).validate(rounds)
      },
      CodeBlock {
        import SessionComplete._
        val home = HomePage.goto.validate
        val h2 = home.clickToLightDark(LightDarkAddOn.DarkTheme)
        val sb = h2.clickListDuplicateButton.validate(dupid.get)
        sb.clickDuplicate(dupid.get).validate
      }
    )
  }

  it should "allow players names to be entered at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Entering Names",
      CodeBlock {
        import SessionTable1._
        var sk = TablePage.current(EnterNames).validate(rounds).clickBoard(1,1).asInstanceOf[TableEnterScorekeeperPage].validate
        sk.checkErrorMsg("Please enter scorekeeper's name")
        sk.isOKEnabled mustBe false
        sk.takeScreenshot(docsScreenshotDir, "TableEnterNamesSK")
        sk = sk.enterScorekeeper(team1original.one).esc
        sk.checkErrorMsg("Please select scorekeeper's position")
        sk = sk.clickPos(North)
        sk.checkErrorMsg("")
        sk.isOKEnabled mustBe true
        sk.findSelectedPos mustBe Some(North)
        var en = sk.clickOK.validate
        en.isOKEnabled mustBe false
        en.checkErrorMsg("Please enter missing player name(s)")
        en.takeScreenshot(docsScreenshotDir, "TableEnterNamesOthers")
        en = en.enterPlayer(South, team1original.two).enterPlayer(East, team2.two)
        en.isOKEnabled mustBe false
        en = en.enterPlayer(West, team2.two).esc
        en.checkErrorMsg("Please fix duplicate player names")
        en = en.enterPlayer(East, team2.one).esc
        en.isOKEnabled mustBe true
        en.checkErrorMsg("")
        val hand = en.clickOK.asInstanceOf[HandPage].validate
      },
      CodeBlock {
        import SessionTable2._
        var sk = TablePage.current(EnterNames).validate(rounds).clickRound(1).asInstanceOf[TableEnterScorekeeperPage].validate
        sk.isOKEnabled mustBe false
        sk = sk.enterScorekeeper(team3.one).esc.clickPos(North)
        sk.isOKEnabled mustBe true
        sk.findSelectedPos mustBe Some(North)
        var en = sk.clickOK.validate
        en.isOKEnabled mustBe false
        en = en.enterPlayers(team3.two, team4.one, team4.two).esc
        en.isOKEnabled mustBe true
        val scoreboard = en.clickOK.asInstanceOf[ScoreboardPage].validate(4::5::6::Nil)
        val hand = scoreboard.clickBoardToHand(4).validate
      },
      CodeBlock {
        import SessionComplete._
        // test for fix https://github.com/thebridsk/bridgescorer/pull/290
        // and https://github.com/thebridsk/bridgescorer/pull/299 either will fix
        // browser not being updated anymore.
        // The following code will setup the condition of the browser not
        // updating.  One of the following test cases will fail.
        val sb = ScoreboardPage.current
        val menu = sb.clickMainMenu.validate
        eventually { menu.findElemById("Summary")}
        val sum = menu.clickSummary.validate
        sum.clickDuplicate(dupid.get).validate
      }
    )
  }

  var nsForTable1Round1: Option[String] = None

  def getSampleHandImage: File = {
    val f1 = File("../testdata/SampleHand.jpg").toAbsolute.toCanonical.toFile
    if (f1.isFile) f1
    else File("testdata/SampleHand.jpg").toAbsolute.toCanonical.toFile
  }

  it should "allow first round to be played at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Playing first round",
      CodeBlock {
        import SessionTable1._
        PageBrowser.withClueAndScreenShot(screenshotDir, "Round1Table1EnterHand", "Entering hands R1T1") {
          val hand = HandPage.current
          hand.getScore mustBe ( "Missing required information", "", "Enter contract tricks" )
          hand.isOkEnabled mustBe false
          hand.getInputStyle mustBe Some("Guide")
//          hand.enterContract(3, Hearts, Doubled, West, Made, 4, None, None)
//          hand.takeScreenshot(docsScreenshotDir, "DuplicateHand")
//          hand.clickClear
          val board = hand.enterHand( 1, 1, 1, allHands, team1original, team2)
          board.checkBoardButtons(1, true, 1).checkBoardButtons(1, false, 2, 3).checkBoardButtonSelected(1)
          val board1 = board.clickPlayedBoard(1).validate
          val nsRound1 = allHands.movement.map { mov =>
            mov.hands.find( hit => hit.round == 1 && hit.table === 1).map { hit =>
              hit.ns
            }.getOrElse( fail(s"Did not find table 1 round 1"))
          }.getOrElse( fail(s"Unable to determine NS for table 1 round 1"))
          val teamId = data.Team.id(nsRound1)
          nsForTable1Round1 = Some(teamId.id)
          val hand1 = board1.clickHand(nsRound1).validate
          val hand1a = hand1.validatePicture()
          hand1a.checkShowDisplayed(false)
          hand1a.checkDeleteDisplayed(false)
          val hand1b = hand1a.selectPictureFile( getSampleHandImage )
          hand1b.checkShowDisplayed(true)
          hand1b.checkDeleteDisplayed(true)
          val hand1c = hand1b.clickShowPicture.validatePicture(true)
          val hand1d = hand1c.clickOkPicture.validatePicture(false)
          val board1a = hand1d.clickOk
          board1a.validatePictures(false,List(data.Team.id(nsRound1).id))
          val hand2 = board1a.clickUnplayedBoard(2).validate
          val board2 = hand2.enterHand( 1, 1, 2, allHands, team1original, team2)
          board2.checkBoardButtons(2, true,1,2).checkBoardButtons(2, false, 3).checkBoardButtonSelected(2)
          val hand3 = board2.clickUnplayedBoard(3).validate
          hand.enterContract(3, Hearts, Doubled, West, Made, -1, None, None)
          hand.takeScreenshot(docsScreenshotDir, "EnterHand")
          hand.clickClear
          val board3 = hand3.enterHand( 1, 1, 3, allHands, team1original, team2)
          board3.checkBoardButtons(3, true,1,2,3).checkBoardButtons(3, false).checkBoardButtonSelected(3)
        }
      },
      CodeBlock {
        import SessionTable2._
        PageBrowser.withClueAndScreenShot(screenshotDir, "Round1Table2EnterHand", "Entering hands R1T2") {
          val hand = HandPage.current
          hand.getScore mustBe Tuple3( "Missing required information", "", "Enter contract tricks" )
          hand.isOkEnabled mustBe false
          hand.getInputStyle mustBe Some("Guide")
          val board = hand.enterHand( 2, 1, 4, allHands, team3, team4)
          board.checkBoardButtons(4, true,4).checkBoardButtons(4, false, 5, 6).checkBoardButtonSelected(4)
          val hand2 = board.clickUnplayedBoard(5).validate
          val board2 = hand2.enterHand( 2, 1, 5, allHands, team3, team4)
          board2.checkBoardButtons(5, true,4,5).checkBoardButtons(5, false, 6).checkBoardButtonSelected(5)
          val hand3 = board2.clickUnplayedBoard(6).validate
          val board3 = hand3.enterHand( 2, 1, 6, allHands, team3, team4)
          board3.checkBoardButtons(6, true,4,5,6).checkBoardButtons(6, false).checkBoardButtonSelected(6)
        }
      }
    )
  }

  def checkPlayedBoards(
        sb: ScoreboardPage,
        checkmarks: Boolean,
        table: Option[Int],
        round: Int,
        allplayed: Boolean,
        screenshot: Option[Int] = None,
        screenshotName: Option[String] = None,
        boardWithPicture: Option[Int] = None,
        nsForPicture: Option[String] = None,
        pictureButtonShowing: Boolean = true
      )( implicit
         webDriver: WebDriver
      ): ScoreboardPage = {
    val boards = table match {
      case Some(t) => allHands.getBoardsInTableRound(t, round)
      case None => allHands.getBoardsInRound(round)
    }

    val pr = boards.foldLeft( None: Option[BoardPage] ) { (progress,board) =>

      def processBoard( bp: BoardPage ) = {
        val bp1 = bp.clickPlayedBoard(board).validate
        testlog.fine(s"table ${table}, round ${round}, current board is ${board}, checkmarks ${checkmarks}, all boards are ${boards}: board picture ${boardWithPicture}, ${nsForPicture}, ${pictureButtonShowing}")
        boardWithPicture.filter(b=>b==board).foreach { b =>
          nsForPicture.foreach { ns =>
            if (pictureButtonShowing) {
              testlog.fine(s"table ${table}, round ${round}, current board is ${board}, checkmarks ${checkmarks}, all boards are ${boards}: show picture")
              bp.validatePictures(ids = ns::Nil)
              val bp1 = bp.clickShowPicture(ns).validatePictures(true)
              bp1.clickOkPicture.validatePictures(ids=ns::Nil)
            } else {
              testlog.fine(s"table ${table}, round ${round}, current board is ${board}, checkmarks ${checkmarks}, all boards are ${boards}: no picture")
              bp.validatePictures(notShowingIds = ns::Nil)
            }
          }
        }
        testlog.fine(s"trying to take screen shot of board ${screenshot}, current board is ${board}, all boards are ${boards}")
        screenshot.filter(b=>b==board).map { b =>
          bp1.takeScreenshot(docsScreenshotDir, screenshotName.get)
        }.getOrElse(bp1)
      }

      val bb = progress match {
        case Some(bp) =>
          processBoard(bp)
        case None =>
          val bp = sb.clickBoardToBoard(board).validate
          processBoard(bp)
      }

      val bb1 = if (table.isDefined) {
        bb.checkBoardButtons(board, true, boards:_*)
      } else {
        val (bplayed,bnotplayed) = if (allplayed) {
          (boards,Nil)
        } else {
          (Nil,boards)
        }
        bb.checkBoardButtonColors(board, Nil, bplayed, bnotplayed)
      }

      Some(bb1.checkHand(round, board, allHands, checkmarks))
    }

    pr.map(bp=>bp.clickScoreboard).getOrElse(sb)
  }

  def toOriginal( data: (List[TeamScoreboard], List[PlaceEntry]) ): (List[TeamScoreboard], List[PlaceEntry]) = {
    val (tsf,pesf) = data
    val ts = tsf.map { t =>
      if (t.team == team1) t.copy(team=team1original)
      else t
    }
    val pes = pesf.map { pe =>
      pe.copy( teams = pe.teams.map( t => if (t == team1) team1original else t ) )
    }
    (ts,pes)
  }

  it should "show the director's scoreboard and complete scoreboard shows checkmarks for the played games" in {
    tcpSleep(10)
    waitForFutures(
      "Checking scoreboards",
      CodeBlock{
        import SessionDirector._

        val sb = ScoreboardPage.current
        sb.checkTable(resultAfterOneRoundZero:_*)
        val (ts,pes) = toOriginal(allHands.getScoreToRound(1, HandDirectorView))
        sb.checkTable( ts: _*)
        sb.checkPlaceTable( pes: _*)
        checkPlayedBoards( sb, false, None, 1, false, boardWithPicture = Some(1), nsForPicture = nsForTable1Round1, pictureButtonShowing = true )
      },
      CodeBlock{
        import SessionComplete._

        val sb = ScoreboardPage.current
        // a failure here could be because fixes https://github.com/thebridsk/bridgescorer/pull/290
        // and https://github.com/thebridsk/bridgescorer/pull/299 have not been applied
        sb.checkTable(resultAfterOneRoundCheckMark:_*)
        val (ts,pes) = toOriginal(allHands.getScoreToRound(1, HandCompletedView))
        sb.checkTable( ts: _*)
        sb.checkPlaceTable( pes: _*)
        checkPlayedBoards( sb, true, None, 1, false, boardWithPicture = Some(1), nsForPicture = nsForTable1Round1, pictureButtonShowing = false )
      },
      CodeBlock{
        import SessionTable1._

        val bp = BoardPage.current
        val tp = bp.clickTableButton(1).validate.setTarget(Hands)
        val sb = tp.clickRound(1).asInstanceOf[ScoreboardPage].validate
        sb.checkTable(resultAfterOneRoundCheckMark:_*)
        val (ts,pes) = toOriginal(allHands.getScoreToRound(1, HandTableView( 1, 1, team1.teamid, team2.teamid )))
        sb.checkTable( ts: _*)
        sb.checkPlaceTable( pes: _*)
        checkPlayedBoards( sb, false, Some(1), 1, false, boardWithPicture = Some(1), nsForPicture = nsForTable1Round1, pictureButtonShowing = true )
      },
      CodeBlock{
        import SessionTable2._

        val bp = BoardPage.current
        val sb = bp.clickScoreboard.validate
        sb.checkTable(resultAfterOneRoundCheckMark:_*)
        val (ts,pes) = toOriginal(allHands.getScoreToRound(1, HandTableView( 2, 1, team3.teamid, team4.teamid )))
        sb.checkTable( ts: _*)
        sb.checkPlaceTable( pes: _*)
        checkPlayedBoards( sb, false, Some(2), 1, false, nsForPicture = nsForTable1Round1, pictureButtonShowing = false )
      }
    )
  }

  def selectScorekeeper( currentPage: ScoreboardPage,
                         table: Int, round: Int,
                         ns: Team, ew: Team,
                         scorekeeper: PlayerPosition,
                         mustswap: Boolean,
                         takeScreenshot: Boolean = false
                       )( implicit
                           webDriver: WebDriver
                       ): ScoreboardPage = {

    val tp = currentPage.clickTableButton(table).validate.setTarget(SelectNames)
    val ss = tp.clickRound(round).asInstanceOf[TableSelectScorekeeperPage].validate

    val screenShotDir = if (takeScreenshot) Some((docsScreenshotDir,"SelectSK")) else None
    val sn = ss.verifyAndSelectScorekeeper(ns.one, ns.two, ew.one, ew.two, scorekeeper, screenShotDir )
    if (takeScreenshot) sn.takeScreenshot(docsScreenshotDir, "SelectNames")
    sn.verifyNamesAndSelect(ns.teamid, ew.teamid, ns.one, ns.two, ew.one, ew.two, scorekeeper, mustswap).asInstanceOf[ScoreboardPage]
  }

  it should "allow player 1 on team 1 name to be changed" in {
    import SessionDirector._

    val sb = ScoreboardPage.current.validate
    val en = sb.clickEditNames

    val playersBefore = (team1original::team2::team3::team4::Nil).flatMap( t => t.one::t.two::Nil ).grouped(2).toList
    val playersAfter = (team1::team2::team3::team4::Nil).flatMap( t => t.one::t.two::Nil ).grouped(2).toList

    en.getNames mustBe playersBefore

    en.setName(1, 1, team1.one)

    en.getNames mustBe playersAfter

    val menu = en.clickOK.validate.clickMainMenu.validate
    eventually { menu.findElemById("Director")}
    menu.clickDirectorButton.validate
  }

  it should "allow selecting players for round 2" in {
    tcpSleep(10)
    waitForFutures(
      "Selecting players for round 2",
      CodeBlock{
        import SessionTable1._
        val sbc = ScoreboardPage.current.clickTableButton(1).validate.takeScreenshot(docsScreenshotDir, "TableRound2").clickCompletedScoreboard.validate
        val sb = selectScorekeeper(ScoreboardPage.current,1,2, team1, team2, East, false, true )
      },
      CodeBlock{
        import SessionTable2._
        val sb = selectScorekeeper(ScoreboardPage.current,2,2, team4, team3, East, true )
      }
    )
  }

  it should "allow second round to be played at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Playing second round",
      CodeBlock {
        import SessionTable1._
        PageBrowser.withClueAndScreenShot(screenshotDir, "Round2Table1EnterHand", "Entering hands R2T1") {
          val sb = ScoreboardPage.current
          val hand = sb.clickBoardToHand(4).validate
          hand.setInputStyle("Prompt")
          val board = hand.enterHand( 1, 2, 4, allHands, team1, team2)
          board.checkBoardButtons(4, true,4).checkBoardButtons(4, false, 5, 6).checkBoardButtonSelected(4)
          board.takeScreenshot(docsScreenshotDir, "BoardFromTable")
          val hand2 = board.clickUnplayedBoard(5).validate
          val board2 = hand2.enterHand( 1, 2, 5, allHands, team1, team2)
          board2.checkBoardButtons(5, true,4,5).checkBoardButtons(5, false, 6).checkBoardButtonSelected(5)
          val hand3 = board2.clickUnplayedBoard(6).validate
          val board3 = hand3.enterHand( 1, 2, 6, allHands, team1, team2)
          board3.checkBoardButtons(6, true,4,5,6).checkBoardButtons(6, false).checkBoardButtonSelected(6)
        }
      },
      CodeBlock {
        import SessionTable2._
        PageBrowser.withClueAndScreenShot(screenshotDir, "Round2Table2EnterHand", "Entering hands R2T2") {
          val sb = ScoreboardPage.current
          val hand = sb.clickBoardToHand(1).validate
          hand.setInputStyle("Original")
          val board = hand.enterHand( 2, 2, 1, allHands, team4, team3)
          board.checkBoardButtons(1, true,1).checkBoardButtons(1, false, 2, 3).checkBoardButtonSelected(1)
          val hand2 = board.clickUnplayedBoard(2).validate
          val board2 = hand2.enterHand( 2, 2, 2, allHands, team4, team3)
          board2.checkBoardButtons(2, true,1,2).checkBoardButtons(2, false, 3).checkBoardButtonSelected(2)
          val hand3 = board2.clickUnplayedBoard(3).validate
          hand3.setInputStyle("Guide")
          hand.takeScreenshot(docsScreenshotDir, "EnterHandBefore")
          val board3 = hand3.enterHand( 2, 2, 3, allHands, team4, team3)
          board3.checkBoardButtons(3, true,1,2,3).checkBoardButtons(3, false).checkBoardButtonSelected(3)
        }
      }
    )
  }

  it should "show the results on the scoreboards after round 2" in {
    tcpSleep(10)
    waitForFutures(
      "Checking scoreboards",
      CodeBlock{
        import SessionDirector._

        val sb = ScoreboardPage.current
        val (ts,pes) = allHands.getScoreToRound(2, HandDirectorView)
        sb.checkTable( ts: _*)
        sb.checkPlaceTable( pes: _*)
        checkPlayedBoards( sb, false, None, 2, true )
      },
      CodeBlock{
        import SessionComplete._

        val sb = ScoreboardPage.current
        val (ts,pes) = allHands.getScoreToRound(2, HandCompletedView)
        sb.checkTable( ts: _*)
        sb.checkPlaceTable( pes: _*)
        checkPlayedBoards( sb, true, None, 2, true, Some(5), Some("BoardPage5") )
      },
      CodeBlock{
        import SessionTable1._

        val bp = BoardPage.current
        val tp = bp.clickTableButton(1).validate.setTarget(Hands)
        val sb = tp.clickRound(2).asInstanceOf[ScoreboardPage].validate
        val (ts,pes) = allHands.getScoreToRound(2, HandTableView( 1, 2, team1.teamid, team2.teamid ))
        sb.checkTable( ts: _*)
        sb.checkPlaceTable( pes: _*)
        checkPlayedBoards( sb, true, Some(1), 2, true )
      },
      CodeBlock{
        import SessionTable2._

        val bp = BoardPage.current
        val sb = bp.clickScoreboard.validate
        val (ts,pes) = allHands.getScoreToRound(2, HandTableView( 2, 2, team4.teamid, team3.teamid ))
        sb.checkTable( ts: _*)
        sb.checkPlaceTable( pes: _*)
        checkPlayedBoards( sb, true, Some(2), 2, true )
      }
    )
  }


  /**
   * @param currentPage the current page
   * @param table
   * @param round
   * @param nsTeam
   * @param ewTeam
   * @param scorekeeper
   * @param mustswap
   * @param boards the boards to play
   * @return a ScoreboardPage object representing the page when done.
   */
  def playRound(
      currentPage: ScoreboardPage,
      table: Int,
      round: Int,
      nsTeam: Team,
      ewTeam: Team,
      scorekeeper: PlayerPosition,
      mustswap: Boolean,
      boards: List[Int],
      verifyNSteam: Team,
      verifyEWteam: Team
    )( implicit
         webDriver: WebDriver
    ): Unit = {

    PageBrowser.withClueAndScreenShot(screenshotDir, s"Round${round}Table${table}EnterHand", s"Enter Hand R${round}T${table}") {
      val sb = selectScorekeeper(currentPage,table,round, nsTeam, ewTeam, scorekeeper, mustswap )

      val board = withClue( s"""board ${boards.head}""" ) {
        val hand = sb.clickBoardToHand(boards.head).validate
        hand.setInputStyle("Prompt")
        val brd = hand.enterHand( table, round, boards.head, allHands, verifyNSteam, verifyEWteam)
        brd.checkBoardButtons(boards.head,true, boards.head).checkBoardButtons(boards.head,false, boards.tail:_*).checkBoardButtonSelected(boards.head)
      }

      var playedBoards = boards.head::Nil
      var unplayedBoards = boards.tail

      var currentBoard = board
      while (!unplayedBoards.isEmpty) {
        val b = unplayedBoards.head
        unplayedBoards = unplayedBoards.tail
        playedBoards = b::playedBoards

        val board = withClue( s"""board ${b}""" ) {
          val hand2 = currentBoard.clickUnplayedBoard(b).validate
          currentBoard = hand2.enterHand( table, round, b, allHands, verifyNSteam, verifyEWteam)
          currentBoard = currentBoard.checkBoardButtons(b,true,playedBoards:_*).checkBoardButtons(b,false, unplayedBoards:_*).checkBoardButtonSelected(b)
        }

      }

      val sbr = currentBoard.clickScoreboard.validate
    }
  }

  /**
   * @param currentPage the current page
   * @param table
   * @param round
   * @param nsTeam
   * @param ewTeam
   * @param imp true if IMP scoring, false if MP scoring.  default is false.
   * @return a ScoreboardPage object representing the page when done.
   */
  def validateRound(
      currentPage: ScoreboardPage,
      table: Int,
      round: Int,
      nsTeam: Team,
      ewTeam: Team,
      imp: Boolean = false
    )( implicit
         webDriver: WebDriver
    ): ScoreboardPage = {

    PageBrowser.withClueAndScreenShot(screenshotDir, s"Round${round}Table${table}Validate", "Validate R${round}T${table}") {

      val sbr = currentPage.validate
      val (ts,pes) = allHands.getScoreToRound(round, HandTableView( table, round, nsTeam.teamid, ewTeam.teamid ), imp)
      sbr.checkTable( ts: _*)
      sbr.checkPlaceTable( pes: _*)

      sbr
    }
  }

  it should "allow round 3 to be played at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Playing round 3",
      CodeBlock {
        import SessionTable1._
        playRound(ScoreboardPage.current,1,3,team3.swap,team1,West,true,List(7,8,9),team3.swap,team1 )
      },
      CodeBlock {
        import SessionTable2._
        playRound(ScoreboardPage.current,2,3,team2.swap,team4,East,true,List(10,11,12),team2.swap,team4 )
      }
    )
  }

  it should "validate round 3 at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "validating round 3",
      CodeBlock {
        import SessionComplete._
        val sb = ScoreboardPage.current
        sb.takeScreenshot(docsScreenshotDir, "Scoreboard")
        checkPlayedBoards( sb, true, None, 3, false, Some(8), Some("BoardPage8") )
      },
      CodeBlock {
        import SessionTable1._
        val sb = validateRound(ScoreboardPage.current,1,3,team3.swap,team1 )
        sb.takeScreenshot(docsScreenshotDir, "ScoreboardFromTable")
      },
      CodeBlock {
        import SessionTable2._
        validateRound(ScoreboardPage.current,2,3,team2.swap,team4 )
      }
    )
  }

  it should "show all boards" in {
    tcpSleep(60)
    waitForFutures(
      "Checking all boards",
      CodeBlock {
        import SessionDirector._
        withClue( """On session Director""" ) {
          val page = ScoreboardPage.current.clickAllBoards.validate

          page.getBoardIds must contain theSameElementsAs allHands.boards

          page.checkHand(3, allHands.getBoardsInTableRound(1, 1).head, allHands, false)
          page.checkHand(3, allHands.getBoardsInTableRound(1, 3).head, allHands, false)
          page.checkHand(3, allHands.getBoardsInTableRound(2, 3).head, allHands, false)
          page.checkHand(3, allHands.getBoardsInTableRound(1, 5).head, allHands, false)

          page.clickScoreboard
        }
      },
      CodeBlock {
        import SessionComplete._
        withClue( """On session Complete""" ) {
          val page = ScoreboardPage.current.clickAllBoards.validate

          page.getBoardIds must contain theSameElementsAs allHands.boards

          page.checkHand(3, allHands.getBoardsInTableRound(1, 1).head, allHands, true)
          page.checkHand(3, allHands.getBoardsInTableRound(1, 3).head, allHands, true)
          page.checkHand(3, allHands.getBoardsInTableRound(2, 3).head, allHands, true)
          page.checkHand(3, allHands.getBoardsInTableRound(1, 5).head, allHands, true)

          page.clickScoreboard
        }
      },
      CodeBlock {
        import SessionTable1._
        withClue( """On session Table 1""" ) {
          val page = ScoreboardPage.current.clickAllBoards.validate

          page.getBoardIds must contain theSameElementsAs allHands.boards

          page.checkHand(3, allHands.getBoardsInTableRound(1, 1).head, allHands, true)
          page.checkHand(3, allHands.getBoardsInTableRound(1, 3).head, allHands, false)  // played on this table
          page.checkHand(3, allHands.getBoardsInTableRound(2, 3).head, allHands, true)
          page.checkHand(3, allHands.getBoardsInTableRound(1, 5).head, allHands, true)

          page.clickScoreboard
        }
      },
      CodeBlock {
        import SessionTable2._
        withClue( """On session Table 2""" ) {
          val page = ScoreboardPage.current.clickAllBoards.validate

          page.checkHand(3, allHands.getBoardsInTableRound(1, 1)(2), allHands, true)
          page.checkHand(3, allHands.getBoardsInTableRound(1, 3)(2), allHands, true)
          page.checkHand(3, allHands.getBoardsInTableRound(2, 3)(2), allHands, false)  // played on this table
          page.checkHand(3, allHands.getBoardsInTableRound(1, 5)(2), allHands, true)

          page.getBoardIds must contain theSameElementsAs allHands.boards

          page.clickScoreboard
        }
      }
    )
  }

  it should "allow round 4 to be played at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Playing round 4",
      CodeBlock {
        import SessionTable1._
        val sb = BoardPage.current.clickScoreboard.validate
        playRound(sb,1,4,team3,team1.swap,North,false,List(10,11,12),team3,team1.swap )
      },
      CodeBlock {
        import SessionTable2._
        val sb = BoardPage.current.clickScoreboard.validate
        playRound(sb,2,4,team4.swap,team2,South,true,List(7,8,9),team4.swap,team2 )
      }
    )
  }

  it should "validate round 4 at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "validating round 4",
      CodeBlock {
        import SessionTable1._
        val sb = ScoreboardPage.current
        validateRound(sb,1,4,team3.swap,team1.swap )
      },
      CodeBlock {
        import SessionTable2._
        val sb = ScoreboardPage.current
        validateRound(sb,2,4,team4.swap,team2.swap )
      }
    )
  }

  it should "allow round 5 to be played at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Playing round 5",
      CodeBlock {
        import SessionTable1._
        val sb = BoardPage.current.clickScoreboard.validate
        playRound(sb,1,5,team2,team3,West,false,List(13,14,15),team2,team3 )
      },
      CodeBlock {
        import SessionTable2._
        val sb = BoardPage.current.clickScoreboard.validate
        playRound(sb,2,5,team1,team4,West,false,List(16,17,18),team1,team4 )
      }
    )
  }

  it should "validate round 5 at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "validating round 5",
      CodeBlock {
        import SessionTable1._
        val sb = ScoreboardPage.current
        validateRound(sb,1,5,team2,team3 )
      },
      CodeBlock {
        import SessionTable2._
        val sb = ScoreboardPage.current
        validateRound(sb,2,5,team1,team4 )
      }
    )
  }

  it should "allow round 6 to be played at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Playing round 6",
      CodeBlock {
        import SessionTable1._
        val sb = BoardPage.current.clickScoreboard.validate
        playRound(sb,1,6,team2,team3,West,false,List(16,17,18),team2,team3 )
      },
      CodeBlock {
        import SessionTable2._
        val sb = BoardPage.current.clickScoreboard.validate
        playRound(sb,2,6,team4,team1.swap,South,false,List(13,14,15),team4,team1.swap )
      }
    )
  }

  it should "validate round 6 at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "validating round 6",
      CodeBlock {
        import SessionTable1._
        val sb = ScoreboardPage.current
        validateRound(sb,1,6,team2,team3 )
      },
      CodeBlock {
        import SessionTable2._
        val sb = ScoreboardPage.current.setScoreStyle(ScoreboardPage.ScoreStyleIMP).validate
        // Thread.sleep(500L)
        validateRound(sb,2,6,team4,team1, true )
      }
    )
  }

  it should "show the result on the summary page" in {
    import SessionComplete._

    dupid match {
      case Some(id) =>
        val page = ScoreboardPage.current
        // Thread.sleep(100)
        page.takeScreenshot(docsScreenshotDir, "FinalScoreboard")
        val menu = page.clickMainMenu.validate
        eventually { menu.findElemById("Summary")}
        val lp = menu.clickSummary.validate( id )
        eventually {
          val names = lp.getNames(false)
          names must contain ( team1.one )
        }
        lp.takeScreenshot(docsScreenshotDir, "ListDuplicate")
        lp.checkResults(id, listDuplicateResult:_*)
      case None =>
        ScoreboardPage.current.clickSummary.validate
    }
  }

  it should "show the people page" in {
    import SessionComplete._

    val sb = ListDuplicatePage.current
    val ids = sb.getMatchIds

    val lp = sb.clickMainMenu.validate
    eventually {
      lp.findElemById("Statistics")
    }

    val peoplePage = lp.clickStatistics.validate.takeScreenshot(docsScreenshotDir, "Stats").clickPeopleResults

    if (ids.size == 1) {
      peoplePage.checkPeopleMP( peopleResult:_*)
    } else {
      testlog.info(s"Not testing the people page with results, number of matchs played is ${ids.size}")
    }

  }

  case class ResponseData( duplicate: MatchDuplicate )
  case class QueryResponse( data: ResponseData )

  it should "have rest call and queryml call return the same match" in {
    import com.github.thebridsk.bridge.data.rest.JsonSupport._
    implicit val rdFormat = Json.format[ResponseData]
    implicit val qrFormat = Json.format[QueryResponse]

    dupid match {
      case Some(duplicateId) =>
        val url: URL = new URL(TestServer.hosturl+"v1/rest/duplicates/"+duplicateId)
        val connection = url.openConnection()
        val is = connection.getInputStream
        var pl: Option[MatchDuplicate] = None
        implicit val instanceJson = new BridgeServiceFileStoreConverters(true).matchDuplicateJson
        var qmlis: InputStream = null
        try {
          val json = Source.fromInputStream(is)(Codec.UTF8).mkString

          val (storegood,played) = new MatchDuplicateCacheStoreSupport(false).fromJSON(json)
          pl = Some(played)

          val duplicateQML = s"""
            |{
            |  duplicate( id: "$duplicateId") {
            |    id
            |    teams {
            |      id
            |      player1
            |      player2
            |      created
            |      updated
            |    }
            |    boards {
            |      id
            |      nsVul
            |      ewVul
            |      dealer
            |      hands {
            |        id
            |        played {
            |          id
            |          contractTricks
            |          contractSuit
            |          contractDoubled
            |          declarer
            |          nsVul
            |          ewVul
            |          madeContract
            |          tricks
            |          created
            |          updated
            |        }
            |        table
            |        round
            |        board
            |        nsTeam
            |        nIsPlayer1
            |        ewTeam
            |        eIsPlayer1
            |        created
            |        updated
            |      }
            |      created
            |      updated
            |    }
            |    boardset
            |    movement
            |    created
            |    updated
            |  }
            |}
            |""".stripMargin

          val data = GraphQLUtils.queryToJson(duplicateQML)

          val qmlurl: URL = new URL( TestServer.hosturl+"v1/graphql")
          val qmlconn = qmlurl.openConnection()
          val headersForPost=Map("Content-Type" -> "application/json; charset=UTF-8",
                                 "Accept" -> "application/json")
          headersForPost.foreach { e =>
            qmlconn.setRequestProperty(e._1, e._2)
          }
          qmlconn.setDoOutput(true)
          qmlconn.setDoInput(true)
          val wr = new OutputStreamWriter(qmlconn.getOutputStream(), "UTF8")
          wr.write(data)
          wr.flush()
          // Get the response
          qmlis = qmlconn.getInputStream()
          val qmljson = Source.fromInputStream(qmlis)(Codec.UTF8).mkString
          Json.fromJson[QueryResponse]( Json.parse(qmljson) ) match {
            case JsSuccess(qmlplayed,path) =>
              played mustBe qmlplayed.data.duplicate
            case JsError(err) =>
              fail( s"Unable to parse response from graphQL: $err")
          }
        } catch {
          case x: Exception =>
            pl match {
              case Some(played) =>
                val j = new MatchDuplicateCacheStoreSupport(false).toJSON(played)
                FileIO.writeFileSafe(s"${screenshotDir}/DuplicateTestPages.QueryML.json", j)
              case None =>
            }
            throw x

        } finally {
          is.close()
          if (qmlis != null) qmlis.close()
        }
      case _ =>
    }
  }

  it should "have timestamps on all objects in the MatchDuplicate record" in {
    dupid match {
      case Some(duplicateId) =>
        val url: URL = new URL(TestServer.hosturl+"v1/rest/duplicates/"+duplicateId)
        val connection = url.openConnection()
        val is = connection.getInputStream
        var pl: Option[MatchDuplicate] = None
        implicit val instanceJson = new BridgeServiceFileStoreConverters(true).matchDuplicateJson
        try {
          val json = Source.fromInputStream(is)(Codec.UTF8).mkString

          val (id,played) = new MatchDuplicateCacheStoreSupport(false).fromJSON(json)
          pl = Some(played)

          val created = played.created
          val updated = played.updated

          created must not be (0)
          updated must not be (0)
          created must be <= updated

          played.boards.foreach( b => {
            b.created must not be (0)
            b.updated must not be (0)
            b.created must be <= b.updated
            assert( created-100 <= b.created && b.created <= updated+100 )
            assert( created-100 <= b.updated && b.updated <= updated+100 )
            b.hands.foreach( h=> {
              h.created must not be (0)
              h.updated must not be (0)
              h.created must be <= h.updated
              assert( b.created-100 <= h.created && h.created <= b.updated+100 )
              assert( b.created-100 <= h.updated && h.updated <= b.updated+100 )
            })
          })
          played.teams.foreach(t =>{
            t.created must not be (0)
            t.updated must not be (0)
            t.created must be <= t.updated
            assert( created-100 <= t.created && t.created <= updated+100 )
            assert( created-100 <= t.updated && t.updated <= updated+100 )
          })
        } catch {
          case x: Exception =>
            pl match {
              case Some(played) =>
                val j = new MatchDuplicateCacheStoreSupport(false).toJSON(played)
                FileIO.writeFileSafe("DuplicateTest.TimeError.json", j)
              case None =>
            }
            throw x

        } finally {
          is.close()
        }
      case None =>
    }
  }

  it should "try going to latest new match, and fail" in {
    import SessionComplete._

    val hp = StatisticsPage.current.clickHome.validate

    val hpe = hp.clickLatestNewDuplicateButton(false).asInstanceOf[HomePage]

    hpe.validatePopup(true)

    hpe.getPopUpText mustBe "Did not find an unfinished duplicate match"

    hpe.clickPopUpCancel

    val sp = ListDuplicatePage.current.validate

    sp.clickHome.validate
  }

  var dupid2: Option[String] = None

  it should "allow creating a new duplicate match from the home page" in {
    import SessionDirector._

    val menu = ScoreboardPage.current.clickMainMenu.validate
    eventually {menu.findElemById("Summary")}
    menu.clickSummary.validate.clickNewDuplicateButton.validate
//    HomePage.goto.validate.clickNewDuplicateButton.validate
  }

  it should "create another new duplicate match" in {
    import SessionDirector._

    val curPage = NewDuplicatePage.current

    val boards = MovementsPage.getBoardsFromMovement(movement)

    testlog.info(s"Boards are $boards")

    dupid2 = curPage.click(boardset, movement).validate(boards).dupid
    dupid2 mustBe Symbol("defined")

    testlog.info(s"Duplicate id is ${dupid2.get}")

    allHands.boardsets mustBe Symbol("defined")
  }

  it should "try going to latest new match, and succeed" in {
    import SessionComplete._

    val hp = HomePage.current

    PageBrowser.withClueAndScreenShot(screenshotDir,"LatestNew",s"Going to latest looking for ${dupid2}",true) {
      val sp = hp.clickLatestNewDuplicateButton(true).validate.asInstanceOf[ScoreboardPage]
      sp.dupid mustBe dupid2
    }

  }

  it should "go to table 1 page and go to round 1 and test suggestions" in {
    import SessionDirector._

    PageBrowser.withClueAndScreenShot(screenshotDir, "TestSuggestion", "testing name suggestions") {
      val table = ScoreboardPage.current.clickTableButton(1).validate

      val es = table.clickRound(1).asInstanceOf[TableEnterScorekeeperPage].validate
      es.enterScorekeeper(prefixThatMatchesSomeNames)
      val firstsug = eventually {
        es.isScorekeeperSuggestionsVisible mustBe true
        val suggestions = es.getScorekeeperSuggestions
        val sugNames = suggestions.map(e=>e.text)
        sugNames must contain allElementsOf matchedNames
        sugNames.size must be >= matchedNames.size
        sugNames.foreach(e => e.toLowerCase().startsWith(prefixThatMatchesSomeNames))
        suggestions
      }.head
      val firsttext = firstsug.text
      firstsug.click
      eventually {
        es.getScorekeeper mustBe firsttext
      }
      val en = es.clickPos(East).clickOK.validate

      en.enterPlayer(North, prefixThatMatchesNoOne)
      en.isPlayerSuggestionsVisible(North) mustBe true
      val sugN = en.getPlayerSuggestions(North)
      eventually {
        sugN.size mustBe 1
        sugN.head.text mustBe "No names matched"
      }

      en.clickCancel.validate.clickCompletedScoreboard.validate
    }
  }

  it should "go to second game and delete it" in {
    import SessionDirector._

    PageBrowser.withClueAndScreenShot(screenshotDir, "DeleteSecondGame", "") {

//      val sb = ScoreboardPage.goto(dupid2.get).validate
      val sb = ScoreboardPage.current
      val menu = sb.clickMainMenu.validate
      eventually {menu.findElemById("Director")}
      val dsb = menu.clickDirectorButton.validate

      dsb.isPopupDisplayed mustBe false

      val pop = dsb.clickDelete.validatePopup()
      // Thread.sleep(2000L)
      val dsb2 = pop.clickPopUpCancel.validatePopup(false)

      val listpage = dsb2.clickDelete.validatePopup().clickDeleteOk.validate
      listpage.getMatchIds must not contain dupid2.get
    }
  }

  it should "go to first game when it is selected on summary page" in {
    import SessionDirector._

    val ld = ListDuplicatePage.current.validate

    ld.validate(dupid.get)

    ld.getMatchIds must contain (MatchDuplicate.id(dupid.get))

    val sb = ld.clickDuplicate(dupid.get).validate

    val menu = sb.clickMainMenu.validate
    eventually {menu.findElemById("Summary")}
    val listpage = menu.clickSummary.validate.clickHome
  }

  var importZipFile: Option[File] = None
  it should "export zip" in {
    import SessionDirector._

    val hp = HomePage.current.validate

//    val menu = hp.clickMainMenu.validate
//    eventually {menu.findElemById("Export")}
    val ep = hp.clickExport.validate

    val f = ep.export

    importZipFile = Some(f)

    testlog.info( s"Downloaded export zip: ${f}" )

    import scala.jdk.CollectionConverters._
    Using.resource( new ZipFile(f.jfile) ) { zip =>
      zip.entries().asScala.map { ze => ze.getName }.toList must contain( s"store/MatchDuplicate.${dupid.get}.yaml" )
    }

    ep.clickHome
  }

  it should "import zip" in {
    import SessionDirector._

    val hp = HomePage.current.validate  // .clickMainMenu.validate

    eventually {hp.findElemById("Import")}

    val ip = hp.clickImport.validate

    val initcount = ip.getImportedIds.length

    val (rp,row) = ip.selectFile(importZipFile.get).validateSuccess(importZipFile,initcount)

    val ldp = rp.importDuplicate(importZipFile.get.name, row.get).validate

    ldp.checkResults(dupid.get, listDuplicateResult:_*)

    val ldpr = ldp.clickImportDuplicate( dupid.get )

    val newId = ldpr.checkSuccessfulImport( dupid.get )

    val bp = ldpr.clickPopUpOk.validate.clickHome.validate.clickListDuplicateButton.validate( newId ).clickDuplicate(newId).validate.clickBoardToBoard(1)
    bp.validatePictures( ids = nsForTable1Round1.toList )
    val bp1 = bp.clickShowPicture(nsForTable1Round1.get).validatePictures(true).clickOkPicture.validatePictures( ids = nsForTable1Round1.toList )
    val sp = bp1.clickScoreboard.validate
    sp.clickMainMenu.validateMainMenu.clickSummary.validate
  }


  it should "go to the pair suggestion page" in {
    import SessionDirector._

    val lp = ListDuplicatePage.current.validate.clickMainMenu.validate
    eventually {
      lp.findElemById("Suggest")
    }
    lp.clickSuggestion.validate

  }

  it should "show calculate button with 8 known names selected" in {
    import SessionDirector._

    val sug = SuggestionPage.current.validate
    sug.getNumberNameFields mustBe 8

    sug.isCalculateEnabled mustBe false

    sug.getKnownNames must contain allElementsOf( matchedNames )

    val ss = (0 until 8).foldLeft(sug) { (s,n) =>
      sug.toggleKnownName(n)
    }

    ss.getNumberNameFields mustBe 0

    ss.isCalculateEnabled mustBe true
  }

  it should "show calculate button with 7 known names selected and one entered name" in {
    import SessionDirector._

    val sug = SuggestionPage.current
    val ss = sug.toggleKnownName(0)

    eventually {
      ss.getNumberKnownNames must be >= 8
      ss.getNumberChecked mustBe 7
      ss.getNumberNameFields mustBe 1
    }

    ss.isCalculateEnabled mustBe false

    val se = ss.setNameField(0, "Iqbal")

    eventually {
      se.isCalculateEnabled mustBe true
    }

  }

  it should "show never pair table" in {
    import SessionDirector._

    val sug = SuggestionPage.current
    sug.clickNeverPair
    val neverPairNames = eventually {
      val ns = sug.getNeverPairTableNames
      ns.length mustBe 8
      ns
    }

    val checkNames = sug.getKnownNames.drop(1).take(7)
    val checkNames2 = Checkbox.findAllChecked().map(e => e.label.text.trim)
    checkNames mustBe checkNames2
    val players = "Iqbal"::checkNames

    players must contain theSameElementsAs neverPairNames

    sug.takeScreenshot(docsScreenshotDir, "PairingsEnter")
  }

  it should "calculate a pairing" in {
    import SessionDirector._

    val sug = SuggestionPage.current
    val ss = sug.clickCalculate.validate

    eventually {
      ss.findButton("ToggleDetails") mustBe Symbol("displayed")
    }

    val se = SuggestionPage.current
    se.pageType mustBe SuggestionPage.ResultWithNeverPair

    se.clickCancelNeverPair.validate.takeScreenshot(docsScreenshotDir, "Pairings")
  }

  it should "go to duplicate list page from suggestion page" in {
    import SessionDirector._

    SuggestionPage.current.clickCancel.validate
  }

  it should "show server URLs" in {
    import SessionDirector._

    val ldp = ListDuplicatePage.current
    ldp.withClueAndScreenShot(screenshotDir, "ShowServerURLs", "Unable to see server URLs") {
      val ldpurl = ldp.clickServerURL.validateServerURL
      val urls = ldpurl.getServerURLs
      urls.length mustBe 1
      ldpurl.checkForValidServerURLs
      ldpurl.clickServerURLOK.validate
    }
  }

  it should "save the matches in the store" in {
    val support = backend.duplicates.persistent.support
    val future = backend.duplicates.readAll().map { rm =>
      rm match {
        case Right(map) =>
          val dir = new java.io.File(finalMatchDir)
          dir.mkdirs()
          map.foreach { e =>
            val (id,md) = e
            val mdf = new java.io.File(dir,s"MatchDuplicate.${md.id}.yaml")
            val s = support.toJSON(md)
            FileIO.writeFile(mdf,s)
          }
        case Left(err) =>
          fail(s"Error getting matches: $err")
      }
    }
    Await.ready(future, 10.seconds)
  }
}
