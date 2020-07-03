package com.github.thebridsk.bridge.server.rest

import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.Id
import akka.event.Logging
import akka.event.Logging._
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import com.github.thebridsk.bridge.server.util.HasActorSystem
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import javax.ws.rs.Path
import com.github.thebridsk.bridge.data.RestMessage
import akka.http.scaladsl.model.headers.Location
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.server.backend.resource.Resources
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
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.DELETE
import com.github.thebridsk.bridge.data.DuplicatePicture
import akka.http.scaladsl.model.StatusCodes
import com.github.thebridsk.bridge.server.backend.resource.Store
import akka.http.scaladsl.server.Route
import scala.util.Success
import scala.util.Failure
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.Accept
import java.io.InputStream
import akka.util.ByteString
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes
import akka.stream.scaladsl.Source
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.MediaType
import com.github.thebridsk.bridge.server.backend.resource.MetaData.MetaDataFile
import akka.http.scaladsl.server.directives.FileInfo
import java.io.{ File => JFile }
import scala.reflect.io.Directory
import scala.reflect.io.File
import io.swagger.v3.oas.annotations.media.Encoding
import com.github.thebridsk.bridge.server.backend.resource.ChangeContext
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateDuplicatePicture

object RestNestedPicture {
  val log = Logger[RestNestedPicture]()

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
class RestNestedPicture( store: Store[Id.MatchDuplicate,MatchDuplicate], parent: RestDuplicate ) {

  import UtilsPlayJson._

  val resName = parent.resName
  val resNameWithSlash = if (resName.startsWith("/")) resName else "/" + resName

  lazy val nestedPictureHands = new RestNestedPictureHand(store,this)

  /**
    * spray route for all the methods on this resource
    */
  @Hidden
  def route(
      implicit @Parameter(hidden = true) dupid: Id.MatchDuplicate
  ) = pathPrefix("pictures") {
    logRequestResult("RestNestedPicture.route", DebugLevel) {
      nestedRoute ~ getPictures ~ deletePicture
    }
  }

  def nestedRoute(
    implicit @Parameter(hidden = true) dupId: Id.MatchDuplicate
  ) = logRequest("RestNestedPicture.nestedRoute", DebugLevel) {
    pathPrefix("""[a-zA-Z0-9]+""".r) { boardId =>
      nestedPictureHands.route(dupId,boardId)
    }
  }

  def getAllPictures( dupId: String ) = {
    store.metaData.listFilesFilter(dupId) { f => RestNestedPictureHand.getPartsMetadataFile(f).isDefined }.map { ri =>
      ri match {
        case Right(it) =>
          val r = it.flatMap { mdf =>
            val parts = RestNestedPictureHand.getPartsMetadataFile(mdf).get
            DuplicatePicture(parts.boardId,parts.handId,RestNestedPictureHand.getUrlOfPicture(resNameWithSlash,dupId,parts.boardId,parts.handId))::Nil
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
      ),
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
                description = "Information about a picture of the cards for the board."
              ),
              arraySchema =
                new Schema(description = "All the pictures from the match.")
            )
          )
        )
      )
    )
  )
  def xxxgetPictures = {}
  def getPictures(
      implicit @Parameter(hidden = true) dupId: Id.MatchDuplicate
  ) = pathEndOrSingleSlash {
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
          complete((StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}"))
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
      new ApiResponse(responseCode = "204", description = "Picture deleted.")
    )
  )
  def xxxdeletePicture = {}
  def deletePicture(
      implicit @Parameter(hidden = true) dupId: Id.MatchDuplicate
  ) = delete {
    path("""[a-zA-Z0-9]+""".r) { boardid =>
      val changeContext = ChangeContext()
      val fut = store.metaData.listFiles(dupId).map { rimdf =>
        rimdf match {
          case Right(imdf) =>
            val deletes = imdf.filter { mdf =>
              val isimage = RestNestedPictureHand.isImageFilename(mdf,boardid)
              log.fine(s"RestNestedPicture.delete(${mdf}): isimage=${isimage}")
              isimage
            }.map { mdf =>
              store.metaData.delete(dupId,mdf).transform { tr =>
                tr match {
                  case Success(r) =>
                    r match {
                      case Right(value) =>
                        log.fine(s"RestNestedPicture.delete(${mdf}): deleted")
                        val parts = RestNestedPictureHand.getPartsMetadataFile(mdf)
                        parts.foreach( p => changeContext.update(
                          UpdateDuplicatePicture(
                            dupid = dupId,
                            boardid = boardid,
                            handId = p.handId,
                            picture = None
                          )
                        ))
                      case Left(ex) =>
                        log.warning(s"RestNestedPicture.delete(${mdf}): error deleting ${ex}")
                    }
                  case Failure(ex) =>
                    log.warning(s"Error deleting image file ${mdf} for board: ${ex}",ex)
                }
                tr
              }
            }.toList
            onComplete(Future.foldLeft(deletes)(Result.unit) { (ac,v) => ac}) {
              case Success(value) =>
                log.fine(s"RestNestedPicture.delete(${boardid}): deleted image")
                store.notify(changeContext)
                complete(StatusCodes.NoContent)
              case Failure(ex) =>
                log.warning(s"Error deleting image file for board: ${ex}",ex)
                complete(StatusCodes.InternalServerError, RestMessage("Internal server error"))
            }
          case Left((code,msg)) =>
            complete(code,msg)
        }
      }
      onComplete(fut) {
        case Success(value) =>
          value
        case Failure(ex) =>
          log.warning(s"Error deleting image file for board",ex)
          complete(StatusCodes.InternalServerError, RestMessage("Internal server error"))
      }
    }
  }
}
