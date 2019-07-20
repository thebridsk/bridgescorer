package com.github.thebridsk.bridge.clientcommon.react

import com.github.thebridsk.bridge.data.SystemTime
import scala.scalajs.js

object DateUtils {

  val months = Array( "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" )

  def formatDate( time: SystemTime.Timestamp ): String = {
    val d = new js.Date(time)

    if (true) {
      val month = d.getMonth()
      val mon = months(month)
      val day = d.getDate()
      val year = d.getFullYear()
      val hour = d.getHours()
      val h = if (hour >= 12) hour-12 else hour
      val ampm = if (hour == h) "AM" else "PM"
      val hh = if (h==0) 12 else h
      val min = d.getMinutes()
      val sec = d.getSeconds()
      f"$mon%s $day%d, $year%d $hh%d:$min%02d:$sec%02d $ampm%s"
    } else {

      d.toLocaleDateString()+" "+d.toLocaleTimeString()
    }
  }

  def formatTime( time: SystemTime.Timestamp ): String = {
    val d = new js.Date(time)

    if (true) {
      val hour = d.getHours()
      val h = if (hour >= 12) hour-12 else hour
      val ampm = if (hour == h) "AM" else "PM"
      val hh = if (h==0) 12 else h
      val min = d.getMinutes()
      val sec = d.getSeconds()
      f"$hh%d:$min%02d:$sec%02d $ampm%s"
    } else {

      d.toLocaleDateString()+" "+d.toLocaleTimeString()
    }
  }

  def formatLogTime( time: SystemTime.Timestamp ): String = {
    val d = new js.Date(time)

    if (true) {
      val hour = d.getHours()
      val min = d.getMinutes()
      val sec = d.getSeconds()
      val milli = d.getMilliseconds()
      f"$hour%d:$min%02d:$sec%02d.$milli%03d"
    } else {

      d.toLocaleDateString()+" "+d.toLocaleTimeString()
    }
  }

  def formatDay( time: SystemTime.Timestamp ): String = {
    val d = new js.Date(time)

    if (true) {
      val month = d.getMonth()
      val mon = months(month)
      val day = d.getDate()
      val year = d.getFullYear()
      f"$mon%s $day%d, $year%d"
    } else {

      d.toLocaleDateString()+" "+d.toLocaleTimeString()
    }
  }

  def showDate( time: SystemTime.Timestamp ): String = {
    if (time == 0) ""
    else formatDate(time)
  }

  def showDay( time: SystemTime.Timestamp ): String = {
    if (time == 0) ""
    else formatDay(time)
  }

}
