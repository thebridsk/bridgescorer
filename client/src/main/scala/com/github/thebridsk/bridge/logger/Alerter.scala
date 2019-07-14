package com.github.thebridsk.bridge.logger

import com.github.thebridsk.utilities.logging.Logger
import java.io.StringWriter
import java.io.PrintWriter
import org.scalactic.source.Position
import com.github.thebridsk.bridge.source._
import org.scalajs.dom.raw.Event

trait Alerter {

  val log: Logger

  def isAlertToConsoleEnabled = log.isFineLoggable()
  def isThrowEnabled = log.isFinestLoggable()

  def tryit[T]( f: => T )( implicit pos: Position ) = {
    try {
      f
    } catch {

      case x: Throwable =>
        log.warning(s"Alerter(${pos.line}): ${x.toString()}", x)
        if (isAlertToConsoleEnabled) alert( s"Alerter(${pos.line}): ${x.toString()}\n${exceptionToString(x)}" )
        throw x
    }
  }

  def maybeThrow( x: Throwable ) = {
    if (isThrowEnabled) throw x
  }

  def tryitWithUnit[T]( f: => T )( implicit pos: Position ): Unit = {
    try {
      f
    } catch {

      case x: Throwable =>
        log.warning(s"Alerter(${pos.line}): ${x.toString()}", x)
        if (isAlertToConsoleEnabled) alert( s"Alerter(${pos.line}): ${x.toString()}\n${exceptionToString(x)}" )
        maybeThrow(x)
    }
  }

  def tryitWithDefault[T]( defaultValue: => T )( f: => T )( implicit pos: Position ): T = {
    try {
      f
    } catch {

      case x: Throwable =>
        log.warning(s"Alerter(${pos.line}): ${x.toString()}", x)
        if (isAlertToConsoleEnabled) alert( s"Alerter(${pos.line}): ${x.toString()}\n${exceptionToString(x)}" )
        maybeThrow(x)
        defaultValue
    }
  }

  def tryitfun[A]( f: A=>Unit )( implicit pos: Position ): A=>Unit = (a:A)=> tryitWithUnit { f(a) }

  def tryitfunr[A,R]( defaultValue: => R)( f: A=>R )( implicit pos: Position ) = (a:A)=> {
    tryitWithDefault[R](defaultValue) { f(a) }
  }

  def tryit[A,R]( f: A=>R )( implicit pos: Position ) = (a:A)=> {
    tryit[R] { f(a) }
  }

  def tryAlert( msg: String )( implicit pos: Position ) = {
    if (isAlertToConsoleEnabled) alert( s"Alerter(${pos.line}): msg" )
  }

  def tryAlert( msg: String, ex: Throwable )( implicit pos: Position ) = {
    if (isAlertToConsoleEnabled) alert( s"Alerter(${pos.line}): ${msg}\n${exceptionToString(ex)}" )
  }

  def exceptionToString( ex: Throwable ) = {
    val sw = new StringWriter
    val pw = new PrintWriter(sw)
    pw.append(ex.getClass.getName).append(": ")
    ex.printStackTrace(pw)
    pw.flush()
    sw.toString()
  }

  def alert( msg: String ) = {
    import org.scalajs.dom
    dom.window.alert( msg )
  }
}

object Alerter extends Alerter {

  lazy val log = Logger("bridge.logger.Alerter")


  /**
   * Called when an error occurs on page
   * @param message: error message (string). Available as event (sic!) in HTML onerror="" handler.
   * @param source: URL of the script where the error was raised (string)
   * @param lineno: Line number where error was raised (number)
   * @param colno: Column number for the line where the error occurred (number)
   * @param error: Error Object (object)
   * @return When the function returns true, this prevents the firing of the default event handler.
   */
  def onError5(message: Event, source: String, lineno: Int, colno: Int, error: Any): Boolean = {
    Alerter.alert(s"""onError ${source}: ${lineno}:${colno}\n${message}\nerror=${error}""")
    false
  }

  /**
   * Called when an error occurs on page
   * @param message: error message (string). Available as event (sic!) in HTML onerror="" handler.
   * @param source: URL of the script where the error was raised (string)
   * @param lineno: Line number where error was raised (number)
   * @param colno: Column number for the line where the error occurred (number)
   * @param error: Error Object (object)
   * @return When the function returns true, this prevents the firing of the default event handler.
   */
  def onError1(message: Event ): Boolean = {
    Alerter.alert(s"""onError\n${message}""")
    false
  }

  def setupError() = {
    val window = org.scalajs.dom.window
//    val window = org.scalajs.dom.document.defaultView
    window.onerror = onError5
  }

}

object CommAlerter extends Alerter {

  lazy val log = Logger("comm.logger.Alerter")
}
