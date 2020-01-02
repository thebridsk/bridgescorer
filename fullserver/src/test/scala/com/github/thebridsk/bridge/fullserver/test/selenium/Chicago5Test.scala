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
import com.github.thebridsk.bridge.server.test.util.MonitorTCP
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStoreConverters
import com.github.thebridsk.bridge.server.backend.MatchChicagoCacheStoreSupport
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage
import com.github.thebridsk.browserpages.Session
import scala.math.Ordering.Double.TotalOrdering
import com.github.thebridsk.bridge.server.test.selenium.TestServer

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

  import ChicagoUtils._

  val log = Logger[Chicago5Test]

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
    import scala.concurrent._
    import ExecutionContext.Implicits.global
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

  behavior of "Chicago test with 5 people of Bridge Server"

  it should "return a root page that has a title of \"The Bridge Score Keeper\"" in {
    tcpSleep(15)
    go to (TestServer.getAppPage())
    pageTitle mustBe ("The Bridge Score Keeper")
  }

  it should "allow us to score a Chicago match for five people" in {
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

  it should "allow player names to be entered when playing Chicago" in {

    find(id("Ok")) must not be Symbol("Enabled")

    eventuallyFindAndClickButton("ToggleFive")

    textField("North").value = "Nancy"
    textField("South").value = "Sam"
    textField("East").value = "Ellen"
    textField("West").value = "Wayne"
    textField("Extra").value = "Brian"
    tcpSleep(1)
    pressKeys(Keys.ESCAPE)
    tcpSleep(1)

    click on id("PlayerNFirstDealer")

    eventually( find(id("Ok")) mustBe Symbol("Enabled") )

    click on id("Ok")
    eventually (find(id("North")).text mustBe "Nancy vul")
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
      eventually( testPlayers("Nancy","Sam","Ellen","Wayne", "Brian") mustBe true )
    }

    InputStyleHelper.hitInputStyleButton( "Original" )
  }

  it should "play a round of 4 hands" in {
    tcpSleep(40)
    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne", "Brian" ) _
    enterHand(4,Spades,NotDoubled,North,Made,4, Some("Nancy"))  // NS score 420
    assertScore( Seq( 420, 420, 0, 0, 0 ))
    click on id("NextHand")
    enterHand(4,Hearts,NotDoubled,East,Made,4, Some("Ellen"))  // EW score 620
    assertScore( Seq( 420, 420, 620, 620, 0 ))
    click on id("NextHand")
    enterHand(5,Diamonds,NotDoubled,South,Made,5, Some("Sam"))  // NS score 600
    assertScore( Seq( 1020, 1020, 620, 620, 0 ))
    click on id("NextHand")
    enterHand(3,Clubs,NotDoubled,West,Made,4, Some("Wayne"))  // EW score 130
    assertScore( Seq( 1020, 1020, 750, 750, 0 ))

    InputStyleHelper.hitInputStyleButton( "Guide" )

    click on id("NewRound")
  }

  it should "allow setting the partner and dealer for second round" in {
    tcpSleep(30)
    eventually{ find(id("OK")) }
    click on id("Player_Ellen")
    eventuallyFindAndClickButton("Fixture1")

    takeScreenshot(docsScreenshotDir, "SelectNames5")

    eventuallyFindAndClickButton("SwapS")

    eventuallyFindAndClickButton("DealerN")
    eventuallyFindAndClickButton("OK")
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
    find(id("South")).text mustBe "Brian vul"
    find(id("East")).text mustBe "Sam vul"
    find(id("West")).text mustBe "Wayne vul"

    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne", "Brian" ) _
    enterHand(4,Spades,NotDoubled,North,Made,4, Some("Nancy"))  // NS score 420
    assertScore( Seq( 1440, 1020, 750, 750, 420 ))
    click on id("NextHand")
    enterHand(4,Hearts,NotDoubled,East,Made,4, Some("Sam"))  // EW score 420
    assertScore( Seq( 1440, 1640, 750, 1370, 420 ))
    click on id("NextHand")
    enterHand(5,Diamonds,NotDoubled,South,Made,5, Some("Brian"))  // NS score 400
    assertScore( Seq( 2040, 1640, 750, 1370, 1020 ))
    click on id("NextHand")
    enterHand(3,Clubs,NotDoubled,West,Made,4, Some("Wayne"))  // EW score 130
    assertScore( Seq( 2040, 1770, 750, 1500, 1020 ))

    InputStyleHelper.hitInputStyleButton( "Prompt" )

    click on id("NewRound")
  }

  it should "allow setting the scorekeeper and dealer for third round" in {
    tcpSleep(30)

    eventuallyFindAndClickButton("Player_Sam")
    eventuallyFindAndClickButton("DealerE")
    eventuallyFindAndClickButton("OK")
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
    find(id("North")).text mustBe "Brian vul"
    find(id("South")).text mustBe "Ellen vul"
    find(id("East")).text mustBe "Nancy vul"
    find(id("West")).text mustBe "Wayne vul"

    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne", "Brian" ) _
    enterHand(4,Spades,NotDoubled,North,Made,4, Some("Nancy"))  // NS score 420
    assertScore( Seq( 2040, 1770, 1170, 1500, 1440 ))
    click on id("NextHand")
    enterHand(4,Hearts,NotDoubled,East,Made,4, Some("Ellen"))  // EW score 420
    assertScore( Seq( 2460, 1770, 1170, 1920, 1440 ))
    click on id("NextHand")
    enterHand(5,Diamonds,NotDoubled,South,Made,5, Some("Wayne"))  // NS score 400
    assertScore( Seq( 2460, 1770, 1570, 1920, 1840 ))
    click on id("NextHand")
    enterHand(3,Clubs,NotDoubled,West,Made,4, Some("Brian"))  // EW score 130
    assertScore( Seq( 2590, 1770, 1570, 2050, 1840 ))

    InputStyleHelper.hitInputStyleButton( "Original" )

    click on id("NewRound")
  }

  it should "allow setting the scorekeeper, partner and dealer for fourth round" in {
    tcpSleep(30)

    eventuallyFindAndClickButton("Player_Wayne")
    eventuallyFindAndClickButton("DealerN")
    eventuallyFindAndClickButton("clockwise")
    eventuallyFindAndClickButton("OK")
  }

  it should "play fourth round of 4 hands" in {
    find(id("North")).text mustBe "Nancy vul"
    find(id("South")).text mustBe "Ellen vul"
    find(id("East")).text mustBe "Brian vul"
    find(id("West")).text mustBe "Sam vul"

    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne", "Brian" ) _
    enterHand(4,Spades,NotDoubled,North,Made,4, Some("Nancy"))  // NS score 420
    assertScore( Seq( 3010, 1770, 1990, 2050, 1840 ))
    click on id("NextHand")
    enterHand(4,Hearts,NotDoubled,East,Made,4, Some("Brian"))  // EW score 620
    assertScore( Seq( 3010, 2390, 1990, 2050, 2460 ))
    click on id("NextHand")
    enterHand(5,Diamonds,NotDoubled,South,Made,5, Some("Ellen"))  // NS score 600
    assertScore( Seq( 3610, 2390, 2590, 2050, 2460 ))
    click on id("NextHand")
    enterHand(3,Clubs,NotDoubled,West,Made,4, Some("Sam"))  // EW score 130
    assertScore( Seq( 3610, 2520, 2590, 2050, 2590 ))
    click on id("NewRound")
  }

  it should "allow setting the scorekeeper, partner and dealer for fifth round" in {
    tcpSleep(30)

    eventuallyFindAndClickButton("DealerN")
    eventuallyFindAndClickButton("OK")
  }

  it should "play fifth round of 4 hands" in {
    find(id("North")).text mustBe "Brian vul"
    find(id("South")).text mustBe "Wayne vul"
    find(id("East")).text mustBe "Ellen vul"
    find(id("West")).text mustBe "Sam vul"

    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne", "Brian" ) _
    enterHand(4,Spades,NotDoubled,North,Made,4, Some("Brian"))  // NS score 420
    assertScore( Seq( 3610, 2520, 2590, 2470, 3010 ))
    click on id("NextHand")
    enterHand(4,Hearts,NotDoubled,East,Made,4, Some("Ellen"))  // EW score 620
    assertScore( Seq( 3610, 3140, 3210, 2470, 3010 ))
    click on id("NextHand")
    enterHand(5,Diamonds,NotDoubled,South,Made,5, Some("Wayne"))  // NS score 600
    assertScore( Seq( 3610, 3140, 3210, 3070, 3610 ))
    click on id("NextHand")
    enterHand(3,Clubs,NotDoubled,West,Made,4, Some("Sam"))  // EW score 130
    assertScore( Seq( 3610, 3270, 3340, 3070, 3610 ))
  }

  it should "show the correct result in the chicago list page" in {
    val nameAndScoreInSameCell = chicagoListURL match {
      case Some(url) =>
        go to (TestServer.hosturl+url)
        false
      case None =>
        chicagoToListId match {
          case Some(bid) =>
            eventuallyFindAndClickButton(bid)
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
      cells.size() mustBe 8

      cells.get(2).getText mustBe "Brian - 3610"
      cells.get(3).getText mustBe "Nancy - 3610"
      cells.get(4).getText mustBe "Ellen - 3340"
      cells.get(5).getText mustBe "Sam - 3270"
      cells.get(6).getText mustBe "Wayne - 3070"
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

      cells.get(1).getText mustBe "Brian"
      cells.get(2).getText mustBe "3610"
      cells.get(3).getText mustBe "Nancy"
      cells.get(4).getText mustBe "3610"
      cells.get(5).getText mustBe "Ellen"
      cells.get(6).getText mustBe "3340"
      cells.get(7).getText mustBe "Sam"
      cells.get(8).getText mustBe "3270"
      cells.get(9).getText mustBe "Wayne"
      cells.get(10).getText mustBe "3070"
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

}
