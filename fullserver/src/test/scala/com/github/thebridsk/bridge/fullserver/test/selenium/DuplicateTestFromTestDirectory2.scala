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
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStore
import com.github.thebridsk.utilities.file.FileIO
import scala.reflect.io.Directory
import java.io.File
import scala.language.postfixOps
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.Id
import java.net.URL
import java.io.InputStreamReader
import scala.io.Codec
import scala.io.Source
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.server.test.util.HttpUtils
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.bridge.data.MatchPlayerPosition
import org.scalatest.exceptions.TestFailedException
import com.github.thebridsk.source.SourcePosition
import com.github.thebridsk.browserpages.GenericPage
import com.github.thebridsk.browserpages.Element
import com.github.thebridsk.bridge.server.test.util.MonitorTCP
import com.github.thebridsk.bridge.server.test.util.HttpUtils.ResponseFromHttp
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStoreConverters
import com.github.thebridsk.bridge.server.backend.MatchDuplicateCacheStoreSupport
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ListDuplicatePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ScoreboardPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage.EnterOrSelectNames
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TableEnterOrSelectNamesPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.BoardPage
import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.bridge.data.DuplicateHandV2
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.PageWithBoardButtons
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.PageWithBoardButtons
import com.github.thebridsk.browserpages.Session
import org.scalatest.CancelAfterFailure
import com.github.thebridsk.bridge.server.test.util.TestServer

/**
 * Test playing duplicate matches.  The duplicates matches to play are in the testdata directory.
 * @author werewolf
 */
class DuplicateTestFromTestDirectory2 extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll
    with EventuallyUtils
    with CancelAfterFailure
{
  import Eventually.{ patienceConfig => _, _ }
  import com.github.thebridsk.browserpages.PageBrowser._
  import ParallelUtils._

  val testlog = Logger[DuplicateTestFromTestDirectory2]()

  val screenshotDir = "target/DuplicateTestFromTestDirectory2"

  import scala.concurrent.duration._

  val timeoutMillis = 20000
  val intervalMillis = 750

  implicit val timeoutduration = Duration( 60, TimeUnit.SECONDS )

  implicit def patienceConfig = PatienceConfig(timeout=scaled(Span(timeoutMillis, Millis)), interval=scaled(Span(intervalMillis,Millis)))

  val sessionDirector = new DirectorSession()
  val sessionComplete = new CompleteSession()
  val globalSessionTables = scala.collection.mutable.Map[Id.Table,TableSession]()

  def getAllGames() = TestData.getAllGames()

  override
  def beforeAll() = {

    MonitorTCP.nextTest()
    try {
      import Session._
      waitForFutures(
          "beforeAll",
          logFuture { TestServer.start() }
      )
    } catch {
      case e: Throwable =>
        afterAll()
        throw e
    }
  }

  override
  def afterAll() = {
    val sessions =
          logMyFuture { sessionComplete.sessionStop() }::
          logMyFuture { sessionDirector.sessionStop() }::
          logMyFuture { TestServer.stop() }::
          globalSessionTables.values.map { ts => logMyFuture { ts.sessionStop() } }.toList
    waitForFuturesIgnoreTimeouts( "Stopping browsers and server", sessions: _* )
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
//      mp.play()
//    }
//  }

  forAll(templates) { md =>
    it should s"play a complete match for ${md.id}" in {
      firstScorekeeper = firstScorekeeper.nextDealer
      testlog.warning("Starting on MatchDuplicate "+md.id+", first scorekeeper "+firstScorekeeper.name)
      val mp = new MatchPlay(md)
      mp.play()
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

  def logMyFuture[T]( t: =>T ) = CodeBlock { logException("logFuture")( t ) }

  var firstScorekeeper: PlayerPosition = North

  class MatchPlay( template: MatchDuplicate) {
    val numbertables = template.getTableIds().size

    val templateScore = MatchDuplicateScore(template,PerspectiveDirector)

    var dupid: Option[String] = None

    var newTableSessions = List[String]()
    val sessionTables = scala.collection.mutable.Map[Id.Table,TableSession]()

    val sessionCompleteRunning = sessionComplete.isSessionRunning
    val sessionDirectorRunning = sessionDirector.isSessionRunning

    template.getTableIds().foreach { id =>
      globalSessionTables.get(id) match {
        case Some(st) =>
          sessionTables += (id -> st)
        case None =>
          val sid = id.toString
          newTableSessions = sid::newTableSessions
          val st = new TableSession(sid)
          sessionTables += (id -> st)
          globalSessionTables += (id -> st)
      }
    }

    def startup() = {
      import sessionDirector._
      withClueAndScreenShot(screenshotDir, "play", s"In startup for template match ${template.id}") {
        gotoSummaryPage()
        newDuplicate()
        gotoDirectorsPage()
      }
    }

    def play() = {
      gotoRootPage()
      startup()
      startCompleteAndTables()

      playAllRounds()

      compareResultLocal()
      compareResultRest()
      atEndGotoSummaryPage()
    }

    def compareResultLocal(): Unit = {

      TestServer.onlyRunWhenStartingServer(Some("Skipping comparing to database, no access to database")) { () =>
        eventually {
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
                  FileIO.writeFileSafe("MatchDuplicate.PlayedJson"+conv.getWriteExtension, conv.toJSON(played))
                  FileIO.writeFileSafe("MatchDuplicate.Template"+conv.getWriteExtension, conv.toJSON(template))
                  throw e
              }
            case Left((statuscode,restMessage)) =>
              fail("Did not find newly created MatchDuplicate in store, id="+dupid.get+": "+statuscode+" "+restMessage )
          }
        }
      }
    }

    def compareResultRest(): Unit = {
      eventually {
        val url: URL = new URL(TestServer.hosturl+"v1/rest/duplicates/"+dupid.get)
        testlog.fine("Rest URL for MatchDuplicate "+dupid.get+" is "+url)
        val connection = url.openConnection()
        val is = connection.getInputStream
        try {
          val json = Source.fromInputStream(is)(Codec.UTF8).mkString
          import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._
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
              FileIO.writeFileSafe("MatchDuplicate.PlayedJson"+conv.getWriteExtension, conv.toJSON(played))
              FileIO.writeFileSafe("MatchDuplicate.Template"+conv.getWriteExtension, conv.toJSON(template))
              throw e
          }
        } finally {
          is.close()
        }
      }
    }

    def gotoRootPage() = {
      import Session._
      val envSessionTable = getPropOrEnv("SessionTable")
      val sessions = sessionTables.values.toList.sortWith((l,r)=> l.number < r.number)
      val firstSession = sessions.head
      val restSession = sessions.tail

      waitForFutures(
        "gotoRootPage",
        logFuture {
          sessionDirector.sessionStartIfNotRunning(getPropOrEnv("SessionDirector")).setQuadrant(1,1024,768)
          import sessionDirector._
          val hp = if (!sessionDirectorRunning) {
            HomePage.goto
          } else {
            HomePage.current
          }
          hp.validate
        }::
        logFuture { sessionComplete.sessionStartIfNotRunning(getPropOrEnv("SessionComplete")).setQuadrant(2,1024,768) }::
        logFuture { firstSession.sessionStartIfNotRunning(envSessionTable).setQuadrant(3,1024,768) }::
        restSession.map { ts => logFuture { val x = ts.sessionStartIfNotRunning(envSessionTable).setQuadrant(4,1024,768) } }.toList : _*
      )
    }

    def gotoSummaryPage() = {
      import sessionDirector._

      withClueAndScreenShot(
          screenshotDir,
          s"${template.id}SummaryPage",
          s"Error going to summary page to play ${template.id}"
      ) {

        val hp = HomePage.current

        val sum = hp.clickListDuplicateButton.validate

        val dc = hp.getButton("DuplicateCreate",Some("New")) mustBe Symbol("Enabled")
      }
    }

    def newDuplicate() = {
      import sessionDirector._

      withClueAndScreenShot(
          screenshotDir,
          s"${template.id}NewDuplicate",
          s"Error going to summary page to play ${template.id}"
      ) {

        val ldp = ListDuplicatePage.current

        val newd = ldp.clickNewDuplicateButton.validate

        val boardset = template.boardset
        val movement = template.movement

        newd.getNewButton(boardset, movement) mustBe Symbol("Enabled")

        tcpSleep(2)

        val dup = newd.click(boardset, movement).validate

        dupid = dup.dupid

        assert( dupid.isDefined && dupid.get.length()>0)
      }

    }

    def gotoDirectorsPage() = {
      import sessionDirector._

      val sbp = ScoreboardPage.current

      sbp.validate( (1 to 18).toList )

      val dsbp = sbp.clickMainMenu.validateMainMenu.clickDirectorButton

    }

    def startCompleteAndTables() = {
    waitForFutures(
        "startCompleteAndTables",
        logFuture {
          import sessionComplete._
          withClueAndScreenShot(screenshotDir, "startCompleteAndTables", s"In startup complete for template match ${template.id}") {

            assert( dupid.isDefined && dupid.get.length()>0)
            val hp = if (!sessionCompleteRunning) {
              HomePage.goto
            } else {
              HomePage.current
            }
            val dlp = hp.validate.clickListDuplicateButton.validate
            val sbp = dlp.clickDuplicate(dupid.get).validate

            sbp.checkTableButton("1")
          }
        } ::
        sessionTables.values.map { st => {
          logFuture {
            import st._
            withClueAndScreenShot(screenshotDir, "startCompleteAndTables", s"In startup table ${st.table} for template match ${template.id}") {
              assert( dupid.isDefined && dupid.get.length()>0)
              val hp = if (newTableSessions.contains(number)) {
                HomePage.goto
              } else {
                HomePage.current
              }
              val dlp = hp.validate.clickListDuplicateButton.validate
              val sbp = dlp.clickDuplicate(dupid.get).validate

              sbp.checkTableButton(st.number)
              sbp.clickTableButton(number.toInt).validate
            }
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

          val hp = page.clickBoardButton(board.id).validate

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

      withClueAndScreenShot(screenshotDir, "playRound", s"Table ${sessionTable.table} Round ${round}") {

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
                tp.setInputStyle("Guide")
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
        sessionTables.values.map { st => logFuture {playRound(round,st)}}.toList : _*
      )(roundtimeoutduration)
    }

    var boardSet: Option[BoardSet] = None

    def playAllRounds(): Unit = {
      val boardsetName = templateScore.getBoardSet

      import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._
      val ResponseFromHttp(status,loc,ce,bs,cd) = HttpUtils.getHttpObject[BoardSet](TestServer.getUrl("/v1/rest/boardsets/"+boardsetName))
      boardSet = bs

      val rounds = templateScore.tables.values.toList.head.length
      for (r <- 1 to rounds) playRound(r)
    }

    /**
     * Assumes on a Table page with a Round_n button for the round being played.
     */
    def atEndGotoSummaryPage(): Unit = {
      tcpSleep(30*numbertables)
      val roundtimeoutduration = Duration( 180, TimeUnit.SECONDS )
      waitForFutures(
        "atEndGotoSummaryPage",
        logFuture {
          import sessionDirector._
//          click on id("Game")
//          eventually { find(id("AllGames")) }
//          click on id("AllGames")
          eventually { find(id("Home")) }
          click on id("Home")
        }::
        logFuture {
          import sessionComplete._
//          click on id("AllGames")
          eventually { find(id("Home")) }
          click on id("Home")
        }::
        sessionTables.values.map { st =>
          logFuture {
            import st._
//            click on id("Game")
//            eventually { find(id("AllGames")) }
//            click on id("AllGames")
            eventually { find(id("Home")) }
            click on id("Home")
          }
        }.toList : _*
      )(roundtimeoutduration)
    }
  }

}
