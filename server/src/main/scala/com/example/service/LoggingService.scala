package com.example.service

import com.example.backend.BridgeService
import com.example.data.Board
import com.example.data.MatchDuplicate
import akka.event.Logging
import akka.event.Logging._
import io.swagger.annotations._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.example.util.HasActorSystem
import akka.http.scaladsl.model.StatusCode
import com.example.data.Id
import com.example.data.DuplicateSummary
import javax.ws.rs.Path
import com.example.data.RestMessage
import com.example.data.SystemTime
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.RejectionHandler
import akka.http.scaladsl.server.MissingCookieRejection
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.server.MethodRejection
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.UnsupportedWebSocketSubprotocolRejection
import akka.util.ByteString
import com.example.rest.Service
import com.example.data.websocket.DuplexProtocol.LogEntryV2

/**
 * <p>
 * The REST API and all the methods are documented using
 * swagger annotations.
 */
@Path( "" )
@Api(tags= Array("Logging"), description = "logging operations.", produces="application/json")
trait LoggingService extends HasActorSystem with ClientLoggingService {

  private lazy val log = Logging(actorSystem, classOf[LoggingService])

  val loggerUrlPrefix = "logger"

  /**
   * spray route for all the methods on this resource
   */
  def loggingRoute = {
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

  import scala.language.postfixOps

  @Path("/logger")
  @ApiOperation(value = "Remote logging", notes = "", nickname = "callRemoteLogging",
                httpMethod = "PUT", code=204, response=classOf[String], protocols="http, https")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", value = "the value 'entry'", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "body", value = "Log entry", dataTypeClass = classOf[LogEntryV2],
                         required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "Accepted", response=classOf[Void] )
  ))
  def callRemoteLogging( @ApiParam(hidden=true) ips: String ) =
    path("entry") {
      (post | put) {
        import com.example.rest.UtilsPlayJson._
        entity(as[LogEntryV2]) { traceMsg =>
          Service.logFromBrowser(ips, "rs", traceMsg)
          complete(StatusCodes.NoContent)
        } ~ logRequest("ERROR") {
          entity(as[ByteString]) { bs =>
            val s = bs.decodeString("ASCII")
            Service.logStringFromBrowser(ips, s)
            log.error(s"ClientLog($ips): ERROR did not get a string for /logging\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n")
            complete(StatusCodes.NoContent)
          } ~ complete {
            log.error(s"ClientLog($ips): ERROR did not get a string for /logging\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n")
            StatusCodes.NoContent
          }
        }
      } ~ logRequest("ERROR") {
        entity(as[ByteString]) { bs =>
          val s = bs.decodeString("ASCII")
          Service.logStringFromBrowser(ips, s)
          complete {
            log.error(s"ClientLog($ips): ERROR did not get a POST or PUT for /logging\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n")
            StatusCodes.NoContent
          }
        } ~ complete {
          log.error(s"ClientLog($ips): ERROR did not get a string for /logging\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n")
          StatusCodes.NoContent
        }
      }
    }

  @Path("/logger/ws")
  @ApiOperation(value = "Remote logging", notes = "", nickname = "callRemoteLoggingWS",
                protocols="ws, wss", httpMethod = "GET", code=101, response=classOf[String] )
  @ApiResponses(Array(
    new ApiResponse(code = 101, message = "Switching to websocket protocol", response=classOf[Void] )
  ))
  def callRemoteLoggingWS( @ApiParam(hidden=true) ips: String ) =
    pathPrefix("ws") {
//      logRequest(ips, logLevelForTracingRequestResponse) {
        routeLogging(ips)
//      }
    }

}
