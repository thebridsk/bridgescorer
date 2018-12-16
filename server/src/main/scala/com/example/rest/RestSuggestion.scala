package com.example.rest

import com.example.data.Ack
import akka.event.Logging
import akka.event.Logging._
import io.swagger.annotations._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import com.example.util.HasActorSystem
import java.util.Date
import com.example.backend.BridgeService
import javax.ws.rs.Path
import com.example.data.RestMessage
import scala.util.Success
import scala.util.Failure
import com.example.data.duplicate.suggestion.DuplicateSuggestions
import com.example.data.duplicate.suggestion.DuplicateSuggestionsCalculation
import com.example.backend.resource.Result

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Rest API implementation for the logger config
 * <p>
 * The REST API and all the methods are documented using
 * swagger annotations.
 */
@Path("/rest/suggestions")
@Api(tags= Array("Duplicate"), description = "Get pairs suggestion", produces="application/json", protocols="http, https")
trait RestSuggestion extends HasActorSystem {

  /**
   * The bridge service backend
   */
  implicit val restService: BridgeService

  import UtilsPlayJson._

  /**
   * spray route for all the methods on this resource
   */
  def route = pathPrefix("suggestions") {
    suggestion
  }

  @ApiOperation(value = "Get a suggestion of pairings", notes = "", response=classOf[String], nickname = "suggestion", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "The 8 names of the players and the number of suggestions wanted", dataTypeClass = classOf[DuplicateSuggestions], required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 201, message = "The suggestion", response=classOf[DuplicateSuggestions]),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[RestMessage])
  ))
  def suggestion = pathEndOrSingleSlash {
    post {
      entity(as[DuplicateSuggestions]) { input =>
        val f = restService.getDuplicateSummaries().map { rds =>
          rds match {
            case Right(ds) =>
              val output = DuplicateSuggestionsCalculation.calculate(input, ds)
              Result(output)
            case Left( error ) =>
              Result(error)
          }
        }
        resourceCreatedNoLocationHeader( f )
      }
    }
  }
}
