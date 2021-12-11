package com.github.thebridsk.bridge.clientcommon

import scala.concurrent.ExecutionContext

object BridgeExecutionContext {
  implicit val global = ExecutionContext.Implicits.global
}
