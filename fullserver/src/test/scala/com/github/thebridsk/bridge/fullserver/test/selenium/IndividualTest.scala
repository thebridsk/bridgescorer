package com.github.thebridsk.bridge.fullserver.test.selenium

import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.bridge.fullserver.test.pages.individual.AllHandsInMatch
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.BoardSetsPage
import com.github.thebridsk.bridge.fullserver.test.pages.individual.IndividualMovementsPage
import com.github.thebridsk.bridge.fullserver.test.pages.individual.HandsOnBoard
import com.github.thebridsk.bridge.fullserver.test.pages.individual.EnterHand
import com.github.thebridsk.bridge.data.bridge._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.BeforeAndAfterAll
import com.github.thebridsk.bridge.server.test.util.EventuallyUtils
import org.scalatest.CancelAfterFailure
import com.github.thebridsk.bridge.server.test.util.TestServer
import java.util.concurrent.TimeUnit
import org.scalatest.concurrent.Eventually
import com.github.thebridsk.bridge.server.test.util.ParallelUtils
import org.scalatest.time.Span
import org.scalatest.time.Millis
import com.github.thebridsk.bridge.server.test.util.MonitorTCP
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.util.Strings
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage
import com.github.thebridsk.bridge.fullserver.test.pages.individual.ListDuplicatePage
import com.github.thebridsk.bridge.fullserver.test.pages.individual.NewDuplicatePage
import com.github.thebridsk.bridge.fullserver.test.pages.individual.ScoreboardPage
import com.github.thebridsk.bridge.fullserver.test.pages.individual.TablePage
import com.github.thebridsk.bridge.fullserver.test.pages.LightDarkAddOn
import com.github.thebridsk.bridge.fullserver.test.pages.individual.OtherHandPlayed
import com.github.thebridsk.bridge.fullserver.test.pages.individual.TableEnterNamesPage
import com.github.thebridsk.bridge.fullserver.test.pages.individual.HandPage
import com.github.thebridsk.bridge.fullserver.test.pages.individual.OtherHand
import com.github.thebridsk.bridge.fullserver.test.pages.individual.OtherHandNotPlayed
import com.github.thebridsk.bridge.fullserver.test.pages.individual.BoardPage
import com.github.thebridsk.bridge.data.IndividualBoard
import org.openqa.selenium.WebDriver
import com.github.thebridsk.bridge.data.IndividualDuplicate
import scala.concurrent.Await
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.fullserver.test.pages.individual.HandViewType
import com.github.thebridsk.bridge.fullserver.test.pages.individual.HandDirectorView
import com.github.thebridsk.bridge.fullserver.test.pages.individual.HandCompletedView
import com.github.thebridsk.bridge.fullserver.test.pages.individual.HandTableView


object IndividualTest {

  val log: Logger = Logger[IndividualTest]()

  val screenshotDir = "target/IndividualPages"
  val docsScreenshotDir = "target/docs/Individual"

  TestStartLogging.startLogging()

  val cm = Strings.checkmark
  val bl = ""
  val zr = "0"

  val players: List[String] = List(
    "Nick", "Sam",
    "Ethan", "Wayne",
    "Ellen", "Wilma",
    "Nora", "Sally"
  )

  def getPlayer(i: Int): ScoreboardPage.Player = {
    ScoreboardPage.Player(i, getPlayerName(i))
  }

  def getPlayerName(i: Int): String = {
    players(i-1)
  }

  def getPlayerTextInHand(i: Int): String = {
    s"${i} ${getPlayerName(i)}"
  }

  val movement = "Individual2Tables"
  val boardset = "StandardBoards"

  case class FirstRoundBoards(
    hob: HandsOnBoard,
    other: List[OtherHand],
    hoballhands: HandsOnBoard,
    otherallhands: List[OtherHand]
  ) {
    def getForCheckFirstRound = hob.setOther(other:_*)
    def getForCheck = {
      val o = if (otherallhands != null) otherallhands else other
      val h = if (hoballhands != null) hoballhands else hob
      h.setOther(o:_*)
    }
  }

  lazy val firstRoundHands: List[FirstRoundBoards] = List(
    FirstRoundBoards(
      HandsOnBoard(
        1, 1, 1,
        EnterHand(
          8, 1, 5, 7,
          110, 0, 0, 0,  // neither vul
          1, Spades, NotDoubled, North, Made, 2, NotVul
        )
      ),
      OtherHandNotPlayed(2, 1, 1)::Nil,
      HandsOnBoard(
        1, 1, 1,
        EnterHand(
          8, 1, 5, 7,
          110, 2, 0, 1,  // neither vul
          1, Spades, NotDoubled, North, Made, 2, NotVul
        )
      ),
      OtherHandPlayed(2, 1, 1, 0, 2, -1, 1)::Nil
    ),
    FirstRoundBoards(
      HandsOnBoard(
        2, 1, 2,
        EnterHand(
          2, 6, 4, 3,
          110, 0, 0, 0,  // ns vul
          1, Spades, NotDoubled, North, Made, 2, Vul
        )
      ),
      OtherHandNotPlayed(1, 1, 2)::Nil,
      HandsOnBoard(
        2, 1, 2,
        EnterHand(
          2, 6, 4, 3,
          110, 1, 1, 0,  // ns vul
          1, Spades, NotDoubled, North, Made, 2, Vul
        )
      ),
      OtherHandPlayed(1, 1, 2, 1, 1, 0, 0)::Nil
    ),

    FirstRoundBoards(
      HandsOnBoard(
        1, 1, 2,
        EnterHand(
          8, 1, 5, 7,
          110, 1, 1, 0,  // ns vul
          1, Spades, NotDoubled, North, Made, 2, Vul
        )
      ),
      OtherHandPlayed(2, 1, 2, 1, 1, 0, 0)::Nil,
      null,
      null
    ),
    FirstRoundBoards(
      HandsOnBoard(
        2, 1, 1,
        EnterHand(
          2, 6, 4, 3,
          80, 0, 2, -1,  // neither vul
          1, Spades, NotDoubled, North, Made, 1, NotVul
        )
      ),
      OtherHandPlayed(1, 1, 1, 2, 0, 1, -1)::Nil,
      null,
      null
    ),
  )

  val allrounds: List[Int] = 1::2::3::4::5::6::7::Nil

  lazy val allHands: AllHandsInMatch = new AllHandsInMatch(
    firstRoundHands.map { h => h.getForCheck }:::
    List(
      HandsOnBoard(
        1, 1, 3,
        EnterHand(
          8, 1, 5, 7,
          400, 1, 1, 0,  // ew vul
          3, NoTrump, NotDoubled, North, Made, 3, NotVul
        ),
        OtherHandPlayed(2, 1, 3, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        2, 1, 3,
        EnterHand(
          2, 6, 4, 3,
          400, 1, 1, 0,  // ew vul
          3, NoTrump, NotDoubled, North, Made, 3, NotVul
        ),
        OtherHandPlayed(1, 1, 3, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        1, 2, 4,
        EnterHand(
          8, 2, 6, 1,
          600, 1, 1, 0,  // both vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(2, 2, 4, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        2, 2, 4,
        EnterHand(
          3, 7, 5, 4,
          600, 1, 1, 0,  // both vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(1, 2, 4, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        1, 2, 5,
        EnterHand(
          8, 2, 6, 1,
          600, 1, 1, 0,  // ns vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(2, 2, 5, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        2, 2, 5,
        EnterHand(
          3, 7, 5, 4,
          600, 1, 1, 0,  // ns vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(1, 2, 5, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        1, 2, 6,
        EnterHand(
          8, 2, 6, 1,
          400, 1, 1, 0,  // ew vul
          3, NoTrump, NotDoubled, North, Made, 3, NotVul
        ),
        OtherHandPlayed(2, 2, 6, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        2, 2, 6,
        EnterHand(
          3, 7, 5, 4,
          400, 1, 1, 0,  // ew vul
          3, NoTrump, NotDoubled, North, Made, 3, NotVul
        ),
        OtherHandPlayed(1, 2, 6, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        1, 3, 7,
        EnterHand(
          8, 3, 7, 2,
          600, 1, 1, 0,  // both vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(2, 3, 7, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        2, 3, 7,
        EnterHand(
          4, 1, 6, 5,
          600, 1, 1, 0,  // both vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(1, 3, 7, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        1, 3, 8,
        EnterHand(
          8, 3, 7, 2,
          400, 1, 1, 0,  // neither vul
          3, NoTrump, NotDoubled, North, Made, 3, NotVul
        ),
        OtherHandPlayed(2, 3, 8, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        2, 3, 8,
        EnterHand(
          4, 1, 6, 5,
          400, 1, 1, 0,  // neither vul
          3, NoTrump, NotDoubled, North, Made, 3, NotVul
        ),
        OtherHandPlayed(1, 3, 8, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        1, 3, 9,
        EnterHand(
          8, 3, 7, 2,
          400, 1, 1, 0,  // ew vul
          3, NoTrump, NotDoubled, North, Made, 3, NotVul
        ),
        OtherHandPlayed(2, 3, 9, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        2, 3, 9,
        EnterHand(
          4, 1, 6, 5,
          400, 1, 1, 0,  // ew vul
          3, NoTrump, NotDoubled, North, Made, 3, NotVul
        ),
        OtherHandPlayed(1, 3, 9, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        1, 4, 10,
        EnterHand(
          8, 4, 1, 3,
          600, 1, 1, 0,  // both vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(2, 4, 10, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        2, 4, 10,
        EnterHand(
          5, 2, 7, 6,
          600, 1, 1, 0,  // both vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(1, 4, 10, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        1, 4, 11,
        EnterHand(
          8, 4, 1, 3,
          400, 1, 1, 0,  // neither vul
          3, NoTrump, NotDoubled, North, Made, 3, NotVul
        ),
        OtherHandPlayed(2, 4, 11, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        2, 4, 11,
        EnterHand(
          5, 2, 7, 6,
          400, 1, 1, 0,  // neither vul
          3, NoTrump, NotDoubled, North, Made, 3, NotVul
        ),
        OtherHandPlayed(1, 4, 11, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        1, 4, 12,
        EnterHand(
          8, 4, 1, 3,
          600, 1, 1, 0,  // ns vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(2, 4, 12, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        2, 4, 12,
        EnterHand(
          5, 2, 7, 6,
          600, 1, 1, 0,  // ns vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(1, 4, 12, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        1, 5, 13,
        EnterHand(
          8, 5, 2, 4,
          600, 1, 1, 0,  // both vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(2, 5, 13, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        2, 5, 13,
        EnterHand(
          6, 3, 1, 7,
          600, 1, 1, 0,  // both vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(1, 5, 13, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        1, 5, 14,
        EnterHand(
          8, 5, 2, 4,
          400, 1, 1, 0,  // neither vul
          3, NoTrump, NotDoubled, North, Made, 3, NotVul
        ),
        OtherHandPlayed(2, 5, 14, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        2, 5, 14,
        EnterHand(
          6, 3, 1, 7,
          400, 1, 1, 0,  // neither vul
          3, NoTrump, NotDoubled, North, Made, 3, NotVul
        ),
        OtherHandPlayed(1, 5, 14, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        1, 5, 15,
        EnterHand(
          8, 5, 2, 4,
          600, 1, 1, 0,  // ns vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(2, 5, 15, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        2, 5, 15,
        EnterHand(
          6, 3, 1, 7,
          600, 1, 1, 0,  // ns vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(1, 5, 15, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        1, 6, 16,
        EnterHand(
          8, 6, 3, 5,
          400, 1, 1, 0,  // ew vul
          3, NoTrump, NotDoubled, North, Made, 3, NotVul
        ),
        OtherHandPlayed(2, 6, 16, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        2, 6, 16,
        EnterHand(
          7, 4, 2, 1,
          400, 1, 1, 0,  // ew vul
          3, NoTrump, NotDoubled, North, Made, 3, NotVul
        ),
        OtherHandPlayed(1, 6, 16, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        1, 6, 17,
        EnterHand(
          8, 6, 3, 5,
          400, 1, 1, 0,  // neither vul
          3, NoTrump, NotDoubled, North, Made, 3, NotVul
        ),
        OtherHandPlayed(2, 6, 17, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        2, 6, 17,
        EnterHand(
          7, 4, 2, 1,
          400, 1, 1, 0,  // neither vul
          3, NoTrump, NotDoubled, North, Made, 3, NotVul
        ),
        OtherHandPlayed(1, 6, 17, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        1, 6, 18,
        EnterHand(
          8, 6, 3, 5,
          600, 1, 1, 0,  // ns vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(2, 6, 18, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        2, 6, 18,
        EnterHand(
          7, 4, 2, 1,
          600, 1, 1, 0,  // ns vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(1, 6, 18, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        1, 7, 19,
        EnterHand(
          8, 7, 4, 6,
          400, 1, 1, 0,  // ew vul
          3, NoTrump, NotDoubled, North, Made, 3, NotVul
        ),
        OtherHandPlayed(2, 7, 19, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        2, 7, 19,
        EnterHand(
          1, 5, 3, 2,
          400, 1, 1, 0,  // ew vul
          3, NoTrump, NotDoubled, North, Made, 3, NotVul
        ),
        OtherHandPlayed(1, 7, 19, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        1, 7, 20,
        EnterHand(
          8, 7, 4, 6,
          600, 1, 1, 0,  // both vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(2, 7, 20, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        2, 7, 20,
        EnterHand(
          1, 5, 3, 2,
          600, 1, 1, 0,  // both vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(1, 7, 20, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        1, 7, 21,
        EnterHand(
          8, 7, 4, 6,
          600, 1, 1, 0,  // ns vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(2, 7, 21, 1, 1, 0, 0)
      ),
      HandsOnBoard(
        2, 7, 21,
        EnterHand(
          1, 5, 3, 2,
          600, 1, 1, 0,  // ns vul
          3, NoTrump, NotDoubled, North, Made, 3, Vul
        ),
        OtherHandPlayed(1, 7, 21, 1, 1, 0, 0)
      ),
    ),
    players,
    BoardSetsPage.getBoardSet(boardset),
    IndividualMovementsPage.getMovement(movement)
  ).checkFixHands

}

class IndividualTest
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll
    with EventuallyUtils
    with CancelAfterFailure {
  import Eventually.{patienceConfig => _, _}
  import ParallelUtils._

  import IndividualTest._

  import scala.concurrent.duration._

  val SessionDirector = new DirectorSession()
  val SessionComplete = new CompleteSession()
  val SessionTable1 = new TableSession("1")
  val SessionTable2 = new TableSession("2")

  val timeoutMillis = 15000
  val intervalMillis = 1000

  val backend = TestServer.backend

//  case class MyDuration( timeout: Long, units: TimeUnit )
  type MyDuration = Duration
  val MyDuration = Duration

  implicit val timeoutduration: FiniteDuration =
    MyDuration(60, TimeUnit.SECONDS)

  val defaultPatienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(timeoutMillis, Millis)),
    interval = scaled(Span(intervalMillis, Millis))
  )
  implicit def patienceConfig: PatienceConfig = defaultPatienceConfig

  override def beforeAll(): Unit = {

    log.fine(s"DuplicateTestPages patienceConfig=${patienceConfig}")

    MonitorTCP.nextTest()
    TestStartLogging.startLogging()
    try {
      // The sessions for the tables and complete is defered to the test that gets the home page url.
      waitForFutures(
        "Starting browser or server",
        CodeBlock {
          SessionDirector
            .sessionStart(getPropOrEnv("SessionDirector"))
            .setQuadrant(1, 1024, 768)
        },
        CodeBlock { TestServer.start() }
      )
    } catch {
      case e: Throwable =>
        afterAll()
        throw e
    }
  }

  override def afterAll(): Unit = {
    waitForFuturesIgnoreTimeouts(
      "Stopping browsers and server",
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
    withClue("allHands must have all rounds defined in allrounds") {
      allHands.rounds.sorted mustBe allrounds.sorted
    }

    waitForFutures(
      "Starting browsers",
      CodeBlock {
        SessionTable1
          .sessionStart(getPropOrEnv("SessionTable1"))
          .setQuadrant(4, 1024, 768)
        allHands.boards
      },
      CodeBlock {
        SessionTable2
          .sessionStart(getPropOrEnv("SessionTable2"))
          .setQuadrant(3, 1024, 768)
      },
      CodeBlock {
        SessionComplete
          .sessionStart(getPropOrEnv("SessionComplete"))
          .setQuadrant(2, 1024, 768)
      },
      CodeBlock {
        import SessionDirector._
        captureLogsOnError {
          HomePage.goto.validate.takeScreenshot(docsScreenshotDir, "HomePage")
        }

      }
    )

  }

  it should "go to duplicate list page" in {
    import SessionDirector._

    captureLogsOnError {
      HomePage.current.clickListIndividualButton.validate
    }
  }

  it should "go to movements page" in {
    import SessionDirector._

    captureLogsOnError {

      val lp = ListDuplicatePage.current.validate.clickMainMenu.validate
      eventually {
        lp.findElemById("Movements")
      }
      lp.withClueAndScreenShot(
        screenshotDir,
        "Movement",
        "trying to click first movement"
      ) {
        val mp = lp.clickMovements.validate
        val mpi = mp.clickPlayIndividual.validate
          .takeScreenshot(screenshotDir, "MovementBefore")
        val mp1 = mpi.click(IndividualMovementsPage.movementsIndividual.head).validate
        mp1.clickOK.validate
      }
    }
  }

  it should "allow creating a new duplicate match" in {
    import SessionDirector._
    captureLogsOnError {

      val dp = ListDuplicatePage.current
      dp.withClueAndScreenShot(
        screenshotDir,
        "NewDuplicate",
        "clicking NewDuplicate button"
      ) {
        val nd = dp.clickNewDuplicateButton.validate
        nd.clickPlayIndividual.validate
          .takeScreenshot(docsScreenshotDir, "IndividualCreate")
      }
    }
  }

  it should "create a new duplicate match" in {
    import SessionDirector._
    captureLogsOnError {

      val curPage = NewDuplicatePage.current.validate

      val boards = IndividualMovementsPage.getBoardsFromMovement(movement)

      log.info(s"Boards are $boards")

      dupid = Option(curPage.clickIndividual(boardset, movement).validate(boards).dupid)
      dupid mustBe Symbol("defined")

      log.info(s"Individual Duplicate id is ${dupid.get}")

      allHands.boardsets mustBe Symbol("defined")
    }
  }

  var rounds: List[Int] = Nil

  it should "go to duplicate match game in complete, table 1, and table 2 browsers" in {
    tcpSleep(60)

    rounds = IndividualMovementsPage.getRoundsFromMovement(movement)

    waitForFutures(
      "got to matches",
      CodeBlock {
        import SessionDirector._
        captureLogsOnError {
          val menu = ScoreboardPage.current.clickMainMenu.validate
          eventually {
            menu.findElemById("Director")
          }
          menu.clickDirectorButton.validate
        }
      },
      CodeBlock {
        import SessionTable1._
        captureLogsOnError {
          ScoreboardPage
            .goto(dupid.get)
            .takeScreenshot(docsScreenshotDir, "ScoreboardFromTable")
            .validate
            .clickTableButton(1)
            .validate(rounds)
            .takeScreenshot(docsScreenshotDir, "TableRound1")
        }
      },
      CodeBlock {
        import SessionTable2._
        captureLogsOnError {
          TablePage.goto(dupid.get, "2", TablePage.EnterNames).validate(rounds)
        }
      },
      CodeBlock {
        import SessionComplete._
        try {
          val home = HomePage.goto.validate
          val h2 = home.clickToLightDark(LightDarkAddOn.DarkTheme)
          val sb = h2.clickListIndividualButton.validate(dupid.get)
          sb.clickIndividual(dupid.get).validate
        } catch {
          case x: Exception =>
            showLogs()
            throw x
        }
      }
    )
  }

  it should "allow players names to be entered at both tables" in {
    tcpSleep(60)
    waitForFutures(
      "allow players names to be entered at both tables",
      CodeBlock {
        import SessionTable1._
        var sk = TablePage
          .current(TablePage.EnterNames)
          .validate(rounds)
          .clickBoard(1, 1)
          .asInstanceOf[TableEnterNamesPage]
          .validate
        sk.enterPlayer(North, getPlayerName(8))
        sk.enterPlayer(South, getPlayerName(1))
        sk.enterPlayer(East, getPlayerName(5))
        sk.enterPlayer(West, getPlayerName(7))
        sk.setScorekeeper(North)
        val hand = sk.clickOK.asInstanceOf[HandPage].validate
      },
      CodeBlock {
        import SessionTable2._
        var sk = TablePage
          .current(TablePage.EnterNames)
          .validate(rounds)
          .clickBoard(1, 2)
          .asInstanceOf[TableEnterNamesPage]
          .validate
        sk.enterPlayer(North, getPlayerName(2))
        sk.enterPlayer(South, getPlayerName(6))
        sk.enterPlayer(East, getPlayerName(4))
        sk.enterPlayer(West, getPlayerName(3))
        sk.setScorekeeper(North)
        val hand = sk.clickOK.asInstanceOf[HandPage].validate
      }
    )
  }

  it should "have all the players set in the server" in {
    val f = TestServer.backend.individualduplicates.read(dupid.map{ s => IndividualDuplicate.id(s)}.get)
    Await.result(f, Duration(10, "seconds")) match {
      case Right(v) =>
        v.id.id mustBe dupid.get
        v.players mustBe players
      case Left(v) =>
        fail(s"Did not find IndividualDuplicate object with id ${dupid}")
    }
  }

  it should "play the first hand in round 1" in {
    tcpSleep(60)
    waitForFutures(
      "play the first hand in round 1",
      CodeBlock {
        import SessionTable1._

        enterHandAndCheckOther(HandPage.current.validate, firstRoundHands(0).getForCheckFirstRound)
      },
      CodeBlock {
        import SessionTable2._

        enterHandAndCheckOther(HandPage.current.validate, firstRoundHands(1).getForCheckFirstRound)
      },
    )
  }

  it should s"check boards already played in round 1" in {
    waitForFutures(
      "check boards already played in round 1",
      CodeBlock {
        import SessionComplete._

        val hands = firstRoundHands(0).getForCheckFirstRound :: firstRoundHands(1).getForCheckFirstRound :: Nil

        val b = hands.head.board

        val sp = ScoreboardPage.current.validate
        val bp = sp.clickBoardToBoard(b).validate
        val bp2 = validateBoardsInRound(bp, 1, true, hands)
        bp2.clickScoreboard.validate
      },
      CodeBlock {
        import SessionDirector._

        val hands = firstRoundHands(0).getForCheckFirstRound :: firstRoundHands(1).getForCheckFirstRound :: Nil

        val b = hands.head.board

        val sp = ScoreboardPage.current.validate
        val bp = sp.clickBoardToBoard(b).validate
        val bp2 = validateBoardsInRound(bp, 1, false, hands)
        bp2.clickScoreboard.validate
      }
    )
  }

  it should "play the remaining hand in round 1" in {
    tcpSleep(60)
    waitForFutures(
      "play the remaining hand in round 1",
      CodeBlock {
        import SessionTable1._

        val hob = firstRoundHands(0).getForCheckFirstRound
        val remaining = allHands.getHandsInTableRound(1, 1).filter(h => h.board != hob.board)
        val bp = BoardPage.current.validate
        val bp2 = enterRemainingHandsInRound(bp, 1, 1, remaining)
      },
      CodeBlock {
        import SessionTable2._

        val hob = firstRoundHands(1).getForCheckFirstRound
        val remaining = allHands.getHandsInTableRound(2, 1).filter(h => h.board != hob.board)
        val bp = BoardPage.current.validate
        val bp2 = enterRemainingHandsInRound(bp, 2, 1, remaining)
      },
    )
  }

  it should s"check boards in round 1" in {
    waitForFutures(
      "check boards in round 1",
      CodeBlock {
        import SessionTable1._

        val bp = BoardPage.current.validate
        val bp2 = validateBoardsOnTableInRound(bp, 1, 1, true)
        val sp2 = bp2.clickScoreboard.validate
        validateScoreboardToRound(sp2, 1, HandTableView(1, 1, 8, 1, 5, 7))
      },
      CodeBlock {
        import SessionTable2._

        val bp = BoardPage.current.validate
        val bp2 = validateBoardsOnTableInRound(bp, 2, 1, true)
        val sp2 = bp2.clickScoreboard.validate
        validateScoreboardToRound(sp2, 1, HandTableView(2, 1, 2, 6, 4, 3))
      },
      CodeBlock {
        import SessionComplete._

        val b = allHands.boards.head

        val sp = ScoreboardPage.current.validate
        val bp = sp.clickBoardToBoard(b).validate
        val bp2 = validateBoardsInRound(bp, 1, true)
        val sp2 = bp2.clickScoreboard.validate
        validateScoreboardToRound(sp2, 1, HandCompletedView)
      },
      CodeBlock {
        import SessionDirector._

        val b = allHands.boards.head

        val sp = ScoreboardPage.current.validate
        val bp = sp.clickBoardToBoard(b).validate
        val bp2 = validateBoardsInRound(bp, 1, false)
        val sp2 = bp2.clickScoreboard.validate
        validateScoreboardToRound(sp2, 1, HandDirectorView)
      }
    )
  }

  allrounds.tail.foreach { round =>
    it should s"play hands in round ${round}" in {
      waitForFutures(
        s"play hands in round ${round}",
        CodeBlock {
          import SessionTable1._

          val sp = ScoreboardPage.current.validate
          val tp = sp.clickTableButton(1).validate
          val bp2 = enterHandInRound(tp, 1, round)
        },
        CodeBlock {
          import SessionTable2._

          val sp = ScoreboardPage.current.validate
          val tp = sp.clickTableButton(2).validate
          val bp2 = enterHandInRound(tp, 2, round)
        },
      )
    }
    it should s"check boards in round ${round}" in {
      waitForFutures(
        s"check boards in round ${round}",
        CodeBlock {
          import SessionTable1._

          val b = allHands.getHandsInTableRound(1, round).head

          val bp = BoardPage.current.validate
          val bp2 = validateBoardsOnTableInRound(bp, 1, round, true)
          val sp2 = bp2.clickScoreboard.validate
          validateScoreboardToRound(sp2, round, HandTableView(1, round, b.hand.north, b.hand.south, b.hand.east, b.hand.west))
        },
        CodeBlock {
          import SessionTable2._

          val b = allHands.getHandsInTableRound(2, round).head

          val bp = BoardPage.current.validate
          val bp2 = validateBoardsOnTableInRound(bp, 2, round, true)
          val sp2 = bp2.clickScoreboard.validate
          validateScoreboardToRound(sp2, round, HandTableView(2, round, b.hand.north, b.hand.south, b.hand.east, b.hand.west))
        },
        CodeBlock {
          import SessionComplete._

          val b = allHands.boards.head

          val sp = ScoreboardPage.current.validate
          val bp = sp.clickBoardToBoard(b).validate
          val bp2 = validateBoardsInRound(bp, round, true)
          val sp2 = bp2.clickScoreboard.validate
          validateScoreboardToRound(sp2, round, HandCompletedView)
        },
        CodeBlock {
          import SessionDirector._

          val b = allHands.boards.head

          val sp = ScoreboardPage.current.validate
          val bp = sp.clickBoardToBoard(b).validate
          val bp2 = validateBoardsInRound(bp, round, false)
          val sp2 = bp2.clickScoreboard.validate
          validateScoreboardToRound(sp2, round, HandDirectorView)
        }
      )
    }
  }

  /**
    * Enter the hand
    *
    * @param hp the HandPage currently showing
    * @param hob the hand to play
    * @param webDriver
    * @return The BoardPage with the result of the entered hand
    */
  def enterHand(hp: HandPage, hob: HandsOnBoard)(implicit webDriver: WebDriver): BoardPage = {

    hp.dupid mustBe dupid.get
    hp.viewtype mustBe ScoreboardPage.TableViewType(s"${hob.table}", s"${hob.round}")
    hp.board mustBe IndividualBoard.id(hob.board).id
    hp.hand mustBe s"p${hob.hand.north}"
    hp.getPlayer(North).toString() mustBe getPlayerTextInHand(hob.hand.north)
    hp.getPlayer(South).toString() mustBe getPlayerTextInHand(hob.hand.south)
    hp.getPlayer(East).toString() mustBe getPlayerTextInHand(hob.hand.east)
    hp.getPlayer(West).toString() mustBe getPlayerTextInHand(hob.hand.west)
    allHands.getBoardFromBoardSet(hob.board) match {
      case Some(b) =>
        hp.checkVulnerable(Vulnerability(b.nsVul), Vulnerability(b.ewVul))

        hp.withClueAndScreenShot(
          screenshotDir,
          s"EnterR${hob.round}T${hob.table}${IndividualBoard.id(hob.board).id}",
          s"EnterR${hob.round}T${hob.table}${IndividualBoard.id(hob.board).id}"
        ) {
          val bp = hp.onlyEnterHand(
                    hob,
                    b,
                    getPlayer(hob.hand.north),
                    getPlayer(hob.hand.south),
                    getPlayer(hob.hand.east),
                    getPlayer(hob.hand.west)
                  ).validate
          bp
        }
      case None =>
        fail(s"Unable to find board ${hob.board}")
    }

  }

  /**
    * Enter the hand and check the BoardPage results
    *
    * @param hp the HandPage currently showing
    * @param hob the hand to play
    * @param webDriver
    * @return The BoardPage with the result of the entered hand
    */
  def enterHandAndCheckOther(hp: HandPage, hob: HandsOnBoard)(implicit webDriver: WebDriver): BoardPage = {
    val bp = enterHand(hp, hob)
    bp.checkOthers(
      hob,
      allHands
    )
    bp
  }

  /**
    * Enter the hand.  The current page must be a TableView of a BoardPage in same round of the hand to play.
    *
    * @param hob the hand to play
    * @param webDriver
    * @return The BoardPage with the result of the entered hand
    */
  def enterHandFromBoard(hob: HandsOnBoard)(implicit webDriver: WebDriver): BoardPage = {
    val boardp = BoardPage.current.validate
    val hp = boardp.clickBoardButton(hob.board).validate
    enterHand(hp, hob)
  }

  /**
    *
    * @param tp - the current page, a Table Page
    * @param table - the table, must be the same as tp is showing
    * @param round - the round to play
    * @return the board page with table view
    */
  def enterHandInRound(tp: TablePage, table: Int, round: Int)(implicit webDriver: WebDriver): BoardPage = {
    tp.validate.tableid mustBe Table.id(table).id
    val hands = allHands.getHandsInTableRound(table, round)

    if (hands.length > 0) {
      val tnp = tp.setTarget(TablePage.EnterNames).clickRound(round).asInstanceOf[TableEnterNamesPage].validate
      val sp = tnp.setScorekeeper(North).clickOK.asInstanceOf[ScoreboardPage].validate

      val hob1 = hands.head
      val hp1 = sp.clickBoardToHand(hob1.board).validate
      var bp = enterHand(hp1, hob1)
      bp = enterRemainingHandsInRound(bp, table, round, hands.tail)
      bp
    } else {
      fail(s"No hands played on table ${table} in round ${round}")
    }

  }

  def enterRemainingHandsInRound(bp: BoardPage, table: Int, round: Int, remaining: List[HandsOnBoard])(implicit webDriver: WebDriver): BoardPage = {
    var bb = bp
    for (hob <- remaining) {
      val hp = bb.clickBoardButton(hob.board).validate
      bb = enterHand(hp, hob)
    }
    bb
  }

  /**
    *
    * @param tp - the current page, a Table Page
    * @param table - the table, must be the same as tp is showing
    * @param round - the round to play
    * @param checkmarks - shows checkmarks for played hands if players haven't played yet
    * @return the board page with table view
    */
  def validateBoardsOnTableInRound(bp: BoardPage, table: Int, round: Int, checkmarks: Boolean)(implicit webDriver: WebDriver): BoardPage = {
    val hands = allHands.getHandsInTableRound(table, round)

    var bb = bp
    for (hob <- hands) {
      if (bb.boardId != IndividualBoard.id(hob.board).id) {
        bb = bb.clickPlayedBoard(hob.board).validate
      }
      bb = bb.checkHand(round, hob.board, allHands, checkmarks)
      bb = bb.checkOthers(hob, allHands, checkmarks)
    }
    val sp = bb.clickScoreboard.validate

    bb
  }

  /**
    *
    * @param tp - the current page, a Table Page, must not be on the first board in the round
    * @param table - the table, must be the same as tp is showing
    * @param round - the round to play
    * @param checkmarks - shows checkmarks for played hands if players haven't played yet
    * @return the board page with table view
    */
  def validateBoardsInRound(bp: BoardPage, round: Int, checkmarks: Boolean, hands: List[HandsOnBoard])(implicit webDriver: WebDriver): BoardPage = {

    var bb = bp
    for (hob <- hands) {
      if (bb.boardId != IndividualBoard.id(hob.board).id) {
        bb = bb.clickPlayedBoard(hob.board).validate
      }
      val allplayed =
          hob.other.find(oh => oh.isInstanceOf[OtherHandNotPlayed]).isEmpty
      log.fine(s"""validateBoardsInRound round=${round}, checkmarks=${checkmarks}, allplayed=${allplayed}, hob=${hob}""")
      if (!allplayed && checkmarks) {
        bb.takeScreenshot(screenshotDir)
        bb = bb.checkHandPlayedWithCheckmarks(hob.hand.north, hob.hand.south, hob.hand.east, hob.hand.west)
      } else {
        bb = bb.checkHandScores(hob.hand)
      }

      bb = bb.checkOthers(hob, allHands, checkmarks)
    }
    bb
  }

  /**
    *
    * @param tp - the current page, a Table Page, must not be on the first board in the round
    * @param table - the table, must be the same as tp is showing
    * @param round - the round to play
    * @param checkmarks - shows checkmarks for played hands if players haven't played yet
    * @return the board page with table view
    */
  def validateBoardsInRound(bp: BoardPage, round: Int, checkmarks: Boolean)(implicit webDriver: WebDriver): BoardPage = {
    val hands = allHands.getHandsInRound(round).distinctBy(hob => hob.board)
    validateBoardsInRound(bp, round, checkmarks, hands)
  }

  def validateScoreboardToRound(
    sp: ScoreboardPage,
    round: Int,
    viewtype: HandViewType
  )(
    implicit webDriver: WebDriver
  ): ScoreboardPage = {
    val (playerscores, places) = allHands.getScoreToRound(round, viewtype, false)
    log.fine(s"""validateScoreboardToRound round ${round} viewtype ${viewtype}
                |playerscores
                |${playerscores.mkString("  ","\n  ","")}
                |places
                |${places.mkString("  ","\n  ","")}
                |""".stripMargin)
    val f = viewtype match {
      case HandCompletedView => s"Director${round}"
      case HandDirectorView => s"Complete${round}"
      case HandTableView(table, round, north, south, east, west) => s"Table${table}_${round}"
    }
    sp.checkPlaceTable(screenshotDir, f, places:_*)
    sp.checkTable(playerscores:_*)
    sp
  }
}
