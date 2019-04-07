package com.example.rest

import com.example.data.Ack
import akka.event.Logging
import akka.event.Logging._
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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.GET


/**
 * Rest API implementation for the logger config
 * <p>
 * The REST API and all the methods are documented using
 * swagger annotations.
 */
@Path("/rest/names")
@Tags( Array( new Tag(name="Server")))
trait RestNames extends HasActorSystem {

  /**
   * The bridge service backend
   */
  implicit val restService: BridgeService

  import UtilsPlayJson._

  /**
   * spray route for all the methods on this resource
   */
  def route = pathPrefix("names") {
    getNames
  }

  @GET
  @Operation(
      summary = "Get all known names",
      operationId = "getNames",
      responses = Array(
          new ApiResponse(
              responseCode = "200",
              description = "The names as a list of string objects",
              content = Array(
                  new Content(
                      mediaType = "application/json",
                      array = new ArraySchema(
                          minItems = 0,
                          uniqueItems = true,
                          schema = new Schema( implementation=classOf[String] )
                      )
                  )
              )
          )
      )
  )
  def getNames = pathEndOrSingleSlash {
    get {
      resourceList( restService.getAllNames() )
    }
  }
}
