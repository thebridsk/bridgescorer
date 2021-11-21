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
          110, 0, 0, 0,
          1, Spades, NotDoubled, North, Made, 2, NotVul
        )
      ),
      OtherHandNotPlayed(2, 1, 1)::Nil,
      HandsOnBoard(
        1, 1, 1,
        EnterHand(
          8, 1, 5, 7,
          110, 2, 0, 1,
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
          110, 0, 0, 0,
          1, Spades, NotDoubled, North, Made, 2, Vul
        )
      ),
      OtherHandNotPlayed(1, 1, 2)::Nil,
      HandsOnBoard(
        2, 1, 2,
        EnterHand(
          2, 6, 4, 3,
          110, 1, 1, 0,
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
          110, 1, 1, 0,
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
          80, 0, 2, -1,
          1, Spades, NotDoubled, North, Made, 1, NotVul
        )
      ),
      OtherHandPlayed(1, 1, 1, 2, 0, 1, -1)::Nil,
      null,
      null
    ),
  )

  lazy val allHands: AllHandsInMatch = new AllHandsInMatch(
    firstRoundHands.map { h => h.getForCheck }:::
    List(
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
          .takeScreenshot(screenshotDir, "MovementBefore")
        val mp1 = mp.click(IndividualMovementsPage.movements.head).validate
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
        dp.clickNewDuplicateButton.validate
          .takeScreenshot(docsScreenshotDir, "DuplicateCreate")
      }
    }
  }

  it should "create a new duplicate match" in {
    import SessionDirector._
    captureLogsOnError {

      val curPage = NewDuplicatePage.current

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
      "Starting browsers",
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
      "Entering Names",
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
      },
      // CodeBlock {
      //   import SessionComplete._
      //   // test for fix https://github.com/thebridsk/bridgescorer/pull/290
      //   // and https://github.com/thebridsk/bridgescorer/pull/299 either will fix
      //   // browser not being updated anymore.
      //   // The following code will setup the condition of the browser not
      //   // updating.  One of the following test cases will fail.
      //   val sb = ScoreboardPage.current
      //   val menu = sb.clickMainMenu.validate
      //   eventually { menu.findElemById("Summary") }
      //   val sum = menu.clickSummary.validate
      //   sum.clickDuplicate(dupid.get).validate
      // }
    )
  }

  // it should "have all the players set in the server" in {
  //   val f = TestServer.backend.individualduplicates.read(dupid.map{ s => IndividualDuplicate.id(s)}.get)
  //   Await.result(f, Duration(10, "seconds")) match {
  //     case Right(v) =>
  //       v.id.id mustBe dupid.get
  //       v.players mustBe players
  //     case Left(v) =>
  //       fail(s"Did not find IndividualDuplicate object with id ${dupid}")
  //   }
  // }

  it should "play the first hand in round 1" in {
    tcpSleep(60)
    waitForFutures(
      "Entering Names",
      CodeBlock {
        import SessionTable1._

        enterHand(HandPage.current.validate, firstRoundHands(0).getForCheckFirstRound)

        // val frh = firstRoundHands(0)
        // val hp = HandPage.current.validate
        // hp.dupid mustBe dupid.get
        // hp.viewtype mustBe ScoreboardPage.TableViewType("1", "1")
        // hp.board mustBe "B1"
        // hp.hand mustBe "p8"
        // hp.getPlayer(North).toString() mustBe getPlayerTextInHand(8)
        // hp.getPlayer(South).toString() mustBe getPlayerTextInHand(1)
        // hp.getPlayer(East).toString() mustBe getPlayerTextInHand(5)
        // hp.getPlayer(West).toString() mustBe getPlayerTextInHand(7)
        // hp.checkVulnerable(NotVul, NotVul)

        // hp.withClueAndScreenShot(
        //   screenshotDir,
        //   "EnterR1T1B1",
        //   "Enter R1T1B1"
        // ) {
        //   val bp = hp.onlyEnterHand(
        //             frh.getForCheckFirstRound,
        //             allHands.getBoardFromBoardSet(frh.hob.board).get,
        //             getPlayer(8),
        //             getPlayer(1),
        //             getPlayer(5),
        //             getPlayer(7)
        //           ).validate
        //   bp.checkOthers(
        //     frh.getForCheckFirstRound,
        //     allHands
        //   )
        // }
      },
      CodeBlock {
        import SessionTable2._

        enterHand(HandPage.current.validate, firstRoundHands(1).getForCheckFirstRound)

        // val frh = firstRoundHands(1)
        // val hp = HandPage.current.validate
        // hp.dupid mustBe dupid.get
        // hp.viewtype mustBe ScoreboardPage.TableViewType("2", "1")
        // hp.board mustBe "B2"
        // hp.hand mustBe "p2"
        // hp.getPlayer(North).toString() mustBe getPlayerTextInHand(2)
        // hp.getPlayer(South).toString() mustBe getPlayerTextInHand(6)
        // hp.getPlayer(East).toString() mustBe getPlayerTextInHand(4)
        // hp.getPlayer(West).toString() mustBe getPlayerTextInHand(3)
        // hp.checkVulnerable(Vul, NotVul)

        // hp.withClueAndScreenShot(
        //   screenshotDir,
        //   "EnterR1T2B2",
        //   "Enter R1T2B2"
        // ) {
        //   val bp = hp.onlyEnterHand(
        //             frh.getForCheckFirstRound,
        //             allHands.getBoardFromBoardSet(frh.hob.board).get,
        //             getPlayer(2),
        //             getPlayer(6),
        //             getPlayer(4),
        //             getPlayer(3)
        //           ).validate
        //   bp.checkOthers(
        //     frh.getForCheckFirstRound,
        //     allHands
        //   )
        // }
      },
    )
  }

  it should "play the second hand in round 1" in {
    tcpSleep(60)
    waitForFutures(
      "Entering Names",
      CodeBlock {
        import SessionTable1._

        enterHandFromBoard(firstRoundHands(2).getForCheckFirstRound)

        // val boardp = BoardPage.current.validate
        // val frh = firstRoundHands(2)
        // frh.hob.table mustBe 1
        // frh.hob.board mustBe 2
        // val hp = boardp.clickBoardButton(frh.hob.board).validate

        // hp.dupid mustBe dupid.get
        // hp.viewtype mustBe ScoreboardPage.TableViewType("1", "1")
        // hp.board mustBe "B2"
        // hp.hand mustBe "p8"
        // hp.getPlayer(North).toString() mustBe getPlayerTextInHand(8)
        // hp.getPlayer(South).toString() mustBe getPlayerTextInHand(1)
        // hp.getPlayer(East).toString() mustBe getPlayerTextInHand(5)
        // hp.getPlayer(West).toString() mustBe getPlayerTextInHand(7)
        // hp.checkVulnerable(Vul, NotVul)

        // hp.withClueAndScreenShot(
        //   screenshotDir,
        //   "EnterR1T1B2",
        //   "Enter R1T1B2"
        // ) {
        //   val bp = hp.onlyEnterHand(
        //             frh.getForCheckFirstRound,
        //             allHands.getBoardFromBoardSet(frh.hob.board).get,
        //             getPlayer(8),
        //             getPlayer(1),
        //             getPlayer(5),
        //             getPlayer(7)
        //           ).validate
        //   bp.checkOthers(
        //     frh.getForCheckFirstRound,
        //     allHands
        //   )
        // }
      },
      CodeBlock {
        import SessionTable2._

        enterHandFromBoard(firstRoundHands(3).getForCheckFirstRound)

        // val boardp = BoardPage.current.validate
        // val frh = firstRoundHands(3)
        // val hp = boardp.clickBoardButton(frh.hob.board).validate

        // hp.dupid mustBe dupid.get
        // hp.viewtype mustBe ScoreboardPage.TableViewType(s"${frh.hob.table}", s"${frh.hob.round}")
        // hp.board mustBe IndividualBoard.id(frh.hob.board).id
        // hp.hand mustBe s"p${frh.hob.hand.north}"
        // hp.getPlayer(North).toString() mustBe getPlayerTextInHand(frh.hob.hand.north)
        // hp.getPlayer(South).toString() mustBe getPlayerTextInHand(frh.hob.hand.south)
        // hp.getPlayer(East).toString() mustBe getPlayerTextInHand(frh.hob.hand.east)
        // hp.getPlayer(West).toString() mustBe getPlayerTextInHand(frh.hob.hand.west)
        // hp.checkVulnerable(NotVul, NotVul)

        // hp.withClueAndScreenShot(
        //   screenshotDir,
        //   s"EnterR${frh.hob.round}T${frh.hob.table}${IndividualBoard.id(frh.hob.board).id}",
        //   s"EnterR${frh.hob.round}T${frh.hob.table}${IndividualBoard.id(frh.hob.board).id}"
        // ) {
        //   val bp = hp.onlyEnterHand(
        //             frh.getForCheckFirstRound,
        //             allHands.getBoardFromBoardSet(frh.hob.board).get,
        //             getPlayer(frh.hob.hand.north),
        //             getPlayer(frh.hob.hand.south),
        //             getPlayer(frh.hob.hand.east),
        //             getPlayer(frh.hob.hand.west)
        //           ).validate
        //   bp.checkOthers(
        //     frh.getForCheckFirstRound,
        //     allHands
        //   )
        // }
      },
    )
  }

  def enterHand(hp: HandPage, hob: HandsOnBoard)(implicit webDriver: WebDriver) = {

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
          bp.checkOthers(
            hob,
            allHands
          )
          bp
        }
      case None =>
        fail(s"Unable to find board ${hob.board}")
    }

  }

  def enterHandFromBoard(hob: HandsOnBoard)(implicit webDriver: WebDriver) = {
    val boardp = BoardPage.current.validate
    val hp = boardp.clickBoardButton(hob.board).validate
    enterHand(hp, hob)
  }
}
