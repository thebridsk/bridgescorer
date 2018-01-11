package com.example.rest

import com.example.backend.BridgeService
import com.example.data.Board
import com.example.data.MatchDuplicate
import akka.event.Logging
import akka.event.Logging._
import io.swagger.annotations._
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

/**
 * Rest API implementation for the board resource.
 * <p>
 * The REST API and all the methods are documented using
 * swagger annotations.
 */
@Path( "/rest/duplicatesummaries" )
@Api(tags= Array("Duplicate"), description = "Operations about duplicate summaries.", produces="application/json", protocols="http, https")
trait RestDuplicateSummary extends HasActorSystem {

  private lazy val log = Logging(actorSystem, classOf[RestDuplicate])

  val restService: BridgeService

  val resName = "duplicatesummaries"

  import UtilsPlayJson._

  /**
   * spray route for all the methods on this resource
   */
  def route = pathPrefix(resName) {
//    logRequest("route", DebugLevel) {
        getDuplicateSummaries
//      }
  }

  @ApiOperation(value = "Get all duplicate matches", notes = "Returns a list of matches.", response=classOf[DuplicateSummary], responseContainer="List", nickname = "getDuplicateSummary", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "A list of match summaries, as a JSON array", response=classOf[DuplicateSummary], responseContainer="List")
  ))
  def getDuplicateSummaries =
//    logRequest("routeDuplicateSummaries", DebugLevel ) { logResult("routeDuplicateSummaries", DebugLevel ) {
      get {
        pathEndOrSingleSlash {
          resourceList(restService.getDuplicateSummaries())
        }
      }
//    }}

}
