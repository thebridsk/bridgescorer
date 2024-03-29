package com.github.thebridsk.bridge.server.rest

import akka.event.Logging._
import akka.http.scaladsl.server.Directives._
import jakarta.ws.rs.Path
import com.github.thebridsk.bridge.data.RestMessage
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.server.backend.resource.Resources
import com.github.thebridsk.bridge.server.backend.BridgeNestedResources
import scala.concurrent.Future
import com.github.thebridsk.bridge.server.backend.resource.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.DELETE
import com.github.thebridsk.bridge.data.Round
import akka.http.scaladsl.server.Route

/**
  * Rest API implementation for the round resource.
  * <p>
  * The REST API and all the methods are documented using
  * swagger annotations.
  */
@Path("/rest/chicagos/{chiId}/rounds")
@Tags(Array(new Tag(name = "Chicago")))
class RestNestedChicagoRound {

  import UtilsPlayJson._

  val nestedHands = new RestNestedChicagoRoundHand

  /**
    * spray route for all the methods on this resource
    */
  @Hidden
  def route(implicit
      @Parameter(hidden = true) res: Resources[String, Round]
  ): Route =
    pathPrefix("rounds") {
      logRequest("route", DebugLevel) {
        getRound ~ getRounds ~ postRound ~ putRound ~ deleteRound ~ restNestedHands
      }
    }

  @GET
  @Operation(
    summary = "Get all rounds",
    description = "Returns a list of rounds.",
    operationId = "getRounds",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the chicago match that contains the rounds to get",
        in = ParameterIn.PATH,
        name = "chiId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "A list of rounds, as a JSON array",
        content = Array(
          new Content(
            mediaType = "application/json",
            array = new ArraySchema(
              minItems = 0,
              uniqueItems = true,
              schema = new Schema(implementation = classOf[Round])
            )
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
  def xxxgetRounds: Unit = {}
  def getRounds(implicit
      @Parameter(hidden = true) res: Resources[String, Round]
  ): Route =
    pathEndOrSingleSlash {
      get {
        resourceMap(res.readAll())
      }
    }

  @Path("/{roundId}")
  @GET
  @Operation(
    summary = "Get the round by ID",
    operationId = "getRoundById",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description =
          "ID of the chicago match that contains the rounds to manipulate",
        in = ParameterIn.PATH,
        name = "chiId",
        required = true,
        schema = new Schema(`type` = "string")
      ),
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the round to get",
        in = ParameterIn.PATH,
        name = "roundId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The round, as a JSON object",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[Round])
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
  def xxxgetRound: Unit = {}
  def getRound(implicit
      @Parameter(hidden = true) res: Resources[String, Round]
  ): Route =
    logRequest("getRound", DebugLevel) {
      get {
        path("""[a-zA-Z0-9]+""".r) { id =>
          resource(res.select(id).read())
        }
      }
    }

  def restNestedHands(implicit
      @Parameter(hidden = true) res: Resources[String, Round]
  ): Route =
    logRequestResult("RestNestedRound.restNestedHand", DebugLevel) {
      pathPrefix("""[a-zA-Z0-9]+""".r) { id =>
        import BridgeNestedResources._
        nestedHands.route(res.select(id).resourceHands)
      }
    }

  @POST
  @Operation(
    summary = "Create a round",
    operationId = "createRound",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description =
          "ID of the chicago match that contains the rounds to manipulate",
        in = ParameterIn.PATH,
        name = "chiId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    requestBody = new RequestBody(
      description = "chicago round to create",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[Round])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description = "The created round's JSON",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[Round])
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
  def xxxpostRound: Unit = {}
  def postRound(implicit
      @Parameter(hidden = true) res: Resources[String, Round]
  ): Route =
    pathEnd {
      post {
        entity(as[Round]) { round =>
          resourceCreated(
            res.resourceURI,
            addIdToFuture(res.createChild(round))
          )
        }
      }
    }

  def addIdToFuture(f: Future[Result[Round]]): Future[Result[(String, Round)]] =
    f.map { r =>
      r match {
        case Right(md) => Right((md.id, md))
        case Left(e)   => Left(e)
      }
    }

  @Path("/{roundId}")
  @PUT
  @Operation(
    summary = "Update a round",
    operationId = "updateRound",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description =
          "ID of the chicago match that contains the rounds to manipulate",
        in = ParameterIn.PATH,
        name = "chiId",
        required = true,
        schema = new Schema(`type` = "string")
      ),
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the round to update",
        in = ParameterIn.PATH,
        name = "roundId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    requestBody = new RequestBody(
      description = "chicago round to update",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[Round])
        )
      )
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "Round updated"),
      new ApiResponse(
        responseCode = "404",
        description = "Does not exist.",
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
  def xxxputRound: Unit = {}
  def putRound(implicit
      @Parameter(hidden = true) res: Resources[String, Round]
  ): Route =
    put {
      path("""[a-zA-Z0-9]+""".r) { id =>
        entity(as[Round]) { round =>
          resourceUpdated(res.select(id).update(round))
        }
      }
    }
  @Path("/{roundId}")
  @DELETE
  @Operation(
    summary = "Delete a round by ID",
    operationId = "deleteRoundById",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description =
          "ID of the chicago match that contains the rounds to manipulate",
        in = ParameterIn.PATH,
        name = "chiId",
        required = true,
        schema = new Schema(`type` = "string")
      ),
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the round to delete",
        in = ParameterIn.PATH,
        name = "roundId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "Round deleted."),
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
  def xxxdeleteRound: Unit = {}
  def deleteRound(implicit
      @Parameter(hidden = true) res: Resources[String, Round]
  ): Route =
    delete {
      path("""[a-zA-Z0-9]+""".r) { id =>
        resourceDelete(res.select(id).delete())
      }
    }
}
