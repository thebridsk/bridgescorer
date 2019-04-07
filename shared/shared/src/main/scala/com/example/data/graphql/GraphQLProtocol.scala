package com.example.data.graphql

import scala.annotation.meta._
import play.api.libs.json._
import scala.reflect.ClassTag
import org.scalactic.source.Position
import com.example.data.rest.JsonException
import io.swagger.v3.oas.annotations.media.Schema

object GraphQLProtocol {

  @Schema(description = "A GraphQL Request")
  case class GraphQLRequest(
      @Schema(description="The GraphQL query", required=true)
      query: String,
      @Schema(description="The operationName, optional", required=false, `type`="string")
      operationName: Option[String],
      @Schema(description="The variables (optional object)", `type`="object", required=false)
      variables: Option[JsObject]
  )


// Errors from graphql api:
// {"data":null,"errors":[
//  { "message":"Cannot query field 'xxx' on type 'Query'. (line 1, column 3):\n{ xxx }\n  ^",
//    "locations":[{"line":1,"column":3}]
//  }]}
// {"error":"Syntax error while parsing GraphQL query. Invalid input 'x', expected OperationDefinition, FragmentDefinition or TypeSystemDefinition (line 1, column 1):\nxxx\n^"}

  @Schema(description = "An error location")
  case class ErrorLocation(
      @Schema(description="The line number", `type`="int", required=false)
      line: Option[Int],
      @Schema(description="The column number", `type`="int", required=false)
      column: Option[Int]
  ) {
    @Schema(hidden = true)
    override
    def toString() = s"""${line.getOrElse("unknown")}:${column.map(i=>i.toString()).getOrElse("unknown")}"""
  }

  @Schema(description = "An Error message")
  case class ErrorMessage(
      @Schema(description="The text of the error message", required=false)
      message: Option[String],
      @Schema(description="The location of the error", required=false)
      locations: Option[List[ErrorLocation]]
  ) {
    @Schema(hidden = true)
    override
    def toString() = s"""${message.getOrElse("")} from location ${locations.map(l=>l.mkString(", ")).getOrElse("")}"""
  }

  @Schema(description = "An GraphQL response")
  case class GraphQLResponse(
      @Schema(description="The response data", required=false)
      data: Option[JsValue],
      @Schema(description="An error message", `type`="string", required=false)
      error: Option[String],
      @Schema(description="Error message(s)", required=false)
      errors: Option[List[ErrorMessage]],
      @Schema(description="Extensions", required=false)
      extensions: Option[JsValue]
  ) {

    @Schema(hidden = true)
    def getError() = {
      val er = error.toList:::errors.map(l=>l.map(e=>e.toString())).getOrElse(Nil)
      er.mkString("\n")
    }

    @Schema(hidden = true)
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

    @Schema(hidden = true)
    def hasData = data.isDefined
  }

  implicit val errorLocationFormat = Json.format[ErrorLocation]
  implicit val ErrorMessageFormat = Json.format[ErrorMessage]
  implicit val graphQLResponseFormat = Json.format[GraphQLResponse]

}
