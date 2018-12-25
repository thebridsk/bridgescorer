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
import com.example.rest2.AjaxErrorReturn
import com.example.data.graphql.GraphQLProtocol._
import scala.scalajs.js
import scala.scalajs.js.JSON

class Query[Variables](
                        query: String,
                        client: GraphQLBaseClient
                      )(
                        implicit
                          writer: Writes[Variables],
                          xpos: Position
                      ) {

  import GraphQLBaseClient._

  def execute( variables: Option[Variables],
               operation: Option[String] = None,
               headers: Map[String, String] = headersForPost,
               timeout: Duration = AjaxResult.defaultTimeout
             ): AjaxResult[GraphQLResponse] = {
    client.requestVars(query, variables, operation, headers, timeout)
  }

}

class GraphQLException( val error: GraphQLResponse, cause: Throwable = null ) extends Exception( error.getError(), cause )

object GraphQLBaseClient {

  val log = Logger("bridge.GraphQLBaseClient")

  val headersForPost=Map("Content-Type" -> "application/json; charset=UTF-8",
                         "Accept" -> "application/json")

  def queryToJson( query: String, variables: Option[JsObject]= None, operation: Option[String] = None ) = {
    JsObject(
        operation.map( v => "operationName" -> JsString(v) ).toList :::
        variables.map( v => "variables" -> v ).toList :::
        "query" -> JsString(query) :: Nil
    )

  }

}

class GraphQLBaseClient(
                      val url: String
                    )(
                      implicit
                        executor: ExecutionContext
                    ) {
  import GraphQLBaseClient._
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

  def requestWithBody(
      body: js.Object,
      headers: Map[String, String] = headersForPost,
      timeout: Duration = AjaxResult.defaultTimeout
    )(
      implicit
        xpos: Position
  ): AjaxResult[GraphQLResponse] = {
    val data = JSON.stringify(body)
    log.fine( s"GraphQLBaseClient.request(${xpos.line}): sending ${data}" )
    val rr = AjaxResult.post(url, data, timeout, headers )
    rr.map { wrapper =>
      val body = wrapper.responseText
      log.fine( s"GraphQLBaseClient.request(${xpos.line}): received ${body}" )
      val r = AjaxResult.fromJson[GraphQLResponse](body, url)
      if (r.data.isEmpty || r.data.get == JsNull) throw new AjaxErrorReturn( wrapper.status, wrapper.responseText, rr )
      r
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
    log.fine( s"GraphQLBaseClient.request(${xpos.line}): sending ${data}" )
    val rr = AjaxResult.post(url, data, timeout, headers )
    rr.map { wrapper =>
      val body = wrapper.responseText
      log.fine( s"GraphQLBaseClient.request(${xpos.line}): received ${body}" )
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
