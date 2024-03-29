package com.github.thebridsk.bridge.server.rest

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes.{Success => _, _}
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import scala.reflect.ClassTag
import com.github.thebridsk.bridge.server.json.BridgePlayJsonSupport
import akka.http.scaladsl.marshalling._
import play.api.libs.json._
import akka.http.scaladsl.server.Route
import com.github.thebridsk.bridge.server.backend.resource.Result
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import com.github.thebridsk.utilities.logging.Logger
import akka.http.scaladsl.server.{RequestContext, RouteResult}

class UtilsPlayJson

object UtilsPlayJson extends BridgePlayJsonSupport {

  val utilslog: Logger = Logger[UtilsPlayJson]()

  def resourceCreated[T](
      resName: String,
      f: Future[Result[(String, T)]],
      successStatus: StatusCode = Created,
      prefix: String = "/v1/rest"
  )(implicit marshaller: ToResponseMarshaller[T], writer: Writes[T]): Route =
    onComplete(f) {
      case Success(r) =>
        r match {
          case Right((id, l)) =>
            extractScheme { scheme =>
              val rn = if (resName.startsWith("/")) s"${prefix}${resName}" else s"${prefix}/${resName}"
              headerValue(Service.extractHostPort) { host =>
                val hn =
                  if (host.endsWith("/")) host.substring(0, host.length - 1)
                  else host
                utilslog.fine(
                  s"resourceCreated, hn=${hn}, rn=${rn}, id=${id}, setting location to ${scheme}://${hn}${rn}/${id}"
                )
                respondWithHeader(Location(s"${scheme}://${hn}${rn}/${id}")) {
                  complete(successStatus, l)
                }
              } ~
                extractRequest { request =>
                  val h = request.uri.authority.host.toString()
                  val p = request.uri.authority.port
                  val sp =
                    if (scheme == "https" && p == 443) h
                    else if (scheme == "http" && p == 80) h
                    else s"${h}:${p}"
                  respondWithHeader(Location(s"${scheme}://${sp}${rn}/${id}")) {
                    complete(successStatus, l)
                  }
                }
            }
          case Left(r) =>
            complete(r._1, r._2)
        }
      case Failure(ex) =>
        complete((InternalServerError, s"An error occurred: ${ex.getMessage}"))
    }

  def resourceCreatedNoLocationHeader[T](
      f: Future[Result[T]],
      successStatus: StatusCode = Created
  )(implicit marshaller: ToResponseMarshaller[T], writer: Writes[T]): Route =
    onComplete(f) {
      case Success(r) =>
        r match {
          case Right(l) =>
            complete(successStatus, l)
          case Left(r) =>
            complete(r._1, r._2)
        }
      case Failure(ex) =>
        complete((InternalServerError, s"An error occurred: ${ex.getMessage}"))
    }

  def resourceUpdated[T](
      f: Future[Result[T]],
      successStatus: StatusCode = NoContent,
      msg: Option[String] = None
  )(implicit
      marshaller: ToResponseMarshaller[T],
      writer: Writes[T]
  ): RequestContext => Future[RouteResult] =
    onComplete(f) {
      case Success(r) =>
        r match {
          case Right(l) =>
            msg
              .map { s =>
                complete(successStatus, s)
              }
              .getOrElse(complete(successStatus))
          case Left(r) => complete(r._1, r._2)
        }
      case Failure(ex) =>
        complete((InternalServerError, s"An error occurred: ${ex.getMessage}"))
    }

  def resource[T](
      f: Future[Result[T]],
      successStatus: StatusCode = OK
  )(implicit
      marshaller: ToResponseMarshaller[T],
      writer: Writes[T]
  ): RequestContext => Future[RouteResult] =
    onComplete(f) {
      case Success(r) =>
        r match {
          case Right(l) => complete(successStatus, l)
          case Left(r)  => complete(r._1, r._2)
        }
      case Failure(ex) =>
        complete((InternalServerError, s"An error occurred: ${ex.getMessage}"))
    }

  def resourceDelete[T](
      f: Future[Result[T]],
      successStatus: StatusCode = NoContent,
      msg: Option[String] = None
  ): RequestContext => Future[RouteResult] =
    onComplete(f) {
      case Success(r) =>
        r match {
          case Right(l) =>
            msg
              .map { s =>
                complete(successStatus, s)
              }
              .getOrElse(complete(successStatus))
          case Left((NotFound, _)) =>
            msg
              .map { s =>
                complete(successStatus, s)
              }
              .getOrElse(complete(successStatus))
          case Left((statuscode, restmessage)) =>
            complete(statuscode, restmessage)
        }
      case Failure(ex) =>
        complete((InternalServerError, s"An error occurred: ${ex.getMessage}"))
    }

  def resourceMap[T, I](f: Future[Result[Map[I, T]]])(implicit
      arrayMarshaller: ToResponseMarshaller[Array[T]],
      awriter: Writes[Array[T]],
      twriter: Writes[T],
      classtag: ClassTag[T]
  ): RequestContext => Future[RouteResult] =
    onComplete(f) {
      case Success(r) =>
        r match {
          case Right(l) =>
            val rl = l.values.toArray
            complete(OK, rl)
          case Left(r) => complete(r._1, r._2)
        }
      case Failure(ex) =>
        complete((InternalServerError, s"An error occurred: ${ex.getMessage}"))
    }

  def resourceList[T](f: Future[Result[List[T]]])(implicit
      arrayMarshaller: ToResponseMarshaller[Array[T]],
      awriter: Writes[Array[T]],
      twriter: Writes[T],
      classtag: ClassTag[T]
  ): RequestContext => Future[RouteResult] =
    onComplete(f) {
      case Success(r) =>
        r match {
          case Right(l) =>
            complete(OK, l)
          case Left(r) => complete(r._1, r._2)
        }
      case Failure(ex) =>
        complete((InternalServerError, s"An error occurred: ${ex.getMessage}"))
    }

}
