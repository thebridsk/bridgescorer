package com.example.rest

import com.example.backend.BridgeService
import com.example.data.Board
import akka.event.Logging
import akka.event.Logging._
import io.swagger.annotations._
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

/**
 * Rest API implementation for the board resource.
 * <p>
 * The REST API and all the methods are documented using
 * swagger annotations.
 */
@Path( "/rest/movements" )
@Api(tags= Array("Duplicate"), description = "Operations about movements.", produces="application/json", protocols="http, https")
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
  def route =pathPrefix(resName) {
    logRequest("movements", DebugLevel) {
      logResult("movements", DebugLevel) {
        getBoard ~ getBoards ~ postBoard ~ putBoard ~ deleteBoard
      }
    }
  }

  @ApiOperation(value = "Get all movements", notes = "Returns a list of movements.", response=classOf[Movement], responseContainer="List", nickname = "getMovements", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "A list of movements, as a JSON array", response=classOf[Movement], responseContainer="List")
  ))
  def getBoards = pathEnd {
    get {
      resourceMap( store.readAll())
    }
  }

  @Path("/{movementId}")
  @ApiOperation(value = "Get the movement by ID", notes = "", response=classOf[Movement], nickname = "getMovementById", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "movementId", value = "ID of the movement to get", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "The movement, as a JSON object", response=classOf[Movement]),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage])
  ))
  def getBoard = logRequest("getMovement", DebugLevel) { get {
    path( """[a-zA-Z0-9]+""".r ) { id =>
      resource( store.select(id).read() )
    }
  }}


  @ApiOperation(value = "Create a movement", notes = "", response=classOf[Movement], nickname = "createMovement", httpMethod = "POST", code=201)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "board to create", required = true,
        dataTypeClass = classOf[Movement], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 201, message = "The created board's JSON", response=classOf[Movement],
        responseHeaders= Array(
            new ResponseHeader( name="Location", description="The URL of the newly created resource", response=classOf[String] )
            )
        ),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[RestMessage])
  ))
  def postBoard = pathEnd {
    post {
        entity(as[Movement]) { board =>
          resourceCreated( resName, store.createChild(board), Created )
        }
    }
  }


  @Path("/{movementId}")
  @ApiOperation(value = "Update a movement", notes = "", response=classOf[Movement], nickname = "updateMovement", httpMethod = "PUT", code=204)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "movementId", value = "ID of the movement to update", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "body", value = "board to update", required = true,
        dataTypeClass = classOf[Movement], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "Movement updated" ),
    new ApiResponse(code = 404, message = "Does not exist", response=classOf[RestMessage]),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[RestMessage])
  ))
  def putBoard =
    put {
      path( """[a-zA-Z0-9]+""".r ) { id =>
        entity(as[Movement]) { board =>
          resourceUpdated( store.select(id).update(board.copy(name=id)) )
        }
      }
    }


  @Path("/{movementId}")
  @ApiOperation(value = "Delete a movement by ID", response=classOf[RestMessage], notes = "", nickname = "deleteMovementById", httpMethod = "DELETE", code=204)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "movementId", value = "ID of the movement to delete", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "Movement deleted." )
  ))
  def deleteBoard = delete {
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
