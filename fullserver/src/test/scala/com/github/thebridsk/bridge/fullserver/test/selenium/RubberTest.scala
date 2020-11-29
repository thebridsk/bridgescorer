package com.github.thebridsk.bridge.fullserver.test.selenium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.scalatest.time.Millis
import org.scalatest.time.Span
import com.github.thebridsk.bridge.data.bridge._
import java.util.concurrent.TimeUnit
import org.scalactic.source.Position
import org.openqa.selenium.Keys
import com.github.thebridsk.bridge.server.test.util.EventuallyUtils
import com.github.thebridsk.bridge.fullserver.test.util.SeleniumUtils
import com.github.thebridsk.bridge.server.test.util.NoResultYet
import org.openqa.selenium.WebDriver
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.server.test.util.MonitorTCP
import java.net.URL
import scala.io.Source
import scala.io.Codec
import com.github.thebridsk.bridge.server.test.util.HttpUtils.ResponseFromHttp
import com.github.thebridsk.bridge.server.test.util.HttpUtils
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.source.SourcePosition
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStoreConverters
import com.github.thebridsk.bridge.server.backend.MatchRubberCacheStoreSupport
import com.github.thebridsk.browserpages.PageBrowser
import com.github.thebridsk.bridge.data.MatchRubber
import play.api.libs.json.Json
import java.io.InputStream
import com.github.thebridsk.bridge.server.test.util.GraphQLUtils
import java.io.OutputStreamWriter
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import com.github.thebridsk.bridge.server.backend.BridgeResources
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage
import java.util.zip.ZipFile
import scala.reflect.io.File
import com.github.thebridsk.browserpages.Session
import com.github.thebridsk.bridge.fullserver.test.pages.LightDarkAddOn
import scala.util.Using
import scala.math.Ordering.Double.TotalOrdering
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.PageWithErrorMsg
import com.github.thebridsk.browserpages.Element
import scala.util.matching.Regex

/**
  * @author werewolf
  */
class RubberTest
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll
    with EventuallyUtils
    with SeleniumUtils
    with CancelAfterFailure {
  import com.github.thebridsk.browserpages.PageBrowser._
  import Eventually.{patienceConfig => _, _}

  import scala.concurrent.duration._

  val testlog: Logger = Logger[RubberTest]()

  val docsScreenshotDir = "target/docs/Rubber"

  val Session1 = new Session
  val SessionWatcher = new Session("Watcher")

  val timeoutMillis = 10000
  val intervalMillis = 500

  val backend = TestServer.backend

  implicit val itimeout: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(timeoutMillis, Millis)),
    interval = scaled(Span(intervalMillis, Millis))
  )

  type MyDuration = Duration
  val MyDuration = Duration

  implicit val timeoutduration: FiniteDuration =
    MyDuration(60, TimeUnit.SECONDS)

  TestStartLogging.startLogging()

  override def beforeAll(): Unit = {
    import com.github.thebridsk.bridge.server.test.util.ParallelUtils._

    MonitorTCP.nextTest()

    try {
      waitForFutures(
        "Starting browser and server",
        CodeBlock {
          Session1.sessionStart().setPositionRelative(0, 0).setSize(1300, 800)
        },
        CodeBlock { SessionWatcher.sessionStart().setQuadrant(3, 1300, 800) },
        CodeBlock { TestServer.start() }
      )
    } catch {
      case e: Throwable =>
        testlog.warning(
          "Exception starting sessions and server, stopping all",
          e
        )
        try {
          afterAll()
        } catch {
          case x: Exception =>
            e.addSuppressed(x)
            testlog.warning(
              "Exception stopping sessions and server after trying to start",
              x
            )
        }
        throw e
    }

  }

  override def afterAll(): Unit = {
    import scala.concurrent._
    import com.github.thebridsk.bridge.server.test.util.ParallelUtils._

    waitForFuturesIgnoreTimeouts(
      "Stopping browser and server",
      CodeBlock {
        try {
          Session1.sessionStop()
        } catch {
          case x: TimeoutException =>
            testlog.warning("Timeout closing sessions and server, ignoring", x)
        }
      },
      CodeBlock {
        try {
          SessionWatcher.sessionStop()
        } catch {
          case x: TimeoutException =>
            testlog.warning("Timeout closing sessions and server, ignoring", x)
        }
      },
      CodeBlock { TestServer.stop() }
    )
  }

  var rubberId = "C1" // this will be obtained dynamically
  var startingNumberOfRubbersInServer = 0

  private val urlprefix = TestServer.getAppPageUrl("rubber/")

  /**
    * @return the duplicate id of the match
    */
  def checkForRubber()(implicit webDriver: WebDriver): Option[String] = {
    val cUrl = currentUrl
    testlog.fine("currentUrl: " + cUrl)
    testlog.fine("prefix    : " + urlprefix)
    if (cUrl.startsWith(urlprefix)) {
      val idx = cUrl.substring(urlprefix.length())
      val ida = idx.split("/")
      val id = ida(0)
      if (id.startsWith("R")) Some(id)
      else None
    } else {
      None
    }
  }

  behavior of "Rubber Match test of Bridge Server"

  it should "return a root page that has a title of \"The Bridge ScoreKeeper\"" in {
    import Session1._

    tcpSleep(15)
    val home = HomePage.goto.validate
    home.clickToLightDark(LightDarkAddOn.MediumTheme)
  }

  it should "allow us to score a rubber match" in {
    import Session1._

    if (TestServer.isServerStartedByTest) {
      startingNumberOfRubbersInServer =
        backend.rubbers.syncStore.readAll() match {
          case Right(l)        => l.size
          case Left((rc, msg)) => 0
        }
    }

    findButtonAndClick("NewRubber")
    if (TestServer.isServerStartedByTest) {
      eventually(backend.rubbers.syncStore.readAll() match {
        case Right(l) => l.size mustBe startingNumberOfRubbersInServer + 1
        case Left((rc, msg)) =>
          throw new NoResultYet(rc.toString() + ": " + msg)
      })
    }

    rubberId = eventuallySome { checkForRubber() }

    testlog.info("Created rubber match " + rubberId)
  }

  it should "allow player names to be entered" in {
    import Session1._

    eventually(
      find(
        xpath("//h6[3]/span")
      ).text mustBe "Enter players and identify first dealer"
    )
    (new PageWithErrorMsg).checkErrorMsg("Please enter missing player name(s)")
  }

  val screenshotDir = "target/screenshots/RubberTest"

  it should "allow player names to be entered with suggestions when playing rubber match" in {
    import Session1._

    takeScreenshot(docsScreenshotDir, "EnterNames")

    withClueAndScreenShot(
      screenshotDir,
      "AllowNamesWithSug",
      "Enter names with suggestions"
    ) {
      eventually(find(id("ResetNames")) mustBe Symbol("Enabled"))
      find(id("Ok")) must not be Symbol("Enabled")

      textField("North").value = "Nancy"
      textField("South").value = "Sam"
      textField("East").value = "asfdfs"
      eventually {
        val visibleDivs = findAll(
          xpath(
            """//input[@name='East']/parent::div/following-sibling::div/div"""
          )
        )
        withClue("Must have one visible div") { visibleDivs.size mustBe 1 }
        val listitems = findAll(
          xpath(
            """//input[@name='East']/parent::div/following-sibling::div/div/div/ul/li"""
          )
        )
        withClue(
          "Must have one li in visible div, found " + listitems.mkString(
            ","
          ) + ": "
        ) { listitems.size mustBe 1 }
        withClue(
          "One li in visible div must show no names, found " + listitems.head.text + ": "
        ) {
          listitems.head.text must fullyMatch regex ("""No suggested names|No names matched""")
        }
      }
    }

  }

  it should "allow player names to be reset when playing rubber match" in {
    import Session1._

    withClueAndScreenShot(
      screenshotDir,
      "AllowNamesReset",
      "Names reset with suggestions"
    ) {
      PageBrowser.esc
      find(id("ResetNames")) mustBe Symbol("Enabled")
      findButtonAndClick("ResetNames")

      val fields =
        eventuallyFindAllInput("text", "North", "South", "East", "West")
      fields("North").value mustBe ""
      fields("South").value mustBe ""
      fields("East").value mustBe ""
      fields("West").value mustBe ""
    }
  }

  it should "allow player names to be entered when playing rubber match and select first dealer" in {
    import Session1._

    find(id("Ok")) must not be Symbol("Enabled")

    textField("North").value = "Nancy"
    textField("South").value = "Sam"
    textField("East").value = "Ellen"
    textField("West").value = "Ellen"
    tcpSleep(1)
    pressKeys(Keys.ESCAPE)
    tcpSleep(1)

    (new PageWithErrorMsg).checkErrorMsg("Please fix duplicate player names")

    textField("West").value = "Wayne"
    tcpSleep(1)
    pressKeys(Keys.ESCAPE)
    tcpSleep(1)

    (new PageWithErrorMsg).checkErrorMsg("Please select a dealer")

    eventually(find(id("Ok")) must not be Symbol("Enabled"))

    eventuallyFindAndClickButton("PlayerNFirstDealer")

    (new PageWithErrorMsg).checkErrorMsg("")

    eventually(find(id("Ok")) mustBe Symbol("Enabled"))

    findButtonAndClick("Ok")

    eventually(findButton("NextHand"))
  }

  it should "send the player names to the server" in {

    def testPlayers(
        north: String,
        south: String,
        east: String,
        west: String
    ) = {
      backend.rubbers.syncStore.read(MatchRubber.id(rubberId)) match {
        case Right(c) =>
          north == c.north && south == c.south && east == c.east && west == c.west
        case Left(r) => false
      }
    }

    if (TestServer.isServerStartedByTest) {
      eventuallyTrue(testPlayers("Nancy", "Sam", "Ellen", "Wayne"))
    }

  }

  it should "allow the input style to be set to original" in {
    import Session1._

    InputStyleHelper.hitInputStyleButton("Original")
  }

  it should "play first game in rubber" in {
    import Session1._

    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne") _

    findButtonAndClick("NextHand")
    verifyVul(false, false)
    enterHand(
      1,
      Spades,
      NotDoubled,
      North,
      0,
      North,
      Made,
      4,
      dealer = Some("Nancy")
    ) // NS score 120
    assertScore(220, 0) // NS partial game bonus 100
    assertRowDetails(2, "30 (90)", "")
    checkRubberTable(("Game 1", 30 :: Nil, Nil), ("Above", 90 :: Nil, Nil))

    tcpSleep(10)

    findButtonAndClick("NextHand")
    verifyVul(false, false)
    enterHand(
      1,
      Diamonds,
      NotDoubled,
      West,
      0,
      North,
      Made,
      2,
      dealer = Some("Ellen")
    ) // EW 40
    assertScore(120, 40) // no bonus
    assertRowDetails(3, "", "20 (20)")
    checkRubberTable(
      ("Game 1", 30 :: Nil, 20 :: Nil),
      ("Above", 90 :: Nil, 20 :: Nil)
    )

    tcpSleep(10)

    InputStyleHelper.hitInputStyleButton("Prompt")

    findButtonAndClick("NextHand")
    verifyVul(false, false)
    enterHand(
      1,
      Hearts,
      NotDoubled,
      North,
      100,
      West,
      Made,
      2,
      dealer = Some("Sam")
    ) // NS score 60 EW 100 honors
    assertScore(180, 140) // no bonus
    assertRowDetails(4, "30 (30)", "H100")
    checkRubberTable(
      ("Game 1", 30 :: 30 :: Nil, 20 :: Nil),
      ("Above", 30 :: 90 :: Nil, 100 :: 20 :: Nil)
    )

    tcpSleep(10)

    InputStyleHelper.hitInputStyleButton("Guide")

    findButtonAndClick("NextHand")
    verifyVul(false, false)
    takeScreenshot(docsScreenshotDir, "HandBefore")

    enterHand(
      1,
      NoTrump,
      NotDoubled,
      North,
      150,
      North,
      Made,
      4,
      dealer = Some("Wayne"),
      screenshot = Some("Hand")
    ) // NS score 130 + 150 honors
    assertScore(760, 140) // NS game bonus 300
    assertRowDetails(5, "40 (90) H150", "")
    checkRubberTable(
      ("Game 1", 40 :: 30 :: 30 :: Nil, 20 :: Nil),
      ("Above", 240 :: 30 :: 90 :: Nil, 100 :: 20 :: Nil)
    )

    tcpSleep(30)
  }

  it should "play second game in rubber" in {
    import Session1._

    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne") _

    findButtonAndClick("NextHand")
    verifyVul(true, false)
    enterHand(
      1,
      Spades,
      NotDoubled,
      East,
      0,
      North,
      Made,
      4,
      dealer = Some("Nancy")
    ) // EW score 120
    assertScore(760, 360) // NS game bonus 300, EW partial game bonus 100
    assertRowDetails(7, "", "30 (90)")
    checkRubberTable(
      ("Game 2", Nil, 30 :: Nil),
      ("Above", 240 :: 30 :: 90 :: Nil, 90 :: 100 :: 20 :: Nil),
      ("Game 1", 40 :: 30 :: 30 :: Nil, 20 :: Nil)
    )

    tcpSleep(10)

    findButtonAndClick("NextHand")
    verifyVul(true, false)
    enterHand(
      1,
      Diamonds,
      NotDoubled,
      North,
      0,
      North,
      Made,
      2,
      dealer = Some("Ellen")
    ) // NS 40
    assertScore(800, 260) // NS game bonus 300
    assertRowDetails(8, "20 (20)", "")
    checkRubberTable(
      ("Game 2", 20 :: Nil, 30 :: Nil),
      ("Above", 20 :: 240 :: 30 :: 90 :: Nil, 90 :: 100 :: 20 :: Nil),
      ("Game 1", 40 :: 30 :: 30 :: Nil, 20 :: Nil)
    )

    tcpSleep(10)

    findButtonAndClick("NextHand")
    verifyVul(true, false)
    enterHand(
      2,
      NoTrump,
      NotDoubled,
      East,
      0,
      North,
      Made,
      4,
      dealer = Some("Sam")
    ) // EW score 130
    assertScore(500, 390) // no bonus
    assertRowDetails(9, "", "70 (60)")
    checkRubberTable(
      ("Game 2", 20 :: Nil, 70 :: 30 :: Nil),
      ("Above", 20 :: 240 :: 30 :: 90 :: Nil, 60 :: 90 :: 100 :: 20 :: Nil),
      ("Game 1", 40 :: 30 :: 30 :: Nil, 20 :: Nil)
    )

    takeScreenshot(docsScreenshotDir, "SummaryPage")

    tcpSleep(30)
  }

  it should "play third game in rubber" in {
    import Session1._

    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne") _

    findButtonAndClick("NextHand")
    verifyVul(true, true)
    enterHand(
      1,
      Spades,
      NotDoubled,
      North,
      0,
      North,
      Made,
      4,
      dealer = Some("Wayne")
    ) // NS score 120
    assertScore(720, 390) // NS partial game bonus 100
    assertRowDetails(11, "30 (90)", "")
    checkRubberTable(
      ("Game 3", 30 :: Nil, Nil),
      (
        "Above",
        90 :: 20 :: 240 :: 30 :: 90 :: Nil,
        60 :: 90 :: 100 :: 20 :: Nil
      ),
      ("Game 2", 20 :: Nil, 70 :: 30 :: Nil),
      ("Game 1", 40 :: 30 :: 30 :: Nil, 20 :: Nil)
    )

    tcpSleep(10)

    findButtonAndClick("NextHand")
    verifyVul(true, true)
    enterHand(
      2,
      NoTrump,
      NotDoubled,
      North,
      0,
      North,
      Made,
      4,
      dealer = Some("Nancy")
    ) // NS score 130
    assertScore(1250, 390) // NS bonus 500
    assertRowDetails(12, "70 (60)", "")
    checkRubberTable(
      ("Game 3", 70 :: 30 :: Nil, Nil),
      (
        "Above",
        60 :: 90 :: 20 :: 240 :: 30 :: 90 :: Nil,
        60 :: 90 :: 100 :: 20 :: Nil
      ),
      ("Game 2", 20 :: Nil, 70 :: 30 :: Nil),
      ("Game 1", 40 :: 30 :: 30 :: Nil, 20 :: Nil)
    )

    tcpSleep(30)
  }

  it should "quit playing the rubber match" in {
    import Session1._

    findButtonAndClick("Quit")
  }

  it should "have timestamps on all objects in the MatchRubber record" in {

    val url: URL = new URL(TestServer.hosturl + "v1/rest/rubbers/" + rubberId)
    val connection = url.openConnection()
    val is = connection.getInputStream
    try {
      val json = Source.fromInputStream(is)(Codec.UTF8).mkString

      implicit val instanceJson =
        new BridgeServiceFileStoreConverters(true).matchRubberJson
      val (id, played) = new MatchRubberCacheStoreSupport(false).fromJSON(json)

      val created = played.created
      val updated = played.updated

      created must not be (0)
      updated must not be (0)
      created must be <= updated

      played.hands.map(h => {
        h.created must not be (0)
        h.updated must not be (0)
        h.created must be <= h.updated
        assert(created - 100 <= h.created && h.created <= updated + 100)
        assert(created - 100 <= h.updated && h.updated <= updated + 100)
      })

    } finally {
      is.close()
    }
  }

  case class ResponseData(rubber: MatchRubber)
  case class ResponseMainStore(mainStore: ResponseData)
  case class QueryResponse(data: ResponseMainStore)

  it should "have rest call and queryml call return the same match" in {

    val bridgeResources = BridgeResources(false)

    import com.github.thebridsk.bridge.data.rest.JsonSupport._
    implicit val rdFormat = Json.format[ResponseData]
    implicit val msFormat = Json.format[ResponseMainStore]
    implicit val qrFormat = Json.format[QueryResponse]

    val url: URL = new URL(TestServer.hosturl + "v1/rest/rubbers/" + rubberId)
    val connection = url.openConnection()
    val is = connection.getInputStream
    var pl: Option[MatchRubber] = None
    implicit val instanceJson =
      new BridgeServiceFileStoreConverters(true).matchDuplicateJson
    var qmlis: InputStream = null
    try {
      val json = Source.fromInputStream(is)(Codec.UTF8).mkString

      val (storegood, played) =
        bridgeResources.matchRubberCacheStoreSupport.fromJSON(json)
      pl = Some(played)

      val duplicateQML = s"""
                            |{
                            |  mainStore {
                            |    rubber( id: "$rubberId") {
                            |      id
                            |      north
                            |      south
                            |      east
                            |      west
                            |      dealerFirstHand
                            |      hands {
                            |        id
                            |        hand {
                            |          id
                            |          contractTricks
                            |          contractSuit
                            |          contractDoubled
                            |          declarer
                            |          nsVul
                            |          ewVul
                            |          madeContract
                            |          tricks
                            |          created
                            |          updated
                            |        }
                            |        honors
                            |        honorsPlayer
                            |        created
                            |        updated
                            |      }
                            |      created
                            |      updated
                            |    }
                            |  }
                            |}
                            |""".stripMargin

      val data = GraphQLUtils.queryToJson(duplicateQML)

      val qmlurl: URL = new URL(TestServer.hosturl + "v1/graphql")
      val qmlconn = qmlurl.openConnection()
      val headersForPost = Map(
        "Content-Type" -> "application/json; charset=UTF-8",
        "Accept" -> "application/json"
      )
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
      Json.fromJson[QueryResponse](Json.parse(qmljson)) match {
        case JsSuccess(qmlplayed, path) =>
          played mustBe qmlplayed.data.mainStore.rubber
        case JsError(err) =>
          fail(s"Unable to parse response from graphQL: $err")
      }
    } finally {
      is.close()
      if (qmlis != null) qmlis.close()
    }
  }

  behavior of "playing a second rubber match"

  var secondUrl: Option[String] = None

  it should "play another rubber match" in {
    import Session1._

    findButtonAndClick("New")

    eventually(
      find(
        xpath("//h6[3]/span")
      ).text mustBe "Enter players and identify first dealer"
    )

    secondUrl = Some(currentUrl)
  }

  it should "start the watcher" in {
    import SessionWatcher._

    secondUrl match {
      case Some(url) =>
        val names = "/names"
        val u =
          if (url.endsWith(names))
            url.substring(0, url.length() - names.length())
          else url
        println(s"Second URL is ${u}")
        go to u
      case None =>
        fail("Did not get the URL of the page to watch")
    }

  }

  it should "enter names in the second match" in {
    import Session1._

    eventually(findButton("Ok").isEnabled mustBe false)

    textField("North").value = " Nancy"
    textField("South").value = "Sam "
    textField("East").value = " Ellen "
    textField("West").value = "Wayne"
    tcpSleep(1)
    pressKeys(Keys.ESCAPE)
    tcpSleep(1)

    eventuallyFindAndClickButton("PlayerNFirstDealer")

    eventually(findButton("Ok").isEnabled mustBe true)

    findButtonAndClick("Ok")

    eventually(findButton("NextHand"))
  }

  it should "play the second rubber" in {
    import Session1._

    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne") _

    findButtonAndClick("NextHand")
    verifyVul(false, false)
    enterHand(
      7,
      NoTrump,
      Redoubled,
      East,
      150,
      East,
      Made,
      7,
      dealer = Some("Nancy")
    ) // EW 2130
    assertScore(0, 2430) // EW game bonus 300
    assertRowDetails(2, "", "880 (1100) H150")
    checkRubberTable(("Game 1", Nil, 880 :: Nil), ("Above", Nil, 1250 :: Nil))

    findButtonAndClick("NextHand")
    verifyVul(false, true)
    enterHand(
      6,
      NoTrump,
      Doubled,
      East,
      0,
      North,
      Made,
      6,
      dealer = Some("Ellen")
    ) // EW 1180
    assertScore(0, 4010) // EW bonus 700
    assertRowDetails(4, "", "380 (800)")
    checkRubberTable(
      ("Game 2", Nil, 380 :: Nil),
      ("Above", Nil, 800 :: 1250 :: Nil),
      ("Game 1", Nil, 880 :: Nil)
    )
  }

  it should "have the same score on the watcher" in {
    import SessionWatcher._

    val assertScore = assertTotals("Nancy", "Sam", "Ellen", "Wayne") _

    assertScore(0, 4010)
    assertRowDetails(4, "", "380 (800)")

    checkRubberTable(
      ("Game 2", Nil, 380 :: Nil),
      ("Above", Nil, 800 :: 1250 :: Nil),
      ("Game 1", Nil, 880 :: Nil)
    )

  }

  behavior of "Names resource"

  it should "show the names without leading and trailing spaces" in {

    val rnames: ResponseFromHttp[Option[Array[String]]] =
      HttpUtils.getHttpObject(new URL(TestServer.hosturl + "v1/rest/names"))

    rnames.data match {
      case Some(names) =>
        withClue(s"""the names ${names.mkString(
          "['",
          "', '",
          "']"
        )} must not contain leading or trailing spaces""") {
          names.foreach { n =>
            withClue(s"""in name "$n" """) {
              n.charAt(0) must not be ' '
              n.last must not be ' '
            }
          }
        }
      case None =>
        fail("did not get names from server")
    }
  }

  behavior of "Rubber test of entering names"

  it should "start a new rubber game" in {
    import Session1._

    go to (TestServer.getAppPage)
    pageTitle mustBe ("The Bridge ScoreKeeper")

    findButtonAndClick("NewRubber")
    if (TestServer.isServerStartedByTest) {
      eventually(backend.rubbers.syncStore.readAll() match {
        case Right(l) => l.size mustBe startingNumberOfRubbersInServer + 3
        case Left((rc, msg)) =>
          throw new NoResultYet(rc.toString() + ": " + msg)
      })
    }
    eventually(
      find(
        xpath("//h6[3]/span")
      ).text mustBe "Enter players and identify first dealer"
    )
  }

  it should "give player suggestions when entering names" in {
    import Session1._

    eventually(find(id("ResetNames")) mustBe Symbol("Enabled"))
    find(id("Ok")) must not be Symbol("Enabled")

    textField("North").value = "n"
    tcpSleep(2)
    val first = eventually {
      val listitems = findAll(
        xpath(
          """//input[@name='North']/parent::div/following-sibling::div/div/div/ul/li"""
        )
      )
      assert(!listitems.isEmpty, "list of candidate entries must not be empty")
      listitems.foreach(li => li.text must startWith regex ("(?i)n"))
      listitems.head
    }
    val text = first.text
    Thread.sleep(500)
    first.click
    eventually(textField("North").value mustBe text)

    textField("South").value = "s"
    tcpSleep(2)
    eventually {
      val listitems = findAll(
        xpath(
          """//input[@name='South']/parent::div/following-sibling::div/div/div/ul/li"""
        )
      )
      assert(!listitems.isEmpty, "list of candidate entries must not be empty")
      listitems.foreach(li => li.text must startWith regex ("(?i)s"))
    }

    textField("East").value = "asfdfs"
    eventually {
      val listitems = findAll(
        xpath(
          """//input[@name='East']/parent::div/following-sibling::div/div/div/ul/li"""
        )
      )
      assert(!listitems.isEmpty, "list of candidate entries must not be empty")
      listitems.foreach(li => li.text must startWith("No names matched"))
    }

    eventually(textField("North").value mustBe "Nancy")
    textField("South").value mustBe "s"
    textField("East").value mustBe "asfdfs"
    textField("West").value mustBe ""

  }

  it should "delete the match just created" in {
    import Session1._

    click on id("Cancel")

    eventually { find(id("Home")) }

    eventually { find(id("popup")).isDisplayed mustBe false }

    val buttons = eventually {
      findAll(
        xpath(HomePage.divBridgeAppPrefix + "//table/tbody/tr[2]/td/button")
      )
    }
    buttons.size mustBe 2
    val buttontext = buttons(0).text
    buttons(1).click

    eventually { find(id("popup")).isDisplayed mustBe true }

    click on id("PopUpCancel")

    eventually { find(id("popup")).isDisplayed mustBe false }

    val buttons2 = eventually {
      findAll(
        xpath(HomePage.divBridgeAppPrefix + "//table/tbody/tr[2]/td/button")
      )
    }
    buttons2.size mustBe 2
    buttons2(1).click

    eventually { find(id("popup")).isDisplayed mustBe true }

    click on id("PopUpOk")

    eventually { find(id("popup")).isDisplayed mustBe false }

    val buttons3 = eventually {
      findAll(
        xpath(HomePage.divBridgeAppPrefix + "//table/tbody/tr[2]/td/button")
      )
    }
    val button3text = buttons3(0).text

    buttontext must not be button3text

    takeScreenshot(docsScreenshotDir, "ListPage")

  }

  var importZipFile: Option[File] = None
  it should "export zip" in {
    import Session1._

    val hp = HomePage.goto.validate

    val ep = hp.clickExport.validate

    val f = ep.export

    importZipFile = Some(f)

    testlog.info(s"Downloaded export zip: ${f}")

    import scala.jdk.CollectionConverters._
    Using.resource(new ZipFile(f.jfile)) { zip =>
      zip.entries().asScala.map { ze => ze.getName }.toList must contain(
        s"store/MatchRubber.${rubberId}.yaml"
      )
    }

    ep.clickHome
  }

  it should "import zip" in {
    import Session1._

    val hp = HomePage.current.validate

    val ip = hp.clickImport.validate

    val initcount = ip.getImportedIds.length

    val (rp, row) =
      ip.selectFile(importZipFile.get).validateSuccess(importZipFile, initcount)

    val lp = rp.importRubber(importZipFile.get.name, row.get).validate

    lp.checkImportButton(rubberId)

    val ldpr = lp.clickImport(rubberId)

    val newId = ldpr.checkSuccessfulImport(rubberId)

    val lp2 = ldpr.clickPopUpOk.validate
    val main = lp2.clickHome.validate.clickListRubberButton.validate(newId)
  }

  def getDivByClass(clss: String): String =
    "div[contains(concat(' ', @class, ' '), ' " + clss + " ')]"

  def assertTotals(north: String, south: String, east: String, west: String)(
      nsScore: Int,
      ewScore: Int
  )(implicit webDriver: WebDriver): Assertion = {
    assertRubberTotals(north, south, east, west)(nsScore, ewScore)
    assertDetailsTotals(north, south, east, west)(nsScore, ewScore)
  }

  def assertRubberTotals(
      north: String,
      south: String,
      east: String,
      west: String
  )(nsScore: Int, ewScore: Int)(implicit webDriver: WebDriver): Assertion = {

    val header = findAll(
      xpath(
        "//" + getDivByClass("rubDivRubberMatchView") + "/table/thead/tr/th"
      )
    ).toList.drop(1)
    header.size mustBe 2

    withClue("North/South is " + north + " " + south) {
      header(0).text mustBe north + " " + south
    }
    withClue("East/West is " + east + " " + west) {
      header(1).text mustBe east + " " + west
    }

    val totals = findAll(
      xpath(
        "(//" + getDivByClass(
          "rubDivRubberMatchView"
        ) + "/table/tfoot/tr)[last()]/td"
      )
    ).toList
    totals.size mustBe 3

    withClue("NS gets " + nsScore) { totals(1).text mustBe nsScore.toString() }
    withClue("EW gets " + ewScore) { totals(2).text mustBe ewScore.toString() }

  }

  def assertDetailsTotals(
      north: String,
      south: String,
      east: String,
      west: String
  )(nsScore: Int, ewScore: Int)(implicit webDriver: WebDriver): Assertion = {

    val header = findAll(
      xpath(
        "//" + getDivByClass("rubDivDetailsView") + "/div/table/thead/tr/th"
      )
    ).toList.drop(5)
    header.size mustBe 2

    withClue("North/South is " + north + " " + south) {
      header(0).text mustBe north + " " + south
    }
    withClue("East/West is " + east + " " + west) {
      header(1).text mustBe east + " " + west
    }

    val totals = findAll(
      xpath(
        "(//" + getDivByClass(
          "rubDivDetailsView"
        ) + "/div/table/tfoot/tr)[last()]/td"
      )
    ).toList
    totals.size mustBe 3

    withClue("NS gets " + nsScore) { totals(1).text mustBe nsScore.toString() }
    withClue("EW gets " + ewScore) { totals(2).text mustBe ewScore.toString() }

  }

  def assertRowDetails(irow: Int, nsScore: String, ewScore: String)(implicit
      webDriver: WebDriver
  ): Assertion = {

    val totals = findAll(
      xpath(
        "//" + getDivByClass(
          "rubDivDetailsView"
        ) + "/div/table/tbody/tr[" + irow + "]/td"
      )
    ).toList
    totals.size mustBe 7

    withClue("NS on row " + irow + " gets " + nsScore) {
      totals(5).text mustBe nsScore
    }
    withClue("EW on row " + irow + " gets " + ewScore) {
      totals(6).text mustBe ewScore
    }

  }

  def verifyVul(nsVul: Boolean, ewVul: Boolean)(implicit
      webDriver: WebDriver
  ): Assertion = {
    eventually { find(xpath("//h6[3]/span")).text mustBe "Enter Hand" }

    val nsVulT = if (nsVul) "Vul" else "vul"
    val ewVulT = if (ewVul) "Vul" else "vul"

    getElemByXPath("""//span[@id="North"]/span""").get.text mustBe nsVulT
    getElemByXPath("""//span[@id="South"]/span""").get.text mustBe nsVulT
    getElemByXPath("""//span[@id="East"]/span""").get.text mustBe ewVulT
    getElemByXPath("""//span[@id="West"]/span""").get.text mustBe ewVulT

//    val vulButtons = findButtons("nsVul","ewVul")
//
//    val nsClass = vulButtons("nsVul").attribute("class").getOrElse("").contains("handVulnerable")
//    val ewClass = vulButtons("ewVul").attribute("class").getOrElse("").contains("handVulnerable")
//
//    nsVul mustBe nsClass
//    ewVul mustBe ewClass
  }

  def getHonorsButtons()(implicit
      webDriver: WebDriver
  ): Map[String, Element] = {
    val div = find(
      xpath("//" + getDivByClass("handViewHonors") + "/div/div[1]")
    )
    div.findAll(tagName("button")).toList.map(e => (e.id.get, e)).toMap
  }

  def checkHonorPoints(
      contractTricks: ContractTricks,
      contractSuit: ContractSuit
  )(implicit webDriver: WebDriver): Assertion = {
    val buttons = getHonorsButtons()
    contractTricks match {
      case PassedOut =>
        buttons must not contain key("Honors0")
        buttons must not contain key("Honors100")
        buttons must not contain key("Honors150")
      case _ =>
        buttons must contain key ("Honors0")
        buttons must contain key ("Honors150")
        if (contractSuit != NoTrump) buttons must contain key ("Honors100")
        else buttons must not contain key("Honors100")
    }
  }

  def checkHonorPos(contractTricks: ContractTricks, honorPoints: Int)(implicit
      webDriver: WebDriver
  ): Assertion = {
    val div = find(
      xpath("//" + getDivByClass("handViewHonors") + "/div/div[2]")
    )
    contractTricks match {
      case PassedOut =>
        div.attribute("class").get must include("baseNotVisible")
      case _ =>
        honorPoints match {
          case 0 =>
            div.attribute("class").get must include("baseNotVisible")
          case _ =>
            div.attribute("class").get must not include ("baseNotVisible")
        }
    }
  }

  def enterHand(
      contractTricks: ContractTricks,
      contractSuit: ContractSuit,
      contractDoubled: ContractDoubled,
      declarer: PlayerPosition,
      honors: Int,
      honorsPlayer: PlayerPosition,
      madeOrDown: MadeOrDown,
      tricks: Int,
      testHonorsButtons: Boolean = true,
      dealer: Option[String] = None,
      screenshot: Option[String] = None
  )(implicit webDriver: WebDriver): Unit = {
    eventually { find(xpath("//h6[3]/span")).text mustBe "Enter Hand" }

    dealer.foreach(dealerName =>
      eventually { find(id("Dealer")).text } mustBe dealerName
    )

    contractTricks match {
      case ContractTricks(0) =>
        findButtonAndClick("CTPassed")
        if (testHonorsButtons) checkHonorPoints(contractTricks, contractSuit)
      case _ =>
        findButtonAndClick("CT" + contractTricks.tricks)
        findButtonAndClick("CS" + contractSuit.suit)
        findButtonAndClick("Doubled" + contractDoubled.doubled)
        findButtonAndClick("Dec" + declarer.pos)
        if (testHonorsButtons) checkHonorPoints(contractTricks, contractSuit)
        findButtonAndClick("Honors" + honors)
        if (testHonorsButtons) checkHonorPos(contractTricks, honors)
        if (honors > 0) {
//          findButtonAndClick("HonPlay"+honorsPlayer.pos)
          click on id("HonPlay" + honorsPlayer.pos)
        }
        findButtonAndClick(madeOrDown.forScore)
        findButtonAndClick("T" + tricks)
    }
    screenshot.foreach(f => takeScreenshot(docsScreenshotDir, f))
    findButtonAndClick("Ok")
  }

  val regSplitter: Regex = """\s""".r

  case class Game(label: String, ns: List[Int] = Nil, ew: List[Int] = Nil) {

    def getInts(list: String): List[Int] = {
      regSplitter.split(list).map(s => s.toInt).toList
    }

    def add(vns: String, vew: String): Game = {
      val nsn = if (vns.length() > 0) getInts(vns) ::: ns else ns
      val ewn = if (vew.length() > 0) getInts(vew) ::: ew else ew
      copy(ns = nsn, ew = ewn)
    }
  }

  case class ListGames(list: List[Game]) {
    def add(r: List[String]): ListGames =
      r match {
        case label :: ns :: ew :: Nil =>
          copy(list = Game(label).add(ns, ew) :: list)
        case ns :: ew :: Nil => copy(list = list.head.add(ns, ew) :: list.tail)
        case _               => fail("Rubber table rows must have 2 or 3 cells")
      }

    def toMap: Map[String, Game] = list.map { g => (g.label, g) }.toMap
  }

  /**
    * Gets the rows of the rubber table, PageRubberMatchInternal.component
    */
  def getRubberTable()(implicit webDriver: WebDriver): Map[String, Game] = {
    val rows = findAll(
      xpath("//" + getDivByClass("rubDivRubberMatchView") + "/table/tbody/tr")
    )
    rows
      .map { r =>
        {
          r.findAll(tagName("td")).map { e => e.text }.toList
        }
      }
      .foldLeft(ListGames(Nil))((a, r) => a.add(r))
      .toMap
  }

  def checkRubberTable(
      checker: (String, List[Int], List[Int])*
  )(implicit pos: Position, webDriver: WebDriver): Unit = {
    checker.foreach(c => {
      val (label, ns, ew) = c
      getRubberTable().get(label) match {
        case Some(g) =>
          g.ns must contain theSameElementsAs ns
          g.ew must contain theSameElementsAs ew
        case None =>
          fail(
            "Rubber table did not have a label of " + label + " called from " + pos.line
          )
      }
    })
  }
}
