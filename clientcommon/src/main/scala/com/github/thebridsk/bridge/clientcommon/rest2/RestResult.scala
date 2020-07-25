package com.github.thebridsk.bridge.clientcommon.rest2

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.CanAwait
import scala.concurrent.ExecutionContext
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.reflect.ClassTag
import com.github.thebridsk.utilities.logging.Logger
import scala.concurrent.Awaitable
import scala.concurrent.Promise
import org.scalactic.source.Position
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import play.api.libs.json._
import com.github.thebridsk.bridge.data.rest.JsonSupport._
import com.github.thebridsk.bridge.clientcommon.logger.CommAlerter
import scala.language.implicitConversions

object RestResult {

  val log = Logger("bridge.RestResult")

  def classtag[T]( implicit ct: ClassTag[T] ) = ct
  val classtagUnit = classtag[Unit].runtimeClass
  def returnUnit: Unit = {}

  def unit: Result[Unit] = new ResultObject( Future.unit)

  def tryit[T]( fun: String, comment: => String)( f: => Try[T] )( implicit ct: ClassTag[T] ): Try[T] = {
    try {
      val ret = f
      ret match {
        case Success(x) =>
          log.fine(s"${fun}: success")
        case Failure(ex) =>
          CommAlerter.tryAlert(s"${fun}: failure return type is ${ct.runtimeClass.getName}, data was:\n${comment}",ex)
          log.warning(s"${fun}: failure ${Alerter.exceptionToString(ex)}")
      }
      ret
    } catch {
      case x: Throwable =>
        CommAlerter.tryAlert(s"${fun}: return type is ${ct.runtimeClass.getName}, data was:\n${comment}",x)
        log.warning(s"${fun}: exception ${x}: return type is ${ct.runtimeClass.getName}, data was:\n${comment}\n******** ${Alerter.exceptionToString(x)}")
        Failure(x)
    }
  }

  private implicit def transformToT[T]( t: Try[WrapperXMLHttpRequest])( implicit classtag: ClassTag[T], reader: Reads[T]): Try[T] = t match {
    case Success(req) =>
      if (classtag.runtimeClass == RestResult.classtagUnit) {
        Success( RestResult.returnUnit.asInstanceOf[T] )
      } else {
        def readJson[T]( s: String )( implicit reader: Reads[T] ): T = {
          val json = Alerter.tryit( Json.parse(s) )(Position.here)
          Alerter.tryit( convertJson[T](json) )(Position.here)
        }

        val j = req.responseText
        val t = RestResult.tryit( "RestResult.transformToT", j ) {
          Success( readJson[T](j) )
        }
        t
      }
    case Failure(fail) =>
      if (!fail.isInstanceOf[AjaxDisabled]) {
        RestResult.log.info(s"RestResult.transformToT: failure ${fail.getClass.getName} ${fail}", fail)
      }
      Failure(fail)
  }

  def ajaxToRestResult[T](
                           ajaxResult: AjaxResult[WrapperXMLHttpRequest]
                         )(
                           implicit
                             pos: Position,
                             classtag: ClassTag[T],
                             reader: Reads[T],
                             executor: ExecutionContext
                         ) = {
    new RestResult[T](ajaxResult, ajaxResult.transform( t => transformToT(t) ) )
  }

  private def transformToArrayT[T]( t: Try[WrapperXMLHttpRequest])( implicit classtag: ClassTag[T], reader: Reads[T]): Try[Array[T]] = t match {
    case Success(req) =>
      val j = req.responseText
      RestResult.tryit( "RestResultArray.transformToT", j ) {
        Success( readJson[Array[T]](j) )
      }
    case Failure(fail) =>
      if (!fail.isInstanceOf[AjaxDisabled]) {
        RestResult.log.info(s"RestResultArray.transformToT: failure ${fail.getClass.getName} ${fail}",fail)
      }
      Failure(fail)
  }

  implicit def ajaxToRestResultArray[T](
                                         ajaxResult: AjaxResult[WrapperXMLHttpRequest]
                                       )(
                                         implicit
                                           pos: Position,
                                           classtag: ClassTag[T],
                                           reader: Reads[T],
                                           executor: ExecutionContext
                                       ) = {
    new RestResultArray[T](ajaxResult, ajaxResult.transform( t => transformToArrayT(t)) )
  }

}

trait Result[T] extends Cancellable[T] with Awaitable[T] {

  def recordFailure( rec: ResultRecorder = ResultRecorder )(implicit executor: ExecutionContext, pos: Position): Result[T]

  // Members declared in scala.concurrent.Future
  def isCompleted: Boolean

  def onComplete[U](f: Try[T] => U)(implicit executor: ExecutionContext): Unit

  def transform[S](f: Try[T] => Try[S])(implicit executor: ExecutionContext): Result[S]

  def transformWith[S](f: Try[T] => Future[S])(implicit executor: ExecutionContext): Result[S]

  def value: Option[Try[T]]

  def foreach[U](f: T => U)(implicit executor: ExecutionContext): Unit = onComplete { _ foreach f }

  def failed: Result[Throwable]

  def result(atMost: Duration)(implicit permit: CanAwait): T

  def ready(atMost: Duration)(implicit permit: CanAwait): this.type

  def map[S](f: T => S)(implicit executor: ExecutionContext): Result[S] = transform(_ map f)

}

class ResultObject[T]( future: Future[T] ) extends Result[T] {

  def this( t: T ) = this( Promise[T]().success(t).future )

  /**
   * calls Promise.failure( RequestCancelled ) if successfully cancelled
   * returns true if cancelled, false otherwise
   */
  def cancel(): Boolean = false

  // Members declared in scala.concurrent.Awaitable
  def ready(atMost: Duration)(implicit permit: CanAwait) = {
    future.ready(atMost)
    this
  }
  def result(atMost: Duration)(implicit permit: CanAwait): T = {
    future.result(atMost)
  }

  implicit
  def wrap[S]( r: Future[S] )(implicit pos: Position ): ResultObject[S] = {
    new ResultObject( r )
  }

  def failed: ResultObject[Throwable] = future.failed

  // Members declared in scala.concurrent.Future
  def isCompleted: Boolean = future.isCompleted
  def onComplete[U](f: Try[T] => U)(implicit executor: ExecutionContext): Unit = future.onComplete( t => f(t) )
  def transform[S](f: Try[T] => Try[S])(implicit executor: ExecutionContext): ResultObject[S] = future.transform( t => f(t) )
  def transformWith[S](f: Try[T] => Future[S])(implicit executor: ExecutionContext): ResultObject[S] = future.transformWith( t => f(t) )
  def value: Option[Try[T]] = future.value match {
    case None => None
    case Some( t ) => Some(t)
  }

  def recordFailure( rec: ResultRecorder = ResultRecorder )(implicit executor: ExecutionContext, pos: Position) = {
    this
  }
}

class RestResult[T]( val ajaxResult: AjaxResult[WrapperXMLHttpRequest], val future: Future[T] )(implicit pos: Position ) extends Result[T] {
  /**
   * calls Promise.failure( RequestCancelled ) if successfully cancelled
   * returns true if cancelled, false otherwise
   */
  def cancel(): Boolean = ajaxResult.cancel()

  // Members declared in scala.concurrent.Awaitable
  def ready(atMost: Duration)(implicit permit: CanAwait) = {
    future.ready(atMost)
    this
  }
  def result(atMost: Duration)(implicit permit: CanAwait): T = {
    future.result(atMost)
  }

  implicit
  def wrap[S]( r: Future[S] )(implicit pos: Position ): RestResult[S] = {
    new RestResult( ajaxResult, r )
  }

  def failed: RestResult[Throwable] = future.failed

  // Members declared in scala.concurrent.Future
  def isCompleted: Boolean = future.isCompleted
  def onComplete[U](f: Try[T] => U)(implicit executor: ExecutionContext): Unit = {
    future.onComplete( t => Alerter.tryit( f(t) ) )
  }
  def transform[S](f: Try[T] => Try[S])(implicit executor: ExecutionContext): Result[S] = {
    future.transform( t => Alerter.tryit( f(t) ) )
  }
  def transformWith[S](f: Try[T] => Future[S])(implicit executor: ExecutionContext): Result[S] = {
    future.transformWith( t => Alerter.tryit( f(t) ) )
  }
  def value: Option[Try[T]] = Alerter.tryit( future.value match {
    case None => None
    case Some( t ) => Some(t)
  } )

  def recordFailure( rec: ResultRecorder = ResultRecorder )(implicit executor: ExecutionContext, pos: Position) = {
    ajaxResult.recordFailure(rec)
    this
  }
}

class RestResultArray[T]( ajaxResult: AjaxResult[WrapperXMLHttpRequest], result: Future[Array[T]] )(implicit pos: Position) extends Result[Array[T]] {
  /**
   * calls Promise.failure( RequestCancelled ) if successfully cancelled
   * returns true if cancelled, false otherwise
   */
  def cancel(): Boolean = ajaxResult.cancel()

  implicit
  def wrap[S]( r: Future[S] )(implicit pos: Position ): RestResult[S] = {
    new RestResult( ajaxResult, r )
  }

  def failed: RestResult[Throwable] = result.failed

  // Members declared in scala.concurrent.Awaitable
  def ready(atMost: Duration)(implicit permit: CanAwait) = {
    result.ready(atMost)
    this
  }
  def result(atMost: Duration)(implicit permit: CanAwait): Array[T] = {
    result.result(atMost)
  }

  // Members declared in scala.concurrent.Future
  def isCompleted: Boolean = result.isCompleted
  def onComplete[U](f: Try[Array[T]] => U)(implicit executor: ExecutionContext): Unit = {
    result.onComplete( t => Alerter.tryit( f(t) ) )
  }
  def transform[S](f: Try[Array[T]] => Try[S])(implicit executor: ExecutionContext): RestResult[S] = {
    result.transform( t => Alerter.tryit( f(t) ) )
  }
  def transformWith[S](f: Try[Array[T]] => Future[S])(implicit executor: ExecutionContext): RestResult[S] = {
    result.transformWith( t => Alerter.tryit( f(t) ) )
  }
  def value: Option[Try[Array[T]]] = Alerter.tryit( result.value match {
    case None => None
    case Some( t ) => Some(t)
  } )

  def recordFailure( rec: ResultRecorder = ResultRecorder )(implicit executor: ExecutionContext, pos: Position) = {
    ajaxResult.recordFailure(rec)
    this
  }

}
