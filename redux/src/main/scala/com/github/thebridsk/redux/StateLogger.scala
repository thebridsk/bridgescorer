package com.github.thebridsk.redux

import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.javascript.ObjectToString
import scala.scalajs.js

object StateLogger {

  private val log: Logger = Logger("redux")

  // val logger: Middleware[State] = loggerfn _

  def logger[S <: State](
    middlewareAPI: MiddlewareAPI[S]
  )(
    next: Dispatch[S]
  )(
    action: js.Any
  ): js.Any = {
    js.typeOf(action) match {
      case "object" =>
        log.info(s"dispatching ${ObjectToString.objToString(action.asInstanceOf[js.Object], "  ")}")
      case "function" =>
        log.info(s"dispatching thunk ${action}")
      case _ =>
        log.info(s"dispatching js.Any ${action}")
    }
    val ret = next(action.asInstanceOf[NativeAction[Action]])
    log.info(s"state after dispatch: ${ObjectToString.objToString(middlewareAPI.getState(), "  ")}")
    // log.info(s"state after dispatch: ${JSON.stringify(middlewareAPI.getState())}")
    ret
  }
}
