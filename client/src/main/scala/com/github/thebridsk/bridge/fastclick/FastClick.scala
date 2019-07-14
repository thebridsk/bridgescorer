//package com.github.thebridsk.bridge.fastclick

//import scala.scalajs.js
//import scala.scalajs.js.annotation.JSImport
//import org.scalajs.dom.raw.HTMLElement
//
//@js.native
//trait RootFastClick extends js.Any {
//  def attach( layer: HTMLElement, options: js.Object ): FastClick = js.native
//  def notNeeded( layer: HTMLElement ): Unit = js.native
//}
//
//@JSImport("fastclick", JSImport.Namespace)
//@js.native
//object RootFastClick extends RootFastClick
//
//@js.native
//trait FastClick extends js.Any {
//  def destroy(): Unit = js.native
//}
//
//object FastClick {
//  def apply() = {
//    val doc = org.scalajs.dom.document
//    val body = doc.body
//    val options = js.Dynamic.literal()
//    RootFastClick.attach(body, options)
//  }
//}
