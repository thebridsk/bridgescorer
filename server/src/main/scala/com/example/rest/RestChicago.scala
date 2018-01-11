package com.example.rest

import com.example.backend.BridgeService
import com.example.data.MatchChicago
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
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object RestChicago {
  implicit class OrdFoo( val x: MatchChicago) extends AnyVal with Ordered[MatchChicago] {
    def compare(that:MatchChicago) = Id.idComparer(that.id, x.id)
  }

}

import RestChicago._

/**
 * Rest API implementation for the board resource.
 * <p>
 * The REST API and all the methods are documented using
 * swagger annotations.
 */
@Path( "/rest/chicagos" )
@Api(tags= Array("Chicago"), description = "Operations about chicago matches.", produces="application/json", protocols="http, https")
trait RestChicago extends HasActorSystem {

  /**
   * The bridge service backend
   */
  implicit val restService: BridgeService
  lazy val store = restService.chicagos

  val resName = "chicagos"

  import UtilsPlayJson._

  def sort( a: Array[MatchChicago] ) = {

    Sorting.quickSort(a)
    a
  }

  /**
   * spray route for all the methods on this resource
   */
  def route =pathPrefix(resName) {
//    logRequest("route", DebugLevel) {
        getChicago ~ getChicagos ~ postChicago ~ putChicago ~ deleteChicago
//      }
  }

  @ApiOperation(value = "Get all chicago matches", notes = "Returns a list of matches.", response=classOf[MatchChicago], responseContainer="List", nickname = "getChicagos", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "A list of matches, as a JSON array", response=classOf[MatchChicago], responseContainer="List")
  ))
  def getChicagos = pathEnd {
    get {
      resourceMap( store.readAll() )
    }
  }

  @Path("/{matchId}")
  @ApiOperation(value = "Get the match by ID", notes = "", response=classOf[MatchChicago], nickname = "getChicagoById", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "matchId", value = "ID of the board to get", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "The board, as a JSON object", response=classOf[MatchChicago]),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage])
  ))
  def getChicago = logRequest("RestChicago.getChicago", DebugLevel) { logResult("RestChicago.postChicago") { get {
    path( """[a-zA-Z0-9]+""".r ) { id =>
      resource( store.select(id).read() )
    }
  }}}


  @ApiOperation(value = "Create a chicago match", notes = "", response=classOf[MatchChicago], nickname = "createChicago", httpMethod = "POST", code=201)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Chicago Match to create", dataTypeClass = classOf[MatchChicago], required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 201, message = "The created match's JSON", response=classOf[MatchChicago],
        responseHeaders= Array(
            new ResponseHeader( name="Location", description="The URL of the newly created resource", response=classOf[String] )
            )
        ),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[RestMessage])
  ))
  def postChicago =
    logRequest("RestChicago.postChicago") {
      logResult("RestChicago.postChicago") {
        pathEnd {
          post {
            entity(as[MatchChicago]) { chi =>
              resourceCreated( resName, store.createChild(chi) )
            }
          }
        }
      }
    }


  @Path("/{matchId}")
  @ApiOperation(value = "Update a chicago match", notes = "", response=classOf[MatchChicago], nickname = "updateChicago", httpMethod = "PUT", code=204)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "matchId", value = "ID of the board to get", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "body", value = "Chicago Match to update", dataTypeClass = classOf[MatchChicago], required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "Chicago match updated", response=classOf[Void] ),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage]),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[RestMessage])
  ))
  def putChicago =
    logRequest("RestChicago.putChicago") {
      logResult("RestChicago.putChicago") {
        path( """[a-zA-Z0-9]+""".r ) { id =>
          put {
            entity(as[MatchChicago]) { chi =>
              resourceUpdated( store.select(id).update(chi) )
            }
          }
        }
      }
    }


  @Path("/{matchId}")
  @ApiOperation(value = "Delete a match by ID", notes = "", response=classOf[RestMessage], nickname = "deleteChicagoById", httpMethod = "DELETE", code=204)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "matchId", value = "ID of the match to delete", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "Chicago match deleted." )
  ))
  def deleteChicago = path( """[a-zA-Z0-9]+""".r ) { id => {
    delete {
        resourceDelete( store.select(id).delete() )
    } }
  }
}
