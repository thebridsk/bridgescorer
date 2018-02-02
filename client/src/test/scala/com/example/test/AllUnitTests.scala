package com.example.test

import org.scalatest.Sequential
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.annotation.JSExportTopLevel

/**
 * @author werewolf
 */
@JSExportTopLevel("com.example.test.AllUnitTests")
class AllUnitTests extends Sequential(

    new MyTest,
    new TestDuplicateStore,
    new TestLogFilter,
    new TestSerialize

) {
  println( "Creating AllUnitTests" )
}
