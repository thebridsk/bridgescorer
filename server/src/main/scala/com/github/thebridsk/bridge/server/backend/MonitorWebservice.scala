package com.github.thebridsk.bridge.server.backend

import scala.concurrent.Await

import com.github.thebridsk.bridge.data.websocket.DuplexProtocol

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.RemoteAddress
import akka.http.scaladsl.model.ws.BinaryMessage
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.ws.TextMessage
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
import akka.http.scaladsl.server.RejectionHandler
import com.github.thebridsk.bridge.data.VersionedInstance

abstract class MonitorWebservice[VId <: Comparable[
  VId
], VType <: VersionedInstance[
  VType,
  VType,
  VId
]](
    totallyMissingResourceHandler: RejectionHandler
)(implicit
    fm: Materializer,
    system: ActorSystem
) extends Directives {
  private val log = Logging(system, classOf[MonitorWebservice[_, _]])
  val monitor: StoreMonitorManager[VId, VType]

//  system.scheduler.schedule(15.second, 15.second) {
//    theChat.injectMessage(ChatMessage(sender = "clock", s"Bling! The time is ${new Date().toString}."))
//  }

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

  def websocketMonitor(sender: RemoteAddress): Flow[Message, Message, NotUsed] =
    Flow[Message]
      .mapConcat {
        case TextMessage.Strict(msg) =>
          log.debug("(" + sender + "): Received " + msg)
          msg :: Nil // unpack incoming WS text messages...
        case tm: TextMessage =>
          log.debug("From (" + sender + "): Received TextMessage.Streamed")
          collect(tm.textStream)(_ + _) :: Nil
        case bm: BinaryMessage =>
          log.debug("From (" + sender + "): Received BinaryMessage")
          // ignore binary messages but drain content to avoid the stream being clogged
          bm.dataStream.runWith(Sink.ignore)
          Nil
      }
      .via(
        monitor.monitorFlow(sender)
      ) // ... and route them through the chatFlow ...
      .mapConcat {
        case msg: DuplexProtocol.DuplexMessage =>
          log.debug("(" + sender + "): Sending " + msg)
          TextMessage.Strict(
            DuplexProtocol.toString(msg)
          ) :: Nil // ... pack outgoing messages into WS JSON messages ...
        // case msg: Message =>
        //   log.debug(s"""(${sender}): Sending message ${msg}""")
        //   msg :: Nil
        case msg =>
          log.info(s"""(${sender}): Unknown message: ${msg.getClass} ${msg}""")
          Nil
      }
      .withAttributes(Attributes.inputBuffer(initial = 32, max = 128))
      .via(
        reportErrorsFlow(sender)
      ) // ... then log any processing errors on stdin

  def reportErrorsFlow[T](sender: RemoteAddress): reportErrorsFlow[T] =
    new reportErrorsFlow[T](sender)
  class reportErrorsFlow[T](sender: RemoteAddress)
      extends GraphStage[FlowShape[T, T]] {
    val in: Inlet[T] = Inlet[T]("reportErrorsFlow.in")
    val out: Outlet[T] = Outlet[T]("reportErrorsFlow.out")
    override val shape: FlowShape[T, T] = FlowShape(in, out)
    override def initialAttributes: Attributes =
      Attributes(List(Name("reportErrorsFlow")))
    def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
      new GraphStageLogic(shape) {
        setHandler(
          in,
          new InHandler {
            override def onPush(): Unit = push(out, grab(in))

            override def onUpstreamFinish(): Unit = {
              log.info("(" + sender + "): Upstream finished")
              completeStage()
            }

            override def onUpstreamFailure(ex: Throwable): Unit = {
              log.error(ex, "(" + sender + "): Upstream failure ${ex}")
              failStage(ex)
            }
          }
        )
        setHandler(
          out,
          new OutHandler {
            override def onPull(): Unit = pull(in)
            override def onDownstreamFinish(cause: Throwable): Unit = {
              log.info("(" + sender + "): Downstream finished", cause)
              completeStage()
            }
          }
        )
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
