package com.github.thebridsk.bridge.fullserver.test.selenium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.scalatest.time.Millis
import org.scalatest.time.Span
import com.github.thebridsk.bridge.data.bridge._
import org.openqa.selenium.By
import java.util.concurrent.TimeUnit
import org.scalactic.source.Position
import scala.jdk.CollectionConverters._
import org.openqa.selenium.Keys
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
import com.github.thebridsk.browserpages.PageBrowser
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage
import com.github.thebridsk.browserpages.Session
import com.github.thebridsk.bridge.server.test.util.CaptureLog
import scala.math.Ordering.Double.TotalOrdering
import com.github.thebridsk.browserpages.Element
import com.github.thebridsk.bridge.server.test.selenium.TestServer

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

  import ChicagoUtils._

  val log = Logger[ChicagoTest]

  lazy val inTravis = sys.props
    .get("TRAVIS_BUILD_NUMBER")
    .orElse(sys.env.get("TRAVIS_BUILD_NUMBER"))
    .isDefined

  val screenshotDir = "target/ChicagoTest"
  val docsScreenshotDir = "target/docs/Chicago"

  val Session1 = new Session

  val timeoutMillis = 15000
  val intervalMillis = 500

  val backend = TestServer.backend

  implicit val itimeout = PatienceConfig(timeout=scaled(Span(timeoutMillis, Millis)), interval=scaled(Span(intervalMillis,Millis)))

  val newChicagoButtonId = "Chicago2"
  val chicagoListURL: Option[String] = None
  val chicagoToListId: Option[String] = Some("Quit")

  val captureLog = new CaptureLog

  implicit val timeoutduration = Duration( 60, TimeUnit.SECONDS )

  TestStartLogging.startLogging()

  override
  def beforeAll() = {
    import scala.concurrent._
    import ExecutionContext.Implicits.global
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
  def afterAll() = {
    import scala.concurrent._
    import ExecutionContext.Implicits.global
    import com.github.thebridsk.bridge.server.test.util.ParallelUtils._

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
    pageTitle mustBe ("The Bridge Score Keeper")
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
    chicagoId = Some(eventuallySome{ checkForChicago() })
  }

  it should "allow player names to be entered" in {
    eventually( find(xpath("//h6[3]/span")).text mustBe "Enter players and identify first dealer" )
  }

  it should "allow player names to be entered with suggestions when playing Chicago" in {

    eventually( find(id("ResetNames")) mustBe Symbol("Enabled") )
    find(id("Ok")) must not be Symbol("Enabled")

    textField("North").value = "Nancy"
    textField("South").value = "Sam"
    textField("East").value = "asfdfs"
    eventually {
      try {
        val visibleDivs = findElements(By.xpath("""//input[@name='East']/parent::div/following-sibling::div/div"""))
        withClue("Must have one visible div") { visibleDivs.size() mustBe 1}
        val listitems = findElements(By.xpath("""//input[@name='East']/parent::div/following-sibling::div/div/div/ul/li"""))
        withClue("Must have one li in visible div, found "+listitems.asScala.mkString(",")+": ") { listitems.size mustBe 1 }
        withClue("One li in visible div must show no names, found "+listitems.get(0).getText+": ") {
          listitems.get(0).getText() must fullyMatch regex ( """No suggested names""" )
        }
      } catch {
        case x: Exception =>
          saveDom("debugDomSuggestion.html")
          throw x
      }
    }

  }

  it should "allow player names to be reset when playing Chicago" in {

    find(id("ResetNames")) mustBe Symbol("Enabled")
    click on id("ResetNames")

    eventually (textField("North").value mustBe "")
    textField("South").value mustBe ""
    textField("East").value mustBe ""
    textField("West").value mustBe ""

  }

  it should "allow player names to be entered when playing Chicago" in {

    find(id("Ok")) must not be Symbol("Enabled")

    textField("North").value = "Nancy"
    textField("South").value = "Sam"
    textField("East").value = "Ellen"
    textField("West").value = "Wayne"
    tcpSleep(1)
    pressKeys(Keys.ESCAPE)
    tcpSleep(1)

    click on id("PlayerNFirstDealer")

    captureLog.printLogOnException {
      log.severe("testing capture log")
      val ok = eventually {
        val elem = find(id("Ok"))
        elem.isEnabled mustBe true
        elem
      }

      ok.click

      eventually {
        val text = find(id("North")).text
        text mustBe "Nancy vul"
      }

    }
    find(id("South")).text mustBe "Sam vul"
    find(id("East")).text mustBe "Ellen vul"
    find(id("West")).text mustBe "Wayne vul"

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
      eventually( testPlayers("Nancy","Sam","Ellen","Wayne") mustBe true )
    }

    InputStyleHelper.hitInputStyleButton( "Original" )
  }

  it should "play a round of 4 hands" in {
    tcpSleep(40)
    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne" ) _
    enterHand(4,Spades,NotDoubled,North,Made,4, Some("Nancy"))  // NS score 420
    assertScore( Seq( 420, 420, 0, 0 ))
    click on id("NextHand")
    enterHand(4,Hearts,NotDoubled,East,Made,4, Some("Ellen"))  // EW score 620
    assertScore( Seq( 420, 420, 620, 620 ))
    click on id("NextHand")
    enterHand(5,Diamonds,NotDoubled,South,Made,5, Some("Sam"))  // NS score 600
    assertScore( Seq( 1020, 1020, 620, 620 ))
    click on id("NextHand")
    enterHand(3,Clubs,NotDoubled,West,Made,4, Some("Wayne"))  // EW score 130
    assertScore( Seq( 1020, 1020, 750, 750 ))

    InputStyleHelper.hitInputStyleButton( "Guide" )

    click on id("NewRound")
  }

  it should "allow setting the partner and dealer for second round" in {
    tcpSleep(30)
    eventually { find(id("Ok")) }

    takeScreenshot(docsScreenshotDir, "SelectNames4")

    click on id("West2")

    eventually{ find(id("West2")).attribute("class").get must include ("baseButtonSelected") }
    eventually{ find(id("South1")).attribute("class").get must include ("baseButtonSelected") }
    eventually{ find(id("East3")).attribute("class").get must include ("baseButtonSelected") }

    click on id("PlayerEFirstDealer")
    click on id("Ok")
  }

  it should "send to the server that there are 4 games per round" in {

    def getGamesPerRound( ) = {
        backend.chicagos.syncStore.read(chicagoId.get) match {
          case Right(c) => c.gamesPerRound
          case Left(r) => -1
        }
    }

    if (TestServer.isServerStartedByTest) {
      eventually( getGamesPerRound mustBe 4 )
    }
  }

  it should "play another round of 4 hands" in {
    find(id("North")).text mustBe "Nancy vul"
    find(id("South")).text mustBe "Ellen vul"
    find(id("East")).text mustBe "Sam vul"
    find(id("West")).text mustBe "Wayne vul"

    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne" ) _
    enterHand(4,Spades,NotDoubled,North,Made,4, Some("Sam"))  // NS score 420
    assertScore( Seq( 1440, 1020, 1170, 750 ))
    click on id("NextHand")
    enterHand(4,Hearts,NotDoubled,East,Made,4, Some("Ellen"))  // EW score 420
    assertScore( Seq( 1440, 1440, 1170, 1170 ))
    click on id("NextHand")
    enterHand(5,Diamonds,NotDoubled,South,Made,5, Some("Wayne"))  // NS score 400
    assertScore( Seq( 1840, 1440, 1570, 1170 ))
    click on id("NextHand")
    enterHand(3,Clubs,NotDoubled,West,Made,4, Some("Nancy"))  // EW score 130
    assertScore( Seq( 1840, 1570, 1570, 1300 ))

    InputStyleHelper.hitInputStyleButton( "Prompt" )

    click on id("NewRound")
  }

  it should "allow setting the scorekeeper and dealer for third round" in {
    tcpSleep(30)

    find(id("Ok")) must not be Symbol("Enabled")

    click on id("PlayerWFirstDealer")
    click on id("Ok")
  }

  it should "send to the server that there are 4 games per round once more" in {
    // keeping this for more checks

    def getGamesPerRound( ) = {
        backend.chicagos.syncStore.read(chicagoId.get) match {
          case Right(c) => c.gamesPerRound
          case Left(r) => -1
        }
    }

    if (TestServer.isServerStartedByTest) {
      eventually( getGamesPerRound mustBe 4 )
    }
  }

  it should "play third round of 4 hands" in {
    find(id("North")).text mustBe "Nancy vul"
    find(id("South")).text mustBe "Wayne vul"
    find(id("East")).text mustBe "Ellen vul"
    find(id("West")).text mustBe "Sam vul"

    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne" ) _
    enterHand(4,Spades,NotDoubled,North,Made,4, Some("Sam"))  // NS score 420
    assertScore( Seq( 2260, 1570, 1570, 1720 ))
    click on id("NextHand")
    enterHand(4,Hearts,NotDoubled,East,Made,4, Some("Nancy"))  // EW score 420
    assertScore( Seq( 2260, 1990, 1990, 1720 ))
    click on id("NextHand")
    enterHand(5,Diamonds,NotDoubled,South,Made,5, Some("Ellen"))  // NS score 400
    assertScore( Seq( 2660, 1990, 1990, 2120 ))
    click on id("NextHand")
    enterHand(3,Clubs,NotDoubled,West,Made,4, Some("Wayne"))  // EW score 130
    assertScore( Seq( 2660, 2120, 2120, 2120 ))

    InputStyleHelper.hitInputStyleButton( "Original" )

    click on id("NewRound")
  }


  it should "allow setting the scorekeeper, partner and dealer for fourth round" in {
    tcpSleep(30)

    click on id("South1")
    click on id("East3")
    click on id("PlayerSFirstDealer")
    click on id("Ok")
  }

  it should "send to the server that there are 4 games per round once more again" in {
    // keeping this for more checks

    def getGamesPerRound( ) = {
        backend.chicagos.syncStore.read(chicagoId.get) match {
          case Right(c) => c.gamesPerRound
          case Left(r) => -1
        }
    }

    if (TestServer.isServerStartedByTest) {
      eventually( getGamesPerRound mustBe 4 )
    }
  }

  it should "play fourth round of 4 hands" in {
    find(id("North")).text mustBe "Nancy vul"
    find(id("South")).text mustBe "Ellen vul"
    find(id("East")).text mustBe "Wayne vul"
    find(id("West")).text mustBe "Sam vul"

    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne" ) _
    enterHand(4,Spades,NotDoubled,North,Made,4, Some("Ellen"))  // NS score 420
    assertScore( Seq( 3080, 2120, 2540, 2120 ))
    click on id("NextHand")
    enterHand(4,Hearts,NotDoubled,East,Made,4, Some("Sam"))  // EW score 620
    assertScore( Seq( 3080, 2740, 2540, 2740 ))
    click on id("NextHand")
    enterHand(5,Diamonds,NotDoubled,South,Made,5, Some("Nancy"))  // NS score 600
    assertScore( Seq( 3680, 2740, 3140, 2740 ))
    click on id("NextHand")
    enterHand(3,Clubs,NotDoubled,West,Made,4, Some("Wayne"))  // EW score 130
    assertScore( Seq( 3680, 2870, 3140, 2870 ))
    click on id("NewRound")
  }

  it should "show the correct result in the chicago list page" in {
    tcpSleep(30)
    val nameAndScoreInSameCell = chicagoListURL match {
      case Some(url) =>
        go to (TestServer.hosturl+url)
        false
      case None =>
        chicagoToListId match {
          case Some(bid) =>
            eventually {
              find(id("Cancel"))
            }
            click on id("Cancel")
            eventually {
              find(id(bid))
            }
            click on id(bid)
            true
          case None =>
            fail("Must specify either chicagoListURL or chicagoToListId")
        }
    }

    tcpSleep(4)


    if (nameAndScoreInSameCell) {
      val rows = eventually {
        pageTitle mustBe ("The Bridge Score Keeper")

        val rows = findElements(By.xpath(HomePage.divBridgeAppPrefix+"//table[1]/tbody/tr[1]"))
        rows.size() mustBe 1
        rows
      }
      val r1 = rows.get(0)
      val cells = r1.findElements(By.xpath("td"))
      cells.size() mustBe 7

      cells.get(2).getText mustBe "Nancy - 3680"
      cells.get(3).getText mustBe "Ellen - 3140"
      cells.get(4).getText mustBe "Sam - 2870"
      cells.get(5).getText mustBe "Wayne - 2870"
    } else {
      val rows = eventually {
        pageTitle mustBe ("The Bridge Score Keeper")

        val rows = findElements(By.xpath(HomePage.divBridgeAppPrefix+"//table[1]/tbody/tr[1]"))
        rows.size() mustBe 1
        rows
      }
      val r1 = rows.get(0)
      val cells = r1.findElements(By.xpath("td"))
      cells.size() mustBe 10

      cells.get(1).getText mustBe "Nancy"
      cells.get(2).getText mustBe "3680"
      cells.get(3).getText mustBe "Ellen"
      cells.get(4).getText mustBe "3140"
      cells.get(5).getText mustBe "Sam"
      cells.get(6).getText mustBe "2870"
      cells.get(7).getText mustBe "Wayne"
      cells.get(8).getText mustBe "2870"
    }

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
    go to (TestServer.getAppPage())
    pageTitle mustBe ("The Bridge Score Keeper")

    click on id(newChicagoButtonId)
    if (TestServer.isServerStartedByTest) {
      eventually( backend.chicagos.syncStore.readAll() match {
        case Right(l) => l.size mustBe startingNumberOfChicagosInServer+2
        case Left((rc,msg)) => throw new NoResultYet( rc.toString()+": "+msg )
      })
    }
    eventually( find(xpath("//h6[3]/span")).text mustBe "Enter players and identify first dealer" )
  }

  def findNorthInputList = findAllElems[Element]( xpath("""//input[@name='North']/parent::div/following-sibling::div/div/div/ul/li""") )

  it should "give player suggestions when entering names" in {
    withClueAndScreenShot(screenshotDir,"SuggestName","") {
      eventually( find(id("ResetNames")) mustBe Symbol("Enabled") )
      find(id("Ok")) must not be Symbol("Enabled")

      textField("North").value = "n"
      tcpSleep(2)

      val first = eventually {
        val listitems = findNorthInputList
        assert( !listitems.isEmpty, "list of candidate entries must not be empty" )
        listitems.foreach ( li =>
          li.text must startWith regex( "(?i)n" )
        )
        listitems(0)
      }
      val text = first.text
      PageBrowser.scrollToElement(first)
      Thread.sleep(10)
      findNorthInputList.headOption.map ( first => first.click ).getOrElse( fail("Did not find North input field list") )
      eventually (textField("North").value mustBe text)

      textField("South").value = "s"
      tcpSleep(2)
      eventually {
        val listitems = findElements(By.xpath("""//input[@name='South']/parent::div/following-sibling::div/div/div/ul/li"""))
        assert( !listitems.isEmpty(), "list of candidate entries must not be empty" )
        listitems.forEach ( li =>
          li.getText() must startWith regex( "(?i)s" )
        )
      }

      textField("East").value = "asfdfs"
      eventually {
        val listitems = findElements(By.xpath("""//input[@name='East']/parent::div/following-sibling::div/div/div/ul/li"""))
        assert( !listitems.isEmpty(), "list of candidate entries must not be empty" )
        listitems.forEach ( li =>
          li.getText() must startWith ( "No names matched" )
        )
      }

      esc

      eventually (textField("North").value mustBe "Nancy")
      textField("South").value mustBe "s"
      textField("East").value mustBe "asfdfs"
      textField("West").value mustBe ""

    }

  }

  it should "delete the match just created" in {
    click on id("Cancel")

    eventually { find( id("Quit") ) }

    click on id("Quit")

    eventually { find( id("Home") ) }

    eventually { find( id("popup") ).isDisplayed mustBe false }

    val buttons = eventually { findAll( xpath( HomePage.divBridgeAppPrefix+"//table/tbody/tr[1]/td/button" ) ) }
    buttons.size mustBe 2
    val buttontext = buttons(0).text
    buttons(1).click

    eventually { find( id("popup") ).isDisplayed mustBe true }

    click on id("PopUpCancel")

    eventually { find( id("popup") ).isDisplayed mustBe false }

    val buttons2 = eventually { findAll( xpath( HomePage.divBridgeAppPrefix+"//table/tbody/tr[1]/td/button" ) ) }
    buttons2.size mustBe 2
    buttons2(1).click

    eventually { find( id("popup") ).isDisplayed mustBe true }

    click on id("PopUpOk")

    eventually { find( id("popup") ).isDisplayed mustBe false }

    val buttons3 = eventually { findAll( xpath( HomePage.divBridgeAppPrefix+"//table/tbody/tr[1]/td/button" ) ) }
    val button3text = buttons3(0).text

    buttontext must not be button3text
  }

}
