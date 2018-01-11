package com.example.data

object SystemTime {
  type Timestamp = Double

  private var timekeeper = new SystemTime {
      def currentTimeMillis(): Timestamp = 0
  }

  def currentTimeMillis(): Timestamp = timekeeper.currentTimeMillis()

  def doubleToTimestamp( d: Double ): SystemTime.Timestamp = d

  def setTimekeeper( t: SystemTime ): Unit = {
    timekeeper = t
  }
}

trait SystemTime {
  def currentTimeMillis(): SystemTime.Timestamp
}
