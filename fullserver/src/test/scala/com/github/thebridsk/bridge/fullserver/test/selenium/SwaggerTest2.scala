package com.github.thebridsk.bridge.fullserver.test.selenium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import java.util.concurrent.TimeUnit
import org.scalatest.time.Span
import org.scalatest.time.Millis
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.server.test.util.EventuallyUtils
import com.github.thebridsk.bridge.server.test.util.HttpUtils
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.server.test.util.MonitorTCP
import com.github.thebridsk.bridge.server.test.util.ParallelUtils
import com.github.thebridsk.browserpages.PageBrowser
import com.github.thebridsk.browserpages.Session
import com.github.thebridsk.bridge.server.test.util.TestServer

/**
  * @author werewolf
  */
class SwaggerTest2 extends AnyFlatSpec with Matchers with BeforeAndAfterAll {
  import com.github.thebridsk.browserpages.PageBrowser._
  import ParallelUtils._

  val logger: Logger = Logger[SwaggerTest]()

  import Eventually.{patienceConfig => _, _}
  import EventuallyUtils._
  import HttpUtils._

  val testlog: Logger = Logger[SwaggerTest]()

  import scala.concurrent.duration._

  object TestSession extends Session

  val timeoutMillis = 30000
  val intervalMillis = 500

  val backend = TestServer.backend

  type MyDuration = Duration
  val MyDuration = Duration
  implicit val timeoutduration: FiniteDuration =
    MyDuration(60, TimeUnit.SECONDS)

  override def beforeAll(): Unit = {

    MonitorTCP.nextTest()

    TestStartLogging.startLogging()

    waitForFutures(
      "Stopping browsers and server",
      CodeBlock {
        TestSession.sessionStart().setPositionRelative(0, 0).setSize(1100, 900)
      },
      CodeBlock { TestServer.start() }
    )
  }

  override def afterAll(): Unit = {

    waitForFuturesIgnoreTimeouts(
      "Stopping browsers and server",
      CodeBlock { TestSession.sessionStop() },
      CodeBlock { TestServer.stop() }
    )
  }

  var dupid: Option[String] = None

  lazy val defaultPatienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(timeoutMillis, Millis)),
    interval = scaled(Span(intervalMillis, Millis))
  )
  implicit def patienceConfig: PatienceConfig = defaultPatienceConfig

  behavior of "Swagger test of Bridge Server"

  it should "get the swagger.yaml" in {
    tcpSleep(15)
    implicit val webDriver = TestSession.webDriver

    val ResponseFromHttp(status, headerloc, contentEncoding, resp, cd) =
      getHttp(TestServer.getUrl("/v1/api-docs/swagger.yaml"))
    val r = resp
    r must include regex """Scorekeeper for a Duplicate bridge, Chicago bridge, and Rubber bridge\."""
    r must not include ("""Function1RequestContextFutureRouteResult""")
  }

  it should "display the swagger docs going to /public/apidocs.html.gz" in {
    implicit val webDriver = TestSession.webDriver

    go to TestServer.getUrl("/public/apidocs.html.gz")
    eventually {
      val we = find(
        xpath(
          "//h2[contains(concat(' ', normalize-space(@class), ' '), ' title ')]"
        )
      )
      val text = we.text
      text must startWith("Duplicate Bridge Scorekeeper")
    }
  }

  it should "show the bridge REST API in the page from apidocs.html" in {
    implicit val webDriver = TestSession.webDriver

    eventually {
      find(
        xpath(
          "//h3[contains(concat(' ', normalize-space(@class), ' '), ' opblock-tag ')]/a/span[contains(text(), 'Duplicate')]"
        )
      )
    }
  }

// <div class="opblock-tag-section">
//   <h4 class="opblock-tag">
//     <span>Duplicate</span>
//     <small>Duplicate bridge operations</small>
//     <button class="expand-operation" title="Expand operation">
//       <svg class="arrow" width="20" height="20">
//         <use xmlns:xlink="http://www.w3.org/1999/xlink" xlink:href="#large-arrow"></use>
//       </svg>
//     </button>
//   </h4>
//   <noscript></noscript>
// </div>

  it should "allow \"duplicate\" to be selected" in {
    implicit val webDriver = TestSession.webDriver

    eventually {
      val l = find(
        xpath(
          """//h3[contains(concat(' ', @class, ' '), 'opblock-tag')]/a/span[.='Duplicate']"""
        )
      )
      l.isEnabled mustBe true
      l.isDisplayed mustBe true
      PageBrowser.scrollToElement(l)
      l.click
    }
    val li = eventually { find(id("operations-Duplicate-getBoardsets")) }
  }

  it should "allow \"get /v1/rest/boardsets\" to be tried from apidocs.html" in {
    implicit val webDriver = TestSession.webDriver

    val x = eventually { find(id("operations-Duplicate-getBoardsets")) }
    val anchor = eventually { x.find(xpath("div/button/span[2]/a")) }
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

    val method = eventually {
      val l = x.find(xpath("div[2]"))
      l.isDisplayed mustBe true
      l
    }

    val button = eventually {
      val l = method.find(xpath("//button[text()='Try it out ']"))
      l.isEnabled mustBe true
      l.isDisplayed mustBe true
      l
    }
    PageBrowser.scrollToElement(button)
    button.click

    val execute = eventually {
      val l = method.find(xpath("//button[text()='Execute']"))
      l.isEnabled mustBe true
      l.isDisplayed mustBe true
      l
    }
    PageBrowser.scrollToElement(execute)
    execute.click

    eventually {
      val l = method.find(xpath("//div[ h5[text() = 'Response body']]/div/pre"))
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
