package com.github.thebridsk.bridge.server.test

import org.scalatest.Sequential

/**
 * @author werewolf
 */
class TravisAllSuites extends Sequential(

  new TestStartLogging,
  new AllUnitTests,
)
