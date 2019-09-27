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
import scala.concurrent.Await
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
import com.github.thebridsk.utilities.classpath.ClassPath
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
import com.github.thebridsk.utilities.main.Subcommand
import com.github.thebridsk.utilities.logging.Logger
import javax.net.ssl.TrustManagerFactory
import com.github.thebridsk.bridge.server.service.ShutdownHook
import java.net.NetworkInterface
import java.net.Inet4Address
import com.github.thebridsk.bridge.server.logging.RemoteLoggingConfig
import com.github.thebridsk.bridge.server.backend.BridgeServiceWithLogging
import com.github.thebridsk.bridge.server.backend.BridgeServiceZipStore
import akka.http.scaladsl.settings.ServerSettings
import akka.http.scaladsl.server.Route

/**
  * This is the main program for the REST server for our application.
  */
object StartServer extends Subcommand("start") with ShutdownHook {

  val logger = Logger(StartServer.getClass.getName)

  val defaultRunFor = "12h"

  val defaultCacheFor = "6h"

  val defaultHttpsPort = 8443
  val defaultCertificate = "keys/example.com.p12"

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  import com.github.thebridsk.utilities.main.Converters._

  descr("Start HTTP(S) server")

  banner(s"""
Start HTTP server for scoring duplicate and chicago bridge

Syntax:
  ${Server.cmdName} start options
Options:""")
  val optionInterface = opt[String](
    "interface",
    short = 'i',
    descr = "the port the server listens on, default=0.0.0.0",
    argName = "ip",
    default = Some("0.0.0.0")
  )
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
  val optionStore = opt[Path](
    "store",
    short = 's',
    descr = "The store directory, default=./store",
    argName = "dir",
    default = Some("./store")
  )
  val optionRunFor = opt[Duration](
    "runfor",
    short = 'r',
    descr = s"Run for specified as a duration, default ${defaultRunFor}",
    argName = "dur",
    default = Some(Duration(defaultRunFor)),
    validate = { p =>
      p.compare(Duration.Zero) > 0
    }
  );
//  val optionShutdown = toggle("shutdown", default = Some(false), noshort = true,
//                              descrYes = "Shutdown a server running on the same machine, other options should be the same as when starting server",
//                              descrNo = "Start the server." )
  val optionBrowser = toggle(
    "browser",
    default = Some(false),
    noshort = true,
    descrYes = "Start a browser on the home page of the server",
    descrNo = "Do not start the browser"
  )
  val optionChrome = toggle(
    "chrome",
    default = Some(false),
    noshort = true,
    descrYes =
      "Start the browser on the home page of the server in fullscreen mode",
    descrNo = "Do not start the chrome browser"
  )
  val optionLoopback = toggle(
    "loopback",
    default = Some(false),
    noshort = true,
    descrYes = "Use loopback as host name when starting browser",
    descrNo = "Use localhost as host name when starting browser"
  )
  val optionHttp2 = toggle(
    "http2",
    default = Some(false),
    noshort = true,
    descrYes = "Enable http2 support",
    descrNo = "Disable http2 support"
  )
  val optionCache = opt[Duration](
    "cache",
    descr =
      s"time to set in cache-control header of responses.  0s for no-cache. default ${defaultCacheFor}",
    argName = "dur",
    default = Some(Duration(defaultCacheFor)),
    validate = { p =>
      p.compare(Duration.Zero) >= 0
    }
  )

  val optionRemoteLogger =
    opt[Path](
      "remotelogging",
      short = 'l',
      descr =
        "Specify remote logging YAML profile to use instead of built in one",
      argName = "file",
      default = None
    )

  val optionIPadRemoteLogging = opt[String](
    "ipad",
    noshort = true,
    descr = "The remote logging profile to use for the iPad, default: off",
    argName = "profile",
    default = Some("default")
  )

  val optionBrowserRemoteLogging = opt[String](
    "browserlogging",
    noshort = true,
    descr = "The remote logging profile to use for browsers, default: default",
    argName = "profile",
    default = Some("default")
  )

  val optionDiagnosticDir = opt[Path](
    "diagnostics",
    noshort = true,
    descr =
      "The directory that contains the log files, default is none.  All .log files in directory may be collected for diagnostic purposes.",
    argName = "dir",
    default = None
  )

  footer(s"""
To have the server listen for HTTPS, you must use one or both of the following options:
  --https
  --certificate
If one of the above is not specified the following defaults are used:
  --https ${defaultHttpsPort}
  --certificate ${defaultCertificate} from the classpath
If the certificate is not specified and the password is not specified,
then the default password for the default certificate is used.
If both https and http is started, then http will be redirected to https
""")

  private var server: Option[StartServer] = None

  private def getServer(
      expectRunning: Boolean,
      startServer: Boolean = false
  ) = {
    server match {
      case Some(s) =>
        if (expectRunning) s
        else throw new IllegalStateException("Already running")

      case None =>
        if (expectRunning) {
          if (startServer) {
            server = Some(new StartServer())
            server.get
          } else {
            throw new IllegalStateException("Not running")
          }
        } else {
          throw new IllegalArgumentException("Not running, as expected")
        }

    }
  }

  def executeSubcommand(): Int = {
    getServer(true, true).execute()
  }

  def terminateServer() = {
    getServer(true, false).terminateServer()
  }

  def terminateServerIn(duration: Duration = 10 seconds) = {
    getServer(true, false).terminateServerIn(duration)
  }

  /**
    * Start the server
    * @param interface the interface the server is listening on, default: loopback
    * @param httpPort the port for http to listen on, default: Some(8080)
    * @param httpsPort the port for https to listen on, default: None
    * @param bridge the BridgeService to use, if None then BridgeServiceInMemory is used
    * @param connectionContext the connection context, MUST be specified if httpsPort is not None.
    *                          Default is for http communications.
    * @return A Future that completes when the server is ready and listening on the ports.
    */
  def start(
      interface: String = "loopback",
      httpPort: Option[Int] = Some(8080),
      httpsPort: Option[Int] = None,
      bridge: Option[BridgeService] = None,
      connectionContext: Option[ConnectionContext] = None,
      cache: Option[Duration] = None,
      optRemoteLogger: Option[Path] = None,
      optBrowserRemoteLogging: Option[String] = None,
      optIPadRemoteLogging: Option[String] = None,
      diagnosticDir: Option[Directory] = None
  ): Future[MyService] = {
    getServer(true, true).start(
      interface,
      httpPort,
      httpsPort,
      bridge,
      connectionContext,
      cache,
      optRemoteLogger,
      optBrowserRemoteLogging,
      optIPadRemoteLogging,
      diagnosticDir
    )
  }

  /**
    * Stop the Server
    */
  def stopServer(): Future[Unit] = {
    getServer(true, false).stopServer()
  }
}

private class StartServer {
  import StartServer._
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("bridgescorer")
  val log = StartServer.logger // Logging(system, Server.getClass)
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

//    if (optionShutdown.isSupplied) {
//      var rc = 0
//      val dur = 60 seconds
//
//      try {
//        Await.ready( shutdownServer(), dur )
//      } catch {
//        case _: TimeoutException =>
//          log.info("Timed out after "+dur.toString())
//          rc = 1
//      }
//      rc
//    } else {
    startingServer()
//    }
  }

  def getURL(interface: String) = {
    val httpsURL = optionHttps.toOption match {
      case Some(port) =>
        if (port == 443) Some("https://" + interface + "/")
        else Some("https://" + interface + ":" + port + "/")
      case None => None
    }
    val httpURL = optionPort.toOption match {
      case Some(port) =>
        if (port == 80) Some("http://" + interface + "/")
        else Some("http://" + interface + ":" + port + "/")
      case None if (httpsURL.isEmpty) => Some("http://" + interface + ":8080/")
      case None                       => None
    }

    httpsURL.getOrElse(httpURL.get)
  }

  def getHostPort(interface: String) = {
    val httpsURL = optionHttps.toOption match {
      case Some(port) => Some("https://" + interface + ":" + port + "/")
      case None       => None
    }
    val httpURL = optionPort.toOption match {
      case Some(port)                 => Some("http://" + interface + ":" + port + "/")
      case None if (httpsURL.isEmpty) => Some("http://" + interface + ":8080/")
      case None                       => None
    }

    httpsURL.getOrElse(httpURL.get)
  }

  def startingServer(): Int = {

    val bs = optionStore.toOption match {
      case Some(p) =>
        val d = p.toDirectory
        if (p.isFile && p.extension == "zip") {
          Some(new BridgeServiceZipStore("root", p.toFile))
        } else if (!d.isDirectory) {
          if (!d.createDirectory().isDirectory) {
            logger.severe("Unable to create directory for FileStore: " + d)
            return 1
          }
          Some(new BridgeServiceFileStore(d, oid = Some("root")))
        } else {
          Some(new BridgeServiceFileStore(d, oid = Some("root")))
        }

      case None => None
    }

    val httpsPort = optionHttps.toOption match {
      case p: Some[Int] =>
        // HTTPS port was specified
        p
      case None =>
        // HTTPS port was not specified
        optionCertificate.toOption match {
          case p: Some[String] =>
            // Certificate was specified
            // use default HTTPS port
            Some(defaultHttpsPort)
          case None =>
            // Certificate was NOT specified
            // don't startup HTTPS
            None
        }
    }

    val http2Support = optionHttp2()

    val context: Option[ConnectionContext] = if (httpsPort.isDefined) {
      Some(serverContext)
    } else {
      None
    }

    val startFuture = start(
      interface = optionInterface(),
      httpPort = getHttpPortOption(),
      httpsPort = httpsPort,
      connectionContext = context,
      bridge = bs,
      cache = optionCache.toOption,
      optRemoteLogger = optionRemoteLogger.toOption,
      optBrowserRemoteLogging = optionBrowserRemoteLogging.toOption,
      optIPadRemoteLogging = optionIPadRemoteLogging.toOption,
      //         optBrowserLogger = optionBrowserLogger.toOption,
      optDiagnosticDir = optionDiagnosticDir.toOption.map(p => p.toDirectory),
      http2Support = http2Support
    )

    MyService.shutdownHook = Some(StartServer)

    startFuture.onComplete(_ match {
      case Success(_) =>
        if (optionBrowser.isSupplied || optionChrome.isSupplied) {
          val hostForBrowser = optionLoopback.toOption
            .filter(lb => lb)
            .map(lb => "loopback")
            .getOrElse("localhost")
          Browser.start(getURL(hostForBrowser), optionChrome.getOrElse(false))
        }
      case Failure(e) =>
        log.severe(s"Failed to start the server ${e.getMessage}")
        terminatePromise.complete(Try("Failed " + e.getMessage))
    })

    var rc: Int = 0
    val dur = optionRunFor.toOption.get
    logger.info(s"Waiting for " + dur);
    try {
      Await.ready(terminatePromise.future, dur)
    } catch {
      case _: TimeoutException =>
        log.info("Timed out after " + dur.toString())
        rc = 1
    }
    logger.info("Ending")

    Await.ready(stopServer(), 60 seconds)

    rc
  }

  // This will complete if the server fails to start
  val terminatePromise = Promise[String]()

  def terminateServerIn(duration: Duration = 10 seconds) = {
    Future {
      log.info("Shutting down server in " + duration)
      Thread.sleep(duration.toMillis)
      terminateServer()
    }
  }

  def terminateServer() = {
    log.info("Shutting down server now")
    terminatePromise.success("Terminate")
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
//    val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
//    trustManagerFactory.init(ks)
    context.init(
      keyManagerFactory.getKeyManagers,
      /* trustManagerFactory.getTrustManagers */ null,
      new SecureRandom
    )
    // start up the web server
    ConnectionContext.https(context)
  }

  def redirectRoute(scheme: String, port: Int) =
    extractUri { uri =>
      redirect(
        uri.withScheme(scheme).withPort(port),
        StatusCodes.PermanentRedirect
      )
    }

  var bindingHttp: Option[Future[ServerBinding]] = None
  var bindingHttps: Option[Future[ServerBinding]] = None

  /**
    * Start the server
    * @param interface the interface the server is listening on, default: loopback
    * @param httpPort the port for http to listen on, default: Some(8080)
    * @param httpsPort the port for https to listen on, default: None
    * @param bridge the BridgeService to use, if None then BridgeServiceInMemory is used
    * @param connectionContext the connection context, MUST be specified if httpsPort is not None.
    *                          Default is for http communications.
    * @return A Future that completes when the server is ready and listening on the ports.
    */
  def start(
      interface: String = "loopback",
      httpPort: Option[Int] = Some(8080),
      httpsPort: Option[Int] = None,
      bridge: Option[BridgeService] = None,
      connectionContext: Option[ConnectionContext] = None,
      cache: Option[Duration] = None,
      optRemoteLogger: Option[Path] = None,
      optBrowserRemoteLogging: Option[String] = None,
      optIPadRemoteLogging: Option[String] = None,
      optDiagnosticDir: Option[Directory] = None,
      http2Support: Boolean = false
  ): Future[MyService] = {
    val myService = new MyService {

      /**
        * The backend service object for our service.
        */
      lazy val restService: BridgeService =
        bridge.getOrElse(new BridgeServiceInMemory("root"))
      override val diagnosticDir: Option[Directory] = optDiagnosticDir
      lazy val actorSystem: ActorSystem = system
      lazy val materializer: ActorMaterializer = myMaterializer

      override lazy val cacheDuration =
        cache.getOrElse(Duration(defaultCacheFor))

      override def ports = ServerPort(httpPort, httpsPort)
      override lazy val host = if (interface == "0.0.0.0") {
        import scala.collection.JavaConverters._
        val x = NetworkInterface.getNetworkInterfaces.asScala
          .filter { x =>
            x.isUp() && !x.isLoopback()
          }
          .flatMap { ni =>
            ni.getInetAddresses.asScala
          }
          .filter { x =>
            x.isInstanceOf[Inet4Address]
          }
          .map { x =>
            x.getHostAddress
          }
          .toList
        x.headOption.getOrElse("loopback")
        "loopback"
      } else {
        interface
      }

      val rlc = optRemoteLogger
        .flatMap { f =>
          Option(RemoteLoggingConfig.readConfig(f.jfile))
        }
        .getOrElse {
          BridgeServiceWithLogging
            .getDefaultRemoteLoggerConfig()
            .getOrElse(RemoteLoggingConfig())
        }

      optBrowserRemoteLogging.map { p =>
        rlc.browserConfig("default", p).foreach { lc =>
          log.info(s"Setting browser logging to ${p}: ${lc}")
          restService.setDefaultLoggerConfig(lc, false)
        }
      }
      optIPadRemoteLogging.map { p =>
        rlc.browserConfig("ipad", p).foreach { lc =>
          log.info(s"Setting iPad logging to ${p}: ${lc}")
          restService.setDefaultLoggerConfig(lc, true)
        }
      }

    }
    logger.info("Starting server")
    httpsPort match {
      case Some(port) =>
      case None       =>
    }
    val settings = ServerSettings(system)
    val httpSettings = settings.withPreviewServerSettings(
      settings.previewServerSettings.withEnableHttp2(false)
    )
    val httpsSettings = settings.withPreviewServerSettings(
      settings.previewServerSettings.withEnableHttp2(http2Support)
    )
    bindingHttps = if (httpsPort.isDefined) {
      Some(
        Http().bindAndHandleAsync(
          Route.asyncHandler(myService.myRouteWithLogging),
          interface,
          httpsPort.get,
          connectionContext = connectionContext.get,
          settings = httpsSettings
        )
      )
    } else {
      None
    }
    bindingHttp = if (httpPort.isDefined) {
      if (httpsPort.isDefined) {
        // both http and https defined, redirect http to https
        Some(
          Http().bindAndHandleAsync(
            Route.asyncHandler(redirectRoute("https", httpsPort.get)),
            interface,
            httpPort.get,
            settings = httpSettings
          )
        )
      } else {
        // only http defined
        Some(
          Http().bindAndHandleAsync(
            Route.asyncHandler(myService.myRouteWithLogging),
            interface,
            httpPort.get,
            settings = httpSettings
          )
        )
      }
    } else {
      if (httpsPort.isDefined) {
        // Only run with https port
        None
      } else {
        // no http or https port defined.  Use port 8080 for http
        Some(
          Http().bindAndHandleAsync(
            Route.asyncHandler(myService.myRouteWithLogging),
            interface,
            8080,
            settings = httpSettings
          )
        )
      }
    }
    bindingHttp.foreach { f =>
      f.onComplete(_ match {
        case Success(binding) =>
          val localAddress = binding.localAddress
          log.info(
            s"Server is listening on ${localAddress.getHostName}:${localAddress.getPort} for HTTP"
          )
        case Failure(e) =>
          log.severe(
            s"HTTP binding failed with ${e.getClass.getName} ${e.getMessage}"
          )
//          system.terminate()
      })
    }
    bindingHttps.foreach { f =>
      f.onComplete(_ match {
        case Success(binding) =>
          val localAddress = binding.localAddress
          log.info(
            s"Server is listening on ${localAddress.getHostName}:${localAddress.getPort} for HTTPS"
          )
        case Failure(e) =>
          log.severe(
            s"HTTPS binding failed with ${e.getClass.getName} ${e.getMessage}"
          )
//          system.terminate()
      })
    }
    log.info("Starting to wait for the bindings to finish")
    val f = waitfor(60 seconds, bindingHttp, bindingHttps)
    f.onComplete(_ match {
      case Success(_) =>
        log.info(s"ClassPath:\n${ClassPath.show("  ", getClass.getClassLoader)}")
        log.info(s"System Properties:\n${ClassPath.showProperties("  ")}")
      case Failure(e) =>
        log.severe(
          s"Waiting for bindings to finish failed with ${e.getMessage}"
        )
    })
    f.transform(s => myService, t => t)
  }

  /**
    * Wait for a number of Futures to finish
    * @param f the futures
    * @param sec the maximum time to wait for a future to finish
    */
  def waitfor(dur: Duration, f: Option[Future[_]]*): Future[Unit] =
    Future {
      f.foreach {
        _.foreach { ff =>
          Await.ready(ff, dur)
          ff.value match {
            case Some(Success(s)) =>
            case Some(Failure(f)) =>
              throw f
            case None =>
              throw new TimeoutException()
          }
        }
      }
    }

  /**
    * Just stop the HTTP and HTTPS service
    */
  def stopHttp(): Future[Unit] = {
    // not sure this works
    logger.info("Stopping server")
    val stophttp = bindingHttp.map { binding =>
      val f = binding.flatMap(_.unbind()) // trigger unbinding from the port
      f.onComplete(_ match {
        case Success(_) => log.info("HTTP server stopped")
        case Failure(e) =>
          log.severe(s"Unbinding HTTP server failed with ${e.getMessage}")
      })
      f
    }
    val stophttps = bindingHttps.map { binding =>
      val f = binding.flatMap(_.unbind()) // trigger unbinding from the port
      f.onComplete(_ match {
        case Success(_) => log.info("HTTPS server stopped")
        case Failure(e) =>
          log.severe(s"Unbinding HTTPS server failed with ${e.getMessage}")
      })
      f
    }
    waitfor(60 seconds, stophttp, stophttps)
  }

  /**
    * Stop the Server
    */
  def stopServer(): Future[Unit] = {
    logger.info("Stopping server")
    val f = stopHttp()
    f.onComplete { _ =>
      log.info("Server stopped")
      system.terminate() // and shutdown when done
    }
    f
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
