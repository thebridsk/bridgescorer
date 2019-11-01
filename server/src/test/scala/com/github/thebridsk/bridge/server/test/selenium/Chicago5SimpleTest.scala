package com.github.thebridsk.bridge.server.test.selenium

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
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
import org.openqa.selenium.By.ByXPath
import com.github.thebridsk.bridge.server.test.util.MonitorTCP
import com.github.thebridsk.bridge.server.test.util.HttpUtils.ResponseFromHttp
import com.github.thebridsk.bridge.server.test.util.HttpUtils
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStoreConverters
import com.github.thebridsk.bridge.server.backend.MatchChicagoCacheStoreSupport
import com.github.thebridsk.bridge.server.test.pages.bridge.HomePage
import com.github.thebridsk.browserpages.Session
import scala.math.Ordering.Double.TotalOrdering

/**
 * @author werewolf
 */
class Chicago5SimpleTest extends FlatSpec
    with MustMatchers
    with BeforeAndAfterAll
    with EventuallyUtils
    with CancelAfterFailure
{
  import com.github.thebridsk.browserpages.PageBrowser._
  import Eventually.{ patienceConfig => _, _ }

  import scala.concurrent.duration._

  import ChicagoUtils._

  val log = Logger[Chicago5SimpleTest]

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

  behavior of "Chicago test with 5 people and simple rotation of Bridge Server"

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

    eventually( find(id("LabelQuintet")))

    textField("Extra").value = " Brian"
    textField("North").value = " Nancy"
    textField("South").value = "Sam "
    textField("East").value = " Ellen "
    textField("West").value = "Wayne"
    tcpSleep(1)
    pressKeys(Keys.ESCAPE)
    tcpSleep(1)

    click on id("LabelQuintet")
    tcpSleep(1)

    eventually(find(id("Simple")))
    click on id("Simple")
    tcpSleep(1)

    click on id("PlayerNFirstDealer")

    eventually( find(id("Ok")) mustBe Symbol("Enabled") )

    click on id("Ok")
    eventually (find(id("North")).text mustBe "Nancy vul")
    find(id("South")).text mustBe "Sam vul"
    find(id("East")).text mustBe "Ellen vul"
    find(id("West")).text mustBe "Wayne vul"

    find(id("Dealer")).text mustBe "Nancy"

  }

  it should "send the player names to the server" in {

    def testPlayers( players: String* ) = {
        backend.chicagos.syncStore.read(chicagoId.get) match {
          case Right(c) =>
            // check if all players in MatchChicago are same as players argument
            players.zip(c.players).find( p => p._1!=p._2 ).isEmpty
          case Left(r) =>
            fail("Did not find MatchChicago record")
        }
    }

    def testSimple() = {
        backend.chicagos.syncStore.read(chicagoId.get) match {
          case Right(c) =>
            c.gamesPerRound mustBe 1
            c.simpleRotation mustBe true
          case Left(r) =>
            fail("Did not find MatchChicago record")
        }
    }

    if (TestServer.isServerStartedByTest) {
      eventually( testPlayers("Nancy","Sam","Ellen","Wayne", "Brian") mustBe true )
      testSimple()
    }

    InputStyleHelper.hitInputStyleButton( "Original" )
  }

  it should "play a round" in {
    tcpSleep(30)
    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne", "Brian" ) _
    enterHand(4,Spades,NotDoubled,North,Made,4, Some("Nancy"))  // NS score 420
    assertScore( Seq( 420, 420, 0, 0, 0 ))

    InputStyleHelper.hitInputStyleButton( "Guide" )

    click on id("NewRound")
  }

  def checkPositions( table: String, dealer: PlayerPosition,
                      north: String, south: String, east: String, west: String, extra: String ) = {

    //  <div>
    //    <h1>Last hand</h1>    or Next hand
    //    <table>

    //  //div/h1[text()="Last hand"]/following-sibling::table

    val current = findAll(xpath(s"""//div/p[text()="${table}"]/following-sibling::table/tbody/tr/td""")).toList

//    current.foreach(e => println(s"checkPosition $table ${e.text}") )

    current.size mustBe 9

    def getDealer( pos: PlayerPosition ) = if (pos == dealer) "Dealer\n" else ""

    current(0).text mustBe s"Sitting out\n$extra"
    current(2).text mustBe getDealer(South)+south
    current(4).text mustBe getDealer(East)+east
    current(5).text mustBe getDealer(West)+west
    current(7).text mustBe getDealer(North)+north

  }

  it should "show the partnerships and dealer for second round" in {
    tcpSleep(30)
    eventually{ find(id("OK")) }

    takeScreenshot(docsScreenshotDir, "SelectNamesSimple")

    checkPositions( "Prior hand", North, "Nancy", "Sam", "Ellen", "Wayne", "Brian" )
    checkPositions( "Next hand", East,  "Brian", "Sam", "Ellen", "Wayne", "Nancy" )

    eventuallyFindAndClickButton("OK")
  }

  it should "play another round of 4 hands" in {
    find(id("North")).text mustBe "Brian vul"
    find(id("South")).text mustBe "Sam vul"
    find(id("East")).text mustBe "Ellen vul"
    find(id("West")).text mustBe "Wayne vul"

    find(id("Dealer")).text mustBe "Ellen"

    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne", "Brian" ) _
    enterHand(4,Spades,NotDoubled,North,Made,4, Some("Ellen"))  // NS score 420
    assertScore( Seq( 420, 840, 0, 0, 420 ))

    InputStyleHelper.hitInputStyleButton( "Prompt" )

    click on id("NewRound")
  }

  it should "show the partnerships and dealer for third round" in {
    tcpSleep(30)
    eventually{ find(id("OK")) }

    checkPositions( "Prior hand", East,  "Brian", "Sam", "Ellen", "Wayne", "Nancy" )
    checkPositions( "Next hand", South, "Brian", "Sam", "Nancy", "Wayne", "Ellen" )

    eventuallyFindAndClickButton("OK")
  }

  it should "play third round of 4 hands" in {
    find(id("North")).text mustBe "Brian vul"
    find(id("South")).text mustBe "Sam vul"
    find(id("East")).text mustBe "Nancy vul"
    find(id("West")).text mustBe "Wayne vul"

    find(id("Dealer")).text mustBe "Sam"

    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne", "Brian" ) _
    enterHand(4,Spades,NotDoubled,North,Made,4, Some("Sam"))  // NS score 420
    assertScore( Seq( 420, 1260, 0, 0, 840 ))

    InputStyleHelper.hitInputStyleButton( "Original" )

    click on id("NewRound")
  }

  it should "show the partnerships and dealer for fourth round" in {
    tcpSleep(30)
    eventually{ find(id("OK")) }

    checkPositions( "Prior hand", South, "Brian", "Sam", "Nancy", "Wayne", "Ellen" )
    checkPositions( "Next hand", West,  "Brian", "Ellen", "Nancy", "Wayne", "Sam" )

    eventuallyFindAndClickButton("OK")
  }

  it should "play fourth round of 4 hands" in {
    find(id("North")).text mustBe "Brian vul"
    find(id("South")).text mustBe "Ellen vul"
    find(id("East")).text mustBe "Nancy vul"
    find(id("West")).text mustBe "Wayne vul"

    find(id("Dealer")).text mustBe "Wayne"

    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne", "Brian" ) _
    enterHand(4,NoTrump,NotDoubled,North,Made,4, Some("Wayne"))  // NS score 430
    assertScore( Seq( 420, 1260, 430, 0, 1270 ))

    click on id("NewRound")
  }

  it should "show the partnerships and dealer for fifth round" in {
    tcpSleep(30)
    eventually{ find(id("OK")) }

    checkPositions( "Prior hand", West,  "Brian", "Ellen", "Nancy", "Wayne", "Sam" )
    checkPositions( "Next hand", North, "Brian", "Ellen", "Nancy", "Sam", "Wayne" )

    eventuallyFindAndClickButton("OK")
  }

  it should "play fifth round of 4 hands" in {
    find(id("North")).text mustBe "Brian vul"
    find(id("South")).text mustBe "Ellen vul"
    find(id("East")).text mustBe "Nancy vul"
    find(id("West")).text mustBe "Sam vul"

    find(id("Dealer")).text mustBe "Brian"

    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne", "Brian" ) _
    enterHand(4,Diamonds,NotDoubled,East,Made,4, Some("Brian"))  // EW score 130
    assertScore( Seq( 550, 1390, 430, 0, 1270 ))

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

      cells.get(2).getText mustBe "Sam - 1390"
      cells.get(3).getText mustBe "Brian - 1270"
      cells.get(4).getText mustBe "Nancy - 550"
      cells.get(5).getText mustBe "Ellen - 430"
      cells.get(6).getText mustBe "Wayne - 0"
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

      cells.get(1).getText mustBe "Sam"
      cells.get(2).getText mustBe "1390"
      cells.get(3).getText mustBe "Brian"
      cells.get(4).getText mustBe "1270"
      cells.get(5).getText mustBe "Nancy"
      cells.get(6).getText mustBe "550"
      cells.get(7).getText mustBe "Ellen"
      cells.get(8).getText mustBe "430"
      cells.get(9).getText mustBe "Wayne"
      cells.get(10).getText mustBe "0"
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

  behavior of "Names resource"

  it should "show the names without leading and trailing spaces" in {
    import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._
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
