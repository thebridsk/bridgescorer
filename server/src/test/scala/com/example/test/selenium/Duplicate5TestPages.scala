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
import com.example.pages.duplicate.TableEnterMissingNamesPage
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
import com.example.pages.duplicate.TableEnterNamesPage
import com.example.pages.duplicate.TablePage.MissingNames
import com.example.test.util.MonitorTCP

object Duplicate5TestPages {

  val testlog = Logger[Duplicate5TestPages]

  val screenshotDir = "target/screenshots/Duplicate5TestPages"

  val cm = Strings.checkmark
  val bl = ""
  val zr = "0"
  val xx = Strings.xmark
  val half = Strings.half

  val team1 = Team( 1, "Nick", "Sam")
  val team2 = Team( 2, "Ethan", "Wayne")
  val team3 = Team( 3, "Ellen", "Wilma")
  val team4 = Team( 4, "Nora", "Sally")
  val team5 = Team( 5, "Alice", "Andy" )

  val peopleResult = List(
                        PeopleRow(team4.one,"100.00","100.00","47.50","1","1.00","1","0","9"+half,"20"),
                        PeopleRow(team4.two,"100.00","100.00","47.50","1","1.00","1","0","9"+half,"20"),
                        PeopleRow(team5.one,"0.00","0.00","45.00","0","0.00","1","0","9","20"),
                        PeopleRow(team5.two,"0.00","0.00","45.00","0","0.00","1","0","9","20"),
                        PeopleRow(team2.one,"0.00","0.00","40.00","0","0.00","1","0","8","20"),
                        PeopleRow(team2.two,"0.00","0.00","40.00","0","0.00","1","0","8","20"),
                        PeopleRow(team3.one,"0.00","0.00","37.50","0","0.00","1","0","7"+half,"20"),
                        PeopleRow(team3.two,"0.00","0.00","37.50","0","0.00","1","0","7"+half,"20"),
                        PeopleRow(team1.one,"0.00","0.00","30.00","0","0.00","1","0","6","20"),
                        PeopleRow(team1.two,"0.00","0.00","30.00","0","0.00","1","0","6","20")
      )


  val listDuplicateResult = List(
        team4.one+"\n1\n9"+half,
        team4.two+"\n1\n9"+half,
        team5.one+"\n2\n9",
        team5.two+"\n2\n9",
        team2.one+"\n3\n8",
        team2.two+"\n3\n8",
        team3.one+"\n4\n7"+half,
        team3.two+"\n4\n7"+half,
        team1.one+"\n5\n6",
        team1.two+"\n5\n6",
      )

  val prefixThatMatchesSomeNames = "e"
  lazy val matchedNames = allHands.teams.flatMap{ t => List(t.one,t.two).filter(n=> n.toLowerCase().startsWith(prefixThatMatchesSomeNames))}
  val prefixThatMatchesNoOne = "asdf"

  val movement = "Howell2Table5Teams"
  val boardset = "StandardBoards"

  lazy val allHands = AllHandsInMatch(
      List(team1,team2,team3,team4,team5),
      BoardSetsPage.getBoardSet(boardset).get,
      MovementsPage.getMovement(movement).get,

      // board 1    NS      EW
      EnterHand( 2, 110,0,  4,0,   1,Spades,NotDoubled,North,Made,2,NotVul)::
      EnterHand( 3,  80,0,  5,1,   1,Spades,NotDoubled,North,Made,1,NotVul)::
      Nil,

      // board 2    NS      EW
      EnterHand( 2, 110,0,  4,0,   2,Spades,NotDoubled,North,Made,2,Vul)::
      EnterHand( 3, 140,1,  5,0,   2,Spades,NotDoubled,North,Made,3,Vul)::
      Nil,

      // board 3    NS      EW
      EnterHand( 5, 140,0,   4,0,   3,Spades,NotDoubled,North,Made,3,NotVul)::
      EnterHand( 3, 140,0.5, 2,0.5, 3,Spades,NotDoubled,North,Made,3,NotVul)::
      Nil,

      // board 4    NS      EW
      EnterHand( 5, 620,0,   4,0,   4,Spades,NotDoubled,North,Made,4,Vul)::
      EnterHand( 3, 620,0.5, 2,0.5, 4,Spades,NotDoubled,North,Made,4,Vul)::
      Nil,

      // board 5    NS      EW
      EnterHand( 4, 650,0,   3,0,   5,Spades,NotDoubled,North,Made,5,Vul)::
      EnterHand( 1,   0,0,   5,1,   0,Spades,NotDoubled,North,Made,5,Vul)::
      Nil,

      // board 6    NS      EW
      EnterHand( 4,1010,0,   3,0,   6,Spades,NotDoubled,North,Made,7,NotVul)::
      EnterHand( 1, 980,0,   5,1,   6,Spades,NotDoubled,North,Made,6,NotVul)::
      Nil,

      // board 7    NS      EW
      EnterHand( 3,720,0,    5,0,   1,Hearts,Redoubled,North,Made,1,Vul)::
      EnterHand( 4,720,0.5,  1,0.5, 1,Hearts,Redoubled,North,Made,1,Vul)::
      Nil,

      // board 8    NS      EW
      EnterHand( 3,470,0,    5,0,   2,Hearts,Doubled,North,Made,2,NotVul)::
      EnterHand( 4,470,0.5,  1,0.5, 2,Hearts,Doubled,North,Made,2,NotVul)::
      Nil,

      // board 9    NS      EW
      EnterHand( 5,140,0,    4,0,   3,Hearts,NotDoubled,North,Made,3,NotVul)::
      EnterHand( 2,140,0.5,  1,0.5, 3,Hearts,NotDoubled,North,Made,3,NotVul)::
      Nil,

      // board 10   NS      EW
      EnterHand( 5,630,0,    4,0,   4,NoTrump,NotDoubled,North,Made,4,Vul)::
      EnterHand( 2,660,1,    1,0,   4,NoTrump,NotDoubled,North,Made,5,Vul)::
      Nil,

      // board 11   NS      EW
      EnterHand( 5,460,0,    2,0,   5,NoTrump,NotDoubled,North,Made,5,NotVul)::
      EnterHand( 4,460,0.5,  1,0.5, 5,NoTrump,NotDoubled,North,Made,5,NotVul)::
      Nil,

      // board 12   NS      EW
      EnterHand( 5,2220,0,   2,0,   7,NoTrump,NotDoubled,North,Made,7,Vul)::
      EnterHand( 4,2220,0.5, 1,0.5, 7,NoTrump,NotDoubled,North,Made,7,Vul)::
      Nil,

      // board 13   NS      EW
      EnterHand( 5, 70,0,   2,0,   1,Diamonds,NotDoubled,North,Made,1,Vul)::
      EnterHand( 1, 70,0.5, 3,0.5, 1,Diamonds,NotDoubled,North,Made,1,Vul)::
      Nil,

      // board 14   NS      EW
      EnterHand( 5, 90,0,   2,0,   2,Diamonds,NotDoubled,North,Made,2,NotVul)::
      EnterHand( 1, 90,0.5, 3,0.5, 2,Diamonds,NotDoubled,North,Made,2,NotVul)::
      Nil,

      // board 15   NS      EW
      EnterHand( 1,110,0,   5,0,   3,Diamonds,NotDoubled,North,Made,3,Vul)::
      EnterHand( 3,110,0.5, 2,0.5, 3,Diamonds,NotDoubled,North,Made,3,Vul)::
      Nil,

      // board 16   NS      EW
      EnterHand( 1,-100,0,  5,0,   4,Clubs,NotDoubled,North,Down,2,NotVul)::
      EnterHand( 3, -50,1,  2,0,   4,Clubs,NotDoubled,North,Down,1,NotVul)::
      Nil,

      // board 17   NS      EW
      EnterHand( 4,-150,0,   3,0,   5,Clubs,NotDoubled,North,Down,3,NotVul)::
      EnterHand( 2,-150,0.5, 1,0.5, 5,Clubs,NotDoubled,North,Down,3,NotVul)::
      Nil,

      // board 18   NS      EW
      EnterHand( 4,-100,0,    3,0,   6,Clubs,NotDoubled,North,Down,1,Vul)::
      EnterHand( 2,-100,0.5,  1,0.5, 6,Clubs,NotDoubled,North,Down,1,Vul)::
      Nil,

      // board 19   NS      EW
      EnterHand( 1,-150,0,    3,0,   5,Clubs,NotDoubled,North,Down,3,NotVul)::
      EnterHand( 2,-150,0.5,  4,0.5, 5,Clubs,NotDoubled,North,Down,3,NotVul)::
      Nil,

      // board 20   NS      EW
      EnterHand( 1,-100,0,    3,0,   6,Clubs,NotDoubled,North,Down,1,Vul)::
      EnterHand( 2,-100,0.5,  4,0.5, 6,Clubs,NotDoubled,North,Down,1,Vul)::
      Nil
  )

  // this is here to validate the AllHandsInMatch.getScoreToRound call
  val resultAfterOneRoundCheckMark = List(
        TeamScoreboard(team1.blankNames, 0, "0", List(xx,xx,xx,xx,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl)),
        TeamScoreboard(team2, 0, "0", List(bl,bl,bl,bl,xx,xx,xx,xx,bl,bl,cm,cm,cm,cm,bl,bl,bl,bl,bl,bl)),
        TeamScoreboard(team3, 0, "0", List(bl,bl,bl,bl,cm,cm,bl,bl,xx,xx,xx,xx,bl,bl,bl,bl,cm,cm,bl,bl)),
        TeamScoreboard(team4, 0, "0", List(bl,bl,bl,bl,cm,cm,bl,bl,bl,bl,bl,bl,xx,xx,xx,xx,cm,cm,bl,bl)),
        TeamScoreboard(team5, 0, "0", List(bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,cm,cm,cm,cm,bl,bl,xx,xx,xx,xx))
      )

  val resultAfterOneRoundZero = List(
        TeamScoreboard(team1.blankNames, 0, "0", List(xx,xx,xx,xx,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,bl)),
        TeamScoreboard(team2, 0, "0", List(bl,bl,bl,bl,xx,xx,xx,xx,bl,bl,zr,zr,zr,zr,bl,bl,bl,bl,bl,bl)),
        TeamScoreboard(team3, 0, "0", List(bl,bl,bl,bl,zr,zr,bl,bl,xx,xx,xx,xx,bl,bl,bl,bl,zr,zr,bl,bl)),
        TeamScoreboard(team4, 0, "0", List(bl,bl,bl,bl,zr,zr,bl,bl,bl,bl,bl,bl,xx,xx,xx,xx,zr,zr,bl,bl)),
        TeamScoreboard(team5, 0, "0", List(bl,bl,bl,bl,bl,bl,bl,bl,bl,bl,zr,zr,zr,zr,bl,bl,xx,xx,xx,xx))
      )
}

/**
 * Test going from the table view, by hitting a board button,
 * to the names view, to the hand view.
 * @author werewolf
 */
class Duplicate5TestPages extends FlatSpec with DuplicateUtils with MustMatchers with BeforeAndAfterAll with EventuallyUtils with ParallelUtils {
    import Eventually.{ patienceConfig => _, _ }

  import Duplicate5TestPages._

  import scala.concurrent.duration._

  val SessionDirector = new DirectorSession()
  val SessionComplete = new CompleteSession()
  val SessionTable1 = new TableSession("1")
  val SessionTable2 = new TableSession("2")

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
    TestStartLogging.startLogging()
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
                    Future { SessionTable1.sessionStop() },
                    Future { SessionTable2.sessionStop() },
                    Future { SessionComplete.sessionStop() },
                    Future { SessionDirector.sessionStop() },
                    Future { TestServer.stop() }
                    )
  }

  var dupid: Option[String] = None
  var boardSet: Option[BoardSet] = None

  behavior of "Duplicate test pages of Bridge Server"

  it should "go to the home page" in {
    import Session._
    waitForFutures(
      "Starting browsers",
      Future { SessionTable1.sessionStart(getPropOrEnv("SessionTable1")).setQuadrant(4) },
      Future { SessionTable2.sessionStart(getPropOrEnv("SessionTable2")).setQuadrant(3) },
      Future { SessionComplete.sessionStart(getPropOrEnv("SessionComplete")).setQuadrant(2) },
      Future {
        import SessionDirector._
        HomePage.goto.validate
      }
    )

  }

  it should "go to duplicate list page" in {
    import SessionDirector._

    tcpSleep(15)
    HomePage.current.clickListDuplicateButton.validate
  }

  it should "go to boardsets page" in {
    import SessionDirector._

    ListDuplicatePage.current.validate.clickBoardSets.validate.click(BoardSetsPage.boardsets.head).validate.clickOK.validate
  }

  it should "go to movements page" in {
    import SessionDirector._

    ListDuplicatePage.current.clickMovements.validate.click(MovementsPage.movements.head).validate.clickOK.validate
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

  it should "go to duplicate match game in complete, table 1, and table 2 browsers" in {
    tcpSleep(60)

    rounds = MovementsPage.getRoundsFromMovement(movement)

    waitForFutures(
      "Starting browsers",
      Future {
        import SessionDirector._
        ScoreboardPage.current.clickDirectorButton.validate
      },
      Future {
        import SessionTable1._
        ScoreboardPage.goto(dupid.get).validate.clickTableButton(1).validate(rounds)
      },
      Future {
        import SessionTable2._
        TablePage.goto(dupid.get,"2", EnterNames).validate(rounds)
      },
      Future {
        import SessionComplete._
        ScoreboardPage.goto(dupid.get).validate
      }
    )
  }

  it should "allow players names to be entered at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Entering Names",
      Future {
        import SessionTable1._
        val (nsTeam,ewTeam) = allHands.getNSEW(1, 1)
        var sk = TablePage.current(EnterNames).validate(rounds).clickRound(1).asInstanceOf[TableEnterScorekeeperPage].validate
        sk.isOKEnabled mustBe false
        sk = sk.enterScorekeeper(nsTeam.one).esc.clickPos(North)
        sk.isOKEnabled mustBe true
        sk.findSelectedPos mustBe Some(North)
        var en = sk.clickOK.validate
        en.isOKEnabled mustBe false
        en = en.enterPlayer(South, nsTeam.two).enterPlayer(East, ewTeam.one)
        en.isOKEnabled mustBe false
        en = en.enterPlayer(West, ewTeam.two).esc
        en.isOKEnabled mustBe true
        val scoreboard = en.clickOK.asInstanceOf[ScoreboardPage].validate( allHands.getBoardsInTableRound(1, 1) )
      },
      Future {
        import SessionTable2._
        val (nsTeam,ewTeam) = allHands.getNSEW(2, 1)
        var sk = TablePage.current(EnterNames).validate(rounds).clickRound(1).asInstanceOf[TableEnterScorekeeperPage].validate
        sk.isOKEnabled mustBe false
        sk = sk.enterScorekeeper(nsTeam.one).esc.clickPos(North)
        sk.isOKEnabled mustBe true
        sk.findSelectedPos mustBe Some(North)
        var en = sk.clickOK.validate
        en.isOKEnabled mustBe false
        en = en.enterPlayers(nsTeam.two, ewTeam.one, ewTeam.two).esc
        en.isOKEnabled mustBe true
        val scoreboard = en.clickOK.asInstanceOf[ScoreboardPage].validate( allHands.getBoardsInTableRound(2, 1) )
      }
    )
  }

  it should "allow first round to be played at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Playing first round",
      Future {
        import SessionTable1._
        val (nsTeam,ewTeam) = allHands.getNSEW(1, 1)
        playRound(ScoreboardPage.current, 1, 1, North, false)
      },
      Future {
        import SessionTable2._
        val (nsTeam,ewTeam) = allHands.getNSEW(2, 1)
        playRound(ScoreboardPage.current, 2, 1, North, false)
      }
    )
  }

  def checkPlayedBoards(
        sb: ScoreboardPage,
        checkmarks: Boolean,
        table: Option[Int],
        round: Int
      )( implicit
         webDriver: WebDriver
      ): ScoreboardPage = {
    val boards = table match {
      case Some(t) => allHands.getBoardsInTableRound(t, round)
      case None => allHands.getBoardsInRound(round)
    }

    sb.withClueAndScreenShot(screenshotDir, s"screenshotTable${table.getOrElse(0)}Round${round}", s"Table ${table}, round ${round} looking at boards ${boards}") {
      sb.validate
      val pr = boards.foldLeft( None: Option[BoardPage] ) { (progress,board) =>
        val bb = progress match {
          case Some(bp) => bp.clickPlayedBoard(board).validate
          case None => sb.clickBoardToBoard(board).validate
        }

        Some(bb.checkHand(round, board, allHands, checkmarks))
      }

      pr.map(bp=>bp.clickScoreboard).getOrElse(sb)
    }
  }

  it should "show the director's scoreboard and complete scoreboard shows checkmarks for the played games" in {
    tcpSleep(10)
    waitForFutures(
      "Checking scoreboards",
      Future{
        import SessionDirector._

        val sb = ScoreboardPage.current
        sb.checkTable(resultAfterOneRoundZero:_*)
        val (ts,pes) = allHands.getScoreToRound(1, HandDirectorView)
        val (ts1,pes1) = fixTables(ts, pes, 1)
        sb.checkTable( ts1: _*)
        sb.checkPlaceTable( pes1: _*)
        checkPlayedBoards( sb, false, None, 1 )
      },
      Future{
        import SessionComplete._

        val sb = ScoreboardPage.current
        sb.checkTable(resultAfterOneRoundCheckMark:_*)
        val (ts,pes) = allHands.getScoreToRound(1, HandCompletedView)
        val (ts1,pes1) = fixTables(ts, pes, 1)
        sb.checkTable( ts1: _*)
        sb.checkPlaceTable( pes1: _*)
        checkPlayedBoards( sb, true, None, 1 )
      },
      Future{
        import SessionTable1._

        val sb = ScoreboardPage.current
        sb.checkTable(resultAfterOneRoundCheckMark:_*)
        val (ts,pes) = allHands.getScoreToRound(1, HandTableView( 1, 1, team1.teamid, team2.teamid ))
        val (ts1,pes1) = fixTables(ts, pes, 1)
        sb.checkTable( ts1: _*)
        sb.checkPlaceTable( pes1: _*)
        checkPlayedBoards( sb, false, Some(1), 1 )
      },
      Future{
        import SessionTable2._

        val sb = ScoreboardPage.current
        sb.checkTable(resultAfterOneRoundCheckMark:_*)
        val (ts,pes) = allHands.getScoreToRound(1, HandTableView( 2, 1, team4.teamid, team3.teamid ))
        val (ts1,pes1) = fixTables(ts, pes, 1)
        sb.checkTable( ts1: _*)
        sb.checkPlaceTable( pes1: _*)
        checkPlayedBoards( sb, false, Some(2), 1 )
      }
    )
  }

  def selectScorekeeper( currentPage: ScoreboardPage,
                         table: Int, round: Int,
                         scorekeeper: PlayerPosition,
                         mustswap: Boolean
                       )( implicit
                           webDriver: WebDriver
                       ) = {
    val (ns,ew) = allHands.getNSEW(table, round)
    val tp = currentPage.clickTableButton(table).validate.setTarget(SelectNames)
    val ss = tp.clickRound(round).asInstanceOf[TableSelectScorekeeperPage].validate

    val sn = ss.verifyAndSelectScorekeeper(ns.one, ns.two, ew.one, ew.two, scorekeeper)
    sn.verifyNamesAndSelect(ns.teamid, ew.teamid, ns.one, ns.two, ew.one, ew.two, scorekeeper, mustswap).asInstanceOf[ScoreboardPage]
  }

  it should "allow selecting players for round 2" in {
    tcpSleep(10)
    waitForFutures(
      "Selecting players for round 2",
      Future{
        import SessionTable1._
        val sb = selectScorekeeper(ScoreboardPage.current,1,2,North,false )
      },
      Future{
        import SessionTable2._
        val (ns,ew) = allHands.getNSEW(2, 2)

        val em = ScoreboardPage.current.
                                clickTableButton(2).validate.
                                setTarget(MissingNames).
                                clickRound(2).asInstanceOf[TableEnterMissingNamesPage].validate
        em.getInputFieldNames.foreach( p => p match {
          case North => em.enterPlayer(North, ns.one)
          case South => em.enterPlayer(South, ns.two)
          case East => em.enterPlayer(East, ew.one)
          case West => em.enterPlayer(West, ew.two)
        })
        val ss = em.esc.validate.clickOK
        val sn = ss.verifyAndSelectScorekeeper(ns.one, ns.two, ew.one, ew.two, North)
        sn.verifyNamesAndSelect(ns.teamid, ew.teamid, ns.one, ns.two, ew.one, ew.two, North, false).asInstanceOf[ScoreboardPage]
      }
    )
  }

  it should "allow second round to be played at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Playing second round",
      Future {
        import SessionTable1._
        playRound(ScoreboardPage.current, 1, 2, North, false)
      },
      Future {
        import SessionTable2._
        playRound(ScoreboardPage.current, 2, 2, North, false)
      }
    )
  }

  it should "show the results on the scoreboards after round 2" in {
    tcpSleep(10)
    waitForFutures(
      "Checking scoreboards",
      Future{
        import SessionDirector._

        val sb = ScoreboardPage.current
        val (ts,pes) = allHands.getScoreToRound(2, HandDirectorView)
        sb.checkTable( ts: _*)
        sb.checkPlaceTable( pes: _*)
        checkPlayedBoards( sb, false, None, 2 )
      },
      Future{
        import SessionComplete._

        val sb = ScoreboardPage.current
        val (ts,pes) = allHands.getScoreToRound(2, HandCompletedView)
        sb.checkTable( ts: _*)
        sb.checkPlaceTable( pes: _*)
        checkPlayedBoards( sb, true, None, 2 )
      },
      Future{
        import SessionTable1._

        val sb = ScoreboardPage.current
        val (ts,pes) = allHands.getScoreToRound(2, HandTableView( 1, 2, team1.teamid, team2.teamid ))
        sb.checkTable( ts: _*)
        sb.checkPlaceTable( pes: _*)
        checkPlayedBoards( sb, false, Some(1), 2 )
      },
      Future{
        import SessionTable2._

        val sb = ScoreboardPage.current
        val (ts,pes) = allHands.getScoreToRound(2, HandTableView( 2, 2, team3.teamid, team4.teamid ))
        sb.checkTable( ts: _*)
        sb.checkPlaceTable( pes: _*)
        checkPlayedBoards( sb, false, Some(2), 2 )
      }
    )
  }

  /**
   * Fix the tables to accound for not having all the names in the first round
   */
  def fixTables( ts: List[TeamScoreboard],
                 pes: List[ScoreboardPage.PlaceEntry],
                 round: Int
               ): (List[TeamScoreboard],List[ScoreboardPage.PlaceEntry]) = {
    if (round == 1) {
      val notplaying = allHands.getTeamThatDidNotPlay(round)
      val ts1 = {
        ts.map { t =>
          if (notplaying.contains(t.team.teamid)) {
            t.copy(team = t.team.blankNames )
          } else {
            t
          }
        }
      }
      val pes1 = {
        pes.map { p =>
          val nteams = p.teams.map{ t =>
            if (notplaying.contains(t.teamid)) {
              t.blankNames
            } else {
              t
            }
          }
          p.copy( teams = nteams )
        }
      }
      (ts1,pes1)
    } else {
      (ts,pes)
    }
  }

  /**
   * Only plays the round.  The positions of the players must already be selected.
   */
  def playRound(
      currentPage: ScoreboardPage,
      table: Int,
      round: Int,
      scorekeeper: PlayerPosition,
      mustswap: Boolean
    )( implicit
         webDriver: WebDriver
    ) = {

    val (nsTeam,ewTeam) = allHands.getNSEW(table, round)
    val boards = allHands.getBoardsInTableRound(table, round)

    withClue(s"On table ${table} round ${round}") {

      val board = withClue( s"""board ${boards.head}""" ) {
        val hand = currentPage.clickBoardToHand(boards.head).validate
        hand.setInputStyle("Prompt")
        val brd = hand.enterHand( table, round, boards.head, allHands, nsTeam, ewTeam)
        brd.checkBoardButtons(boards.head,true,boards.head).checkBoardButtons(boards.head,false, boards.tail:_*).checkBoardButtonSelected(boards.head)
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
          currentBoard = hand2.enterHand( table, round, b, allHands, nsTeam, ewTeam)
          currentBoard = currentBoard.checkBoardButtons(b,true,playedBoards:_*).checkBoardButtons(b,false, unplayedBoards:_*).checkBoardButtonSelected(b)
        }

      }

      val sbr = currentBoard.clickScoreboard.validate
      val (ts,pes) = allHands.getScoreToRound(round, HandTableView( table, round, nsTeam.teamid, ewTeam.teamid ))
      val (ts1,pes1) = fixTables(ts, pes, round)
      sbr.checkTable( ts1: _*)
      sbr.checkPlaceTable( pes1: _*)

      sbr
    }
  }

  it should "allow round 3 to be played at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Playing round 3",
      Future {
        import SessionTable1._
        val sb = selectScorekeeper(ScoreboardPage.current,1,3,North,false)
        playRound(sb,1,3,North,false )
      },
      Future {
        import SessionTable2._
        val sb = selectScorekeeper(ScoreboardPage.current,2,3,North,false )
        playRound(sb,2,3,North,false )
      }
    )
  }

  it should "show all boards" in {
    tcpSleep(60)
    waitForFutures(
      "Checking all boards",
      Future {
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
      Future {
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
      Future {
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
      Future {
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
      Future {
        import SessionTable1._
        val sb = BoardPage.current.clickScoreboard.validate
        val sb1 = selectScorekeeper(sb,1,4,North,false)
        playRound(sb1,1,4,North,false )
      },
      Future {
        import SessionTable2._
        val sb = BoardPage.current.clickScoreboard.validate
        val sb1 = selectScorekeeper(sb,2,4,North,false)
        playRound(sb1,2,4,North,false )
      }
    )
  }

  it should "allow round 5 to be played at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "Playing round 5",
      Future {
        import SessionTable1._
        val sb = BoardPage.current.clickScoreboard.validate
        val sb1 = selectScorekeeper(sb,1,5,North,false)
        playRound(sb1,1,5,North,false)
      },
      Future {
        import SessionTable2._
        val sb = BoardPage.current.clickScoreboard.validate
        val sb1 = selectScorekeeper(sb,2,5,North,false)
        playRound(sb1,2,5,North,false )
      }
    )
  }

  it should "show the result on the summary page" in {
    import SessionComplete._

    dupid match {
      case Some(id) =>
        val page = ScoreboardPage.current.clickSummary.validate( id )
        page.checkResults(id, listDuplicateResult:_*)
      case None =>
        ScoreboardPage.current.clickSummary.validate
    }
  }

  it should "show the people page" in {
    import SessionComplete._

    val sb = ListDuplicatePage.current
    val ids = sb.getMatchIds
    val peoplePage = sb.clickPairs.validate.clickPeopleDetails

    if (ids.size == 1) {
      peoplePage.checkPeople( peopleResult:_*)
    } else {
      testlog.info(s"Not testing the people page with results, number of matchs played is ${ids.size}")
    }

  }

}
