package com.github.thebridsk.bridge.fullserver.test

import org.scalatest.Sequential
import com.github.thebridsk.bridge.server.test.TestStartLogging

/**
 * @author werewolf
 */
class AllUnitTests extends Sequential(

  new TestStartLogging,
  new MyServiceSpec,
  new Swagger2Spec,
  new TestGetFromResource,
  new TestWebJarFinder,
)
