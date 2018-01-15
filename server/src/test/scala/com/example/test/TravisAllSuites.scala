package com.example.test

import org.scalatest.Sequential
import com.example.test.selenium.TravisSeleniumSuites

/**
 * @author werewolf
 */
class TravisAllSuites extends Sequential(

  new TestStartLogging,
  new AllUnitTests,
  new TravisSeleniumSuites
)
