package com.github.thebridsk.bridge.clientcommon.demo

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js

/**
 * @author werewolf
 */
@JSExportTopLevel("BridgeDemo")
object BridgeDemo {   // need to figure out how to use new way to call main

  def setDemo( demo: Boolean ) = {
    // val g = js.Dynamic.global.asInstanceOf[js.Dictionary[Boolean]]
    // g("demo") = demo
    js.Dynamic.global.demo = demo
  }

  def isDemo: Boolean = {
    // val g = js.Dynamic.global.asInstanceOf[js.Dictionary[Boolean]]
    // if ( g.contains("demo") ) {
    //   g("demo")
    // } else {
    //   false
    // }
    if (js.typeOf(js.Dynamic.global.demo) == "boolean") {
      val f: Boolean = js.Dynamic.global.demo.asInstanceOf[Boolean]
      f
    } else {
      false
    }
  }

  def demoLogLevel: String = {
    // val g = js.Dynamic.global.asInstanceOf[js.Dictionary[String]]
    // if ( g.contains("demoLogLevel") ) {
    //   g("demoLogLevel")
    // } else {
    //   ""
    // }
    if (js.typeOf(js.Dynamic.global.demoLogLevel) == "string") {
      val f: String = js.Dynamic.global.demoLogLevel.toString()
      f
    } else {
      ""
    }
  }

}
