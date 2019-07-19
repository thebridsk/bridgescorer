package com.github.thebridsk.bridge.server.test

import com.github.thebridsk.bridge.server.test.selenium.AllSeleniumSuites
import org.scalatest.Sequential

/**
 * @author werewolf
 */
class AllSuites extends Sequential(

  new TestStartLogging,
  new AllUnitTests,
  new AllSeleniumSuites
)
