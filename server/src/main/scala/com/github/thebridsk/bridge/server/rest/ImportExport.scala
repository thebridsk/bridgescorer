package com.github.thebridsk.bridge.server.rest

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import javax.ws.rs.Path
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.server.backend.BridgeService
import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.stream.scaladsl.StreamConverters
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.utilities.logging.Logger
import scala.util.Success
import scala.util.Failure
import akka.http.scaladsl.server.Directive.addByNameNullaryApply
import akka.http.scaladsl.server.Directive.addDirectiveApply
import akka.http.scaladsl.server.directives.FileInfo
import java.io.{File => JFile}
import scala.reflect.io.Directory
import scala.reflect.io.File
import akka.http.scaladsl.model.headers.`Content-Disposition`
import scala.concurrent.Future
import akka.http.scaladsl.model.headers.ContentDispositionTypes
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.GET
import javax.ws.rs.POST
import com.github.thebridsk.bridge.server.backend.ImportStore.importStoreDotExtension
import com.github.thebridsk.bridge.data.ImportStoreConstants
import com.github.thebridsk.bridge.data.ImportStoreData
import play.api.libs.json.Writes
import com.github.thebridsk.bridge.data.rest.JsonSupport
import io.swagger.v3.oas.annotations.media.Encoding
import akka.http.scaladsl.server.Route

object ImportExport {
  val log: Logger = Logger[ImportExport]()

  case class MultipartFile(
      @Schema(
        `type` = "string",
        format = "binary",
        description =
          "Bridge store file, must have an extension of '.bridgestore' or '.zip'"
      )
      zip: String
  )

}

@Tags(Array(new Tag(name = "Server")))
//@Api( tags = Array("Server"),
//      description = "Import/Export operations.", protocols="http, https")
trait ImportExport {
  import ImportExport._

  val restService: BridgeService

  val diagnosticDir: Option[Directory] = None

  lazy val importExportRoute: Route = {
    exportStore ~ importStore ~ diagnostics
  }

  @Path("/export")
  @GET
  @Operation(
    summary = "Export matches",
    description =
      "Export matches, returns a bridge store file with the matches.  This file can be used for import.",
    operationId = "exportStore",
    parameters = Array(
      new Parameter(
        allowEmptyValue = true,
        description =
          "If present, the Ids of the items to export.  A comma separated list.  If omitted, all are exported.",
        example = "M1,M2",
        in = ParameterIn.QUERY,
        name = "filter",
        required = false,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The exported bridge store file.",
        content = Array(
          // new Content(
          //   mediaType = "application/zip",
          //   schema = new Schema(`type` = "string", format = "binary")
          // ),
          new Content(
            mediaType = "application/octet-stream",
            schema = new Schema(`type` = "string", format = "binary")
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
  def xxxexportStore(): Unit = {}
  val exportStore: Route = get {
    path("export") {
      parameter("filter".?) { (filter) =>
        val filt = filter.map { f =>
          f.split("""\s*,\s*""").toList
        }
        log.fine(s"starting to export bridge store with Ids ${filt}")
        val byteSource: Source[ByteString, Unit] = StreamConverters
          .asOutputStream()
          .mapMaterializedValue { os =>
            restService.doExport(os, filt).onComplete { tr =>
              tr match {
                case Success(Right(list)) =>
                  try {
                    os.flush()
                    os.close()
                    log.fine(
                      s"Successfully export bridge store with Ids ${list}"
                    )
                  } catch {
                    case x: Exception =>
                      log.warning(
                        s"Exception closing stream for exporting bridge store with Ids ${filt}",
                        x
                      )
                  }
                case Success(Left((statusCode, msg))) =>
                  log.warning(
                    s"Error exporting bridge store with Ids ${filt}: ${statusCode} ${msg.msg}"
                  )
                  os.close()
                case Failure(err) =>
                  log.warning(
                    s"Failure exporting bridge store with Ids ${filt}",
                    err
                  )
                  os.close()
              }
            }
          }
        complete(
          HttpResponse(
            entity =
              HttpEntity(MediaTypes.`application/octet-stream`, byteSource),
            headers = `Content-Disposition`(
              ContentDispositionTypes.attachment,
              Map(
                "filename" -> s"BridgeScorerExport.${ImportStoreConstants.importStoreFileExtension}"
              )
            ) :: Nil
          )
        )
      }
    }
  }

  lazy val tempDir: Directory =
    Directory.makeTemp("tempImportStore", ".dir", null)

  def tempDestination(fileInfo: FileInfo): JFile = {
    val fn = fileInfo.fileName
    if (fn.endsWith(".zip") || fn.endsWith(importStoreDotExtension))
      new JFile(tempDir.toString(), fileInfo.fileName)
    else throw new IllegalArgumentException(s"Filename not valid: $fn")
  }

  def result[T](
      statuscode: StatusCode,
      t: T
  )(implicit
      writer: Writes[T]
  ): HttpResponse = {
    HttpResponse(
      statuscode,
      entity =
        HttpEntity(ContentTypes.`application/json`, JsonSupport.writeJson(t))
    )
  }

  import UtilsPlayJson._

  @Path("/import")
  @POST
  @Operation(
    summary = "Import matches",
    description =
      "Import matches from a bridge store file.  This bridge store file is created by the export api call.",
    operationId = "importStore",
    // See https://community.smartbear.com/t5/Swagger-Open-Source-Tools/How-to-swagger-annotate-multipart-form-data-with-resteasy/td-p/178776
    requestBody = new RequestBody(
      description = "Properties:",
      required = true,
      content = Array(
        new Content(
          mediaType = "multipart/form-data",
          schema = new Schema(implementation = classOf[MultipartFile]),
          encoding = Array(
            new Encoding(
              name = "zip",
              contentType = "application/octet-stream"
            )
          )
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description =
          "The bridge store file was imported.  Summary information is returned.",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[ImportStoreData])
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
  def xxximportStore(): Unit = {}
  val importStore: Route = post {
    path("import") {
      restService.importStore
        .map { is =>
          storeUploadedFiles("zip", tempDestination) { files =>
            if (files.length != 1) {
              complete(
                StatusCodes.BadRequest,
                RestMessage("Only one store can be imported at a time")
              )
            } else {
              // one bridgestore zip file
              val (metadata, file) = files.head
              val f = File(file.toString())

              complete(
                if (
                  metadata.fileName.endsWith(".zip") || metadata.fileName
                    .endsWith(importStoreDotExtension)
                ) {
                  val rr =
                    is.create(metadata.fileName, f).flatMap { tr =>
                      tr match {
                        case Right(importedstore) =>
                          file.delete()
                          log.fine(
                            s"imported zipfile ${metadata.fileName}."
                          )
                          importedstore.importStoreData
                            .map(is => result(StatusCodes.OK, is))
                        case Left((statusCode, msg)) =>
                          file.delete()
                          log.warning(
                            s"Error importing bridge store ${metadata.fileName}: ${statusCode} ${msg.msg}"
                          )
                          Future(
                            result(
                              statusCode,
                              RestMessage(
                                s"Error importing bridge store ${metadata.fileName}: ${msg.msg}"
                              )
                            )
                          )
                      }
                    }
                  rr
                } else {
                  Future(
                    result(
                      StatusCodes.BadRequest,
                      RestMessage("Only bridge store files are accepted")
                    )
                  )
                }
              )
            }
          }
        }
        .getOrElse(
          complete(
            StatusCodes.BadRequest,
            RestMessage("Import store is not defined")
          )
        )
    }
  }

  @Path("/diagnostics")
  @GET
  @Operation(
    summary = "Get diagnostic information",
    description =
      "Export diagnostic information from the server.  This consists of the logs and store.",
    operationId = "diagnostics",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The diagnostic information as a zip file.",
        content = Array(
          new Content(
            mediaType = "application/octet-stream",
            schema = new Schema(`type` = "string", format = "binary")
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
  def xxxdiagnostics(): Unit = {}
  val diagnostics: Route = get {
    path("diagnostics") {
      log.fine(s"starting to export of diagnostic information")
      val byteSource: Source[ByteString, Unit] = StreamConverters
        .asOutputStream()
        .mapMaterializedValue { os =>
          restService.doExport(os, None, true).onComplete { tr =>
            tr match {
              case Success(Right(list)) =>
                try {
                  os.flush()
                  os.close()
                  log.fine(
                    "Successfully export bridge store diagnostics"
                  )
                } catch {
                  case x: Exception =>
                    log.warning(
                      "Exception closing stream for exporting bridge store diagnostics",
                      x
                    )
                }
              case Success(Left((statusCode, msg))) =>
                log.warning(
                  s"Error exporting bridge store diagnostics: ${statusCode} ${msg.msg}"
                )
                os.close()
              case Failure(err) =>
                log.warning(
                  "Failure exporting bridge store diagnostics",
                  err
                )
                os.close()
            }
          }
        }
      complete(
        HttpResponse(
          entity =
            HttpEntity(MediaTypes.`application/octet-stream`, byteSource),
          headers = `Content-Disposition`(
            ContentDispositionTypes.attachment,
            Map(
              "filename" -> s"BridgeScorerDiagnostics.${ImportStoreConstants.importStoreFileExtension}"
            )
          ) :: Nil
        )
      )
    }
  }
}
