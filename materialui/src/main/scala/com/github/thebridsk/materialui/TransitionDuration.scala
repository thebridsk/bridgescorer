package com.github.thebridsk.materialui

import scala.scalajs.js

@js.native
trait TransitionDuration extends js.Object {

  val enter: js.UndefOr[Double] = js.native

  val exit: js.UndefOr[Double] = js.native
}
object TransitionDuration {

  def apply() = new js.Object().asInstanceOf[TransitionDuration]

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
