package com.example.logger

import com.example.data.LoggerConfig
import com.example.rest2.RestClientLoggerConfig
import scala.scalajs.js
import scala.scalajs.js.Object
import utils.logging.Logger
import utils.logging.impl.LoggerImplFactory
import utils.logging.js.JsConsoleHandler
import utils.logging.Level
import scala.reflect.ClassTag
import utils.logging.Handler
import com.example.pages.info.InfoPage
import org.scalactic.source.Position
import com.example.source._
import java.io.StringWriter
import java.io.PrintWriter
import com.example.debug.DebugLoggerHandler
import utils.logging.js.JsConsoleHandlerInfo
import com.example.controller.Controller
import com.example.rest2.AjaxResult
import com.example.Bridge
import utils.logging.Filter
import utils.logging.TraceMsg

/**
 * Logging manager.
 *
 * Use the apply() method to initialize the system.  This will make a
 * call to the server to get the logging configuration.  The configuration is
 * applied once it is received.
 *
 * The com.example.data.LoggerConfig object, obtained from the server, contains the configuration.
 *
 * This has two fields, one to configure the loggers, the other to configure the handlers.
 *
 * The logger configuration is a list of string values, where each string has the following syntax:
 *
 * <code><pre>
 * loggerstring := lname "=" level
 * lname        :=   // the loggername
 * level        := "OFF"|"SEVERE"|"WARNING"|"INFO"|"CONFIG"|"FINE"|"FINER"|"FINEST"|"ALL"
 * </pre></code>
 *
 * The handler configuration is a list of string values, where each string has the following syntax:
 *
 * <code><pre>
 * handlerstring := hname "=" level [ "," loggername ]
 * hname         := "console"|"server"|"websocket"
 * level         := "OFF"|"SEVERE"|"WARNING"|"INFO"|"CONFIG"|"FINE"|"FINER"|"FINEST"|"ALL"
 * loggername    :=   // the loggername
 * </pre></code>
 *
 *
 * @author werewolf
 */
object Init {
  {
    val handler = new JsConsoleHandler
    LoggerImplFactory.init(handler)
    if (Bridge.isDemo) {
      handler.level = Level.ALL
      handler.filter = new Filter {
        def isLogged( traceMsg: TraceMsg ) = {
          !traceMsg.logger.startsWith("comm.")
        }
      }
    }
  }
  lazy val logger = Logger("comm.logger.Init")

  private var debugLoggerEnabled = false

  private var pclientid: Option[String] = None

  def clientid = pclientid

  def isDebugLoggerEnabled = debugLoggerEnabled

  val defaultLoggerForRemoteHandlers = "bridge"

  def noop() = {}

  def apply( f: ()=>Unit = noop _ ): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    if (AjaxResult.isEnabled.getOrElse(false)) {
      RestClientLoggerConfig.get("").foreach( config => CommAlerter.tryit {
        logger.info("Got "+config)
        pclientid = config.clientid
        config.useRestToServer.map( b => Controller.useRestToServer = b )
        config.useSSEFromServer.map( b => Controller.useSSEFromServer = b )
        if (config.loggers.length > 0) {
          // set trace levels in loggers to config.loggers
          processLoggers(config.loggers)
        }

        if (config.appenders.length>0) {
          // set handler levels in handlers to config.appenders
          processHandlers(config.appenders)
        }

        import scala.scalajs.js.timers._

        setTimeout(1000) {
          InfoPage.info()

          f()
        }
      })
    } else {
      import scala.scalajs.js.timers._

      setTimeout(1000) {
        InfoPage.info()

        f()
      }
    }

  }

  import scala.language.postfixOps

  val loggerSpec = """([^=]+)=(.*)"""r

    // LoggerConfig( "[root]=ALL"::Nil, "console=INFO"::"server=ALL"::Nil)
  def processLoggers( spec: List[String] ) = {
    spec.foreach(s => {
      s match {
        case loggerSpec(name,level) =>
          val n = if (name == "[root]") "" else name
          Level.toLevel(level) match {
            case Some(l) => Logger(n).setLevel(l)
            case None =>
              logger.warning("Unknown logger level: %s",s)
          }
        case _ =>
          logger.warning("Unknown logger specification: %s",s)
      }
    })
  }

  val handlerSpec = """([^=]+)=([^,]*),(.*)"""r
  val handler2Spec = """([^=]+)=([^,]*)"""r

    // LoggerConfig( "[root]=ALL"::Nil, "console=INFO"::"server=ALL"::Nil)
  def processHandlers( spec: List[String] ) = {
    spec.foreach(s => {
      s match {
        case handlerSpec(name,level,loggername) => setHandler(s,name,level,loggername)
        case handler2Spec(name,level) => setHandler(s,name,level,"")
        case _ => logger.warning("Unknown handler specification: %s",s)
      }
    })
  }

  def filterTraceSend( h: Handler ): Unit = {
    val f = new Filter() {
      def isLogged( traceMsg: TraceMsg ): Boolean = {
        traceMsg.pos.fileName!="AjaxCall.scala" || !traceMsg.message.startsWith("PUT /v1/logger/entry")
      }
    }
    h.filter = f
  }

  def getLoggerName( forRemoteHandler: Boolean, loggername: String = "" ) = if (loggername.length()==0) {
    if (forRemoteHandler) "bridge" else "[root]"
  } else {
    loggername
  }

  def startMaybeDebugLogging(ignoreIfStarted: Boolean = false, loggername: String = "", l: Level = Level.FINEST) = {
    if (!ignoreIfStarted || !debugLoggerEnabled) {
      debugLoggerEnabled = true
      val target = Logger(loggername)
      target.getHandlers().find(h=>h.isInstanceOf[DebugLoggerHandler]) match {
        case Some(h) =>
          logger.info("On %s setting debug logger trace level to %s",getLoggerName(false,loggername),l)
          h.level=l
        case None =>
          logger.info("On %s starting debug logger trace with level %s",getLoggerName(false,loggername),l)
          val h = new DebugLoggerHandler
          h.level = l
          target.addHandler(h)
          filterTraceSend(h)
      }
    }
  }

  def setHandler( spec: String, name: String, level: String, loggername: String ) = {
    val target = Logger(loggername)
    def getLoggerName( forRemoteHandler: Boolean ) = if (loggername.length()==0) {
      if (forRemoteHandler) "bridge" else "[root]"
    } else {
      loggername
    }
    Level.toLevel(level) match {
      case Some(l) =>
        name match {
          case "con" =>
            target.getHandlers().find(h=>h.isInstanceOf[JsConsoleHandlerInfo]) match {
              case Some(h) => target.removeHandler(h)
              case None =>
            }
            target.getHandlers().find(h=>h.isInstanceOf[JsConsoleHandler]) match {
              case Some(h) =>
                logger.info("On %s setting console trace level to %s",getLoggerName(false),l)
                h.level=l
                filterTraceSend(h)
              case None =>
                logger.info("On %s starting console trace with level %s",getLoggerName(false),l)
                val h = new JsConsoleHandler
                h.level = l
                target.addHandler(h)
            }
          case "console" =>
            target.getHandlers().find(h=>h.isInstanceOf[JsConsoleHandler]) match {
              case Some(h) => target.removeHandler(h)
              case None =>
            }
            target.getHandlers().find(h=>h.isInstanceOf[JsConsoleHandlerInfo]) match {
              case Some(h) =>
                logger.info("On %s setting console trace level to %s",getLoggerName(false),l)
                h.level=l
              case None =>
                logger.info("On %s starting console trace with level %s",getLoggerName(false),l)
                val h = new JsConsoleHandlerInfo
                h.level = l
                target.addHandler(h)
                filterTraceSend(h)
            }
          case "debug" =>
            startMaybeDebugLogging(true, loggername, l)
          case "server" =>
            target.getHandlers().find(h=>h.isInstanceOf[SendToServerHandler]) match {
              case Some(h) =>
                logger.info("On %s setting SendToServer trace level to %s",getLoggerName(true),l)
                h.level=l
              case None =>
                logger.info("On %s starting SendToServer trace with level %s",getLoggerName(true),l)
                val h = new SendToServerHandler
                h.filter = LogFilter()
                h.level = l
                target.addHandler(h)
            }
          case "websocket" =>
            target.getHandlers().find(h=>h.isInstanceOf[SendToWebsocketHandler]) match {
              case Some(h) =>
                logger.info("On %s setting SendToWebsocket trace level to %s",getLoggerName(true),l)
                h.level=l
              case None =>
                logger.info("On %s starting SendToWebsocket trace with level %s",getLoggerName(true),l)
                val h = new SendToWebsocketHandler
                h.filter = LogFilter()
                h.level = l
                target.addHandler(h)
            }
          case _ =>
            logger.warning("Unknown handler: %s",spec)
        }
      case None =>
        logger.warning("Unknown logger level: %s",spec)
    }
  }

}
