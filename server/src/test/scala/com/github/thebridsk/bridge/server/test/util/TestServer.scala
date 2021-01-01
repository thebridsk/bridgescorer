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
import java.net.InetAddress

/**
  * Keep track of all variables associated with the server,
  * and optionally start the bridgescorer server for testing
  *
  * The following variables affect the variables and the server,
  * they may be specified as environment variables or system properties.
  * If both are specified, the system properties is used.
  *
  * - **TestServerStart** (Boolean, default: `true`) start the test server
  * - **TestServerListen** (String, default: `http://localhost:8081`) the protocol, interface and port the server will listen on.
  *                        If port is not specified, then 8081 is used.
  *                        Only used when **TestServerStart** is true.
  * - **TestServerURL** (String, default: `${TestServerProtocol}://${TestServerListen}/`)
  *                     the URL to use to connect to the server.  Must end in "/"
  * - **TestServerFixHostInURL** (Boolean, default: `false`) fix the URL in TestServerURL
  *                              fixes the host in the URL to make sure it can be resolved.
  *                              This is required for SSL tests, since the host name is checked
  *                              in the SSL host certificate.
  * - **TestProductionPage** (Boolean, default: `false`) whether production pages are tested
  *
  * The [[start]] method has a parameter, *https* that defaults to false.  This
  * parameter determines whether the server is started with https.  If https is started,
  * it will be on port+443-80, where port is the http port in **TestServerListen**.
  * This parameter is ignored if the protocol in **TestServerListen** is `https`
  *
  */
object TestServer {

  val testlog: Logger = Logger(TestServer.getClass.getName)


  /**
    * Normalize the URL.
    *
    * Add protocol http if none is specified,
    * use port 8081 if none specified
    *
    * This must match same function in BridgeServer.scala
    */
  private def normalizeURL(url: String) = {
    val u = new URL(url)
    val port = {
      val p = u.getPort()
      if (p < 0) 8081
      else p
    }
    new URL(
      Option(u.getProtocol()).getOrElse("http"),
      u.getHost(),
      port,
      u.getFile()
    )
  }

  /**
    * Get the value of a URL system property or environment variable.
    *
    * If both system property and environment variable is set, the
    * system property value is used.
    *
    * Normalized the URL, this adds protocol http if none is specified,
    * use port 8081 if none specified.
    *
    * Checks for protocol being http or https
    *
    * optionally checks the file part of the URL to make sure it ends in a "/"
    *
    * @param name the name of the environment variable or system property
    * @param defaultValue the value to use if none is set
    * @param ignoreFile if true, then don't check if file part ends in "/"
    *
    * @throws Exception if the URL is not valid.
    *
    * This must match same function in BridgeServer.scala
    */
  private def getPropURL(
      name: String,
      defaultValue: String,
      ignoreFile: Boolean = false
  ): URL = {
    val v = getProp(name).getOrElse {
      testlog.fine(s"Using default value for ${name}: ${defaultValue}")
      defaultValue
    }
    try {
      val url = normalizeURL(v)
      val protocol = url.getProtocol()
      if (protocol != "http" && protocol != "https") {
        throw new Exception(s"${name} must be http or https, found ${protocol}: ${v}")
      }
      if (!ignoreFile && url.getFile() != "/") {
        throw new Exception(s"${name} must end in '/': ${v}")
      }
      url
    } catch {
      case x: Exception =>
        throw new Exception(s"Value ${name} is not a valid URL: ${v}\n${x}",x)
    }
  }

  private def endsInSlash(v: String ) = {
    if (v.endsWith("/")) v
    else s"$v/"
  }

  private def fixHostInURL(url: URL) = {
    if (getBooleanProp("TestServerFixHostInURL", false)) {
      try {
        val h = url.getHost()
        val hip = InetAddress.getByName(h)
        new URL(
          url.getProtocol(),
          hip.getHostAddress(),
          url.getPort(),
          url.getFile()
        )
      } catch {
        case x: Exception =>
          testlog.warning(s"Unable to fix host in ${url}", x)
          url
      }
    } else {
      url
    }
  }

  val testServerStart: Boolean = getBooleanProp("TestServerStart", true)
  /**
    * The protocol, interface and port that the server will listen on.
    *
    * This must match same val in BridgeServer.scala
    */
  val testServerListen: URL = getPropURL("TestServerListen", "http://localhost:8081", true)
  /**
    * The URL to use to connect to the server.
    *
    * This must match same val in BridgeServer.scala
    */
  val testServerURL: URL =
    fixHostInURL(getPropURL("TestServerURL", endsInSlash(testServerListen.toString())))

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

  private val hostname = testServerListen.getHost()
  private val port = testServerListen.getPort()
  private val isPortHttps = testServerListen.getProtocol()=="https"

  val hosturl = testServerURL.toString()

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
    if (testServerStart) {
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

  private def httpsContext = {
    StartServer.serverSSLContext(
      Some("abcdef"),
      Some("../server/key/examplebridgescorekeeper.jks")
    )
  }

  def start(https: Boolean = false): Unit = {
    MonitorTCP.startMonitoring()
    try {
      onlyRunWhenStartingServer(Some("Using existing server")) { () =>
        synchronized {
          if ((getAndIncrementStartCount()) == 0) {
            testlog.info(
              s"Starting TestServer https=${https}, testServerURL=${testServerURL}, useWebsocketLogging=${useWebsocketLogging}, backend.defaultLoggerConfig=${backend
                .getDefaultLoggerConfig(false)}"
            )
            Server.init()
            val (httpPort, httpsPort, connectionContext) =
              if (isPortHttps) {
                testlog.info(s"HTTPS port is ${port}")
                (None, Some(port), Some(httpsContext))
              } else if (https) {
                val httpsport = getHttpsPort
                testlog.info(s"HTTPS port is ${httpsport}, HTTP port is ${port}")
                (Some(port), Some(httpsport), Some(httpsContext))
              } else {
                testlog.info(s"HTTP port is ${port}")
                (Some(port), None, None)
              }
            val future = StartServer.start(
              interface = hostname,
              httpPort = httpPort,
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

  def isServerStartedByTest = testServerStart

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
