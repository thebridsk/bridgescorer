package com.github.thebridsk.bridgedemo.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.utilities.logging.Logger
import org.scalatest.BeforeAndAfterAll
import java.io.File
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import com.github.thebridsk.browserpages.Session
import com.github.thebridsk.bridge.server.test.util.ParallelUtils._
import org.scalatest.concurrent.Eventually.{patienceConfig => _, _}
import org.scalatest.time.Span
import org.scalatest.time.Millis
import com.github.thebridsk.bridge.server.test.util.TestServer

object TestDemo {
  val testlog: Logger = Logger[TestDemo]()
}

/**
  * Test going from the table view, by hitting a board button,
  * to the names view, to the hand view.
  * @author werewolf
  */
class TestDemo extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  val log: Logger = Logger[TestDemo]()

  TestStartLogging.startLogging()

  implicit val timeoutduration: FiniteDuration = Duration(60, TimeUnit.SECONDS)

  val timeoutMillis = 15000
  val intervalMillis = 500

  implicit val itimeout: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(timeoutMillis, Millis)),
    interval = scaled(Span(intervalMillis, Millis))
  )

  val rawdemodir =
    new File("target/demo").getAbsoluteFile.getCanonicalFile

  val Session1 = new Session

  var demoServer: Option[DemoServer] = None

  override def beforeAll(): Unit = {

    try {
      waitForFutures(
        "Starting a browser or server",
        CodeBlock {
          Session1.sessionStart().setPositionRelative(0, 0).setSize(1100, 900)
        },
        CodeBlock {
          demoServer = Some(DemoServer(rawdemodir, TestServer.testServerListen))
        }
      )
    } catch {
      case e: Throwable =>
        afterAll()
        throw e
    }

  }

  override def afterAll(): Unit = {

    waitForFuturesIgnoreTimeouts(
      "Stopping a browser or server",
      CodeBlock { Session1.sessionStop() },
      CodeBlock {
        demoServer.foreach { ds =>
          demoServer = None
          ds.stopServer()
        }
      }
    )
  }
  val rawdemodirstring = rawdemodir.toString.replace('\\', '/')
  val demourl = TestServer.testServerURL.toString()

  val isFileURL = demourl.startsWith("file://")
  def allow = !isFileURL || !Session1.isRemote

  behavior of "Demo WebSite"

  it should "show the main demo page" in {
    import Session1._
    import com.github.thebridsk.browserpages.PageBrowser._

    log.info(s"Demo URL base is ${demourl}")
    assume(!isFileURL, "Demo website can only be tested with http:// URL")

    val index = TestServer.getAppDemoPage
    val mainpage = TestServer.getAppDemoPage

    go to index

    eventually {
      val url = currentUrl
      url mustBe mainpage

      find(xpath("""//div[@id='url']/h1""")).text mustBe "Server"

      val server = find(xpath("""//div[@id='url']/ul/li""")).text
      server mustBe "Demo mode, all data entered will be lost on page refresh or closing page"
    }

  }

  it should "show the help page" in {
    import Session1._
    import com.github.thebridsk.browserpages.PageBrowser._

    log.info(s"Demo URL base is ${demourl}")
    assume(allow, "Local browser must be used with a file:// URL")

    val index = s"${demourl}help/index.html"
    val intro = s"${demourl}help/introduction.html"

    go to index

    eventually {
      val url = currentUrl
      url mustBe intro

      find(xpath(s"""//div[@id='header']/p/span""")).text must not be "Unknown"

    }

  }

}
