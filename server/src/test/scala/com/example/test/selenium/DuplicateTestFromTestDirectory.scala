package com.example.test.selenium

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import org.openqa.selenium.WebDriver
import org.scalatest.selenium.Firefox
import org.scalatest.BeforeAndAfterAll
import org.scalatest._
import selenium._
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
import com.example.data.MatchDuplicate
import com.example.backend.BridgeServiceFileStore
import com.example.backend.resource.FileIO
import scala.reflect.io.Directory
import java.io.File
import scala.language.postfixOps
import com.example.data.DuplicateHand
import com.example.data.Team
import com.example.data.Id
import java.net.URL
import java.io.InputStreamReader
import scala.io.Codec
import scala.io.Source
import com.example.data.BoardSet
import com.example.test.util.HttpUtils
import com.example.test.TestStartLogging
import com.example.data.MatchPlayerPosition
import org.scalatest.exceptions.TestFailedException
import com.example.source.SourcePosition
import com.example.pages.GenericPage
import com.example.pages.Element
import com.example.test.util.MonitorTCP
import com.example.test.util.HttpUtils.ResponseFromHttp
import com.example.backend.BridgeServiceFileStoreConverters
import com.example.backend.MatchDuplicateCacheStoreSupport
import com.example.test.util.ParallelUtils.CodeBlock
import com.example.pages.PageBrowser
import com.example.pages.bridge.HomePage
import com.example.pages.duplicate.ListDuplicatePage
import com.example.pages.duplicate.ScoreboardPage
import com.example.pages.duplicate.PageWithBoardButtons
import com.example.pages.duplicate.TablePage.EnterOrSelectNames
import com.example.pages.duplicate.TablePage
import com.example.pages.duplicate.TableEnterOrSelectNamesPage
import com.example.data.DuplicateHandV2
import com.example.pages.duplicate.BoardPage

/**
 * Test playing duplicate matches.  The duplicates matches to play are in the testdata directory.
 * @author werewolf
 */
class DuplicateTestFromTestDirectory extends FlatSpec with DuplicateUtils with MustMatchers with BeforeAndAfterAll with EventuallyUtils {
  import Eventually.{ patienceConfig => _, _ }
  import com.example.pages.PageBrowser._
  import ParallelUtils._

  val testlog = Logger[DuplicateTestFromTestDirectory]

  val screenshotDir = "target/DuplicateTestFromTestDirectory"

  import scala.concurrent.duration._

  val timeoutMillis = 20000
  val intervalMillis = 1000

  implicit val timeoutduration = Duration( 60, TimeUnit.SECONDS )

  val defaultPatienceConfig = PatienceConfig(timeout=scaled(Span(timeoutMillis, Millis)), interval=scaled(Span(intervalMillis,Millis)))
  implicit def patienceConfig = defaultPatienceConfig

  def getAllGames() = TestData.getAllGames()

  override
  def beforeAll() = {

    MonitorTCP.nextTest()
    try {
      TestServer.start()
    } catch {
      case e: Throwable =>
        afterAll()
        throw e
    }
  }

  override
  def afterAll() = {
    try {
      TestServer.stop()
    } catch {
      case e: Throwable =>
        throw e
    }
  }

  def showDOM()( implicit webDriver: WebDriver ): Unit = {
    val doc = executeScript("document.documentElement.outerHTML");
    testlog.warning("DOM:" + doc)
  }

  def genericPage( implicit webDriver: WebDriver, createdPos: SourcePosition ) = {
    GenericPage.current
  }

  behavior of "Play a duplicate match"

  it should "start logging" in {
    TestStartLogging.startLogging()
    tcpSleep(15)
  }

  import org.scalatest.prop.TableDrivenPropertyChecks._
  val templates = Table( "MatchDuplicate", getAllGames(): _* )

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
    it should s"play a complete match ${md.id}" in {
      firstScorekeeper = firstScorekeeper.nextDealer
      testlog.info("Starting on MatchDuplicate "+md.id+", first scorekeeper "+firstScorekeeper.name)
      val mp = new MatchPlay(md)
      try {
        mp.setup()
        mp.play()
      } finally {
        mp.tearDown()
      }
    }
  }

  def logException[T]( label: String )( t: =>T ): T =
    {
      try {
        t
      } catch {
        case ex: Exception =>
          testlog.severe( label, ex )
          throw ex
      }
    }


  def logFuture[T]( t: =>T ) = CodeBlock { logException("logFuture")( t ) }

  var firstScorekeeper: PlayerPosition = North

  class MatchPlay( template: MatchDuplicate) {
    val numbertables = template.getTableIds().size

    val templateScore = MatchDuplicateScore(template,PerspectiveDirector)

    val sessionDirector = new DirectorSession()
    val sessionComplete = new CompleteSession()
    val sessionTables = template.getTableIds().map { id => {
      val sid = id.toString
      new TableSession(sid) }
    }

    var dupid: Option[String] = None

    def setup(): Unit = {
      try {
        import Session._
        waitForFutures( "setup",
                        logFuture { sessionDirector.sessionStart(getPropOrEnv("SessionDirector")).setQuadrant(1) },
                        logFuture { TestServer.start() }
                        )
      } catch {
        case e: Throwable =>
          tearDown()
          throw e
      }
    }

    def play() = {
      gotoRootPage()
      gotoSummaryPage()
      newDuplicate()
      gotoDirectorsPage()
      startCompleteAndTables()

      playAllRounds()

      compareResultLocal()
      compareResultRest()
    }

    def tearDown() = {
      try {
        waitForFutures( "tearDown",
                        logFuture { sessionComplete.sessionStop() }::
                        logFuture { sessionDirector.sessionStop() }::
                        logFuture { TestServer.stop() }::
                        sessionTables.map { ts => logFuture { ts.sessionStop() } }.toList : _*
                        )
      } catch {
        case e: Throwable =>
          testlog.warning("Error stopping browsers", e)
          // ignore errors
          // throw e
      }
    }

    def compareResultLocal(): Unit = {

      TestServer.onlyRunWhenStartingServer(Some("Skipping comparing to database, no access to database")) { () =>
        TestServer.backend.duplicates.syncStore.read(dupid.get) match {
          case Right(played) =>
            val n = played.copy(id=template.id)
            try {
              n.equalsIgnoreModifyTime(template,true) mustBe true
              testlog.warning("Backend compare for MatchDuplicate "+dupid.get+" is ok")
            } catch {
              case e: Exception =>
                testlog.severe(s"""The two MatchDuplicates for ${template.id} don't compare.\n  1 - Played\n  2 - Template""", e)
                implicit val instanceJson = new BridgeServiceFileStoreConverters(true).matchDuplicateJson
                val conv = new MatchDuplicateCacheStoreSupport(false)
                FileIO.writeFileSafe("MatchDuplicate.PlayedJson"+conv.getWriteExtension(), conv.toJSON(played))
                FileIO.writeFileSafe("MatchDuplicate.Template"+conv.getWriteExtension(), conv.toJSON(template))
                throw e
            }
          case Left((statuscode,restMessage)) =>
            fail("Did not find newly created MatchDuplicate in store, id="+dupid.get+": "+statuscode+" "+restMessage )
        }
      }
    }

    def compareResultRest(): Unit = {

      val url: URL = new URL(TestServer.hosturl+"v1/rest/duplicates/"+dupid.get)
      testlog.fine("Rest URL for MatchDuplicate "+dupid.get+" is "+url)
      val connection = url.openConnection()
      val is = connection.getInputStream
      try {
        val json = Source.fromInputStream(is)(Codec.UTF8).mkString
        import com.example.rest.UtilsPlayJson._

        val played = readJson[MatchDuplicate](json)
        val n = played.copy(id=template.id)
        try {
          n.equalsIgnoreModifyTime(template,true) mustBe true
          testlog.warning("Rest compare for MatchDuplicate "+dupid.get+" is ok")
        } catch {
          case e: Exception =>
            testlog.severe(s"""The two MatchDuplicates for ${template.id} don't compare.\n  1 - Played\n  2 - Template""", e)
            implicit val instanceJson = new BridgeServiceFileStoreConverters(true).matchDuplicateJson
            val conv = new MatchDuplicateCacheStoreSupport(false)
            FileIO.writeFileSafe("MatchDuplicate.PlayedJson"+conv.getWriteExtension(), conv.toJSON(played))
            FileIO.writeFileSafe("MatchDuplicate.Template"+conv.getWriteExtension(), conv.toJSON(template))
            throw e
        }
      } finally {
        is.close()
      }
    }

    def gotoRootPage() = {
      import Session._
      val envSessionTable = getPropOrEnv("SessionTable")
      val sessions = sessionTables.toList
      val firstSession = sessions.head
      val restSession = sessions.tail

      waitForFutures(
        "gotoRootPage",
        logFuture { val x = firstSession.sessionStart(envSessionTable).setQuadrant(3) }::
        logFuture { val x = sessionComplete.sessionStart(getPropOrEnv("SessionComplete")).setQuadrant(2) }::
        logFuture {
          import sessionDirector._
          val hp = HomePage.goto.validate
        }::
        restSession.map { ts => logFuture { val x = ts.sessionStart(envSessionTable).setQuadrant(4) } }.toList : _*
      )
    }

    def gotoSummaryPage() = {
      import sessionDirector._

      val hp = HomePage.current

      val sum = hp.clickListDuplicateButton.validate

      val dc = hp.getButton("DuplicateCreate",Some("New")) mustBe 'Enabled
    }

    def newDuplicate() = {
      import sessionDirector._

      val ldp = ListDuplicatePage.current

      val newd = ldp.clickNewDuplicateButton.validate

      val boardset = template.boardset
      val movement = template.movement

      newd.getNewButton(boardset, movement) mustBe 'Enabled

      tcpSleep(2)

      val dup = newd.click(boardset, movement).validate

      dupid = dup.dupid

      assert( dupid.isDefined && dupid.get.length()>0)

    }

    def gotoDirectorsPage() = {
      import sessionDirector._

      val sbp = ScoreboardPage.current

      sbp.validate( (1 to 18).toList )

      val dsbp = sbp.clickDirectorButton

    }

    def startCompleteAndTables() = {
    waitForFutures(
        "startCompleteAndTables",
        logFuture {
          import sessionComplete._
          assert( dupid.isDefined && dupid.get.length()>0)
          ScoreboardPage.goto(dupid.get).validate.checkTableButton("1")
        } ::
        sessionTables.map { st => {
          logFuture {
            import st._
            assert( dupid.isDefined && dupid.get.length()>0)
            val sbp = ScoreboardPage.goto(dupid.get).validate
            sbp.checkTableButton(st.number)
            sbp.clickTableButton(number.toInt).validate
          }
        } }.toList : _*
      )
    }

    def getPlayers( hand: DuplicateHand ): (String,String,String,String) = {
      val MatchPlayerPosition(north,south,east,west,allspecified,gotns,gotew) = template.determinePlayerPosition(hand)
      (north,south,east,west)
    }

    /**
     * @param id the board id, starts with "Board_B"
     */
    def boardIdToNumber( id: String ) = {
      if (id.startsWith("B")) {
        id.drop(1).toInt
      }
      else 0
    }

    /**
     * Assumes on a by Scoreboard or BoardPage page with a Board_n buttons for the round being played.
     */
    def doPlayHand( sessionTable: TableSession, hand: DuplicateHand, page: PageWithBoardButtons ) = try {
      import sessionTable._
      val boardButton = "Board_"+hand.board
      val (north,south,east,west) = getPlayers(hand)

      hand.hand match {
        case Some(h) =>
          val bh = BridgeHand(h)
          val bid = boardIdToNumber(hand.board)
          val board = boardSet.get.boards.find( b => b.id == bid).get
          val nsvul = board.nsVul
          val ewvul = board.ewVul
          val nsteam = Id.teamIdToTeamNumber(hand.nsTeam)
          val ewteam = Id.teamIdToTeamNumber(hand.ewTeam)

          val hp = page.clickBoardButton(board.id)

          val r = hp.onlyEnterHand( bh.contractTricks,
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

    } catch {
      case ex: Exception =>
        testlog.severe("doPlayHand template="+template.id+", table "+sessionTable.number+", hand "+hand,ex)
        throw ex
    }

    /**
     * Assumes on a Table page with a Round_n button for the round being played.
     */
    def playRound( round: Int, sessionTable: TableSession ): Unit = try {
      import sessionTable._

      PageBrowser.withClueAndScreenShot(screenshotDir, s"Round${round}Table${sessionTable.table}", s"Round ${round} Table ${sessionTable.table}") {

        val tp = TablePage.current(EnterOrSelectNames)

        templateScore.tables.get(sessionTable.table) match {
          case Some(rounds) =>
            rounds.find { r => r.round == round } match {
              case Some(r) =>
                val hands = r.boards.map { bs => bs.board.hands.find { h => h.table==sessionTable.table && h.round==round } }.
                                     map { x => x match {
                                       case Some(h) => h
                                       case None =>
                                         fail("Did not find all hands for table "+sessionTable.table+" round "+round)
                                     }}
                tp.setInputStyle("Yellow")
                val eos = tp.clickRound(round).asInstanceOf[TableEnterOrSelectNamesPage]

                val (north,south,east,west) = getPlayers(hands.head)
                var scorekeeper = firstScorekeeper
                (1 to (sessionTable.number.toInt+round)).foreach { i => scorekeeper = scorekeeper.nextDealer }
                val page: PageWithBoardButtons = eos.playEnterNames(north, south, east, west, scorekeeper).asInstanceOf[ScoreboardPage]

                def folder(pg: PageWithBoardButtons,hand: DuplicateHandV2): PageWithBoardButtons = {
                  val np = doPlayHand(sessionTable,hand,pg)
                  np
                }

                val bp = hands.foldLeft(page)(folder).asInstanceOf[BoardPage]
                bp.clickTableButton(table.toInt).validate
              case None =>
                fail("Did not find round "+round+" for table "+sessionTable.table)
            }
          case None =>
            fail("Did not find rounds for table "+sessionTable.table)
        }
      }
    } catch {
      case ex: Exception =>
        testlog.severe("PlayRound template="+template.id+", table "+sessionTable.number+", round "+round,ex)
        throw ex
    }

    /**
     * Assumes on a Table page with a Round_n button for the round being played.
     */
    def playRound( round: Int ): Unit = {
      tcpSleep(30*numbertables)
      val roundtimeoutduration = Duration( 180, TimeUnit.SECONDS )
      waitForFutures(
        "playRound",
        sessionTables.map { st => logFuture {playRound(round,st)}}.toList : _*
      )(roundtimeoutduration)
    }

    var boardSet: Option[BoardSet] = None

    def playAllRounds(): Unit = {
      val boardsetName = templateScore.getBoardSet()

      import com.example.rest.UtilsPlayJson._
      val ResponseFromHttp(status,loc,ce,bs,cd) = HttpUtils.getHttpObject[BoardSet](TestServer.getUrl("/v1/rest/boardsets/"+boardsetName))
      boardSet = bs

      val rounds = templateScore.tables.values.toList.head.length
      for (r <- 1 to rounds) playRound(r)
    }
  }


}
