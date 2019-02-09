package com.example.rest

import akka.http.scaladsl.server._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import io.swagger.annotations._
import javax.ws.rs.Path
import com.example.data.RestMessage
import com.example.backend.BridgeService
import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.stream.scaladsl.StreamConverters
import scala.concurrent.ExecutionContext.Implicits.global
import com.example.backend.resource.Implicits._
import utils.logging.Logger
import scala.util.Success
import scala.util.Failure
import akka.http.scaladsl.marshalling.ToResponseMarshallable.apply
import akka.http.scaladsl.model.ContentType.apply
import akka.http.scaladsl.server.Directive.addByNameNullaryApply
import akka.http.scaladsl.server.Directive.addDirectiveApply
import java.util.UUID
import akka.http.scaladsl.server.directives.FileInfo
import java.io.{ File => JFile }
import scala.reflect.io.Directory
import scala.reflect.io.File
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.headers.`Content-Disposition`
import java.io.BufferedOutputStream
import java.util.zip.ZipOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.io.FileInputStream
import java.nio.file.Files
import java.io.InputStream
import java.io.OutputStream
import scala.concurrent.Future
import akka.http.scaladsl.model.headers.ContentDispositionTypes
import com.example.version.VersionServer
import com.example.version.VersionShared
import com.example.utilities.version.VersionUtilities

object ImportExport {
  val log = Logger[ImportExport]
}

@Path( "" )
@Api( tags = Array("Server"),
      description = "Import/Export operations.", protocols="http, https")
trait ImportExport {
  import ImportExport._

  val restService: BridgeService

  val diagnosticDir: Option[Directory] = None

  lazy val importExportRoute = {
    exportStore ~ importStore ~ diagnostics
  }

  @Path( "export" )
  @ApiOperation(
      value = "Export a bridge store",
      notes = "",
      response=classOf[Array[Byte]],
      nickname = "exportStore",
      httpMethod = "GET",
      code=200,
      produces="application/zip"
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
        name = "filter",
        value = "If present, the Ids of the items to export.  A comma separated list.  If omitted, all are exported.",
        required = false,
        dataType = "string",
        paramType = "query"
    )
  ))
  @ApiResponses(Array(
    new ApiResponse(
        code = 200,
        message = "The exported data store as a zip file.",
        response=classOf[Array[Byte]],
    ),
    new ApiResponse(
        code = 400,
        message = "Bad request",
        response=classOf[RestMessage]
    )
  ))
  def exportStore = get {
    path( "export" ) {
      parameter( 'filter.? ) { (filter) =>
        val filt = filter.map{ f =>
          f.split("""\s*,\s*""").toList
        }
        log.fine(s"starting to export bridge store with Ids ${filt}")
        val byteSource: Source[ByteString, Unit] = StreamConverters.asOutputStream()
                          .mapMaterializedValue { os =>
                            restService.export(os,filt).onComplete { tr =>
                              tr match {
                                case Success(Right(list)) =>
                                  try {
                                    os.flush()
                                    os.close()
                                    log.fine(s"Successfully export bridge store with Ids ${list}")
                                  } catch {
                                    case x: Exception =>
                                      log.warning(s"Exception closing stream for exporting bridge store with Ids ${filt}", x)
                                  }
                                case Success(Left((statusCode,msg))) =>
                                  log.warning(s"Error exporting bridge store with Ids ${filt}: ${statusCode} ${msg.msg}")
                                  os.close()
                                case Failure(err) =>
                                  log.warning(s"Failure exporting bridge store with Ids ${filt}", err)
                                  os.close()
                              }
                            }
                          }
        complete(
            HttpResponse(entity = HttpEntity(
                                    MediaTypes.`application/zip`,
                                    byteSource),
                         headers = `Content-Disposition`( ContentDispositionTypes.attachment,
                                                          Map( "filename" -> "BridgeScorerExport.zip") )::Nil
                        )
        )
      }
    }
  }

  lazy val tempDir = Directory.makeTemp("tempImportStore", ".dir", null)

  def tempDestination(fileInfo: FileInfo): JFile = {
    val fn = fileInfo.fileName
    if (fn.endsWith(".zip")) new JFile( tempDir.toString(), fileInfo.fileName )
    else throw new IllegalArgumentException("Filename not valid")
  }


  def successhtml( url: String, filename: String ) = HttpResponse(
                              StatusCodes.OK,
                              entity=HttpEntity(ContentTypes.`text/html(UTF-8)`,
                                                s"""<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<meta http-equiv="refresh" content="5;url=${url}" />
<title>Import Store</title>
</head>
<body>
<div id="BridgeApp">
<div style="position: fixed; top: 0; left: 0; width: 100%;
            padding-left: 16px; padding-right: 16px;
            display: flex; position: relative; align-items: center;
            color: #fff; background-color: #3f51b5;
            font-size: x-large;
            height: 64px;
           " >
  <h6 style="font-size: 1.25rem; font-family: Arial, sans-serif; font-weight: 500; line-height: 1.6; letter-spacing: 0.0075em;
      " >Bridge ScoreKeeper</h6>
  <span style="padding-left: 30px; font-size: 2.5rem">
      <span class="headerSuitSize" style="color: black;"> &spades;</span>
      <span class="headerSuitSize" style="color: red;"> &hearts;</span>
      <span class="headerSuitSize" style="color: red;"> &diams;</span>
      <span class="headerSuitSize" style="color: black;"> &clubs;</span>
  </span>
</div>
<h1>Successfully imported ${filename}</h1>
<p><a href="${url}">Redirect</a></p>
</div>
</body>
</html>
"""
                              )
                            )

  def failurehtml( url: String, filename: String, error: String, statusCode: StatusCode = StatusCodes.BadRequest ) = HttpResponse(
                              statusCode,
                              entity=HttpEntity(ContentTypes.`text/html(UTF-8)`,
                                                s"""<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Error Import Store</title>
</head>
<body>
<div id="BridgeApp">
<div style="position: fixed; top: 0; left: 0; width: 100%;
            padding-left: 16px; padding-right: 16px;
            display: flex; position: relative; align-items: center;
            color: #fff; background-color: #3f51b5;
            font-size: x-large;
            height: 64px;
           " >
  <h6 style="font-size: 1.25rem; font-family: Arial, sans-serif; font-weight: 500; line-height: 1.6; letter-spacing: 0.0075em;
      " >Bridge ScoreKeeper</h6>
  <span style="padding-left: 30px; font-size: 2.5rem">
      <span class="headerSuitSize" style="color: black;"> &spades;</span>
      <span class="headerSuitSize" style="color: red;"> &hearts;</span>
      <span class="headerSuitSize" style="color: red;"> &diams;</span>
      <span class="headerSuitSize" style="color: black;"> &clubs;</span>
  </span>
</div>
<h1>Error importing ${filename}</h1>
<p>${error}</p>
<p><a href="${url}">Return</a></p>
</div>
</body>
</html>
"""
                              )
                            )

  import UtilsPlayJson._

  @Path( "import" )
  @ApiOperation(
      value = "Import a bridge store",
      notes = "",
      response=classOf[Array[Byte]],
      nickname = "importStore",
      httpMethod = "POST",
      code=200,
      produces="text/html"
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
        name = "url",
        value = "The URL to redirect the successful page to.  Default is \"/\"",
        required = false,
        dataType = "string",
        paramType = "query"
    ),
    new ApiImplicitParam(
        name = "zip",
        value = "The zip file that contains the bridge store.",
        required = false,
        dataType = "file",
        paramType = "form"
    )
  ))
  @ApiResponses(Array(
    new ApiResponse(
        code = 200,
        message = "The bridge store was imported.",
    ),
    new ApiResponse(
        code = 400,
        message = "Bad request",
    )
  ))
  def importStore = post {
    path( "import" ) {
      if (restService.importStore.isDefined) {
        parameter( 'url.? ) { (opturl) =>
          extractScheme { scheme =>
            headerValue(Service.extractHostPort) { host =>
              storeUploadedFiles("zip", tempDestination) { files =>
                val futures = files.map { entry =>

                  val (metadata, file) = entry

                  val is = restService.importStore.get
                  val f = File( file.toString() )
                  val url = opturl.getOrElse("/")

                  val r =
                  if (metadata.fileName.endsWith(".zip")) {
                    val rr =
                    is.create(metadata.fileName, f).transform { tr =>
                      tr match {
                        case Success(Right(_)) =>
                          file.delete()
                          val hn = if (host.endsWith("/")) host.substring(0,host.length-1) else host
                          log.fine( s"imported zipfile ${metadata.fileName}, redirecting to location to ${scheme}://${hn}/" )
                          Success(successhtml( url, metadata.fileName ))
                        case Success(Left((statusCode, msg ))) =>
                          file.delete()
                          log.warning(s"Error importing bridge store ${metadata.fileName}: ${statusCode} ${msg.msg}")
                          Success(failurehtml( url, metadata.fileName, msg.msg ))
                        case Failure(ex) =>
                          file.delete()
                          log.warning(s"Failure importing bridge store ${metadata.fileName}", ex)
                          Success(failurehtml( url, metadata.fileName, s"An error occurred: ${ex.getMessage}", StatusCodes.InternalServerError))
                      }
                    }
                    rr
                  } else {
                    Future.successful( failurehtml( url, metadata.fileName, "Only zip files are accepted", StatusCodes.BadRequest ))
                  }
                  r
                }
                val ret = Future.foldLeft(futures)(HttpResponse(StatusCodes.OK)) { (ac, v) =>
                  if (ac.status == StatusCodes.OK) v
                  else ac
                }
                complete(ret)
              }
            }
          }
        }
      } else {
        complete( StatusCodes.BadRequest, RestMessage("Import store is not defined") )
      }
    }
  }

  @Path( "diagnostics" )
  @ApiOperation(
      value = "Export diagnostic information from the server.  This consists of the logs and store.",
      notes = "",
      response=classOf[Array[Byte]],
      nickname = "exportStore",
      httpMethod = "GET",
      code=200,
      produces="application/zip"
  )
  @ApiResponses(Array(
    new ApiResponse(
        code = 200,
        message = "The diagnostic information as a zip file.",
        response=classOf[Array[Byte]],
    ),
    new ApiResponse(
        code = 400,
        message = "Bad request",
        response=classOf[RestMessage]
    )
  ))
  def diagnostics = get {
    path( "diagnostics" ) {
      log.fine(s"starting to export of diagnostic information")
      val byteSource: Source[ByteString, Unit] = StreamConverters.asOutputStream()
                        .mapMaterializedValue { os =>

                          val buf = new BufferedOutputStream(os)
                          val zip = new ZipOutputStream( buf, StandardCharsets.UTF_8 )

                          {
                            val nameInZip = "version.txt"
                            val ze = new ZipEntry(nameInZip)
                            println(s"Adding version info => ${ze.getName}")
                            zip.putNextEntry(ze)
                            val v = s"""${VersionServer.toString}\n${VersionShared.toString}\n${VersionUtilities.toString}"""
                            zip.write(v.getBytes("UTF8"))
                            zip.closeEntry()
                          }

                          restService.exportToZip(zip,None).onComplete { tr =>
                            tr match {
                              case Success(Right(list)) =>
                                diagnosticDir.foreach { dir =>
                                  dir.files.filter( f => f.extension == "log" || f.extension == "csv").foreach { f =>
                                    zip.putNextEntry( new ZipEntry("logs/"+f.name) )
                                    import _root_.resource._
                                    for ( in <- managed(new FileInputStream( f.jfile )) ) {
                                      copy(in, zip)
                                    }
                                  }
                                }
                                try {
                                  zip.finish()
                                  buf.flush()
                                  os.flush()
                                  os.close()
                                  log.fine(s"Successfully export bridge store with Ids ${list}")
                                } catch {
                                  case x: Exception =>
                                    log.warning("Exception closing stream for exporting diagnostic information", x)
                                }
                              case Success(Left((statusCode,msg))) =>
                                log.warning(s"Error exporting diagnostic information: ${statusCode} ${msg.msg}")
                                os.close()
                              case Failure(err) =>
                                log.warning("Failure exporting diagnostic information", err)
                                os.close()
                            }
                          }
                        }
      complete(
          HttpResponse(entity = HttpEntity(
                                  MediaTypes.`application/zip`,
                                  byteSource),
                       headers = `Content-Disposition`( ContentDispositionTypes.attachment,
                                                        Map( "filename" -> "BridgeScorerDiagnostics.zip") )::Nil
                      )
      )
    }
  }

  private def copy( in: InputStream, out: OutputStream ) = {
    val b = new Array[Byte]( 1024*1024 )

    var count: Long = 0
    var rlen = 0
    while ( { rlen=in.read(b); rlen } > 0 ) {
      out.write(b, 0, rlen)
      count += rlen
    }
    count
  }
}
