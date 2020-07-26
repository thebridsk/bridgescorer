package com.github.thebridsk.bridge.data.js

import com.github.thebridsk.bridge.data.SystemTime
import scala.scalajs.js.Date

import com.github.thebridsk.utilities.logging.impl.{ SystemTime => LogSystemTime }
import com.github.thebridsk.utilities.logging.impl.LoggerImplFactory
import com.github.thebridsk.bridge.clientcommon.react.DateUtils

class SystemTimeJs extends SystemTime with LogSystemTime {
  def currentTimeMillis(): Double = {
    val d = new Date
    d.getTime()
  }

  /**
   * @param time the time in milliseconds since 1/1/1970
   * @return the returned string has the format HH:mm:ss.SSS
   */
  def formatTime( time: Double ): String = {
    DateUtils.formatLogTime(time)
  }
}

object SystemTimeJs extends SystemTimeJs {

  def apply(): Unit = {

    SystemTime.setTimekeeper(this)
    LoggerImplFactory.setSystemTimeObject(this)
  }

}
