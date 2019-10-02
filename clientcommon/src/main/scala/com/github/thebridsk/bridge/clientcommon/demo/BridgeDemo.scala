package com.github.thebridsk.bridge.clientcommon.demo

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js

/**
 * @author werewolf
 */
@JSExportTopLevel("BridgeDemo")
object BridgeDemo {   // need to figure out how to use new way to call main

  def isDemo: Boolean = {
    val g = js.Dynamic.global.asInstanceOf[js.Dictionary[Boolean]]
    if ( g.contains("demo") ) {
      g("demo")
    } else {
      false
    }
  }

  def demoLogLevel: String = {
    val g = js.Dynamic.global.asInstanceOf[js.Dictionary[String]]
    if ( g.contains("demoLogLevel") ) {
      g("demoLogLevel")
    } else {
      ""
    }
  }

}
