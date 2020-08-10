package com.github.thebridsk.bridge.client.pages.duplicate

import com.github.thebridsk.bridge.data.util.Strings

object Utils {

  def toPointsString(pts: Double): String = {
    val ipts = pts.floor
    val fpts = pts - ipts
    val fract = if (fpts < 0.01) "" else Strings.half // 1/2
    if (ipts == 0 && fpts > 0) fract else ipts.toString + fract
  }

  def toString(v: Double): String = {
    import scala.scalajs.js.JSNumberOps._
    v.toFixed(2)
  }

  def toPctString(v: Double): String = {
    import scala.scalajs.js.JSNumberOps._
    v.toFixed(2)
  }
}
