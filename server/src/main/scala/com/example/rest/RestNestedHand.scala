package com.example.rest

import com.example.data.Board
import com.example.data.DuplicateHand
import com.example.data.MatchDuplicate
import com.example.data.Id
import akka.event.Logging
import akka.event.Logging._
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import com.example.util.HasActorSystem
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import javax.ws.rs.Path
import com.example.data.RestMessage
import akka.http.scaladsl.model.headers.Location
import scala.concurrent.ExecutionContext.Implicits.global
import com.example.backend.resource.Resources
import scala.concurrent.Future
import com.example.backend.resource.Result
import utils.logging.Logger
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
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.DELETE

object RestNestedHand {
  val log = Logger[RestNestedHand]
}

import RestNestedHand._

/**
  * Rest API implementation for the hand resource.
  * <p>
  * The REST API and all the methods are documented using
  * swagger annotations.
  */
@Path("/rest/duplicates/{dupId}/boards/{boardId}/hands")
@Tags(Array(new Tag(name = "Duplicate")))
class RestNestedHand {

  import UtilsPlayJson._

  /**
    * spray route for all the methods on this resource
    */
  @Hidden
  def route(
      implicit @Parameter(hidden = true) res: Resources[Id.Team, DuplicateHand]
  ) = pathPrefix("hands") {
    logRequestResult("route", DebugLevel) {
      getHand ~ getHands ~ postHand ~ putHand ~ deleteHand
    }
  }

  @GET
  @Operation(
    summary = "Get all hands",
    description = "Returns a list of hands.",
    operationId = "getHands",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description =
          "ID of the duplicate that contains the boards to manipulate",
        in = ParameterIn.PATH,
        name = "dupId",
        required = true,
        schema = new Schema(`type` = "string")
      ),
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the board that contains the hands to manipulate",
        in = ParameterIn.PATH,
        name = "boardId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "A list of hands, as a JSON array",
        content = Array(
          new Content(
            mediaType = "application/json",
            array = new ArraySchema(
              minItems = 0,
              uniqueItems = true,
              schema = new Schema(
                implementation = classOf[DuplicateHand],
                description = "A hand from the board."
              ),
              arraySchema =
                new Schema(description = "All the hands from the board.")
            )
          )
        )
      )
    )
  )
  def xxxgetHands = {}
  def getHands(
      implicit @Parameter(hidden = true) res: Resources[
        Id.DuplicateHand,
        DuplicateHand
      ]
  ) = pathEndOrSingleSlash {
    get {
      resourceMap(res.readAll())
    }
  }

  @Path("/{handId}")
  @GET
  @Operation(
    summary = "Get the hand by ID",
    operationId = "getHandById",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description =
          "ID of the duplicate that contains the boards to manipulate",
        in = ParameterIn.PATH,
        name = "dupId",
        required = true,
        schema = new Schema(`type` = "string")
      ),
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the board that contains the hands to manipulate",
        in = ParameterIn.PATH,
        name = "boardId",
        required = true,
        schema = new Schema(`type` = "string")
      ),
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the hand to get",
        in = ParameterIn.PATH,
        name = "handId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The hand, as a JSON object",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[DuplicateHand])
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
  def xxxgetHand = {}
  def getHand(
      implicit @Parameter(hidden = true) res: Resources[String, DuplicateHand]
  ) = logRequest("getHand", DebugLevel) {
    get {
      path("""[a-zA-Z0-9]+""".r) { id =>
        resource(res.select(id).read())
      }
    }
  }

  @POST
  @Operation(
    summary = "Create a hand",
    operationId = "createHand",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description =
          "ID of the duplicate that contains the boards to manipulate",
        in = ParameterIn.PATH,
        name = "dupId",
        required = true,
        schema = new Schema(`type` = "string")
      ),
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the board that contains the hands to manipulate",
        in = ParameterIn.PATH,
        name = "boardId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    requestBody = new RequestBody(
      description = "hand to create",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[DuplicateHand])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description = "The created hand's JSON",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[DuplicateHand])
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
  def xxxpostHand = {}
  def postHand(
      implicit @Parameter(hidden = true) res: Resources[String, DuplicateHand]
  ) = pathEnd {
    post {
      entity(as[DuplicateHand]) { hand =>
        log.fine(s"Creating new hand ${hand} in ${res.resourceURI}")
        resourceCreated(res.resourceURI, addIdToFuture(res.createChild(hand)))
      }
    }
  }

  def addIdToFuture(
      f: Future[Result[DuplicateHand]]
  ): Future[Result[(String, DuplicateHand)]] =
    f.map { r =>
      r match {
        case Right(md) => Right((md.id.toString(), md))
        case Left(e)   => Left(e)
      }
    }

  @Path("/{handId}")
  @PUT
  @Operation(
    summary = "Update a hand",
    operationId = "updateHand",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description =
          "ID of the duplicate that contains the boards to manipulate",
        in = ParameterIn.PATH,
        name = "dupId",
        required = true,
        schema = new Schema(`type` = "string")
      ),
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the board that contains the hands to manipulate",
        in = ParameterIn.PATH,
        name = "boardId",
        required = true,
        schema = new Schema(`type` = "string")
      ),
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the hand to update",
        in = ParameterIn.PATH,
        name = "handId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    requestBody = new RequestBody(
      description = "hand to update",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[DuplicateHand])
        )
      )
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "Hand updated"),
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
  def xxxputHand = {}
  def putHand(
      implicit @Parameter(hidden = true) res: Resources[String, DuplicateHand]
  ) = logRequest("putHand", DebugLevel) {
    logResult("putHand", DebugLevel) {
      put {
        path("""[a-zA-Z0-9]+""".r) { id =>
          entity(as[DuplicateHand]) { hand =>
            resourceUpdated(res.select(id).update(hand))
          }
        }
      }
    }
  }

  @Path("/{handId}")
  @DELETE
  @Operation(
    summary = "Delete a hand by ID",
    operationId = "deleteHandById",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description =
          "ID of the duplicate that contains the boards to manipulate",
        in = ParameterIn.PATH,
        name = "dupId",
        required = true,
        schema = new Schema(`type` = "string")
      ),
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the board that contains the hands to manipulate",
        in = ParameterIn.PATH,
        name = "boardId",
        required = true,
        schema = new Schema(`type` = "string")
      ),
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the hand to delete",
        in = ParameterIn.PATH,
        name = "handId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "Hand deleted.")
    )
  )
  def xxxdeleteHand = {}
  def deleteHand(
      implicit @Parameter(hidden = true) res: Resources[String, DuplicateHand]
  ) = delete {
    path("""[a-zA-Z0-9]+""".r) { id =>
      resourceDelete(res.select(id).delete())
    }
  }
}
