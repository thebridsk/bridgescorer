package com.github.thebridsk.bridge.server.service

import akka.event.Logging._
import akka.http.scaladsl.server.Directives._
import javax.ws.rs.Path
import akka.http.scaladsl.server.RejectionHandler
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.RemoteAddress
import scala.concurrent.duration._
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.POST
import com.github.thebridsk.utilities.logging.Logger
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import scala.concurrent.Future

/**
  * <p>
  * The REST API and all the methods are documented using
  * swagger annotations.
  */
@Tags(Array(new Tag(name = "Server")))
abstract class ServerService(totallyMissingHandler: RejectionHandler) {

  val log: Logger = Logger[ServerService]()

  /**
    * Allow the logging level to be overridden for
    * tracing request and responses
    */
  val logLevelForTracingRequestResponse = DebugLevel // ErrorLevel // DebugLevel

  val listenInterface: List[String]

  val serverUrlPrefix = "shutdown"

  /**
    * spray route for all the methods on this resource
    */
  val serverRoute: RequestContext => Future[RouteResult] = {
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
    }
  }

  /**
    * Handler for converting rejections into HttpResponse
    */
//  def totallyMissingHandler: RejectionHandler

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
    responses = Array(
      new ApiResponse(responseCode = "204", description = "Accepted"),
      new ApiResponse(responseCode = "400", description = "Bad request"),
      new ApiResponse(responseCode = "404", description = "Not found")
    )
  )
  def xxxshutdown: Unit = {}
  def shutdown(@Parameter(hidden = true) ip: RemoteAddress): Route =
    logRequestResult(ip.toString(), logLevelForTracingRequestResponse) {
      post {
        parameterMap { params =>
          if (params.get("doit").getOrElse("") == "yes") {
            ip.toOption match {
              case Some(inet) if inet.isLoopbackAddress()
                                 || listenInterface.contains(inet.getHostAddress()) =>
                  log.info("Terminating server")
                  import scala.language.postfixOps
                  MyService.shutdownHook match {
                    case Some(hook) =>
                      hook.terminateServerIn(1 second)
                      complete(StatusCodes.NoContent)
                    case None =>
                      log.severe("Error")
                      complete(StatusCodes.NotFound)
                  }
              case _ =>
                log.severe(
                  "Could not determine remote address or it is not local, ip=" + ip
                )
                complete(
                  StatusCodes.BadRequest,
                  "Request not from valid address"
                )
            }
          } else {
            log.severe("Missing secret")
            complete(StatusCodes.BadRequest, "Request is missing secret")
          }
        }
      }
    }
}
