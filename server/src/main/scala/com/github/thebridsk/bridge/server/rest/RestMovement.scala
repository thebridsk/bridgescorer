package com.github.thebridsk.bridge.server.rest

import com.github.thebridsk.bridge.server.backend.BridgeService
import akka.event.Logging
import akka.event.Logging._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import com.github.thebridsk.bridge.server.util.HasActorSystem
import javax.ws.rs.Path
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.data.Movement
import scala.concurrent.ExecutionContext.Implicits.global
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
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
import scala.concurrent.Future
import com.github.thebridsk.bridge.server.backend.resource.Result
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Route

/**
  * Rest API implementation for the movement resource.
  * <p>
  * The REST API and all the methods are documented using
  * swagger annotations.
  */
@Path("/rest/movements")
@Tags(Array(new Tag(name = "Duplicate")))
trait RestMovement extends HasActorSystem {

  lazy val testlog: LoggingAdapter = Logging(actorSystem, classOf[RestMovement])

  val resName = "movements"

  import UtilsPlayJson._

  /**
    * The bridge service backend
    */
  implicit val restService: BridgeService

  lazy val store = restService.movements

  /**
    * spray route for all the methods on this resource
    */
  val route: Route = pathPrefix(resName) {
    logRequest("movements", DebugLevel) {
      logResult("movements", DebugLevel) {
        getMovement ~ getMovements ~ postMovement ~ putMovement ~ deleteMovement
      }
    }
  }

  @GET
  @Operation(
    summary = "Get all movements",
    description = "Returns a list of movements.",
    operationId = "getMovements",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "A list of movements, as a JSON array",
        content = Array(
          new Content(
            mediaType = "application/json",
            array = new ArraySchema(
              minItems = 0,
              uniqueItems = true,
              schema = new Schema(implementation = classOf[Movement])
            )
          )
        )
      )
    )
  )
  def xxxgetMovements(): Unit = {}
  val getMovements: Route = pathEnd {
    get {
      resourceMap(store.readAll())
    }
  }

  @Path("/{movementId}")
  @GET
  @Operation(
    summary = "Get the movement by ID",
    operationId = "getMovementById",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the movement to get",
        in = ParameterIn.PATH,
        name = "movementId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The movement, as a JSON object",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[Movement])
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
  def xxxgetMovement(): Unit = {}
  val getMovement: Route = logRequest("getMovement", DebugLevel) {
    get {
      path("""[a-zA-Z0-9]+""".r) { sid =>
        val id = Movement.id(sid)
        resource(store.select(id).read())
      }
    }
  }

  import scala.language.implicitConversions
  implicit def addIdToFuture(
      f: Future[Result[Movement]]
  ): Future[Result[(String, Movement)]] =
    f.map { r =>
      r match {
        case Right(md) => Right((md.id.id, md))
        case Left(e)   => Left(e)
      }
    }

  @POST
  @Operation(
    summary = "Create a movement",
    operationId = "createMovement",
    requestBody = new RequestBody(
      description = "movement to create",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[Movement])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description = "The created movement's JSON",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[Movement])
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
  def xxxpostMovement(): Unit = {}
  val postMovement: Route = pathEnd {
    post {
      entity(as[Movement]) { movement =>
        resourceCreated(resName, store.createChild(movement), Created)
      }
    }
  }
  @Path("/{movementId}")
  @PUT
  @Operation(
    summary = "Update a movement",
    operationId = "updateMovement",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the movement to update",
        in = ParameterIn.PATH,
        name = "movementId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    requestBody = new RequestBody(
      description = "The updated duplicate Match",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[Movement])
        )
      )
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "Movement updated"),
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
  def xxxputMovement(): Unit = {}
  val putMovement: Route =
    put {
      path("""[a-zA-Z0-9]+""".r) { sid =>
        entity(as[Movement]) { movement =>
          val id = Movement.id(sid)
          resourceUpdated(store.select(id).update(movement.copy(name = id)))
        }
      }
    }
  @Path("/{movementId}")
  @DELETE
  @Operation(
    summary = "Delete a movement by ID",
    operationId = "deleteMovementById",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the movement to delete",
        in = ParameterIn.PATH,
        name = "movementId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "Movement deleted."),
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
  def xxxdeleteMovement(): Unit = {}
  val deleteMovement: Route = delete {
    logRequest("movement.delete", DebugLevel) {
      logResult("movement.delete", DebugLevel) {
        path("""[a-zA-Z0-9]+""".r) { sid =>
          val id = Movement.id(sid)
          resourceDelete(store.select(id).delete())
        }
      }
    }

  }
}
