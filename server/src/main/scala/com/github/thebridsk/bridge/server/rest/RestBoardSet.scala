package com.github.thebridsk.bridge.server.rest

import com.github.thebridsk.bridge.server.backend.BridgeService
import akka.event.Logging
import akka.event.Logging._
import akka.http.scaladsl.server.Directives._
import com.github.thebridsk.bridge.server.util.HasActorSystem
import javax.ws.rs.Path
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.server.backend.resource.Result
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.headers.Header
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.DELETE
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.annotations.tags.Tag
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Route

/**
  * Rest API implementation for the board resource.
  * <p>
  * The REST API and all the methods are documented using
  * swagger annotations.
  */
@Path("/rest/boardsets")
@Tags(Array(new Tag(name = "Duplicate")))
trait RestBoardSet extends HasActorSystem {

  lazy val testlog: LoggingAdapter = Logging(actorSystem, classOf[RestBoardSet])

  val resName = "boardsets"

  import UtilsPlayJson._

  /**
    * The bridge service backend
    */
  implicit val restService: BridgeService

  lazy val store = restService.boardSets

  /**
    * spray route for all the methods on this resource
    */
  val route: Route = pathPrefix(resName) {
    logRequest("boardsets", DebugLevel) {
      logResult("boardsets", DebugLevel) {
        getBoard ~ getBoards ~ postBoard ~ putBoard ~ deleteBoard
      }
    }
  }

  @GET
  @Operation(
    summary = "Get all boardsets",
    description = "Returns a list of boardsets.",
    operationId = "getBoardsets",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "A list of boardsets, as a JSON array",
        content = Array(
          new Content(
            mediaType = "application/json",
            array = new ArraySchema(
              minItems = 0,
              uniqueItems = true,
              schema = new Schema(implementation = classOf[BoardSet])
            )
          )
        )
      )
    )
  )
  def xxxgetBoards(): Unit = {}
  val getBoards: Route = pathEnd {
    get {
      resourceMap(store.readAll())
    }
  }

  @Path("/{boardsetId}")
  @GET
  @Operation(
    summary = "Get the boardset by ID",
    description = "Returns the specified boardset.",
    operationId = "getBoardsetById",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the boardset to get",
        in = ParameterIn.PATH,
        name = "boardsetId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "A boardset, as a JSON object",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[BoardSet])
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
  def xxxgetBoard(): Unit = {}
  val getBoard: Route = logRequest("getBoardset", DebugLevel) {
    get {
      path("""[a-zA-Z0-9]+""".r) { id =>
        resource(store.select(BoardSet.id(id)).read())
      }
    }
  }

  import scala.language.implicitConversions
  implicit def addIdToFuture(
      f: Future[Result[BoardSet]]
  ): Future[Result[(String, BoardSet)]] =
    f.map { r =>
      r match {
        case Right(md) => Right((md.name.id, md))
        case Left(e)   => Left(e)
      }
    }

  @POST
  @Operation(
    summary = "Create a boardset",
    operationId = "createBoardset",
    requestBody = new RequestBody(
      description = "the boardset to create",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[BoardSet])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description = "The boardset was created",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[BoardSet])
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
  def xxxpostBoard(): Unit = {}
  val postBoard: Route = pathEnd {
    post {
      entity(as[BoardSet]) { board =>
        resourceCreated(resName, store.createChild(board))
      }
    }
  }
  @Path("/{boardsetId}")
  @PUT
  @Operation(
    summary = "Update a boardset",
    description =
      "The boardset given in the body replaces the boardset with the specified boardsetId, the id field in the given boardset is set to boardsetId",
    operationId = "updateBoardset",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the boardset to update",
        in = ParameterIn.PATH,
        name = "boardsetId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    requestBody = new RequestBody(
      description = "the boardset to update",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[BoardSet])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "204",
        description = "The boardset was updated"
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
  def xxxputBoard(): Unit = {}
  val putBoard: Route = logRequest("putBoardset", DebugLevel) {
    logResult("putBoardsets", DebugLevel) {
      put {
        path("""[a-zA-Z0-9]+""".r) { sid =>
          val id = BoardSet.id(sid)
          testlog.info("putBoardset: id is " + id)
          entity(as[BoardSet]) { board =>
            testlog.info("putBoardset: board is " + board)
            resourceUpdated(store.select(id).update(board.copy(name = id)))
          }
        }
      }
    }
  }
  @Path("/{boardsetId}")
  @DELETE
  @Operation(
    summary = "Delete a boardset by ID",
    operationId = "deleteBoardsetById",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the boardset to delete",
        in = ParameterIn.PATH,
        name = "boardsetId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "204",
        description = "The boardset was deleted"
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
  def xxxdeleteBoard(): Unit = {}
  val deleteBoard: Route = delete {
    logRequest("boardsets.delete", DebugLevel) {
      logResult("boardsets.delete", DebugLevel) {
        path("""[a-zA-Z0-9]+""".r) { sid =>
          val id = BoardSet.id(sid)
          resourceDelete(store.select(id).delete())
        }
      }
    }

  }
}
