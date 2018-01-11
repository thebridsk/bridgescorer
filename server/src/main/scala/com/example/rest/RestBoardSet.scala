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
import com.example.data.BoardSet
import akka.http.scaladsl.model.headers.Location
import com.example.backend.resource.Result
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Rest API implementation for the board resource.
 * <p>
 * The REST API and all the methods are documented using
 * swagger annotations.
 */
@Path( "/rest/boardsets" )
@Api(tags = Array("Duplicate"), description = "Operations about boardsets.", produces="application/json", protocols="http, https")
trait RestBoardSet extends HasActorSystem {

  lazy val testlog = Logging(actorSystem, classOf[RestBoardSet])

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
  def route =pathPrefix(resName) {
    logRequest("boardsets", DebugLevel) {
      logResult("boardsets", DebugLevel) {
        getBoard ~ getBoards ~ postBoard ~ putBoard ~ deleteBoard
      }
    }
  }

  @ApiOperation(value = "Get all boardsets", notes = "Returns a list of boardsets.", nickname = "getBoardsets", httpMethod = "GET", code=200, response=classOf[BoardSet], responseContainer="List")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "A list of boardsets, as a JSON array", response=classOf[BoardSet], responseContainer="List")
  ))
  def getBoards = pathEnd {
    get {
      resourceMap( store.readAll() )
    }
  }

  @Path("/{boardsetId}")
  @ApiOperation(value = "Get the boardset by ID", notes = "", nickname = "getBoardsetById", httpMethod = "GET", code=200, response=classOf[BoardSet])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "boardsetId", value = "ID of the boardset to get", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "The boardset, as a JSON object", response=classOf[BoardSet]),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage])
  ))
  def getBoard = logRequest("getBoardset", DebugLevel) { get {
    path( """[a-zA-Z0-9]+""".r ) { id =>
      resource( store.select(id).read() )
    }
  }}


  @ApiOperation(value = "Create a boardset", notes = "", nickname = "createBoardset", httpMethod = "POST", code=201, response=classOf[BoardSet])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "board to create", required = true,
        dataTypeClass = classOf[BoardSet], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 201, message = "The created board's JSON", response=classOf[BoardSet],
        responseHeaders= Array(
            new ResponseHeader( name="Location", description="The URL of the newly created resource", response=classOf[String] )
            )
        ),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[RestMessage])
  ))
  def postBoard = pathEnd {
    post {
        entity(as[BoardSet]) { board =>
          resourceCreated( resName, store.createChild(board) )
        }
    }
  }


  @Path("/{boardsetId}")
  @ApiOperation(value = "Update a boardset", notes = "", nickname = "updateBoardset", httpMethod = "PUT", code=204, response=classOf[RestMessage])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "boardsetId", value = "ID of the boardset to update", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "body", value = "board to update", required = true,
        dataTypeClass = classOf[BoardSet], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "BoardSet updated", response=classOf[Void] ),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage]),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[RestMessage])
  ))
  def putBoard = logRequest("putBoardset", DebugLevel) { logResult("putBoardsets", DebugLevel) {
    put {
      path( """[a-zA-Z0-9]+""".r ) { id =>
        testlog.info("putBoardset: id is "+id)
        entity(as[BoardSet]) { board =>
          testlog.info("putBoardset: board is "+board)
          resourceUpdated( store.select(id).update(board.copy(name=id)) )
        }
      }
    }
  }}


  @Path("/{boardsetId}")
  @ApiOperation(value = "Delete a boardset by ID", notes = "", nickname = "deleteBoardsetById", httpMethod = "DELETE", code=204, response=classOf[RestMessage])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "boardsetId", value = "ID of the boardset to delete", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "Boardset deleted.", response=classOf[Void])
  ))
  def deleteBoard = delete {
    logRequest("boardsets.delete", DebugLevel) {
      logResult("boardsets.delete", DebugLevel) {
        path( """[a-zA-Z0-9]+""".r ) {
          id => {
            resourceDelete( store.select(id).delete())
          }
        }
      }
    }

  }
}
