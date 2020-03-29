package com.github.thebridsk.bridge.client.test

import org.scalatest.Sequential
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.annotation.JSExportTopLevel

/**
 * @author werewolf
 */
// @JSExportTopLevel("AllUnitTests")
class AllUnitTests extends Sequential(

    new MyTest,
    new TestDuplicateStore,
    new TestLogFilter,
    new TestSerialize,
    // new TestRouter
) {
  println( "Creating AllUnitTests" )
}
