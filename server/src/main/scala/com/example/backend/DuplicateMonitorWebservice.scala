package com.example.backend

import scala.concurrent.duration.DurationLong
import scala.language.postfixOps

import com.example.data.websocket.DuplexProtocol
import com.example.data.websocket.Protocol

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.RemoteAddress
import akka.http.scaladsl.model.ws.BinaryMessage
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directive.addByNameNullaryApply
import akka.http.scaladsl.server.Directive.addDirectiveApply
import akka.http.scaladsl.server.Directives
import akka.stream.Attributes
import akka.stream.Attributes.Name
import akka.stream.FlowShape
import akka.stream.Inlet
import akka.stream.Materializer
import akka.stream.Outlet
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.stage.GraphStage
import akka.stream.stage.GraphStageLogic
import akka.stream.stage.InHandler
import akka.stream.stage.OutHandler
import akka.actor.Props
import akka.http.scaladsl.model.sse.ServerSentEvent
import com.example.data.Id
import akka.http.scaladsl.server.RejectionHandler
import akka.http.scaladsl.server.MalformedRequestContentRejection
import com.example.data.RestMessage
import akka.http.scaladsl.server.MethodRejection
import akka.http.scaladsl.model.headers.Allow
import akka.http.scaladsl.server.UnsupportedRequestContentTypeRejection
import akka.http.scaladsl.model.MediaTypes
import javax.ws.rs.Path
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Content
import javax.ws.rs.GET
import com.example.data.MatchDuplicate
import com.example.backend.StoreMonitor.NewParticipantSSEDuplicate

@Path("")
class DuplicateMonitorWebservice(
    totallyMissingResourceHandler: RejectionHandler
)(
    implicit fm: Materializer,
    system: ActorSystem,
    bridgeService: BridgeService
) extends MonitorWebservice[Id.MatchDuplicate, MatchDuplicate](
      totallyMissingResourceHandler
    ) {
  val log = Logging(system, classOf[DuplicateMonitorWebservice])
  val monitor = new StoreMonitorManager(
    system,
    bridgeService.duplicates,
    classOf[DuplicateStoreMonitor],
    NewParticipantSSEDuplicate.apply _
  )
  import system.dispatcher
//  system.scheduler.schedule(15.second, 15.second) {
//    theChat.injectMessage(ChatMessage(sender = "clock", s"Bling! The time is ${new Date().toString}."))
//  }

  def route = routews ~ routesse
  @Path("/ws")
  @GET
  @Operation(
    tags = Array("Server"),
    summary = "BridgeScorer websocket, protocol: WS, WSS",
    operationId = "MonitorWebserviceroute",
    responses = Array(
      new ApiResponse(
        responseCode = "101",
        description = "Switching to websocket protocol"
      )
    )
  )
  def xxxroutews = {}
  val routews =
    get {
      pathPrefix("ws") {
        handleRejections(totallyMissingResourceHandler) {
          //      pathPrefix("duplicates") {
          //        pathPrefix( """[a-zA-Z0-9]+""".r ) { id =>
          pathEndOrSingleSlash {
            extractClientIP { ip =>
              {
                handleWebSocketMessagesForProtocol(
                  websocketMonitor(ip),
                  Protocol.DuplicateBridge
                )
              }
            }
          }
          //        }
          //      }
        }
      }
    }
  @Path("/sse/duplicates/{dupId}")
  @GET
  @Operation(
    tags = Array("Duplicate"),
    summary = "BridgeScorer server set event on a duplicate match",
    operationId = "MonitorSSE",
    parameters = Array(
      new Parameter(
        allowEmptyValue = false,
        description = "ID of the match to get",
        in = ParameterIn.PATH,
        name = "dupId",
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
  def xxxroutesse = {}
  val routesse = {
    import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
    import akka.http.scaladsl.model.sse.ServerSentEvent
    pathPrefix("sse") {
      get {
        logRequest("sse", Logging.DebugLevel) {
          logResult("sse", Logging.DebugLevel) {
            pathPrefix("duplicates") {
              handleRejections(totallyMissingResourceHandler) {
                pathPrefix("""[a-zA-Z0-9]+""".r) { id =>
                  pathEndOrSingleSlash {
                    extractClientIP { ip =>
                      {
                        log.info(s"SSE from $ip for $id")
                        reject(
                          UnsupportedRequestContentTypeRejection(
                            Set(MediaTypes.`text/event-stream`)
                          )
                        )
                        complete {
                          val dupid: Id.MatchDuplicate = id
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
