package com.example.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native @JSGlobal
class TransitionDuration extends js.Object {

  val enter: js.UndefOr[Double] = js.native

  val exit: js.UndefOr[Double] = js.native
}
object TransitionDuration {

  def apply(
      enter: js.UndefOr[Double] = js.undefined,
      exit: js.UndefOr[Double] = js.undefined
  ) = {
    val p = js.Dynamic.literal()

    enter.foreach(p.updateDynamic("enter")(_))
    exit.foreach(p.updateDynamic("exit")(_))

    p.asInstanceOf[TransitionDuration]
  }
}
