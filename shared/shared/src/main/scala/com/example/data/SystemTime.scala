package com.example.data

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
}

trait SystemTime {
  def currentTimeMillis(): SystemTime.Timestamp
}
