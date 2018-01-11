package com.example.rest

import com.example.data.Board
import com.example.data.DuplicateHand
import com.example.data.MatchDuplicate
import com.example.data.Id
import akka.event.Logging
import akka.event.Logging._
import io.swagger.annotations._
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
@Api(tags= Array("Duplicate"), description = "Operations about hands.", produces="application/json", protocols="http, https")
class RestNestedHand {

  import UtilsPlayJson._

  /**
   * spray route for all the methods on this resource
   */
  def route(implicit @ApiParam(hidden=true) res: Resources[Id.Team, DuplicateHand]) =pathPrefix("hands") {
    logRequestResult("route", DebugLevel) {
        getHand ~ getHands ~ postHand ~ putHand ~ deleteHand
    }
  }

  @ApiOperation(value = "Get all hands", notes = "Returns a list of hands.", response=classOf[DuplicateHand], responseContainer="List", nickname = "getHands", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "dupId", value = "ID of the match duplicate that contains the boards to manipulate", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "boardId", value = "ID of the board that contains the hands to manipulate", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "A list of hands, as a JSON array", response=classOf[DuplicateHand], responseContainer="List")
  ))
  def getHands(implicit @ApiParam(hidden=true) res: Resources[Id.DuplicateHand, DuplicateHand]) = pathEndOrSingleSlash {
    get {
      resourceMap( res.readAll() )
    }
  }

  @Path("/{handId}")
  @ApiOperation(value = "Get the hand by ID", notes = "", response=classOf[DuplicateHand], nickname = "getHandById", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "dupId", value = "ID of the match duplicate that contains the boards to manipulate", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "boardId", value = "ID of the board that contains the hands to manipulate", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "handId", value = "ID of the hand to get", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "The hand, as a JSON object", response=classOf[DuplicateHand]),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage])
  ))
  def getHand(implicit @ApiParam(hidden=true) res: Resources[String, DuplicateHand]) = logRequest("getHand", DebugLevel) {
    get {
      path( """[a-zA-Z0-9]+""".r ) { id =>
        resource( res.select(id).read() )
      }
    }
  }

  @ApiOperation(value = "Create a hand", notes = "", response=classOf[DuplicateHand], nickname = "createHand", httpMethod = "POST", code=201)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "dupId", value = "ID of the match duplicate that contains the boards to manipulate", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "boardId", value = "ID of the board that contains the hands to manipulate", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "body", value = "hand to create", dataTypeClass = classOf[DuplicateHand], required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 201, message = "The created hand's JSON", response=classOf[DuplicateHand],
        responseHeaders= Array(
            new ResponseHeader( name="Location", description="The URL of the newly created resource", response=classOf[String] )
            )
        ),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[RestMessage])
  ))
  def postHand(implicit @ApiParam(hidden=true) res: Resources[String, DuplicateHand]) = pathEnd {
    post {
        entity(as[DuplicateHand]) { hand =>
          log.fine(s"Creating new hand ${hand} in ${res.resourceURI}")
          resourceCreated( res.resourceURI, addIdToFuture(res.createChild(hand)) )
        }
    }
  }

  def addIdToFuture( f: Future[Result[DuplicateHand]] ): Future[Result[(String,DuplicateHand)]] =
    f.map { r =>
      r match {
        case Right(md) => Right((md.id.toString(),md))
        case Left(e) => Left(e)
      }
    }

  @Path("/{handId}")
  @ApiOperation(value = "Update a hand", notes = "", response=classOf[DuplicateHand], nickname = "updateHand", httpMethod = "PUT", code=204)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "dupId", value = "ID of the match duplicate that contains the boards to manipulate", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "boardId", value = "ID of the board that contains the hands to manipulate", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "handId", value = "ID of the hand to update", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "body", value = "duplicate Match to create", dataTypeClass = classOf[DuplicateHand], required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "Hand updated" ),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage]),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[RestMessage])
  ))
  def putHand(implicit @ApiParam(hidden=true) res: Resources[String, DuplicateHand]) = logRequest("putHand", DebugLevel) { logResult("putHand", DebugLevel) {
    put {
      path( """[a-zA-Z0-9]+""".r ) { id =>
        entity(as[DuplicateHand]) { hand =>
          resourceUpdated( res.select(id).update(hand) )
        }
      }
    }
  }}

  @Path("/{handId}")
  @ApiOperation(value = "Delete a hand by ID", notes = "", response=classOf[RestMessage], nickname = "deleteHandById", httpMethod = "DELETE", code=204)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "dupId", value = "ID of the match duplicate that contains the boards to manipulate", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "boardId", value = "ID of the board that contains the hands to manipulate", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "handId", value = "ID of the hand to delete", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "Hand deleted." )
  ))
  def deleteHand(implicit @ApiParam(hidden=true) res: Resources[String, DuplicateHand]) = delete {
    path( """[a-zA-Z0-9]+""".r ) { id =>
      resourceDelete( res.select(id).delete() )
    }
  }
}
