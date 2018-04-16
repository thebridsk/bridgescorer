package com.example.rest

import com.example.backend.BridgeService
import com.example.data.MatchDuplicateResult
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
import com.example.data.MatchDuplicate
import com.example.data.DuplicateSummary
import utils.logging.Logger
import scala.util.Success
import scala.util.Failure
import akka.http.scaladsl.model.StatusCodes
import scala.concurrent.ExecutionContext.Implicits.global

object RestDuplicateResult {
  implicit class OrdFoo( val x: MatchDuplicateResult) extends AnyVal with Ordered[MatchDuplicateResult] {
    def compare(that:MatchDuplicateResult) = Id.idComparer(that.id, x.id)
  }
}

import RestDuplicateResult._

/**
 * Rest API implementation for the board resource.
 * <p>
 * The REST API and all the methods are documented using
 * swagger annotations.
 */
@Path( "/rest/duplicateresults" )
@Api(tags= Array("Duplicate"),
     description = "Operations about duplicateresults.",
     produces="application/json",
     protocols="http, https")
trait RestDuplicateResult extends HasActorSystem {

  private lazy val log = Logging(actorSystem, classOf[RestDuplicate])

  /**
   * The bridge service backend
   */
  implicit val restService: BridgeService
  lazy val store = restService.duplicateresults

  val resName = "duplicateresults"

  import UtilsPlayJson._

  def sort( a: Array[MatchDuplicateResult] ) = {

    Sorting.quickSort(a)
    a
  }

  /**
   * spray route for all the methods on this resource
   */
  def route =pathPrefix(resName) {
//    logRequest("route", DebugLevel) {
        getDuplicateResult ~ getDuplicateResults ~ postDuplicateResult ~ putDuplicateResult ~ deleteDuplicateResult
//      }
  }

  @ApiOperation(value = "Get all duplicate results",
                notes = "Returns a list of matches.",
                response=classOf[MatchDuplicateResult],
                responseContainer="List",
                nickname = "getDuplicateResults",
                httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200,
                    message = "A list of matches, as a JSON array",
                    response=classOf[MatchDuplicateResult],
                    responseContainer="List")
  ))
  def getDuplicateResults = pathEnd {
    get {
      resourceMap( store.readAll() )
    }
  }

  @Path("/{matchId}")
  @ApiOperation(value = "Get the match by ID",
                notes = "",
                response=classOf[MatchDuplicateResult],
                nickname = "getDuplicateResultById",
                httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "matchId",
                         value = "ID of the board to get",
                         required = true,
                         dataType = "string",
                         paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200,
                    message = "The board, as a JSON object",
                    response=classOf[MatchDuplicateResult]),
    new ApiResponse(code = 404,
                    message = "Does not exist.",
                    response=classOf[RestMessage])
  ))
  def getDuplicateResult = logRequest("RestDuplicateResult.getDuplicateResult", DebugLevel) { logResult("RestDuplicateResult.postDuplicateResult") { get {
    path( """[a-zA-Z0-9]+""".r ) { id =>
      resource( store.select(id).read() )
    }
  }}}


  @ApiOperation(value = "Create a chicago match",
                notes = "",
                response=classOf[MatchDuplicateResult],
                nickname = "createDuplicateResult",
                httpMethod = "POST",
                code=201)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "test", value = "If present, create test match", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "default", value = "If present, indicates boards and hands should be added.  Default movements is Armonk2Tables, default boards is ArmonkBoards", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "boards", value = "If present, indicates which boards to use", allowableValues="StandardBoards, ArmonkBoards", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "movements", value = "If present, indicates which movements to use", allowableValues="Howell3TableNoRelay, Mitchell3Table, Howell2Table5Teams, Armonk2Tables", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "body",
                         value = "DuplicateResult Match to create",
                         dataTypeClass = classOf[MatchDuplicateResult],
                         required = true,
                         paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 201,
                    message = "The created match's JSON",
                    response=classOf[MatchDuplicateResult],
        responseHeaders= Array(
            new ResponseHeader( name="Location",
                                description="The URL of the newly created resource",
                                response=classOf[String] )
            )
        ),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[RestMessage])
  ))
  def postDuplicateResult =
    logRequest("RestDuplicateResult.postDuplicateResult") {
      logResult("RestDuplicateResult.postDuplicateResult") {
        pathEnd {
          post {
            parameter( 'test.?, 'default.?, 'boards.?, 'movements.? ) { (test,default,boards,movements) =>
              entity(as[MatchDuplicateResult]) { dup =>
//                log.warning("Creating duplicate match from "+dup)
                RestDuplicate.createMatchDuplicate(restService, MatchDuplicate.create(), test, default, boards, movements) match {
                  case Some(fut) =>
                    onComplete( fut ) {
                      case Success(r) =>
                        r match {
                          case Right(md) =>
                            val mdr = MatchDuplicateResult.createFrom(md,Some(dup))
                            resourceCreated( resName, store.createChild( mdr ) )
                          case Left((code,msg)) =>
                            complete(code,msg)
                        }
                      case Failure(ex) =>
                        RestLoggerConfig.log.info("Exception posting duplicate result: ", ex)
                        complete( StatusCodes.InternalServerError, "Internal server error" )
                    }
                  case None =>
                    resourceCreated( resName, store.createChild(dup) )
                }
              }
            }
          }
        }
      }
    }

  @Path("/{matchId}")
  @ApiOperation(value = "Update a chicago match",
                notes = "",
                response=classOf[MatchDuplicateResult],
                nickname = "updateDuplicateResult",
                httpMethod = "PUT",
                code=204)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "matchId",
                         value = "ID of the board to get",
                         required = true,
                         dataType = "string",
                         paramType = "path"),
    new ApiImplicitParam(name = "body",
                         value = "DuplicateResult Match to update",
                         dataTypeClass = classOf[MatchDuplicateResult],
                         required = true,
                         paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "DuplicateResult match updated", response=classOf[Void] ),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage]),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[RestMessage])
  ))
  def putDuplicateResult =
    logRequest("RestDuplicateResult.putDuplicateResult") {
      logResult("RestDuplicateResult.putDuplicateResult") {
        path( """[a-zA-Z0-9]+""".r ) { id =>
          put {
            entity(as[MatchDuplicateResult]) { chi =>
              resourceUpdated( store.select(id).update(chi) )
            }
          }
        }
      }
    }


  @Path("/{matchId}")
  @ApiOperation(value = "Delete a match by ID",
                notes = "",
                response=classOf[RestMessage],
                nickname = "deleteDuplicateResultById",
                httpMethod = "DELETE",
                code=204)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "matchId",
                         value = "ID of the match to delete",
                         required = true,
                         dataType = "string",
                         paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "DuplicateResult match deleted." )
  ))
  def deleteDuplicateResult = path( """[a-zA-Z0-9]+""".r ) { id => {
    delete {
        resourceDelete( store.select(id).delete() )
    } }
  }
}
