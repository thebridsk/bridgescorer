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

  val Session1 = new Session

  override def beforeAll(): Unit = {

    try {
      waitForFutures(
        "Starting a browser or server",
        CodeBlock {
          Session1.sessionStart().setPositionRelative(0, 0).setSize(1100, 900)
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
      CodeBlock { Session1.sessionStop() }
    )
  }

  behavior of "Demo WebSite"

  it should "test the generated demo website" in {
    import Session1._
    import com.github.thebridsk.browserpages.PageBrowser._

    assume(!isRemote, "Demo website can only be tested with file:// URL, requires local browser")

    val rawdemodir =
      new File("target/demo").getAbsoluteFile.getCanonicalFile.toString
        .replace('\\', '/')
    val demodir =
      if (rawdemodir.charAt(0) == '/') rawdemodir else s"/$rawdemodir"
    log.info(s"Demo directory is ${demodir}")

    val index = s"file://$demodir/help/index.html"
    val intro = s"file://$demodir/help/introduction.html"

    go to index

    eventually {
      val url = currentUrl
      url mustBe intro

      find(xpath(s"""//div[@id='header']/p/span""")).text must not be "Unknown"

    }

  }

}
