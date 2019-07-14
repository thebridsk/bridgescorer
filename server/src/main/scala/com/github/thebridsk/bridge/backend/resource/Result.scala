package com.github.thebridsk.bridge.backend.resource

import com.github.thebridsk.utilities.logging.Logger
import akka.http.scaladsl.model.StatusCode
import com.github.thebridsk.bridge.data.RestMessage
import scala.concurrent.Future
import scala.concurrent.Promise

object Result {

  val log = Logger(Result.getClass.getName)

  def apply[T](t: T): Result[T] = Right(t)

  def apply[T](statusCode: StatusCode, msg: String): Result[T] =
    Left((statusCode, RestMessage(msg)))

  def apply[T](statusCode: StatusCode, msg: RestMessage): Result[T] =
    Left((statusCode, msg))

  def apply[T](error: (StatusCode, RestMessage)): Result[T] = Left(error)

  def future[T](t: Result[T]): Future[Result[T]] =
    Promise.successful(t).future

  def future[T](t: T): Future[Result[T]] =
    Promise.successful(Result(t)).future

  def future[T](statusCode: StatusCode, msg: String): Future[Result[T]] =
    Promise.successful(Result(statusCode, RestMessage(msg))).future

  def future[T](statusCode: StatusCode, msg: RestMessage): Future[Result[T]] =
    Promise.successful(Result(statusCode, msg)).future

  def future[T](error: (StatusCode, RestMessage)): Future[Result[T]] =
    Promise.successful(Result(error)).future

}
