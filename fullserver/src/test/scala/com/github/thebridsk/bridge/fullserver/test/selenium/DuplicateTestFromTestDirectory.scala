package com.github.thebridsk.bridge.fullserver.test.selenium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.openqa.selenium.WebDriver
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import java.util.concurrent.TimeUnit
import org.scalatest.time.Span
import org.scalatest.time.Millis
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.utilities.logging.Logger
import org.scalactic.source.Position
import com.github.thebridsk.bridge.server.test.util.EventuallyUtils
import com.github.thebridsk.bridge.server.test.util.ParallelUtils
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.utilities.file.FileIO
import com.github.thebridsk.bridge.data.DuplicateHand
import java.net.URL
import scala.io.Codec
import scala.io.Source
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.server.test.util.HttpUtils
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.bridge.data.MatchPlayerPosition
import com.github.thebridsk.source.SourcePosition
import com.github.thebridsk.browserpages.GenericPage
import com.github.thebridsk.bridge.server.test.util.MonitorTCP
import com.github.thebridsk.bridge.server.test.util.HttpUtils.ResponseFromHttp
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStoreConverters
import com.github.thebridsk.bridge.server.backend.MatchDuplicateCacheStoreSupport
import com.github.thebridsk.browserpages.PageBrowser
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ListDuplicatePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ScoreboardPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.PageWithBoardButtons
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage.EnterOrSelectNames
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TableEnterOrSelectNamesPage
import com.github.thebridsk.bridge.data.DuplicateHandV2
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.BoardPage
import com.github.thebridsk.browserpages.Session
import com.github.thebridsk.bridge.server.test.util.TestServer
import org.scalatest.Assertion
import org.scalatest.prop.TableFor1

/**
  * Test playing duplicate matches.  The duplicates matches to play are in the testdata directory.
  * @author werewolf
  */
class DuplicateTestFromTestDirectory
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll
    with EventuallyUtils {
  import Eventually.{patienceConfig => _, _}
  import com.github.thebridsk.browserpages.PageBrowser._
  import ParallelUtils._

  val testlog: Logger = Logger[DuplicateTestFromTestDirectory]()

  TestStartLogging.startLogging()

  val screenshotDir = "target/DuplicateTestFromTestDirectory"

  import scala.concurrent.duration._

  val timeoutMillis = 20000
  val intervalMillis = 1000

  implicit val timeoutduration: FiniteDuration = Duration(60, TimeUnit.SECONDS)

  implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(timeoutMillis, Millis)),
    interval = scaled(Span(intervalMillis, Millis))
  )

  def getAllGames(): List[MatchDuplicate] = TestData.getAllGames()

  override def beforeAll(): Unit = {

    MonitorTCP.nextTest()
    try {
      TestServer.start()
    } catch {
      case e: Throwable =>
        afterAll()
        throw e
    }
  }

  override def afterAll(): Unit = {
    try {
      TestServer.stop()
    } catch {
      case e: Throwable =>
        throw e
    }
  }

  def showDOM()(implicit webDriver: WebDriver): Unit = {
    val doc = executeScript("document.documentElement.outerHTML");
    testlog.warning("DOM:" + doc)
  }

  def genericPage(implicit
      webDriver: WebDriver,
      createdPos: SourcePosition
  ): GenericPage = {
    GenericPage.current
  }

  behavior of "Play a duplicate match"

  import org.scalatest.prop.TableDrivenPropertyChecks._
  val templates: TableFor1[MatchDuplicate] =
    Table("MatchDuplicate", getAllGames(): _*)

//  it should "play a complete match" in {
//    forAll(templates) { md =>
//      firstScorekeeper = firstScorekeeper.nextDealer
//      testlog.warning("Starting on MatchDuplicate "+md.id+", first scorekeeper "+firstScorekeeper.name)
//      val mp = new MatchPlay(md)
//      try {
//        mp.setup()
//        mp.play()
//      } finally {
//        mp.tearDown()
//      }
//    }
//  }

  forAll(templates) { md =>
    it should s"play a complete match ${md.id.id}" in {
      firstScorekeeper = firstScorekeeper.nextDealer
      testlog.info(
        "Starting on MatchDuplicate " + md.id + ", first scorekeeper " + firstScorekeeper.name
      )
      val mp = new MatchPlay(md)
      try {
        mp.setup()
        mp.play()
      } finally {
        mp.tearDown()
      }
    }
  }

  def logException[T](label: String)(t: => T): T = {
    try {
      t
    } catch {
      case ex: Exception =>
        testlog.severe(label, ex)
        throw ex
    }
  }

  def logFuture[T](t: => T)(implicit pos: Position): CodeBlock[T] =
    CodeBlock { logException("logFuture")(t) }

  var firstScorekeeper: PlayerPosition = North

  class MatchPlay(template: MatchDuplicate) {
    val numbertables: Int = template.getTableIds().size

    val templateScore: MatchDuplicateScore =
      MatchDuplicateScore(template, PerspectiveDirector)

    val sessionDirector = new DirectorSession()
    val sessionComplete = new CompleteSession()
    val sessionTables: List[TableSession] = template.getTableIds().map { id =>
      {
        val sid = id.toNumber
        new TableSession(sid)
      }
    }

    var dupid: Option[String] = None

    def setup(): Unit = {
      try {
        import Session._
        waitForFutures(
          "setup",
          logFuture {
            sessionDirector
              .sessionStart(getPropOrEnv("SessionDirector"))
              .setQuadrant(1, 1024, 768)
          },
          logFuture { TestServer.start() }
        )
      } catch {
        case e: Throwable =>
          tearDown()
          throw e
      }
    }

    def play(): Unit = {
      gotoRootPage()
      gotoSummaryPage()
      newDuplicate()
      gotoDirectorsPage()
      startCompleteAndTables()

      playAllRounds()

      compareResultLocal()
      compareResultRest()
    }

    def tearDown(): Unit = {
      try {
        waitForFutures(
          "tearDown",
          logFuture { sessionComplete.sessionStop() } ::
            logFuture { sessionDirector.sessionStop() } ::
            logFuture { TestServer.stop() } ::
            sessionTables.map { ts =>
              logFuture { ts.sessionStop() }
            }.toList: _*
        )
      } catch {
        case e: Throwable =>
          testlog.warning("Error stopping browsers", e)
        // ignore errors
        // throw e
      }
    }

    def compareResultLocal(): Unit = {

      TestServer.onlyRunWhenStartingServer(
        Some("Skipping comparing to database, no access to database")
      ) { () =>
        eventually {
          TestServer.backend.duplicates.syncStore
            .read(MatchDuplicate.id(dupid.get)) match {
            case Right(played) =>
              val n = played.copy(id = template.id)
              try {
                n.equalsIgnoreModifyTime(template, true) mustBe true
                testlog.warning(
                  "Backend compare for MatchDuplicate " + dupid.get + " is ok"
                )
              } catch {
                case e: Exception =>
                  testlog.severe(
                    s"""The two MatchDuplicates for ${template.id} don't compare.\n  1 - Played\n  2 - Template""",
                    e
                  )
                  implicit val instanceJson =
                    new BridgeServiceFileStoreConverters(
                      true
                    ).matchDuplicateJson
                  val conv = new MatchDuplicateCacheStoreSupport(false)
                  FileIO.writeFileSafe(
                    "MatchDuplicate.PlayedJson" + conv.getWriteExtension,
                    conv.toJSON(played)
                  )
                  FileIO.writeFileSafe(
                    "MatchDuplicate.Template" + conv.getWriteExtension,
                    conv.toJSON(template)
                  )
                  throw e
              }
            case Left((statuscode, restMessage)) =>
              fail(
                "Did not find newly created MatchDuplicate in store, id=" + dupid.get + ": " + statuscode + " " + restMessage
              )
          }
        }
      }
    }

    def compareResultRest(): Unit = {
      eventually {
        val url: URL =
          new URL(TestServer.hosturl + "v1/rest/duplicates/" + dupid.get)
        testlog.fine("Rest URL for MatchDuplicate " + dupid.get + " is " + url)
        val connection = url.openConnection()
        val is = connection.getInputStream
        try {
          val json = Source.fromInputStream(is)(Codec.UTF8).mkString
          import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._

          val played = readJson[MatchDuplicate](json)
          val n = played.copy(id = template.id)
          try {
            n.equalsIgnoreModifyTime(template, true) mustBe true
            testlog.warning(
              "Rest compare for MatchDuplicate " + dupid.get + " is ok"
            )
          } catch {
            case e: Exception =>
              testlog.severe(
                s"""The two MatchDuplicates for ${template.id} don't compare.\n  1 - Played\n  2 - Template""",
                e
              )
              implicit val instanceJson =
                new BridgeServiceFileStoreConverters(true).matchDuplicateJson
              val conv = new MatchDuplicateCacheStoreSupport(false)
              FileIO.writeFileSafe(
                "MatchDuplicate.PlayedJson" + conv.getWriteExtension,
                conv.toJSON(played)
              )
              FileIO.writeFileSafe(
                "MatchDuplicate.Template" + conv.getWriteExtension,
                conv.toJSON(template)
              )
              throw e
          }
        } finally {
          is.close()
        }
      }
    }

    def gotoRootPage(): Unit = {
      import Session._
      val envSessionTable = getPropOrEnv("SessionTable")
      val sessions = sessionTables.toList
      val firstSession = sessions.head
      val restSession = sessions.tail

      waitForFutures(
        "gotoRootPage",
        logFuture {
          val x =
            firstSession.sessionStart(envSessionTable).setQuadrant(3, 1024, 768)
        } ::
          logFuture {
            val x = sessionComplete
              .sessionStart(getPropOrEnv("SessionComplete"))
              .setQuadrant(2, 1024, 768)
          } ::
          logFuture {
            import sessionDirector._
            val hp = HomePage.goto.validate
          } ::
          restSession.map { ts =>
            logFuture {
              val x = ts.sessionStart(envSessionTable).setQuadrant(4, 1024, 768)
            }
          }.toList: _*
      )
    }

    def gotoSummaryPage(): Unit = {
      import sessionDirector._

      PageBrowser.withClueAndScreenShot(
        screenshotDir,
        s"${template.id}SummaryPage",
        s"Error going to summary page to play ${template.id}"
      ) {

        val hp = HomePage.current

        val sum = hp.clickListDuplicateButton.validate

        val dc =
          hp.getButton("DuplicateCreate", Some("New")) mustBe Symbol("Enabled")
      }
    }

    def newDuplicate(): Assertion = {
      import sessionDirector._

      PageBrowser.withClueAndScreenShot(
        screenshotDir,
        s"${template.id}NewDuplicate",
        s"Error creating new duplicate to play ${template.id}"
      ) {

        val ldp = ListDuplicatePage.current

        val newd = ldp.clickNewDuplicateButton.validate

        val boardset = template.boardset
        val movement = template.movement

        newd.getNewButton(boardset.id, movement.id) mustBe Symbol("Enabled")

        tcpSleep(2)

        val dup = newd.click(boardset.id, movement.id).validate

        dupid = dup.dupid

        assert(dupid.isDefined && dupid.get.length() > 0)
      }
    }

    def gotoDirectorsPage(): Unit = {
      import sessionDirector._

      val sbp = ScoreboardPage.current

      sbp.validate((1 to 18).toList)

      val dsbp = sbp.clickMainMenu.validateMainMenu.clickDirectorButton

    }

    def startCompleteAndTables(): Unit = {
      waitForFutures(
        "startCompleteAndTables",
        logFuture {
          import sessionComplete._
          assert(dupid.isDefined && dupid.get.length() > 0)
          ScoreboardPage.goto(dupid.get).validate.checkTableButton("1")
        } ::
          sessionTables.map { st =>
            {
              logFuture {
                import st._
                assert(dupid.isDefined && dupid.get.length() > 0)
                val sbp = ScoreboardPage.goto(dupid.get).validate
                sbp.checkTableButton(st.number)
                sbp.clickTableButton(number.toInt).validate
              }
            }
          }.toList: _*
      )
    }

    def getPlayers(hand: DuplicateHand): (String, String, String, String) = {
      val MatchPlayerPosition(
        north,
        south,
        east,
        west,
        allspecified,
        gotns,
        gotew
      ) = template.determinePlayerPosition(hand)
      (north, south, east, west)
    }

    /**
      * Assumes on a by Scoreboard or BoardPage page with a Board_n buttons for the round being played.
      */
    def doPlayHand(
        sessionTable: TableSession,
        hand: DuplicateHand,
        page: PageWithBoardButtons
    ): PageWithBoardButtons =
      try {
        val boardButton = s"Board_${hand.board.id}"
        val (north, south, east, west) = getPlayers(hand)

        withClue(s"Going to board ${hand.board}") {
          hand.hand match {
            case Some(h) =>
              val bh = BridgeHand(h)
              val bid = hand.board.toInt
              val board = boardSet.get.boards.find(b => b.id == bid).get
              val nsvul = board.nsVul
              val ewvul = board.ewVul
              val nsteam = hand.nsTeam.toNumber
              val ewteam = hand.ewTeam.toNumber

              val hp = page.clickBoardButton(board.id).validate

              val r = hp.onlyEnterHand(
                bh.contractTricks,
                bh.contractSuit,
                bh.contractDoubled,
                bh.declarer,
                bh.madeOrDown,
                bh.tricks,
                false
              )

              r
            case None =>
              page
          }
        }

      } catch {
        case ex: Exception =>
          testlog.severe(
            "doPlayHand template=" + template.id + ", table " + sessionTable.number + ", hand " + hand,
            ex
          )
          throw ex
      }

    /**
      * Assumes on a Table page with a Round_n button for the round being played.
      */
    def playRound(round: Int, sessionTable: TableSession): Unit =
      try {
        import sessionTable._

        PageBrowser.withClueAndScreenShot(
          screenshotDir,
          s"${template.id}Round${round}Table${sessionTable.table}",
          s"Round ${round} Table ${sessionTable.table}"
        ) {

          val tp = TablePage.current(EnterOrSelectNames)

          templateScore.tables.get(sessionTable.table) match {
            case Some(rounds) =>
              rounds.find { r => r.round == round } match {
                case Some(r) =>
                  val hands = r.boards
                    .map { bs =>
                      bs.board.hands.find { h =>
                        h.table == sessionTable.table && h.round == round
                      }
                    }
                    .map { x =>
                      x match {
                        case Some(h) => h
                        case None =>
                          fail(
                            "Did not find all hands for table " + sessionTable.table + " round " + round
                          )
                      }
                    }
                  tp.setInputStyle("Guide")
                  val eos = tp
                    .clickRound(round)
                    .asInstanceOf[TableEnterOrSelectNamesPage]

                  val (north, south, east, west) = getPlayers(hands.head)
                  var scorekeeper = firstScorekeeper
                  (1 to (sessionTable.number.toInt + round)).foreach { i =>
                    scorekeeper = scorekeeper.nextDealer
                  }
                  val page: PageWithBoardButtons = eos
                    .playEnterNames(north, south, east, west, scorekeeper)
                    .validate
                    .asInstanceOf[ScoreboardPage]

                  def folder(
                      pg: PageWithBoardButtons,
                      hand: DuplicateHandV2
                  ): PageWithBoardButtons = {
                    val np = doPlayHand(sessionTable, hand, pg)
                    np
                  }

                  val bp = hands.foldLeft(page)(folder).asInstanceOf[BoardPage]
                  bp.clickTableButton(table.toInt).validate
                case None =>
                  fail(
                    "Did not find round " + round + " for table " + sessionTable.table
                  )
              }
            case None =>
              fail("Did not find rounds for table " + sessionTable.table)
          }
        }
      } catch {
        case ex: Exception =>
          testlog.severe(
            "PlayRound template=" + template.id + ", table " + sessionTable.number + ", round " + round,
            ex
          )
          throw ex
      }

    /**
      * Assumes on a Table page with a Round_n button for the round being played.
      */
    def playRound(round: Int): Unit = {
      tcpSleep(30 * numbertables)
      val roundtimeoutduration = Duration(180, TimeUnit.SECONDS)
      waitForFutures(
        "playRound",
        sessionTables.map { st =>
          logFuture { playRound(round, st) }
        }.toList: _*
      )(roundtimeoutduration)
    }

    var boardSet: Option[BoardSet] = None

    def playAllRounds(): Unit = {
      val boardsetName = templateScore.getBoardSet

      import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._
      val ResponseFromHttp(status, loc, ce, bs, cd) =
        HttpUtils.getHttpObject[BoardSet](
          TestServer.getUrl("/v1/rest/boardsets/" + boardsetName.id)
        )
      boardSet = bs

      val rounds = templateScore.tables.values.toList.head.length
      for (r <- 1 to rounds) playRound(r)
    }
  }

}
