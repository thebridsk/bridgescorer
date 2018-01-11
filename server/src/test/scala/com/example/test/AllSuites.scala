package com.example.test

import com.example.test.selenium.AllSeleniumSuites
import org.scalatest.Sequential

/**
 * @author werewolf
 */
class AllSuites extends Sequential(

  new TestStartLogging,
  new AllUnitTests,
  new AllSeleniumSuites
)
