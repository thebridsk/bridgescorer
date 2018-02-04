package com.example.service.graphql

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import play.api.libs.json._
import sangria.marshalling.playJson._
import sangria.parser.QueryParser
import sangria.execution.Executor
import sangria.execution.deferred.DeferredResolver
import sangria.execution.QueryAnalysisError
import sangria.execution.ErrorWithResolver
import scala.util.Failure
import com.example.backend.BridgeService
import akka.http.scaladsl.server.Route
import com.example.data.rest.JsonSupport._
import com.example.rest.UtilsPlayJson._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success
import io.swagger.annotations._
import javax.ws.rs.Path
import utils.logging.Logger

object GraphQLRoute {
  val log = Logger[GraphQLRoute]
}

@Path( "" )
@Api( tags = Array("Server"),
      description = "Execute GraphQL requests.", protocols="http, https")
trait GraphQLRoute {
  import GraphQLRoute._

  implicit val restService: BridgeService

  val query = new Query()

  implicit val jsObjectReads = Reads.JsObjectReads


  @Path( "graphql" )
  @ApiOperation(
      value = "Make a GraphQL request",
      notes = "",
      response=classOf[Array[Byte]],
      nickname = "graphql",
      httpMethod = "POST",
      code=200,
      produces="application/json"
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body",
                         value = "The request.  A JSON object with three fields: query (string), operationName (optional string), variables (optional object)",
                         dataType = "object",
                         required = true,
                         paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(
        code = 200,
        message = "The result of the GraphQL request.",
    ),
    new ApiResponse(
        code = 400,
        message = "Bad request"
    ),
    new ApiResponse(
        code = 500,
        message = "Internal server error"
    )
  ))
  def graphQLRoute: Route =
    pathPrefix("v1") {
      (post & path("graphql")) {
        entity(as[JsObject]) { requestJson ⇒
          val f = query.query(requestJson, restService)
          onComplete(f) {
            case Success((statusCode,obj)) =>
              complete(statusCode, obj)
            case Failure(ex) =>
              log.warning( "Internal server error", ex )
              complete((InternalServerError, JsObject( Seq(("error", JsString(s"An error occurred: ${ex.getMessage}")))) ))
          }
        }
      }
    }
}
