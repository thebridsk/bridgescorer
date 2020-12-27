package com.github.thebridsk.bridge.server.rest

import com.github.thebridsk.bridge.data.IndividualBoard
import akka.event.Logging._
import akka.http.scaladsl.server.Directives._
import javax.ws.rs.Path
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
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.DELETE
import akka.http.scaladsl.server.Route

/**
  * Rest API implementation for the board resource.
  * <p>
  * The REST API and all the methods are documented using
  * swagger annotations.
  */
@Path("/rest/individualduplicates/{dupId}/boards")
@Tags(Array(new Tag(name = "IndividualDuplicate")))
class RestNestedIndividualBoard {

  import UtilsPlayJson._

  val nestedHands = new RestNestedIndividualHand

  /**
    * spray route for all the methods on this resource
    */
  @Hidden
  def route(implicit
      @Parameter(hidden = true) res: Resources[
        IndividualBoard.Id,
        IndividualBoard
      ]
  ): Route =
    pathPrefix("boards") {
//    logRequest("route", DebugLevel) {
      getBoard ~ getBoards ~ postBoard ~ putBoard ~ deleteBoard ~ restNestedHands
//      }
    }

  @GET
  @Operation(
    summary = "Get all boards",
    description = "Returns a list of boards.",
    operationId = "getBoards",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description =
          "ID of the duplicate that contains the boards to manipulate",
        in = ParameterIn.PATH,
        name = "dupId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "A list of boards, as a JSON array",
        content = Array(
          new Content(
            mediaType = "application/json",
            array = new ArraySchema(
              minItems = 0,
              uniqueItems = true,
              schema = new Schema(implementation = classOf[IndividualBoard])
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
  def xxxgetBoards: Unit = {}
  def getBoards(implicit
      @Parameter(hidden = true) res: Resources[
        IndividualBoard.Id,
        IndividualBoard
      ]
  ): Route =
    pathEndOrSingleSlash {
      get {
        resourceMap(res.readAll())
      }
    }

  @Path("/{boardId}")
  @GET
  @Operation(
    summary = "Get the board by ID",
    operationId = "getBoardById",
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
        description = "ID of the board to get",
        in = ParameterIn.PATH,
        name = "boardId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The board, as a JSON object",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[IndividualBoard])
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
  def xxxgetBoard: Unit = {}
  def getBoard(implicit
      @Parameter(hidden = true) res: Resources[
        IndividualBoard.Id,
        IndividualBoard
      ]
  ): Route =
    logRequest("getBoard", DebugLevel) {
      get {
        path("""[a-zA-Z0-9]+""".r) { id =>
          resource(res.select(IndividualBoard.id(id)).read())
        }
      }
    }

  def restNestedHands(implicit
      @Parameter(hidden = true) res: Resources[
        IndividualBoard.Id,
        IndividualBoard
      ]
  ): Route =
    logRequestResult("RestNestedBoard.restNestedHand", DebugLevel) {
      pathPrefix("""[a-zA-Z0-9]+""".r) { id =>
        import BridgeNestedResources._
        nestedHands.route(res.select(IndividualBoard.id(id)).resourceHands)
      }
    }

  @POST
  @Operation(
    summary = "Create a board",
    operationId = "createBoard",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description =
          "ID of the duplicate that contains the boards to manipulate",
        in = ParameterIn.PATH,
        name = "dupId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    requestBody = new RequestBody(
      description = "duplicate board to create",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[IndividualBoard])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description = "The created board's JSON",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[IndividualBoard])
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
  def xxxpostBoard: Unit = {}
  def postBoard(implicit
      @Parameter(hidden = true) res: Resources[
        IndividualBoard.Id,
        IndividualBoard
      ]
  ): Route =
    pathEnd {
      post {
        entity(as[IndividualBoard]) { board =>
          resourceCreated(
            res.resourceURI,
            addIdToFuture(res.createChild(board))
          )
        }
      }
    }

  def addIdToFuture(f: Future[Result[IndividualBoard]]): Future[Result[(String, IndividualBoard)]] =
    f.map { r =>
      r match {
        case Right(md) => Right((md.id.id, md))
        case Left(e)   => Left(e)
      }
    }

  @Path("/{boardId}")
  @PUT
  @Operation(
    summary = "Update a board",
    operationId = "updateBoard",
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
        description = "ID of the board to update",
        in = ParameterIn.PATH,
        name = "boardId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    requestBody = new RequestBody(
      description = "duplicate board to update",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[IndividualBoard])
        )
      )
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "Board updated"),
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
  def xxxputBoard: Unit = {}
  def putBoard(implicit
      @Parameter(hidden = true) res: Resources[
        IndividualBoard.Id,
        IndividualBoard
      ]
  ): Route =
    put {
      path("""[a-zA-Z0-9]+""".r) { id =>
        entity(as[IndividualBoard]) { board =>
          resourceUpdated(res.select(IndividualBoard.id(id)).update(board))
        }
      }
    }
  @Path("/{boardId}")
  @DELETE
  @Operation(
    summary = "Delete a board by ID",
    operationId = "deleteBoardById",
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
        description = "ID of the board to delete",
        in = ParameterIn.PATH,
        name = "boardId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "Board deleted."),
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
  def xxxdeleteBoard: Unit = {}
  def deleteBoard(implicit
      @Parameter(hidden = true) res: Resources[
        IndividualBoard.Id,
        IndividualBoard
      ]
  ): Route =
    delete {
      path("""[a-zA-Z0-9]+""".r) { id =>
        resourceDelete(res.select(IndividualBoard.id(id)).delete())
      }
    }
}
