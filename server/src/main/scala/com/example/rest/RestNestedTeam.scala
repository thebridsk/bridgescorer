package com.example.rest

import com.example.data.Board
import com.example.data.DuplicateHand
import com.example.data.MatchDuplicate
import com.example.data.Team
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
import com.example.backend.resource.Resources
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.example.backend.resource.Result

/**
 * Rest API implementation for the board resource.
 * <p>
 * The REST API and all the methods are documented using
 * swagger annotations.
 */
@Path("/rest/duplicates/{dupId}/teams")
@Api(tags= Array("Duplicate"), description = "Operations about teams.", produces="application/json", protocols="http, https")
class RestNestedTeam {

  val resName = "teams"

  import UtilsPlayJson._

  /**
   * spray route for all the methods on this resource
   */
  def route(implicit @ApiParam(hidden=true) res: Resources[Id.Team, Team]) =
    logRequest("RestDuplicate.nestedTeam", DebugLevel) { logResult("RestDuplicate.nestedTeam") {
      pathPrefix(resName) {
        logRequestResult("RestDuplicate.nestedTeam", DebugLevel) {
          getTeam ~ getTeams ~ postTeam ~ putTeam ~ deleteTeam()
        }
      }
    }
  }

  @ApiOperation(value = "Get all teams", notes = "Returns a list of teams.", response=classOf[Team], responseContainer="List", nickname = "getTeams", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "dupId", value = "ID of the match duplicate that contains the teams to manipulate",
                         required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "A list of teams, as a JSON array", response=classOf[Team], responseContainer="List")
  ))
  def getTeams(implicit @ApiParam(hidden=true) res: Resources[Id.Team, Team]) = pathEnd {
    get {
      resourceMap( res.readAll() )
    }
  }

  @Path("/{teamId}")
  @ApiOperation(value = "Get the team by ID", notes = "", response=classOf[Team], nickname = "getTeamById", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "dupId", value = "ID of the match duplicate that contains the teams to manipulate",
                         required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "teamId", value = "ID of the team to get", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "The team, as a JSON object", response=classOf[Team]),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage])
  ))
  def getTeam(implicit @ApiParam(hidden=true) res: Resources[Id.Team, Team]) = logRequest("getTeam", DebugLevel) {
    get {
      path( """[a-zA-Z0-9]+""".r ) { id =>
        resource( res.select(id).read() )
      }
    }
  }

  @ApiOperation(value = "Create a team", notes = "", response=classOf[Team], nickname = "createTeam", httpMethod = "POST", code=201)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "dupId", value = "ID of the match duplicate that contains the teams to manipulate",
                         required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "body", value = "team to create", dataTypeClass = classOf[Team], required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 201, message = "The created team's JSON", response=classOf[Team],
        responseHeaders= Array(
            new ResponseHeader( name="Location", description="The URL of the newly created resource", response=classOf[String] )
            )
        ),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[RestMessage])
  ))
  def postTeam(implicit @ApiParam(hidden=true) res: Resources[Id.Team, Team]) = pathEnd {
    post {
        entity(as[Team]) { hand =>
          resourceCreated( res.resourceURI, addIdToFuture(res.createChild(hand)) )
        }
    }
  }

  def addIdToFuture( f: Future[Result[Team]] ): Future[Result[(String,Team)]] =
    f.map { r =>
      r match {
        case Right(md) => Right((md.id.toString(),md))
        case Left(e) => Left(e)
      }
    }

  @Path("/{teamId}")
  @ApiOperation(value = "Update a team", notes = "", response=classOf[Team], nickname = "createTeam", httpMethod = "PUT", code=204)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "dupId", value = "ID of the match duplicate that contains the teams to manipulate",
                         required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "teamId", value = "ID of the team to delete", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "body", value = "team to create", dataTypeClass = classOf[Team], required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "Team updated" ),
    new ApiResponse(code = 404, message = "Does not exist.", response=classOf[RestMessage]),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[RestMessage])
  ))
  def putTeam(implicit @ApiParam(hidden=true) res: Resources[Id.Team, Team]) = put {
    path( """[a-zA-Z0-9]+""".r ) { id =>
      entity(as[Team]) { hand =>
        resourceUpdated( res.select(id).update(hand) )
      }
    }
  }

  @Path("/{teamId}")
  @ApiOperation(value = "Delete a team by ID", notes = "", response=classOf[RestMessage], nickname = "deleteTeamById", httpMethod = "DELETE", code=204)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "teamId", value = "ID of the team to delete", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "Team deleted." )
  ))
  def deleteTeam()(implicit @ApiParam(hidden=true) res: Resources[Id.Team, Team]) =
    delete {
      path( """[a-zA-Z0-9]+""".r ) { id =>
        resourceDelete( res.select(id).delete() )
      }
    }
}
