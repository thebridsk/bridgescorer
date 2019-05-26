package com.example.data.graphql

import scala.annotation.meta._
import play.api.libs.json._
import scala.reflect.ClassTag
import org.scalactic.source.Position
import com.example.data.rest.JsonException
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema

object GraphQLProtocol {

//      anyOf:
//        - type: string
//        - type: number
//        - type: integer
//        - type: boolean
//        - type: array
//          items: {}
//        - type: object

  @Schema( name="string", `type`="string")
  class VJsString

  @Schema( name="number", `type`="number")
  class VJsNumber

  @Schema( name="integer", `type`="integer")
  class VJsInt

  @Schema( name="boolean", `type`="boolean")
  class VJsBoolean

  @Schema( name="object", `type`="object", requiredProperties=Array())
  class VJsObject

  @Schema( name="objectreq", `type`="object", requiredProperties=Array())
  class VJsObjectReq

  @Schema(
      name="anyvalue",
      description="Any valid JSON",
      oneOf=Array(
          classOf[VJsString],
          classOf[VJsNumber],
          classOf[VJsInt],
          classOf[VJsBoolean],
          classOf[VJsObject],
          classOf[VJsArray]
      ),
      nullable = true
  )
  class AnyValue

  @ArraySchema(
      minItems=0,
      uniqueItems=false,
      schema=new Schema(
          implementation=classOf[AnyValue]
      ),
      arraySchema=new Schema(
          name="Array",
          description="array of any value",
      )
  )
  class VJsArray

  @Schema(
      name="anyvaluedata",
      description="Any valid JSON",
      oneOf=Array(
          classOf[VJsString],
          classOf[VJsNumber],
          classOf[VJsInt],
          classOf[VJsBoolean],
          classOf[VJsObject],
          classOf[VJsArray]
      ),
      nullable = true
  )
  class AnyValueData

  @Schema(
      name="anyvalueext",
      description="Any valid JSON",
      oneOf=Array(
          classOf[VJsString],
          classOf[VJsNumber],
          classOf[VJsInt],
          classOf[VJsBoolean],
          classOf[VJsObject],
          classOf[VJsArray]
      ),
      nullable = true
  )
  class AnyValueExt

  @Schema(
      title = "GraphQLRequest - A GraphQL Request",
      description = "A GraphQL Request")
  case class GraphQLRequest(
      @Schema(description="The GraphQL query", required=true)
      query: String,
      @Schema(description="The operationName, optional", required=false)
      operationName: Option[String],
      @Schema(description="The variables (optional object)", implementation=classOf[VJsObjectReq], required=false)
      variables: Option[JsObject]
  )


// Errors from graphql api:
// {"data":null,"errors":[
//  { "message":"Cannot query field 'xxx' on type 'Query'. (line 1, column 3):\n{ xxx }\n  ^",
//    "locations":[{"line":1,"column":3}]
//  }]}
// {"error":"Syntax error while parsing GraphQL query. Invalid input 'x', expected OperationDefinition, FragmentDefinition or TypeSystemDefinition (line 1, column 1):\nxxx\n^"}

  @Schema(
      title ="ErrorLocation - An error location",
      description = "The error location of a GraphQL request")
  case class ErrorLocation(
      @Schema(description="The line number", `type`="integer", required=false)
      line: Option[Int],
      @Schema(description="The column number", `type`="integer", required=false)
      column: Option[Int]
  ) {
    @Schema(hidden = true)
    override
    def toString() = s"""${line.getOrElse("unknown")}:${column.map(i=>i.toString()).getOrElse("unknown")}"""
  }

  @Schema(
      title = "ErrorMessage - A GraphQL error message",
      description = "An Error message")
  case class ErrorMessage(
      @Schema(description="The text of the error message", required=false)
      message: Option[String],
      @ArraySchema(
          minItems=0,
          schema=new Schema(
              description="The location of the error",
              implementation=classOf[ErrorLocation]
          ),
          arraySchema=new Schema(
              required=false
          )
      )
      locations: Option[List[ErrorLocation]]
  ) {
    @Schema(hidden = true)
    override
    def toString() = s"""${message.getOrElse("")} from location ${locations.map(l=>l.mkString(", ")).getOrElse("")}"""
  }

  @Schema(
      title = "GraphQLResponse - An GraphQL response",
      description = "An GraphQL response"
  )
  case class GraphQLResponse(
      @Schema(description="The response data", implementation=classOf[AnyValueData], required=false)
      data: Option[JsValue],
      @Schema(name="error", description="An error message", `type`="string", required=false)
      error: Option[String],
      @ArraySchema(
          minItems=0,
          schema=new Schema(implementation=classOf[ErrorMessage]),
          uniqueItems=false,
          arraySchema = new Schema( description = "Error message(s)", required=false)
      )
      errors: Option[List[ErrorMessage]],
      @Schema(description="Extensions",  implementation=classOf[AnyValueExt], required=false)
      extensions: Option[JsValue]
  ) {

//    @Schema(hidden = true)
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
