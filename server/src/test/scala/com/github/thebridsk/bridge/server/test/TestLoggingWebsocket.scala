package com.github.thebridsk.bridge.server.test

import java.net.InetAddress

import scala.language.postfixOps

import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import com.github.thebridsk.bridge.server.service.MyService

import akka.event.Logging
import akka.http.scaladsl.model.RemoteAddress.IP
import akka.http.scaladsl.model.headers.`Remote-Address`
import akka.http.scaladsl.model.ws.BinaryMessage
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.testkit.WSProbe
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes._
import scala.concurrent.duration._
import akka.actor.ActorSystem

trait XXX extends MyService {
  import com.github.thebridsk.bridge.server.rest.ServerPort
  import com.github.thebridsk.bridge.server.test.backend.BridgeServiceTesting
  val restService = new BridgeServiceTesting

  val httpport = 8080
  override
  def ports = ServerPort( Option(httpport), None )

  implicit val actorSystem: ActorSystem
}

class TestLoggingWebsocket extends AnyFlatSpec with ScalatestRouteTest with Matchers {

  implicit val actorSystem = system
  implicit val actorMaterializer = materializer
  implicit val actorExecutor = executor

  lazy val myService = new XXX {
    implicit val actorSystem = system
    implicit val materializer = actorMaterializer
  }

  lazy val testlog = Logging(actorSystem, classOf[TestLoggingWebsocket])

  TestStartLogging.startLogging()

  behavior of "Test Websocket again"

  val remoteAddress = `Remote-Address`( IP( InetAddress.getLocalHost, Some(12345) ))

  it should "Fail to open a Websocket" in {
    val wsClient = WSProbe()
    WS("/v1/logger/ws/", wsClient.flow, "xxx"::Nil ) ~> addHeader(remoteAddress) ~> Route.seal { myService.myRouteWithLogging } ~>
      check {
        wsClient.inProbe.within(10 seconds) {
          status mustBe BadRequest
          responseAs[String] mustBe "Protocol not accepted"
        }
      }
  }

  it should "Open a Websocket and send invalid data" in {
    val wsClient = WSProbe()
    WS("/v1/logger/ws/", wsClient.flow, "Logging"::Nil ) ~> addHeader(remoteAddress) ~> Route.seal { myService.myRouteWithLogging } ~>
      check {
        status mustBe SwitchingProtocols
        isWebSocketUpgrade mustEqual true

        testlog.debug("socket has been upgraded")

        wsClient.inProbe.within(1 seconds) {
          // Must burn the startup message in the queue before sending
          try {
            val msg = wsClient.expectMessage()
            fail("Was not expecting a message")
          } catch {
            case x: AssertionError =>
              testlog.debug("got exception as expected: "+x)
//              testlog.error(x,"got exception as expected")
          }
          try {
            wsClient.expectNoMessage( 1 millis)
            testlog.warning("got no message as expected from startup")
          } catch {
            case x: AssertionError =>
            testlog.error(x,"got on startup")
          }
        }
        wsClient.inProbe.within(100 seconds) {
          wsClient.sendMessage("Starting up")

          (1 to 101).foreach{ i => {
            wsClient.sendMessage("Peter")
            if (i == 100) {
              val msg = wsClient.expectMessage()
              msg match {
                case TextMessage.Strict(msg) =>
                  testlog.info("got response "+msg)
                case _ =>
                  fail("Expecting a text message, got: "+msg )
              }
            } else {
              try {
                wsClient.expectNoMessage( 1 millis)
                testlog.warning(i.toString()+": got no message")
              } catch {
                case x: AssertionError =>
                testlog.error(x,i.toString()+": got ")
              }
            }
          }}

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
      }
  }

  it should "Open a Websocket and send valid data" in {
    val wsClient = WSProbe()
    WS("/v1/logger/ws/", wsClient.flow, "Logging"::Nil ) ~> addHeader(remoteAddress) ~> Route.seal { myService.myRouteWithLogging } ~>
      check {
        status mustBe SwitchingProtocols
        isWebSocketUpgrade mustEqual true

        testlog.debug("socket has been upgraded")

        wsClient.inProbe.within(1 seconds) {
          // Must burn the startup message in the queue before sending
          try {
            val msg = wsClient.expectMessage()
            fail("Was not expecting a message")
          } catch {
            case x: AssertionError =>
              testlog.debug("got exception as expected: "+x)
//              testlog.error(x,"got exception as expected")
          }
          try {
            wsClient.expectNoMessage( 1 millis)
            testlog.warning("got no message as expected from startup")
          } catch {
            case x: AssertionError =>
            testlog.error(x,"got on startup")
          }
        }
        wsClient.inProbe.within(100 seconds) {

          (1 to 101).foreach { i =>
            wsClient.sendMessage(getLogEntry(i))
          }
          wsClient.expectNoMessage(1 seconds)

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
      }
  }

  def getLogEntry( i: Int ) = {
    import com.github.thebridsk.bridge.data.websocket.DuplexProtocol
    import com.github.thebridsk.bridge.data.websocket.Protocol

    //  position: String, timestamp: Long, level: String, url: String, message: String, cause: String, args: List[String])
    val dp = DuplexProtocol.LogEntryV2( position, "logger", System.currentTimeMillis(), "I", "http://example.com/test", "message: %s %s %s", "", List("hello", "world", i.toString()))
    DuplexProtocol.toString(dp)
  }

  import org.scalactic.source.Position
  def position( implicit pos: Position ) = {
    pos.fileName+":"+pos.lineNumber
  }

  def process( msg: String ) = {
    import com.github.thebridsk.bridge.data.websocket.DuplexProtocol
    val r = DuplexProtocol.fromString(msg)
    r match {
      case DuplexProtocol.ErrorResponse(data,seq) =>
        data mustBe "Unknown message"
      case _ =>
        fail("Unknown response: "+r)
    }

  }

}

