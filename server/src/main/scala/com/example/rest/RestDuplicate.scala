package com.example.rest

import com.example.backend.BridgeService
import com.example.data.Board
import com.example.data.MatchDuplicate
import akka.event.Logging
import akka.event.Logging._
import io.swagger.annotations._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.example.util.HasActorSystem
import akka.http.scaladsl.model.StatusCode
import com.example.data.Id
import com.example.data.DuplicateSummary
import javax.ws.rs.Path
import com.example.data.RestMessage
import com.example.data.SystemTime
import akka.http.scaladsl.model.headers.Location
import scala.util.Success
import scala.util.Failure
import akka.http.scaladsl.model.StatusCodes
import scala.concurrent.ExecutionContext.Implicits.global
import com.example.backend.BridgeNestedResources._

/**
 * Rest API implementation for the board resource.
 * <p>
 * The REST API and all the methods are documented using
 * swagger annotations.
 */
@Path( "/rest/duplicates" )
@Api(tags= Array("Duplicate"), description = "Operations about duplicates.", produces="application/json", protocols="http, https")
trait RestDuplicate extends HasActorSystem {

  private lazy val log = Logging(actorSystem, classOf[RestDuplicate])

  val restService: BridgeService

  lazy val store = restService.duplicates

  val resName = "duplicates"

  import UtilsPlayJson._

  val nestedBoards = new RestNestedBoard
  val nestedTeams = new RestNestedTeam

  /**
   * spray route for all the methods on this resource
   */
  def route = pathPrefix(resName) {
//    logRequest("route", DebugLevel) {
        getDuplicate ~ getDuplicates ~ postDuplicate ~ putDuplicate ~ deleteDuplicate ~ nested
//      }
  }

  @ApiOperation(value = "Get all duplicate matches", notes = "Returns a list of matches.", response=classOf[MatchDuplicate], responseContainer="List", nickname = "getDuplicates", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "A list of matches, as a JSON array", response=classOf[MatchDuplicate], responseContainer="List")
  ))
  def getDuplicates = pathEnd {
    get {
      resourceMap( store.readAll() )
    }
  }

  @Path("/{dupId}")
  @ApiOperation(value = "Get the match by ID", notes = "", response=classOf[MatchDuplicate], nickname = "getDuplicateById", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "dupId", value = "ID of the board to get", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "The board, as a JSON object", response=classOf[MatchDuplicate]),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage])
  ))
  def getDuplicate = logRequest("RestDuplicate.getDuplicate", DebugLevel) { logResult("RestDuplicate.getDuplicate") { get {
    pathPrefix( """[a-zA-Z0-9]+""".r ) { id: Id.MatchDuplicate =>
      pathEndOrSingleSlash {
        resource( store.select(id).read() )
      }
    }
  }}}

  def nested= logRequest("RestDuplicate.nested", DebugLevel) { logResult("RestDuplicate.nested") {
    pathPrefix( """[a-zA-Z0-9]+""".r ) { id: Id.MatchDuplicate =>
      val selected = store.select(id)
      nestedBoards.route( selected.resourceBoards ) ~
      nestedTeams.route( selected.resourceTeams )
    }
  }}

  @ApiOperation(value = "Create a duplicate match", notes = "", response=classOf[MatchDuplicate], nickname = "createDuplicate", httpMethod = "POST", code=201)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "test", value = "If present, create test match", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "default", value = "If present, indicates boards and hands should be added.  Default movements is Armonk2Tables, default boards is ArmonkBoards", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "boards", value = "If present, indicates which boards to use", allowableValues="StandardBoards, ArmonkBoards", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "movements", value = "If present, indicates which movements to use", allowableValues="Howell3TableNoRelay, Mitchell3Table, Howell2Table5Teams, Armonk2Tables", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "body", value = "duplicate Match to create", dataTypeClass = classOf[MatchDuplicate], required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 201, message = "The created match's JSON", response=classOf[MatchDuplicate],
        responseHeaders= Array(
            new ResponseHeader( name="Location", description="The URL of the newly created resource", response=classOf[String] )
            )
        ),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[RestMessage])
  ))
  def postDuplicate =
    logRequest("RestDuplicate.postDuplicate") {
      logResult("RestDuplicate.postDuplicate") {
        pathEnd {
          post {
            parameter( 'test.?, 'default.?, 'boards.?, 'movements.? ) { (test,default,boards,movements) =>
              entity(as[MatchDuplicate]) { dup =>
//                log.warning("Creating duplicate match from "+dup)
                RestDuplicate.createMatchDuplicate(restService, dup, test, default, boards, movements) match {
                  case Some(fut) =>
                    onComplete( fut ) {
                      case Success(r) =>
                        r match {
                          case Right(md) =>
                            resourceCreated( resName, store.createChild( md ) )
                          case Left((code,msg)) =>
                            complete(code,msg)
                        }
                      case Failure(ex) =>
                        RestLoggerConfig.log.info("Exception posting duplicate: ", ex)
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


  @Path("/{dupId}")
  @ApiOperation(value = "Update a duplicate match", notes = "", response=classOf[MatchDuplicate], nickname = "updateDuplicate", httpMethod = "PUT", code=204)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "dupId", value = "ID of the match to delete", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "body", value = "The updated duplicate Match", dataTypeClass = classOf[MatchDuplicate], required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "Duplicate match updated" ),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage]),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[RestMessage])
  ))
  def putDuplicate =
    logRequest("RestDuplicate.putDuplicate") {
      logResult("RestDuplicate.putDuplicate") {
        put {
          path( """[a-zA-Z0-9]+""".r ) { id: Id.MatchDuplicate =>
            entity(as[MatchDuplicate]) { dup =>
              resourceUpdated( store.select(id).update(dup) )
            }
          }
        }
      }
    }


  @Path("/{dupId}")
  @ApiOperation(value = "Delete a match by ID", notes = "", response=classOf[RestMessage], nickname = "deleteDuplicateById", httpMethod = "DELETE", code=204)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "dupId", value = "ID of the match to delete", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "duplicate deleted." )
  ))
  def deleteDuplicate = delete {
    path( """[a-zA-Z0-9]+""".r ) {
      id: Id.MatchDuplicate => {
        resourceDelete( store.select(id).delete() )
      }
    }
  }
}

object RestDuplicate {
  def createMatchDuplicate(
                            restService: BridgeService,
                            dup: MatchDuplicate,
                            test: Option[String],
                            default: Option[String],
                            boards: Option[String],
                            movements: Option[String]
                          ) = {
    test match {
      case None =>
        if (default.isDefined || boards.isDefined || movements.isDefined) {
          val b = boards.getOrElse(restService.defaultBoards)
          val h = movements.getOrElse(restService.defaultMovement)
          Some(restService.fillBoards(dup,b,h))
        } else {
          None
        }
      case Some(s) =>
        Some(restService.createTestDuplicate(dup))
    }
  }
}
