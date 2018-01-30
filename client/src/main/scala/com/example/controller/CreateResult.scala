package com.example.controller

import scala.concurrent.duration.Duration
import scala.concurrent.CanAwait
import com.example.rest2.Result
import scala.concurrent.ExecutionContext
import scala.util.Try
import utils.logging.Logger
import scala.concurrent.Future
import com.example.rest2.ResultRecorder
import org.scalactic.source.Position
import com.example.rest2.RestResult
import com.example.rest2.AjaxResult
import com.example.rest2.WrapperXMLHttpRequest

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
