package com.example.graphql

import play.api.libs.json._
import utils.logging.Logger
import com.example.rest2.Result
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import com.example.rest2.AjaxResult
import org.scalactic.source.Position
import com.example.data.rest.JsonSupport._
import com.example.rest2.RestResult
import scala.reflect.ClassTag
import scala.util.Success
import scala.util.Failure
import com.example.data.rest.JsonException
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global

class GraphQLRequest( val url: String ) {
  import GraphQLRequest._
  import scala.language.implicitConversions

  implicit def ajaxToRestResult[T : Reads : Writes : ClassTag]( ajaxResult: AjaxResult )(implicit pos: Position) = new RestResult[T](ajaxResult)

  def request[T]( query: String,
                  variables: Option[Map[String,JsValue]]= None,
                  operation: Option[String] = None,
                  headers: Map[String, String] = headersForPost,
                  timeout: Duration = AjaxResult.defaultTimeout
                )( implicit reader: Reads[T],
                            writer: Writes[T],
                            classtag: ClassTag[T],
                            xpos: Position
                ): Future[T] = {  // should return a Result[T]
    val data = writeJson( queryToJson( query, variables, operation ) )
    val rr: RestResult[JsObject] = AjaxResult.post(url, data, timeout, headers )
    rr.transform { tjson: Try[JsObject] =>
      tjson match {
        case Success(json) =>
          json \ "data" match {
            case JsDefined( data ) =>
              val x: JsResult[T] = Json.fromJson(data)(reader)
              x match {
                case JsSuccess(t,path) => Success(t)
                case JsError(error) => Failure( new JsonException( "Data not valid" ) )
              }
            case x: JsUndefined =>
              Failure( new JsonException( "Data not valid" ) )
          }
        case Failure(error) =>
          Failure(error)
      }
    }
  }
}

object GraphQLRequest {

  val log = Logger("bridge.GraphQLRequest")

  val headersForPost=Map("Content-Type" -> "application/json; charset=UTF-8",
                         "Accept" -> "application/json")

  def queryToJson( query: String, variables: Option[Map[String,JsValue]]= None, operation: Option[String] = None ) = {
    JsObject(
        operation.map( v => "operationName" -> JsString(v) ).toList :::
        variables.map( v => "variables" -> JsObject( v.toSeq ) ).toList :::
        "query" -> JsString(query) :: Nil
    )

  }

  def processError( resp: JsValue ) = {
    resp match {
      case _: JsObject =>
        resp \ "error" match {
          case JsDefined( JsArray( messages ) ) =>
            println( messages.map { v => v.toString() }.mkString("Errors:\n","\n","") )
          case JsDefined( _ ) =>
            log.warning(s"Expecting a messages, got ${resp}")
          case _: JsUndefined =>
            // no error
        }

      case _ =>
        log.warning(s"Expecting a JsObject, got ${resp}")
    }
  }

}
