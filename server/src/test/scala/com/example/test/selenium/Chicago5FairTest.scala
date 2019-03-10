package com.example.test.selenium

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.scalatest.time.Millis
import org.scalatest.time.Span
import com.example.data.bridge._
import org.openqa.selenium.By
import java.util.concurrent.TimeUnit
import org.scalactic.source.Position
import scala.collection.convert.ImplicitConversionsToScala._
import org.openqa.selenium.Keys
import com.example.test.util.NoResultYet
import com.example.test.util.EventuallyUtils
import org.scalatest.concurrent.Eventually
import utils.logging.Logger
import java.net.URL
import scala.io.Source
import scala.io.Codec
import org.openqa.selenium.By.ByXPath
import org.scalatest.exceptions.TestFailedException
import com.example.source.SourcePosition
import com.example.test.TestStartLogging
import com.example.test.util.MonitorTCP
import com.example.backend.BridgeServiceFileStoreConverters
import com.example.backend.MatchChicagoCacheStoreSupport
import com.example.test.pages.PageBrowser
import com.example.test.pages.bridge.HomePage

/**
 * @author werewolf
 */
class Chicago5FairTest extends FlatSpec
    with MustMatchers
    with BeforeAndAfterAll
    with EventuallyUtils
    with CancelAfterFailure
{
  import com.example.test.pages.PageBrowser._
  import Eventually.{ patienceConfig => _, _ }

  import scala.concurrent.duration._

  import ChicagoUtils._

  val log = Logger[Chicago5FairTest]

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

  val screenshotDir = "target/screenshots/Chicago5FairTest"

  TestStartLogging.startLogging()

  behavior of "Chicago test with 5 people and simple rotation of Bridge Server"

  it should "return a root page that has a title of \"The Bridge Score Keeper\"" in {
    tcpSleep(15)
    go to (TestServer.getAppPage())
    eventually { pageTitle mustBe ("The Bridge Score Keeper") }
//    takeScreenshot(screenshotDir,"title.png")
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

    find(id("Ok")) must not be 'Enabled

    eventuallyFindAndClickButton("ToggleFive")

    eventually(find(id("LabelQuintet")))

    textField("Extra").value = "Brian"
    textField("East").value = "Ellen"
    textField("South").value = "Sam"
    textField("West").value = "Wayne"
    textField("North").value = "Nancy"
    tcpSleep(1)
    pressKeys(Keys.ESCAPE)

    eventually { click on id("LabelQuintet") }

    eventually(find(id("Fair")))

    takeScreenshot(docsScreenshotDir, "EnterNames5")

    click on id("Fair")

    click on id("PlayerNFirstDealer")

    eventually( find(id("Ok")) mustBe 'Enabled )

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
            c.simpleRotation mustBe false
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

  it should "cancel the hand in the first round" in {
    click on id("Cancel")
    eventually(find(id("NextHand")))
    assertTotals("Nancy", "Sam", "Ellen", "Wayne", "Brian" )( 0, 0, 0, 0, 0 )
    click on id("NextHand")
  }

  it should "play a round" in {
    tcpSleep(30)
    val assertScore: (Int*) => Unit = assertTotals("Nancy", "Sam", "Ellen", "Wayne", "Brian" ) _
    enterHand(4,Spades,NotDoubled,North,Made,4, Some("Nancy"))  // NS score 420
    assertScore( 420, 420, 0, 0, 0 )

//    val table = find(xpath("""//div/div/div/div"""))
//    table.takeScreenshot(screenshotDir, "summarytable.png")

    InputStyleHelper.hitInputStyleButton( "Yellow" )

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

    checkPositions( "Prior hand", North, "Nancy", "Sam", "Ellen", "Wayne", "Brian" )

    eventuallyFindButtons("Player_Ellen", "Player_Sam", "Player_Wayne", "Player_Nancy")
    dontFindButtons("Player_Brian")
    eventuallyFindAndClickButton("Player_Nancy")

    checkPositions( "Next hand", East,  "Brian", "Wayne", "Ellen", "Sam", "Nancy" )

    takeScreenshot(docsScreenshotDir, "SelectNamesFair")

    eventuallyFindAndClickButton("OK")
  }

  it should "cancel playing the second round" in {
    click on id("Cancel")
    eventually(find(id("NextHand")))
    assertTotals("Nancy", "Sam", "Ellen", "Wayne", "Brian" )( 420, 420, 0, 0, 0 )
    click on id("NextHand")

    eventually{ find(id("OK")) }

    checkPositions( "Prior hand", North, "Nancy", "Sam", "Ellen", "Wayne", "Brian" )

    eventuallyFindButtons("Player_Ellen", "Player_Sam", "Player_Wayne", "Player_Nancy")
    dontFindButtons("Player_Brian" )
    eventuallyFindAndClickButton("Player_Nancy")

    checkPositions( "Next hand", East,  "Brian", "Wayne", "Ellen", "Sam", "Nancy" )

    eventuallyFindAndClickButton("OK")
  }

  it should "play another round of 4 hands" in {
    find(id("North")).text mustBe "Brian vul"
    find(id("South")).text mustBe "Wayne vul"
    find(id("East")).text mustBe "Ellen vul"
    find(id("West")).text mustBe "Sam vul"

    find(id("Dealer")).text mustBe "Ellen"

    val assertScore: (Int*)=>Unit = assertTotals("Nancy", "Sam", "Ellen", "Wayne", "Brian" ) _
    enterHand(3,NoTrump,NotDoubled,North,Made,3, Some("Ellen"))  // NS score 400
    assertScore( 420, 420, 0, 400, 400 )

    InputStyleHelper.hitInputStyleButton( "Prompt" )

    click on id("NewRound")
  }

  it should "show the partnerships and dealer for third round" in {
    tcpSleep(30)
    eventually{ find(id("OK")) }

    checkPositions( "Prior hand", East,  "Brian", "Wayne", "Ellen", "Sam", "Nancy" )

    eventuallyFindButtons("Player_Ellen", "Player_Sam", "Player_Wayne")
    dontFindButtons("Player_Brian", "Player_Nancy")
    eventuallyFindAndClickButton("Player_Ellen")

    checkPositions( "Next hand", South, "Sam", "Wayne", "Nancy", "Brian", "Ellen" )

    eventuallyFindAndClickButton("OK")
  }

  it should "play third round of 4 hands" in {
    find(id("North")).text mustBe "Sam vul"
    find(id("South")).text mustBe "Wayne vul"
    find(id("East")).text mustBe "Nancy vul"
    find(id("West")).text mustBe "Brian vul"

    find(id("Dealer")).text mustBe "Wayne"

    val assertScore: (Int*)=>Unit = assertTotals("Nancy", "Sam", "Ellen", "Wayne", "Brian" ) _
    enterHand(4,Spades,NotDoubled,North,Made,5, Some("Wayne"))  // NS score 450
    assertScore( 420, 870, 0, 850, 400 )

    InputStyleHelper.hitInputStyleButton( "Original" )

    PageBrowser.takeScreenshot(docsScreenshotDir, "SummaryQuintetPage")

    click on id("NewRound")
  }

  it should "show the partnerships and dealer for fourth round" in {
    tcpSleep(30)
    eventually{ find(id("OK")) }

    checkPositions( "Prior hand", South, "Sam", "Wayne", "Nancy", "Brian", "Ellen" )

    eventuallyFindButtons("Player_Sam", "Player_Wayne")
    dontFindButtons("Player_Brian", "Player_Ellen", "Player_Nancy")
    eventuallyFindAndClickButton("Player_Wayne")

    checkPositions( "Next hand", West,  "Nancy", "Ellen", "Sam", "Brian", "Wayne" )

    eventuallyFindAndClickButton("OK")
  }

  it should "play fourth round of 4 hands" in {
    find(id("North")).text mustBe "Nancy vul"
    find(id("South")).text mustBe "Ellen vul"
    find(id("East")).text mustBe "Sam vul"
    find(id("West")).text mustBe "Brian vul"

    find(id("Dealer")).text mustBe "Brian"

    val assertScore: (Int*)=>Unit = assertTotals("Nancy", "Sam", "Ellen", "Wayne", "Brian" ) _
    enterHand(1,NoTrump,NotDoubled,North,Made,4, Some("Brian"))  // NS score 180
    assertScore( 600, 870, 180, 850, 400 )

    click on id("NewRound")
  }

  it should "show the partnerships and dealer for fifth round" in {
    tcpSleep(30)
    eventually{ find(id("OK")) }

    checkPositions( "Prior hand", West,  "Nancy", "Ellen", "Sam", "Brian", "Wayne" )
    eventuallyFindButtons("Player_Sam" )
    dontFindButtons("Player_Brian", "Player_Ellen", "Player_Wayne", "Player_Nancy")
    checkPositions( "Next hand", North, "Brian", "Ellen", "Wayne", "Nancy", "Sam" )

    eventuallyFindAndClickButton("OK")
  }

  it should "play fifth round of 4 hands" in {
    find(id("North")).text mustBe "Brian vul"
    find(id("South")).text mustBe "Ellen vul"
    find(id("East")).text mustBe "Wayne vul"
    find(id("West")).text mustBe "Nancy vul"

    find(id("Dealer")).text mustBe "Brian"

    val assertScore: (Int*)=>Unit = assertTotals("Nancy", "Sam", "Ellen", "Wayne", "Brian" ) _
    enterHand(6,NoTrump,NotDoubled,South,Made,6, Some("Brian"))  // NS score 990
    assertScore( 600, 870, 1170, 850, 1390 )

    click on id("NewRound")
  }

  it should "show the partnerships and dealer for sixth round" in {
    tcpSleep(30)
    eventually{ find(id("OK")) }

    checkPositions( "Prior hand", North, "Brian", "Ellen", "Wayne", "Nancy", "Sam" )

    eventuallyFindButtons("Player_Brian", "Player_Ellen", "Player_Wayne", "Player_Nancy")

    dontFindButtons("Player_Sam")

    eventuallyFindAndClickButton("Player_Brian")

    checkPositions( "Next hand", East, "Sam", "Nancy", "Wayne", "Ellen", "Brian" )

    eventuallyFindAndClickButton("OK")
  }

  it should "play sixth round of 4 hands" in {
    find(id("North")).text mustBe "Sam vul"
    find(id("South")).text mustBe "Nancy vul"
    find(id("East")).text mustBe "Wayne vul"
    find(id("West")).text mustBe "Ellen vul"

    find(id("Dealer")).text mustBe "Wayne"

    val assertScore: (Int*)=>Unit = assertTotals("Nancy", "Sam", "Ellen", "Wayne", "Brian" ) _
    enterHand(5,Diamonds,NotDoubled,North,Made,7, Some("Wayne"))  // NS score 440
    assertScore( 1040, 1310, 1170, 850, 1390 )

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

      cells.get(2).getText mustBe "Brian - 1390"
      cells.get(3).getText mustBe "Sam - 1310"
      cells.get(4).getText mustBe "Ellen - 1170"
      cells.get(5).getText mustBe "Nancy - 1040"
      cells.get(6).getText mustBe "Wayne - 850"
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

  val converters = new BridgeServiceFileStoreConverters(true)
  import converters._

      val (id,played) = new MatchChicagoCacheStoreSupport(false).fromJSON(json)

      val created = played.created
      val updated = played.updated

      created must not be (0)
      updated must not be (0)
      created must not be (updated)

      played.rounds.foreach(r => {
        r.created must not be (0)
        r.updated must not be (0)
        r.created must not be (r.updated)
        assert( created-100 <= r.created && r.created <= updated+100 )
        assert( created-100 <= r.updated && r.updated <= updated+100 )
        r.hands.foreach( h=> {
          h.created must not be (0)
          h.updated must not be (0)
          assert( h.created-100 <= h.updated )
          assert( r.created-100 <= h.created && h.created <= r.updated+100 )
          assert( r.created-100 <= h.updated && h.updated <= r.updated+100 )
        })
      })

   } finally {
      is.close()
    }
  }

  def eventuallyFindButtons( ids: String* )(implicit pos: Position) = eventually {
    ids.foreach( bid => {
      withClue(s"""${pos.line} looking for button with id ${bid}""") {
        find(id(bid))
      }
    })
  }

  def dontFindButtons( ids: String* )(implicit pos: Position) = {

    val buttons = findAll( tagName("button") ).map(e => e.id.get )
    withClue(s"""${pos.line} must not have buttons with id ${ids}""") {
      ids.foreach( id => buttons must not contain id)
    }
  }
}
