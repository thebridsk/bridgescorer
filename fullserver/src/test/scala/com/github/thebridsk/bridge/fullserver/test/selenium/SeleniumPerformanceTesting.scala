package com.github.thebridsk.bridge.fullserver.test.selenium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest._
import org.openqa.selenium._
import org.scalatest.concurrent.Eventually
import java.util.concurrent.TimeUnit
import com.github.thebridsk.bridge.server.Server
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.server.backend.BridgeServiceInMemory
import com.github.thebridsk.bridge.server.backend.BridgeService
import org.scalatest.time.Span
import org.scalatest.time.Millis
import com.github.thebridsk.bridge.data.bridge._
import scala.jdk.CollectionConverters._
import scala.util.Failure
import scala.concurrent._
import ExecutionContext.Implicits.global
import com.github.thebridsk.utilities.logging.Logger
import java.util.logging.Level
import org.scalactic.source.Position
import com.github.thebridsk.bridge.data.util.Strings
import com.github.thebridsk.bridge.server.test.util.NoResultYet
import com.github.thebridsk.bridge.server.test.util.EventuallyUtils
import com.github.thebridsk.bridge.server.test.util.ParallelUtils
import org.scalatest.concurrent.Eventually
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ListDuplicatePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.NewDuplicatePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.MovementsPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.BoardSetsPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ScoreboardPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage.EnterNames
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TableEnterScorekeeperPage
import com.github.thebridsk.browserpages.GenericPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.HandPage
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.source.SourcePosition
import com.github.thebridsk.bridge.server.test.util.MonitorTCP
import com.github.thebridsk.browserpages.Session
import com.github.thebridsk.bridge.server.test.util.TestServer

/**
 * Test going from the table view, by hitting a board button,
 * to the names view, to the hand view.
 * @author werewolf
 */
class SeleniumPerformanceTesting extends AnyFlatSpec with Matchers with BeforeAndAfterAll with EventuallyUtils {
    import Eventually.{ patienceConfig => _, _ }
    import com.github.thebridsk.browserpages.PageBrowser._

  val log = Logger[SeleniumPerformanceTesting]

  import scala.concurrent.duration._

  val SessionDirector = new DirectorSession()

//  val Session1 = new Session

  val timeoutMillis = 15000
  val intervalMillis = 500

  val backend = TestServer.backend

//  case class MyDuration( timeout: Long, units: TimeUnit )
  type MyDuration = Duration
  val MyDuration = Duration

  implicit val timeoutduration = MyDuration( 60, TimeUnit.SECONDS )

  val defaultPatienceConfig = PatienceConfig(timeout=scaled(Span(timeoutMillis, Millis)), interval=scaled(Span(intervalMillis,Millis)))
  implicit def patienceConfig = defaultPatienceConfig

  override
  def beforeAll() = {
    import Session._

    MonitorTCP.nextTest()

    TestStartLogging.startLogging()

    TestServer.start()
    SessionDirector.sessionStart(getPropOrEnv("SessionDirector"))
  }

  override
  def afterAll() = {
    SessionDirector.sessionStop()
    TestServer.stop()
  }

  var dupid: Option[String] = None

  behavior of "Duplicate test pages of Bridge Server"

  def logBlock[T]( name: String )( block: => T )(implicit pos: SourcePosition): T = {
    val start = System.currentTimeMillis()
    def time = (System.currentTimeMillis()-start).toString+" ms"
    log.info(s"${pos.line}: Starting ${name}")
    try {
      val t = block
      log.info(s"${pos.line}: Normal return ${name}: ${time}")
      t
    } catch {
      case x: Exception =>
        log.info(s"${pos.line}: Exception return ${name}: ${time}")
        throw x
    }
  }

  it should "go to the home page" in {

    import SessionDirector._

    logBlock("Starting go to page") {
      go to (TestServer.getAppPageUrl("handduplicate"))
    }

    logBlock("eventually find Cancel"){
      eventually {
        logBlock("find Cancel") {
          find( id("Cancel") )
        }
      }
    }
  }

  it should "find all buttons using WebDriver" in {
    import SessionDirector._

    val buttons = logBlock("find all buttons") {
      findElements(By.tagName("button"))
    }

    logBlock("get text of all buttons") {
      buttons.asScala.foreach{ b =>
        logBlock("get text of a button") {
          b.getText
        }
      }
    }
  }

  it should "find all buttons using WebBrowser" in {
    import SessionDirector._

    val buttons = logBlock("find all buttons") {
      findAll(tagName("button")).toList
    }

    logBlock("get text of all buttons") {
      buttons.foreach{ b =>
        logBlock("get text of a button") {
          b.text
        }
      }
    }
  }

}
