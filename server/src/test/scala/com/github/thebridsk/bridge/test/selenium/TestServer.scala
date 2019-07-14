package com.github.thebridsk.bridge.test.selenium

import com.github.thebridsk.bridge.backend.BridgeServiceInMemory
import com.github.thebridsk.bridge.Server
import com.github.thebridsk.bridge.data.LoggerConfig
import com.github.thebridsk.utilities.logging.Logger
import java.util.logging.Level
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import com.github.thebridsk.bridge.test.util.MonitorTCP
import com.github.thebridsk.bridge.StartServer
import java.net.URL
import com.github.thebridsk.bridge.service.MyService
import scala.reflect.io.Directory
import com.github.thebridsk.bridge.backend.FileImportStore
import com.github.thebridsk.bridge.logging.RemoteLoggingConfig
import java.io.File
import com.github.thebridsk.bridge.backend.BridgeServiceWithLogging

object TestServer {

  val testlog = Logger(TestServer.getClass.getName)

  val useTestServerURL = getProp("UseBridgeScorerURL")
  val useTestServerScheme = getProp("UseBridgeScorerScheme")
  val useTestServerHost = getProp("UseBridgeScorerHost")
  val useTestServerPort = getProp("UseBridgeScorerPort")
  val useWebsocketLogging = getProp("UseWebsocketLogging")
  val envUseProductionPage = getBooleanProp("UseProductionPage",false)

  val useFastOptOnly = getBooleanProp("OnlyBuildDebug",false)

  val useFullOptOnly = getBooleanProp("UseFullOpt",false)

  val optRemoteLogger = getProp("OptRemoteLogger")
  val optBrowserRemoteLogging = getProp("OptBrowserRemoteLogging")
  val optIPadRemoteLogging = getProp("OptIPadRemoteLogging")

  val useProductionPage = useFullOptOnly || (envUseProductionPage && !useFastOptOnly)

  def loggingConfig(l: List[String]) = LoggerConfig( "[root]=ALL"::Nil, "console=INFO"::l)

  val backend = {
    val bs = new BridgeServiceInMemory("root") {

      override
      val importStore = {
        val importdir = Directory.makeTemp("importStore", ".dir")
        import scala.concurrent.ExecutionContext.Implicits.global
        Some( new FileImportStore( importdir ))
      }

    }

    var setConfig: Boolean = false

    val rlc = optRemoteLogger.flatMap { f =>
      val x = Option(RemoteLoggingConfig.readConfig(new File(f)))
      testlog.info(s"Maybe using ${f}: ${x}")
      setConfig = true
      x
    }.getOrElse {
      BridgeServiceWithLogging.getDefaultRemoteLoggerConfig().getOrElse(RemoteLoggingConfig())
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
      bs.setDefaultLoggerConfig( useWebsocketLogging match {
          case Some(v) =>
            testlog.info(s"useWebsocketLogging: ${v}")
            if (java.lang.Boolean.parseBoolean(v)) loggingConfig("websocket=ALL,bridge"::Nil)
            else loggingConfig("server=ALL"::Nil)
          case _ =>
            testlog.info(s"useWebsocketLogging was not set")
            loggingConfig("server=ALL"::Nil)
        }, false
      )
    }
    testlog.fine("Using browser logging config: "+bs.getDefaultLoggerConfig(false) )
    bs
  }

  val startingServer = useTestServerHost.isEmpty && useTestServerPort.isEmpty && useTestServerURL.isEmpty

  val scheme = useTestServerScheme.getOrElse("http")
  val interface = "localhost"          // the interface to start the server on
  val hostname = useTestServerHost.getOrElse(interface)   // the hostname for URLs
  val port = useTestServerPort match {
    case None => 8081
    case Some(sport) => sport.toInt
  }
  val schemeport = if (scheme=="http") 80 else 443
  val portuse = if (port==schemeport) "" else { ":"+port }
  val hosturl = useTestServerURL.getOrElse( scheme+"://"+hostname+portuse ) +"/"
  val pageprod = hosturl+"public/index.html"
  val pagedev = hosturl+"public/index-fastopt.html"
  val docs = hosturl+"v1/docs/"

  testlog.info( s"""Testing ${if (useProductionPage) "Prod" else "Dev"} pages""" )

  private var startCount = 0
  private def getAndIncrementStartCount() = synchronized {
    val r = startCount
    startCount += 1
    r
  }
  private def decrementAndGetStartCount() = synchronized {
    startCount -= 1
    startCount
  }

  def onlyRunWhenStartingServer(notRunMsg: Option[String] = None )
                               ( runWhenStarting: ()=>Unit ) = {
    if (startingServer) {
      runWhenStarting()
    } else {
      notRunMsg match {
        case Some(m) => testlog.fine(m)
        case None =>
      }
    }
  }

  private var myService: Option[MyService] = None

  def getMyService = myService.get

  def start() = {
    MonitorTCP.startMonitoring()
    onlyRunWhenStartingServer(Some("Using existing server")) { ()=>
      synchronized {
        if ( (getAndIncrementStartCount()) == 0 ) {
          testlog.info(s"Starting TestServer hosturl=${hosturl}, useWebsocketLogging=${useWebsocketLogging}, backend.defaultLoggerConfig=${backend.getDefaultLoggerConfig(false)}")
          Server.init()
          val future = StartServer.start(interface,Some(port),None,Some(backend))
          import scala.concurrent.ExecutionContext.Implicits.global
          future.map( s => myService = Some(s))
          Await.ready( future, 60 seconds )
        }
      }
    }
  }

  def stop() = {
    MonitorTCP.stopMonitoring()
    onlyRunWhenStartingServer() { ()=>
      synchronized {
        if ((decrementAndGetStartCount()) == 0) {
          Await.ready( StartServer.stopServer(), 60 seconds )
        }
      }
    }
  }

  def isServerStartedByTest = startingServer

  def getAppPage() = if (useProductionPage) getAppPageProd else  getAppPageDev
  def getAppPageUrl( uri: String ) = if (useProductionPage) getAppPageProdUrl(uri) else getAppPageDevUrl(uri)

  def getAppPageProd() = pageprod
  def getAppPageProdUrl( uri: String ) = if (uri.length()==0) pageprod else pageprod+"#"+uri

  def getAppPageDev() = pagedev
  def getAppPageDevUrl( uri: String ) = if (uri.length()==0) pagedev else pagedev+"#"+uri

  def getDocs() = docs
  def getDocs( fragment: String ) = if (fragment.length()==0) docs else docs+"#!"+fragment

  def getHelpPage( page: String = "" ) = hosturl+"help/"+page

  /**
   * Returns the URL for the given URI.
   * @param uri the URI.  Must start with a '/'
   * @throws IllegalArgumentException if the uri does not start with a '/'
   */
  def getUrl( uri: String ) = {
    if (uri.charAt(0) != '/') throw new IllegalArgumentException("uri must start with a '/'")
    new URL(hosturl+uri.substring(1))
  }

  def getBooleanProp( name: String, default: Boolean ) = {
    getProp(name).map(s => s.equalsIgnoreCase("true") || s.equals("1")).getOrElse(default)
  }

  def getProp( name: String ) = {
    sys.props.get(name) match {
      case Some(s) => Some(s)
      case None => sys.env.get(name)
    }
  }
}
