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
import scala.concurrent.ExecutionContext
import com.example.source.SourcePosition

class GraphQLRequest(
                      val url: String
                    )(
                      implicit
                        executor: ExecutionContext
                    ) {
  import GraphQLRequest._
  import scala.language.implicitConversions

  implicit def ajaxToRestResult[T](
                                    ajaxResult: AjaxResult
                                  )( implicit
                                       reader: Reads[T],
                                       classtag: ClassTag[T]
                                  ) = {
    RestResult.ajaxToRestResult(ajaxResult)
  }

  def request[T]( query: String,
                  variables: Option[Map[String,JsValue]]= None,
                  operation: Option[String] = None,
                  headers: Map[String, String] = headersForPost,
                  timeout: Duration = AjaxResult.defaultTimeout
                )( implicit reader: Reads[T],
                            writer: Writes[T],
                            classtag: ClassTag[T],
                            xpos: Position
                ): Result[T] = {
    val data = writeJson( queryToJson( query, variables, operation ) )
    val rr: RestResult[JsObject] = AjaxResult.post(url, data, timeout, headers )
    rr.transform { tjson: Try[JsObject] =>
      tjson match {
        case Success(json) =>
          json \ "data" match {
            case JsDefined( data ) if data != JsNull =>
              val x: JsResult[T] = Json.fromJson(data)(reader)
              x match {
                case JsSuccess(t,path) => Success(t)
                case JsError(errors) =>
                  val err = errors.map { entry =>
                    val (path,errs) = entry
                    s"""${path}: ${errs.map(e=>e.message).mkString("\n    ","\n    ","")}"""
                  }.mkString(" Parsing errors:\n","\n","")
                  Failure( new JsonException( s"${xpos.line}:${err}\nOn data: ${data}" ) )
              }
            case _ =>
              // data is null or undefined
//              {
//                "data":null,
//                "errors": [
//                  {
//                    "message":"Variable '$mdid' of type 'TeamId!' used in position expecting type 'DuplicateId!'. (line 2, column 22):\n         query Error($mdid: TeamId!) {\n                     ^\n (line 3, column 26):\n           duplicate(id: $mdid) {\n                         ^",
//                    "locations": [
//                      { "line":2,"column":22 },
//                      { "line":3,"column":26}
//                    ]
//                  }
//                ]
//              }

              json \ "errors" match {
                case JsDefined( JsArray( errors ) ) =>
                  val err = errors.map{ error =>
                    error \ "message" match {
                      case JsDefined( JsString(msg) ) =>
                        msg
                      case _ =>
                        s"""Unknown format for error: ${error}"""
                    }
                  }.mkString("\n","\n","")
                  Failure( new JsonException(s"""${xpos.line}: Error from server:${err}""") )
                case _ =>
                  json \ "error" match {
                    case JsDefined( JsString( error ) ) =>
                      val err = error
                      Failure( new JsonException(s"""${xpos.line}: Error from server:${err}""") )
                    case _ =>
                      Failure( new JsonException( s"${xpos.line}: Data not valid: ${json}" ) )
                  }
              }

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
            log.warning( messages.map { v => v.toString() }.mkString("Errors:\n","\n","") )
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
