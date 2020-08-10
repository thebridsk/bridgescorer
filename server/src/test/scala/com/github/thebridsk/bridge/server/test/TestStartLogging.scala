package com.github.thebridsk.bridge.server.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.thebridsk.utilities.logging.Config
import com.github.thebridsk.utilities.classpath.ClassPath
import java.util.logging.Logger
import com.github.thebridsk.utilities.logging.FileHandler
import com.github.thebridsk.utilities.logging.FileFormatter
import java.util.logging.Level
import com.github.thebridsk.utilities.logging.RedirectOutput
import com.github.thebridsk.utilities.logging

object TestStartLogging {

  val testlog: logging.Logger =
    com.github.thebridsk.utilities.logging.Logger[TestStartLogging]()

  private var loggingInitialized = false

  val logFilePrefix = "UseLogFilePrefix"
  val logFilePrefixDefault = "logs/unittest"

  def getProp(name: String, default: String): String = {
    sys.props.get(name) match {
      case Some(s) => s
      case None    => sys.env.get(name).getOrElse(default)
    }
  }

  def startLogging(logFilenamePrefix: String = null): Unit = {
    if (!loggingInitialized) {
      loggingInitialized = true
      val logfilenameprefix = Option(logFilenamePrefix).getOrElse(
        getProp(logFilePrefix, logFilePrefixDefault)
      )
      Config.configureFromResource(
        Config.getPackageNameAsResource(getClass) + "logging.properties",
        getClass.getClassLoader
      )
      // println(s"current directory for starting logging is ${(new File(".")).getAbsoluteFile.getCanonicalPath}")
      val handler = new FileHandler(s"${logfilenameprefix}.%d.%u.log")
      handler.setFormatter(new FileFormatter)
      handler.setLevel(Level.ALL)
      Logger.getLogger("").addHandler(handler)
      RedirectOutput.traceStandardOutAndErr()
      testlog.fine(ClassPath.show("    ", getClass.getClassLoader))
    }
  }
}

/**
  * Test class to start the logging system
  */
class TestStartLogging
    extends AnyFlatSpec
    with ScalatestRouteTest
    with Matchers {
  import TestStartLogging._

  behavior of "the start logging test"

  it should "start logging" in {
    startLogging()
  }
}
