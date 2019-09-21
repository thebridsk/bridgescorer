package com.github.thebridsk.bridge.server.test.util

import com.github.thebridsk.utilities.logging.InMemoryHandler
import java.util.logging.Logger
import java.util.logging.LogManager
import java.util.logging.Level

class CaptureLog( private var enabled: Boolean = false, level: Level = Level.ALL ) {

  private var handler: Option[InMemoryHandler] = None

  def enable = enabled = true
  def disable = enabled = false

  private def getRootLogger(): Logger = {
    LogManager.getLogManager().getLogger("")
  }

  def startCapture() = if (enabled) synchronized {
    handler = Some( new InMemoryHandler)
    handler.get.setLevel(level)
    getRootLogger().addHandler(handler.get)
  }

  def stopCapture() = synchronized {
    handler.foreach( getRootLogger().removeHandler(_) )
    handler = None
  }

  def printLogOnException[T]( fun: => T ): T = {

    startCapture()
    try {
      fun
    } catch {
      case x: Throwable =>
        handler.foreach{ h =>
          printLogs(h)
        }
        throw x
    } finally {
      handler.foreach( _.clear() )
      stopCapture()
    }
  }

  def printLogOnCompletion[T]( fun: => T ): T = {

    startCapture()
    try {
      fun
    } finally {
      handler.foreach { h =>
        printLogs(h)
        h.clear()
      }
      stopCapture()
    }
  }

  private def printLogs( h: InMemoryHandler ) = {
    println( "---Captured logs---")
    println( h.getLog() )
    println( "---End captured logs---")
  }
}
