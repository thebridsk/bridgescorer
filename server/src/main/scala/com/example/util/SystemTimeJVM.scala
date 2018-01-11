package com.example.util

import com.example.data.SystemTime

object SystemTimeJVM {

  def apply() =
    SystemTime.setTimekeeper(new SystemTime {
      def currentTimeMillis() = System.currentTimeMillis()
    })

}