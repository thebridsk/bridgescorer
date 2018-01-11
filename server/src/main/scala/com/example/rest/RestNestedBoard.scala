package com.example.rest

import com.example.data.Board
import com.example.data.MatchDuplicate
import com.example.data.DuplicateHand
import akka.event.Logging
import akka.event.Logging._
import io.swagger.annotations._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import com.example.util.HasActorSystem
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.StatusCode
import com.example.backend.BridgeService
import com.example.data.Id
import javax.ws.rs.Path
import com.example.data.RestMessage
import akka.http.scaladsl.model.headers.Location
import scala.concurrent.ExecutionContext.Implicits.global
import com.example.backend.resource.Resources
import com.example.backend.BridgeNestedResources
import scala.concurrent.Future
import com.example.backend.resource.Result

/**
 * Rest API implementation for the board resource.
 * <p>
 * The REST API and all the methods are documented using
 * swagger annotations.
 */
@Path("/rest/duplicates/{dupId}/boards")
@Api(tags= Array("Duplicate"), description = "Operations about boards in duplicate matches.", produces="application/json", protocols="http, https")
class RestNestedBoard {

  import UtilsPlayJson._

  val nestedHands = new RestNestedHand

  /**
   * spray route for all the methods on this resource
   */
  def route( implicit @ApiParam(hidden=true) res: Resources[Id.DuplicateBoard, Board]) =pathPrefix("boards") {
//    logRequest("route", DebugLevel) {
        getBoard ~ getBoards ~ postBoard ~ putBoard ~ deleteBoard ~ restNestedHands
//      }
  }

  @ApiOperation(value = "Get all boards", notes = "Returns a list of boards.", response=classOf[Board], responseContainer="List", nickname = "getBoards", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "dupId", value = "ID of the duplicate that contains the boards to manipulate", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "A list of boards, as a JSON array", response=classOf[Board], responseContainer="List")
  ))
  def getBoards( implicit @ApiParam(hidden=true) res: Resources[Id.DuplicateBoard, Board]) = pathEndOrSingleSlash {
    get {
      resourceMap( res.readAll() )
    }
  }

  @Path("/{boardId}")
  @ApiOperation(value = "Get the board by ID", notes = "", response=classOf[Board], nickname = "getBoardById", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "dupId", value = "ID of the duplicate that contains the boards to manipulate", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "boardId", value = "ID of the board to get", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "The board, as a JSON object", response=classOf[Board]),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage])
  ))
  def getBoard( implicit @ApiParam(hidden=true) res: Resources[Id.DuplicateBoard, Board]) = logRequest("getBoard", DebugLevel) { get {
    path( """[a-zA-Z0-9]+""".r ) { id =>
      resource( res.select(id).read() )
    }
  }}

  def restNestedHands( implicit @ApiParam(hidden=true) res: Resources[Id.DuplicateBoard, Board]) = logRequestResult("RestNestedBoard.restNestedHand", DebugLevel) {
    pathPrefix( """[a-zA-Z0-9]+""".r ) { id =>
      import BridgeNestedResources._
      nestedHands.route(res.select(id).resourceHands)
    }
  }

  @ApiOperation(value = "Create a board", notes = "", response=classOf[Board], nickname = "createBoard", httpMethod = "POST", code=201)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "dupId", value = "ID of the duplicate that contains the boards to manipulate", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "body", value = "duplicate board to create", dataTypeClass = classOf[Board], required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 201, message = "The created board's JSON", response=classOf[Board],
        responseHeaders= Array(
            new ResponseHeader( name="Location", description="The URL of the newly created resource", response=classOf[String] )
            )
        ),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[RestMessage])
  ))
  def postBoard( implicit @ApiParam(hidden=true) res: Resources[Id.DuplicateBoard, Board]) = pathEnd {
    post {
        entity(as[Board]) { board =>
          resourceCreated( res.resourceURI, addIdToFuture(res.createChild(board)) )
        }
    }
  }

  def addIdToFuture( f: Future[Result[Board]] ): Future[Result[(String,Board)]] =
    f.map { r =>
      r match {
        case Right(md) => Right((md.id.toString(),md))
        case Left(e) => Left(e)
      }
    }

  @Path("/{boardId}")
  @ApiOperation(value = "Update a board", notes = "", response=classOf[Board], nickname = "createBoard", httpMethod = "PUT", code=204)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "dupId", value = "ID of the duplicate that contains the boards to manipulate", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "boardId", value = "ID of the board to update", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "body", value = "duplicate board to create", dataTypeClass = classOf[Board], required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "Board updated" ),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage]),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[RestMessage])
  ))
  def putBoard( implicit @ApiParam(hidden=true) res: Resources[Id.DuplicateBoard, Board]) =
    put {
      path( """[a-zA-Z0-9]+""".r ) { id =>
        entity(as[Board]) { board =>
          resourceUpdated( res.select(id).update(board) )
        }
      }
    }


  @Path("/{boardId}")
  @ApiOperation(value = "Delete a board by ID", response=classOf[RestMessage], notes = "", nickname = "deleteBoardById", httpMethod = "DELETE", code=204)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "dupId", value = "ID of the duplicate that contains the boards to manipulate", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "boardId", value = "ID of the board to delete", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "Board deleted." )
  ))
  def deleteBoard( implicit @ApiParam(hidden=true) res: Resources[Id.DuplicateBoard, Board]) = delete {
    path( """[a-zA-Z0-9]+""".r ) { id =>
      resourceDelete( res.select(id).delete() )
    }
  }
}
