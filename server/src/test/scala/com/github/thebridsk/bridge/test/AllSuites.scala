package com.github.thebridsk.bridge.test

import com.github.thebridsk.bridge.test.selenium.AllSeleniumSuites
import org.scalatest.Sequential

/**
 * @author werewolf
 */
class AllSuites extends Sequential(

  new TestStartLogging,
  new AllUnitTests,
  new AllSeleniumSuites
)
