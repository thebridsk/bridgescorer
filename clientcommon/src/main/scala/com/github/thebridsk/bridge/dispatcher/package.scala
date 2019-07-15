package com.github.thebridsk.bridge

import com.github.thebridsk.utilities.logging.TraceMsg

package object dispatcher {

  trait Action

  case class PostLogEntry( traceMsg: TraceMsg ) extends Action
  case class StopLogs() extends Action
  case class StartLogs() extends Action
  case class ClearLogs() extends Action

}
