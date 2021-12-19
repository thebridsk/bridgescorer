package com.github.thebridsk.redux

import scala.scalajs.js

@js.native
trait MiddlewareAPI[S <: State] extends js.Any {
  val dispatch: js.Function1[Action, S] = js.native
  val getState: js.Function0[S] = js.native
}
