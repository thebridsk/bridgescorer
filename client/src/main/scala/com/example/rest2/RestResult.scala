package com.example.rest2

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.CanAwait
import scala.concurrent.ExecutionContext
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.reflect.ClassTag
import org.scalajs.dom.ext.AjaxException
import utils.logging.Logger
import scala.concurrent.Awaitable
import scala.concurrent.Promise
import org.scalactic.source.Position
import com.example.logger.Alerter
import play.api.libs.json._
import com.example.data.rest.JsonSupport._
import com.example.logger.CommAlerter

object RestResult {

  val log = Logger("bridge.RestResult")

  def classtag[T]( implicit ct: ClassTag[T] ) = ct
  val classtagUnit = classtag[Unit].runtimeClass
  def returnUnit: Unit = {}

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
}

trait Result[T] extends CancellableFuture[T] {
  /**
   * calls Promise.failure( RequestCancelled ) if successfully cancelled
   * returns true if cancelled, false otherwise
   */
  def cancel(): Boolean

  def recordFailure( rec: ResultRecorder = ResultRecorder )(implicit executor: ExecutionContext, pos: Position): Result[T]

}

class ResultObject[T]( t: T ) extends Result[T] {

  val future = Promise[T].success(t).future

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

  // Members declared in scala.concurrent.Future
  def isCompleted: Boolean = future.isCompleted
  def onComplete[U](f: Try[T] => U)(implicit executor: ExecutionContext): Unit = future.onComplete( t => f(t) )
  def transform[S](f: Try[T] => Try[S])(implicit executor: ExecutionContext): Future[S] = future.transform( t => f(t) )
  def transformWith[S](f: Try[T] ⇒ Future[S])(implicit executor: ExecutionContext): Future[S] = future.transformWith( t => f(t) )
  def value: Option[Try[T]] = future.value match {
    case None => None
    case Some( t ) => Some(t)
  }

  def recordFailure( rec: ResultRecorder = ResultRecorder )(implicit executor: ExecutionContext, pos: Position) = {
    this
  }
}

class RestResult[T]( ajaxResult: AjaxResult )(implicit reader: Reads[T], classtag: ClassTag[T], pos: Position ) extends Result[T] {
  /**
   * calls Promise.failure( RequestCancelled ) if successfully cancelled
   * returns true if cancelled, false otherwise
   */
  def cancel(): Boolean = ajaxResult.cancel()

  private var cacheResult: Option[Try[T]] = None

  import scala.language.implicitConversions
  private implicit def transformToT( t: Try[WrapperXMLHttpRequest]): Try[T] = t match {
    case Success(req) =>
      if (classtag.runtimeClass == RestResult.classtagUnit) {
        Success( RestResult.returnUnit.asInstanceOf[T] )
      } else {
        def readJson[T]( s: String )( implicit reader: Reads[T] ): T = {
          val json = Alerter.tryit( Json.parse(s) )(Position.here)
          Alerter.tryit( convertJson[T](json) )(Position.here)
        }

        cacheResult.getOrElse {
          val j = req.responseText
          val t = RestResult.tryit( "RestResult.transformToT", j ) {
            Success( readJson[T](j) )
          }
          cacheResult = Some(t)
          t
        }
      }
    case Failure(fail) =>
      if (!fail.isInstanceOf[AjaxDisabled]) {
        RestResult.log.info(s"RestResult.transformToT: failure ${fail.getClass.getName} ${fail}", fail)
      }
      Failure(fail)
  }

  // Members declared in scala.concurrent.Awaitable
  def ready(atMost: Duration)(implicit permit: CanAwait) = {
    ajaxResult.ready(atMost)
    this
  }
  def result(atMost: Duration)(implicit permit: CanAwait): T = {
    val r = ajaxResult.result(atMost)
    readJson[T](r.responseText)
  }

  // Members declared in scala.concurrent.Future
  def isCompleted: Boolean = ajaxResult.isCompleted
  def onComplete[U](f: Try[T] => U)(implicit executor: ExecutionContext): Unit = {
    ajaxResult.onComplete( t => Alerter.tryit( f(t) ) )
  }
  def transform[S](f: Try[T] => Try[S])(implicit executor: ExecutionContext): Future[S] = {
    ajaxResult.transform( t => Alerter.tryit( f(t) ) )
  }
  def transformWith[S](f: Try[T] ⇒ Future[S])(implicit executor: ExecutionContext): Future[S] = {
    ajaxResult.transformWith( t => Alerter.tryit( f(t) ) )
  }
  def value: Option[Try[T]] = Alerter.tryit( ajaxResult.value match {
    case None => None
    case Some( t ) => Some(t)
  } )

  def recordFailure( rec: ResultRecorder = ResultRecorder )(implicit executor: ExecutionContext, pos: Position) = {
    ajaxResult.recordFailure(rec)
    this
  }

}

class RestResultArray[T]( ajaxResult: AjaxResult )(implicit reader: Reads[T], classtag: ClassTag[T]) extends Future[Array[T]] {
  /**
   * calls Promise.failure( RequestCancelled ) if successfully cancelled
   * returns true if cancelled, false otherwise
   */
  def cancel(): Boolean = ajaxResult.cancel()


  import scala.language.implicitConversions
  private implicit def transformToT( t: Try[WrapperXMLHttpRequest]): Try[Array[T]] = t match {
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

  // Members declared in scala.concurrent.Awaitable
  def ready(atMost: Duration)(implicit permit: CanAwait) = {
    ajaxResult.ready(atMost)
    this
  }
  def result(atMost: Duration)(implicit permit: CanAwait): Array[T] = {
    val r = ajaxResult.result(atMost)
    readJson[Array[T]](r.responseText)
  }

  // Members declared in scala.concurrent.Future
  def isCompleted: Boolean = ajaxResult.isCompleted
  def onComplete[U](f: Try[Array[T]] => U)(implicit executor: ExecutionContext): Unit = {
    ajaxResult.onComplete( t => Alerter.tryit( f(t) ) )
  }
  def transform[S](f: Try[Array[T]] => Try[S])(implicit executor: ExecutionContext): Future[S] = {
    ajaxResult.transform( t => Alerter.tryit( f(t) ) )
  }
  def transformWith[S](f: Try[Array[T]] ⇒ Future[S])(implicit executor: ExecutionContext): Future[S] = {
    ajaxResult.transformWith( t => Alerter.tryit( f(t) ) )
  }
  def value: Option[Try[Array[T]]] = Alerter.tryit( ajaxResult.value match {
    case None => None
    case Some( t ) => Some(t)
  } )

  def recordFailure( rec: ResultRecorder = ResultRecorder )(implicit executor: ExecutionContext, pos: Position) = {
    ajaxResult.recordFailure(rec)
    this
  }

}
