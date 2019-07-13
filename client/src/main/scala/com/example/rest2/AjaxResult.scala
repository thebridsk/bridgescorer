package com.github.thebridsk.bridge.rest2

import scala.util.Try
import scala.concurrent.ExecutionContext
import org.scalajs.dom.ext.Ajax.InputData
import scala.concurrent.Future
import scala.concurrent.Promise
import org.scalactic.source.Position
import org.scalajs.dom.raw.XMLHttpRequest
import com.github.thebridsk.bridge.data.RestMessage
import scala.concurrent.CanAwait
import scala.concurrent.duration.Duration
import scala.util.Failure
import com.github.thebridsk.bridge.logger.CommAlerter
import com.github.thebridsk.bridge.logger.Alerter
import com.github.thebridsk.utilities.logging.Logger
import scala.reflect.ClassTag
import scala.util.Success
import play.api.libs.json.Reads
import play.api.libs.json.Json
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import com.github.thebridsk.bridge.data.rest.JsonException
import play.api.libs.json.JsValue

trait Cancellable[T] {
  /**
   * calls Promise.failure( RequestCancelled ) if successfully cancelled
   * returns true if cancelled, false otherwise
   */
  def cancel(): Boolean

  def onComplete[U]( f: Try[T] => U)(implicit executor: ExecutionContext): Unit

}

/**
 * Wraps the result of an Ajax call.
 *
 * @param T the type of the successful return
 *
 * @constructor
 * @param req
 * @param url
 * @param reqbody
 * @param future
 * @param promise
 * @param pos
 */
class AjaxResult[T]( val req: WrapperXMLHttpRequest, val url: String, val reqbody: InputData, future: Future[T], promise: Promise[WrapperXMLHttpRequest], val pos: Position ) extends Future[T] with Cancellable[T] {

  import scala.language.implicitConversions

  implicit
  private def withFuture[U]( newfuture: Future[U] ) = new AjaxResult(req,url,reqbody,newfuture,promise,pos)

  /**
   * calls Promise.failure( RequestCancelled ) if successfully cancelled
   * returns true if cancelled, false otherwise
   */
  def cancel(): Boolean = {
    req.abort()
    if (req.readyState == XMLHttpRequest.UNSENT) {
      try {
        promise.failure( new RequestCancelled( RestMessage("Cancelled"), this ) )
        true
      } catch {
        case x: IllegalStateException =>
          false
      }
    } else {
      false
    }
  }

  // Members declared in scala.concurrent.Awaitable
  def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
    future.ready(atMost)
    this
  }

  def result(atMost: Duration)(implicit permit: CanAwait): T = future.result(atMost)

  // Members declared in scala.concurrent.Future
  def isCompleted: Boolean = future.isCompleted
  def onComplete[U](f: Try[T] => U)(implicit executor: ExecutionContext): Unit = {
    future.onComplete(CommAlerter.tryitfunr(null.asInstanceOf[U])(f))
  }

  override
  def transform[S](s: T => S, f: Throwable => Throwable)(implicit executor: ExecutionContext): AjaxResult[S] = {
    future.transform( CommAlerter.tryit(s), CommAlerter.tryit(f))
  }

  def transform[S](f: Try[T] => Try[S])(implicit executor: ExecutionContext): AjaxResult[S] = {
    future.transform( CommAlerter.tryit(f))
  }
  def transformWith[S](f: Try[T] => Future[S])(implicit executor: ExecutionContext): AjaxResult[S] = {
    future.transformWith(CommAlerter.tryit(f))
  }
  def value: Option[Try[T]] = future.value

  override
  def failed: AjaxResult[Throwable] = future.failed

  override
  def foreach[U](f: T => U)(implicit executor: ExecutionContext): Unit = future.foreach(CommAlerter.tryit(f))

  override
  def map[S](f: T => S)(implicit executor: ExecutionContext): AjaxResult[S] = future.map(CommAlerter.tryit(f))

  override
  def flatMap[S](f: T => Future[S])(implicit executor: ExecutionContext): AjaxResult[S] = future.flatMap(CommAlerter.tryit(f))

  override
  def flatten[S](implicit ev: T <:< Future[S]): AjaxResult[S] = future.flatten(ev)

  override
  def filter( p: T => Boolean)(implicit executor: ExecutionContext): AjaxResult[T] = future.filter(CommAlerter.tryit(p))

  override
  def collect[S](pf: PartialFunction[T, S])(implicit executor: ExecutionContext): AjaxResult[S] = future.collect(pf)

  override
  def recover[U >: T](pf: PartialFunction[Throwable, U])(implicit executor: ExecutionContext): AjaxResult[U] = future.recover(pf)

  override
  def recoverWith[U >: T](pf: PartialFunction[Throwable, Future[U]])(implicit executor: ExecutionContext): AjaxResult[U] = future.recoverWith(pf)

  override
  def zip[U](that: Future[U]): AjaxResult[(T, U)] = future.zip(that)

  override
  def zipWith[U, R](that: Future[U])(f: (T, U) => R)(implicit executor: ExecutionContext): AjaxResult[R] = future.zipWith(that)(f)

  override
  def fallbackTo[U >: T](that: Future[U]): AjaxResult[U] = future.fallbackTo(that)

  override
  def mapTo[S](implicit tag: ClassTag[S]): AjaxResult[S] = future.mapTo(tag)

  override
  def andThen[U](pf: PartialFunction[Try[T], U])(implicit executor: ExecutionContext): AjaxResult[T] = future.andThen(pf)

  /**
   * returns a future to an instance of U which was unmarshalled from the body of the HTTP error response.
   * @param U the class of the return type.  Must have an implicit Reads in scope.
   * @param executor an executor
   * @param reader A Reads object to unmarshall a U
   * @return if this future failed with an AjaxErrorReturn exception, then a Future to the U object is returned.
   * If the body in the AjaxErrorReturn could not be unmarshalled, then a JsonException will fail the returned future.
   * If this future fails with an exception other than AjaxErrorReturn, then the returned future is failed with the exception
   * If this future succeeds then the returned future will never complete.
   */
  def mapErrorReturn[U](implicit executor: ExecutionContext, reader: Reads[U], classtag: ClassTag[U]): AjaxResult[U] = {
    future.onlyExceptions.
           map { t =>
             if (!t.isInstanceOf[AjaxErrorReturn]) throw t
             val error = t.asInstanceOf[AjaxErrorReturn]
             AjaxResult.fromJson[U](error.body, url, t)
           }
  }

  /**
   * returns a future to an exception.
   * @param executor an executor
   * @return a future to the exception.  If this future does not fail with an exception then the returned future never completes.
   */
  def onlyExceptions(implicit executor: ExecutionContext): AjaxResult[Throwable] = {
    val p = Promise[Throwable]()
    onComplete { tr =>
      tr match {
        case Success(t) =>
        case Failure(error) => p.success(error)
      }
    }
    p.future
  }

  def recordFailure( rec: ResultRecorder = ResultRecorder )(implicit executor: ExecutionContext, pos: Position) = {
    future.failed.foreach( Alerter.tryit { rec.record(_) })
    this
  }

}

/**
 * Wraps an XMLHttpRequest to provide an easy one-line way of making
 * an Ajax call, returning a Future.
 */
object AjaxResult {

  val log = Logger("bridge.AjaxResult")

  import scala.concurrent.duration._
  import scala.language.postfixOps
  val defaultTimeout = 30 seconds

  private var fAjaxCall: IAjaxCall = AjaxCall

  def setEnabled( f: Boolean ) = {
    if (f) {
      fAjaxCall = AjaxCall
    } else {
      fAjaxCall = AjaxCallDisabled
    }
    log.info("Setting RestClient.enabled to "+f)
  }

  def setAjaxCall( ac: IAjaxCall ) = {
    fAjaxCall = ac
  }

  def ajaxCall: IAjaxCall = fAjaxCall

  /**
   * @return Some(true) if enabled and sending to server
   *          Some(false) if disabled
   *          None if a mock test has been set
   */
  def isEnabled: Option[Boolean] = {
    fAjaxCall match {
      case `AjaxCall` => Some(true)
      case `AjaxCallDisabled` => Some(false)
      case _ => None
    }
  }

  def get(url: String, data: InputData = null, timeout: Duration = defaultTimeout,
      headers: Map[String, String] = Map.empty,
      withCredentials: Boolean = false, responseType: String = "")( implicit pos: Position) = {
    apply("GET", url, data, timeout, headers, withCredentials, responseType)(pos)
  }

  def post(url: String, data: InputData = null, timeout: Duration = defaultTimeout,
      headers: Map[String, String] = Map.empty,
      withCredentials: Boolean = false, responseType: String = "")( implicit pos: Position) = {
    apply("POST", url, data, timeout, headers, withCredentials, responseType)(pos)
  }

  def put(url: String, data: InputData = null, timeout: Duration = defaultTimeout,
      headers: Map[String, String] = Map.empty,
      withCredentials: Boolean = false, responseType: String = "")( implicit pos: Position) = {
    apply("PUT", url, data, timeout, headers, withCredentials, responseType)(pos)
  }

  def delete(url: String, data: InputData = null, timeout: Duration = defaultTimeout,
      headers: Map[String, String] = Map.empty,
      withCredentials: Boolean = false, responseType: String = "")( implicit pos: Position) = {
    apply("DELETE", url, data, timeout, headers, withCredentials, responseType)(pos)
  }

  def apply(method: String, url: String, data: InputData, timeout: Duration,
      headers: Map[String, String], withCredentials: Boolean,
      responseType: String)( implicit pos: Position): AjaxResult[WrapperXMLHttpRequest] = {
    ajaxCall.send(method, url, data, timeout, headers, withCredentials, responseType)
  }

  def fromJson[U]( s: String, url: String, cause: Throwable = null )(implicit reader: Reads[U], classtag: ClassTag[U]): U = {
    val json = Json.parse(s)
    fromJsonValue[U](json,url)
  }

  def fromJsonValue[U]( json: JsValue, url: String, cause: Throwable = null )(implicit reader: Reads[U], classtag: ClassTag[U]): U = {
    Json.fromJson[U](json) match {
      case JsSuccess(t,path) =>
        t
      case JsError(errors) =>
        val clsname = classtag.runtimeClass.getName
        val s = errors.map { entry =>
          val (path, verrs) = entry
          s"""\n  ${path}: ${verrs.map(e=>e.message).mkString("\n    ","\n    ","")}"""
        }
        throw new JsonException(s"""Error unmarshalling a ${clsname} from request ${url}: ${s}""", cause)
    }
  }

}
