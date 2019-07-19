package com.github.thebridsk.bridge.test.utils

import com.github.thebridsk.bridge.clientcommon.logger.Init

object StartLogging {

  var initialized = false

  def start( level: String = "ALL" ) = {
    if (!initialized) {
      initialized = true
      Init.processHandlers( List( s"console=${level}" ) )
    }
  }
}
