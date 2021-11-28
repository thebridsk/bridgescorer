package com.github.thebridsk.bridge.server.rest

import com.github.thebridsk.bridge.server.backend.BridgeService
import com.github.thebridsk.bridge.data.MatchRubber
import akka.event.Logging._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import com.github.thebridsk.bridge.server.util.HasActorSystem
import jakarta.ws.rs.Path
import com.github.thebridsk.bridge.data.RestMessage
import scala.util.Sorting
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
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.DELETE
import scala.concurrent.Future
import com.github.thebridsk.bridge.server.backend.resource.Result
import akka.http.scaladsl.server.Route

object RestRubber {
  implicit class OrdFoo(val x: MatchRubber)
      extends AnyVal
      with Ordered[MatchRubber] {
    def compare(that: MatchRubber): Int = that.id.compare(x.id)
  }

}

import RestRubber._
import com.github.thebridsk.bridge.server.backend.BridgeNestedResources

/**
  * Rest API implementation for the board resource.
  * <p>
  * The REST API and all the methods are documented using
  * swagger annotations.
  */
@Path("/rest/rubbers")
@Tags(Array(new Tag(name = "Rubber")))
trait RestRubber extends HasActorSystem {

  /**
    * The bridge service backend
    */
  implicit val restService: BridgeService
  lazy val store = restService.rubbers

  val resName = "rubbers"

  val nestedHands = new RestNestedRubberHand

  import UtilsPlayJson._

  def sort(a: Array[MatchRubber]): Array[MatchRubber] = {

    Sorting.quickSort(a)
    a
  }

  /**
    * spray route for all the methods on this resource
    */
  val route: Route = pathPrefix(resName) {
//    logRequest("route", DebugLevel) {
    getRubber ~ getRubbers ~ postRubber ~ putRubber ~ deleteRubber ~ restNestedHands
//      }
  }

  @GET
  @Operation(
    summary = "Get all rubber matches",
    description = "Returns a list of matches.",
    operationId = "getRubbers",
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
              schema = new Schema(implementation = classOf[MatchRubber])
            )
          )
        )
      )
    )
  )
  def xxxgetRubbers(): Unit = {}
  val getRubbers: Route = pathEnd {
    get {
      resourceMap(store.readAll())
    }
  }

  @Path("/{rubId}")
  @GET
  @Operation(
    summary = "Get the match by ID",
    description = "Returns the specified rubber match.",
    operationId = "getRubberById",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the rubber match to get",
        in = ParameterIn.PATH,
        name = "rubId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The requested Rubber match, as a JSON object",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[MatchRubber])
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
  def xxxgetRubber(): Unit = {}
  val getRubber: Route = logRequest("RestRubber.getRubber", DebugLevel) {
    logResult("RestRubber.postRubber") {
      get {
        path("""[a-zA-Z0-9]+""".r) { sid =>
          val id = MatchRubber.id(sid)
          resource(store.select(id).read())
        }
      }
    }
  }

  val restNestedHands: Route =
    logRequestResult("RestNestedRubberHand.restNestedHand", DebugLevel) {
      pathPrefix("""[a-zA-Z0-9]+""".r) { sid =>
        val id = MatchRubber.id(sid)
        import BridgeNestedResources._
        nestedHands.route(store.select(id).resourceHands)
      }
    }

  import scala.language.implicitConversions
  implicit def addIdToFuture(
      f: Future[Result[MatchRubber]]
  ): Future[Result[(String, MatchRubber)]] =
    f.map { r =>
      r match {
        case Right(md) => Right((md.id.id, md))
        case Left(e)   => Left(e)
      }
    }

  @POST
  @Operation(
    summary = "Create a rubber match",
    operationId = "createRubber",
    requestBody = new RequestBody(
      description = "Rubber Match to create",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[MatchRubber])
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
            schema = new Schema(implementation = classOf[MatchRubber])
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
  def xxxpostRubber(): Unit = {}
  val postRubber: Route =
    logRequest("RestRubber.postRubber") {
      logResult("RestRubber.postRubber") {
        pathEnd {
          post {
            entity(as[MatchRubber]) { chi =>
              resourceCreated(resName, store.createChild(chi), Created)
            }
          }
        }
      }
    }
  @Path("/{rubId}")
  @PUT
  @Operation(
    summary = "Update a rubber match",
    description =
      "Update a rubber match.  The id of the rubber match in the body is replaced with rubId",
    operationId = "updateRubber",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the match to get",
        in = ParameterIn.PATH,
        name = "rubId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    requestBody = new RequestBody(
      description = "Rubber Match to update",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[MatchRubber])
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
  @RequestBody(
    description = "Rubber Match to update",
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[MatchRubber])
      )
    )
  )
  def xxxputRubber(): Unit = {}
  val putRubber: Route =
    logRequest("RestRubber.putRubber") {
      logResult("RestRubber.putRubber") {
        path("""[a-zA-Z0-9]+""".r) { sid =>
          val id = MatchRubber.id(sid)
          put {
            entity(as[MatchRubber]) { chi =>
              resourceUpdated(store.select(id).update(chi))
            }
          }
        }
      }
    }
  @Path("/{rubId}")
  @DELETE
  @Operation(
    summary = "Delete a match by ID",
    operationId = "deleteRubberById",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the match to delete",
        in = ParameterIn.PATH,
        name = "rubId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "204",
        description = "Rubber match deleted."
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
  def xxxdeleteRubber(): Unit = {}
  val deleteRubber: Route = delete {
    path("""[a-zA-Z0-9]+""".r) { sid =>
      val id = MatchRubber.id(sid)
      resourceDelete(store.select(id).delete())
    }
  }
}
