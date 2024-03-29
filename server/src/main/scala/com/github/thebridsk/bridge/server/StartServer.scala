package com.github.thebridsk.bridge.server

import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Future
import scala.concurrent.Await
import com.github.thebridsk.bridge.server.backend.BridgeService
import com.github.thebridsk.bridge.server.backend.BridgeServiceInMemory
import akka.http.scaladsl.Http.ServerBinding
import scala.util.Failure
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Sink, Source}
import com.github.thebridsk.bridge.server.service.MyService
import akka.http.scaladsl.ConnectionContext
import javax.net.ssl.SSLContext
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
import com.github.thebridsk.bridge.server.service.ShutdownHook
import java.net.NetworkInterface
import java.net.Inet4Address
import com.github.thebridsk.bridge.server.logging.RemoteLoggingConfig
import com.github.thebridsk.bridge.server.backend.BridgeServiceWithLogging
import com.github.thebridsk.bridge.server.backend.BridgeServiceZipStore
import akka.http.scaladsl.settings.ServerSettings
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.HttpsConnectionContext
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import scala.concurrent.ExecutionContextExecutor
import com.github.thebridsk.bridge.server.rest.RestLoggerConfig
import akka.http.scaladsl.model.AttributeKey

/**
  * This is the main program for the REST server for our application.
  */
object StartServer extends Subcommand("start") with ShutdownHook {

  val logger: Logger = Logger(StartServer.getClass.getName)

  val defaultRunFor = "12h"

  val defaultCacheFor = "6h"

  val defaultHttpsPort = 8443
  val defaultCertificate = "keys/examplebridgescorekeeper.p12"

  import com.github.thebridsk.utilities.main.Converters._

  descr("Start HTTP(S) server")

  banner(s"""
Start HTTP server for scoring duplicate and chicago bridge

Syntax:
  ${Server.cmdName} start options
Options:""")
  val optionInterface: ScallopOption[String] = opt[String](
    "interface",
    short = 'i',
    descr = "the port the server listens on, default=0.0.0.0",
    argName = "ip",
    default = Some("0.0.0.0")
  )
  val optionPort: ScallopOption[Int] = opt[Int](
    "port",
    short = 'p',
    descr = "the port the server listens on, use 0 for no http, default=8080",
    argName = "port",
    default = Some(8080),
    validate = { p =>
      p >= 0 && p <= 65535
    }
  )
  val optionCertificate: ScallopOption[String] = opt[String](
    "certificate",
    short = 'c',
    descr = "the private certificate for the server, default=None",
    argName = "p12",
    default = None
  )
  val optionCertPassword: ScallopOption[String] = opt[String](
    "certpassword",
    descr = "the password for the private certificate, default=None",
    argName = "pw",
    default = None
  )
  val optionHttps: ScallopOption[Int] = opt[Int](
    "https",
    short = 'h',
    descr = "https port to use",
    argName = "port",
    default = None,
    validate = { p =>
      p > 0 && p <= 65535
    }
  );
  val optionStore: ScallopOption[Path] = opt[Path](
    "store",
    short = 's',
    descr = "The store directory, default=./store",
    argName = "dir",
    default = Some("./store")
  )
  val optionCACertificate: ScallopOption[Path] = opt[Path](
    "cacert",
    noshort = true,
    descr = "The public CA certificate, in DER format",
    argName = "crt",
    default = None,
    validate = { f => f.isFile }
  )
  val optionRunFor: ScallopOption[Duration] = opt[Duration](
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
  val optionBrowser: ScallopOption[Boolean] = toggle(
    "browser",
    default = Some(false),
    noshort = true,
    descrYes = "Start a browser on the home page of the server",
    descrNo = "Do not start the browser"
  )
  val optionChrome: ScallopOption[Boolean] = toggle(
    "chrome",
    default = Some(false),
    noshort = true,
    descrYes =
      "Start the browser on the home page of the server in fullscreen mode",
    descrNo = "Do not start the chrome browser"
  )
  val optionLoopback: ScallopOption[Boolean] = toggle(
    "loopback",
    default = Some(false),
    noshort = true,
    descrYes = "Use loopback as host name when starting browser",
    descrNo = "Use localhost as host name when starting browser"
  )
  val optionHttp2: ScallopOption[Boolean] = toggle(
    "http2",
    default = Some(false),
    noshort = true,
    descrYes = "Enable http2 support",
    descrNo = "Disable http2 support"
  )
  val optionCache: ScallopOption[Duration] = opt[Duration](
    "cache",
    descr =
      s"time to set in cache-control header of responses.  0s for no-cache. default ${defaultCacheFor}",
    argName = "dur",
    default = Some(Duration(defaultCacheFor)),
    validate = { p =>
      p.compare(Duration.Zero) >= 0
    }
  )

  val optionRemoteLogger: ScallopOption[Path] =
    opt[Path](
      "remotelogging",
      short = 'l',
      descr =
        "Specify remote logging YAML profile to use instead of built in one",
      argName = "file",
      default = None
    )

  val optionIPadRemoteLogging: ScallopOption[String] = opt[String](
    "ipad",
    noshort = true,
    descr = "The remote logging profile to use for the iPad, default: off",
    argName = "profile",
    default = Some("default")
  )

  val optionBrowserRemoteLogging: ScallopOption[String] = opt[String](
    "browserlogging",
    noshort = true,
    descr = "The remote logging profile to use for browsers, default: default",
    argName = "profile",
    default = Some("default")
  )

  val optionDiagnosticDir: ScallopOption[Path] = opt[Path](
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

  val attributeInetSocketRemote = AttributeKey[String]("InetSocketRemote")
  val attributeInetSocketLocal = AttributeKey[String]("InetSocketLocal")

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

  def terminateServer(): Promise[String] = {
    getServer(true, false).terminateServer()
  }

  def terminateServerIn(
      duration: Duration = 10 seconds
  ): Future[Promise[String]] = {
    getServer(true, false).terminateServerIn(duration)
  }

  /**
    * Get the ssl context
    */
  def serverSSLContext(
      certPassword: Option[String] = optionCertPassword.toOption,
      certificate: Option[String] = optionCertificate.toOption
  ): HttpsConnectionContext = {
    logger.info(
      s"Creating serverSSLContext, certificate=$certificate, workingDirectory=${new File(".").getAbsoluteFile().getCanonicalFile()}"
    )
    val password =
      certPassword.getOrElse("abcdef").toCharArray // default NOT SECURE
    val context = SSLContext.getInstance("TLS")
    val ks = certificate match {
      case Some(cert) =>
        val ks = if (cert.endsWith(".jks")) {
          KeyStore.getInstance("JKS")
        } else {
          KeyStore.getInstance("PKCS12")
        }
        ks.load(new FileInputStream(cert), password)
        ks
      case None =>
        val ks = if (defaultCertificate.endsWith(".jks")) {
          KeyStore.getInstance("JKS")
        } else {
          KeyStore.getInstance("PKCS12")
        }
        ks.load(
          getClass.getClassLoader.getResourceAsStream(defaultCertificate),
          password
        )
        ks
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
    ConnectionContext.httpsServer(context)
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
      connectionContext: Option[HttpsConnectionContext] = None,
      cache: Option[Duration] = None,
      optRemoteLogger: Option[Path] = None,
      optBrowserRemoteLogging: Option[String] = None,
      optIPadRemoteLogging: Option[String] = None,
      diagnosticDir: Option[Directory] = None,
      optCACert: Option[Path] = None
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
      diagnosticDir,
      optCACert = optCACert
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
  implicit val system: ActorSystem = ActorSystem("bridgescorer")
  val log = StartServer.logger // Logging(system, Server.getClass)
  implicit val executor: ExecutionContextExecutor = system.dispatcher

  implicit val timeout: Timeout = Timeout(20.seconds)

  val defaultRunFor = "12h"

  val defaultHttpsPort = 8443
  val defaultCertificate = "keys/examplebridgescorekeeper.p12"

  def getHttpPortOption(): Option[Int] = {
    optionPort.toOption match {
      case Some(0) => None
      case x       => x
    }
  }

  def execute(): Int = {

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

  def getURL(interface: String): String = {
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

  def getHostPort(interface: String): String = {
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
          Some(
            new BridgeServiceZipStore("root", p.toFile) {
              override val diagnosticDir: Option[Directory] =
                optionDiagnosticDir.toOption.map(_.toDirectory)
            }
          )
        } else if (!d.isDirectory) {
          if (!d.createDirectory().isDirectory) {
            logger.severe("Unable to create directory for FileStore: " + d)
            return 1
          }
          Some(
            new BridgeServiceFileStore(d, oid = Some("root")) {
              override val diagnosticDir: Option[Directory] =
                optionDiagnosticDir.toOption.map(_.toDirectory)
            }
          )
        } else {
          Some(
            new BridgeServiceFileStore(d, oid = Some("root")) {
              override val diagnosticDir: Option[Directory] =
                optionDiagnosticDir.toOption.map(_.toDirectory)
            }
          )
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

    val context: Option[HttpsConnectionContext] = if (httpsPort.isDefined) {
      Some(serverSSLContext())
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
      http2Support = http2Support,
      optCACert = optionCACertificate.toOption
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
  val terminatePromise: Promise[String] = Promise[String]()

  def terminateServerIn(
      duration: Duration = 10 seconds
  ): Future[Promise[String]] = {
    Future {
      log.info("Shutting down server in " + duration)
      Thread.sleep(duration.toMillis)
      terminateServer()
    }
  }

  def terminateServer(): Promise[String] = {
    log.info("Shutting down server now")
    terminatePromise.success("Terminate")
  }

  def redirectRoute(
      scheme: String,
      port: Int
  ): RequestContext => Future[RouteResult] =
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
    * @param connectionContext the SSL connection context, default is serverContext
    *                          This is only used if httpsPort is not None
    * @return A Future that completes when the server is ready and listening on the ports.
    */
  def start(
      interface: String = "loopback",
      httpPort: Option[Int] = Some(8080),
      httpsPort: Option[Int] = None,
      bridge: Option[BridgeService] = None,
      connectionContext: Option[HttpsConnectionContext] = None,
      cache: Option[Duration] = None,
      optRemoteLogger: Option[Path] = None,
      optBrowserRemoteLogging: Option[String] = None,
      optIPadRemoteLogging: Option[String] = None,
      optDiagnosticDir: Option[Directory] = None,
      http2Support: Boolean = false,
      optCACert: Option[Path] = None
  ): Future[MyService] = {
    val myService = new MyService {

      /**
        * The backend service object for our service.
        */
      lazy val restService: BridgeService =
        bridge.getOrElse(new BridgeServiceInMemory("root"))
      override val diagnosticDir: Option[Directory] = optDiagnosticDir
      lazy val actorSystem: ActorSystem = system

      override lazy val cacheDuration =
        cache.getOrElse(Duration(defaultCacheFor))

      override lazy val listenInterface = {
        import scala.jdk.CollectionConverters._
        NetworkInterface.getNetworkInterfaces.asScala
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
      }
      override def ports = ServerPort(httpPort, httpsPort)
      override lazy val host = if (interface == "0.0.0.0") {
        listenInterface.headOption.getOrElse("loopback")
        "loopback"
      } else {
        interface
      }

      override val certhttppath = optCACert.map { certfile =>
        log.info(s"Using CA cert ${certfile}")
        path("servercert") {
          getFromFile(
            certfile.jfile,
            ContentType(MediaTypes.`application/x-x509-ca-cert`)
          )
        }
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

    val settings = ServerSettings(system)
    val httpSettings = settings.withPreviewServerSettings(
      settings.previewServerSettings.withEnableHttp2(false)
    )
    val httpsSettings = settings.withPreviewServerSettings(
      settings.previewServerSettings.withEnableHttp2(http2Support)
    )
    // https://doc.akka.io/docs/akka-http/current/server-side/low-level-api.html
    val sinkRoute = Sink.foreach[Http.IncomingConnection] { connection =>
          val remote = connection.remoteAddress
          val local = connection.localAddress
          def rte(rc: RequestContext): Future[RouteResult] = {
            val rc2 = rc.mapRequest { req =>
              req
                .addAttribute(attributeInetSocketRemote, remote.getAddress().getHostAddress())
                .addAttribute(attributeInetSocketLocal, local.getAddress().getHostAddress())
            }
            myService.myRouteWithLogging(rc2)
          }
          connection.handleWith(rte _)
        }
    bindingHttps = if (httpsPort.isDefined) {
      Some(
        Http()
          .newServerAt(interface, httpsPort.get)
          .enableHttps(connectionContext.getOrElse(serverSSLContext()))
          .withSettings(httpsSettings)
          // .bind(myService.myRouteWithLogging)
          .connectionSource()
          .to(sinkRoute)
          .run()
      )
    } else {
      None
    }
    bindingHttp = if (httpPort.isDefined) {
      if (httpsPort.isDefined) {
        // both http and https defined, redirect http to https

        val redirectHttpToHttps = redirectRoute("https", httpsPort.get)
        val httpToHttps = respondWithHeaders(myService.cacheHeaders) {
          (myService.certhttppath.toList ::: List(redirectHttpToHttps))
            .reduceLeft((ac, v) => ac ~ v)
        }
        Some(
          Http()
            .newServerAt(interface, httpPort.get)
            .withSettings(httpSettings)
            .bind(httpToHttps)
        )
      } else {
        // only http defined
        Some(
          Http()
            .newServerAt(interface, httpPort.get)
            .withSettings(httpSettings)
            // .bind(myService.myRouteWithLogging)
            .connectionSource()
            .to(sinkRoute)
            .run()
        )
      }
    } else {
      if (httpsPort.isDefined) {
        // Only run with https port
        None
      } else {
        // no http or https port defined.  Use port 8080 for http
        Some(
          Http()
            .newServerAt(interface, 8080)
            .withSettings(httpSettings)
            // .bind(myService.myRouteWithLogging)
            .connectionSource()
            .to(sinkRoute)
            .run()
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
        log.info(
          s"ClassPath:\n${ClassPath.show("  ", getClass.getClassLoader)}"
        )
        log.info(s"System Properties:\n${ClassPath.showProperties("  ")}")
        val urls = RestLoggerConfig.serverURL(myService.ports, true)
        val page = myService.getRootPage.map(_.substring(1)).getOrElse("")
        println(
          s"The server can be found at:${urls.serverUrl.map(_ + page).mkString("\n  ","\n  ","\n")}"
        )
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
          connectionContext = serverSSLContext(),
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
