package com.example.test.selenium.integrationtest

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import org.openqa.selenium.WebDriver
import org.scalatest.selenium.Firefox
import org.scalatest.BeforeAndAfterAll
import org.scalatest._
import selenium._
import org.openqa.selenium._
import org.scalatest.concurrent.Eventually
import java.util.concurrent.TimeUnit
import com.example.Server
import scala.concurrent.Await
import com.example.data.bridge._
import com.example.backend.BridgeServiceInMemory
import com.example.backend.BridgeService
import org.scalatest.time.Span
import org.scalatest.time.Millis
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.safari.SafariDriver
import scala.collection.convert.ImplicitConversionsToScala._
import com.example.data.MatchDuplicate
import utils.logging.Logger
import java.util.logging.Level
import org.scalactic.source.Position
import com.example.test.util.NoResultYet
import com.example.test.util.EventuallyUtils
import com.example.test.util.HttpUtils
import java.net.URL
import java.io.InputStream
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.io.IOException
import java.net.HttpURLConnection
import com.example.test.TestStartLogging
import com.example.data.BoardSet
import akka.http.scaladsl.coding.GzipDecompressor
import com.example.test.pages.Element
import com.example.test.util.MonitorTCP
import com.example.test.util.ParallelUtils
import com.example.test.pages.PageBrowser
import com.example.test.selenium.Session
import com.example.test.selenium.TestServer

/**
 * @author werewolf
 */
class HelpTest extends FlatSpec with MustMatchers with BeforeAndAfterAll {
  import com.example.test.pages.PageBrowser._
  import ParallelUtils._

  val logger = Logger[HelpTest]


  import Eventually.{ patienceConfig => _, _ }
  import EventuallyUtils._
  import HttpUtils._

  import scala.concurrent.duration._

  object TestSession extends Session

  val timeoutMillis = 30000
  val intervalMillis = 500

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

  behavior of "Help test of Bridge Server"

  it should "display the help page" in {
    implicit val webDriver = TestSession.webDriver

    go to TestServer.getUrl("/")
    val help = eventually {
      val we = findElem[Element]( id("Help") )
      val text = we.text
      text mustBe "Help"
      we
    }

    click on help

    val hrefvals=List( "/introduction/", "/home/", "/duplicate/", "/chicago/" )
    val hosturl = TestServer.hosturl
    val hrefurls=hrefvals.map( v => hosturl+"help"+v )

    eventually {
      val links = findAll( xpath("""//ul[contains(concat(' ', @class, ' '), ' topics ')]/li/a"""))
      val href = links.flatMap( e => e.attribute("href") )
      href must contain theSameElementsAs hrefurls
    }

    val play = find( xpath("""//section[@id='shortcuts']/ul/li[2]/a""") )
    play.text mustBe "Play"
    click on play

    val url = TestServer.pageprod
    eventually {
      currentUrl mustBe url
    }
  }

}
