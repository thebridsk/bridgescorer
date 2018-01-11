package com.example.test

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import com.example.data.Board
import com.example.data.Table
import com.example.service.MyService
import com.example.data.Hand
import com.example.test.backend.BridgeServiceTesting
import com.example.backend.BridgeService
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Flow
import org.scalatest._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.unmarshalling.FromResponseUnmarshaller
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.headers.`Remote-Address`
import akka.http.scaladsl.model.RemoteAddress.IP
import java.net.InetAddress
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.testkit.WSProbe
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Source
import akka.http.scaladsl.model.ws.BinaryMessage
import akka.stream.scaladsl.Sink
import akka.http.scaladsl.server.Directives._
import akka.util.ByteString
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.NotUsed
import akka.stream.impl.fusing.GraphStages
import akka.stream.stage.GraphStage
import akka.stream.Attributes
import akka.stream.stage.GraphStageLogic
import akka.stream.Inlet
import akka.stream.Outlet
import akka.stream.FlowShape
import akka.stream.stage.InHandler
import akka.stream.stage.OutHandler
import akka.stream.Shape
import akka.stream.Attributes.Name
import akka.event.Logging
import com.example.rest.ServerPort

class TestWebsocket extends FlatSpec with ScalatestRouteTest with MustMatchers with MyService {
  val restService = new BridgeServiceTesting

  val httpport = 8080
  override
  def ports = ServerPort( Option(httpport), None )

  implicit val actorSystem = system
  implicit val actorExecutor = executor
  implicit val actorMaterializer = materializer

  lazy val testlog = Logging(actorSystem, classOf[TestWebsocket])

  behavior of "Test Websocket"

  val remoteAddress = `Remote-Address`( IP( InetAddress.getLocalHost, Some(12345) ))

  def greeter: Flow[Message, Message, Any] =
    Flow[Message].mapConcat {
    case tm: TextMessage =>
      testlog.debug ("got TextMessage")
      TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
    case bm: BinaryMessage =>
      testlog.debug ("got BinaryMessage")
      // ignore binary messages
      bm.dataStream.runWith(Sink.ignore)
      Nil
  }

  def websocketMonitor: Flow[Message, Message, NotUsed] =
    Flow[Message]
      .mapConcat {
      case TextMessage.Strict(s) =>
        testlog.info("got strict textmessage: "+s)
        TextMessage.Strict( "Hello "+s+"!" ) :: Nil
      case tm: TextMessage =>
        testlog.info ("got TextMessage")
        TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
      case bm: BinaryMessage =>
        testlog.info ("got BinaryMessage")
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

  def reportErrorsFlow[T] =
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
              testlog.info(s"WS stream finished (upstream)")
              completeStage()
            }

            override def onUpstreamFailure(ex: Throwable): Unit = {
              testlog.info(s"WS stream failed with $ex")
              failStage(ex)
            }
          })
          setHandler(out, new OutHandler {
            override def onPull(): Unit = pull(in)
            override def onDownstreamFinish(): Unit = {
              testlog.info(s"WS stream finished (downstream)")
              completeStage()
            }
          })
        }
      }
    }


  val websocketRoute = akka.http.scaladsl.server.Directives.path("greeter") {
    handleWebSocketMessages( websocketMonitor /*greeter*/)
  }

  it should "Open a Websocket" in {
    val wsClient = WSProbe()
    WS("/greeter", wsClient.flow) ~> websocketRoute ~>
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
