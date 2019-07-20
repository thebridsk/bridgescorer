package com.github.thebridsk.bridge.clientcommon.test

import org.scalatest.Sequential
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.annotation.JSExportTopLevel

/**
 * @author werewolf
 */
// @JSExportTopLevel("AllCommonUnitTests")
class AllCommonUnitTests extends Sequential(

    new TestColor

) {
  println( "Creating AllCommonUnitTests" )
}
