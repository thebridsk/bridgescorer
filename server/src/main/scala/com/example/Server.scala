package com.example

import akka.actor.{ActorSystem, Props, Actor}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps
import utils.main.Main
import java.util.logging.Level
import scala.concurrent.Future
import scala.concurrent.Await
import akka.actor.ActorRef
import akka.io.Tcp
import com.example.backend.BridgeService
import com.example.service.MyService
import com.example.backend.BridgeServiceInMemory
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import scala.util.Success
import scala.util.Failure
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.example.service.MyService
import com.example.util.SystemTimeJVM
import akka.event.Logging
import akka.http.scaladsl.ConnectionContext
import javax.net.ssl.SSLContext
import akka.http.scaladsl.HttpExt
import com.example.backend.BridgeServiceFileStore
import scala.reflect.io.Directory
import scala.reflect.io.Path
import java.io.File
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import java.security.SecureRandom
import com.example.rest.ServerPort
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
import com.example.version.VersionServer
import com.example.version.VersionShared
import java.net.URLClassLoader
import com.example.datastore.DataStoreCommands
import scala.annotation.tailrec
import java.util.logging.ConsoleHandler

/**
 * This is the main program for the REST server for our application.
 */
object Server extends Main {

  override def init() = {
    SystemTimeJVM()
    0
  }

  override def cleanup() = {}

  implicit def dateConverter: ValueConverter[Duration] = singleArgConverter[Duration](Duration(_))

  import utils.main.Converters._

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
          Some( new File(url.getFile).getName )
        } else {
          None
        }
      case None => None
    }) match {
      case Some(jarname) =>
        "java -jar "+jarname
      case None =>
        val x = Server.getClass.getName
        "scala "+x.substring(0, x.length()-1)
    }
  }

  val serverVersion = "BridgeScorer Server version "+VersionServer.version+
                    "\n  Build date "+VersionServer.builtAtString+" UTC"+
                    "\n  Scala "+VersionServer.scalaVersion+", SBT "+VersionServer.sbtVersion+
                    "\nBridgeScorer Shared version "+VersionShared.version+
                    "\n  Build date "+VersionShared.builtAtString+" UTC"+
                    "\n  Scala "+VersionShared.scalaVersion+", SBT "+VersionShared.sbtVersion
  version(serverVersion)
  banner(s"""
HTTP server for scoring duplicate and chicago bridge

Syntax:
  ${cmdName} options cmd cmdoptions
  ${cmdName} --help
  ${cmdName} cmd --help
Options:""")

  shortSubcommandsHelp(true)

  addSubcommand( StartServer )
  addSubcommand( ShutdownServer )
  addSubcommand( UpdateInstall )
  addSubcommand( Install )
//  addSubcommand( InstallCleanup )
  addSubcommand( CollectLogs )

  addSubcommand( DataStoreCommands )

  footer(s"""
To get help on subcommands, use the command:
  ${cmdName} cmd --help


""")

  def execute(): Int = {
    logger.severe("Unknown options specified")
    1
  }

  lazy val isConsoleLoggingToInfo = {

    import java.util.logging.{ Logger => JLogger }
    @tailrec
    def findConsoleHandler( log: JLogger ): Boolean = {
      val handlers = log.getHandlers.filter( h => h.isInstanceOf[ConsoleHandler] )
      if (handlers.length != 0) {
        val infohandler = handlers.find( h=> h.getLevel.intValue() <= Level.INFO.intValue() )
        infohandler.isDefined
      } else {
        val parent = log.getParent
        if (parent != null) findConsoleHandler(parent)
        else false
      }
    }

    findConsoleHandler(logger.logger)
  }

  def output( s: String ) = {
    logger.info(s)
    if (!isConsoleLoggingToInfo) println(s)
  }

}
