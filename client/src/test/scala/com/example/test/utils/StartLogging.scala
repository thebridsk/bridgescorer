package com.example.test.utils

import com.example.logger.Init

object StartLogging {

  var initialized = false

  def start( level: String = "ALL" ) = {
    if (!initialized) {
      initialized = true
      Init.processHandlers( List( s"console=${level}" ) )
    }
  }
}
