package com.github.thebridsk.bridge.server.rest

import akka.http.scaladsl.server.Directives._
import com.github.thebridsk.bridge.server.util.HasActorSystem
import com.github.thebridsk.bridge.server.backend.BridgeService
import jakarta.ws.rs.Path
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.GET
import akka.http.scaladsl.server.Route

/**
  * Rest API implementation for the logger config
  * <p>
  * The REST API and all the methods are documented using
  * swagger annotations.
  */
@Path("/rest/names")
@Tags(Array(new Tag(name = "Server")))
trait RestNames extends HasActorSystem {

  /**
    * The bridge service backend
    */
  implicit val restService: BridgeService

  import UtilsPlayJson._

  /**
    * spray route for all the methods on this resource
    */
  val route: Route = pathPrefix("names") {
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
              schema = new Schema(implementation = classOf[String])
            )
          )
        )
      )
    )
  )
  def xxxgetNames(): Unit = {}
  val getNames: Route = pathEndOrSingleSlash {
    get {
      resourceList(restService.getAllNames())
    }
  }
}
