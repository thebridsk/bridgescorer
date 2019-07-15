package com.github.thebridsk.bridge.test

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
    new TestSerialize
) {
  println( "Creating AllUnitTests" )
}
