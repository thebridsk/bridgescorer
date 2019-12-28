package com.github.thebridsk.session.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.openqa.selenium.WebDriver
import org.scalatest.BeforeAndAfterAll
import org.scalatest._
import org.openqa.selenium._
import org.scalatest.concurrent.Eventually
import java.util.concurrent.TimeUnit
import org.scalatest.time.Span
import org.scalatest.time.Millis
import scala.collection.convert.ImplicitConversionsToScala._
import org.scalactic.source.Position
import com.github.thebridsk.browserpages.Session

/**
 * @author werewolf
 */
class SessionTest extends AnyFlatSpec with Matchers {

  import Eventually.{ patienceConfig => _, _ }

  import scala.concurrent.duration._

  object TestSession extends Session

  val timeoutMillis = 30000
  val intervalMillis = 500

  type MyDuration = Duration
  val MyDuration = Duration
  implicit val timeoutduration = MyDuration( 60, TimeUnit.SECONDS )

  lazy val defaultPatienceConfig = PatienceConfig(timeout=scaled(Span(timeoutMillis, Millis)), interval=scaled(Span(intervalMillis,Millis)))
  implicit def patienceConfig = defaultPatienceConfig

  behavior of "Session"

  it should "create a browser" in {
    TestSession.sessionStart().setPositionRelative(0,0).setSize(1100, 900)

    TestSession.sessionStop()
  }

// End of tests

}
