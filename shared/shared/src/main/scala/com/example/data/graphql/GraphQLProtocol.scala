package com.example.data.graphql

import io.swagger.annotations._
import scala.annotation.meta._
import play.api.libs.json._
import scala.reflect.ClassTag
import org.scalactic.source.Position
import com.example.data.rest.JsonException

object GraphQLProtocol {

  @ApiModel(description = "A GraphQL Request")
  case class GraphQLRequest(
      @(ApiModelProperty @field)(value="The GraphQL query", required=true)
      query: String,
      @(ApiModelProperty @field)(value="The operationName, optional", required=false)
      operationName: Option[String],
      @(ApiModelProperty @field)(value="The variables (optional object)", dataType="object", required=false)
      variables: Option[JsObject]
  )


// Errors from graphql api:
// {"data":null,"errors":[
//  { "message":"Cannot query field 'xxx' on type 'Query'. (line 1, column 3):\n{ xxx }\n  ^",
//    "locations":[{"line":1,"column":3}]
//  }]}
// {"error":"Syntax error while parsing GraphQL query. Invalid input 'x', expected OperationDefinition, FragmentDefinition or TypeSystemDefinition (line 1, column 1):\nxxx\n^"}

  @ApiModel(description = "An error location")
  case class ErrorLocation(
      @(ApiModelProperty @field)(value="The line number", dataType="int", required=false)
      line: Option[Int],
      @(ApiModelProperty @field)(value="The column number", dataType="int", required=false)
      column: Option[Int]
  ) {
    @ApiModelProperty(hidden = true)
    override
    def toString() = s"""${line.getOrElse("unknown")}:${column.map(i=>i.toString()).getOrElse("unknown")}"""
  }

  @ApiModel(description = "An Error message")
  case class ErrorMessage(
      @(ApiModelProperty @field)(value="The text of the error message", required=false)
      message: Option[String],
      @(ApiModelProperty @field)(value="The location of the error", required=false)
      locations: Option[List[ErrorLocation]]
  ) {
    @ApiModelProperty(hidden = true)
    override
    def toString() = s"""${message.getOrElse("")} from location ${locations.map(l=>l.mkString(", ")).getOrElse("")}"""
  }

  @ApiModel(description = "An GraphQL response")
  case class GraphQLResponse(
      @(ApiModelProperty @field)(value="The response data", required=false)
      data: Option[JsValue],
      @(ApiModelProperty @field)(value="An error message", dataType="string", required=false)
      error: Option[String],
      @(ApiModelProperty @field)(value="Error message(s)", required=false)
      errors: Option[List[ErrorMessage]],
      @(ApiModelProperty @field)(value="Extensions", required=false)
      extensions: Option[JsValue]
  ) {

    def getError() = {
      val er = error.toList:::errors.map(l=>l.map(e=>e.toString())).getOrElse(Nil)
      er.mkString("\n")
    }

    @ApiModelProperty(hidden = true)
    def getResponse[T]( url: String)( implicit reads: Reads[T], classtag: ClassTag[T], xpos: Position ): Option[T] = {
      data.map { json =>
        Json.fromJson[T](json) match {
          case JsSuccess(t,path) =>
            t
          case JsError(errors) =>
            val clsname = classtag.runtimeClass.getName
            val s = errors.map { entry =>
              val (path, verrs) = entry
              s"""\n  ${path}: ${verrs.map(e=>e.message).mkString("\n    ","\n    ","")}"""
            }
            throw new JsonException(s"""Error unmarshalling a ${clsname} from request ${url}: ${s}""")
        }
      }
    }

    @ApiModelProperty(hidden = true)
    def hasData = data.isDefined
  }

  implicit val errorLocationFormat = Json.format[ErrorLocation]
  implicit val ErrorMessageFormat = Json.format[ErrorMessage]
  implicit val graphQLResponseFormat = Json.format[GraphQLResponse]

}
