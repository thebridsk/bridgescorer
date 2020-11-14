package com.github.thebridsk.bridge.server.service.graphql

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import play.api.libs.json._
import scala.util.Failure
import com.github.thebridsk.bridge.server.backend.BridgeService
import akka.http.scaladsl.server.Route
import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._
import scala.util.Success
import javax.ws.rs.Path
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.graphql.GraphQLProtocol.GraphQLResponse
import com.github.thebridsk.bridge.data.graphql.GraphQLProtocol.GraphQLRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import javax.ws.rs.POST
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.annotations.tags.Tag

object GraphQLRoute {
  val log: Logger = Logger[GraphQLRoute]()
}

@Path("")
@Tags(Array(new Tag(name = "Server")))
trait GraphQLRoute {
  import GraphQLRoute._

  implicit val restService: BridgeService

  val query = new Query()

  implicit val jsObjectReads: Reads.JsObjectReads.type = Reads.JsObjectReads
  @Path("graphql")
  @POST
  @Operation(
    summary = "Make a GraphQL request",
    operationId = "graphql",
    requestBody = new RequestBody(
      description =
        "The request.  A JSON object with three fields: query (string), operationName (optional string), variables (optional object)",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[GraphQLRequest])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The result of the GraphQL request.",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[GraphQLResponse])
          )
        )
      ),
      new ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[GraphQLResponse])
          )
        )
      ),
      new ApiResponse(
        responseCode = "500",
        description = "Internal server error",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[GraphQLResponse])
          )
        )
      )
    )
  )
  def xxxgraphQLRoute: Unit = {}
  val graphQLRoute: Route =
    pathPrefix("v1") {
      logRequestResult("GraphQLRoute") {
        (post & path("graphql")) {
          entity(as[JsObject]) { requestJson =>
            val f = query.query(requestJson, restService)
            onComplete(f) {
              case Success((statusCode, obj)) =>
                complete(statusCode, obj)
              case Failure(ex) =>
                log.warning("Internal server error", ex)
                complete(
                  (
                    InternalServerError,
                    JsObject(
                      Seq(
                        (
                          "error",
                          JsString(s"An error occurred: ${ex.getMessage}")
                        )
                      )
                    )
                  )
                )
            }
          }
        }
      }
    }
}
