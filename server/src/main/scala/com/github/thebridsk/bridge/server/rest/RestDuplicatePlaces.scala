package com.github.thebridsk.bridge.server.rest

import com.github.thebridsk.bridge.server.backend.BridgeService
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.MatchDuplicate
import akka.event.Logging
import akka.event.Logging._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.thebridsk.bridge.server.util.HasActorSystem
import akka.http.scaladsl.model.StatusCode
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.data.DuplicateSummary
import javax.ws.rs.Path
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.data.SystemTime
import akka.http.scaladsl.model.headers.Location
import scala.util.Success
import scala.util.Failure
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.GET
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerPlaces

/**
  * Rest API implementation for the board resource.
  * <p>
  * The REST API and all the methods are documented using
  * swagger annotations.
  */
@Path("/rest/duplicateplaces")
@Tags(Array(new Tag(name = "Duplicate")))
trait RestDuplicatePlaces extends HasActorSystem {

  private lazy val log = Logging(actorSystem, classOf[RestDuplicatePlaces])

  val restService: BridgeService

  val resName = "duplicateplaces"

  import UtilsPlayJson._

  /**
    * spray route for all the methods on this resource
    */
  val route = pathPrefix(resName) {
//    logRequest("route", DebugLevel) {
    getDuplicatePlaces
//      }
  }

  @GET
  @Operation(
    summary = "Get all player places for duplicate matches",
    description = "Returns the player places.",
    operationId = "getDuplicatePlaces",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The player places",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[PlayerPlaces])
          )
        )
      )
    )
  )
  def xxxgetDuplicatePlaces() = {}
  val getDuplicatePlaces =
//    logRequest("getDuplicatePlaces", DebugLevel ) { logResult("getDuplicatePlaces", DebugLevel ) {
    get {
      pathEndOrSingleSlash {
        resource(restService.getDuplicatePlaceResults())
      }
    }
//    }}

}
