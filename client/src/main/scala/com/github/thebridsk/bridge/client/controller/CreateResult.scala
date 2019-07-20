package com.github.thebridsk.bridge.client.controller

import scala.concurrent.duration.Duration
import scala.concurrent.CanAwait
import com.github.thebridsk.bridge.clientcommon.rest2.Result
import scala.concurrent.ExecutionContext
import scala.util.Try
import com.github.thebridsk.utilities.logging.Logger
import scala.concurrent.Future
import com.github.thebridsk.bridge.clientcommon.rest2.ResultRecorder
import org.scalactic.source.Position
import com.github.thebridsk.bridge.clientcommon.rest2.RestResult
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxResult
import com.github.thebridsk.bridge.clientcommon.rest2.WrapperXMLHttpRequest

abstract class CreateResult[T](
                                ajaxResult: AjaxResult[WrapperXMLHttpRequest],
                                future: Future[T]
                              )(
                                implicit
                                  pos: Position,
                                  executor: ExecutionContext
                              ) extends RestResult[T](ajaxResult,future) {

  def this( result: RestResult[T] )(
                                implicit
                                  pos: Position,
                                  executor: ExecutionContext
                              ) = {
    this( result.ajaxResult, result.future )
  }

  private var storeUpdated = false

  future.onComplete( t => update(t) )

  def updateStore( t: T ): T

  def update( mc: T ): T = {
    if (!storeUpdated) {
      storeUpdated = true
      updateStore(mc)
    }
    mc
  }

  def update( t: Try[T] ): Try[T] = {
    if (!storeUpdated) {
      t.foreach( mc => {
        updateStore(mc)
      })
    }
    t
  }

  def update( o: Option[Try[T]] ): Option[Try[T]] = {
    o.foreach(t => update(t))
    o
  }

}
