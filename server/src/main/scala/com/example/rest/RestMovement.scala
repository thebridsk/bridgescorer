package com.example.rest

import com.example.backend.BridgeService
import com.example.data.Board
import akka.event.Logging
import akka.event.Logging._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import com.example.util.HasActorSystem
import akka.http.scaladsl.model.StatusCode
import javax.ws.rs.Path
import com.example.data.RestMessage
import com.example.data.Movement
import akka.http.scaladsl.model.headers.Location
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

/**
 * Rest API implementation for the board resource.
 * <p>
 * The REST API and all the methods are documented using
 * swagger annotations.
 */
@Path( "/rest/movements" )
@Tags( Array( new Tag(name="Duplicate")))
trait RestMovement extends HasActorSystem {

  lazy val testlog = Logging(actorSystem, classOf[RestMovement])

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
  val route =pathPrefix(resName) {
    logRequest("movements", DebugLevel) {
      logResult("movements", DebugLevel) {
        getBoard ~ getBoards ~ postBoard ~ putBoard ~ deleteBoard
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
                          schema = new Schema( implementation=classOf[Movement] )
                      )
                  )
              )
          )
      )
  )
  def xxxgetBoards() = {}
  val getBoards = pathEnd {
    get {
      resourceMap( store.readAll())
    }
  }

  @Path("/{movementId}")
  @GET
  @Operation(
      summary = "Get the movement by ID",
      operationId = "getMovementById",
      parameters = Array(
          new Parameter(
              allowEmptyValue=false,
              description="ID of the movement to get",
              in=ParameterIn.PATH,
              name="movementId",
              required=true,
              schema=new Schema(`type`="string")
          )
      ),
      responses = Array(
          new ApiResponse(
              responseCode = "200",
              description = "The movement, as a JSON object",
              content = Array(
                  new Content(
                      mediaType = "application/json",
                      schema = new Schema( implementation=classOf[Movement] )
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
  def xxxgetBoard() = {}
  val getBoard = logRequest("getMovement", DebugLevel) { get {
    path( """[a-zA-Z0-9]+""".r ) { id =>
      resource( store.select(id).read() )
    }
  }}

  @POST
  @Operation(
      summary = "Create a movement",
      operationId = "createMovement",
      requestBody = new RequestBody(
          description = "movement to create",
          content = Array(
              new Content(
                  mediaType = "application/json",
                  schema = new Schema(
                      implementation = classOf[Movement]
                  )
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
                      schema = new Schema( implementation=classOf[Movement] )
                  )
              ),
              headers = Array(
                  new Header(
                      name="Location",
                      description="The URL of the newly created resource",
                      schema = new Schema( implementation=classOf[String] )
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
  def xxxpostBoard() = {}
  val postBoard = pathEnd {
    post {
        entity(as[Movement]) { board =>
          resourceCreated( resName, store.createChild(board), Created )
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
              allowEmptyValue=false,
              description="ID of the movement to update",
              in=ParameterIn.PATH,
              name="movementId",
              required=true,
              schema=new Schema(`type`="string")
          )
      ),
      requestBody = new RequestBody(
          description = "The updated duplicate Match",
          content = Array(
              new Content(
                  mediaType = "application/json",
                  schema = new Schema(
                      implementation = classOf[Movement]
                  )
              )
          )
      ),
      responses = Array(
          new ApiResponse(
              responseCode = "204",
              description = "Movement updated",
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
  def xxxputBoard() = {}
  val putBoard =
    put {
      path( """[a-zA-Z0-9]+""".r ) { id =>
        entity(as[Movement]) { board =>
          resourceUpdated( store.select(id).update(board.copy(name=id)) )
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
              allowEmptyValue=false,
              description="ID of the movement to delete",
              in=ParameterIn.PATH,
              name="movementId",
              required=true,
              schema=new Schema(`type`="string")
          )
      ),
      responses = Array(
          new ApiResponse(
              responseCode = "204",
              description = "Movement deleted.",
          )
      )
  )
  def xxxdeleteBoard() = {}
  val deleteBoard = delete {
    logRequest("movement.delete", DebugLevel) {
      logResult("movement.delete", DebugLevel) {
        path( """[a-zA-Z0-9]+""".r ) {
          id => {
            resourceDelete( store.select(id).delete() )
          }
        }
      }
    }

  }
}
