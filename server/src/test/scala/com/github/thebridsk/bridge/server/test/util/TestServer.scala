package com.github.thebridsk.bridge.server.test.util

import com.github.thebridsk.bridge.server.backend.BridgeServiceInMemory
import com.github.thebridsk.bridge.server.Server
import com.github.thebridsk.bridge.data.LoggerConfig
import com.github.thebridsk.utilities.logging.Logger
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import com.github.thebridsk.bridge.server.test.util.MonitorTCP
import com.github.thebridsk.bridge.server.StartServer
import java.net.URL
import com.github.thebridsk.bridge.server.service.MyService
import scala.reflect.io.Directory
import com.github.thebridsk.bridge.server.backend.FileImportStore
import com.github.thebridsk.bridge.server.logging.RemoteLoggingConfig
import java.io.File
import com.github.thebridsk.bridge.server.backend.BridgeServiceWithLogging

object TestServer {

  val testlog: Logger = Logger(TestServer.getClass.getName)

  val startTestServer: Boolean = getBooleanProp("StartTestServer", false)
  val useTestServerURL: Option[String] = getProp("UseBridgeScorerURL")
  val useTestServerScheme: Option[String] = getProp("UseBridgeScorerScheme")
  val useTestServerHost: Option[String] = getProp("UseBridgeScorerHost")
  val useTestServerPort: Option[String] = getProp("UseBridgeScorerPort")
  val useWebsocketLogging: Option[String] = getProp("UseWebsocketLogging")
  val testProductionPage: Boolean = getBooleanProp("TestProductionPage", false)

  val optRemoteLogger: Option[String] = getProp("OptRemoteLogger")
  val optBrowserRemoteLogging: Option[String] = getProp(
    "OptBrowserRemoteLogging"
  )
  val optIPadRemoteLogging: Option[String] = getProp("OptIPadRemoteLogging")

  def loggingConfig(l: List[String]): LoggerConfig =
    LoggerConfig("[root]=ALL" :: Nil, "console=INFO" :: l)

  val backend: BridgeServiceInMemory = {
    val bs = new BridgeServiceInMemory("root") {

      override val importStore = {
        val importdir = Directory.makeTemp("importStore", ".dir")
        import scala.concurrent.ExecutionContext.Implicits.global
        Some(new FileImportStore(importdir))
      }

    }

    var setConfig: Boolean = false

    val rlc = optRemoteLogger
      .flatMap { f =>
        val x = Option(RemoteLoggingConfig.readConfig(new File(f)))
        testlog.info(s"Maybe using ${f}: ${x}")
        setConfig = true
        x
      }
      .getOrElse {
        BridgeServiceWithLogging
          .getDefaultRemoteLoggerConfig()
          .getOrElse(RemoteLoggingConfig())
      }

    optBrowserRemoteLogging.orElse(Some("default")).map { p =>
      rlc.browserConfig("default", p).foreach { lc =>
        testlog.info(s"Setting browser logging to ${p}: ${lc}")
        bs.setDefaultLoggerConfig(lc, false)
        setConfig = true
      }
    }
    optIPadRemoteLogging.orElse(Some("default")).foreach { p =>
      rlc.browserConfig("ipad", p).foreach { lc =>
        testlog.info(s"Setting iPad logging to ${p}: ${lc}")
        bs.setDefaultLoggerConfig(lc, true)
        setConfig = true
      }
    }

    if (!setConfig) {
      bs.setDefaultLoggerConfig(
        useWebsocketLogging match {
          case Some(v) =>
            testlog.info(s"useWebsocketLogging: ${v}")
            if (java.lang.Boolean.parseBoolean(v))
              loggingConfig("websocket=ALL,bridge" :: Nil)
            else loggingConfig("server=ALL" :: Nil)
          case _ =>
            testlog.info(s"useWebsocketLogging was not set")
            loggingConfig("server=ALL" :: Nil)
        },
        false
      )
    }
    testlog.fine(
      "Using browser logging config: " + bs.getDefaultLoggerConfig(false)
    )
    bs
  }

  val startingServer: Boolean =
    startTestServer || (
      useTestServerHost.isEmpty && useTestServerPort.isEmpty && useTestServerURL.isEmpty
    )

  val scheme: String = useTestServerScheme.getOrElse("http")
  val interface = useTestServerHost.getOrElse("localhost") // the interface to start the server on
  val hostname: String =
    useTestServerHost.getOrElse(interface) // the hostname for URLs
  val port: Int = useTestServerPort match {
    case None        => 8081
    case Some(sport) => sport.toInt
  }
  val schemeport: Int = if (scheme == "http") 80 else 443
  val portuse: String = if (port == schemeport) "" else { ":" + port }
  val hosturl: String =
    useTestServerURL.getOrElse(scheme + "://" + hostname + portuse) + "/"
  private val pageprod: String = hosturl + "public/index.html"
  private val pagedev: String = hosturl + "public/index-fastopt.html"
  private val pagedemoprod: String = hosturl + "public/demo.html"
  private val pagedemodev: String = hosturl + "public/demo-fastopt.html"
  val docs: String = hosturl + "v1/docs/"

  testlog.info(s"""Testing ${if (testProductionPage) "Prod" else "Dev"} pages""")

  private var startCount = 0
  private def getAndIncrementStartCount() =
    synchronized {
      val r = startCount
      startCount += 1
      r
    }
  private def decrementAndGetStartCount() =
    synchronized {
      startCount -= 1
      startCount
    }

  def onlyRunWhenStartingServer(
      notRunMsg: Option[String] = None
  )(runWhenStarting: () => Unit): Unit = {
    if (startingServer) {
      runWhenStarting()
    } else {
      notRunMsg match {
        case Some(m) => testlog.fine(m)
        case None    =>
      }
    }
  }

  private var myService: Option[MyService] = None

  def getMyService = myService.get

  def getHttpsPort: Int = port + 443 - 80

  def start(https: Boolean = false): Unit = {
    MonitorTCP.startMonitoring()
    try {
      onlyRunWhenStartingServer(Some("Using existing server")) { () =>
        synchronized {
          if ((getAndIncrementStartCount()) == 0) {
            testlog.info(
              s"Starting TestServer https=${https}, hosturl=${hosturl}, useWebsocketLogging=${useWebsocketLogging}, backend.defaultLoggerConfig=${backend
                .getDefaultLoggerConfig(false)}"
            )
            Server.init()
            val (httpsPort, connectionContext) = if (https) {
              val context = StartServer.serverSSLContext(
                Some("abcdef"),
                Some("../server/key/examplebridgescorekeeper.jks")
              )
              val httpsport = getHttpsPort
              testlog.info(s"HTTPS port is ${httpsport}")
              (Some(httpsport), Some(context))
            } else {
              (None, None)
            }
            val future = StartServer.start(
              interface = interface,
              httpPort = Some(port),
              httpsPort = httpsPort,
              bridge = Some(backend),
              connectionContext = connectionContext
            )
            import scala.concurrent.ExecutionContext.Implicits.global
            future.map(s => myService = Some(s))
            Await.ready(future, 60 seconds)
          }
        }
      }
    } catch {
      case x: Throwable =>
        testlog.severe("TestServer.start failed", x)
        throw x
    }
  }

  def stop(): Unit = {
    MonitorTCP.stopMonitoring()
    onlyRunWhenStartingServer() { () =>
      synchronized {
        if ((decrementAndGetStartCount()) == 0) {
          Await.ready(StartServer.stopServer(), 60 seconds)
        }
      }
    }
  }

  def isServerStartedByTest = startingServer

  def getAppPage: String =
    if (testProductionPage) getAppPageProd else getAppPageDev
  def getAppPageUrl(uri: String): String =
    if (testProductionPage) getAppPageProdUrl(uri) else getAppPageDevUrl(uri)

  private def getAppPageProd = pageprod
  private def getAppPageProdUrl(uri: String): String =
    if (uri.length() == 0) pageprod else pageprod + "#" + uri

  private def getAppPageDev = pagedev
  private def getAppPageDevUrl(uri: String): String =
    if (uri.length() == 0) pagedev else pagedev + "#" + uri

  def getAppDemoPage: String =
    if (testProductionPage) getAppDemoPageProd else getAppDemoPageDev
  def getAppDemoPageUrl(uri: String): String =
    if (testProductionPage) getAppDemoPageProdUrl(uri)
    else getAppDemoPageDevUrl(uri)

  private def getAppDemoPageProd = pagedemoprod
  private def getAppDemoPageProdUrl(uri: String): String =
    if (uri.length() == 0) pagedemoprod else pagedemoprod + "#" + uri

  private def getAppDemoPageDev = pagedemodev
  private def getAppDemoPageDevUrl(uri: String): String =
    if (uri.length() == 0) pagedemodev else pagedemodev + "#" + uri

  def getDocs = docs
  def getDocs(fragment: String): String =
    if (fragment.length() == 0) docs else docs + "#!" + fragment

  def getHelpPage(page: String = ""): String = hosturl + "help/" + page

  /**
    * Returns the URL for the given URI.
    * @param uri the URI.  Must start with a '/'
    * @throws IllegalArgumentException if the uri does not start with a '/'
    */
  def getUrl(uri: String): URL = {
    if (uri.charAt(0) != '/')
      throw new IllegalArgumentException("uri must start with a '/'")
    new URL(hosturl + uri.substring(1))
  }

  def getBooleanProp(name: String, default: Boolean): Boolean = {
    getProp(name)
      .map(s => s.equalsIgnoreCase("true") || s.equals("1"))
      .getOrElse(default)
  }

  def getProp(name: String): Option[String] = {
    sys.props.get(name) match {
      case Some(s) =>
        testlog.fine(s"Found system property for $name: $s")
        Some(s)
      case None    =>
        sys.env.get(name)
          .map { s =>
            testlog.fine(s"Found env variable for $name: $s")
            s
          }.orElse {
            testlog.fine(s"Did not find env variable or system property for $name")
            None
          }
    }
  }
}
