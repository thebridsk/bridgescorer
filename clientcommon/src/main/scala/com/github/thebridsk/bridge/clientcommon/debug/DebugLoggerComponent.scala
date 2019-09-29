package com.github.thebridsk.bridge.clientcommon.debug

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.clientcommon.react.DateUtils
import com.github.thebridsk.utilities.logging.TraceMsg
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.dispatcher.Dispatcher
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles.rootStyles
import com.github.thebridsk.utilities.logging.Level
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.logger.Init
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles.baseStyles


/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * DebugLoggerComponent( DebugLoggerComponent.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object DebugLoggerComponent {
  import DebugLoggerComponentInternal._

  case class Props()

  def apply( ) = component( Props() )

  def getLoggerName(forRemoteHandler: Boolean, loggername: String = "") =
    if (loggername.length() == 0) {
      if (forRemoteHandler) ("bridge","bridge") else ("","[root]")
    } else {
      (loggername,loggername)
    }

  def setLoggingLevel( loggername: String, level: Level ): Unit = {
    val (targetlog, targetdisp) = getLoggerName(false,loggername)
    val target = Logger(targetlog)
    target.getHandlers().find(h => h.isInstanceOf[DebugLoggerHandler]) match {
      case Some(h) =>
        logger.info( s"On ${targetdisp} setting debug logger trace level to ${level.name}" )
        h.level = level
      case None =>
        logger.info( s"On ${targetdisp} starting debug logger trace with level ${level.name}" )
        val h = new DebugLoggerHandler
        h.level = level
        Init.filterTraceSend(h)
        target.addHandler(h)
    }
  }

  def getLoggingLevel( loggername: String ) = {
    val (targetlog, targetdisp) = getLoggerName(false,loggername)
    val target = Logger(targetlog)
    target.getHandlers().find(h => h.isInstanceOf[DebugLoggerHandler]) match {
      case Some(h) =>
        h.level
      case None =>
        Level.OFF
    }
  }

  def init(
      loggername: String = "",
      l: Level = Level.FINEST
  ) = {
    setLoggingLevel(loggername,l)
  }

}

object DebugLoggerComponentInternal {
  import DebugLoggerComponent._

  val rootlogger = ""

  /** logger for use only in setting up the component, NOT for handling log messages */
  val logger = Logger("bridge.DebugLoggerComponent")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State(
    level: Level =  DebugLoggerComponent.getLoggingLevel(DebugLoggerComponentInternal.rootlogger)
  ) {
    def withLevel( l: Level ) = copy( level = l )
  }

  def format( msg: TraceMsg ): String = {
    var ret = s"${DateUtils.formatLogTime(msg.time)} ${msg.level.short} ${msg.pos.fileName}:${msg.pos.lineNumber} ${substitute(msg.message, msg.args:_*)}"
    if (msg.cause != null) {
      ret = ret + "\n"+Alerter.exceptionToString(msg.cause)
    }
    ret
  }

  def substitute(str: String, args: Any*) = {
    var ret = str
    for ( i <- 0 until args.length ) {
      val s = args(i)

      val r = s"""\{${i}\}"""
      ret = ret.replaceAll(r, s.toString())
    }
    ret
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def clearLogs() = Callback { Dispatcher.clearLogs() }
    def startLogs() = Callback { Dispatcher.startLogs() }
    def stopLogs() = Callback { Dispatcher.stopLogs() }

    def setLoggingLevelCB( level: Level ) = scope.modState { s =>
      setLoggingLevel(rootlogger,level)
      s.withLevel(level)
    }

    def render( props: Props, state: State ) = {
      val txt = LoggerStore.getMessages().map(m => m.i.toString+": "+format(m.traceMsg)).mkString("\n")
      val enabled = LoggerStore.isEnabled()
      <.div(
        rootStyles.logDiv,
        <.div(
          AppButton( "ClearLogs", "Clear Logs", ^.onClick --> clearLogs() ),
          AppButton( "StopLogs", "Stop Logs", ^.onClick --> stopLogs() ).when(enabled),
          AppButton( "StartLogs", "Start Logs", ^.onClick --> startLogs() ).unless(enabled),

          Level.allLevels.filter(l => l!=Level.STDERR && l!=Level.STDOUT).map { l =>
            val styl = if (state.level != l) baseStyles.normal
                       else baseStyles.buttonSelected
            AppButton( l.name, l.name, ^.onClick --> setLoggingLevelCB(l), styl ).when(enabled),
          }.toTagMod,
        ),
        <.div(
          txt
        )
      )
    }

    val storeCallback = scope.forceUpdate

    def didMount() = Callback {
      LoggerStore.addChangeListener(storeCallback)
      setLoggingLevel(rootlogger,getLoggingLevel(rootlogger))
    }

    def willUnmount() = Callback {
      LoggerStore.removeChangeListener(storeCallback)
    }

  }

  val component = ScalaComponent.builder[Props]("DebugLoggerComponent")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount())
                            .componentWillUnmount( scope => scope.backend.willUnmount() )
                            .build
}

