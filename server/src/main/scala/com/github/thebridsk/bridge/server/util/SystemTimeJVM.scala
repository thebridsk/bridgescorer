package com.github.thebridsk.bridge.server.util

import com.github.thebridsk.bridge.data.SystemTime

object SystemTimeJVM {

  def apply() =
    SystemTime.setTimekeeper(new SystemTime {
      def currentTimeMillis() = System.currentTimeMillis().toDouble
    })

}
