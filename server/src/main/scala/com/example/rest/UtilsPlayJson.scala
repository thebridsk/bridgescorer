package com.example.rest

import com.example.data.MatchDuplicate
import com.example.data.RestMessage

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes.{ Success=>_, _ }
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag
import com.example.json.BridgePlayJsonSupport
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.unmarshalling._
import play.api.libs.json._
import akka.http.scaladsl.server.Route
import com.example.backend.resource.Result
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success
import com.example.data.VersionedInstance
import utils.logging.Logger

class UtilsPlayJson

object UtilsPlayJson extends BridgePlayJsonSupport {

  val utilslog = Logger[UtilsPlayJson]

  def resourceCreated[T]( resName: String, f: Future[Result[(String,T)]], successStatus: StatusCode = Created )
                        (implicit marshaller: ToResponseMarshaller[T], writer: Writes[T]): Route =
    onComplete(f) {
      case Success(r) =>
        r match {
          case Right((id,l)) =>
            extractScheme { scheme =>
              headerValue(Service.extractHostPort) { host =>
                val rn = if (resName.startsWith("/")) resName else "/"+resName
                val hn = if (host.endsWith("/")) host.substring(0,host.length-1) else host
                utilslog.fine( s"resourceCreated, hn=${hn}, rn=${rn}, id=${id}, setting location to ${scheme}://${hn}${rn}/${id}" )
                respondWithHeader( Location( s"${scheme}://${hn}${rn}/${id}" ) ) {
                  complete( successStatus, l )
                }
              } ~
              extractRequest { request =>
                val h = request.uri.authority.host.toString()
                val p = request.uri.authority.port
                val sp = if (scheme=="https" && p==443) h
                         else if (scheme=="http" && p==80) h
                         else s"${h}:${p}"
                val rn = if (resName.startsWith("/")) resName else "/"+resName
                respondWithHeader( Location( s"${scheme}://${sp}${rn}/${id}" ) ) {
                  complete(successStatus, l)
                }
              }
            }
          case Left(r) =>
            complete(r._1, r._2)
        }
      case Failure(ex)    =>
        complete((InternalServerError, s"An error occurred: ${ex.getMessage}"))
    }

  def resourceCreatedNoLocationHeader[T]( f: Future[Result[T]], successStatus: StatusCode = Created )
                        (implicit marshaller: ToResponseMarshaller[T], writer: Writes[T]): Route =
    onComplete(f) {
      case Success(r) =>
        r match {
          case Right(l) =>
            complete( successStatus, l )
          case Left(r) =>
            complete(r._1, r._2)
        }
      case Failure(ex)    =>
        complete((InternalServerError, s"An error occurred: ${ex.getMessage}"))
    }

  import scala.language.implicitConversions
  implicit def addIdToFuture[VType <: VersionedInstance[VType,VType,_]]( f: Future[Result[VType]] ): Future[Result[(String,VType)]] =
    f.map { r =>
      r match {
        case Right(md) => Right((md.id.toString(),md))
        case Left(e) => Left(e)
      }
    }

  def resourceUpdated[T]( f: Future[Result[T]], successStatus: StatusCode = NoContent, msg: Option[String] = None )
                 (implicit marshaller: ToResponseMarshaller[T], writer: Writes[T]) =
    onComplete(f) {
      case Success(r) =>
        r match {
          case Right(l) => msg.map { s=>complete(successStatus,s)}.getOrElse(complete(successStatus))
          case Left(r) => complete(r._1, r._2)
        }
      case Failure(ex) =>
        complete((InternalServerError, s"An error occurred: ${ex.getMessage}"))
    }

  def resource[T]( f: Future[Result[T]], successStatus: StatusCode = OK )
                 (implicit marshaller: ToResponseMarshaller[T], writer: Writes[T]) =
    onComplete(f) {
      case Success(r) =>
        r match {
          case Right(l) => complete(successStatus, l)
          case Left(r) => complete(r._1, r._2)
        }
      case Failure(ex) =>
        complete((InternalServerError, s"An error occurred: ${ex.getMessage}"))
    }

  def resourceDelete[T]( f: Future[Result[T]], successStatus: StatusCode = NoContent, msg: Option[String] = None ) =
    onComplete(f) {
      case Success(r) =>
        r match {
          case Right(l) => msg.map { s=>complete(successStatus,s)}.getOrElse(complete(successStatus))
          case Left((NotFound,_)) => msg.map { s=>complete(successStatus,s)}.getOrElse(complete(successStatus))
          case Left((statuscode,restmessage)) => complete(statuscode,restmessage)
        }
      case Failure(ex) =>
        complete((InternalServerError, s"An error occurred: ${ex.getMessage}"))
    }

  def resourceMap[T]( f: Future[Result[Map[String,T]]] )
                    (implicit arrayMarshaller: ToResponseMarshaller[Array[T]], awriter: Writes[Array[T]], twriter: Writes[T], classtag: ClassTag[T]) =
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

  def resourceList[T]( f: Future[Result[List[T]]] )
                    (implicit arrayMarshaller: ToResponseMarshaller[Array[T]], awriter: Writes[Array[T]], twriter: Writes[T], classtag: ClassTag[T]) =
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
