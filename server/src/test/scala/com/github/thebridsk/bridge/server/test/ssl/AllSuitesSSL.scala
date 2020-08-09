package com.github.thebridsk.bridge.server.test.ssl

import org.scalatest.Sequential
import com.github.thebridsk.bridge.server.test.TestStartLogging

/**
  * @author werewolf
  */
class AllSuitesSSL
    extends Sequential(
      new TestStartLogging,
      new SSLTest
    )
