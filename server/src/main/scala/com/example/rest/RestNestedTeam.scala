package com.example.rest

import com.example.data.Board
import com.example.data.DuplicateHand
import com.example.data.MatchDuplicate
import com.example.data.Team
import com.example.data.Id
import akka.event.Logging
import akka.event.Logging._
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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.Hidden
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
@Path("/rest/duplicates/{dupId}/teams")
@Tags(Array(new Tag(name = "Duplicate")))
class RestNestedTeam {

  val resName = "teams"

  import UtilsPlayJson._

  /**
    * spray route for all the methods on this resource
    */
  @Hidden
  def route(implicit @Parameter(hidden = true) res: Resources[Id.Team, Team]) =
    logRequest("RestDuplicate.nestedTeam", DebugLevel) {
      logResult("RestDuplicate.nestedTeam") {
        pathPrefix(resName) {
          logRequestResult("RestDuplicate.nestedTeam", DebugLevel) {
            getTeam ~ getTeams ~ postTeam ~ putTeam ~ deleteTeam()
          }
        }
      }
    }

  @GET
  @Operation(
    summary = "Get all teams",
    description = "Returns a list of teams.",
    operationId = "getTeams",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description =
          "ID of the match duplicate that contains the teams to manipulate",
        in = ParameterIn.PATH,
        name = "dupId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "A list of teams, as a JSON array",
        content = Array(
          new Content(
            mediaType = "application/json",
            array = new ArraySchema(
              minItems = 0,
              uniqueItems = true,
              schema = new Schema(implementation = classOf[Team])
            )
          )
        )
      )
    )
  )
  def xxxgetTeams = {}
  def getTeams(
      implicit @Parameter(hidden = true) res: Resources[Id.Team, Team]
  ) = pathEnd {
    get {
      resourceMap(res.readAll())
    }
  }

  @Path("/{teamId}")
  @GET
  @Operation(
    summary = "Get the team by ID",
    operationId = "getTeamById",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description =
          "ID of the match duplicate that contains the teams to manipulate",
        in = ParameterIn.PATH,
        name = "dupId",
        required = true,
        schema = new Schema(`type` = "string")
      ),
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the team to get",
        in = ParameterIn.PATH,
        name = "teamId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The team, as a JSON object",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[Team])
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
  def xxxgetTeam = {}
  def getTeam(
      implicit @Parameter(hidden = true) res: Resources[Id.Team, Team]
  ) = logRequest("getTeam", DebugLevel) {
    get {
      path("""[a-zA-Z0-9]+""".r) { id =>
        resource(res.select(id).read())
      }
    }
  }

  @POST
  @Operation(
    summary = "Create a team",
    operationId = "createTeam",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description =
          "ID of the match duplicate that contains the teams to manipulate",
        in = ParameterIn.PATH,
        name = "dupId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    requestBody = new RequestBody(
      description = "team to create",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[Team])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description = "The created team's JSON",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[Team])
          )
        ),
        headers = Array(
          new Header(
            name = "Location",
            description = "The URL of the newly created resource",
            schema = new Schema(implementation = classOf[String])
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
  def xxxpostTeam = {}
  def postTeam(
      implicit @Parameter(hidden = true) res: Resources[Id.Team, Team]
  ) = pathEnd {
    post {
      entity(as[Team]) { hand =>
        resourceCreated(res.resourceURI, addIdToFuture(res.createChild(hand)))
      }
    }
  }

  def addIdToFuture(f: Future[Result[Team]]): Future[Result[(String, Team)]] =
    f.map { r =>
      r match {
        case Right(md) => Right((md.id.toString(), md))
        case Left(e)   => Left(e)
      }
    }

  @Path("/{teamId}")
  @PUT
  @Operation(
    summary = "Update a team",
    operationId = "updateTeam",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description =
          "ID of the match duplicate that contains the teams to manipulate",
        in = ParameterIn.PATH,
        name = "dupId",
        required = true,
        schema = new Schema(`type` = "string")
      ),
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the team to delete",
        in = ParameterIn.PATH,
        name = "teamId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    requestBody = new RequestBody(
      description = "team to update",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[Team])
        )
      )
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "Team updated"),
      new ApiResponse(
        responseCode = "404",
        description = "Does not exist.",
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
  def xxxputTeam = {}
  def putTeam(
      implicit @Parameter(hidden = true) res: Resources[Id.Team, Team]
  ) = put {
    path("""[a-zA-Z0-9]+""".r) { id =>
      entity(as[Team]) { hand =>
        resourceUpdated(res.select(id).update(hand))
      }
    }
  }

  @Path("/{teamId}")
  @DELETE
  @Operation(
    summary = "Delete a team by ID",
    operationId = "deleteTeamById",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the duplicate that contains the team to manipulate",
        in = ParameterIn.PATH,
        name = "dupId",
        required = true,
        schema = new Schema(`type` = "string")
      ),
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the team to delete",
        in = ParameterIn.PATH,
        name = "teamId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "Team deleted.")
    )
  )
  def xxxdeleteTeams = {}
  def deleteTeam()(
      implicit @Parameter(hidden = true) res: Resources[Id.Team, Team]
  ) =
    delete {
      path("""[a-zA-Z0-9]+""".r) { id =>
        resourceDelete(res.select(id).delete())
      }
    }
}
