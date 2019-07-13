package com.github.thebridsk.bridge.util

import com.github.thebridsk.bridge.data.SystemTime

object SystemTimeJVM {

  def apply() =
    SystemTime.setTimekeeper(new SystemTime {
      def currentTimeMillis() = System.currentTimeMillis()
    })

}
