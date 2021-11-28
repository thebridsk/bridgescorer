package com.github.thebridsk.bridge.server.rest

import com.github.thebridsk.bridge.server.backend.BridgeService
import com.github.thebridsk.bridge.data.IndividualDuplicate
import akka.event.Logging
import akka.event.Logging._
import akka.http.scaladsl.server.Directives._
import com.github.thebridsk.bridge.server.util.HasActorSystem
import jakarta.ws.rs.Path
import com.github.thebridsk.bridge.data.RestMessage
import scala.util.Success
import scala.util.Failure
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
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.DELETE
import com.github.thebridsk.bridge.data.BoardSet
import scala.concurrent.Future
import com.github.thebridsk.bridge.server.backend.resource.Result
import akka.http.scaladsl.server.Route
import com.github.thebridsk.bridge.data.IndividualMovement

/**
  * Rest API implementation for the board resource.
  * <p>
  * The REST API and all the methods are documented using
  * swagger annotations.
  */
@Path("/rest/individualduplicates")
@Tags(Array(new Tag(name = "IndividualDuplicate")))
trait RestIndividualDuplicate extends HasActorSystem {

  private lazy val log = Logging(actorSystem, classOf[RestIndividualDuplicate])

  val restService: BridgeService

  lazy val store = restService.individualduplicates

  val resName = "individualduplicates"

  import UtilsPlayJson._

  val nestedBoards = new RestNestedIndividualBoard
  lazy val nestedPictures = new RestNestedPictureIndividual(store, this)

  /**
    * spray route for all the methods on this resource
    */
  val route: Route = pathPrefix(resName) {
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
              schema = new Schema(implementation = classOf[IndividualDuplicate])
            )
          )
        )
      )
    )
  )
  def xxxgetDuplicates(): Unit = {}
  val getDuplicates: Route = pathEnd {
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
            schema = new Schema(implementation = classOf[IndividualDuplicate])
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
  def xxxgetDuplicate(): Unit = {}
  val getDuplicate: Route =
    logRequest("RestIndividualDuplicate.getDuplicate", DebugLevel) {
      logResult("RestIndividualDuplicate.getDuplicate") {
        get {
          pathPrefix("""[a-zA-Z0-9]+""".r) { sid =>
            val id = IndividualDuplicate.id(sid)
            pathEndOrSingleSlash {
              resource(store.select(id).read())
            }
          }
        }
      }
    }

  val nested: Route = logRequest("RestIndividualDuplicate.nested", DebugLevel) {
    logResult("RestIndividualDuplicate.nested") {
      pathPrefix("""[a-zA-Z0-9]+""".r) { sid =>
        val id = IndividualDuplicate.id(sid)
        val selected = store.select(id)
        nestedBoards.route(selected.resourceBoards) ~
          nestedPictures.route(id)
      }
    }
  }

  import scala.language.implicitConversions
  implicit def addIdToFuture(
      f: Future[Result[IndividualDuplicate]]
  ): Future[Result[(String, IndividualDuplicate)]] =
    f.map { r =>
      r match {
        case Right(md) => Right((md.id.id, md))
        case Left(e)   => Left(e)
      }
    }

  @POST
  @Operation(
    summary = "Create a duplicate match",
    operationId = "createDuplicate",
    parameters = Array(
      new Parameter(
        name = "default",
        in = ParameterIn.QUERY,
        allowEmptyValue = true,
        description =
          "If present, indicates boards and hands should be added.  Default movements is 2Tables, default boards is ArmonkBoards, value is ignored.",
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
          schema = new Schema(implementation = classOf[IndividualDuplicate])
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
            schema = new Schema(implementation = classOf[IndividualDuplicate])
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
  def xxxpostDuplicate(): Unit = {}
  val postDuplicate: Route =
    logRequest("RestIndividualDuplicate.postDuplicate") {
      logResult("RestIndividualDuplicate.postDuplicate") {
        pathEnd {
          post {
            parameter("default".?, "boards".?, "movements".?) {
              (default, boards, movements) =>
                entity(as[IndividualDuplicate]) { dup =>
//                log.warning("Creating duplicate match from "+dup)
                  RestIndividualDuplicate.createIndividualDuplicate(
                    restService,
                    dup,
                    default,
                    boards.map(BoardSet.id(_)),
                    movements.map(IndividualMovement.id(_))
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
                          throw ex
                          // ex match {
                          //   case x: IllegalArgumentException =>
                          //     complete(
                          //       StatusCodes.BadRequest,
                          //       s"Bad request: ${x.getMessage()}"
                          //     )
                          //   case _ =>
                          //     complete(
                          //       StatusCodes.InternalServerError,
                          //       "Internal server error"
                          //     )
                          // }
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
          schema = new Schema(implementation = classOf[IndividualDuplicate])
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
  def xxxputDuplicate(): Unit = {}
  val putDuplicate: Route =
    logRequest("RestIndividualDuplicate.putDuplicate") {
      logResult("RestIndividualDuplicate.putDuplicate") {
        put {
          path("""[a-zA-Z0-9]+""".r) { sid =>
            pathEnd {
              val id = IndividualDuplicate.id(sid)
              entity(as[IndividualDuplicate]) { dup =>
                resourceUpdated(store.select(id).update(dup))
              }
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
      new ApiResponse(responseCode = "204", description = "duplicate deleted."),
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
  def xxxdeleteDuplicate(): Unit = {}
  val deleteDuplicate: Route = delete {
    path("""[a-zA-Z0-9]+""".r) { sid =>
      pathEnd {
        val id = IndividualDuplicate.id(sid)
        resourceDelete(store.select(id).delete())
      }
    }
  }

}

object RestIndividualDuplicate {
  def createIndividualDuplicate(
      restService: BridgeService,
      dup: IndividualDuplicate,
      default: Option[String],
      boards: Option[BoardSet.Id],
      movements: Option[IndividualMovement.Id]
  ): Option[Future[Result[IndividualDuplicate]]] = {
    if (default.isDefined || boards.isDefined || movements.isDefined) {
      val b = boards.getOrElse(restService.defaultIndividualBoards)
      val h = movements.getOrElse(restService.defaultIndividualMovement)
      Some(restService.fillBoards(dup, b, h))
    } else {
      None
    }
  }
}
