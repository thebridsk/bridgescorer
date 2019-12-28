package com.github.thebridsk.bridge.server.test.selenium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.BeforeAndAfterAll
import org.openqa.selenium._
import org.scalatest.concurrent.Eventually
import java.util.concurrent.TimeUnit
import com.github.thebridsk.bridge.server.Server
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.server.backend.BridgeServiceInMemory
import com.github.thebridsk.bridge.server.backend.BridgeService
import org.scalatest.time.Span
import org.scalatest.time.Millis
import scala.collection.convert.ImplicitConversionsToScala._
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.utilities.logging.Logger
import java.util.logging.Level
import org.scalactic.source.Position
import com.github.thebridsk.bridge.server.test.util.NoResultYet
import com.github.thebridsk.bridge.server.test.util.EventuallyUtils
import com.github.thebridsk.bridge.server.test.util.HttpUtils
import java.net.URL
import java.io.InputStream
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.io.IOException
import java.net.HttpURLConnection
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.bridge.data.BoardSet
import akka.http.scaladsl.coding.GzipDecompressor
import com.github.thebridsk.browserpages.Element
import com.github.thebridsk.bridge.server.test.util.MonitorTCP
import com.github.thebridsk.bridge.server.test.util.ParallelUtils
import com.github.thebridsk.browserpages.PageBrowser
import com.github.thebridsk.browserpages.Session

/**
 * @author werewolf
 */
class SwaggerTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {
  import com.github.thebridsk.browserpages.PageBrowser._
  import ParallelUtils._

  val logger = Logger[SwaggerTest]


  import Eventually.{ patienceConfig => _, _ }
  import EventuallyUtils._
  import HttpUtils._

  val testlog = Logger[SwaggerTest]

  import scala.concurrent.duration._

  object TestSession extends Session

  val timeoutMillis = 30000
  val intervalMillis = 500

  val backend = TestServer.backend

  type MyDuration = Duration
  val MyDuration = Duration
  implicit val timeoutduration = MyDuration( 60, TimeUnit.SECONDS )

  override
  def beforeAll() = {
    import scala.concurrent._
    import ExecutionContext.Implicits.global

    MonitorTCP.nextTest()

    TestStartLogging.startLogging()

    waitForFutures( "Stopping browsers and server",
                    CodeBlock { TestSession.sessionStart().setPositionRelative(0,0).setSize(1100, 900)},
                    CodeBlock { TestServer.start() }
                  )
  }

  override
  def afterAll() = {
    import scala.concurrent._
    import ExecutionContext.Implicits.global

    waitForFuturesIgnoreTimeouts( "Stopping browsers and server",
                CodeBlock { TestSession.sessionStop() },
                CodeBlock { TestServer.stop() }
               )
  }

  var dupid: Option[String] = None

  lazy val defaultPatienceConfig = PatienceConfig(timeout=scaled(Span(timeoutMillis, Millis)), interval=scaled(Span(intervalMillis,Millis)))
  implicit def patienceConfig = defaultPatienceConfig

  behavior of "Swagger test of Bridge Server"

  it should "get the swagger.yaml" in {
    tcpSleep(15)
    implicit val webDriver = TestSession.webDriver

    val ResponseFromHttp(status,headerloc,contentEncoding,resp,cd) = getHttp( TestServer.getUrl("/v1/api-docs/swagger.yaml") )
    val r = resp
    r must include regex """Scorekeeper for a Duplicate bridge, Chicago bridge, and Rubber bridge\."""
    r must not include ("""Function1RequestContextFutureRouteResult""")
  }

  it should "try to get swagger docs page" in {
    implicit val webDriver = TestSession.webDriver

    val ResponseFromHttp(status,headerloc,contentEncoding,resp,cd) = getHttp( TestServer.getUrl("/v1/docs/") )
    status mustBe 308
    headerloc mustBe Some("/public/swagger-ui-dist/index.html.gz?url=/v1/api-docs/swagger.yaml&validatorUrl=")
//    resp must include regex """\<html\>"""
  }

  it should "get the swagger docs page" in {
    implicit val webDriver = TestSession.webDriver

    val ResponseFromHttp(status,headerloc,contentEncoding,resp,cd) = getHttpAll( TestServer.getUrl("/public/swagger-ui-dist/index.html") )
    status mustBe 200
    resp must include regex """<html[ >]"""
  }

  it should "get the swagger apidocs page" in {
    implicit val webDriver = TestSession.webDriver

    val ResponseFromHttp(status,headerloc,contentEncoding,resp,cd) = getHttpAll( TestServer.getUrl("/public/apidocs.html") )
    status mustBe 200
    resp must include regex """<html[ >]"""
  }

  it should "display the swagger docs going to /v1/docs/" in {
    implicit val webDriver = TestSession.webDriver

    go to TestServer.getDocs()
    eventually {
      val we = find(xpath("//h2[contains(concat(' ', normalize-space(@class), ' '), ' title ')]"))
      val text = we.text
      text must startWith ( "Duplicate Bridge Scorekeeper" )
    }
  }

  it should "display the swagger docs going to /public/swagger-ui-dist/index.html.gz" in {
    implicit val webDriver = TestSession.webDriver

    go to TestServer.getUrl("/public/swagger-ui-dist/index.html.gz?url=/v1/api-docs/swagger.yaml&validatorUrl=")
    eventually {
      val we = find(xpath("//h2[contains(concat(' ', normalize-space(@class), ' '), ' title ')]"))
      val text = we.text
      text must startWith ( "Duplicate Bridge Scorekeeper" )
    }
  }

  it should "show the bridge REST API in the page" in {
    implicit val webDriver = TestSession.webDriver

    eventually{ find(xpath("//h4[contains(concat(' ', normalize-space(@class), ' '), ' opblock-tag ')]/a/span[contains(text(), 'Duplicate')]")) }
  }

  it should "allow \"get /v1/rest/boardsets\" to be tried" in {
    implicit val webDriver = TestSession.webDriver

    val x = eventually { find(id("operations-Duplicate-getBoardsets")) }
    val anchor = eventually { x.find(xpath("div/span[2]/a")) }
    anchor.isDisplayed mustBe true
    anchor.text mustBe "/rest/boardsets"
//    val anchor = eventually{
//      val l = x.find(xpath("div/span[2]/span"))
//      l.isDisplayed mustBe true
//      l.text mustBe "/rest/boardsets"
//      l
//    }
    PageBrowser.scrollToElement(anchor)
    anchor.click

    val method = eventually{
      val l = x.find(xpath( "div[2]" ))
      l.isDisplayed mustBe true
      l
    }

    val button = eventually{
      val l = method.find(xpath( "//button[text()='Try it out ']" ))
      l.isEnabled mustBe true
      l.isDisplayed mustBe true
      l
    }
    PageBrowser.scrollToElement(button)
    button.click

    val execute = eventually{
      val l = method.find(xpath( "//button[text()='Execute']" ))
      l.isEnabled mustBe true
      l.isDisplayed mustBe true
      l
    }
    PageBrowser.scrollToElement(execute)
    execute.click

    eventually {
      val l = method.find(xpath("//div[ h5[text() = 'Response body']]/div/pre" ))
      val text = l.text
      text must startWith("[")

      import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._

      val sets = readJson[List[BoardSet]](text)
      sets.size mustBe 2

    }

    tcpSleep(5)
  }

// End of tests

}
