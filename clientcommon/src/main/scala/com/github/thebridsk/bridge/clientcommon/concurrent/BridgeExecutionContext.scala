package com.github.thebridsk.bridge.clientcommon

// import scala.concurrent.ExecutionContext
import org.scalajs.macrotaskexecutor.MacrotaskExecutor
import scala.concurrent.ExecutionContext

object BridgeExecutionContext {
  // implicit val global = ExecutionContext.Implicits.global

  implicit val global: ExecutionContext = MacrotaskExecutor.Implicits.global
}
