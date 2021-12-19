package com.github.thebridsk.bridge.server

import scala.concurrent.duration._
import com.github.thebridsk.utilities.main.Main
import com.github.thebridsk.utilities.time.jvm.SystemTimeJVM
import scala.reflect.io.Path
import java.io.File
import org.rogach.scallop._
import com.github.thebridsk.bridge.server.version.VersionServer
import com.github.thebridsk.bridge.data.version.VersionShared
import java.net.URLClassLoader
import com.github.thebridsk.bridge.datastore.DataStoreCommands
import scala.annotation.tailrec
import com.github.thebridsk.utilities.logging.ConsoleHandler
import java.util.{logging => jul}
import com.github.thebridsk.bridge.server.util.MemoryMonitor
import com.github.thebridsk.bridge.sslkey.SSLKeyCommands
import com.github.thebridsk.utilities.main.MainConf

object ServerConf {

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

}

class ServerConf extends MainConf {
  import Server.cmdName

  import com.github.thebridsk.utilities.main.Converters._

  banner(
    s"""
       |HTTP server for scoring duplicate and chicago bridge
       |
       |Syntax:
       |  ${cmdName} options cmd cmdoptions
       |  ${cmdName} --help
       |  ${cmdName} cmd --help
       |Options:""".stripMargin
  )

  shortSubcommandsHelp(true)

  val memoryfile: ScallopOption[Path] = opt[Path](
    "memoryfile",
    noshort = true,
    descr =
      "memory monitor filename, start monitoring memory usage every 15 seconds",
    argName = "csvfilename",
    default = None
  )

  addSubcommand(StartServer)
  addSubcommand(ShutdownServer)
  addSubcommand(UpdateInstall)
  addSubcommand(Install)
  addSubcommand(CollectLogs)

  addSubcommand(DataStoreCommands)
  addSubcommand(SSLKeyCommands)

  footer(s"""
            |To get help on subcommands, use the command:
            |  ${cmdName} cmd --help
            |
            |""".stripMargin)

}

/**
  * This is the main program for the REST server for our application.
  */
object Server extends Main[ServerConf] {

  val cmdName: String = {
    ((getClass.getClassLoader match {
      case loader: URLClassLoader =>
        // This doesn't work anymore.  In Java 9 with the modules classloader, the URLClassLoader is not used as
        // the application loader.  The application loader is now an internal JDK class and can't be inspected.
        val urls = loader.getURLs
        if (urls.size != 1) {
          logger.fine("Must have only one jar file in classpath")
          None
        } else {
          val url = urls(0)
          Some(url)
        }
      case _ =>
        logger.fine("Unable to determine the jar file")
        None
    }) match {
      case Some(url) =>
        if (url.getProtocol == "file") {
          Some(new File(url.getFile).getName)
        } else {
          None
        }
      case None => None
    }) match {
      case Some(jarname) =>
        "java -jar " + jarname
      case None =>
        val x = Server.getClass.getName
        "scala " + x.substring(0, x.length() - 1)
    }
  }

  override val version: String =
    s"""BridgeScorer Server version ${VersionServer.version}
       |Build date ${VersionServer.builtAtString} UTC
       |Scala ${VersionServer.scalaVersion}, SBT ${VersionServer.sbtVersion}
       |BridgeScorer Shared version ${VersionShared.version}
       |Build date ${VersionShared.builtAtString} UTC
       |Scala ${VersionShared.scalaVersion}, SBT ${VersionShared.sbtVersion}""".stripMargin

  override def init(): Int = {
    SystemTimeJVM()
    0
  }

  import config._

  override def setup(): Int = {
    memoryfile.foreach(f => MemoryMonitor.start(f.toString()))
    0
  }

  override def cleanup(): Unit = {
    MemoryMonitor.stop()
  }

  def execute(): Int = {
    logger.severe("Unknown options specified")
    1
  }

  lazy val isConsoleLoggingToInfo: Boolean = {

    @tailrec
    def findConsoleHandler(log: jul.Logger): Boolean = {
      val handlers = log.getHandlers.filter(h =>
        h.isInstanceOf[ConsoleHandler] || h.isInstanceOf[jul.ConsoleHandler]
      )
      if (handlers.length != 0) {
        val infohandler =
          handlers.find(h => h.getLevel.intValue() <= jul.Level.INFO.intValue())
        infohandler.isDefined
      } else {
        val parent = log.getParent
        if (parent != null) findConsoleHandler(parent)
        else false
      }
    }

    findConsoleHandler(logger.logger)
  }

  def output(s: String): Unit = {
    logger.info(s)
    if (!isConsoleLoggingToInfo) println(s)
  }

}
