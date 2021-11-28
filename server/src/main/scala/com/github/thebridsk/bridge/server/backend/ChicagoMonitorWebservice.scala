package com.github.thebridsk.bridge.server.backend

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.server.Directive.addByNameNullaryApply
import akka.http.scaladsl.server.Directive.addDirectiveApply
import akka.stream.Materializer
import akka.http.scaladsl.server.RejectionHandler
import com.github.thebridsk.bridge.data.RestMessage
import jakarta.ws.rs.Path
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Content
import jakarta.ws.rs.GET
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.server.backend.StoreMonitor.NewParticipantSSEChicago
import com.github.thebridsk.bridge.server.rest.Service
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Route

@Path("")
class ChicagoMonitorWebservice(
    totallyMissingResourceHandler: RejectionHandler,
    service: Service
)(implicit
    fm: Materializer,
    system: ActorSystem,
    bridgeService: BridgeService
) extends MonitorWebservice[MatchChicago.Id, MatchChicago](
      totallyMissingResourceHandler
    ) {
  val log: LoggingAdapter = Logging(system, classOf[ChicagoMonitorWebservice])
  val monitor = new StoreMonitorManager(
    system,
    bridgeService.chicagos,
    classOf[ChicagoStoreMonitor],
    NewParticipantSSEChicago.apply _,
    service
  )
//  system.scheduler.schedule(15.second, 15.second) {
//    theChat.injectMessage(ChatMessage(sender = "clock", s"Bling! The time is ${new Date().toString}."))
//  }

  def route = routesse

  @Path("/sse/chicagos/{chiId}")
  @GET
  @Operation(
    tags = Array("Chicago"),
    summary = "BridgeScorer server set event on a chicago match",
    operationId = "MonitorSSEChicago",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the match to get",
        in = ParameterIn.PATH,
        name = "chiId",
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
            pathPrefix("chicagos") {
              handleRejections(totallyMissingResourceHandler) {
                pathPrefix("""[a-zA-Z0-9]+""".r) { id =>
                  pathEndOrSingleSlash {
                    extractClientIP { ip =>
                      {
                        log.info(s"SSE from $ip for $id")
                        complete {
                          val dupid = MatchChicago.id(id)
                          monitor.monitorMatch(ip, dupid)
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
