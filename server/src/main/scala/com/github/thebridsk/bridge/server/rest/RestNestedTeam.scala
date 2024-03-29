package com.github.thebridsk.bridge.server.rest

import com.github.thebridsk.bridge.data.Team
import akka.event.Logging._
import akka.http.scaladsl.server.Directives._
import jakarta.ws.rs.Path
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.server.backend.resource.Resources
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.github.thebridsk.bridge.server.backend.resource.Result
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
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.DELETE
import akka.http.scaladsl.server.Route

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
  def route(implicit
      @Parameter(hidden = true) res: Resources[Team.Id, Team]
  ): Route =
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
  def xxxgetTeams: Unit = {}
  def getTeams(implicit
      @Parameter(hidden = true) res: Resources[Team.Id, Team]
  ): Route =
    pathEnd {
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
  def xxxgetTeam: Unit = {}
  def getTeam(implicit
      @Parameter(hidden = true) res: Resources[Team.Id, Team]
  ): Route =
    logRequest("getTeam", DebugLevel) {
      get {
        path("""[a-zA-Z0-9]+""".r) { id =>
          resource(res.select(Team.id(id)).read())
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
  def xxxpostTeam: Unit = {}
  def postTeam(implicit
      @Parameter(hidden = true) res: Resources[Team.Id, Team]
  ): Route =
    pathEnd {
      post {
        entity(as[Team]) { hand =>
          resourceCreated(res.resourceURI, addIdToFuture(res.createChild(hand)))
        }
      }
    }

  def addIdToFuture(f: Future[Result[Team]]): Future[Result[(String, Team)]] =
    f.map { r =>
      r match {
        case Right(md) => Right((md.id.id, md))
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
  def xxxputTeam: Unit = {}
  def putTeam(implicit
      @Parameter(hidden = true) res: Resources[Team.Id, Team]
  ): Route =
    put {
      path("""[a-zA-Z0-9]+""".r) { id =>
        entity(as[Team]) { hand =>
          resourceUpdated(res.select(Team.id(id)).update(hand))
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
        description =
          "ID of the duplicate that contains the team to manipulate",
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
      new ApiResponse(responseCode = "204", description = "Team deleted."),
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
  def xxxdeleteTeams: Unit = {}
  def deleteTeam()(implicit
      @Parameter(hidden = true) res: Resources[Team.Id, Team]
  ): Route =
    delete {
      path("""[a-zA-Z0-9]+""".r) { id =>
        resourceDelete(res.select(Team.id(id)).delete())
      }
    }
}
