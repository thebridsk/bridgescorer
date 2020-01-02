package com.github.thebridsk.bridge.server.test

import org.scalatest.Sequential

/**
 * @author werewolf
 */
class AllSuites extends Sequential(

  new TestStartLogging,
  new AllUnitTests,
)
