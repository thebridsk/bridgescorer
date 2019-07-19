package com.github.thebridsk.bridge.server.backend.resource

import scala.concurrent.ExecutionContext
import org.scalactic.source.Position
import scala.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import scala.util.Success
import scala.util.Failure
import com.github.thebridsk.bridge.source.SourcePosition

object Implicits {

  import Result._

  implicit class WrapResult[T](val r: Result[T]) extends AnyVal {
    def logit(
        comment: => String
    )(implicit pos: Position, caller: SourcePosition): Result[T] = {
      r match {
        case Left((statusCode, msg)) =>
          log.fine(
            s"${comment}: ${pos.line} returning to caller ${caller.line} error ${statusCode} ${msg}"
          )
        case Right(t) =>
          log.finest(
            s"${comment}: ${pos.line} returning to caller ${caller.line} ${t}"
          )
      }
      r
    }

    def isError = r.isLeft

    def isOk = r.isRight

    def toFuture: Future[Result[T]] = Result.future(r)
  }

  implicit class WrapOptionResult[T](val r: Option[Result[T]]) extends AnyVal {
    def logit(
        comment: => String
    )(implicit pos: Position, caller: SourcePosition): Option[Result[T]] = {
      r match {
        case Some(rr) =>
          rr match {
            case Left((statusCode, msg)) =>
              log.fine(
                s"${comment}: ${pos.line} returning to caller ${caller.line} error ${statusCode} ${msg}"
              )
            case Right(t) =>
              log.finest(
                s"${comment}: ${pos.line} returning to caller ${caller.line} ${t}"
              )
          }
        case None =>
          log.finest(
            s"${comment}: ${pos.line} returning to caller ${caller.line} None"
          )
      }
      r
    }
  }

  private val counter = new AtomicInteger

  def toObjectId[T](o: T) = {
    o.getClass.getName + "@" + Integer.toHexString(o.hashCode())
  }

  implicit class WrapFutureResult[T](val fr: Future[Result[T]]) extends AnyVal {
    def onError(comment: String)(
        implicit executor: ExecutionContext,
        pos: Position,
        caller: SourcePosition
    ) = {
      fr.failed.foreach { ex =>
        log.warning(
          s"${comment}: ${pos.line} returning to caller ${caller.line} got exception",
          ex
        )
      }
      fr
    }

    def logit(comment: String)(
        implicit execute: ExecutionContext,
        pos: Position,
        caller: SourcePosition
    ): Future[Result[T]] = {
      val c = counter.incrementAndGet()
      val com = s"${c} ${toObjectId(fr)} ${comment}"
      log.finest(
        s"${com}: ${pos.line} returning to caller ${caller.line} requesting logging the result"
      )
      val x = fr.onComplete { t =>
        t match {
          case Success(r) =>
            r.logit(com)(pos, caller)
          case Failure(ex) =>
            log.warning(
              s"${com}: ${pos.line} returning to caller ${caller.line} ${c} got exception",
              ex
            )
        }
      }
      fr
    }
  }

  implicit class WrapFutureOptionResult[T](val fr: Future[Option[Result[T]]])
      extends AnyVal {
    def onError(comment: String)(
        implicit executor: ExecutionContext,
        pos: Position,
        caller: SourcePosition
    ) = {
      fr.failed.foreach { ex =>
        log.warning(s"${comment}: ${pos.line} got exception", ex)
      }
      fr
    }

    def logit(comment: String)(
        implicit execute: ExecutionContext,
        pos: Position,
        caller: SourcePosition
    ): Future[Option[Result[T]]] = {
      val c = counter.incrementAndGet()
      val com = s"${c} ${toObjectId(fr)} ${comment}"
      log.finest(
        s"${com}: ${pos.line} returning to caller ${caller.line} requesting logging the result"
      )
      val x = fr.onComplete { t =>
        t match {
          case Success(Some(r)) =>
            r.logit(com)(pos, caller)
          case Success(None) =>
            log.warning(
              s"${com}: ${pos.line} returning to caller ${caller.line} ${c} got None"
            )
          case Failure(ex) =>
            log.warning(
              s"${com}: ${pos.line} returning to caller ${caller.line} ${c} got exception",
              ex
            )
        }
      }
      fr
    }
  }
}
