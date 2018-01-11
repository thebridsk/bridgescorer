package com.example.backend

import scala.concurrent.Await
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
import javax.ws.rs.Path
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponses
import io.swagger.annotations.ApiResponse
import akka.actor.Props

@Path( "" )
@Api(tags= Array("Server"), description = "Websocket operations.", produces="application/json")
class MonitorWebservice(implicit fm: Materializer, system: ActorSystem, bridgeService: BridgeService) extends Directives {
  val log = Logging(system, classOf[MonitorWebservice])
  val monitor = new StoreMonitorManager(system, bridgeService.duplicates)
  import system.dispatcher
//  system.scheduler.schedule(15.second, 15.second) {
//    theChat.injectMessage(ChatMessage(sender = "clock", s"Bling! The time is ${new Date().toString}."))
//  }


  @Path("/ws")
  @ApiOperation(value = "BridgeScorer websocket", notes = "", nickname = "MonitorWebserviceroute",
                protocols="WS, WSS", httpMethod = "GET", code=101, response=classOf[String] )
  @ApiResponses(Array(
    new ApiResponse(code = 101, message = "Switching to websocket protocol", response=classOf[Void] )
  ))
  def route =
    get {
//      pathPrefix("duplicates") {
//        pathPrefix( """[a-zA-Z0-9]+""".r ) { id =>
          pathEndOrSingleSlash {
            extractClientIP { ip => {
              handleWebSocketMessagesForProtocol(duplicateWebsocketMonitor(ip), Protocol.DuplicateBridge)
            }}
          }
//        }
//      }
    }

  val maxChunks: Int = 1000
  val maxChunkCollectionMills: Long = 5000
  private def collect[T](stream: Source[T, Any])(reduce: (T, T) => T): T = {
    import scala.language.postfixOps
    import scala.concurrent.duration._
    val g = stream.grouped(maxChunks)
    val w = g.runWith(Sink.head)
//    val a = w.awaitResult(maxChunkCollectionMills.millis)
    val a = Await.result(w, maxChunkCollectionMills millis)
    a.reduce(reduce)
  }

  def duplicateWebsocketMonitor(sender: RemoteAddress): Flow[Message, Message, NotUsed] =
    Flow[Message]
      .mapConcat {
        case TextMessage.Strict(msg) =>
          log.debug("("+sender+"): Received "+msg)
          msg::Nil // unpack incoming WS text messages...
        case tm: TextMessage =>
          log.debug("From ("+sender+"): Received TextMessage.Streamed")
          collect(tm.textStream)(_ + _)::Nil
        case bm: BinaryMessage =>
          log.debug("From ("+sender+"): Received BinaryMessage")
          // ignore binary messages but drain content to avoid the stream being clogged
          bm.dataStream.runWith(Sink.ignore)
          Nil
      }
      .via(monitor.monitorFlow(sender)) // ... and route them through the chatFlow ...
      .mapConcat {
        case msg: DuplexProtocol.DuplexMessage =>
          log.debug("("+sender+"): Sending "+msg )
          TextMessage.Strict(DuplexProtocol.toString(msg))::Nil // ... pack outgoing messages into WS JSON messages ...
        case msg: Message =>
          log.debug(s"""(${sender}): Sending message ${msg}""")
          msg::Nil
        case msg =>
          log.info(s"""(${sender}): Unknown message: ${msg.getClass} ${msg}""")
          Nil
      }.withAttributes(Attributes.inputBuffer(initial = 32, max = 128))
      .via(reportErrorsFlow(sender)) // ... then log any processing errors on stdin

  def reportErrorsFlow[T](sender: RemoteAddress) =
    new GraphStage[FlowShape[T,T]] {
      val in = Inlet[T]("reportErrorsFlow.in")
      val out = Outlet[T]("reportErrorsFlow.out")
      override val shape = FlowShape(in, out)
      override def initialAttributes: Attributes = Attributes( List(Name("reportErrorsFlow")))
      def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
        new GraphStageLogic(shape) {
          setHandler(in, new InHandler {
            override def onPush(): Unit = push(out, grab(in))

            override def onUpstreamFinish(): Unit = {
              log.info("("+sender+"): Upstream finished" )
              completeStage()
            }

            override def onUpstreamFailure(ex: Throwable): Unit = {
              log.error(ex,"("+sender+"): Upstream failure" )
              failStage(ex)
            }
          })
          setHandler(out, new OutHandler {
            override def onPull(): Unit = pull(in)
            override def onDownstreamFinish(): Unit = {
              log.info("("+sender+"): Downstream finished" )
              completeStage()
            }
          })
        }
      }
    }

//  def reportErrorsFlow[T]: Flow[T, T, NotUsed] =
//    Flow[T]
//      .transform(() => new PushStage[T, T] {
//        def onPush(elem: T, ctx: Context[T]): SyncDirective = ctx.push(elem)
//
//        override def onUpstreamFailure(cause: Throwable, ctx: Context[T]): TerminationDirective = {
//          println(s"WS stream failed with $cause")
//          super.onUpstreamFailure(cause, ctx)
//        }
//      })
}
