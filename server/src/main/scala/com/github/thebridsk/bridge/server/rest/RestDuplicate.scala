package com.github.thebridsk.bridge.server.rest

import com.github.thebridsk.bridge.server.backend.BridgeService
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.MatchDuplicate
import akka.event.Logging
import akka.event.Logging._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.thebridsk.bridge.server.util.HasActorSystem
import akka.http.scaladsl.model.StatusCode
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.data.DuplicateSummary
import javax.ws.rs.Path
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.data.SystemTime
import akka.http.scaladsl.model.headers.Location
import scala.util.Success
import scala.util.Failure
import akka.http.scaladsl.model.StatusCodes
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.server.backend.BridgeNestedResources._
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

/**
  * Rest API implementation for the board resource.
  * <p>
  * The REST API and all the methods are documented using
  * swagger annotations.
  */
@Path("/rest/duplicates")
@Tags(Array(new Tag(name = "Duplicate")))
trait RestDuplicate extends HasActorSystem {

  private lazy val log = Logging(actorSystem, classOf[RestDuplicate])

  val restService: BridgeService

  lazy val store = restService.duplicates

  val resName = "duplicates"

  import UtilsPlayJson._

  val nestedBoards = new RestNestedBoard
  val nestedTeams = new RestNestedTeam
  val nestedPictures = new RestNestedPicture(store,this)

  /**
    * spray route for all the methods on this resource
    */
  val route = pathPrefix(resName) {
//    logRequest("route", DebugLevel) {
    getDuplicate ~ getDuplicates ~ postDuplicate ~ putDuplicate ~ deleteDuplicate ~ nested
//      }
  }

  @GET
  @Operation(
    summary = "Get all duplicate matches",
    description = "Returns a list of matches.",
    operationId = "getDuplicates",
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
              schema = new Schema(implementation = classOf[MatchDuplicate])
            )
          )
        )
      )
    )
  )
  def xxxgetDuplicates() = {}
  val getDuplicates = pathEnd {
    get {
      resourceMap(store.readAll())
    }
  }

  @Path("/{dupId}")
  @GET
  @Operation(
    summary = "Get the match by ID",
    description = "Returns the specified match.",
    operationId = "getDuplicateById",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the match to get",
        in = ParameterIn.PATH,
        name = "dupId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "A match, as a JSON object",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[MatchDuplicate])
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
  def xxxgetDuplicate() = {}
  val getDuplicate = logRequest("RestDuplicate.getDuplicate", DebugLevel) {
    logResult("RestDuplicate.getDuplicate") {
      get {
        pathPrefix("""[a-zA-Z0-9]+""".r) { id: Id.MatchDuplicate =>
          pathEndOrSingleSlash {
            resource(store.select(id).read())
          }
        }
      }
    }
  }

  val nested = logRequest("RestDuplicate.nested", DebugLevel) {
    logResult("RestDuplicate.nested") {
      pathPrefix("""[a-zA-Z0-9]+""".r) { id: Id.MatchDuplicate =>
        val selected = store.select(id)
        nestedBoards.route(selected.resourceBoards) ~
          nestedTeams.route(selected.resourceTeams) ~
          nestedPictures.route(id)
      }
    }
  }

  @POST
  @Operation(
    summary = "Create a duplicate match",
    operationId = "createDuplicate",
    parameters = Array(
      new Parameter(
        name = "test",
        in = ParameterIn.QUERY,
        allowEmptyValue = true,
        description = "If present, create test match, value is ignored.",
        required = false,
        schema = new Schema(implementation = classOf[String])
      ),
      new Parameter(
        name = "default",
        in = ParameterIn.QUERY,
        allowEmptyValue = true,
        description =
          "If present, indicates boards and hands should be added.  Default movements is 2TablesArmonk, default boards is ArmonkBoards, value is ignored.",
        required = false,
        schema = new Schema(implementation = classOf[String])
      ),
      new Parameter(
        name = "boards",
        in = ParameterIn.QUERY,
        allowEmptyValue = false,
        description =
          "If present, indicates which boards to use, example values: StandardBoards, ArmonkBoards",
        required = false,
        schema = new Schema(implementation = classOf[String])
      ),
      new Parameter(
        name = "movements",
        in = ParameterIn.QUERY,
        allowEmptyValue = false,
        description =
          "If present, indicates which movements to use, example values: Howell3TableNoRelay, Mitchell3Table, Howell2Table5Teams, 2TablesArmonk",
        required = false,
        schema = new Schema(implementation = classOf[String])
      )
    ),
    requestBody = new RequestBody(
      description = "duplicate Match to create",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[MatchDuplicate])
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
            schema = new Schema(implementation = classOf[MatchDuplicate])
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
  def xxxpostDuplicate() = {}
  val postDuplicate =
    logRequest("RestDuplicate.postDuplicate") {
      logResult("RestDuplicate.postDuplicate") {
        pathEnd {
          post {
            parameter('test.?, 'default.?, 'boards.?, 'movements.?) {
              (test, default, boards, movements) =>
                entity(as[MatchDuplicate]) { dup =>
//                log.warning("Creating duplicate match from "+dup)
                  RestDuplicate.createMatchDuplicate(
                    restService,
                    dup,
                    test,
                    default,
                    boards,
                    movements
                  ) match {
                    case Some(fut) =>
                      onComplete(fut) {
                        case Success(r) =>
                          r match {
                            case Right(md) =>
                              resourceCreated(resName, store.createChild(md))
                            case Left((code, msg)) =>
                              complete(code, msg)
                          }
                        case Failure(ex) =>
                          RestLoggerConfig.log
                            .info("Exception posting duplicate: ", ex)
                          complete(
                            StatusCodes.InternalServerError,
                            "Internal server error"
                          )
                      }
                    case None =>
                      resourceCreated(resName, store.createChild(dup))
                  }
                }
            }
          }
        }
      }
    }
  @Path("/{dupId}")
  @PUT
  @Operation(
    summary = "Update a duplicate match",
    operationId = "updateDuplicate",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the match to update",
        in = ParameterIn.PATH,
        name = "dupId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    requestBody = new RequestBody(
      description = "The updated duplicate Match",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[MatchDuplicate])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "204",
        description = "Duplicate match updated"
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
  def xxxputDuplicate() = {}
  val putDuplicate =
    logRequest("RestDuplicate.putDuplicate") {
      logResult("RestDuplicate.putDuplicate") {
        put {
          path("""[a-zA-Z0-9]+""".r) { id: Id.MatchDuplicate =>
            entity(as[MatchDuplicate]) { dup =>
              resourceUpdated(store.select(id).update(dup))
            }
          }
        }
      }
    }
  @Path("/{dupId}")
  @DELETE
  @Operation(
    summary = "Delete a match by ID",
    operationId = "deleteDuplicateById",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the match to delete",
        in = ParameterIn.PATH,
        name = "dupId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "duplicate deleted.")
    )
  )
  def xxxdeleteDuplicate() = {}
  val deleteDuplicate = delete {
    path("""[a-zA-Z0-9]+""".r) { id: Id.MatchDuplicate =>
      {
        resourceDelete(store.select(id).delete())
      }
    }
  }
}

object RestDuplicate {
  def createMatchDuplicate(
      restService: BridgeService,
      dup: MatchDuplicate,
      test: Option[String],
      default: Option[String],
      boards: Option[String],
      movements: Option[String]
  ) = {
    test match {
      case None =>
        if (default.isDefined || boards.isDefined || movements.isDefined) {
          val b = boards.getOrElse(restService.defaultBoards)
          val h = movements.getOrElse(restService.defaultMovement)
          Some(restService.fillBoards(dup, b, h))
        } else {
          None
        }
      case Some(s) =>
        Some(restService.createTestDuplicate(dup))
    }
  }
}
