package com.example.data.js

import com.example.data.SystemTime
import scala.scalajs.js.Date

import utils.logging.impl.{ SystemTime => LogSystemTime }
import utils.logging.impl.LoggerImplFactory
import com.example.react.DateUtils

class SystemTimeJs extends SystemTime with LogSystemTime {
  def currentTimeMillis() = {
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

  def apply() = {

    SystemTime.setTimekeeper(this)
    LoggerImplFactory.setSystemTimeObject(this)
  }

}
