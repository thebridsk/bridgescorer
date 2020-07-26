package com.github.thebridsk.bridge.server.rest

import akka.http.scaladsl.server.Directives._
import com.github.thebridsk.bridge.server.util.HasActorSystem
import com.github.thebridsk.bridge.server.backend.BridgeService
import javax.ws.rs.Path
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.data.duplicate.suggestion.DuplicateSuggestions
import com.github.thebridsk.bridge.data.duplicate.suggestion.DuplicateSuggestionsCalculation
import com.github.thebridsk.bridge.server.backend.resource.Result

import scala.concurrent.ExecutionContext.Implicits.global
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.POST
import akka.http.scaladsl.server.Route

/**
  * Rest API implementation for the logger config
  * <p>
  * The REST API and all the methods are documented using
  * swagger annotations.
  */
@Path("/rest/suggestions")
@Tags(Array(new Tag(name = "Duplicate")))
trait RestSuggestion extends HasActorSystem {

  /**
    * The bridge service backend
    */
  implicit val restService: BridgeService

  import UtilsPlayJson._

  /**
    * spray route for all the methods on this resource
    */
  val route: Route = pathPrefix("suggestions") {
    suggestion
  }

  @POST
  @Operation(
    summary = "Get a suggestion of pairings",
    operationId = "suggestion",
    requestBody = new RequestBody(
      description =
        "The 8 names of the players and the number of suggestions wanted",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[DuplicateSuggestions])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description = "The suggestion",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[DuplicateSuggestions])
          )
        )
      ),
      new ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[RestMessage])
          )
        )
      )
    )
  )
  def xxxsuggestion(): Unit = {}
  val suggestion: Route = pathEndOrSingleSlash {
    post {
      entity(as[DuplicateSuggestions]) { input =>
        val f = restService.getDuplicateSummaries().map { rds =>
          rds match {
            case Right(ds) =>
              val output = DuplicateSuggestionsCalculation.calculate(input, ds)
              Result(output)
            case Left(error) =>
              Result(error)
          }
        }
        resourceCreatedNoLocationHeader(f)
      }
    }
  }
}
