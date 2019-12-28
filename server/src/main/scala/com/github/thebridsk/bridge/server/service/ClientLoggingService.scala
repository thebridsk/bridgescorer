package com.github.thebridsk.bridge.server.service

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.event.Logging._
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
import akka.http.scaladsl.server.Directives._

import client.LogA
import com.github.thebridsk.bridge.server.util.HasActorSystem
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol
import scala.concurrent.Await
import com.github.thebridsk.bridge.server.rest.Service
import com.github.thebridsk.bridge.data.websocket.Protocol
import java.io.StringWriter
import java.io.PrintWriter
//import jawn.ParseException
import com.github.thebridsk.bridge.data.rest.JsonException
import com.fasterxml.jackson.core.JsonParseException
import io.swagger.v3.oas.annotations.Hidden

class Counter(max: Long) {
  private var counter: Long = 0

  /**
    * Increment the counter
    * @return true if max has been hit, in which case the counter is reset to 0
    */
  def inc() = synchronized {
    counter = counter + 1;
    if (counter > max) {
      counter = 0
      true
    } else {
      false
    }
  }
}

trait ClientLoggingService {
  hasActorSystem: HasActorSystem =>

  private lazy val log =
    Logging(hasActorSystem.actorSystem, classOf[ClientLoggingService])

  @Hidden
  def routeLogging(ip: String) =
    get {
      pathEndOrSingleSlash {
        logRequest("ClientLoggingService.routeLogging pathend", DebugLevel) {
          logResult(
            "ClientLoggingService.routeLogging pathend result",
            DebugLevel
          ) {
            handleWebSocketMessagesForProtocol(
              websocketMonitor(ip, new Counter(100)),
              Protocol.Logging
            )
          }
        }
      }
    }

  def websocketMonitor(
      sender: String,
      counter: Counter
  ): Flow[Message, Message, NotUsed] = {
    log.debug("Setting up logging websocket for " + sender)
    Flow[Message]
      .mapConcat {
        case TextMessage.Strict(msg) =>
//          log.debug("("+sender+"): Received "+msg)
          msg :: Nil // unpack incoming WS text messages...
        case tm: TextMessage =>
//          log.debug("From ("+sender+"): Received TextMessage.Streamed")
          collect(tm.textStream)(_ + _) :: Nil
        case bm: BinaryMessage =>
          log.info("From (" + sender + "): Received BinaryMessage")
          // ignore binary messages but drain content to avoid the stream being clogged
          bm.dataStream.runWith(Sink.ignore)
          Nil
      }
//      .mapConcat { s =>
//        log.info("Got message: "+s)
//        s::Nil
//      }
//      .map { msg =>
//          log.debug("("+sender+"): Sending "+msg )
//          TextMessage.Strict(msg) // ... pack outgoing messages into WS JSON messages ...
//      }
      .mapConcat { s =>
        try {
          DuplexProtocol.fromString(s) match {
            case msg: DuplexProtocol.LogEntryV2 =>
              Service.logFromBrowser(sender.toString(), "ws", msg)
              Nil
            case msg =>
              if (counter.inc())
                DuplexProtocol.ErrorResponse("Unknown message", 0) :: Nil
              else Nil
          }
        } catch {
          case x: JsonException =>
            log.debug("Got invalid data: " + s)
            if (counter.inc()) {
              val sw = new StringWriter
              val pw = new PrintWriter(sw)
              x.printStackTrace(pw)
              pw.flush()
              val e = sw.toString()
              log.warning("Exception processing message: " + e)
              DuplexProtocol.ErrorResponse("Unknown message", 0) :: Nil
            } else {
              Nil
            }
          case x: JsonParseException =>
            log.debug("Got invalid data: " + s)
            if (counter.inc()) {
              val sw = new StringWriter
              val pw = new PrintWriter(sw)
              x.printStackTrace(pw)
              pw.flush()
              val e = sw.toString()
              log.warning("Exception processing message: " + e)
              DuplexProtocol.ErrorResponse("Unknown message", 0) :: Nil
            } else {
              Nil
            }
        }
      }
      .map {
        case msg: DuplexProtocol.DuplexMessage =>
          log.debug("(" + sender + "): Sending " + msg)
          TextMessage.Strict(DuplexProtocol.toString(msg)) // ... pack outgoing messages into WS JSON messages ...
      }
      .withAttributes(Attributes.inputBuffer(initial = 32, max = 128))
      .via(reportErrorsFlow(sender)) // ... then log any processing errors on stdin
  }

  val maxChunks: Int = 1000
  val maxChunkCollectionMills: Long = 5000
  def collect[T](stream: Source[T, Any])(reduce: (T, T) => T): T = {
    import scala.language.postfixOps
    import scala.concurrent.duration._
    val g = stream.grouped(maxChunks)
    val w = g.runWith(Sink.head)
//    val a = w.awaitResult(maxChunkCollectionMills.millis)
    val a = Await.result(w, maxChunkCollectionMills millis)
    a.reduce(reduce)
  }

  def reportErrorsFlow[T](sender: String) =
    new GraphStage[FlowShape[T, T]] {
      val in = Inlet[T]("reportErrorsFlow.in")
      val out = Outlet[T]("reportErrorsFlow.out")
      override val shape = FlowShape(in, out)
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
                log.error(ex, "(" + sender + "): Upstream failure")
                failStage(ex)
              }
            }
          )
          setHandler(
            out,
            new OutHandler {
              override def onPull(): Unit = pull(in)
              override def onDownstreamFinish( cause: Throwable ): Unit = {
                log.info("(" + sender + "): Downstream finished",cause)
                completeStage()
              }
            }
          )
        }
      }
    }

}

object ClientLoggingService {

  val maxChunks: Int = 1000
  val maxChunkCollectionMills: Long = 5000
  def collect[T](
      stream: Source[T, Any]
  )(reduce: (T, T) => T)(implicit materializer: akka.stream.Materializer): T = {
    import scala.language.postfixOps
    import scala.concurrent.duration._
    val g = stream.grouped(maxChunks)
    val w = g.runWith(Sink.head)
//    val a = w.awaitResult(maxChunkCollectionMills.millis)
    val a = Await.result(w, maxChunkCollectionMills millis)
    a.reduce(reduce)
  }
}
