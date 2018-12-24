package com.example.test.selenium

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import utils.logging.Logger
import org.scalatest.time.Millis
import org.scalatest.time.Span
import java.util.concurrent.TimeUnit
import com.example.test.util.MonitorTCP
import com.example.test.util.NoResultYet
import com.example.test.pages.chicago.EnterNamesPage
import com.example.data.bridge._
import com.example.test.pages.chicago.HandPage
import org.scalactic.source.Position
import com.example.test.TestStartLogging
import com.example.data.MatchChicago
import com.example.test.util.HttpUtils
import com.example.test.pages.chicago.SummaryPage
import org.scalatest.CancelAfterFailure
import java.io.InputStream
import scala.io.Source
import play.api.libs.json.Json
import com.example.test.util.GraphQLUtils
import java.net.URL
import java.io.OutputStreamWriter
import scala.io.Codec
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import com.example.backend.resource.FileIO
import com.example.test.pages.bridge.HomePage
import scala.reflect.io.File
import java.util.zip.ZipFile

object ChicagoTestPages {

  val log = Logger[ChicagoTestPages]

  val player1 = "Nancy"
  val player2 = "Sam"
  val player3 = "Ellen"
  val player4 = "Wayne"

  val playerOut = "Oscar"

  val players = player1::player2::player3::player4::Nil

}

class ChicagoTestPages extends FlatSpec with MustMatchers with BeforeAndAfterAll with CancelAfterFailure {
  import com.example.test.pages.PageBrowser._
  import com.example.test.util.EventuallyUtils._
  import Eventually.{ patienceConfig => _, _ }
  import ChicagoTestPages._

  import scala.concurrent.duration._

  import ChicagoUtils._

  val screenshotDir = "target/screenshots/ChicagoTestPages"
  val docsScreenshotDir = "target/docs/Chicago"

  TestStartLogging.startLogging()

  val Session1 = new Session

  val timeoutMillis = 15000
  val intervalMillis = 500

  val backend = TestServer.backend

  implicit val itimeout = PatienceConfig(timeout=scaled(Span(timeoutMillis, Millis)), interval=scaled(Span(intervalMillis,Millis)))

  val newChicagoButtonId = "Chicago2"
  val chicagoListURL: Option[String] = None
  val chicagoToListId: Option[String] = Some("Quit")

  implicit val timeoutduration = Duration( 60, TimeUnit.SECONDS )

  override
  def beforeAll() = {
    import scala.concurrent._
    import ExecutionContext.Implicits.global
    import com.example.test.util.ParallelUtils._

    MonitorTCP.nextTest()

    try {
      waitForFutures("Starting a browser or server",
                     CodeBlock { Session1.sessionStart().setPositionRelative(0,0).setSize(1100, 800)},
                     CodeBlock { TestServer.start() } )
    } catch {
      case e: Throwable =>
        afterAll()
        throw e
    }

  }

  override
  def afterAll() = {
    import scala.concurrent._
    import ExecutionContext.Implicits.global
    import com.example.test.util.ParallelUtils._

    waitForFuturesIgnoreTimeouts("Stopping a browser or server",
                   CodeBlock { Session1.sessionStop() },
                   CodeBlock { TestServer.stop() } )

  }

  var chicagoId: Option[String] = None   // eventually this will be obtained dynamically
  var startingNumberOfChicagosInServer = 0

  import Session1._

  behavior of "Chicago test of Bridge Server"

  it should "return a root page that has a title of \"The Bridge Score Keeper\"" in {
    tcpSleep(15)
    go to (TestServer.getAppPage())
    eventually { pageTitle mustBe ("The Bridge Score Keeper") }
  }

  it should "allow us to score a Chicago match" in {
    if (TestServer.isServerStartedByTest) {
      startingNumberOfChicagosInServer = backend.chicagos.syncStore.readAll() match {
        case Right(l) => l.size
        case Left((rc,msg)) => 0
      }
    }

    click on id(newChicagoButtonId)
    if (TestServer.isServerStartedByTest) {
      eventually( backend.chicagos.syncStore.readAll() match {
        case Right(l) => l.size mustBe startingNumberOfChicagosInServer+1
        case Left((rc,msg)) => throw new NoResultYet( rc.toString()+": "+msg )
      })
    }
    chicagoId = withClueAndScreenShot(screenshotDir, "ValidateEnterNames", "Error validating EnterNamesPage") {
      Some( eventually {
        EnterNamesPage.current.validate.chiid
      })
    }
  }

  it should "allow player names to be entered" in {
    eventually( find(xpath("//h1[2]")).text mustBe "Enter players and identify first dealer" )
    takeScreenshot(docsScreenshotDir, "EnterNames4")
  }

  it should "allow player names to be entered with suggestions when playing Chicago" in {
    val p = EnterNamesPage.current

    eventually( find(id(EnterNamesPage.buttonReset)) mustBe 'Enabled )
    find(id(EnterNamesPage.buttonOK)) must not be 'Enabled

    p.enterPlayer(North, player1).
      enterPlayer(South, player2).
      enterPlayer(East, "asfdfs")

    withClueAndScreenShot(screenshotDir, "Suggestions", "Error getting suggestions") {
      eventually {
        val sug = p.getPlayerSuggestions(East)
        withClue("Must have one li in visible div, found "+sug.mkString(",")+": ") { sug.size mustBe 1 }
        withClue("One li in visible div must show no names, found "+sug.head.text+": ") {
          sug.head.text must fullyMatch regex ( """No suggested names|No names matched""" )
        }
      }
    }

    p.esc

    p.getPlayer(North) mustBe player1
    p.getPlayer(South) mustBe player2
  }

  it should "allow player names to be reset when playing Chicago" in {
    val p = EnterNamesPage.current

    find(id(EnterNamesPage.buttonReset)) mustBe 'Enabled
    p.clickReset

    withClueAndScreenShot(screenshotDir, "Reset", "Error reseting names") {
      eventually {
        p.getPlayer(North) mustBe ""
        p.getPlayer(South) mustBe ""
        p.getPlayer(East) mustBe ""
        p.getPlayer(West) mustBe ""
      }
    }

  }

  it should "not allow duplicate names" in {
    val p = EnterNamesPage.current

    withClue("OK must be disabled") {
      p.isOKEnabled mustBe false
    }
    p.enterPlayer(North, player1, true).
      enterPlayer(South, player1, true).
      enterPlayer(East, player3, true).
      enterPlayer(West, player4, true)

    p.setDealer(North)

    eventually( p.isOKEnabled mustBe false )

    p.enterPlayer(South, player2, true)

    eventually( p.isOKEnabled mustBe true )

    p.clickFive

    eventually( p.isOKEnabled mustBe false )

    p.enterSittingOutPlayer(player1, true)

    eventually( p.isOKEnabled mustBe false )

    p.enterSittingOutPlayer(playerOut, true)

    eventually( p.isOKEnabled mustBe true )

    p.clickReset.validate
  }

  it should "allow player names to be entered when playing Chicago" in {
    val p = EnterNamesPage.current

    withClue("OK must be disabled") {
      p.isOKEnabled mustBe false
    }
    p.enterPlayer(North, player1, true).
      enterPlayer(South, player2, true).
      enterPlayer(East, player3, true).
      enterPlayer(West, player4, true)

    p.isDealer(South) mustBe false
    p.setDealer(South)
    withClueAndScreenShot(screenshotDir, "DealerSouth", "Dealer is south") {
      eventually {
        p.isDealer(South) mustBe true
        p.isDealer(North) mustBe false
      }
    }
    p.setDealer(North)
    withClueAndScreenShot(screenshotDir, "DealerNorth", "Dealer is north") {
      eventually {
        p.isDealer(South) mustBe false
        p.isDealer(North) mustBe true
      }
    }

    eventually( p.isOKEnabled mustBe true )

    val h = p.clickOK.validate

    withClueAndScreenShot(screenshotDir, "PlayerNamesAndVulnerability", "Player names and vulnerability") {
      eventually {
        h.getNameAndVul(North) mustBe s"${player1} vul"
        h.getNameAndVul(South) mustBe s"${player2} vul"
        h.getNameAndVul(East) mustBe s"${player3} vul"
        h.getNameAndVul(West) mustBe s"${player4} vul"
      }
    }

  }


  it should "send the player names to the server" in {

    def testPlayers( players: String* ) = {
        backend.chicagos.syncStore.read(chicagoId.get) match {
          case Right(c) =>
            // check if all players in MatchChicago are same as players argument
            players.zip(c.players).find( p => p._1!=p._2 ).isEmpty
          case Left(r) => false
        }
    }

    if (TestServer.isServerStartedByTest) {
      eventually( testPlayers(players:_*) mustBe true )
    }

    InputStyleHelper.hitInputStyleButton( "Yellow" )
  }


  /**
   * Enter the contract and click OK.
   * @param contractTricks
   * @param contractSuit
   * @param contractDoubled
   * @param declarer
   * @param madeOrDown
   * @param tricks
   * @param score check the score line
   * @param dealer check for dealer
   * @param patienceConfig
   * @param pos
   * @return the next page
   */
  def enterHand(
        h: HandPage,
        contractTricks: Int,
        contractSuit: ContractSuit,
        contractDoubled: ContractDoubled,
        declarer: PlayerPosition,
        madeOrDown: MadeOrDown,
        tricks: Int,
        nsVul: Vulnerability,
        ewVul: Vulnerability,
        score: String,
        dealer: String,
        round: Int,
        hand: Int,
        handScores: List[Int],
        roundScores: List[Int],
        totals: List[Int]
      )(implicit
          pos: Position
      ) = {

    withClueAndScreenShot(screenshotDir, s"EnterHandRound${round}Hand${hand}", s"round ${round} hand ${hand}") {
      h.validate
      val sp = h.enterHand(contractTricks, contractSuit, contractDoubled, declarer, madeOrDown, tricks, nsVul, ewVul, Some(score), Some(dealer))
      sp.validate
      val scores = handScores.map{ s =>
        if (s<0) {
          "x"
        } else if (s==0) {
          ""
        } else {
          s.toString()
        }
      }
      val roundS = roundScores.map(s => s.toString())
      val totalsS = totals.map(s => s.toString())
      sp.checkHandScore(hand, players, scores, roundS)
      sp.checkTotalScore(round, players, roundS, totalsS)
      sp
    }
  }

  it should "play a round of 4 hands" in {
    tcpSleep(30)
    val h = HandPage.current

    takeScreenshot(docsScreenshotDir, "EnterHandBefore")
    h.enterContract(3, Hearts, Doubled, West, Made, -1, None, None)
    h.takeScreenshot(docsScreenshotDir, "EnterHand")
    h.clickClear

    // N player1   S player2   E player3   W player4

    val sp = enterHand(h,4,Spades,NotDoubled,North,Made,4, NotVul, NotVul,
                       s"420 ${player1}-${player2}", player1, 0, 0,
                       List(420,420,0,0), List(420,420,0,0), List(420,420,0,0) )

    takeScreenshot(docsScreenshotDir, "SummaryPage")

    val sp2 = enterHand(sp.clickNextHand,4,Hearts,NotDoubled,East,Made,4, NotVul, Vul,
                        s"620 ${player3}-${player4}", player3, 0, 1,
                        List(0,0,620,620), List(420,420,620,620), List(420,420,620,620) )

    val sp3 = enterHand(sp2.clickNextHand,5,Diamonds,NotDoubled,North,Made,5, Vul, NotVul,
                        s"600 ${player1}-${player2}", player2, 0, 2,
                        List(600,600,0,0), List(1020,1020,620,620), List(1020,1020,620,620) )

    sp3.checkButtons(List(SummaryPage.buttonNextHand),
                     List(SummaryPage.buttonNewRound,SummaryPage.button6HandRound,SummaryPage.button8HandRound)
                    )

    val sp4 = enterHand(sp3.clickNextHand,5,Clubs,NotDoubled,West,Down,1, Vul, Vul,
                        s"100 ${player1}-${player2}", player4, 0, 3,
                        List(100,100,0,0), List(1120,1120,620,620), List(1120,1120,620,620) )

    takeScreenshot(docsScreenshotDir, "SummaryPage4")

  }

  def getChicago( chiid: String ): MatchChicago = {
    import com.example.data.rest.JsonSupport._
    val url = TestServer.getUrl(s"/v1/rest/chicagos/${chiid}")
    val o = HttpUtils.getHttpObject[MatchChicago](url)
    o.data match {
      case Some(r) => r
      case None =>
        log.warning(s"Unable to get MatchChicago from rest API for ${chiid}: ${o}")
        fail(s"Unable to get MatchChicago from rest API for ${chiid}")
    }
  }

  def postChicago( chi: MatchChicago ): String = {
    import com.example.data.rest.JsonSupport._
    val url = TestServer.getUrl(s"/v1/rest/chicagos")
    val o = HttpUtils.postHttpObject[MatchChicago](url, chi)
    o.data match {
      case Some(r) => r.id
      case None =>
        log.warning(s"Unable to post MatchChicago to rest API for ${chi.id}: ${o}")
        fail(s"Unable to post MatchChicago to rest API for ${chi.id}")
    }
  }

  var savedChicago: Option[MatchChicago] = None

  it should "get the MatchChicago object using the REST API" in {
    savedChicago = Some( getChicago(chicagoId.get) )
  }

  case class ResponseData( chicago: MatchChicago )
  case class QueryResponse( data: ResponseData )

  it should "have rest call and queryml call return the same match" in {
    import com.example.data.rest.JsonSupport._
    implicit val rdFormat = Json.format[ResponseData]
    implicit val qrFormat = Json.format[QueryResponse]

    chicagoId match {
      case Some(chiId) =>
        var qmlis: InputStream = null
        try {

          val chicagoQML = s"""
            |{
            |  chicago( id: "${chiId}") {
            |    id
            |    players
            |    rounds {
            |      id
            |      north
            |      south
            |      east
            |      west
            |      dealerFirstRound
            |      hands {
            |        id
            |        contractTricks
            |        contractSuit
            |        contractDoubled
            |        declarer
            |        nsVul
            |        ewVul
            |        madeContract
            |        tricks
            |        created
            |        updated
            |      }
            |      created
            |      updated
            |    }
            |    gamesPerRound
            |    simpleRotation
            |    created
            |    updated
            |  }
            |}
            |""".stripMargin

          val data = GraphQLUtils.queryToJson(chicagoQML)

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
              savedChicago.get mustBe qmlplayed.data.chicago
            case JsError(err) =>
              fail( s"Unable to parse response from graphQL: $err")
          }
        } catch {
          case x: Exception =>
            throw x
        } finally {
          if (qmlis != null) qmlis.close()
        }
      case _ =>
    }
  }

  it should "start playing another game using the saved game using next hand with 6 hands in round" in {
    val chiid = postChicago( savedChicago.get )

    val gp = SummaryPage.current.validate.clickQuit.validate
    tcpSleep(1)
    gp.clickButton(s"Chicago${chiid}")

//    val sp = SummaryPage.goto(chiid)
//    sp.refresh.validate

    val sp = SummaryPage.current.validate

    sp.chiid mustBe chiid

    tcpSleep(30)
    val buttons = sp.getAllButtons

    sp.checkButtons(List(SummaryPage.button6HandRound,SummaryPage.button8HandRound,
                         SummaryPage.buttonNewRound,SummaryPage.buttonNextHand),
                    List() )
    // N player1   S player2   E player3   W player4
//   List(100,100,0,0), List(1120,1120,620,620), List(1120,1120,620,620) )

    val sp2 = enterHand(sp.clickNextHand,4,Spades,NotDoubled,North,Made,4, NotVul, NotVul,
                        s"420 ${player1}-${player2}", player1, 0, 0,
                        List(420,420,0,0), List(1540,1540,620,620), List(1540,1540,620,620) )

    sp2.checkButtons(List(SummaryPage.button6HandRound,SummaryPage.button8HandRound),
                     List(SummaryPage.buttonNewRound,SummaryPage.buttonNextHand)
                    )

    val sp3 = enterHand(sp2.click6HandRound,5,Clubs,NotDoubled,West,Down,1, Vul, Vul,
                        s"100 ${player1}-${player2}", player3, 0, 3,
                        List(100,100,0,0), List(1640,1640,620,620), List(1640,1640,620,620) )

    sp3.checkButtons(
                     List(SummaryPage.buttonNewRound),
                     List(SummaryPage.button6HandRound,SummaryPage.button8HandRound,SummaryPage.buttonNextHand)
                    )

  }

  it should "start playing another game using the saved game using next hand with 8 hands in round" in {
    val chiid = postChicago( savedChicago.get )

    val gp = SummaryPage.current.validate.clickQuit.validate
    tcpSleep(1)
    gp.clickButton(s"Chicago${chiid}")

//    val sp = SummaryPage.goto(chiid)
//    sp.refresh.validate

    val sps = SummaryPage.current.validate

    sps.chiid mustBe chiid

    tcpSleep(30)
    val buttons = sps.getAllButtons

    sps.checkButtons(List(SummaryPage.button6HandRound,SummaryPage.button8HandRound,
                         SummaryPage.buttonNewRound,SummaryPage.buttonNextHand),
                    List() )
    // N player1   S player2   E player3   W player4
//   List(100,100,0,0), List(1120,1120,620,620), List(1120,1120,620,620) )

    val sp = enterHand(sps.clickNextHand,4,Spades,NotDoubled,North,Made,4, NotVul, NotVul,
                        s"420 ${player1}-${player2}", player1, 0, 0,
                        List(420,420,0,0), List(1540,1540,620,620), List(1540,1540,620,620) )

    sp.checkButtons(List(SummaryPage.button6HandRound,SummaryPage.button8HandRound),
                    List(SummaryPage.buttonNewRound,SummaryPage.buttonNextHand)
                   )

    val sp2 = enterHand(sp.click8HandRound,4,Hearts,NotDoubled,East,Made,4, NotVul, Vul,
                        s"620 ${player3}-${player4}", player3, 0, 1,
                        List(0,0,620,620), List(1540,1540,1240,1240), List(1540,1540,1240,1240) )

    sp2.checkButtons(List(SummaryPage.buttonNextHand),
                     List(SummaryPage.buttonNewRound,SummaryPage.button6HandRound,SummaryPage.button8HandRound)
                    )

    val sp3 = enterHand(sp2.clickNextHand,5,Diamonds,NotDoubled,North,Made,5, Vul, NotVul,
                        s"600 ${player1}-${player2}", player2, 0, 2,
                        List(600,600,0,0), List(2140,2140,1240,1240), List(2140,2140,1240,1240) )

    sp3.checkButtons(List(SummaryPage.buttonNextHand),
                     List(SummaryPage.buttonNewRound,SummaryPage.button6HandRound,SummaryPage.button8HandRound)
                    )

    val sp4 = enterHand(sp3.clickNextHand,5,Clubs,NotDoubled,West,Down,1, Vul, Vul,
                        s"100 ${player1}-${player2}", player4, 0, 3,
                        List(100,100,0,0), List(2240,2240,1240,1240), List(2240,2240,1240,1240) )

    sp4.checkButtons(List(SummaryPage.buttonNewRound),
                     List(SummaryPage.buttonNextHand,SummaryPage.button6HandRound,SummaryPage.button8HandRound)
                    )

  }

  it should "start playing another game using the saved game with 6 hands in round" in {
    val chiid = postChicago( savedChicago.get )

    val gp = SummaryPage.current.validate.clickQuit.validate
    takeScreenshot(docsScreenshotDir, "ListPage")
    tcpSleep(1)
    gp.clickButton(s"Chicago${chiid}")

//    val sp = SummaryPage.goto(chiid)
//    sp.refresh.validate

    val sp = SummaryPage.current.validate

    sp.chiid mustBe chiid

    tcpSleep(30)
    val buttons = sp.getAllButtons

    sp.checkButtons(List(SummaryPage.button6HandRound,SummaryPage.button8HandRound,
                         SummaryPage.buttonNewRound,SummaryPage.buttonNextHand),
                    List() )
    // N player1   S player2   E player3   W player4
//   List(100,100,0,0), List(1120,1120,620,620), List(1120,1120,620,620) )

    val sp2 = enterHand(sp.click6HandRound,4,Spades,NotDoubled,North,Made,4, NotVul, NotVul,
                        s"420 ${player1}-${player2}", player1, 0, 0,
                        List(420,420,0,0), List(1540,1540,620,620), List(1540,1540,620,620) )

    sp2.checkButtons(
                     List(SummaryPage.buttonNextHand),
                     List(SummaryPage.button6HandRound,SummaryPage.button8HandRound,SummaryPage.buttonNewRound)
                    )

    val sp3 = enterHand(sp2.clickNextHand,5,Clubs,NotDoubled,West,Down,1, Vul, Vul,
                        s"100 ${player1}-${player2}", player3, 0, 3,
                        List(100,100,0,0), List(1640,1640,620,620), List(1640,1640,620,620) )

    sp3.checkButtons(
                     List(SummaryPage.buttonNewRound),
                     List(SummaryPage.button6HandRound,SummaryPage.button8HandRound,SummaryPage.buttonNextHand)
                    )

    takeScreenshot(docsScreenshotDir, "SummaryPage6")

  }

  it should "start playing another game using the saved game with 8 hands in round" in {
    val chiid = postChicago( savedChicago.get )

    val gp = SummaryPage.current.validate.clickQuit.validate
    tcpSleep(1)
    gp.clickButton(s"Chicago${chiid}")

//    val sp = SummaryPage.goto(chiid)
//    sp.refresh.validate

    val sps = SummaryPage.current.validate

    sps.chiid mustBe chiid

    tcpSleep(30)
    val buttons = sps.getAllButtons

    sps.checkButtons(List(SummaryPage.button6HandRound,SummaryPage.button8HandRound,
                         SummaryPage.buttonNewRound,SummaryPage.buttonNextHand),
                    List() )
    // N player1   S player2   E player3   W player4
//   List(100,100,0,0), List(1120,1120,620,620), List(1120,1120,620,620) )

    val sp = enterHand(sps.click8HandRound,4,Spades,NotDoubled,North,Made,4, NotVul, NotVul,
                        s"420 ${player1}-${player2}", player1, 0, 0,
                        List(420,420,0,0), List(1540,1540,620,620), List(1540,1540,620,620) )

    sp.checkButtons(List(SummaryPage.buttonNextHand),
                    List(SummaryPage.buttonNewRound,SummaryPage.button6HandRound,SummaryPage.button8HandRound)
                   )

    val sp2 = enterHand(sp.clickNextHand,4,Hearts,NotDoubled,East,Made,4, NotVul, Vul,
                        s"620 ${player3}-${player4}", player3, 0, 1,
                        List(0,0,620,620), List(1540,1540,1240,1240), List(1540,1540,1240,1240) )

    sp2.checkButtons(List(SummaryPage.buttonNextHand),
                     List(SummaryPage.buttonNewRound,SummaryPage.button6HandRound,SummaryPage.button8HandRound)
                    )

    val sp3 = enterHand(sp2.clickNextHand,5,Diamonds,NotDoubled,North,Made,5, Vul, NotVul,
                        s"600 ${player1}-${player2}", player2, 0, 2,
                        List(600,600,0,0), List(2140,2140,1240,1240), List(2140,2140,1240,1240) )

    sp3.checkButtons(List(SummaryPage.buttonNextHand),
                     List(SummaryPage.buttonNewRound,SummaryPage.button6HandRound,SummaryPage.button8HandRound)
                    )

    val sp4 = enterHand(sp3.clickNextHand,5,Clubs,NotDoubled,West,Down,1, Vul, Vul,
                        s"100 ${player1}-${player2}", player4, 0, 3,
                        List(100,100,0,0), List(2240,2240,1240,1240), List(2240,2240,1240,1240) )

    sp4.checkButtons(List(SummaryPage.buttonNewRound),
                     List(SummaryPage.buttonNextHand,SummaryPage.button6HandRound,SummaryPage.button8HandRound)
                    )


  }

  var importZipFile: Option[File] = None
  it should "export zip" in {

    val hp = HomePage.goto.validate

    val ep = hp.clickExport.validate

    val f = ep.export

    importZipFile = Some(f)

    log.info( s"Downloaded export zip: ${f}" )

    import collection.JavaConverters._
    import resource._
    for (zip <- managed( new ZipFile(f.jfile) ) ) {
      zip.entries().asScala.map { ze => ze.getName }.toList must contain( s"store/MatchChicago.${chicagoId.get}.yaml" )
    }

    ep.clickHome
  }

  it should "import zip" in {
    val hp = HomePage.current.validate

    val ip = hp.clickImport.validate

    val initcount = ip.getImportedIds.length

    ip.checkSelectedFile(None)

    val rp = ip.selectFile(importZipFile.get).checkSelectedFile(importZipFile).clickImport.validate
    rp.isSuccessful mustBe true

    val ip2 = rp.clickLink.validate

    val imports = ip2.getImportedIds
    imports.length mustBe initcount+1

    val (importId,row) = imports.last

    importId mustBe importZipFile.get.name

    val lp = ip2.importChicago(importZipFile.get.name, row).validate

    lp.checkImportButton(chicagoId.get)

    val ldpr = lp.clickImport( chicagoId.get )

    val newId = ldpr.checkSuccessfulImport( chicagoId.get )

    val lp2 = ldpr.clickPopUpCancel.validate
    val main = lp2.clickHome.validate.clickListChicagoButton.validate( newId )
  }
}
