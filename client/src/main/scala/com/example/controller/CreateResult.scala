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

abstract class CreateResult[T]( result: Result[T])(implicit executor: ExecutionContext) extends Result[T] {
  private var storeUpdated = false

  result.onComplete( t => update(t) )

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

  // Members declared in scala.concurrent.Awaitable
  def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
    result.ready(atMost)
    this
  }
  def result(atMost: Duration)(implicit permit: CanAwait): T = update(result.result(atMost))

  // Members declared in scala.concurrent.Future
  def isCompleted: Boolean = result.isCompleted
  def onComplete[U](f: Try[T] ⇒ U)(implicit executor: ExecutionContext): Unit = result.onComplete( t => f(update(t)) )
  def transform[S](f: Try[T] ⇒ Try[S])(implicit executor: ExecutionContext): Future[S] = result.transform(t => f(update(t)))
  def transformWith[S](f: Try[T] ⇒ Future[S])(implicit executor: ExecutionContext): Future[S] = result.transformWith(t => f(update(t)))
  def value: Option[Try[T]] = update(result.value)

  // Members declared in com.example.rest2.Result
  def cancel(): Boolean = result.cancel()
  def recordFailure(rec: ResultRecorder)(implicit executor: ExecutionContext, pos: Position): this.type = {
    result.recordFailure(rec)
    this
  }

}
