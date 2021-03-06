package com.github.thebridsk.bridge.clientcommon.rest2

import scala.concurrent.ExecutionContext

class ResultHolder[T] {
  private var result: Option[Cancellable[T]] = None

  def set(r: Cancellable[T])(implicit executor: ExecutionContext): Unit = {
    result = Some(r)
    r.onComplete { t =>
      result match {
        case Some(rr) if r eq rr => result = None
        case _                   =>
      }
    }
  }

  def isRunning = result.isDefined

  def cancel(): Boolean =
    result match {
      case Some(r) =>
        result = None
        r.cancel()
      case None =>
        false
    }
}

object ResultHolder {
  def apply[T]() = new ResultHolder[T]()
}
