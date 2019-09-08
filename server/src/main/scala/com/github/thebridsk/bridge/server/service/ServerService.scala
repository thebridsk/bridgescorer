package com.github.thebridsk.bridge.server.service

import com.github.thebridsk.bridge.server.backend.BridgeService
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.MatchDuplicate
import akka.event.Logging
import akka.event.Logging._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.thebridsk.bridge.server.util.HasActorSystem
import akka.http.scaladsl.model.StatusCode
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.data.DuplicateSummary
import javax.ws.rs.Path
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.data.SystemTime
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.RejectionHandler
import akka.http.scaladsl.server.MissingCookieRejection
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.server.MethodRejection
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.UnsupportedWebSocketSubprotocolRejection
import akka.util.ByteString
import akka.http.scaladsl.model.RemoteAddress
import scala.concurrent.duration._
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.GET
import javax.ws.rs.POST
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.jaxrs2.ReaderListener
import io.swagger.v3.jaxrs2.Reader
import io.swagger.v3.oas.models.OpenAPI
import java.util.TreeMap
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.util.ColorTheme
import com.github.thebridsk.bridge.server.rest.UtilsPlayJson
import akka.http.scaladsl.model.headers.HttpCookie
import scala.concurrent.Future
import com.github.thebridsk.bridge.server.backend.resource.Result
import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.DateTime

/**
  * <p>
  * The REST API and all the methods are documented using
  * swagger annotations.
  */
@Tags(Array(new Tag(name = "Server")))
class ServerService(totallyMissingHandler: RejectionHandler) {

  import UtilsPlayJson._

  val log = Logger[ServerService]

  /**
    * Allow the logging level to be overridden for
    * tracing request and responses
    */
  val logLevelForTracingRequestResponse = DebugLevel // ErrorLevel // DebugLevel

  val serverUrlPrefix = "shutdown"

  val cookieTimeout = 30*24*60*60*1000L

  /**
    * spray route for all the methods on this resource
    */
  lazy val serverRoute = {
    extractClientIP { ip =>
      {
        pathPrefix(serverUrlPrefix) {
          logRequest("serverRoute", DebugLevel) {
            handleRejections(totallyMissingHandler) {
              shutdown(ip)
            }
          }
        }
      }
    } ~ getColorTheme ~ setColorTheme
  }

  /**
    * Handler for converting rejections into HttpResponse
    */
//  def totallyMissingHandler: RejectionHandler

  import scala.language.postfixOps

  @Path("/shutdown")
  @POST
  @Operation(
    summary = "Shutdown the server",
    description =
      "Shutdown the server, must be issued through loopback interface",
    operationId = "shutdown",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "Actually do the shutdown",
        in = ParameterIn.QUERY,
        name = "doit",
        required = true,
        schema = new Schema(`type` = "string", allowableValues = Array("yes"))
      )
    ),
    requestBody = new RequestBody(
      description = "movement to create",
      content = Array(
        new Content(
          mediaType = "text/plain",
          schema = new Schema(`type` = "string")
        )
      )
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "Accepted"),
      new ApiResponse(responseCode = "400", description = "Bad request")
    )
  )
  def xxxshutdown = {}
  def shutdown(@Parameter(hidden = true) ip: RemoteAddress) =
    logRequestResult(ip.toString(), logLevelForTracingRequestResponse) {
      post {
        parameterMap { params =>
          if (params.get("doit").getOrElse("") == "yes") {
            ip.toOption match {
              case Some(inet) if (inet.isLoopbackAddress()) =>
                log.info("Terminating server")
                import scala.language.postfixOps
                MyService.shutdownHook match {
                  case Some(hook) =>
                    hook.terminateServerIn(1 second)
                    complete(StatusCodes.NoContent)
                  case None =>
                    log.severe("Error")
                    complete(StatusCodes.BadRequest)
                }
              case _ =>
                log.severe(
                  "Could not determine remote address or it is not local, ip=" + ip
                )
                complete(StatusCodes.BadRequest)
            }
          } else {
            log.severe("Missing secret")
            complete(StatusCodes.BadRequest)
          }
        }
      }
    }

    @Path("/colortheme")
    @GET
    @Operation(
      summary = "Get the color theme setting",
      description =
        "Get the color theme setting from cookie",
      operationId = "getColorTheme",
      responses = Array(
        new ApiResponse(responseCode = "200", description = "Accepted"),
      )
    )
    def xxxGetColorTheme = {}
    val getColorTheme = get {
      pathPrefix("colortheme") {
        pathEndOrSingleSlash {
          optionalCookie("colorTheme") {
            case Some(colorTheme) => complete(Array[ColorTheme]( ColorTheme(colorTheme.value) ) )
            case None             => complete( Array[ColorTheme]( ColorTheme.light ) )
          }
        }
        optionalCookie("colorTheme") {
          case Some(colorTheme) => complete(Array[ColorTheme]( ColorTheme(colorTheme.value) ) )
          case None             => complete( Array[ColorTheme]( ColorTheme.light ) )
        }
      }
    }

    @POST
    @Operation(
      summary = "Sets the color theme",
      description =
        "Set the color theme cookie to the specified theme",
      operationId = "SetColorTheme",
      requestBody = new RequestBody(
        description = "The color theme to use",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[ColorTheme])
          )
        )
      ),
      responses = Array(
        new ApiResponse(
          responseCode = "201",
          description = "The color theme cookie was set",
          content = Array(
            new Content(
              mediaType = "application/json",
              schema = new Schema(implementation = classOf[ColorTheme])
            )
          )
        )
      )
    )
    def xxxSetColorTheme = {}
    val setColorTheme = {
      post {
        pathPrefix("colortheme") {
          entity(as[ColorTheme]) { theme =>
            setCookie(HttpCookie("colorTheme", value = theme.theme, expires=Some(DateTime.now+cookieTimeout))) {
              resourceCreated("colortheme", Future(Result[(String,ColorTheme)]("",theme)), Created)
            }
          }
        }
      }
    }

}
