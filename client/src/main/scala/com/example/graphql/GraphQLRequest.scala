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
import org.scalajs.dom.ext.AjaxException

// Errors from graphql api:
// {"data":null,"errors":[
//  { "message":"Cannot query field 'xxx' on type 'Query'. (line 1, column 3):\n{ xxx }\n  ^",
//    "locations":[{"line":1,"column":3}]
//  }]}
// {"error":"Syntax error while parsing GraphQL query. Invalid input 'x', expected OperationDefinition, FragmentDefinition or TypeSystemDefinition (line 1, column 1):\nxxx\n^"}

case class ErrorLocation( line: Option[Int], column: Option[Int] ) {
  override
  def toString() = s"""${line.getOrElse("unknown")}:${column.map(i=>i.toString()).getOrElse("unknown")}"""
}

case class ErrorMessage( message: Option[String], locations: Option[List[ErrorLocation]] ) {
  override
  def toString() = s"""${message.getOrElse("")} from location ${locations.map(l=>l.mkString(", ")).getOrElse("")}"""
}

case class GraphQLResponse( data: Option[JsValue], error: Option[String], errors: Option[List[ErrorMessage]] ) {

  def getError() = {
    val er = error.toList:::errors.map(l=>l.map(e=>e.toString())).getOrElse(Nil)
    er.mkString("\n")
  }

  def getResponse[T]( implicit reads: Reads[T], xpos: Position ): Option[T] = {
    data.map { j =>
      Json.fromJson[T](j) match {
        case JsSuccess( t, path ) => t
        case JsError( err ) =>
          val e = err.map { entry =>
            val (path,errs) = entry
            s"""${path}: ${errs.map(e=>e.message).mkString("\n    ","\n    ","")}"""
          }.mkString(" Parsing errors:\n","\n","")
          throw new JsonException( s"${xpos.line}:${e}\nOn data: ${data}" )
      }
    }
  }

  def hasData = data.isDefined
}

class Query[Variables,Response](
                                 query: String,
                                 client: GraphQLRequest
                               )(
                                 implicit
                                   reader: Reads[Response],
                                   writer: Writes[Variables],
                                   classtag: ClassTag[Response],
                                   xpos: Position
                               ) {

  import GraphQLRequest._

  def execute( variables: Option[Variables],
               operation: Option[String] = None,
               headers: Map[String, String] = headersForPost,
               timeout: Duration = AjaxResult.defaultTimeout
             ): Result[Response] = {
    client.requestVars(query, variables, operation, headers, timeout)
  }

}

class GraphQLException( val error: GraphQLResponse, cause: Throwable = null ) extends Exception( error.getError(), cause )

object GraphQLRequest {

  val log = Logger("bridge.GraphQLRequest")

  val headersForPost=Map("Content-Type" -> "application/json; charset=UTF-8",
                         "Accept" -> "application/json")

  def queryToJson( query: String, variables: Option[JsObject]= None, operation: Option[String] = None ) = {
    JsObject(
        operation.map( v => "operationName" -> JsString(v) ).toList :::
        variables.map( v => "variables" -> v ).toList :::
        "query" -> JsString(query) :: Nil
    )

  }

  implicit val errorLocationFormat = Json.format[ErrorLocation]
  implicit val ErrorMessageFormat = Json.format[ErrorMessage]
  implicit val graphQLResponseFormat = Json.format[GraphQLResponse]
}

class GraphQLRequest(
                      val url: String
                    )(
                      implicit
                        executor: ExecutionContext
                    ) {
  import GraphQLRequest._
  import scala.language.implicitConversions

  def toGraphQLResponse( json: JsValue ) = {
    Json.fromJson[GraphQLResponse](json) match {
      case JsSuccess(r,path) => r
      case JsError(error) =>
        GraphQLResponse( None, Some(
            s"""Unable to unmarshal response from server:\n${json}"""
            ), None
        )
    }
  }

  implicit def ajaxToRestResult[T](
                                    ajaxResult: AjaxResult
                                  )( implicit
                                       reader: Reads[T],
                                       classtag: ClassTag[T]
                                  ) = {
    RestResult.ajaxToRestResult(ajaxResult)
  }

  def request[T]( query: String,
                  variables: Option[JsObject]= None,
                  operation: Option[String] = None,
                  headers: Map[String, String] = headersForPost,
                  timeout: Duration = AjaxResult.defaultTimeout
                )( implicit reader: Reads[T],
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

  def requestVars[T,V]( query: String,
                        variables: Option[V],
                        operation: Option[String] = None,
                        headers: Map[String, String] = headersForPost,
                        timeout: Duration = AjaxResult.defaultTimeout
                      )( implicit reader: Reads[T],
                                  writerP: Writes[V],
                                  classtag: ClassTag[T],
                                  xpos: Position
                      ): Result[T] = {
    val vars = variables.map( v => Json.toJson(v) match {
      case x: JsObject => x
      case x =>
        log.warning(s"Did not get a JsObject converting ${variables}: ${x}")
        throw new Exception(s"Did not get a JsObject converting ${variables}: ${x}")
    } )
    request(query,vars,operation,headers,timeout)
  }
}
