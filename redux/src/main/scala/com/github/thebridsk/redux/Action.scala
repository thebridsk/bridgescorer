package com.github.thebridsk.redux

import scala.scalajs.js
import scala.language.implicitConversions

trait Action {
  val actiontype: String
}

@js.native
trait NativeAction[+A <: Action] extends js.Object {

  @js.annotation.JSName("type")
  val actiontype: String = js.native
  @js.annotation.JSName("action")
  val actionInternal: js.UndefOr[A] = js.native
}

object NativeAction {

  def apply[A <: Action](action: A): NativeAction[A] = {
    val p = js.Dynamic.literal(
      "type" -> action.actiontype,
      "action" -> action.asInstanceOf[js.Object]
    )
    p.asInstanceOf[NativeAction[A]]
  }

  def simple(actiontype: String): NativeAction[Nothing] = {
    val p = js.Dynamic.literal(
      "type" -> actiontype,
    )
    p.asInstanceOf[NativeAction[Nothing]]
  }

  implicit def actionToNative[A <: Action](action: A): NativeAction[A] = apply(action)

  implicit class wrapNativeAction[A <: Action](val a: NativeAction[A]) extends AnyVal {
    def action = a.actionInternal
  }
}
