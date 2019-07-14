package com.github.thebridsk.bridge.test

import org.scalatest.Sequential
import com.github.thebridsk.bridge.test.selenium.TravisSeleniumSuites

/**
 * @author werewolf
 */
class TravisAllSuites extends Sequential(

  new TestStartLogging,
  new AllUnitTests,
  new TravisSeleniumSuites
)
