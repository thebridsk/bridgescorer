package com.github.thebridsk.bridge.data

import io.swagger.v3.oas.annotations.media.Schema
import com.github.thebridsk.utilities.time.{SystemTime => USystemTime}

object SystemTime {
  @Schema(description = "Timestamp in milliseconds since 1/1/1970")
  type Timestamp = USystemTime.Timestamp

  def currentTimeMillis(): Timestamp = USystemTime.currentTimeMillis()

  def doubleToTimestamp(d: Double): USystemTime.Timestamp = d

  val UseCurrent: Timestamp = -1

  def defaultTime(created: Timestamp, updated: Timestamp): (Timestamp,Timestamp) = {
    if (created == UseCurrent || updated == UseCurrent) {
      val t = currentTimeMillis()
      val c = if (created == UseCurrent) t else created
      val u = if (updated == UseCurrent) t else updated
      (c,u)
    } else {
      (created,updated)
    }
  }
}
