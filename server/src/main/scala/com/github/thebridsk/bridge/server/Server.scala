package com.github.thebridsk.bridge.server

import akka.actor.{Actor, ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps
import com.github.thebridsk.utilities.main.Main
import java.util.logging.Level
import scala.concurrent.Future
import akka.actor.ActorRef
import akka.io.Tcp
import com.github.thebridsk.bridge.server.backend.BridgeService
import com.github.thebridsk.bridge.server.service.MyService
import com.github.thebridsk.bridge.server.backend.BridgeServiceInMemory
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import scala.util.Success
import scala.util.Failure
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.github.thebridsk.bridge.server.service.MyService
import com.github.thebridsk.bridge.server.util.SystemTimeJVM
import akka.event.Logging
import akka.http.scaladsl.ConnectionContext
import javax.net.ssl.SSLContext
import akka.http.scaladsl.HttpExt
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStore
import scala.reflect.io.Directory
import scala.reflect.io.Path
import java.io.File
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import java.security.SecureRandom
import com.github.thebridsk.bridge.server.rest.ServerPort
import akka.http.scaladsl.model.StatusCodes
import java.io.FileInputStream
import org.rogach.scallop._
import scala.concurrent.Promise
import java.util.concurrent.TimeoutException
import scala.util.Try
import scala.util.Success
import java.net.InetSocketAddress
import java.net.InetAddress
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Query
import com.github.thebridsk.bridge.server.version.VersionServer
import com.github.thebridsk.bridge.data.version.VersionShared
import java.net.URLClassLoader
import com.github.thebridsk.bridge.datastore.DataStoreCommands
import scala.annotation.tailrec
import java.util.logging.ConsoleHandler
import com.github.thebridsk.bridge.server.util.MemoryMonitor

/**
  * This is the main program for the REST server for our application.
  */
object Server extends Main {

  override def init() = {
    SystemTimeJVM()
    0
  }

  override def setup() = {
    memoryfile.foreach(f => MemoryMonitor.start(f.toString()))
    0
  }

  override def cleanup() = {
    MemoryMonitor.stop()
  }

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  import com.github.thebridsk.utilities.main.Converters._

  val cmdName = {
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

  val serverVersion =
    s"""BridgeScorer Server version ${VersionServer.version}
       |Build date ${VersionServer.builtAtString} UTC
       |Scala ${VersionServer.scalaVersion}, SBT ${VersionServer.sbtVersion}
       |BridgeScorer Shared version ${VersionShared.version}
       |Build date ${VersionShared.builtAtString} UTC
       |Scala ${VersionShared.scalaVersion}, SBT ${VersionShared.sbtVersion}""".stripMargin
  version(serverVersion)
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

  val memoryfile = opt[Path](
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

  footer(s"""
To get help on subcommands, use the command:
  ${cmdName} cmd --help


""")

  def execute(): Int = {
    logger.severe("Unknown options specified")
    1
  }

  lazy val isConsoleLoggingToInfo = {

    import java.util.logging.{Logger => JLogger}
    @tailrec
    def findConsoleHandler(log: JLogger): Boolean = {
      val handlers = log.getHandlers.filter(h => h.isInstanceOf[ConsoleHandler])
      if (handlers.length != 0) {
        val infohandler =
          handlers.find(h => h.getLevel.intValue() <= Level.INFO.intValue())
        infohandler.isDefined
      } else {
        val parent = log.getParent
        if (parent != null) findConsoleHandler(parent)
        else false
      }
    }

    findConsoleHandler(logger.logger)
  }

  def output(s: String) = {
    logger.info(s)
    if (!isConsoleLoggingToInfo) println(s)
  }

}
