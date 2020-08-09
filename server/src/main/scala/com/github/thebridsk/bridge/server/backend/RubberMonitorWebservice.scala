package com.github.thebridsk.bridge.server.backend

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.server.Directive.addByNameNullaryApply
import akka.http.scaladsl.server.Directive.addDirectiveApply
import akka.stream.Materializer
import akka.http.scaladsl.server.RejectionHandler
import com.github.thebridsk.bridge.data.RestMessage
import javax.ws.rs.Path
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Content
import javax.ws.rs.GET
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.server.backend.StoreMonitor.NewParticipantSSERubber
import com.github.thebridsk.bridge.server.rest.Service
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Route

@Path("")
class RubberMonitorWebservice(
    totallyMissingResourceHandler: RejectionHandler,
    service: Service
)(implicit
    fm: Materializer,
    system: ActorSystem,
    bridgeService: BridgeService
) extends MonitorWebservice[MatchRubber.Id, MatchRubber](
      totallyMissingResourceHandler
    ) {
  val log: LoggingAdapter = Logging(system, classOf[RubberMonitorWebservice])
  val monitor = new StoreMonitorManager(
    system,
    bridgeService.rubbers,
    classOf[RubberStoreMonitor],
    NewParticipantSSERubber.apply _,
    service
  )
//  system.scheduler.schedule(15.second, 15.second) {
//    theChat.injectMessage(ChatMessage(sender = "clock", s"Bling! The time is ${new Date().toString}."))
//  }

  def route = routesse

  @Path("/sse/rubbers/{rubId}")
  @GET
  @Operation(
    tags = Array("Rubber"),
    summary = "BridgeScorer server set event on a rubber match",
    operationId = "MonitorSSERubber",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the match to get",
        in = ParameterIn.PATH,
        name = "rubId",
        required = true,
        schema = new Schema(`type` = "string")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Server sent event stream starting",
        content = Array(
          new Content(
            mediaType = "text/event-stream",
            schema = new Schema(implementation = classOf[String])
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
  def xxxroutesse: Unit = {}
  val routesse: Route = {
    import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
    pathPrefix("sse") {
      get {
        logRequest("sse", Logging.DebugLevel) {
          logResult("sse", Logging.DebugLevel) {
            pathPrefix("rubbers") {
              handleRejections(totallyMissingResourceHandler) {
                pathPrefix("""[a-zA-Z0-9]+""".r) { id =>
                  pathEndOrSingleSlash {
                    extractClientIP { ip =>
                      {
                        log.info(s"SSE from $ip for $id.id")
                        complete {
                          monitor.monitorMatch(ip, MatchRubber.id(id))
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
