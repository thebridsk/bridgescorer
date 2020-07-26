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
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.server.test.util.MonitorTCP
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStoreConverters
import com.github.thebridsk.bridge.server.backend.MatchChicagoCacheStoreSupport
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage
import com.github.thebridsk.browserpages.Session
import com.github.thebridsk.bridge.server.test.util.CaptureLog
import scala.math.Ordering.Double.TotalOrdering
import com.github.thebridsk.browserpages.Element
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.EnterNamesPage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.HandPage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.SummaryPage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.FourSelectPartnersPage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.ListPage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.ChicagoMatchTypeFour

object ChicagoTest {
  val playerN = "Nancy"
  val playerS = "Sam"
  val playerE = "Ellen"
  val playerW = "Wayne"

  val allPlayers: List[String] = playerN::playerS::playerE::playerW::Nil
}

import ChicagoTest._

/**
 * @author werewolf
 */
class ChicagoTest extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll
    with EventuallyUtils
    with CancelAfterFailure
{
  import com.github.thebridsk.browserpages.PageBrowser._
  import Eventually.{ patienceConfig => _, _ }

  import scala.concurrent.duration._

  val log: Logger = Logger[ChicagoTest]()

  lazy val inTravis: Boolean = sys.props
    .get("TRAVIS_BUILD_NUMBER")
    .orElse(sys.env.get("TRAVIS_BUILD_NUMBER"))
    .isDefined

  val screenshotDir = "target/ChicagoTest"
  val docsScreenshotDir = "target/docs/Chicago"

  val Session1 = new Session

  val timeoutMillis = 15000
  val intervalMillis = 500

  val backend = TestServer.backend

  implicit val itimeout: PatienceConfig = PatienceConfig(timeout=scaled(Span(timeoutMillis, Millis)), interval=scaled(Span(intervalMillis,Millis)))

  val newChicagoButtonId = "Chicago2"
  val chicagoListURL: Option[String] = None
  val chicagoToListId: Option[String] = Some("Quit")

  val captureLog = new CaptureLog

  implicit val timeoutduration: FiniteDuration = Duration( 60, TimeUnit.SECONDS )

  TestStartLogging.startLogging()

  override
  def beforeAll(): Unit = {
    import com.github.thebridsk.bridge.server.test.util.ParallelUtils._

    if (true) captureLog.enable

    MonitorTCP.nextTest()

    try {
      waitForFutures("Starting a browser or server",
                     CodeBlock { Session1.sessionStart().setPositionRelative(0,0).setSize(1100, 900)},
                     CodeBlock { TestServer.start() } )
    } catch {
      case e: Throwable =>
        afterAll()
        throw e
    }

  }

  override
  def afterAll(): Unit = {
    import com.github.thebridsk.bridge.server.test.util.ParallelUtils._

    waitForFuturesIgnoreTimeouts("Stopping a browser or server",
                   CodeBlock { Session1.sessionStop() },
                   CodeBlock { TestServer.stop() } )

  }

  var chicagoId: Option[String] = None   // eventually this will be obtained dynamically
  var startingNumberOfChicagosInServer = 0

  import Session1._

  behavior of "Chicago test of Bridge Server"

  it should "return a root page that has a title of \"The Bridge ScoreKeeper\"" in {
    HomePage.goto.validate
  }

  it should "allow us to score a Chicago match" in {
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

  it should "allow player names to be entered with suggestions when playing Chicago" in {

    val enp = EnterNamesPage.current

    eventually( enp.isResetEnabled mustBe true )
    enp.isOKEnabled mustBe false

    enp.enterPlayer(North, playerN)
    enp.enterPlayer(South, playerS)
    enp.enterPlayer(East, "asfdfs")

    eventually {
      try {
        val east = enp.getPlayerCombobox(East)
        val sug = east.suggestions
        withClue("Must have one suggestion in visible div, found "+sug.map(_.text).mkString(",")+": ") {
          sug.length mustBe 1
        }
        withClue("One suggestion line must show no names, found "+sug(0).text+": ") {
          sug(0).text mustBe """No suggested names"""
        }
      } catch {
        case x: Exception =>
          saveDom(s"${screenshotDir}/debugDomSuggestion.html")
          throw x
      }
    }

  }

  it should "allow player names to be reset when playing Chicago" in {

    val enp = EnterNamesPage.current

    eventually( enp.isResetEnabled mustBe true )
    val enp2 = enp.clickReset

    eventually {
      enp2.getPlayer(North) mustBe ""
      enp2.getPlayer(South) mustBe ""
      enp2.getPlayer(East) mustBe ""
      enp2.getPlayer(West) mustBe ""
    }

  }

  it should "allow player names to be entered when playing Chicago" in {

    val enp = EnterNamesPage.current

    enp.isOKEnabled mustBe false

    enp.enterPlayer(North, playerN)
    enp.enterPlayer(South, playerS)
    enp.enterPlayer(East, playerE)
    enp.enterPlayer(West, playerW, hitEscapeAfter = true)

    enp.setDealer(North)

    captureLog.printLogOnException {
      log.severe("testing capture log")
      enp.isOKEnabled mustBe true

      val hp = enp.clickOK.validate

      eventually {
        hp.checkVulnerable(North, NotVul)
        hp.getName(North) mustBe playerN

        hp.getNameAndVul(South) mustBe s"$playerS vul"
        hp.getNameAndVul(East) mustBe s"$playerE vul"
        hp.getNameAndVul(West) mustBe s"$playerW vul"
      }

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
      eventually {
        testPlayers(playerN,playerS,playerE,playerW) mustBe true
      }
    }
  }

  def checkTotalScores( sp: SummaryPage, scores: Int* ): SummaryPage = {
    sp.checkTotalsScore(allPlayers,scores.map(_.toString).toList)
  }

  it should "play a round of 4 hands" in {

    val hp = HandPage.current(ChicagoMatchTypeFour)

    hp.setInputStyle("Original")

    val sp = hp.enterHand(
        4, Spades, NotDoubled, North, Made, 4,
        score = Some(s"420 $playerN-$playerS"), nsVul = NotVul, ewVul = NotVul, dealer = Some(playerN)
    ).validate
    checkTotalScores( sp, 420, 420, 0, 0 )
    val hp2 = sp.clickNextHand
    val sp2 = hp2.enterHand(
        4,Hearts,NotDoubled,East,Made,4,
        score = Some(s"620 $playerE-$playerW"), nsVul = NotVul, ewVul = Vul, dealer = Some(playerE)
    ).validate
    checkTotalScores( sp2, 420, 420, 620, 620 )
    val hp3 = sp2.clickNextHand
    val sp3 = hp3.enterHand(
        5,Diamonds,NotDoubled,South,Made,5,
        score = Some(s"600 $playerN-$playerS"), nsVul = Vul, ewVul = NotVul, dealer = Some(playerS)
    ).validate
    checkTotalScores( sp3, 1020, 1020, 620, 620 )
    val hp4 = sp3.clickNextHand
    val sp4 = hp4.enterHand(
        3,Clubs,NotDoubled,West,Made,4,
        score = Some(s"130 $playerE-$playerW"), nsVul = Vul, ewVul = Vul, dealer = Some(playerW)
    ).validate
    checkTotalScores( sp4, 1020, 1020, 750, 750 )

    sp4.setInputStyle("Guide")

    sp4.clickNewRoundFour.validate
  }

  it should "allow setting the partner and dealer for second round" in {

    val spp = FourSelectPartnersPage.current

    spp.takeScreenshot(docsScreenshotDir, "SelectNames4")

    val spp2 = spp.clickPlayer(West,2)

    eventually {
      spp2.checkPlayer(West,2,true)
      spp2.checkPlayer(South,1,true)
      spp2.checkPlayer(East,3,true)
    }

    spp2.setDealer(East).clickOK.validate

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

    hp.getName(North) mustBe playerN
    hp.getName(South) mustBe playerE
    hp.getName(East) mustBe playerS
    hp.getName(West) mustBe playerW

    val sp = hp.enterHand(
        4, Spades, NotDoubled, North, Made, 4,
        score = Some(s"420 $playerN-$playerE"), nsVul = NotVul, ewVul = NotVul, dealer = Some(playerS)
    ).validate
    checkTotalScores( sp, 1440, 1020, 1170, 750 )
    val hp2 = sp.clickNextHand
    val sp2 = hp2.enterHand(
        4,Hearts,NotDoubled,East,Made,4,
        score = Some(s"420 $playerS-$playerW"), nsVul = Vul, ewVul = NotVul, dealer = Some(playerE)
    ).validate
    checkTotalScores( sp2, 1440, 1440, 1170, 1170 )
    val hp3 = sp2.clickNextHand
    val sp3 = hp3.enterHand(
        5,Diamonds,NotDoubled,South,Made,5,
        score = Some(s"400 $playerN-$playerE"), nsVul = NotVul, ewVul = Vul, dealer = Some(playerW)
    ).validate
    checkTotalScores( sp3, 1840, 1440, 1570, 1170 )
    val hp4 = sp3.clickNextHand
    val sp4 = hp4.enterHand(
        3,Clubs,NotDoubled,West,Made,4,
        score = Some(s"130 $playerS-$playerW"), nsVul = Vul, ewVul = Vul, dealer = Some(playerN)
    ).validate
    checkTotalScores( sp4, 1840, 1570, 1570, 1300 )

    sp4.setInputStyle("Prompt")
    sp4.clickNewRoundFour.validate
  }

  it should "allow setting the scorekeeper and dealer for third round" in {

    val spp = FourSelectPartnersPage.current
    spp.isOKEnabled mustBe false

    val spp2 = spp.setDealer(West)
    eventually ( spp2.isDealer(West) mustBe true )

    spp2.clickOK.validate
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

    hp.getName(North) mustBe playerN
    hp.getName(South) mustBe playerW
    hp.getName(East) mustBe playerE
    hp.getName(West) mustBe playerS

    val sp = hp.enterHand(
        4, Spades, NotDoubled, North, Made, 4,
        score = Some(s"420 $playerN-$playerW"), nsVul = NotVul, ewVul = NotVul, dealer = Some(playerS)
    ).validate
    checkTotalScores( sp, 2260, 1570, 1570, 1720 )
    val hp2 = sp.clickNextHand
    val sp2 = hp2.enterHand(
        4,Hearts,NotDoubled,East,Made,4,
        score = Some(s"420 $playerE-$playerS"), nsVul = Vul, ewVul = NotVul, dealer = Some(playerN)
    ).validate
    checkTotalScores( sp2, 2260, 1990, 1990, 1720 )
    val hp3 = sp2.clickNextHand
    val sp3 = hp3.enterHand(
        5,Diamonds,NotDoubled,South,Made,5,
        score = Some(s"400 $playerN-$playerW"), nsVul = NotVul, ewVul = Vul, dealer = Some(playerE)
    ).validate
    checkTotalScores( sp3, 2660, 1990, 1990, 2120 )
    val hp4 = sp3.clickNextHand
    val sp4 = hp4.enterHand(
        3,Clubs,NotDoubled,West,Made,4,
        score = Some(s"130 $playerE-$playerS"), nsVul = Vul, ewVul = Vul, dealer = Some(playerW)
    ).validate
    checkTotalScores( sp4, 2660, 2120, 2120, 2120 )

    sp4.setInputStyle("Original")
    sp4.clickNewRoundFour.validate
  }


  it should "allow setting the scorekeeper, partner and dealer for fourth round" in {

    val spp = FourSelectPartnersPage.current

    spp.clickPlayer(South,1)
    spp.clickPlayer(East,3)
    spp.setDealer(South)
    spp.clickOK.validate
  }

  it should "send to the server that there are 4 games per round once more again" in {
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

  it should "play fourth round of 4 hands" in {

    val hp = HandPage.current(ChicagoMatchTypeFour)

    hp.getName(North) mustBe playerN
    hp.getName(South) mustBe playerE
    hp.getName(East) mustBe playerW
    hp.getName(West) mustBe playerS

    val sp = hp.enterHand(
        4, Spades, NotDoubled, North, Made, 4,
        score = Some(s"420 $playerN-$playerE"), nsVul = NotVul, ewVul = NotVul, dealer = Some(playerE)
    ).validate
    checkTotalScores( sp, 3080, 2120, 2540, 2120 )
    val hp2 = sp.clickNextHand
    val sp2 = hp2.enterHand(
        4,Hearts,NotDoubled,East,Made,4,
        score = Some(s"620 $playerW-$playerS"), nsVul = NotVul, ewVul = Vul, dealer = Some(playerS)
    ).validate
    checkTotalScores( sp2, 3080, 2740, 2540, 2740 )
    val hp3 = sp2.clickNextHand
    val sp3 = hp3.enterHand(
        5,Diamonds,NotDoubled,South,Made,5,
        score = Some(s"600 $playerN-$playerE"), nsVul = Vul, ewVul = NotVul, dealer = Some(playerN)
    ).validate
    checkTotalScores( sp3, 3680, 2740, 3140, 2740 )
    val hp4 = sp3.clickNextHand
    val sp4 = hp4.enterHand(
        3,Clubs,NotDoubled,West,Made,4,
        score = Some(s"130 $playerW-$playerS"), nsVul = Vul, ewVul = Vul, dealer = Some(playerW)
    ).validate
    checkTotalScores( sp4, 3680, 2870, 3140, 2870 )

    sp4.clickQuit.validate
  }

  it should "show the correct result in the chicago list page" in {

    val lp = ListPage.current

    lp.checkPlayers(0, s"$playerN - 3680", s"$playerE - 3140", s"$playerS - 2870", s"$playerW - 2870" )

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

  behavior of "Chicago test of entering names"

  it should "start a new Chicago game" in {

    val hp = HomePage.goto.validate
    val enp = hp.clickNewChicagoButton.validate

    if (TestServer.isServerStartedByTest) {
      eventually( backend.chicagos.syncStore.readAll() match {
        case Right(l) => l.size mustBe startingNumberOfChicagosInServer+2
        case Left((rc,msg)) => throw new NoResultYet( rc.toString()+": "+msg )
      })
    }
  }

  def findNorthInputList: List[Element] = findAllElems[Element]( xpath("""//input[@name='North']/parent::div/following-sibling::div/div/div/ul/li""") )

  it should "give player suggestions when entering names" in {

    val enp = EnterNamesPage.current

    withClueAndScreenShot(screenshotDir,"SuggestName","") {
      eventually {
        enp.isResetEnabled mustBe true
        enp.isOKEnabled mustBe false
      }

      enp.enterPlayer(North, "n")
      val first = eventually {
        val combobox = enp.getPlayerCombobox(North)
        val listitems = combobox.suggestions
        assert( !listitems.isEmpty, "list of candidate entries must not be empty" )
        listitems.foreach ( li =>
          li.text must startWith regex( "(?i)n" )
        )
        listitems(0)
      }
      val text = first.text
      first.click
      eventually {
        enp.getPlayer(North) mustBe text
      }

      enp.enterPlayer(South, "s")

      eventually {
        val listitems = enp.getPlayerCombobox(South).suggestions
        assert( !listitems.isEmpty, "list of candidate entries must not be empty" )
        listitems.foreach ( li =>
          li.text must startWith regex( "(?i)s" )
        )
      }

      enp.enterPlayer(East, "asfdfs")
      eventually {
        val listitems = enp.getPlayerCombobox(East).suggestions
        assert( !listitems.isEmpty, "list of candidate entries must not be empty" )
        listitems.foreach ( li =>
          li.text must startWith ( "No names matched" )
        )
      }

      enp.esc

      eventually {
        enp.getPlayer(North) mustBe text
        enp.getPlayer(South) mustBe "s"
        enp.getPlayer(East) mustBe "asfdfs"
        enp.getPlayer(West) mustBe ""
      }

    }

  }

  it should "delete the match just created" in {

    val enp = EnterNamesPage.current
    val sp = enp.clickCancel.validate
    val lp = sp.clickQuit.validate

    lp.isPopupDisplayed mustBe false

    val idToDelete = lp.getTable.row(0).id

    lp.clickDelete(0)

    eventually {
      lp.isPopupDisplayed mustBe true
    }
    lp.clickPopUpCancel

    eventually {
      lp.isPopupDisplayed mustBe false
    }

    lp.clickDelete(0)

    eventually {
      lp.isPopupDisplayed mustBe true
    }

    lp.clickPopUpOk

    eventually {
      lp.isPopupDisplayed mustBe false
      lp.getTable.row(0).id must not be idToDelete

    }

  }

}
