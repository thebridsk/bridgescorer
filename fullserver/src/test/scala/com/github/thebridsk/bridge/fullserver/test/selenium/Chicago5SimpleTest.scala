package com.github.thebridsk.bridge.fullserver.test.selenium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest._
import org.scalatest.time.Millis
import org.scalatest.time.Span
import com.github.thebridsk.bridge.data.bridge._
import java.util.concurrent.TimeUnit
import com.github.thebridsk.bridge.server.test.util.NoResultYet
import com.github.thebridsk.bridge.server.test.util.EventuallyUtils
import org.scalatest.concurrent.Eventually
import com.github.thebridsk.utilities.logging.Logger
import java.net.URL
import scala.io.Source
import scala.io.Codec
import com.github.thebridsk.bridge.server.test.util.MonitorTCP
import com.github.thebridsk.bridge.server.test.util.HttpUtils.ResponseFromHttp
import com.github.thebridsk.bridge.server.test.util.HttpUtils
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStoreConverters
import com.github.thebridsk.bridge.server.backend.MatchChicagoCacheStoreSupport
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage
import com.github.thebridsk.browserpages.Session
import scala.math.Ordering.Double.TotalOrdering
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.EnterNamesPage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.ChicagoMatchTypeSimple
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.HandPage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.SummaryPage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.SimpleSelectPartnersPage
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.bridge.data.MatchChicago

object Chicago5SimpleTest {
  val playerN = "Nancy"
  val playerS = "Sam"
  val playerE = "Ellen"
  val playerW = "Wayne"
  val playerO = "Brian"

  val allPlayers = playerN::playerS::playerE::playerW::playerO::Nil

}

import Chicago5SimpleTest._

/**
 * @author werewolf
 */
class Chicago5SimpleTest extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll
    with EventuallyUtils
    with CancelAfterFailure
{
  import com.github.thebridsk.browserpages.PageBrowser._
  import Eventually.{ patienceConfig => _, _ }

  import scala.concurrent.duration._

  val log = Logger[Chicago5SimpleTest]()

  TestStartLogging.startLogging()

  val docsScreenshotDir = "target/docs/Chicago"

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
    import com.github.thebridsk.bridge.server.test.util.ParallelUtils._

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
    import com.github.thebridsk.bridge.server.test.util.ParallelUtils._

    waitForFuturesIgnoreTimeouts("Stopping a browser or server",
                   CodeBlock { Session1.sessionStop() },
                   CodeBlock { TestServer.stop() } )

  }

  var chicagoId: Option[String] = None   // eventually this will be obtained dynamically
  var startingNumberOfChicagosInServer = 0

  import Session1._

  behavior of "Chicago test with 5 people and simple rotation of Bridge Server"

  it should "return a root page that has a title of \"The Bridge ScoreKeeper\"" in {
    HomePage.goto.validate
  }

  it should "allow us to score a Chicago match for five people" in {
    if (TestServer.isServerStartedByTest) {
      startingNumberOfChicagosInServer = backend.chicagos.syncStore.readAll() match {
        case Right(l) => l.size
        case Left((rc,msg)) => 0
      }
    }

    val enp = HomePage.current.clickNewChicagoButton.validate

    if (TestServer.isServerStartedByTest) {
      eventually( backend.chicagos.syncStore.readAll() match {
        case Right(l) => l.size mustBe startingNumberOfChicagosInServer+1
        case Left((rc,msg)) => throw new NoResultYet( rc.toString()+": "+msg )
      })
    }
    chicagoId = Some(enp.chiid)
  }

  it should "allow player names to be entered when playing Chicago" in {

    val enp = EnterNamesPage.current

    enp.isOKEnabled mustBe false

    enp.clickFive

    eventually( enp.validateFive )

    enp.enterSittingOutPlayer( s" $playerO" )
    enp.enterPlayer( East, s" $playerE " )
    enp.enterPlayer( South, s"$playerS " )
    enp.enterPlayer( West, playerW )
    enp.enterPlayer( North, s" $playerN" )

    enp.esc

    enp.clickFastRotation

    eventually {
      enp.isFairRotation mustBe true
      enp.isSimpleRotation mustBe false
    }

    enp.clickSimpleRotation

    eventually {
      enp.isFairRotation mustBe false
      enp.isSimpleRotation mustBe true
    }

    enp.setDealer(North)

    eventually( enp.isOKEnabled mustBe true )

    val hp = enp.clickOK.validate

    hp.getNameAndVul(North) mustBe s"$playerN vul"
    hp.getNameAndVul(South) mustBe s"$playerS vul"
    hp.getNameAndVul(East) mustBe s"$playerE vul"
    hp.getNameAndVul(West) mustBe s"$playerW vul"

    hp.checkDealer( playerN )

    hp.setInputStyle("Original")
  }

  it should "send the player names to the server" in {

    def testPlayers( players: String* ) = {
        backend.chicagos.syncStore.read(MatchChicago.id(chicagoId.get)) match {
          case Right(c) =>
            // check if all players in MatchChicago are same as players argument
            players.zip(c.players).find( p => p._1!=p._2 ).isEmpty
          case Left(r) =>
            fail("Did not find MatchChicago record")
        }
    }

    def testSimple() = {
        backend.chicagos.syncStore.read(MatchChicago.id(chicagoId.get)) match {
          case Right(c) =>
            c.gamesPerRound mustBe 1
            c.simpleRotation mustBe true
          case Left(r) =>
            fail("Did not find MatchChicago record")
        }
    }

    if (TestServer.isServerStartedByTest) {
      eventually( testPlayers(playerN,playerS,playerE,playerW, playerO) mustBe true )
      testSimple()
    }
  }

  def checkTotals( sp: SummaryPage, scoreN: Int, scoreS: Int, scoreE: Int, scoreW: Int, scoreO: Int ) = {
    sp.checkFastTotalsScore( allPlayers, List(scoreN, scoreS, scoreE, scoreW, scoreO).map( _.toString ) )
  }

  it should "play a round" in {

    val hp = HandPage.current(ChicagoMatchTypeSimple)

    val sp = hp.enterHand(
      4,Spades,NotDoubled,North,Made,4,
      nsVul=NotVul, ewVul=NotVul, score=None, dealer=Some(playerN),
    )

    checkTotals( sp, 420, 420, 0, 0, 0 )

    sp.setInputStyle("Guide")

    sp.clickNextHandSimple.validate
  }

  it should "show the partnerships and dealer for second round" in {

    val fspp = SimpleSelectPartnersPage.current

    fspp.checkPositions( "Prior hand", North, playerN, playerS, playerE, playerW, playerO )
    fspp.checkPositions( "Next hand", East,  playerO, playerS, playerE, playerW, playerN )

    takeScreenshot(docsScreenshotDir, "SelectNamesSimple")

    fspp.clickOK.validate
  }

  it should "play another round of 4 hands" in {
    val hp = HandPage.current(ChicagoMatchTypeSimple)

    hp.getNameAndVul(North) mustBe s"$playerO vul"
    hp.getNameAndVul(South) mustBe s"$playerS vul"
    hp.getNameAndVul(East) mustBe s"$playerE vul"
    hp.getNameAndVul(West) mustBe s"$playerW vul"

    hp.checkDealer( playerE )
    val sp = hp.enterHand(
      4,Spades,NotDoubled,North,Made,4,
      nsVul=NotVul, ewVul=NotVul, score=None, dealer=Some(playerE),
    )

    checkTotals( sp, 420, 840, 0, 0, 420 )

    sp.setInputStyle("Prompt")

    sp.clickNextHandSimple.validate
  }

  it should "show the partnerships and dealer for third round" in {

    val fspp = SimpleSelectPartnersPage.current

    fspp.checkPositions( "Prior hand", East,  playerO, playerS, playerE, playerW, playerN )
    fspp.checkPositions( "Next hand", South, playerO, playerS, playerN, playerW, playerE )

    fspp.clickOK.validate
  }

  it should "play third round of 4 hands" in {
    val hp = HandPage.current(ChicagoMatchTypeSimple)

    hp.getNameAndVul(North) mustBe s"$playerO vul"
    hp.getNameAndVul(South) mustBe s"$playerS vul"
    hp.getNameAndVul(East) mustBe s"$playerN vul"
    hp.getNameAndVul(West) mustBe s"$playerW vul"

    hp.checkDealer( playerS )
    val sp = hp.enterHand(
      4,Spades,NotDoubled,North,Made,4,
      nsVul=NotVul, ewVul=NotVul, score=None, dealer=Some(playerS),
    )

    checkTotals( sp, 420, 1260, 0, 0, 840 )

    sp.setInputStyle("Original")

    sp.clickNextHandSimple.validate
  }

  it should "show the partnerships and dealer for fourth round" in {

    val fspp = SimpleSelectPartnersPage.current

    fspp.checkPositions( "Prior hand", South, playerO, playerS, playerN, playerW, playerE )
    fspp.checkPositions( "Next hand", West,  playerO, playerE, playerN, playerW, playerS )

    fspp.clickOK.validate
  }

  it should "play fourth round of 4 hands" in {
    val hp = HandPage.current(ChicagoMatchTypeSimple)

    hp.getNameAndVul(North) mustBe s"$playerO vul"
    hp.getNameAndVul(South) mustBe s"$playerE vul"
    hp.getNameAndVul(East) mustBe s"$playerN vul"
    hp.getNameAndVul(West) mustBe s"$playerW vul"

    hp.checkDealer( playerW )
    val sp = hp.enterHand(
      4,NoTrump,NotDoubled,North,Made,4,
      nsVul=NotVul, ewVul=NotVul, score=None, dealer=Some(playerW),
    )

    checkTotals( sp, 420, 1260, 430, 0, 1270 )

    sp.clickNextHandSimple.validate
  }

  it should "show the partnerships and dealer for fifth round" in {

    val fspp = SimpleSelectPartnersPage.current

    fspp.checkPositions( "Prior hand", West,  playerO, playerE, playerN, playerW, playerS )
    fspp.checkPositions( "Next hand", North, playerO, playerE, playerN, playerS, playerW )

    fspp.clickOK.validate
  }

  it should "play fifth round of 4 hands" in {
    val hp = HandPage.current(ChicagoMatchTypeSimple)

    hp.getNameAndVul(North) mustBe s"$playerO vul"
    hp.getNameAndVul(South) mustBe s"$playerE vul"
    hp.getNameAndVul(East) mustBe s"$playerN vul"
    hp.getNameAndVul(West) mustBe s"$playerS vul"

    hp.checkDealer( playerO )
    val sp = hp.enterHand(
      4,Diamonds,NotDoubled,East,Made,4,
      nsVul=NotVul, ewVul=NotVul, score=None, dealer=Some(playerO),
    )

    checkTotals( sp, 550, 1390, 430, 0, 1270 )
  }


  it should "show the correct result in the chicago list page" in {
    val sp = SummaryPage.current(ChicagoMatchTypeSimple)

    val lp = sp.clickQuit.validate

    lp.checkPlayers(0, s"$playerS - 1390", s"$playerO - 1270", s"$playerN - 550", s"$playerE - 430", s"$playerW - 0")

  }

  it should "have timestamps on all objects in the MatchChicago record" in {
    val url: URL = new URL(TestServer.hosturl+"v1/rest/chicagos/"+chicagoId.get)
    val connection = url.openConnection()
    val is = connection.getInputStream
    try {
      val json = Source.fromInputStream(is)(Codec.UTF8).mkString

      implicit val instanceJson = new BridgeServiceFileStoreConverters(true).matchChicagoJson
      val (id,played) = new MatchChicagoCacheStoreSupport(false).fromJSON(json)

      val created = played.created
      val updated = played.updated

      created must not be (0)
      updated must not be (0)
      created must be <= updated

      played.rounds.foreach(r => {
        r.created must not be (0)
        r.updated must not be (0)
        r.created must be <= r.updated
        assert( created-100 <= r.created && r.created <= updated+100 )
        assert( created-100 <= r.updated && r.updated <= updated+100 )
        r.hands.foreach( h=> {
          h.created must not be (0)
          h.updated must not be (0)
          h.created must be <= h.updated
          assert( r.created-100 <= h.created && h.created <= r.updated+100 )
          assert( r.created-100 <= h.updated && h.updated <= r.updated+100 )
        })
      })

   } finally {
      is.close()
    }
  }

  behavior of "Names resource"

  it should "show the names without leading and trailing spaces" in {
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
