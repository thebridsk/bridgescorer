package com.example.debug

import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.react.DateUtils
import utils.logging.TraceMsg
import com.example.logger.Alerter
import com.example.react.AppButton
import com.example.bridge.action.BridgeDispatcher

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

}

object DebugLoggerComponentInternal {
  import DebugLoggerComponent._
  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State() {

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

    def clearLogs() = Callback { BridgeDispatcher.clearLogs() }
    def startLogs() = Callback { BridgeDispatcher.startLogs() }
    def stopLogs() = Callback { BridgeDispatcher.stopLogs() }

    def render( props: Props, state: State ) = {
      val txt = LoggerStore.getMessages().map(m => m.i.toString+": "+format(m.traceMsg)).mkString("\n")
      val enabled = LoggerStore.isEnabled()
      <.div(
        AppButton( "ClearLogs", "Clear Logs", ^.onClick --> clearLogs() ),
        AppButton( "StopLogs", "Stop Logs", ^.onClick --> stopLogs() ).when(enabled),
        AppButton( "StartLogs", "Start Logs", ^.onClick --> startLogs() ).unless(enabled),

        <.pre(
          <.code(
            txt
          )
        )
      )
    }

    val storeCallback = scope.forceUpdate

    def didMount() = Callback {
      LoggerStore.addChangeListener(storeCallback)
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

