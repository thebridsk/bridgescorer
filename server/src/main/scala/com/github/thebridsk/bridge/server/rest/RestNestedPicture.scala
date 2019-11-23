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

object RestNestedPicture {
  val log = Logger[RestNestedPicture]

  val patternImageFile = """Image\.(.+?)(\.jpg)""".r

  /**
   * @return a Tuple2, the first entry is the the name without extension, and the second is the extension.
   */
  def getPartsMetadataFile( mdf: MetaDataFile ): Option[(String,String)] = {
    mdf match {
      case patternImageFile(id,ext) => Some((id,ext))
      case _ => None
    }
  }

  /**
   * @param boardId
   * @param ext the extension of the image file without the "."
   */
  def makeImageFilename( boardId: String, ext: String) = {
    s"Image.${boardId}.${ext}"
  }

  def isImageFilename( file: MetaDataFile ) = {
    file.startsWith("Image.")
  }

  def isImageFilename( file: MetaDataFile, boardId: String ) = {
    file.startsWith(s"Image.${boardId}.")
  }

  def isImageExtension( file: String ) = {
    val ext = File(file).extension
    val isImage = ext=="jpg"
    (isImage,ext)
  }

  def extractAccept: HttpHeader => Option[Accept] = {
    case h: `Accept` => Some(h)
    case x         => None
  }

  def acceptHeader( route: Option[Accept] => Route ): Route = {
    optionalHeaderValue(extractAccept) { a =>
      route(a)
    }
  }

  lazy val tempDir = Directory.makeTemp("tempImportStore", ".dir", null)

  def tempDestination(boardId: String)( fileInfo: FileInfo): JFile = {
    val fn = fileInfo.fileName
    val (isImage,ext) = isImageExtension(fn)
    if (isImage) {
      val mdf = makeImageFilename(boardId,ext)
      new JFile(tempDir.toString(), mdf)
    }
    else throw new IllegalArgumentException(s"Filename not valid: $fn")
  }

  case class MultipartFile(
    @Schema(`type` = "string", format = "binary", description = "picture file")
    picture: String
  )

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

  /**
    * spray route for all the methods on this resource
    */
  @Hidden
  def route(
      implicit @Parameter(hidden = true) dupid: Id.MatchDuplicate
  ) = pathPrefix("pictures") {
    logRequestResult("pictureroute", DebugLevel) {
      getPicture ~ getPictures ~ putPicture ~ deletePicture
    }
  }

  @Hidden
  def resourceUrl(id: String)( nestedroute: String => Route ): Route = {
    extractScheme { scheme =>
      headerValue(Service.extractHostPort) { host =>
        val rn = if (resName.startsWith("/")) resName else "/" + resName
        val hn =
          if (host.endsWith("/")) host.substring(0, host.length - 1)
          else host
        val ret = s"${scheme}://${hn}${rn}/${id}/pictures"
        utilslog.fine(s"resourceUrl is $ret")
        nestedroute(ret)
      } ~
        extractRequest { request =>
          val h = request.uri.authority.host.toString()
          val p = request.uri.authority.port
          val sp =
            if (scheme == "https" && p == 443) h
            else if (scheme == "http" && p == 80) h
            else s"${h}:${p}"
          val rn =
            if (resName.startsWith("/")) resName else "/" + resName
          val ret = s"${scheme}://${sp}${rn}/${id}/pictures"
          utilslog.fine(s"resourceUrl is $ret")
          nestedroute(ret)
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
      resourceUrl(dupId) { urlprefix =>
        val f = store.metaData.listFilesFilter(dupId) { f => getPartsMetadataFile(f).isDefined }.map { ri =>
          ri match {
            case Right(it) =>
              val r = it.flatMap { mdf =>
                mdf match {
                  case patternImageFile(id,ext) =>
                    DuplicatePicture(id, s"${urlprefix}/${id}")::Nil
                  case _ =>
                    Nil
                }
              }
              Right(r)
            case Left(err) =>
              Left(err)
          }
        }
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
  }

  @Path("/{boardId}")
  @GET
  @Operation(
    summary = "Get the picture information or image by ID",
    operationId = "getPictureById",
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
        description = "ID of the picture that information is wanted for",
        in = ParameterIn.PATH,
        name = "boardId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The picture information object, as a JSON object",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[DuplicatePicture])
          ),
          new Content(
            mediaType = "image/*",
            schema = new Schema(
              `type` = "string",
              format = "binary",
              description = "The picture of the cards for the board."
            )
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
  def xxxgetPicture = {}
  def getPicture(
      implicit @Parameter(hidden = true) dupId: Id.MatchDuplicate
  ) = logRequest("getPicture", DebugLevel) {
    get {
      path("""[a-zA-Z0-9]+""".r) { bid =>
        resourceUrl(dupId) { urlprefix =>
          val fprefix = bid+"."
          val fut = store.metaData.listFilesFilter(dupId) { f =>
            isImageFilename(f) && getPartsMetadataFile(f).map { e => e._1==bid }.getOrElse(false)
          }.map { ri =>
            ri match {
              case Right(it) =>
                val filelist = it.toList
                Right(filelist)
              case Left(err) =>
                Left(err)
            }
          }
          onComplete(fut) {
            case Success(r) =>
              r match {
                case Right(l) =>
                  l.headOption.map { mdf =>
                    val (mdfid, mdfext) = getPartsMetadataFile(mdf).get
                    acceptHeader { optionalAccept =>
                      optionalAccept.flatMap { acc =>
                        acc.mediaRanges.find( mr => mr.isImage )
                      }.map { mr =>
                        // image was requested
                        val fr =
                        store.metaData.read(dupId,mdf).map { ris =>
                          ris match {
                            case Right(is) =>
                              val itbs = iterator(is,0)
                              val byteSource: Source[ByteString,Any] = Source.fromIterator( ()=>itbs )
                              val mediatype = MediaTypes.forExtension(mdfext).asInstanceOf[MediaType.Binary]
                              val contenttype = ContentType(mediatype)
                              val entity: HttpEntity.Chunked = HttpEntity(contenttype, byteSource)
                              complete(
                                HttpResponse(
                                  entity = entity
                                )
                              )
                            case Left((code,msg)) =>
                              complete(code,msg)
                          }
                        }
                        onComplete(fr) {
                          case Success(r) => r
                          case Failure(ex) =>
                            log.warning(s"Error getting image file for board",ex)
                            complete(StatusCodes.InternalServerError, RestMessage("Internal server error"))
                        }
                      }.getOrElse {
                        val mdf = l.head
                        val r =
                        mdf match {
                          case patternImageFile(id,ext) =>
                            complete(StatusCodes.OK, DuplicatePicture(id, s"${urlprefix}/${id}"))
                          case _ =>
                            complete(StatusCodes.NotFound)
                        }
                        r
                      }
                    }
                  }.getOrElse {
                    complete(StatusCodes.NotFound)
                  }
                case Left(r) => complete(r._1, r._2)
              }
            case Failure(ex) =>
              complete((StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}"))
          }
        }
      }
    }
  }

  /**
   *
   */
  def iterator( in: InputStream, maxlen: Int ): Iterator[ByteString] = {
    new Iterator[ByteString] {

      private var forceEOF = false

      /** Tests whether this iterator can provide another element.
       *
       *  @return  `true` if a subsequent call to `next` will yield an element,
       *           `false` otherwise.
       *  @note    Reuse: $preservesIterator
       */
      def hasNext: Boolean = forceEOF || in.available > 0;

      /** Produces the next element of this iterator.
       *
       *  @return  the next element of this iterator, if `hasNext` is `true`,
       *           undefined behavior otherwise.
       *  @note    Reuse: $preservesIterator
       */
      def next(): ByteString = {
        val buf = new Array[Byte](100000)
        val l = in.read(buf)
        if (l > 0) {
          ByteString.fromArrayUnsafe(buf,0,l)
        } else {
          forceEOF = true
          ByteString("")
        }

      } // ByteString("")

    }
  }

  @Path("/{boardId}")
  @PUT
  @Operation(
    summary = "Create/Update a picture of cards for a board",
    operationId = "updatePicture",
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
    requestBody = new RequestBody(
      description = "picture to update",
      content = Array(
        new Content(
          mediaType = "multipart/form-data",
          schema = new Schema(implementation = classOf[MultipartFile]),
          encoding = Array(
            new Encoding(
              name = "picture",
              contentType = "image/*"
            )
          )
        )
      )
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "Picture updated"),
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
  def xxxputPicture = {}
  def putPicture(
      implicit @Parameter(hidden = true) dupId: Id.MatchDuplicate
  ) = logRequest("putPicture", DebugLevel) {
    logResult("putPicture", DebugLevel) {
      put {
        path("""[a-zA-Z0-9]+""".r) { bid =>
          storeUploadedFiles("picture", tempDestination(bid)) { files =>
            if (files.length != 1) {
              complete(
                StatusCodes.BadRequest,
                RestMessage("Only one image can be imported at a time")
              )
            } else {
              // one bridgestore zip file
              val (metadata, file) = files.head
              val f = File(file.toString())
              val mdf = f.name

              val fut = store.metaData.listFiles(dupId).map { ri =>
                ri match {
                  case Right(it) =>
                    val filelist = it.filter( f => isImageFilename(f,bid) ).toList
                    val cur = filelist.find { f => f==mdf }
                    Right((filelist,cur))
                  case Left(err) =>
                    Left(err)
                }
              }
              onComplete(fut) {
                case Success(rf) =>
                  rf match {
                    case Right((filelist,omdf)) =>
                      val deletes = if (filelist.length>1 || omdf.map(f => f!=mdf).getOrElse(false)) {
                        val dels = filelist.filter( f => f!=mdf ).map( f => store.metaData.delete(dupId,f) )
                        Future.foldLeft(dels)(Result.unit) { (ac,v) => ac}
                      } else {
                        Future(Result.unit)
                      }
                      onComplete(deletes) {
                        case Success(runit) =>
                          val write = store.metaData.write(dupId,new File(file),mdf).map { rw =>
                            rw match {
                              case Right(unit) =>
                                complete(StatusCodes.NoContent)
                              case Left((code,msg)) =>
                                complete(code,msg)
                            }
                          }
                          onComplete(write) {
                            case Success(r) => r
                            case Failure(ex) =>
                            log.warning(s"Error writing image file for board",ex)
                            complete(StatusCodes.InternalServerError, RestMessage("Internal server error"))
                          }
                        case Failure(ex) =>
                          log.warning(s"Error deleting old image file for board",ex)
                          complete(StatusCodes.InternalServerError, RestMessage("Internal server error"))
                      }
                    case Left((code,msg)) =>
                      complete(code,msg)
                  }
                case Failure(ex) =>
                  log.warning(s"Error listing current image file for board",ex)
                  complete(StatusCodes.InternalServerError, RestMessage("Internal server error"))
              }
            }
          }
        }
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
      val fut = store.metaData.listFiles(dupId).map { rimdf =>
        rimdf match {
          case Right(imdf) =>
            val deletes = imdf.filter { mdf => isImageFilename(mdf,boardid) }.map { mdf =>
              store.metaData.delete(dupId,mdf)
            }.toList
            onComplete(Future.foldLeft(deletes)(Result.unit) { (ac,v) => ac}) {
              case Success(value) =>
                complete(StatusCodes.NoContent)
              case Failure(ex) =>
                log.warning(s"Error deleting image file for board",ex)
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
