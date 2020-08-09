package com.github.thebridsk.bridge.fullserver.test

import org.scalatest.Sequential
import com.github.thebridsk.bridge.fullserver.test.selenium.TravisSeleniumSuites
import com.github.thebridsk.bridge.server.test.TestStartLogging

/**
  * @author werewolf
  */
class TravisAllSuites
    extends Sequential(
      new TestStartLogging,
      new AllUnitTests,
      new TravisSeleniumSuites
    )
