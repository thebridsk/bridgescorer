package com.example.graphql

import play.api.libs.json._
import utils.logging.Logger
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import com.example.rest2.AjaxResult
import org.scalactic.source.Position
import com.example.data.rest.JsonSupport._
import scala.reflect.ClassTag
import scala.util.Success
import scala.util.Failure
import com.example.data.rest.JsonException
import scala.util.Try
import scala.concurrent.ExecutionContext
import com.example.source.SourcePosition
import org.scalajs.dom.ext.AjaxException
import com.example.rest2.AjaxErrorReturn

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

case class GraphQLResponse( data: Option[JsValue], error: Option[String], errors: Option[List[ErrorMessage]], extensions: Option[JsValue] ) {

  def getError() = {
    val er = error.toList:::errors.map(l=>l.map(e=>e.toString())).getOrElse(Nil)
    er.mkString("\n")
  }

  def getResponse[T]( url: String)( implicit reads: Reads[T], classtag: ClassTag[T], xpos: Position ): Option[T] = {
    data.map { j =>
      AjaxResult.fromJsonValue[T]( j, url )
    }
  }

  def hasData = data.isDefined
}

class Query[Variables](
                        query: String,
                        client: GraphQLRequest
                      )(
                        implicit
                          writer: Writes[Variables],
                          xpos: Position
                      ) {

  import GraphQLRequest._

  def execute( variables: Option[Variables],
               operation: Option[String] = None,
               headers: Map[String, String] = headersForPost,
               timeout: Duration = AjaxResult.defaultTimeout
             ): AjaxResult[GraphQLResponse] = {
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
            ), None, None
        )
    }
  }

  def request( query: String,
               variables: Option[JsObject]= None,
               operation: Option[String] = None,
               headers: Map[String, String] = headersForPost,
               timeout: Duration = AjaxResult.defaultTimeout
             )(
               implicit
                 xpos: Position
             ): AjaxResult[GraphQLResponse] = {
    val data = writeJson( queryToJson( query, variables, operation ) )
    val rr = AjaxResult.post(url, data, timeout, headers )
    rr.map { wrapper =>
      val body = wrapper.responseText
      val r = AjaxResult.fromJson[GraphQLResponse](body, url)
      if (r.data.isEmpty || r.data.get == JsNull) throw new AjaxErrorReturn( wrapper.status, wrapper.responseText, rr )
      r
    }
  }

  def requestVars[V]( query: String,
                      variables: Option[V],
                      operation: Option[String] = None,
                      headers: Map[String, String] = headersForPost,
                      timeout: Duration = AjaxResult.defaultTimeout
                    )(
                      implicit
                        writerP: Writes[V],
                        xpos: Position
                    ): AjaxResult[GraphQLResponse] = {
    val vars = variables.map( v => Json.toJson(v) match {
      case x: JsObject => x
      case x =>
        log.warning(s"Did not get a JsObject converting ${variables}: ${x}")
        throw new Exception(s"Did not get a JsObject converting ${variables}: ${x}")
    } )
    request(query,vars,operation,headers,timeout)
  }
}
