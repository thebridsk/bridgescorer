package com.example.rest

import com.example.backend.BridgeService
import com.example.data.MatchRubber
import akka.event.Logging
import akka.event.Logging._
import io.swagger.annotations._
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import com.example.util.HasActorSystem
import javax.ws.rs.Path
import com.example.data.RestMessage
import com.example.data.Id
import scala.util.Sorting
import akka.http.scaladsl.model.headers.Location
import scala.concurrent.ExecutionContext.Implicits.global

object RestRubber {
  implicit class OrdFoo( val x: MatchRubber) extends AnyVal with Ordered[MatchRubber] {
    def compare(that:MatchRubber) = Id.idComparer(that.id, x.id)
  }

}

import RestRubber._

/**
 * Rest API implementation for the board resource.
 * <p>
 * The REST API and all the methods are documented using
 * swagger annotations.
 */
@Path( "/rest/rubbers" )
@Api(tags= Array("Rubber"), description = "Operations about rubbers.", produces="application/json", protocols="http, https")
trait RestRubber extends HasActorSystem {

  /**
   * The bridge service backend
   */
  implicit val restService: BridgeService
  lazy val store = restService.rubbers

  val resName = "rubbers"

  import UtilsPlayJson._

  def sort( a: Array[MatchRubber] ) = {

    Sorting.quickSort(a)
    a
  }

  /**
   * spray route for all the methods on this resource
   */
  def route =pathPrefix(resName) {
//    logRequest("route", DebugLevel) {
        getRubber ~ getRubbers ~ postRubber ~ putRubber ~ deleteRubber
//      }
  }

  @ApiOperation(value = "Get all rubber matches", notes = "Returns a list of matches.", response=classOf[MatchRubber], responseContainer="List", nickname = "getRubbers", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "A list of matches, as a JSON array", response=classOf[MatchRubber], responseContainer="List")
  ))
  def getRubbers = pathEnd {
    get {
      resourceMap( store.readAll() )
    }
  }

  @Path("/{matchId}")
  @ApiOperation(value = "Get the match by ID", notes = "", response=classOf[MatchRubber], nickname = "getRubberById", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "matchId", value = "ID of the board to get", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "The board, as a JSON object", response=classOf[MatchRubber]),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage])
  ))
  def getRubber = logRequest("RestRubber.getRubber", DebugLevel) { logResult("RestRubber.postRubber") { get {
    path( """[a-zA-Z0-9]+""".r ) { id =>
      resource( store.select(id).read() )
    }
  }}}


  @ApiOperation(value = "Create a rubber match", notes = "", response=classOf[MatchRubber], nickname = "createRubber", httpMethod = "POST", code=201)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Rubber Match to create", dataTypeClass = classOf[MatchRubber], required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 201, message = "The created match's JSON", response=classOf[MatchRubber],
        responseHeaders= Array(
            new ResponseHeader( name="Location", description="The URL of the newly created resource", response=classOf[String] )
            )
        ),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[RestMessage])
  ))
  def postRubber =
    logRequest("RestRubber.postRubber") {
      logResult("RestRubber.postRubber") {
        pathEnd {
          post {
            entity(as[MatchRubber]) { chi =>
              resourceCreated( resName, store.createChild(chi), Created )
            }
          }
        }
      }
    }


  @Path("/{matchId}")
  @ApiOperation(value = "Update a rubber match", notes = "", response=classOf[MatchRubber], nickname = "updateRubber", httpMethod = "PUT", code=204)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "matchId", value = "ID of the board to get", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "body", value = "Rubber Match to update", dataTypeClass = classOf[com.example.data.MatchRubber], required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "Rubber match updated" ),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage]),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[RestMessage])
  ))
  def putRubber =
    logRequest("RestRubber.putRubber") {
      logResult("RestRubber.putRubber") {
        path( """[a-zA-Z0-9]+""".r ) { id =>
          put {
            entity(as[MatchRubber]) { chi =>
              resourceUpdated( store.select(id).update(chi) )
            }
          }
        }
      }
    }


  @Path("/{matchId}")
  @ApiOperation(value = "Delete a match by ID", notes = "", response=classOf[RestMessage], nickname = "deleteRubberById", httpMethod = "DELETE", code=204)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "matchId", value = "ID of the match to delete", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "Rubber match deleted." )
  ))
  def deleteRubber = delete {
    path( """[a-zA-Z0-9]+""".r ) {
      id => {
        resourceDelete( store.select(id).delete() )
      }
    }
  }
}
