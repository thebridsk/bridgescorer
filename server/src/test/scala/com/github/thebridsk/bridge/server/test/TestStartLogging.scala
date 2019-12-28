package com.github.thebridsk.bridge.server.test

import org.scalatest.Finders
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.server.test.backend.BridgeServiceTesting
import com.github.thebridsk.bridge.server.service.MyService
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.headers.`Remote-Address`
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Flow
import org.scalatest._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.unmarshalling.FromResponseUnmarshaller
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.RemoteAddress.IP
import java.net.InetAddress
import com.github.thebridsk.utilities.logging.Config
import com.github.thebridsk.utilities.classpath.ClassPath
import java.util.logging.LogManager
import java.util.logging.Logger
import com.github.thebridsk.utilities.logging.FileHandler
import com.github.thebridsk.utilities.logging.FileFormatter
import java.util.logging.Level
import com.github.thebridsk.utilities.logging.RedirectOutput

object TestStartLogging {

  val testlog = com.github.thebridsk.utilities.logging.Logger[TestStartLogging]

  private var loggingInitialized = false

  val logFilePrefix = "UseLogFilePrefix"
  val logFilePrefixDefault = "logs/unittest"

  def getProp( name: String, default: String ) = {
    sys.props.get(name) match {
      case Some(s) => s
      case None => sys.env.get(name).getOrElse(default)
    }
  }

  def startLogging( logFilenamePrefix: String = null) = {
    if (!loggingInitialized) {
      loggingInitialized = true
      val logfilenameprefix = Option(logFilenamePrefix).getOrElse( getProp(logFilePrefix, logFilePrefixDefault ) )
      Config.configureFromResource(Config.getPackageNameAsResource(getClass)+"logging.properties", getClass.getClassLoader)
      val handler = new FileHandler(s"${logfilenameprefix}.%d.%u.log")
      handler.setFormatter( new FileFormatter )
      handler.setLevel(Level.ALL)
      Logger.getLogger("").addHandler(handler)
      RedirectOutput.traceStandardOutAndErr()
      testlog.fine(ClassPath.show("    ",getClass.getClassLoader))
    }
  }
}

/**
 * Test class to start the logging system
 */
class TestStartLogging extends AnyFlatSpec with ScalatestRouteTest with Matchers {
  import TestStartLogging._

  behavior of "the start logging test"

  it should "start logging" in {
    startLogging()
  }
}
