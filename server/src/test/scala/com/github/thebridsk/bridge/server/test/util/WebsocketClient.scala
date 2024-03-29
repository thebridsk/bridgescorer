package com.github.thebridsk.bridge.server.test.util

import scala.concurrent.duration._
import scala.language.postfixOps
import akka.http.scaladsl.testkit.WSProbe
import akka.http.scaladsl.model.ws.TextMessage
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol
import akka.http.scaladsl.model.ws.BinaryMessage
import org.scalatest.Assertions._
import com.github.thebridsk.bridge.server.service.ClientLoggingService._
import akka.actor.ActorSystem
import akka.stream.Materializer
import com.github.thebridsk.bridge.data.websocket.Protocol.ToServerMessage
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.DuplexMessage
import org.scalatest.matchers.must.Matchers
import akka.http.scaladsl.testkit.RouteTest
import com.github.thebridsk.bridge.data.websocket.Protocol
import akka.http.scaladsl.server.Route
import org.scalatest.Assertions
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateDuplicateTeam
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateDuplicateHand
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateDuplicate
import com.github.thebridsk.bridge.data.websocket.Protocol.MonitorJoined
import com.github.thebridsk.bridge.data.websocket.Protocol.MonitorLeft
import com.github.thebridsk.bridge.data.websocket.Protocol.NoData
import com.github.thebridsk.bridge.data.MatchDuplicate
import akka.event.LoggingAdapter
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateChicago
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateRubber
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateChicagoHand
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateChicagoRound
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateRubberHand
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateDuplicatePicture
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateDuplicatePictures
import org.scalatest.Assertion
import akka.http.scaladsl.model.AttributeKey
import com.github.thebridsk.bridge.server.test.TestDuplicateRestSpecImplicits._
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateIndividualDuplicatePicture
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateIndividualDuplicatePictures
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateIndividualDuplicateHand
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateIndividualDuplicate

@SuppressWarnings(
  Array( // Get really weird errors on missing implicits in WS() call
    "scalafix:ExplicitResultTypes"
  )
)
class WebsocketClient(implicit
    system: ActorSystem,
    materializer: Materializer,
    routetest: RouteTest
) {

  import Matchers._
  import routetest._
  import WebsocketClientImplicits._

  val wsClient = WSProbe()
  var lastSeq = 0

  def nextSeq = {
    lastSeq = lastSeq + 1
    lastSeq
  }

  private var myAddress: Option[String] = None

  def address = myAddress.getOrElse("<notConnected>")

  def connect(
      remoteAddress: Map[AttributeKey[_], _],
      route: Route,
      max: FiniteDuration = 10 seconds
  )(implicit testlog: LoggingAdapter) = {
    if (myAddress.isEmpty) {
      WS("/v1/ws", wsClient.flow, Protocol.DuplicateBridge :: Nil)
        .addAttributes(
          remoteAddress
        ) ~> route ~>
        check {
          myAddress = Some(remoteAddress.toString())
          try {
            isWebSocketUpgrade mustBe true
            within(max) {
              this.testJoin
            }
          } catch {
            case x: Throwable =>
              myAddress = None
              throw x
          }
        }
    } else {
      fail("Already connected")
    }
  }

  def within[T](max: FiniteDuration)(f: => T): T = {
    wsClient.inProbe.within(max)(f)
  }

  def expectProtocolMessage = {
    val r =
      wsClient.expectMessage() match {
        case TextMessage.Strict(s) =>
          DuplexProtocol.fromString(s)
        case tm: TextMessage =>
          val s = collect(tm.textStream)(_ + _)
          DuplexProtocol.fromString(s)
        case x: BinaryMessage =>
          val data = collect(x.dataStream)(_ ++ _)
          fail(
            s"${address} Unexpected response from the monitor, expecting TextMessage: ${x.getClass.getName}"
          )
      }
    r
  }

  def expectUnsolicitedMessage = {
    expectProtocolMessage match {
      case DuplexProtocol.Unsolicited(data) => data
      case dp =>
        fail(
          s"${address} Unexpected response from the monitor, expecting Unsolicited: ${dp}"
        )
    }
  }

  def expectResponseMessage = {
    expectProtocolMessage match {
      case resp: DuplexProtocol.Response => resp
      case dp =>
        fail(
          s"${address} Unexpected response from the monitor, expecting Response: ${dp}"
        )
    }
  }

  def expectNoResponse(max: FiniteDuration = 10 seconds) =
    wsClient.expectNoMessage(max)

  /**
    *  Send a message and not get a response
    */
  def send(data: ToServerMessage) = {
    val d = DuplexProtocol.Send(data)
    sendToServer(d)
  }

  /**
    *  Send a message and get a response
    *  @param data
    *  @param ack true - only an ack is requested
    *  @return the seqence number of the request
    */
  def request(data: ToServerMessage, ack: Boolean = false): Int = {
    val d = DuplexProtocol.Request(data, nextSeq, ack)
    sendToServer(d)
    d.seq
  }

  private def sendToServer(data: DuplexMessage) = {
    wsClient.sendMessage(DuplexProtocol.toString(data))
  }

  def requestResponse(data: ToServerMessage, ack: Boolean = false) = {
    val seq = request(data, ack)
    val resp = expectResponseMessage
    resp.seq mustBe seq
    resp.data
  }

  /**
    * @return true if there were no outstanding messages
    */
  def ensureNoExpected(
      failOnMessages: Boolean = false
  )(implicit testlog: LoggingAdapter): Boolean = {
    var count = 0
    try {
      while (true) {
        within(500 millis) {
          testlog.debug(s"${address} got ${this.expectProtocolMessage}")
          count = count + 1
        }
      }
      false
    } catch {
      case x: AssertionError =>
        if (count > 0) {
          testlog.debug(s"${address} There were ${count} messages, see log")
          if (failOnMessages) {
            fail(s"${address} There were ${count} messages, see log")
          }
          false
        } else {
          true
        }
    }
  }

  def terminate(
      max: FiniteDuration = 10 seconds
  )(implicit testlog: LoggingAdapter) = {
    if (myAddress.isDefined) {
      wsClient.inProbe.within(max) {
        wsClient.sendCompletion()
        wsClient.expectCompletion()
      }
      myAddress = None
    }
  }
}

object WebsocketClient {

  def ensureNoMessage(failOnMessage: Boolean, clients: WebsocketClient*)(
      implicit testlog: LoggingAdapter
  ): Boolean = {
    val rc = clients
      .map(wc => wc.ensureNoExpected(false))
      .foldLeft(true)((ac, b) => ac && b)
    if (!rc && failOnMessage) {
      testlog.info(
        "There were messages on some of the clients, " + clients
          .map(wc => wc.address)
          .mkString(", ")
      )
      fail(
        "There were messages on some of the clients, " + clients
          .map(wc => wc.address)
          .mkString(", ")
      )
    }
    rc
  }
}

object WebsocketClientImplicits {

  implicit class WebsocketClientTester(private val wc: WebsocketClient)
      extends AnyVal {
    import Assertions._

    def testIgnoreJoinLookForUpdate(
        mat: MatchDuplicate
    )(implicit testlog: LoggingAdapter): Unit = {
      while (
        wc.expectUnsolicitedMessage match {
          case uteam: UpdateDuplicateTeam =>
            testlog.debug(
              s"${wc.address} Ignored unexpected response from the monitor: ${uteam}"
            )
            true
          case upict: UpdateDuplicatePicture =>
            testlog.debug(
              s"${wc.address} Ignored unexpected response from the monitor: ${upict}"
            )
            true
          case upict: UpdateDuplicatePictures =>
            testlog.debug(
              s"${wc.address} Ignored unexpected response from the monitor: ${upict}"
            )
            true
          case uboard: UpdateDuplicateHand =>
            testlog.debug(
              s"${wc.address} Ignored unexpected response from the monitor: ${uboard}"
            )
            true
          case UpdateDuplicate(mp) =>
            assert(mat.equalsIgnoreModifyTime(mp))
            false

          case upict: UpdateIndividualDuplicatePicture =>
            fail("Unexpected response from the monitor: " + upict)
          case upict: UpdateIndividualDuplicatePictures =>
            fail("Unexpected response from the monitor: " + upict)
          case uboard: UpdateIndividualDuplicateHand =>
            fail("Unexpected response from the monitor: " + uboard)
          case UpdateIndividualDuplicate(mp) =>
            fail("Unexpected response from the monitor: " + mp)

          case pj: MonitorJoined =>
            testlog.debug(
              s"${wc.address} Ignored Unexpected response from the monitor: ${pj}"
            )
            true
          case pl: MonitorLeft =>
            fail(s"${wc.address} Unexpected response from the monitor: ${pl}")
          case nd: NoData =>
            fail(s"${wc.address} Unexpected response from the monitor: ${nd}")
          case m: UpdateChicago =>
            fail(s"${wc.address} Unexpected response from the monitor: $m")
          case m: UpdateChicagoRound =>
            fail(s"${wc.address} Unexpected response from the monitor: $m")
          case m: UpdateChicagoHand =>
            fail(s"${wc.address} Unexpected response from the monitor: $m")
          case m: UpdateRubber =>
            fail(s"${wc.address} Unexpected response from the monitor: $m")
          case m: UpdateRubberHand =>
            fail("Unexpected response from the monitor: " + m)
        }
      ) {}
    }

    def testUpdate(
        mat: MatchDuplicate
    )(implicit testlog: LoggingAdapter): Assertion = {
      wc.expectUnsolicitedMessage match {
        case uteam: UpdateDuplicateTeam =>
          fail(
            s"${wc.address} Ignored unexpected response from the monitor: ${uteam}"
          )
        case upict: UpdateDuplicatePicture =>
          fail(
            s"${wc.address} Ignored unexpected response from the monitor: ${upict}"
          )
        case upict: UpdateDuplicatePictures =>
          fail(
            s"${wc.address} Ignored unexpected response from the monitor: ${upict}"
          )
        case uboard: UpdateDuplicateHand =>
          fail(
            s"${wc.address} Ignored unexpected response from the monitor: ${uboard}"
          )
        case UpdateDuplicate(mp) =>
          testlog.debug("mat: " + mat)
          testlog.debug("mp : " + mp)
          assert(mat.equalsIgnoreModifyTime(mp))

        case upict: UpdateIndividualDuplicatePicture =>
          fail("Unexpected response from the monitor: " + upict)
        case upict: UpdateIndividualDuplicatePictures =>
          fail("Unexpected response from the monitor: " + upict)
        case uboard: UpdateIndividualDuplicateHand =>
          fail("Unexpected response from the monitor: " + uboard)
        case UpdateIndividualDuplicate(mp) =>
          fail("Unexpected response from the monitor: " + mp)

        case pj: MonitorJoined =>
          fail(s"${wc.address} Unexpected response from the monitor: ${pj}")
        case pl: MonitorLeft =>
          fail(s"${wc.address} Unexpected response from the monitor: ${pl}")
        case nd: NoData =>
          fail(s"${wc.address} Unexpected response from the monitor: ${nd}")
        case m: UpdateChicago =>
          fail(s"${wc.address} Unexpected response from the monitor: $m")
        case m: UpdateChicagoRound =>
          fail(s"${wc.address} Unexpected response from the monitor: $m")
        case m: UpdateChicagoHand =>
          fail(s"${wc.address} Unexpected response from the monitor: $m")
        case m: UpdateRubber =>
          fail(s"${wc.address} Unexpected response from the monitor: $m")
        case m: UpdateRubberHand =>
          fail("Unexpected response from the monitor: " + m)
      }
    }

    def testJoin(implicit testlog: LoggingAdapter): Unit = {
      wc.expectUnsolicitedMessage match {
        case uteam: UpdateDuplicateTeam =>
          fail(
            s"${wc.address} Ignored unexpected response from the monitor: ${uteam}"
          )
        case upict: UpdateDuplicatePicture =>
          fail(
            s"${wc.address} Ignored unexpected response from the monitor: ${upict}"
          )
        case upict: UpdateDuplicatePictures =>
          fail(
            s"${wc.address} Ignored unexpected response from the monitor: ${upict}"
          )
        case uboard: UpdateDuplicateHand =>
          fail(s"${wc.address} Unexpected response from the monitor: ${uboard}")
        case UpdateDuplicate(mp) =>
          fail(s"${wc.address} Unexpected response from the monitor: ${mp}")

        case upict: UpdateIndividualDuplicatePicture =>
          fail("Unexpected response from the monitor: " + upict)
        case upict: UpdateIndividualDuplicatePictures =>
          fail("Unexpected response from the monitor: " + upict)
        case uboard: UpdateIndividualDuplicateHand =>
          fail("Unexpected response from the monitor: " + uboard)
        case UpdateIndividualDuplicate(mp) =>
          fail("Unexpected response from the monitor: " + mp)

        case pl: MonitorLeft =>
          fail(s"${wc.address} Unexpected response from the monitor: ${pl}")
        case pj: MonitorJoined =>
          testlog.debug(s"${wc.address} Got the join: ${pj}")
        case nd: NoData =>
          fail(s"${wc.address} Unexpected response from the monitor: ${nd}")
        case m: UpdateChicago =>
          fail(s"${wc.address} Unexpected response from the monitor: $m")
        case m: UpdateChicagoRound =>
          fail(s"${wc.address} Unexpected response from the monitor: $m")
        case m: UpdateChicagoHand =>
          fail(s"${wc.address} Unexpected response from the monitor: $m")
        case m: UpdateRubber =>
          fail(s"${wc.address} Unexpected response from the monitor: $m")
        case m: UpdateRubberHand =>
          fail("Unexpected response from the monitor: " + m)
      }
    }

    def testLeft(implicit testlog: LoggingAdapter): Unit = {
      wc.expectUnsolicitedMessage match {
        case uteam: UpdateDuplicateTeam =>
          fail(
            s"${wc.address} Ignored unexpected response from the monitor: ${uteam}"
          )
        case upict: UpdateDuplicatePicture =>
          fail(
            s"${wc.address} Ignored unexpected response from the monitor: ${upict}"
          )
        case upict: UpdateDuplicatePictures =>
          fail(
            s"${wc.address} Ignored unexpected response from the monitor: ${upict}"
          )
        case uboard: UpdateDuplicateHand =>
          fail(s"${wc.address} Unexpected response from the monitor: ${uboard}")
        case UpdateDuplicate(mp) =>
          fail(s"${wc.address} Unexpected response from the monitor: ${mp}")

        case upict: UpdateIndividualDuplicatePicture =>
          fail("Unexpected response from the monitor: " + upict)
        case upict: UpdateIndividualDuplicatePictures =>
          fail("Unexpected response from the monitor: " + upict)
        case uboard: UpdateIndividualDuplicateHand =>
          fail("Unexpected response from the monitor: " + uboard)
        case UpdateIndividualDuplicate(mp) =>
          fail("Unexpected response from the monitor: " + mp)

        case pl: MonitorLeft =>
          testlog.debug(s"${wc.address} Got the left: ${pl}")
        case pj: MonitorJoined =>
          fail(s"${wc.address} Unexpected response from the monitor: ${pj}")
        case nd: NoData =>
          fail(s"${wc.address} Unexpected response from the monitor: ${nd}")
        case m: UpdateChicago =>
          fail(s"${wc.address} Unexpected response from the monitor: $m")
        case m: UpdateChicagoRound =>
          fail(s"${wc.address} Unexpected response from the monitor: $m")
        case m: UpdateChicagoHand =>
          fail(s"${wc.address} Unexpected response from the monitor: $m")
        case m: UpdateRubber =>
          fail(s"${wc.address} Unexpected response from the monitor: $m")
        case m: UpdateRubberHand =>
          fail("Unexpected response from the monitor: " + m)
      }
    }

  }

}
