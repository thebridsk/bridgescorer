package com.github.thebridsk.bridge.fullserver.test.selenium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.BeforeAndAfterAll
import org.openqa.selenium._
import java.util.concurrent.TimeUnit
import org.scalatest.time.Span
import org.scalatest.time.Millis
import scala.jdk.CollectionConverters._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.server.test.util.EventuallyUtils
import org.scalatest.concurrent.Eventually
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

  val log: Logger = Logger[SeleniumPerformanceTesting]()

  import scala.concurrent.duration._

  val SessionDirector = new DirectorSession()

//  val Session1 = new Session

  val timeoutMillis = 15000
  val intervalMillis = 500

  val backend = TestServer.backend

//  case class MyDuration( timeout: Long, units: TimeUnit )
  type MyDuration = Duration
  val MyDuration = Duration

  implicit val timeoutduration: FiniteDuration = MyDuration( 60, TimeUnit.SECONDS )

  val defaultPatienceConfig: PatienceConfig = PatienceConfig(timeout=scaled(Span(timeoutMillis, Millis)), interval=scaled(Span(intervalMillis,Millis)))
  implicit def patienceConfig: PatienceConfig = defaultPatienceConfig

  override
  def beforeAll(): Unit = {
    import Session._

    MonitorTCP.nextTest()

    TestStartLogging.startLogging()

    TestServer.start()
    SessionDirector.sessionStart(getPropOrEnv("SessionDirector"))
  }

  override
  def afterAll(): Unit = {
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
