package com.github.thebridsk.bridge.server.rest

import com.github.thebridsk.bridge.server.backend.BridgeService
import com.github.thebridsk.bridge.data.MatchChicago
import akka.event.Logging
import akka.event.Logging._
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import com.github.thebridsk.bridge.server.util.HasActorSystem
import javax.ws.rs.Path
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.data.Id
import scala.util.Sorting
import akka.http.scaladsl.model.headers.Location
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object RestChicago {
  implicit class OrdFoo(val x: MatchChicago)
      extends AnyVal
      with Ordered[MatchChicago] {
    def compare(that: MatchChicago) = Id.idComparer(that.id, x.id)
  }

}

import RestChicago._
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.DELETE
import com.github.thebridsk.bridge.server.backend.BridgeNestedResources

/**
  * Rest API implementation for the board resource.
  * <p>
  * The REST API and all the methods are documented using
  * swagger annotations.
  */
@Path("/rest/chicagos")
@Tags(Array(new Tag(name = "Chicago")))
trait RestChicago extends HasActorSystem {

  /**
    * The bridge service backend
    */
  implicit val restService: BridgeService
  lazy val store = restService.chicagos

  val resName = "chicagos"

  val nestedBoards = new RestNestedChicagoRound

  import UtilsPlayJson._

  /**
    * spray route for all the methods on this resource
    */
  val route = pathPrefix(resName) {
//    logRequest("route", DebugLevel) {
    getChicago ~ getChicagos ~ postChicago ~ putChicago ~ deleteChicago ~ nested
//      }
  }

  @GET
  @Operation(
    summary = "Get all chicago matches",
    description = "Returns a list of matches.",
    operationId = "getChicagos",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "A list of matches, as a JSON array",
        content = Array(
          new Content(
            mediaType = "application/json",
            array = new ArraySchema(
              minItems = 0,
              uniqueItems = true,
              schema = new Schema(implementation = classOf[MatchChicago])
            )
          )
        )
      )
    )
  )
  def xxxgetChicagos() = {}
  val getChicagos = pathEnd {
    get {
      resourceMap(store.readAll())
    }
  }

  @Path("/{chiId}")
  @GET
  @Operation(
    summary = "Get the match by ID",
    description = "Returns the specified chicago match.",
    operationId = "getChicagoById",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the match to get",
        in = ParameterIn.PATH,
        name = "chiId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The requested Chicago match, as a JSON object",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[MatchChicago])
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "Does not exist",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[RestMessage])
          )
        )
      )
    )
  )
  def xxxgetChicago() = {}
  val getChicago = logRequest("RestChicago.getChicago", DebugLevel) {
    logResult("RestChicago.postChicago") {
      get {
        path("""[a-zA-Z0-9]+""".r) { id =>
          resource(store.select(id).read())
        }
      }
    }
  }

  val nested = logRequest("RestChicago.nested", DebugLevel) {
    logResult("RestChicago.nested") {
      pathPrefix("""[a-zA-Z0-9]+""".r) { id: Id.MatchChicago =>
        import BridgeNestedResources._
        val selected = store.select(id)
        nestedBoards.route(selected.resourceRounds)
      }
    }
  }

  @POST
  @Operation(
    summary = "Create a chicago match",
    operationId = "createChicago",
    requestBody = new RequestBody(
      description = "Chicago Match to create",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[MatchChicago])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description = "The created match's JSON",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[MatchChicago])
          )
        ),
        headers = Array(
          new Header(
            name = "Location",
            description = "The URL of the newly created resource",
            schema = new Schema(implementation = classOf[String])
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
  def xxxpostChicago() = {}
  val postChicago =
    logRequest("RestChicago.postChicago") {
      logResult("RestChicago.postChicago") {
        pathEnd {
          post {
            entity(as[MatchChicago]) { chi =>
              resourceCreated(resName, store.createChild(chi))
            }
          }
        }
      }
    }
  @Path("/{chiId}")
  @PUT
  @Operation(
    summary = "Update a chicago match",
    description =
      "Update a chicago match.  The id of the chicago match in the body is replaced with chiId",
    operationId = "updateChicago",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the match to get",
        in = ParameterIn.PATH,
        name = "chiId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    requestBody = new RequestBody(
      description = "Chicago Match to update",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[MatchChicago])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "204",
        description = "The match was updated"
      ),
      new ApiResponse(
        responseCode = "404",
        description = "Does not exist",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[RestMessage])
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
  def xxxputChicago() = {}
  val putChicago =
    logRequest("RestChicago.putChicago") {
      logResult("RestChicago.putChicago") {
        path("""[a-zA-Z0-9]+""".r) { id =>
          put {
            entity(as[MatchChicago]) { chi =>
              resourceUpdated(store.select(id).update(chi))
            }
          }
        }
      }
    }
  @Path("/{chiId}")
  @DELETE
  @Operation(
    summary = "Delete a match by ID",
    operationId = "deleteChicagoById",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the match to delete",
        in = ParameterIn.PATH,
        name = "chiId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "204",
        description = "Chicago match deleted."
      )
    )
  )
  def xxxdeleteChicago() = {}
  val deleteChicago = path("""[a-zA-Z0-9]+""".r) { id =>
    {
      delete {
        resourceDelete(store.select(id).delete())
      }
    }
  }
}
