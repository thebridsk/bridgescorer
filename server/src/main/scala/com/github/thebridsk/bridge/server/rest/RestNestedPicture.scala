package com.github.thebridsk.bridge.server.rest

import com.github.thebridsk.bridge.data.MatchDuplicate
import akka.event.Logging._
import akka.http.scaladsl.server.Directives._
import jakarta.ws.rs.Path
import com.github.thebridsk.bridge.data.RestMessage
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.github.thebridsk.bridge.server.backend.resource.Result
import com.github.thebridsk.utilities.logging.Logger
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.GET
import jakarta.ws.rs.DELETE
import com.github.thebridsk.bridge.data.DuplicatePicture
import akka.http.scaladsl.model.StatusCodes
import com.github.thebridsk.bridge.server.backend.resource.Store
import scala.util.Success
import scala.util.Failure
import com.github.thebridsk.bridge.server.backend.resource.ChangeContext
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateDuplicatePicture
import com.github.thebridsk.bridge.data.Board
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.server.Route

object RestNestedPicture {
  val log: Logger = Logger[RestNestedPicture]()

}

import RestNestedPicture._

/**
  * Rest API implementation for the hand resource.
  * <p>
  * The REST API and all the methods are documented using
  * swagger annotations.
  */
@Path("/rest/duplicates/{dupId}/pictures")
@Tags(Array(new Tag(name = "Duplicate")))
class RestNestedPicture(
    store: Store[MatchDuplicate.Id, MatchDuplicate],
    parent: RestDuplicate
) {

  import UtilsPlayJson._

  val resName = parent.resName
  val resNameWithSlash: String =
    if (resName.startsWith("/")) resName else "/" + resName

  lazy val nestedPictureHands = new RestNestedPictureHand(store, this)

  /**
    * spray route for all the methods on this resource
    */
  @Hidden
  def route(implicit
      @Parameter(hidden = true) dupid: MatchDuplicate.Id
  ): Route =
    pathPrefix("pictures") {
      logRequestResult("RestNestedPicture.route", DebugLevel) {
        nestedRoute ~ getPictures ~ deletePicture
      }
    }

  def nestedRoute(implicit
      @Parameter(hidden = true) dupId: MatchDuplicate.Id
  ): Route =
    logRequest("RestNestedPicture.nestedRoute", DebugLevel) {
      pathPrefix("""[a-zA-Z0-9]+""".r) { boardId =>
        nestedPictureHands.route(dupId, Board.id(boardId))
      }
    }

  def getAllPictures(
      dupId: MatchDuplicate.Id
  ): Future[Either[(StatusCode, RestMessage), Iterator[DuplicatePicture]]] = {
    store.metaData
      .listFilesFilter(dupId) { f =>
        RestNestedPictureHand.getPartsMetadataFile(f).isDefined
      }
      .map { ri =>
        ri match {
          case Right(it) =>
            val r = it.flatMap { mdf =>
              val parts = RestNestedPictureHand.getPartsMetadataFile(mdf).get
              DuplicatePicture(
                parts.boardId,
                parts.handId,
                RestNestedPictureHand.getUrlOfPicture(
                  resNameWithSlash,
                  dupId,
                  parts.boardId,
                  parts.handId
                )
              ) :: Nil
            }
            Right(r)
          case Left(err) =>
            Left(err)
        }
      }
  }

  @GET
  @Operation(
    summary = "Get all picture URLs",
    description = "Returns a list of picture information objects",
    operationId = "getPictures",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description =
          "ID of the duplicate that contains the pictures to manipulate",
        in = ParameterIn.PATH,
        name = "dupId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "A list of picture information objects, as a JSON array",
        content = Array(
          new Content(
            mediaType = "application/json",
            array = new ArraySchema(
              minItems = 0,
              uniqueItems = true,
              schema = new Schema(
                implementation = classOf[DuplicatePicture],
                description =
                  "Information about a picture of the cards for the board."
              ),
              arraySchema =
                new Schema(description = "All the pictures from the match.")
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
  def xxxgetPictures: Unit = {}
  def getPictures(implicit
      @Parameter(hidden = true) dupId: MatchDuplicate.Id
  ): Route =
    pathEndOrSingleSlash {
      get {
        val f = getAllPictures(dupId)
        onComplete(f) {
          case Success(r) =>
            r match {
              case Right(l) =>
                val rl = l.toList
                complete(StatusCodes.OK, rl)
              case Left(r) => complete(r._1, r._2)
            }
          case Failure(ex) =>
            complete(
              (
                StatusCodes.InternalServerError,
                s"An error occurred: ${ex.getMessage}"
              )
            )
        }
      }
    }

  @Path("/{boardId}")
  @DELETE
  @Operation(
    summary = "Delete a picture by ID",
    operationId = "deletePictureById",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description =
          "ID of the duplicate that contains the pictures to manipulate",
        in = ParameterIn.PATH,
        name = "dupId",
        required = true,
        schema = new Schema(`type` = "string")
      ),
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the picture to manipulate",
        in = ParameterIn.PATH,
        name = "boardId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "Picture deleted."),
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
  def xxxdeletePicture: Unit = {}
  def deletePicture(implicit
      @Parameter(hidden = true) dupId: MatchDuplicate.Id
  ): Route =
    delete {
      path("""[a-zA-Z0-9]+""".r) { sboardid =>
        val boardid = Board.id(sboardid)
        val changeContext = ChangeContext()
        val fut = store.metaData.listFiles(dupId).map { rimdf =>
          rimdf match {
            case Right(imdf) =>
              val deletes = imdf
                .filter { mdf =>
                  val isimage =
                    RestNestedPictureHand.isImageFilename(mdf, boardid)
                  log.fine(
                    s"RestNestedPicture.delete(${mdf}): isimage=${isimage}"
                  )
                  isimage
                }
                .map { mdf =>
                  store.metaData.delete(dupId, mdf).transform { tr =>
                    tr match {
                      case Success(r) =>
                        r match {
                          case Right(value) =>
                            log.fine(
                              s"RestNestedPicture.delete(${mdf}): deleted"
                            )
                            val parts =
                              RestNestedPictureHand.getPartsMetadataFile(mdf)
                            parts.foreach(p =>
                              changeContext.update(
                                UpdateDuplicatePicture(
                                  dupid = dupId,
                                  boardid = boardid,
                                  handId = p.handId,
                                  picture = None
                                )
                              )
                            )
                          case Left(ex) =>
                            log.warning(
                              s"RestNestedPicture.delete(${mdf}): error deleting ${ex}"
                            )
                        }
                      case Failure(ex) =>
                        log.warning(
                          s"Error deleting image file ${mdf} for board: ${ex}",
                          ex
                        )
                    }
                    tr
                  }
                }
                .toList
              onComplete(Future.foldLeft(deletes)(Result.unit) { (ac, v) =>
                ac
              }) {
                case Success(value) =>
                  log.fine(
                    s"RestNestedPicture.delete(${boardid}): deleted image"
                  )
                  store.notify(changeContext)
                  complete(StatusCodes.NoContent)
                case Failure(ex) =>
                  log.warning(s"Error deleting image file for board: ${ex}", ex)
                  complete(
                    StatusCodes.InternalServerError,
                    RestMessage("Internal server error")
                  )
              }
            case Left((code, msg)) =>
              complete(code, msg)
          }
        }
        onComplete(fut) {
          case Success(value) =>
            value
          case Failure(ex) =>
            log.warning(s"Error deleting image file for board", ex)
            complete(
              StatusCodes.InternalServerError,
              RestMessage("Internal server error")
            )
        }
      }
    }
}
