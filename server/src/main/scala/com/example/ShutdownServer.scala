package com.github.thebridsk.bridge

import akka.actor.{Actor, ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps
import com.github.thebridsk.utilities.main.Main
import java.util.logging.Level
import scala.concurrent.Future
import scala.concurrent.Await
import akka.actor.ActorRef
import akka.io.Tcp
import com.github.thebridsk.bridge.backend.BridgeService
import com.github.thebridsk.bridge.service.MyService
import com.github.thebridsk.bridge.backend.BridgeServiceInMemory
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
import com.github.thebridsk.bridge.service.MyService
import com.github.thebridsk.bridge.util.SystemTimeJVM
import akka.event.Logging
import akka.http.scaladsl.ConnectionContext
import javax.net.ssl.SSLContext
import akka.http.scaladsl.HttpExt
import com.github.thebridsk.bridge.backend.BridgeServiceFileStore
import scala.reflect.io.Directory
import scala.reflect.io.Path
import java.io.File
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import java.security.SecureRandom
import com.github.thebridsk.bridge.rest.ServerPort
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
import com.github.thebridsk.utilities.main.Subcommand
import com.github.thebridsk.utilities.logging.Logger
import java.net.URL
import java.io.InputStream
import java.net.HttpURLConnection
import java.io.IOException

/**
  * This is the main program for the REST server for our application.
  */
object ShutdownServer extends Subcommand("shutdown") {

  val logger = Logger(ShutdownServer.getClass.getName)

  val defaultHttpsPort = 8443
  val defaultCertificate = "keys/example.com.p12"

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  import com.github.thebridsk.utilities.main.Converters._

  descr("Shutdown a running bridge server")

  banner(s"""
Stops the HTTP server

Syntax:
  ${Server.cmdName} shutdown options
Options:""")
  val optionPort = opt[Int](
    "port",
    short = 'p',
    descr = "the port the server listens on, use 0 for no http, default=8080",
    argName = "port",
    default = Some(8080),
    validate = { p =>
      p >= 0 && p <= 65535
    }
  )
  val optionHttps = opt[Int](
    "https",
    short = 'h',
    descr = "https port to use",
    argName = "port",
    default = None,
    validate = { p =>
      p > 0 && p <= 65535
    }
  );
  val optionCertificate = opt[String](
    "certificate",
    short = 'c',
    descr = "the private certificate for the server, default=None",
    argName = "p12",
    default = None
  )
  val optionCertPassword = opt[String](
    "certpassword",
    descr = "the password for the private certificate, default=None",
    argName = "pw",
    default = None
  )
  footer(s"""
The --port and --https options must be the same as used to start the server.
""")

  def executeSubcommand(): Int = {
    new ShutdownServer().execute()
  }
}

private class ShutdownServer {
  import ShutdownServer._

  private def flushAndCloseInputStream(is: InputStream) = {
    var len = 0
    val buf = new Array[Byte](1024)
    while ({ len = is.read(buf); len > 0 }) len = 0
    is.close()
  }

  def execute(): Int = {
    val url = optionHttps.toOption match {
      case Some(port) =>
        new URL("https://loopback:" + port + "/v1/shutdown?doit=yes")
      case None =>
        val port = optionPort.toOption.getOrElse(8080)
        new URL("http://loopback:" + port + "/v1/shutdown?doit=yes")
    }
    try {
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      conn.setRequestMethod("POST")
      val status = conn.getResponseCode
      if (status < 200 && status >= 300) {
        println("Error shutting down server, status code is " + status)
      }
      try {
        flushAndCloseInputStream(conn.getInputStream)
      } catch {
        case x: IOException =>
          logger.fine("Exception trying to flush", x)
      }

      conn.disconnect()
    } catch {
      case x: IOException =>
        logger.info("Exception trying to shutdown server", x)
    }
    0
  }
}

private class ShutdownServerAkka {
  import ShutdownServer._
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("bridgescorer")
  val log = Logging(system, Server.getClass)
  implicit val executor = system.dispatcher
  implicit val myMaterializer = ActorMaterializer()

  implicit val timeout = Timeout(20.seconds)

  val defaultRunFor = "12h"

  val defaultHttpsPort = 8443
  val defaultCertificate = "keys/example.com.p12"

  def getHttpPortOption() = {
    optionPort.toOption match {
      case Some(0) => None
      case x       => x
    }
  }

  def execute(): Int = {
    import scala.language.postfixOps
    import scala.concurrent.duration._
    var rc = 0
    val dur = 60 seconds

    try {
      Await.ready(shutdownServer(), dur)
    } catch {
      case _: TimeoutException =>
        log.info("Timed out after " + dur.toString())
        rc = 1
    }
    rc
  }

  /**
    * Get the ssl context
    */
  def serverContext = {
    val password = optionCertPassword.toOption.getOrElse("abcdef").toCharArray // default NOT SECURE
    val context = SSLContext.getInstance("TLS")
    val ks = KeyStore.getInstance("PKCS12")
    optionCertificate.toOption match {
      case Some(cert) =>
        ks.load(new FileInputStream(cert), password)
      case None =>
        ks.load(
          getClass.getClassLoader.getResourceAsStream(defaultCertificate),
          password
        )
    }
    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)
    context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom)
    // start up the web server
    ConnectionContext.https(context)
  }

  /**
    * Uses shutdown request to stop the server
    */
  def shutdownServer(): Future[HttpResponse] = {
    val connection = optionHttps.toOption match {
      case Some(port) =>
        Http().outgoingConnectionHttps(
          "loopback",
          port,
          connectionContext = serverContext,
          localAddress =
            Some(new InetSocketAddress(InetAddress.getLoopbackAddress, 0))
        )
      case None =>
        val port = optionPort.toOption.get
        Http().outgoingConnection(
          "loopback",
          port,
          localAddress =
            Some(new InetSocketAddress(InetAddress.getLoopbackAddress, 0))
        )
    }
    val request: HttpRequest = RequestBuilding.Post(
      Uri("/v1/shutdown").withQuery(Query(Map("doit" -> "yes")))
    )
    val responseFuture: Future[HttpResponse] =
      Source
        .single(request)
        .via(connection)
        .runWith(Sink.head)

    responseFuture
      .andThen {
        case Success(_) => println("request succeded")
        case Failure(_) => println("request failed")
      }
      .andThen {
        case _ => system.terminate()
      }

    responseFuture
  }
}
