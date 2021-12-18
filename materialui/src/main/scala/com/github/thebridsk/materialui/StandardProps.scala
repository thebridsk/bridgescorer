package com.github.thebridsk.materialui

import scala.scalajs.js
import japgolly.scalajs.react._

@js.native
trait StandardProps extends js.Any {
  val className: js.UndefOr[String] = js.native
  val onClick: js.UndefOr[ReactEvent => Unit] = js.native
  val sx: js.UndefOr[js.Dictionary[js.Any]] = js.native
}
