package com.github.thebridsk.bridge.server.test

import org.scalatest.Sequential
import com.github.thebridsk.bridge.server.test.selenium.TravisSeleniumSuites

/**
 * @author werewolf
 */
class TravisAllSuites extends Sequential(

  new TestStartLogging,
  new AllUnitTests,
  new TravisSeleniumSuites
)
