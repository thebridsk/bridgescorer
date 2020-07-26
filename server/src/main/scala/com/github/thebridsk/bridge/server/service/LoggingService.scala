package com.github.thebridsk.bridge.server.service

import akka.event.Logging
import akka.http.scaladsl.server.Directives._
import com.github.thebridsk.bridge.server.util.HasActorSystem
import javax.ws.rs.Path
import akka.http.scaladsl.server.RejectionHandler
import akka.http.scaladsl.model.StatusCodes
import akka.util.ByteString
import com.github.thebridsk.bridge.server.rest.Service
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.LogEntryV2
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.GET
import javax.ws.rs.POST
import io.swagger.v3.oas.annotations.Parameter
import akka.http.scaladsl.server.{ RequestContext, Route, RouteResult }
import scala.concurrent.Future

/**
  * <p>
  * The REST API and all the methods are documented using
  * swagger annotations.
  */
@Path("")
@Tags(Array(new Tag(name = "Server")))
trait LoggingService extends HasActorSystem with ClientLoggingService {

  private lazy val log = Logging(actorSystem, classOf[LoggingService])

  val loggerUrlPrefix = "logger"

  /**
    * spray route for all the methods on this resource
    */
  val loggingRoute: RequestContext => Future[RouteResult] = {
    extractClientIP { ip =>
      {
//        log.debug(s"In loggingRoute from ${ip.toString()}")
        val ips = ip.toString()
        pathPrefix(loggerUrlPrefix) {
//          logRequest("loggingRoute", DebugLevel) { logResult( "loggingRoute" ) {
          handleRejections(totallyMissingHandler) {
            callRemoteLogging(ips) ~
              callRemoteLoggingWS(ips)
          }
//          }}
        }
      }
    }
  }

  /**
    * Handler for converting rejections into HttpResponse
    */
  def totallyMissingHandler: RejectionHandler


  @Path("/logger/entry")
  @POST
  @Operation(
    summary = "Remote logging",
    operationId = "callRemoteLogging",
    requestBody = new RequestBody(
      description = "Log entry",
      content = Array(
        new Content(
          mediaType = "application/json",
          schema =
            new Schema(required = true, implementation = classOf[LogEntryV2])
        )
      )
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "Accepted")
    )
  )
  def xxxcallRemoteLogging: Unit = {}
  def callRemoteLogging(@Parameter(hidden = true) ips: String): Route =
    path("entry") {
      (post | put) {
        import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._
        entity(as[LogEntryV2]) { traceMsg =>
          Service.logFromBrowser(ips, "rs", traceMsg)
          complete(StatusCodes.NoContent)
        } ~ logRequest("ERROR") {
          entity(as[ByteString]) { bs =>
            val s = bs.decodeString("ASCII")
            Service.logStringFromBrowser(ips, s)
            log.error(
              s"ClientLog($ips): ERROR did not get a string for /logging\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"
            )
            complete(StatusCodes.NoContent)
          } ~ complete {
            log.error(
              s"ClientLog($ips): ERROR did not get a string for /logging\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"
            )
            StatusCodes.NoContent
          }
        }
      } ~ logRequest("ERROR") {
        entity(as[ByteString]) { bs =>
          val s = bs.decodeString("ASCII")
          Service.logStringFromBrowser(ips, s)
          complete {
            log.error(
              s"ClientLog($ips): ERROR did not get a POST or PUT for /logging\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"
            )
            StatusCodes.NoContent
          }
        } ~ complete {
          log.error(
            s"ClientLog($ips): ERROR did not get a string for /logging\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"
          )
          StatusCodes.NoContent
        }
      }
    }

  @Path("/logger/ws")
  @GET
  @Operation(
    summary = "Remote logging, protocols: WS, WSS",
    operationId = "callRemoteLoggingWS",
    responses = Array(
      new ApiResponse(
        responseCode = "101",
        description = "Switching to websocket protocol"
      )
    )
  )
  def xxxcallRemoteLoggingWS: Unit = {}
  def callRemoteLoggingWS(@Parameter(hidden = true) ips: String): Route =
    pathPrefix("ws") {
//      logRequest(ips, logLevelForTracingRequestResponse) {
      routeLogging(ips)
//      }
    }

}
