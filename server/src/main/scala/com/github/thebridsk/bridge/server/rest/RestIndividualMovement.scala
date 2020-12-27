package com.github.thebridsk.bridge.server.rest

import com.github.thebridsk.bridge.server.backend.BridgeService
import akka.event.Logging
import akka.event.Logging._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import com.github.thebridsk.bridge.server.util.HasActorSystem
import javax.ws.rs.Path
import com.github.thebridsk.bridge.data.RestMessage
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
import com.github.thebridsk.bridge.data.IndividualMovement

/**
  * Rest API implementation for the individual movement resource.
  * <p>
  * The REST API and all the methods are documented using
  * swagger annotations.
  */
@Path("/rest/individualmovements")
@Tags(Array(new Tag(name = "IndividualDuplicate")))
trait RestIndividualMovement extends HasActorSystem {

  lazy val testlog: LoggingAdapter = Logging(actorSystem, classOf[RestIndividualMovement])

  val resName = "individualmovements"

  import UtilsPlayJson._

  /**
    * The bridge service backend
    */
  implicit val restService: BridgeService

  lazy val store = restService.individualMovements

  /**
    * spray route for all the methods on this resource
    */
  val route: Route = pathPrefix(resName) {
    logRequest("individualmovements", DebugLevel) {
      logResult("individualmovements", DebugLevel) {
        getMovement ~ getMovements ~ postMovement ~ putMovement ~ deleteMovement
      }
    }
  }

  @GET
  @Operation(
    summary = "Get all individual movements",
    description = "Returns a list of movements.",
    operationId = "getIndividualMovements",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "A list of individual movements, as a JSON array",
        content = Array(
          new Content(
            mediaType = "application/json",
            array = new ArraySchema(
              minItems = 0,
              uniqueItems = true,
              schema = new Schema(implementation = classOf[IndividualMovement])
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
    summary = "Get the individual movement by ID",
    operationId = "getMovementById",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the individual movement to get",
        in = ParameterIn.PATH,
        name = "movementId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The individual movement, as a JSON object",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[IndividualMovement])
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
        val id = IndividualMovement.id(sid)
        resource(store.select(id).read())
      }
    }
  }

  import scala.language.implicitConversions
  implicit def addIdToFuture(
      f: Future[Result[IndividualMovement]]
  ): Future[Result[(String, IndividualMovement)]] =
    f.map { r =>
      r match {
        case Right(md) => Right((md.id.id, md))
        case Left(e)   => Left(e)
      }
    }

  @POST
  @Operation(
    summary = "Create a individual movement",
    operationId = "createMovement",
    requestBody = new RequestBody(
      description = "individual movement to create",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[IndividualMovement])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description = "The created individual movement's JSON",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[IndividualMovement])
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
      entity(as[IndividualMovement]) { movement =>
        resourceCreated(resName, store.createChild(movement), Created)
      }
    }
  }
  @Path("/{movementId}")
  @PUT
  @Operation(
    summary = "Update a individual movement",
    operationId = "updateMovement",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the individual movement to update",
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
          schema = new Schema(implementation = classOf[IndividualMovement])
        )
      )
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "individual movement updated"),
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
        entity(as[IndividualMovement]) { movement =>
          val id = IndividualMovement.id(sid)
          resourceUpdated(store.select(id).update(movement.copy(name = id)))
        }
      }
    }
  @Path("/{movementId}")
  @DELETE
  @Operation(
    summary = "Delete a individual movement by ID",
    operationId = "deleteMovementById",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the individual movement to delete",
        in = ParameterIn.PATH,
        name = "movementId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "Individual movement deleted."),
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
    logRequest("individualmovement.delete", DebugLevel) {
      logResult("individualmovement.delete", DebugLevel) {
        path("""[a-zA-Z0-9]+""".r) { sid =>
          val id = IndividualMovement.id(sid)
          resourceDelete(store.select(id).delete())
        }
      }
    }

  }
}
