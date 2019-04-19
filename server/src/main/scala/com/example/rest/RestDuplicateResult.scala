package com.example.rest

import com.example.backend.BridgeService
import com.example.data.MatchDuplicateResult
import akka.event.Logging
import akka.event.Logging._
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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.DELETE

object RestDuplicateResult {
  implicit class OrdFoo( val x: MatchDuplicateResult) extends AnyVal with Ordered[MatchDuplicateResult] {
    def compare(that:MatchDuplicateResult) = Id.idComparer(that.id, x.id)
  }
}

import RestDuplicateResult._
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.GET

/**
 * Rest API implementation for the board resource.
 * <p>
 * The REST API and all the methods are documented using
 * swagger annotations.
 */
@Path( "/rest/duplicateresults" )
@Tags( Array( new Tag(name="Duplicate")))
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
  val route =pathPrefix(resName) {
//    logRequest("route", DebugLevel) {
        getDuplicateResult ~ getDuplicateResults ~ postDuplicateResult ~ putDuplicateResult ~ deleteDuplicateResult
//      }
  }

  @GET
  @Operation(
      summary = "Get all duplicate results",
      description = "Returns a list of matches.",
      operationId = "getDuplicateResults",
      responses = Array(
          new ApiResponse(
              responseCode = "200",
              description = "A list of matches, as a JSON array",
              content = Array(
                  new Content(
                      mediaType = "application/json",
                      array = new ArraySchema(
                          minItems = 0,
                          uniqueItems = true,
                          schema = new Schema( implementation=classOf[MatchDuplicateResult] )
                      )
                  )
              )
          )
      )
  )
  def xxxgetDuplicateResults() = {}
  val getDuplicateResults = pathEnd {
    get {
      resourceMap( store.readAll() )
    }
  }

  @Path("/{matchId}")
  @GET
  @Operation(
      summary = "Get the duplicate results by ID",
      description = "Returns the specified duplicate results.",
      operationId = "getDuplicateResultById",
      parameters = Array(
          new Parameter(
              allowEmptyValue=false,
              description="ID of the duplicate results to get",
              in=ParameterIn.PATH,
              name="matchId",
              required=true,
              schema=new Schema(`type`="string")
          )
      ),
      responses = Array(
          new ApiResponse(
              responseCode = "200",
              description = "A duplicate results, as a JSON object",
              content = Array(
                  new Content(
                      mediaType = "application/json",
                      schema = new Schema( implementation=classOf[MatchDuplicateResult] )
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
  def xxxgetDuplicateResult() = {}
  val getDuplicateResult = logRequest("RestDuplicateResult.getDuplicateResult", DebugLevel) { logResult("RestDuplicateResult.postDuplicateResult") { get {
    path( """[a-zA-Z0-9]+""".r ) { id =>
      resource( store.select(id).read() )
    }
  }}}

  @POST
  @Operation(
      summary = "Create a duplicate result",
      operationId = "createDuplicateResult",
      parameters = Array(
          new Parameter(
              name = "test",
              in = ParameterIn.QUERY,
              allowEmptyValue = true,
              description = "If present, create test match, value is ignored.",
              required = false,
              schema = new Schema(implementation=classOf[String]),
          ),
          new Parameter(
              name = "default",
              in = ParameterIn.QUERY,
              allowEmptyValue = true,
              description = "If present, indicates boards and hands should be added.  Default movements is Armonk2Tables, default boards is ArmonkBoards, value is ignored.",
              required = false,
              schema = new Schema(implementation=classOf[String]),
          ),
          new Parameter(
              name = "boards",
              in = ParameterIn.QUERY,
              allowEmptyValue = false,
              description = "If present, indicates which boards to use, example values: StandardBoards, ArmonkBoards",
              required = false,
              schema = new Schema(implementation=classOf[String]),
          ),
          new Parameter(
              name = "movements",
              in = ParameterIn.QUERY,
              allowEmptyValue = false,
              description = "If present, indicates which movements to use, example values: Howell3TableNoRelay, Mitchell3Table, Howell2Table5Teams, Armonk2Tables",
              required = false,
              schema = new Schema(implementation=classOf[String]),
          ),
      ),
      requestBody = new RequestBody(
          description = "duplicate results to create",
          content = Array(
              new Content(
                  mediaType = "application/json",
                  schema = new Schema(
                      implementation = classOf[MatchDuplicateResult]
                  )
              )
          )
      ),
      responses = Array(
          new ApiResponse(
              responseCode = "201",
              description = "The created duplicate result's JSON",
              content = Array(
                  new Content(
                      mediaType = "application/json",
                      schema = new Schema( implementation=classOf[MatchDuplicateResult] )
                  )
              ),
              headers = Array(
                  new Header(
                      name="Location",
                      description="The URL of the newly created resource",
                      schema = new Schema( implementation=classOf[String] )
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
  def xxxpostDuplicateResult() = {}
  val postDuplicateResult =
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
  @PUT
  @Operation(
      summary = "Update a duplicate result",
      operationId = "updateDuplicateResult",
      parameters = Array(
          new Parameter(
              allowEmptyValue=false,
              description="ID of the match to update",
              in=ParameterIn.PATH,
              name="matchId",
              required=true,
              schema=new Schema(`type`="string")
          )
      ),
      requestBody = new RequestBody(
          description = "The updated duplicate Match",
          content = Array(
              new Content(
                  mediaType = "application/json",
                  schema = new Schema(
                      implementation = classOf[MatchDuplicateResult]
                  )
              )
          )
      ),
      responses = Array(
          new ApiResponse(
              responseCode = "204",
              description = "Duplicate result updated",
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
  def xxxputDuplicateResult() = {}
  val putDuplicateResult =
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
  @DELETE
  @Operation(
      summary = "Delete a match by ID",
      operationId = "deleteDuplicateResultById",
      parameters = Array(
          new Parameter(
              allowEmptyValue=false,
              description="ID of the match to delete",
              in=ParameterIn.PATH,
              name="matchId",
              required=true,
              schema=new Schema(`type`="string")
          )
      ),
      responses = Array(
          new ApiResponse(
              responseCode = "204",
              description = "DuplicateResult match deleted.",
          )
      )
  )
  def xxxdeleteDuplicateResult() = {}
  val deleteDuplicateResult = path( """[a-zA-Z0-9]+""".r ) { id => {
    delete {
        resourceDelete( store.select(id).delete() )
    } }
  }
}
