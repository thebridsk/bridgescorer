package com.github.thebridsk.bridge.server.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.server.service.MyService
import com.github.thebridsk.bridge.server.test.backend.BridgeServiceTesting
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Flow
import akka.http.scaladsl.testkit.WSProbe
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Source
import akka.http.scaladsl.model.ws.BinaryMessage
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.NotUsed
import akka.stream.stage.GraphStage
import akka.stream.Attributes
import akka.stream.stage.GraphStageLogic
import akka.stream.Inlet
import akka.stream.Outlet
import akka.stream.FlowShape
import akka.stream.stage.InHandler
import akka.stream.stage.OutHandler
import akka.stream.Attributes.Name
import akka.event.Logging
import com.github.thebridsk.bridge.server.rest.ServerPort
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Route
import TestDuplicateRestSpecImplicits._

class TestWebsocket
    extends AnyFlatSpec
    with ScalatestRouteTest
    with Matchers
    with MyService
    with RoutingSpec {
  val restService = new BridgeServiceTesting

  val httpport = 8080
  override def ports: ServerPort = ServerPort(Option(httpport), None)

  implicit lazy val actorSystem = system //scalafix:ok ExplicitResultTypes
  implicit lazy val actorExecutor = executor //scalafix:ok ExplicitResultTypes
  implicit lazy val actorMaterializer =
    materializer //scalafix:ok ExplicitResultTypes

  lazy val testlog: LoggingAdapter =
    Logging(actorSystem, classOf[TestWebsocket])

  behavior of "Test Websocket"

  def greeter: Flow[Message, Message, Any] =
    Flow[Message].mapConcat {
      case tm: TextMessage =>
        testlog.debug("got TextMessage")
        TextMessage(
          Source.single("Hello ") ++ tm.textStream ++ Source.single("!")
        ) :: Nil
      case bm: BinaryMessage =>
        testlog.debug("got BinaryMessage")
        // ignore binary messages
        bm.dataStream.runWith(Sink.ignore)
        Nil
    }

  def websocketMonitor: Flow[Message, Message, NotUsed] =
    Flow[Message]
      .mapConcat {
        case TextMessage.Strict(s) =>
          testlog.info("got strict textmessage: " + s)
          TextMessage.Strict("Hello " + s + "!") :: Nil
        case tm: TextMessage =>
          testlog.info("got TextMessage")
          TextMessage(
            Source.single("Hello ") ++ tm.textStream ++ Source.single("!")
          ) :: Nil
        case bm: BinaryMessage =>
          testlog.info("got BinaryMessage")
          // ignore binary messages
          bm.dataStream.runWith(Sink.ignore)
          Nil
      }
      .via(reportErrorsFlow) // ... then log any processing errors on stdin

//  def reportErrorsFlow[T]: Flow[T, T, NotUsed] =
//    Flow[T]
//      .transform(() => new PushStage[T, T] {
//        def onPush(elem: T, ctx: Context[T]): SyncDirective = ctx.push(elem)
//
//        override def onUpstreamFailure(cause: Throwable, ctx: Context[T]): TerminationDirective = {
//          testlog.debug(s"WS stream failed with $cause")
//          super.onUpstreamFailure(cause, ctx)
//        }
//      })

  def reportErrorsFlow[T]: GraphStage[FlowShape[T, T]] =
    new WSReportErrorsFlow[T]()
  class WSReportErrorsFlow[T]() extends GraphStage[FlowShape[T, T]] {
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
              testlog.info(s"WS stream finished (upstream)")
              completeStage()
            }

            override def onUpstreamFailure(ex: Throwable): Unit = {
              testlog.info(s"WS stream failed with $ex")
              failStage(ex)
            }
          }
        )
        setHandler(
          out,
          new OutHandler {
            override def onPull(): Unit = pull(in)
            override def onDownstreamFinish(cause: Throwable): Unit = {
              testlog.info(s"WS stream finished (downstream)", cause)
              completeStage()
            }
          }
        )
      }
    }
  }

  val websocketRoute: Route =
    akka.http.scaladsl.server.Directives.path("greeter") {
      handleWebSocketMessages(websocketMonitor /*greeter*/ )
    }

  it should "Open a Websocket" in {
    val wsClient = WSProbe()
    WS("/greeter", wsClient.flow).addAttributes(
      remoteAddress
    ) ~> websocketRoute ~>
      check {
        wsClient.inProbe.within(10 seconds) {
          isWebSocketUpgrade mustEqual true

          wsClient.sendMessage("Peter")
          wsClient.expectMessage("Hello Peter!")

          wsClient.sendMessage(BinaryMessage.apply(ByteString("abcdef")))
          wsClient.expectNoMessage(100.millis)

          wsClient.sendMessage("John")
          wsClient.expectMessage("Hello John!")

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }

      }
  }

}
