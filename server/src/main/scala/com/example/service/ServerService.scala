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
import akka.http.scaladsl.model.RemoteAddress
import scala.concurrent.duration._

/**
 * <p>
 * The REST API and all the methods are documented using
 * swagger annotations.
 */
@Path( "" )
@Api(tags= Array("Server"), description = "server operations.", produces="application/json", protocols="http, https")
trait ServerService extends HasActorSystem with ClientLoggingService {

  private lazy val log = Logging(actorSystem, classOf[LoggingService])

  /**
   * Allow the logging level to be overridden for
   * tracing request and responses
   */
  val logLevelForTracingRequestResponse = DebugLevel // ErrorLevel // DebugLevel

  val serverUrlPrefix = "shutdown"

  /**
   * spray route for all the methods on this resource
   */
  def serverRoute = {
    extractClientIP { ip =>
      {
        pathPrefix(serverUrlPrefix) {
//          logRequest("route", DebugLevel) {
            handleRejections(totallyMissingHandler) {
              shutdown(ip)
            }
//          }
        }
      }
    }
  }

  /**
   * Handler for converting rejections into HttpResponse
   */
  def totallyMissingHandler: RejectionHandler

  import scala.language.postfixOps

  @Path("/shutdown")
  @ApiOperation(value = "Shutdown the server, must be issued through loopback interface", notes = "", nickname = "shutdown", httpMethod = "POST", code=204, response=classOf[String])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "doit", value = "Actually do the shutdown", required = true, dataType = "string", paramType = "query")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "Accepted", response=classOf[Void] ),
    new ApiResponse(code = 400, message = "Bad request", response=classOf[Void] )
  ))
  def shutdown( @ApiParam(hidden=true) ip: RemoteAddress ) =
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
                    hook.terminateServerIn( 1 second)
                    complete(StatusCodes.NoContent)
                  case None =>
                    log.error("Error")
                    complete(StatusCodes.BadRequest)
                }
              case _ =>
                log.error("Could not determine remote address or it is not local, ip="+ip)
                complete(StatusCodes.BadRequest)
            }
          } else {
            log.error("Missing secret")
            complete(StatusCodes.BadRequest)
          }
        }
      }
    }

}
