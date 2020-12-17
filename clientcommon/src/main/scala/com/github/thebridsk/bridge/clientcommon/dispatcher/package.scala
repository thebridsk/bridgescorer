package com.github.thebridsk.bridge.clientcommon

import com.github.thebridsk.utilities.logging.TraceMsg
import com.github.thebridsk.bridge.data.ServerURL

package object dispatcher {

  trait Action

  case class PostLogEntry(traceMsg: TraceMsg) extends Action
  case class StopLogs() extends Action
  case class StartLogs() extends Action
  case class ClearLogs() extends Action

  case class ActionUpdateServerURLs(urls: ServerURL) extends Action

}
