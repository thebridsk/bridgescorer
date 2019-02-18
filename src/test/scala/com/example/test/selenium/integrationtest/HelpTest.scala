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
import com.example.test.pages.bridge.HomePage
import com.example.test.pages.HelpPage
import scala.annotation.tailrec
import akka.http.scaladsl.model.StatusCodes

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

    val homepage = HomePage.goto.validate
    homepage.isMoreMenuVisible mustBe false
    val hp = homepage.clickMoreMenu.validate
    eventually {
      hp.isMoreMenuVisible mustBe true
    }
    hp.findElemById("url").click
    eventually {
      hp.isMoreMenuVisible mustBe false
    }

    val gp = hp.clickHelp

    val helppage = eventually {
      val hh = webDriver.getWindowHandles.flatMap { h =>
        try {
          webDriver.switchTo().window(h)
          Some( HelpPage.current.checkPage("introduction.html") )
        } catch {
          case x: Exception =>
            None
        }
      }
      hh.size mustBe 1
      hh.head
    }
    helppage.validate.checkMainMenu
  }

  it should "display the duplicate summary page" in {
    implicit val webDriver = TestSession.webDriver

    val homepage = HomePage.goto.validate
    val gp = homepage.clickHelp

    val helppage = eventually {
      val hh = webDriver.getWindowHandles.flatMap { h =>
        try {
          webDriver.switchTo().window(h)
          Some( HelpPage.current.checkPage("introduction.html") )
        } catch {
          case x: Exception =>
            None
        }
      }
      hh.size mustBe 1
      hh.head
    }
    helppage.validate.checkMainMenu

    val duplicate = helppage.clickDuplicate.validate

    val summary = duplicate.clickMenu("duplicate/summary.html").validate

    val imageurl = TestServer.getHelpPage("images/gen/Duplicate/ListDuplicate.png")

    HelpPage.checkImage( imageurl )

    summary.findElemByXPath("//img").attribute("src") mustBe Some(imageurl)

  }

  it should "validate all anchor references and collect all image URLs" in {
    implicit val webDriver = TestSession.webDriver

    val helppage = eventually {
      HelpPage.goto("introduction.html").checkPage("introduction.html")
    }

    val helpUrl = TestServer.getHelpPage()

    def followLink( url: String ) = {
      !( url.endsWith(".png") || url.endsWith("#") )
    }

    /*
     * @param queue the queue of pages to check, tuple2( page, target )
     * @param visited the pages that have been processed already
     * @return list of tuple2( pageurl, imageurl )
     */
    @tailrec
    def visitPages(
        queue: List[(String,String)],
        visited: List[String],
        images: List[(String,String)]
    ): List[(String,String)] = {
      logger.finer( s"""visitPages:
                       |  queue: ${queue}
                       |  visited: ${visited}
                       |  images: ${images}""".stripMargin )
      if (!queue.isEmpty) {
        val (page,target) = queue.head
        if (visited.contains(target) || !target.startsWith(helpUrl) || target.endsWith(".png")) {
          logger.fine( s"Ignoring ${target} from page ${page}, already visited or not on site or image" )
          visitPages( queue.tail, visited, images )
        } else {
          logger.fine( s"Going to ${target} from page ${page}" )
          go to target
          withClue(s"From page ${page} unable to get to ${target}") {
            eventually {
              currentUrl mustBe target
              val body = findElem[Element]( xpath("//body") )
              body.attribute("data-url") mustBe 'defined
            }
          }
          val hrefs = HelpPage.gethrefs.filter( t => followLink(t) ).map( t => (target,t) )
          val imagesOnPage = HelpPage.getImages.map( i => (target,i) )
          val ihrefs = hrefs.filter { href => !visited.contains(href._2) }

          visitPages( queue.tail:::ihrefs, target::visited, imagesOnPage:::images )
        }
      } else {
        images
      }
    }

    val curUrl = currentUrl
    val visited = TestServer.hosturl+"play.html"::TestServer.hosturl+"help/play.html"::TestServer.hosturl+"help/"::Nil

    val images = visitPages( (curUrl,curUrl)::Nil, visited, Nil )

    /*
     * @param queue the queue of pages to check, tuple2( page, target )
     * @param visited the pages that have been processed already
     * @return list of tuple2( pageurl, imageurl )
     */
    @tailrec
    def visitImages(
        queue: List[(String,String)],
        visited: List[String]
    ): Unit = {
      if (!queue.isEmpty) {
        val (page,target) = queue.head
        if (visited.contains(target)) {
          visitImages( queue.tail, visited )
        } else {
          withClue(s"From page ${page} unable to get image ${target}") {
            eventually {
              HelpPage.checkImage( target )
            }
          }

          visitImages( queue.tail, target::visited )
        }
      }
    }

    visitImages( images, Nil )
  }

}
