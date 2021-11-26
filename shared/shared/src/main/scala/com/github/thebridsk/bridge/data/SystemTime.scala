package com.github.thebridsk.bridge.data

import io.swagger.v3.oas.annotations.media.Schema

object SystemTime {
  @Schema(description = "Timestamp in milliseconds since 1/1/1970")
  type Timestamp = Double

  private var timekeeper = new SystemTime {
    def currentTimeMillis(): Timestamp = 0
  }

  def currentTimeMillis(): Timestamp = timekeeper.currentTimeMillis()

  def doubleToTimestamp(d: Double): SystemTime.Timestamp = d

  def setTimekeeper(t: SystemTime): Unit = {
    timekeeper = t
  }

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

trait SystemTime {
  def currentTimeMillis(): SystemTime.Timestamp
}
