package com.example.test.selenium

import com.example.backend.BridgeServiceInMemory
import com.example.Server
import com.example.data.LoggerConfig
import utils.logging.Logger
import java.util.logging.Level
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import com.example.test.util.MonitorTCP
import com.example.StartServer
import java.net.URL
import com.example.service.MyService

object TestServer {

  val testlog = Logger(TestServer.getClass.getName)

  val useTestServerURL = getProp("UseBridgeScorerURL")
  val useTestServerScheme = getProp("UseBridgeScorerScheme")
  val useTestServerHost = getProp("UseBridgeScorerHost")
  val useTestServerPort = getProp("UseBridgeScorerPort")
  val useWebsocketLogging = getProp("UseWebsocketLogging")
  val useProductionPage = getProp("UseProductionPage")

  def loggingConfig(l: List[String]) = LoggerConfig( "[root]=ALL"::Nil, "console=INFO"::l)

  val backend = {
    val bs = new BridgeServiceInMemory
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
    testlog.fine("Using browser logging config: "+bs.getDefaultLoggerConfig(false) )
    bs
  }

  val startingServer = useTestServerHost.isEmpty && useTestServerPort.isEmpty && useTestServerURL.isEmpty

  val scheme = useTestServerScheme.getOrElse("http")
  val interface = "loopback"          // the interface to start the server on
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

  val isProductionPage = useProductionPage.map(v => v=="true" || v=="1").getOrElse(false)

  testlog.info( s"""Testing ${if (isProductionPage) "Prod" else "Dev"} pages""" )

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

  def getAppPage() = if (isProductionPage) getAppPageProd else  getAppPageDev
  def getAppPageUrl( uri: String ) = if (isProductionPage) getAppPageProdUrl(uri) else getAppPageDevUrl(uri)

  def getAppPageProd() = pageprod
  def getAppPageProdUrl( uri: String ) = if (uri.length()==0) pageprod else pageprod+"#"+uri

  def getAppPageDev() = pagedev
  def getAppPageDevUrl( uri: String ) = if (uri.length()==0) pagedev else pagedev+"#"+uri

  def getDocs() = docs
  def getDocs( fragment: String ) = if (fragment.length()==0) docs else docs+"#!"+fragment

  /**
   * Returns the URL for the given URI.
   * @param uri the URI.  Must start with a '/'
   * @throws IllegalArgumentException if the uri does not start with a '/'
   */
  def getUrl( uri: String ) = {
    if (uri.charAt(0) != '/') throw new IllegalArgumentException("uri must start with a '/'")
    new URL(hosturl+uri.substring(1))
  }

  def getProp( name: String ) = {
    sys.props.get(name) match {
      case Some(s) => Some(s)
      case None => sys.env.get(name)
    }
  }
}
