package com.github.thebridsk.bridge.client.test

import org.scalatest.Sequential

/**
  * @author werewolf
  */
// @JSExportTopLevel("AllUnitTests")
class AllUnitTests
    extends Sequential(
      new MyTest,
      new TestDuplicateStore,
      new TestLogFilter,
      new TestSerialize
      // new TestRouter
    ) {
  println("Creating AllUnitTests")
}
