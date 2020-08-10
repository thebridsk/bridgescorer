package com.github.thebridsk.bridge.server.rest

import com.github.thebridsk.bridge.server.backend.BridgeService
import akka.event.Logging
import akka.http.scaladsl.server.Directives._
import com.github.thebridsk.bridge.server.util.HasActorSystem
import javax.ws.rs.Path
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.GET
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerPlaces
import akka.http.scaladsl.server.Route

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
  val route: Route = pathPrefix(resName) {
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
  def xxxgetDuplicatePlaces(): Unit = {}
  val getDuplicatePlaces: Route =
//    logRequest("getDuplicatePlaces", DebugLevel ) { logResult("getDuplicatePlaces", DebugLevel ) {
    get {
      pathEndOrSingleSlash {
        resource(restService.getDuplicatePlaceResults())
      }
    }
//    }}

}
