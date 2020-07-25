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
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStoreConverters
import com.github.thebridsk.bridge.server.backend.MatchChicagoCacheStoreSupport
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage
import com.github.thebridsk.browserpages.Session
import scala.math.Ordering.Double.TotalOrdering
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.EnterNamesPage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.SummaryPage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.HandPage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.ChicagoMatchTypeFour
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.FiveSelectPartnersPage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.ChicagoMatchTypeFive
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.bridge.data.MatchChicago

object Chicago5Test {
  val playerN = "Nancy"
  val playerS = "Sam"
  val playerE = "Ellen"
  val playerW = "Wayne"
  val playerO = "Brian"

  val allPlayers = playerN::playerS::playerE::playerW::playerO::Nil
}

/**
 * @author werewolf
 */
class Chicago5Test extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll
    with EventuallyUtils
    with CancelAfterFailure
{
  import com.github.thebridsk.browserpages.PageBrowser._
  import Eventually.{ patienceConfig => _, _ }

  import scala.concurrent.duration._

  import Chicago5Test._

  val log = Logger[Chicago5Test]()

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

  behavior of "Chicago test with 5 people of Bridge Server"

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

    chicagoId = Some(HomePage.current.clickNewChicagoButton.validate.chiid)

    if (TestServer.isServerStartedByTest) {
      eventually( backend.chicagos.syncStore.readAll() match {
        case Right(l) => l.size mustBe startingNumberOfChicagosInServer+1
        case Left((rc,msg)) => throw new NoResultYet( rc.toString()+": "+msg )
      })
    }

  }

  it should "allow player names to be entered when playing Chicago" in {

    val enp = EnterNamesPage.current

    eventually( enp.isResetEnabled mustBe true )
    enp.isOKEnabled mustBe false

    enp.clickFive
    eventually {
      enp.isFive mustBe true
    }

    enp.enterPlayer(North, playerN)
    enp.enterPlayer(South, playerS)
    enp.enterPlayer(East, playerE)
    enp.enterPlayer(West, playerW)
    enp.enterSittingOutPlayer(playerO)

    enp.esc

    enp.setDealer(North)

    eventually { enp.isOKEnabled mustBe true }

    val hp = enp.clickOK.validate
    eventually {
      hp.checkVulnerable(North, NotVul)
      hp.getName(North) mustBe playerN

      hp.getNameAndVul(South) mustBe s"$playerS vul"
      hp.getNameAndVul(East) mustBe s"$playerE vul"
      hp.getNameAndVul(West) mustBe s"$playerW vul"
    }
  }

  it should "send the player names to the server" in {

    def testPlayers( players: String* ) = {
        backend.chicagos.syncStore.read(MatchChicago.id(chicagoId.get)) match {
          case Right(c) =>
            // check if all players in MatchChicago are same as players argument
            players.zip(c.players).find( p => p._1!=p._2 ).isEmpty
          case Left(r) => false
        }
    }

    if (TestServer.isServerStartedByTest) {
      eventually( testPlayers(playerN,playerS,playerE,playerW, playerO) mustBe true )
    }

  }

  def checkTotalScores( sp: SummaryPage, scores: Int* ) = {
    sp.checkTotalsScore(allPlayers,scores.map(_.toString).toList)
  }

  it should "play a round of 4 hands" in {

    val hp = HandPage.current(ChicagoMatchTypeFour)

    hp.setInputStyle("Original")

    val sp = hp.enterHand(
        4, Spades, NotDoubled, North, Made, 4,
        score = Some(s"420 $playerN-$playerS"), nsVul = NotVul, ewVul = NotVul, dealer = Some(playerN)
    ).validate
    checkTotalScores( sp, 420, 420, 0, 0, 0 )
    val hp2 = sp.clickNextHand
    val sp2 = hp2.enterHand(
        4,Hearts,NotDoubled,East,Made,4,
        score = Some(s"620 $playerE-$playerW"), nsVul = NotVul, ewVul = Vul, dealer = Some(playerE)
    ).validate
    checkTotalScores( sp2, 420, 420, 620, 620, 0 )
    val hp3 = sp2.clickNextHand
    val sp3 = hp3.enterHand(
        5,Diamonds,NotDoubled,South,Made,5,
        score = Some(s"600 $playerN-$playerS"), nsVul = Vul, ewVul = NotVul, dealer = Some(playerS)
    ).validate
    checkTotalScores( sp3, 1020, 1020, 620, 620, 0 )
    val hp4 = sp3.clickNextHand
    val sp4 = hp4.enterHand(
        3,Clubs,NotDoubled,West,Made,4,
        score = Some(s"130 $playerE-$playerW"), nsVul = Vul, ewVul = Vul, dealer = Some(playerW)
    ).validate
    checkTotalScores( sp4, 1020, 1020, 750, 750, 0 )

    sp4.setInputStyle("Guide")

    sp4.clickNewRoundFive.validate

  }

  it should "allow setting the partner and dealer for second round" in {

    val fspp = FiveSelectPartnersPage.current

    fspp.isOKEnabled mustBe false

    fspp.checkSittingOutPlayerNames(None,playerN,playerS,playerE,playerW)
    fspp.clickPlayerSittingOut(playerE)
    val pairings = fspp.getPairings
    pairings.length mustBe 2
    val pairing1 = pairings.head
    withClue(s"Testing pairing 0: $pairing1, $playerN-$playerW $playerO-$playerS") {
      pairing1.containsPair(playerN,playerW) mustBe true
      pairing1.containsPair(playerO,playerS) mustBe true
    }
    val pairing2 = pairings.tail.head
    withClue(s"Testing pairing 1: $pairing2, $playerN-$playerO $playerS-$playerW") {
      pairing2.containsPair(playerN,playerO) mustBe true
      pairing2.containsPair(playerS,playerW) mustBe true
    }

    fspp.clickPairing(1)

    takeScreenshot(docsScreenshotDir, "SelectNames5")

    val seats = fspp.getSeats
    seats.north mustBe playerO
    seats.south mustBe playerN
    seats.east mustBe playerS
    seats.west mustBe playerW
    seats.sittingOut mustBe playerE

    fspp.clickSwap(South)

    val seats2 = fspp.getSeats
    seats2.north mustBe playerN
    seats2.south mustBe playerO
    seats2.east mustBe playerS
    seats2.west mustBe playerW
    seats2.sittingOut mustBe playerE

    fspp.clickDealer(North)

    fspp.isOKEnabled mustBe true

    fspp.clickOK.validate
  }

  it should "send to the server that there are 4 games per round" in {

    def getGamesPerRound = {
        backend.chicagos.syncStore.read(MatchChicago.id(chicagoId.get)) match {
          case Right(c) => c.gamesPerRound
          case Left(r) => -1
        }
    }

    if (TestServer.isServerStartedByTest) {
      eventually( getGamesPerRound mustBe 4 )
    }
  }

  it should "play another round of 4 hands" in {

    val hp = HandPage.current(ChicagoMatchTypeFour)

    hp.getNameAndVul(North) mustBe s"$playerN vul"
    hp.getNameAndVul(South) mustBe s"$playerO vul"
    hp.getNameAndVul(East) mustBe s"$playerS vul"
    hp.getNameAndVul(West) mustBe s"$playerW vul"

    val sp = hp.enterHand(
        4, Spades, NotDoubled, North, Made, 4,
        score = Some(s"420 $playerN-$playerO"), nsVul = NotVul, ewVul = NotVul, dealer = Some(playerN)
    ).validate
    checkTotalScores( sp, 1440, 1020, 750, 750, 420 )
    val hp2 = sp.clickNextHand
    val sp2 = hp2.enterHand(
        4,Hearts,NotDoubled,East,Made,4,
        score = Some(s"620 $playerS-$playerW"), nsVul = NotVul, ewVul = Vul, dealer = Some(playerS)
    ).validate
    checkTotalScores( sp2, 1440, 1640, 750, 1370, 420 )
    val hp3 = sp2.clickNextHand
    val sp3 = hp3.enterHand(
        5,Diamonds,NotDoubled,South,Made,5,
        score = Some(s"600 $playerN-$playerO"), nsVul = Vul, ewVul = NotVul, dealer = Some(playerO)
    ).validate
    checkTotalScores( sp3, 2040, 1640, 750, 1370, 1020 )
    val hp4 = sp3.clickNextHand
    val sp4 = hp4.enterHand(
        3,Clubs,NotDoubled,West,Made,4,
        score = Some(s"130 $playerS-$playerW"), nsVul = Vul, ewVul = Vul, dealer = Some(playerW)
    ).validate
    checkTotalScores( sp4, 2040, 1770, 750, 1500, 1020 )

    sp4.clickNewRoundFive.validate
  }

  it should "allow setting the scorekeeper and dealer for third round" in {
    val fspp = FiveSelectPartnersPage.current

    fspp.clickPlayerSittingOut(playerS)
    fspp.clickDealer(East)
    fspp.clickOK.validate
  }

  it should "send to the server that there are 4 games per round once more" in {
    // keeping this for more checks

    def getGamesPerRound = {
        backend.chicagos.syncStore.read(MatchChicago.id(chicagoId.get)) match {
          case Right(c) => c.gamesPerRound
          case Left(r) => -1
        }
    }

    if (TestServer.isServerStartedByTest) {
      eventually( getGamesPerRound mustBe 4 )
    }
  }

  it should "play third round of 4 hands" in {

    val hp = HandPage.current(ChicagoMatchTypeFour)

    hp.setInputStyle("Prompt")

    hp.getNameAndVul(North) mustBe s"$playerO vul"
    hp.getNameAndVul(South) mustBe s"$playerE vul"
    hp.getNameAndVul(East) mustBe s"$playerN vul"
    hp.getNameAndVul(West) mustBe s"$playerW vul"

    val sp = hp.enterHand(
        4, Spades, NotDoubled, North, Made, 4,
        score = Some(s"420 $playerO-$playerE"), nsVul = NotVul, ewVul = NotVul, dealer = Some(playerN)
    ).validate
    checkTotalScores( sp, 2040, 1770, 1170, 1500, 1440 )
    val hp2 = sp.clickNextHand
    val sp2 = hp2.enterHand(
        4,Hearts,NotDoubled,East,Made,4,
        score = Some(s"420 $playerN-$playerW"), nsVul = Vul, ewVul = NotVul, dealer = Some(playerE)
    ).validate
    checkTotalScores( sp2, 2460, 1770, 1170, 1920, 1440 )
    val hp3 = sp2.clickNextHand
    val sp3 = hp3.enterHand(
        5,Diamonds,NotDoubled,South,Made,5,
        score = Some(s"400 $playerO-$playerE"), nsVul = NotVul, ewVul = Vul, dealer = Some(playerW)
    ).validate
    checkTotalScores( sp3, 2460, 1770, 1570, 1920, 1840 )
    val hp4 = sp3.clickNextHand
    val sp4 = hp4.enterHand(
        3,Clubs,NotDoubled,West,Made,4,
        score = Some(s"130 $playerN-$playerW"), nsVul = Vul, ewVul = Vul, dealer = Some(playerO)
    ).validate
    checkTotalScores( sp4, 2590, 1770, 1570, 2050, 1840 )

    sp4.clickNewRoundFive.validate

  }

  it should "allow setting the scorekeeper, partner and dealer for fourth round" in {
    val fspp = FiveSelectPartnersPage.current

    fspp.clickPlayerSittingOut(playerW)
    fspp.clickDealer(North)
    fspp.clickClockwise
    fspp.clickOK.validate
  }

  it should "play fourth round of 4 hands" in {

    val hp = HandPage.current(ChicagoMatchTypeFour)

    hp.setInputStyle("Guide")

    hp.getNameAndVul(North) mustBe s"$playerN vul"
    hp.getNameAndVul(South) mustBe s"$playerE vul"
    hp.getNameAndVul(East) mustBe s"$playerO vul"
    hp.getNameAndVul(West) mustBe s"$playerS vul"

    val sp = hp.enterHand(
        4, Spades, NotDoubled, North, Made, 4,
        score = Some(s"420 $playerN-$playerE"), nsVul = NotVul, ewVul = NotVul, dealer = Some(playerN)
    ).validate
    checkTotalScores( sp, 3010, 1770, 1990, 2050, 1840 )
    val hp2 = sp.clickNextHand
    val sp2 = hp2.enterHand(
        4,Hearts,NotDoubled,East,Made,4,
        score = Some(s"620 $playerO-$playerS"), nsVul = NotVul, ewVul = Vul, dealer = Some(playerO)
    ).validate
    checkTotalScores( sp2, 3010, 2390, 1990, 2050, 2460 )
    val hp3 = sp2.clickNextHand
    val sp3 = hp3.enterHand(
        5,Diamonds,NotDoubled,South,Made,5,
        score = Some(s"600 $playerN-$playerE"), nsVul = Vul, ewVul = NotVul, dealer = Some(playerE)
    ).validate
    checkTotalScores( sp3, 3610, 2390, 2590, 2050, 2460 )
    val hp4 = sp3.clickNextHand
    val sp4 = hp4.enterHand(
        3,Clubs,NotDoubled,West,Made,4,
        score = Some(s"130 $playerO-$playerS"), nsVul = Vul, ewVul = Vul, dealer = Some(playerS)
    ).validate
    checkTotalScores( sp4, 3610, 2520, 2590, 2050, 2590 )

    sp4.clickNewRoundFive.validate
  }

  it should "allow setting the scorekeeper, partner and dealer for fifth round" in {
    val fspp = FiveSelectPartnersPage.current

    fspp.clickDealer(North)
    fspp.clickOK.validate
  }

  it should "play fifth round of 4 hands" in {

    val hp = HandPage.current(ChicagoMatchTypeFour)

    hp.getNameAndVul(North) mustBe s"$playerO vul"
    hp.getNameAndVul(South) mustBe s"$playerW vul"
    hp.getNameAndVul(East) mustBe s"$playerE vul"
    hp.getNameAndVul(West) mustBe s"$playerS vul"

    val sp = hp.enterHand(
        4, Spades, NotDoubled, North, Made, 4,
        score = Some(s"420 $playerO-$playerW"), nsVul = NotVul, ewVul = NotVul, dealer = Some(playerO)
    ).validate
    checkTotalScores( sp, 3610, 2520, 2590, 2470, 3010 )
    val hp2 = sp.clickNextHand
    val sp2 = hp2.enterHand(
        4,Hearts,NotDoubled,East,Made,4,
        score = Some(s"620 $playerE-$playerS"), nsVul = NotVul, ewVul = Vul, dealer = Some(playerE)
    ).validate
    checkTotalScores( sp2, 3610, 3140, 3210, 2470, 3010 )
    val hp3 = sp2.clickNextHand
    val sp3 = hp3.enterHand(
        5,Diamonds,NotDoubled,South,Made,5,
        score = Some(s"600 $playerO-$playerW"), nsVul = Vul, ewVul = NotVul, dealer = Some(playerW)
    ).validate
    checkTotalScores( sp3, 3610, 3140, 3210, 3070, 3610 )
    val hp4 = sp3.clickNextHand
    val sp4 = hp4.enterHand(
        3,Clubs,NotDoubled,West,Made,4,
        score = Some(s"130 $playerE-$playerS"), nsVul = Vul, ewVul = Vul, dealer = Some(playerS)
    ).validate
    checkTotalScores( sp4, 3610, 3270, 3340, 3070, 3610 )
  }

  it should "show the correct result in the chicago list page" in {
    val sp = SummaryPage.current(ChicagoMatchTypeFive)

    val lp = sp.clickQuit.validate

    lp.checkPlayers(0, s"$playerO - 3610", s"$playerN - 3610", s"$playerE - 3340", s"$playerS - 3270", s"$playerW - 3070" )

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

}
