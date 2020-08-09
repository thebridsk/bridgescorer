package com.github.thebridsk.bridge.fullserver.test.selenium

import java.net.URL
import java.util.concurrent.TimeUnit
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.Millis
import org.scalatest.time.Span
import scala.io.Codec
import scala.io.Source

import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.EnterNamesPage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.FairSelectPartnersPage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.HandPage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.ListPage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.SummaryPage
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStoreConverters
import com.github.thebridsk.bridge.server.backend.MatchChicagoCacheStoreSupport
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.bridge.server.test.util.EventuallyUtils
import com.github.thebridsk.bridge.server.test.util.MonitorTCP
import com.github.thebridsk.bridge.server.test.util.NoResultYet
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.browserpages.Session
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.ChicagoMatchTypeFair
import com.github.thebridsk.bridge.data.MatchChicago

object Chicago5FairTest {
  val playerN = "Nancy"
  val playerS = "Sam"
  val playerE = "Ellen"
  val playerW = "Wayne"
  val playerO = "Brian"

  val allPlayers: List[String] =
    playerN :: playerS :: playerE :: playerW :: playerO :: Nil

}

import Chicago5FairTest._

/**
  * @author werewolf
  */
class Chicago5FairTest
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll
    with EventuallyUtils
    with CancelAfterFailure {
  import com.github.thebridsk.browserpages.PageBrowser._
  import Eventually.{patienceConfig => _, _}

  import scala.concurrent.duration._

  val log: Logger = Logger[Chicago5FairTest]()

  val docsScreenshotDir = "target/docs/Chicago"

  val Session1 = new Session

  val timeoutMillis = 15000
  val intervalMillis = 500

  val backend = TestServer.backend

  implicit val itimeout: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(timeoutMillis, Millis)),
    interval = scaled(Span(intervalMillis, Millis))
  )

  val newChicagoButtonId = "Chicago2"
  val chicagoListURL: Option[String] = None
  val chicagoToListId: Option[String] = Some("Quit")

  implicit val timeoutduration: FiniteDuration = Duration(60, TimeUnit.SECONDS)

  override def beforeAll(): Unit = {
    import com.github.thebridsk.bridge.server.test.util.ParallelUtils._

    MonitorTCP.nextTest()

    try {
      waitForFutures(
        "Starting a browser or server",
        CodeBlock {
          Session1.sessionStart().setPositionRelative(0, 0).setSize(1100, 800)
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
    import com.github.thebridsk.bridge.server.test.util.ParallelUtils._

    waitForFuturesIgnoreTimeouts(
      "Stopping a browser or server",
      CodeBlock { Session1.sessionStop() },
      CodeBlock { TestServer.stop() }
    )

  }

  var chicagoId: Option[String] =
    None // eventually this will be obtained dynamically
  var startingNumberOfChicagosInServer = 0

  import Session1._

  val screenshotDir = "target/screenshots/Chicago5FairTest"

  TestStartLogging.startLogging()

  behavior of "Chicago test with 5 people and simple rotation of Bridge Server"

  it should "return a root page that has a title of \"The Bridge ScoreKeeper\"" in {
    HomePage.goto.validate
  }

  it should "allow us to score a Chicago match for five people" in {
    if (TestServer.isServerStartedByTest) {
      startingNumberOfChicagosInServer =
        backend.chicagos.syncStore.readAll() match {
          case Right(l)        => l.size
          case Left((rc, msg)) => 0
        }
    }

    val enp = HomePage.current.clickNewChicagoButton.validate

    if (TestServer.isServerStartedByTest) {
      eventually(backend.chicagos.syncStore.readAll() match {
        case Right(l) => l.size mustBe startingNumberOfChicagosInServer + 1
        case Left((rc, msg)) =>
          throw new NoResultYet(rc.toString() + ": " + msg)
      })
    }
    chicagoId = Some(enp.chiid)
  }

  it should "allow player names to be entered when playing Chicago" in {

    val enp = EnterNamesPage.current

    enp.isOKEnabled mustBe false

    enp.clickFive

    eventually(enp.validateFive)

    enp.enterSittingOutPlayer(playerO)
    enp.enterPlayer(East, playerE)
    enp.enterPlayer(South, playerS)
    enp.enterPlayer(West, playerW)
    enp.enterPlayer(North, playerN)

    enp.esc

    enp.clickFastRotation

    eventually {
      enp.isFairRotation mustBe true
    }

    takeScreenshot(docsScreenshotDir, "EnterNames5")

    enp.clickFairRotation

    enp.setDealer(North)

    eventually(enp.isOKEnabled mustBe true)

    val hp = enp.clickOK.validate

    hp.getNameAndVul(North) mustBe s"$playerN vul"
    hp.getNameAndVul(South) mustBe s"$playerS vul"
    hp.getNameAndVul(East) mustBe s"$playerE vul"
    hp.getNameAndVul(West) mustBe s"$playerW vul"

    hp.checkDealer(playerN)

    hp.setInputStyle("Original")
  }

  it should "send the player names to the server" in {

    def testPlayers(players: String*) = {
      backend.chicagos.syncStore.read(MatchChicago.id(chicagoId.get)) match {
        case Right(c) =>
          // check if all players in MatchChicago are same as players argument
          players.zip(c.players).find(p => p._1 != p._2).isEmpty
        case Left(r) =>
          fail("Did not find MatchChicago record")
      }
    }

    def testSimple() = {
      backend.chicagos.syncStore.read(MatchChicago.id(chicagoId.get)) match {
        case Right(c) =>
          c.gamesPerRound mustBe 1
          c.simpleRotation mustBe false
        case Left(r) =>
          fail("Did not find MatchChicago record")
      }
    }

    if (TestServer.isServerStartedByTest) {
      eventually(
        testPlayers(playerN, playerS, playerE, playerW, playerO) mustBe true
      )
      testSimple()
    }
  }

  def checkTotals(
      sp: SummaryPage,
      scoreN: Int,
      scoreS: Int,
      scoreE: Int,
      scoreW: Int,
      scoreO: Int
  ): SummaryPage = {
    sp.checkFastTotalsScore(
      allPlayers,
      List(scoreN, scoreS, scoreE, scoreW, scoreO).map(_.toString)
    )
  }

  it should "cancel the hand in the first round" in {
    val hp = HandPage.current(ChicagoMatchTypeFair)

    val sp = hp.clickCancel.validate

    checkTotals(sp, 0, 0, 0, 0, 0)

    val enp = sp.clickNextHandFairToEnterNames.validate

    enp.roundid mustBe 0

    enp.clickOK.validate
  }

  it should "play a round" in {

    val hp = HandPage.current(ChicagoMatchTypeFair)

    val sp = hp.enterHand(
      4,
      Spades,
      NotDoubled,
      North,
      Made,
      4,
      nsVul = NotVul,
      ewVul = NotVul,
      score = None,
      dealer = Some(playerN)
    )

    checkTotals(sp, 420, 420, 0, 0, 0)

    sp.setInputStyle("Guide")

    sp.clickNextHandFair.validate

  }

  it should "show the partnerships and dealer for second round" in {

    val fspp = FairSelectPartnersPage.current

    fspp.checkPositions(
      "Prior hand",
      North,
      playerN,
      playerS,
      playerE,
      playerW,
      playerO
    )

    fspp.checkSittingOutPlayerNames(None, playerN, playerS, playerE, playerW)
    fspp.checkNotFoundPlayersForSittingOut(playerO)

    fspp.clickSittingOutPlayer(playerN)

    eventually {
      fspp.checkSittingOutPlayerNames(Some(playerN), playerS, playerE, playerW)
      fspp.checkPositions(
        "Next hand",
        East,
        playerO,
        playerW,
        playerE,
        playerS,
        playerN
      )
    }

    takeScreenshot(docsScreenshotDir, "SelectNamesFair")

    fspp.clickOK.validate
  }

  it should "cancel playing the second round" in {

    val hp = HandPage.current(ChicagoMatchTypeFair)
    val sp = hp.clickCancel.validate
    checkTotals(sp, 420, 420, 0, 0, 0)
    val fspp = sp.clickNextHandFair.validate

    fspp.roundid mustBe 1

    fspp.checkPositions(
      "Prior hand",
      North,
      playerN,
      playerS,
      playerE,
      playerW,
      playerO
    )

    fspp.checkSittingOutPlayerNames(None, playerN, playerS, playerE, playerW)
    fspp.checkNotFoundPlayersForSittingOut(playerO)

    fspp.clickSittingOutPlayer(playerN)

    eventually {
      fspp.checkSittingOutPlayerNames(Some(playerN), playerS, playerE, playerW)
      fspp.checkPositions(
        "Next hand",
        East,
        playerO,
        playerW,
        playerE,
        playerS,
        playerN
      )
    }

    fspp.clickOK.validate
  }

  it should "play another round of 4 hands" in {
    val hp = HandPage.current(ChicagoMatchTypeFair)

    hp.getNameAndVul(North) mustBe s"$playerO vul"
    hp.getNameAndVul(South) mustBe s"$playerW vul"
    hp.getNameAndVul(East) mustBe s"$playerE vul"
    hp.getNameAndVul(West) mustBe s"$playerS vul"

    hp.checkDealer(playerE)
    val sp = hp.enterHand(
      3,
      NoTrump,
      NotDoubled,
      North,
      Made,
      3,
      nsVul = NotVul,
      ewVul = NotVul,
      score = None,
      dealer = Some(playerE)
    )

    checkTotals(sp, 420, 420, 0, 400, 400)

    sp.setInputStyle("Prompt")

    sp.clickNextHandFair.validate
  }

  it should "show the partnerships and dealer for third round" in {

    val fspp = FairSelectPartnersPage.current

    fspp.checkPositions(
      "Prior hand",
      East,
      playerO,
      playerW,
      playerE,
      playerS,
      playerN
    )

    fspp.checkSittingOutPlayerNames(None, playerS, playerE, playerW)
    fspp.checkNotFoundPlayersForSittingOut(playerO, playerN)

    fspp.clickSittingOutPlayer(playerE)

    eventually {
      fspp.checkSittingOutPlayerNames(Some(playerE), playerS, playerW)
      fspp.checkPositions(
        "Next hand",
        South,
        playerS,
        playerW,
        playerN,
        playerO,
        playerE
      )
    }

    fspp.clickOK.validate
  }

  it should "play third round of 4 hands" in {
    val hp = HandPage.current(ChicagoMatchTypeFair)

    hp.getNameAndVul(North) mustBe s"$playerS vul"
    hp.getNameAndVul(South) mustBe s"$playerW vul"
    hp.getNameAndVul(East) mustBe s"$playerN vul"
    hp.getNameAndVul(West) mustBe s"$playerO vul"

    hp.checkDealer(playerW)
    val sp = hp.enterHand(
      4,
      Spades,
      NotDoubled,
      North,
      Made,
      5,
      nsVul = NotVul,
      ewVul = NotVul,
      score = None,
      dealer = Some(playerW)
    )

    checkTotals(sp, 420, 870, 0, 850, 400)

    sp.setInputStyle("Original")

    takeScreenshot(docsScreenshotDir, "SummaryQuintetPage")

    sp.clickNextHandFair.validate

  }

  it should "show the partnerships and dealer for fourth round" in {

    val fspp = FairSelectPartnersPage.current

    fspp.checkPositions(
      "Prior hand",
      South,
      playerS,
      playerW,
      playerN,
      playerO,
      playerE
    )

    fspp.checkSittingOutPlayerNames(None, playerS, playerW)
    fspp.checkNotFoundPlayersForSittingOut(playerO, playerN, playerE)

    fspp.clickSittingOutPlayer(playerW)

    eventually {
      fspp.checkSittingOutPlayerNames(Some(playerW), playerS)
      fspp.checkPositions(
        "Next hand",
        West,
        playerN,
        playerE,
        playerS,
        playerO,
        playerW
      )
    }

    fspp.clickOK.validate

  }

  it should "play fourth round of 4 hands" in {
    val hp = HandPage.current(ChicagoMatchTypeFair)

    hp.getNameAndVul(North) mustBe s"$playerN vul"
    hp.getNameAndVul(South) mustBe s"$playerE vul"
    hp.getNameAndVul(East) mustBe s"$playerS vul"
    hp.getNameAndVul(West) mustBe s"$playerO vul"

    hp.checkDealer(playerO)
    val sp = hp.enterHand(
      1,
      NoTrump,
      NotDoubled,
      North,
      Made,
      4,
      nsVul = NotVul,
      ewVul = NotVul,
      score = None,
      dealer = Some(playerO)
    )

    checkTotals(sp, 600, 870, 180, 850, 400)

    sp.clickNextHandFair.validate
  }

  it should "show the partnerships and dealer for fifth round" in {

    val fspp = FairSelectPartnersPage.current

    fspp.checkPositions(
      "Prior hand",
      West,
      playerN,
      playerE,
      playerS,
      playerO,
      playerW
    )

    fspp.checkSittingOutPlayerNames(Some(playerS))
    fspp.checkNotFoundPlayersForSittingOut(playerO, playerN, playerE, playerW)

    fspp.checkPositions(
      "Next hand",
      North,
      playerO,
      playerE,
      playerW,
      playerN,
      playerS
    )

    fspp.clickOK.validate
  }

  it should "play fifth round of 4 hands" in {
    val hp = HandPage.current(ChicagoMatchTypeFair)

    hp.getNameAndVul(North) mustBe s"$playerO vul"
    hp.getNameAndVul(South) mustBe s"$playerE vul"
    hp.getNameAndVul(East) mustBe s"$playerW vul"
    hp.getNameAndVul(West) mustBe s"$playerN vul"

    hp.checkDealer(playerO)
    val sp = hp.enterHand(
      6,
      NoTrump,
      NotDoubled,
      South,
      Made,
      6,
      nsVul = NotVul,
      ewVul = NotVul,
      score = None,
      dealer = Some(playerO)
    )

    checkTotals(sp, 600, 870, 1170, 850, 1390)

    sp.clickNextHandFair.validate
  }

  it should "show the partnerships and dealer for sixth round" in {

    val fspp = FairSelectPartnersPage.current

    fspp.checkPositions(
      "Prior hand",
      North,
      playerO,
      playerE,
      playerW,
      playerN,
      playerS
    )

    fspp.checkSittingOutPlayerNames(None, playerO, playerN, playerE, playerW)
    fspp.checkNotFoundPlayersForSittingOut(playerS)

    fspp.clickSittingOutPlayer(playerO)

    eventually {
      fspp.checkSittingOutPlayerNames(Some(playerO), playerN, playerE, playerW)
      fspp.checkPositions(
        "Next hand",
        East,
        playerS,
        playerN,
        playerW,
        playerE,
        playerO
      )
    }

    fspp.clickOK.validate
  }

  it should "play sixth round of 4 hands" in {
    val hp = HandPage.current(ChicagoMatchTypeFair)

    hp.getNameAndVul(North) mustBe s"$playerS vul"
    hp.getNameAndVul(South) mustBe s"$playerN vul"
    hp.getNameAndVul(East) mustBe s"$playerW vul"
    hp.getNameAndVul(West) mustBe s"$playerE vul"

    hp.checkDealer(playerW)
    val sp = hp.enterHand(
      5,
      Diamonds,
      NotDoubled,
      North,
      Made,
      7,
      nsVul = NotVul,
      ewVul = NotVul,
      score = None,
      dealer = Some(playerW)
    )

    checkTotals(sp, 1040, 1310, 1170, 850, 1390)

    sp.clickQuit.validate
  }

  it should "show the correct result in the chicago list page" in {

    val lp = ListPage.current

    lp.checkPlayers(
      0,
      s"$playerO - 1390",
      s"$playerS - 1310",
      s"$playerE - 1170",
      s"$playerN - 1040",
      s"$playerW - 850"
    )

  }

  it should "have timestamps on all objects in the MatchChicago record" in {
    val url: URL =
      new URL(TestServer.hosturl + "v1/rest/chicagos/" + chicagoId.get)
    val connection = url.openConnection()
    val is = connection.getInputStream
    try {
      val json = Source.fromInputStream(is)(Codec.UTF8).mkString

      val converters = new BridgeServiceFileStoreConverters(true)
      import converters._

      val (id, played) = new MatchChicagoCacheStoreSupport(false).fromJSON(json)

      val created = played.created
      val updated = played.updated

      created must not be (0)
      updated must not be (0)
      created must not be (updated)

      played.rounds.foreach(r => {
        r.created must not be (0)
        r.updated must not be (0)
        r.created must not be (r.updated)
        assert(created - 100 <= r.created && r.created <= updated + 100)
        assert(created - 100 <= r.updated && r.updated <= updated + 100)
        r.hands.foreach(h => {
          h.created must not be (0)
          h.updated must not be (0)
          assert(h.created - 100 <= h.updated)
          assert(r.created - 100 <= h.created && h.created <= r.updated + 100)
          assert(r.created - 100 <= h.updated && h.updated <= r.updated + 100)
        })
      })

    } finally {
      is.close()
    }
  }
}
