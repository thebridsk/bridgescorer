package com.github.thebridsk.bridge.client.test.utils

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import org.scalajs.dom.Element
import scala.scalajs.js.annotation.JSBracketAccess

@js.native
trait JQuery extends js.Object {
  def find(selector: String): JQuery = js.native

  val length: Int = js.native
  @JSBracketAccess
  def apply(x: Int): Element = js.native

  def text(): String = js.native

  def attr(name: String): js.UndefOr[String] = js.native
}

@JSImport("jquery", JSImport.Default, globalFallback = "$")
@js.native
object JQuery extends JQuery {

  def apply(e: Element): JQuery = js.native
}
