package com.github.thebridsk.bridge.server.rest

import com.github.thebridsk.bridge.server.backend.BridgeService
import akka.event.Logging
import akka.http.scaladsl.server.Directives._
import com.github.thebridsk.bridge.server.util.HasActorSystem
import com.github.thebridsk.bridge.data.DuplicateSummary
import javax.ws.rs.Path
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.GET

/**
  * Rest API implementation for the board resource.
  * <p>
  * The REST API and all the methods are documented using
  * swagger annotations.
  */
@Path("/rest/duplicatesummaries")
@Tags(Array(new Tag(name = "Duplicate")))
trait RestDuplicateSummary extends HasActorSystem {

  private lazy val log = Logging(actorSystem, classOf[RestDuplicateSummary])

  val restService: BridgeService

  val resName = "duplicatesummaries"

  import UtilsPlayJson._

  /**
    * spray route for all the methods on this resource
    */
  val route = pathPrefix(resName) {
//    logRequest("route", DebugLevel) {
    getDuplicateSummaries
//      }
  }

  @GET
  @Operation(
    summary = "Get all duplicate matches",
    description = "Returns a list of matches.",
    operationId = "getDuplicateSummaries",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "A list of match summaries, as a JSON array",
        content = Array(
          new Content(
            mediaType = "application/json",
            array = new ArraySchema(
              minItems = 0,
              uniqueItems = true,
              schema = new Schema(implementation = classOf[DuplicateSummary])
            )
          )
        )
      )
    )
  )
  def xxxgetDuplicateSummaries() = {}
  val getDuplicateSummaries =
//    logRequest("routeDuplicateSummaries", DebugLevel ) { logResult("routeDuplicateSummaries", DebugLevel ) {
    get {
      pathEndOrSingleSlash {
        resourceList(restService.getDuplicateSummaries())
      }
    }
//    }}

}
