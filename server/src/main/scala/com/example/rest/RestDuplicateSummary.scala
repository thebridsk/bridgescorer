package com.example.rest

import com.example.backend.BridgeService
import com.example.data.Board
import com.example.data.MatchDuplicate
import akka.event.Logging
import akka.event.Logging._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.example.util.HasActorSystem
import akka.http.scaladsl.model.StatusCode
import com.example.data.Id
import com.example.data.DuplicateSummary
import javax.ws.rs.Path
import com.example.data.RestMessage
import com.example.data.SystemTime
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

/**
 * Rest API implementation for the board resource.
 * <p>
 * The REST API and all the methods are documented using
 * swagger annotations.
 */
@Path( "/rest/duplicatesummaries" )
@Tags( Array( new Tag(name="Duplicate")))
trait RestDuplicateSummary extends HasActorSystem {

  private lazy val log = Logging(actorSystem, classOf[RestDuplicate])

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
                          schema = new Schema( implementation=classOf[DuplicateSummary] )
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
