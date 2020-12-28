package com.github.thebridsk.bridge.server.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.server.service.MyService
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.data.bridge.South
import com.github.thebridsk.bridge.data.IndividualDuplicate
import com.github.thebridsk.bridge.server.test.backend.BridgeServiceTesting
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.MediaTypes
import com.github.thebridsk.bridge.data.IndividualDuplicateHand
import com.github.thebridsk.bridge.data.bridge.Spades
import com.github.thebridsk.bridge.data.bridge.Doubled
import com.github.thebridsk.bridge.server.backend.BridgeServiceInMemory
import akka.http.scaladsl.testkit.WSProbe
import com.github.thebridsk.bridge.data.websocket.Protocol
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.model.ws.BinaryMessage
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateDuplicateTeam
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateDuplicateHand
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateDuplicate
import com.github.thebridsk.bridge.data.websocket.Protocol.MonitorJoined
import com.github.thebridsk.bridge.data.websocket.Protocol.MonitorLeft
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol
import com.github.thebridsk.bridge.data.websocket.Protocol.NoData
import akka.event.Logging
import com.github.thebridsk.bridge.server.rest.ServerPort
import com.github.thebridsk.bridge.data.websocket.Protocol.ToServerMessage
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.Send
import scala.reflect.ClassTag
import com.github.thebridsk.bridge.server.backend.resource.ChangeContext
import com.github.thebridsk.bridge.server.backend.resource.CreateChangeContext
import com.github.thebridsk.bridge.server.backend.resource.UpdateChangeContext
import com.github.thebridsk.bridge.server.backend.resource.DeleteChangeContext
import com.github.thebridsk.bridge.server.backend.resource.StoreListener
import com.github.thebridsk.bridge.server.backend.resource.Store
import com.github.thebridsk.bridge.data.VersionedInstance
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateRubber
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateChicago
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateChicagoRound
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateChicagoHand
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateRubberHand
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateDuplicatePicture
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateDuplicatePictures
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.AttributeKey
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateIndividualDuplicatePictures
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateIndividualDuplicatePicture
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateIndividualDuplicate
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateIndividualDuplicateHand
import com.github.thebridsk.bridge.data.IndividualBoard
import com.github.thebridsk.bridge.data.websocket.Protocol.StartMonitorIndividualDuplicate

class TestIndividualDuplicateRestSpec
    extends AnyFlatSpec
    with ScalatestRouteTest
    with Matchers
    with MyService
    with RoutingSpec {

  import TestIndividualDuplicateRestSpecImplicits._

  TestStartLogging.startLogging()

  val restService = new BridgeServiceInMemory("test")

  val httpport = 8080
  override def ports: ServerPort = ServerPort(Option(httpport), None)

  implicit lazy val actorSystem = system //scalafix:ok ExplicitResultTypes
  implicit lazy val actorExecutor = executor //scalafix:ok ExplicitResultTypes
  implicit lazy val actorMaterializer =
    materializer //scalafix:ok ExplicitResultTypes

  lazy val testlog: LoggingAdapter =
    Logging(actorSystem, classOf[TestDuplicateRestSpec])

  behavior of "MyService REST for individualduplicate"

  class ListenerStatus[T](implicit classT: ClassTag[T]) extends StoreListener {

    var lastCreate: Option[T] = None
    var lastUpdate: Option[T] = None
    var lastDelete: Option[T] = None

    def reset(): Unit = {
      lastCreate = None
      lastUpdate = None
      lastDelete = None
    }

    def getIndividualDuplicate(context: ChangeContext): Option[T] = {
      log
      testlog.debug("Got a change: " + context)
      val d = context.changes.headOption match {
        case Some(cd) =>
          (cd match {
            case CreateChangeContext(newValue, parentField) => newValue
            case UpdateChangeContext(newValue, parentField) => newValue
            case DeleteChangeContext(oldValue, parentField) => oldValue
          }) match {
            case md: T => Some(md)
            case _     => None
          }
        case None => None
      }
      testlog.debug("Found change: " + d)
      d.map(a => a.asInstanceOf[T])
    }

    override def create(context: ChangeContext): Unit = {
      lastCreate = getIndividualDuplicate(context);
      testlog.debug("Got a create change: " + context)
    }
    override def update(context: ChangeContext): Unit = {
      lastUpdate = getIndividualDuplicate(context);
      testlog.debug("Got a update change: " + context)
    }
    override def delete(context: ChangeContext): Unit = {
      lastDelete = getIndividualDuplicate(context);
      testlog.debug("Got a delete change: " + context)
    }

  }

  def withListener(f: ListenerStatus[IndividualDuplicate] => Unit): Unit = {
    withListener(restService.individualduplicates, f)
  }

  def withListener[Id <: Comparable[Id], T <: VersionedInstance[T, T, Id]](
      store: Store[Id, T],
      f: ListenerStatus[T] => Unit
  )(implicit classT: ClassTag[T]): Unit = {
    val status = new ListenerStatus[T]
    try {
      store.addListener(status)
      f(status)
    } finally {
      store.removeListener(status)
    }
  }

  def testIgnoreJoinLookForUpdate(
      wsClient: WSProbe,
      mat: IndividualDuplicate
  ): Unit = {
    while (
      (wsClient.expectMessage() match {
        case TextMessage.Strict(s) =>
          val dp = DuplexProtocol.fromString(s)
          dp match {
            case DuplexProtocol.Unsolicited(data) => data
            case _ =>
              fail("Unexpected response from the monitor: " + dp)
          }
        case tm: TextMessage =>
          val s = collect(tm.textStream)(_ + _)
          val dp = DuplexProtocol.fromString(s)
          dp match {
            case DuplexProtocol.Unsolicited(data) => data
            case _ =>
              fail("Unexpected response from the monitor: " + dp)
          }
        case x: BinaryMessage =>
          val data = collect(x.dataStream)(_ ++ _)
          fail("Unexpected response from the monitor: " + x.getClass.getName)
      }) match {
        case uteam: UpdateDuplicateTeam =>
          testlog.debug(
            "Ignored unexpected response from the monitor: " + uteam
          )
          true
        case upict: UpdateDuplicatePicture =>
          testlog.debug(
            "Ignored unexpected response from the monitor: " + upict
          )
          true
        case upict: UpdateDuplicatePictures =>
          testlog.debug(
            "Ignored unexpected response from the monitor: " + upict
          )
          true
        case uboard: UpdateDuplicateHand =>
          testlog.debug(
            "Ignored unexpected response from the monitor: " + uboard
          )
          true
        case UpdateDuplicate(mp) =>
          testlog.debug(
            "Ignored unexpected response from the monitor: " + mp
          )
          true
        case upict: UpdateIndividualDuplicatePicture =>
          testlog.debug(
            "Ignored unexpected response from the monitor: " + upict
          )
          true
        case upict: UpdateIndividualDuplicatePictures =>
          testlog.debug(
            "Ignored unexpected response from the monitor: " + upict
          )
          true
        case uboard: UpdateIndividualDuplicateHand =>
          testlog.debug(
            "Ignored unexpected response from the monitor: " + uboard
          )
          true
        case UpdateIndividualDuplicate(mp) =>
          assert(mat.equalsIgnoreModifyTime(mp))
          false
        case uboard: UpdateChicagoRound =>
          testlog.debug(
            "Ignored unexpected response from the monitor: " + uboard
          )
          true
        case uboard: UpdateChicagoHand =>
          testlog.debug(
            "Ignored unexpected response from the monitor: " + uboard
          )
          true
        case pj: MonitorJoined =>
          testlog.debug("Ignored Unexpected response from the monitor: " + pj)
          true
        case pl: MonitorLeft =>
          fail("Unexpected response from the monitor: " + pl)
        case nd: NoData =>
          fail("Unexpected response from the monitor: " + nd)
        case m: UpdateChicago =>
          fail("Unexpected response from the monitor: " + m)
        case m: UpdateRubber =>
          fail("Unexpected response from the monitor: " + m)
        case m: UpdateRubberHand =>
          fail("Unexpected response from the monitor: " + m)
      }
    ) {}
  }

  def testUpdate(wsClient: WSProbe, mat: IndividualDuplicate): Any = {
    (wsClient.expectMessage() match {
      case TextMessage.Strict(s) =>
        val dp = DuplexProtocol.fromString(s)
        dp match {
          case DuplexProtocol.Unsolicited(data) => data
          case _ =>
            fail("Unexpected response from the monitor: " + dp)
        }
      case tm: TextMessage =>
        val s = collect(tm.textStream)(_ + _)
        val dp = DuplexProtocol.fromString(s)
        dp match {
          case DuplexProtocol.Unsolicited(data) => data
          case _ =>
            fail("Unexpected response from the monitor: " + dp)
        }
      case x: BinaryMessage =>
        val data = collect(x.dataStream)(_ ++ _)
        fail("Unexpected response from the monitor: " + x.getClass.getName)
    }) match {
      case uteam: UpdateDuplicateTeam =>
        testlog.debug("Ignored unexpected response from the monitor: " + uteam)
        true
      case upict: UpdateDuplicatePicture =>
        testlog.debug("Ignored unexpected response from the monitor: " + upict)
        true
      case upict: UpdateDuplicatePictures =>
        testlog.debug("Ignored unexpected response from the monitor: " + upict)
        true
      case uboard: UpdateDuplicateHand =>
        testlog.debug("Ignored unexpected response from the monitor: " + uboard)
        true
      case UpdateDuplicate(mp) =>
        testlog.debug(
          "Ignored unexpected response from the monitor: " + mp
        )
        true

      case upict: UpdateIndividualDuplicatePicture =>
        testlog.debug(
          "Ignored unexpected response from the monitor: " + upict
        )
        true
      case upict: UpdateIndividualDuplicatePictures =>
        testlog.debug(
          "Ignored unexpected response from the monitor: " + upict
        )
        true
      case uboard: UpdateIndividualDuplicateHand =>
        testlog.debug(
          "Ignored unexpected response from the monitor: " + uboard
        )
        true
      case UpdateIndividualDuplicate(mp) =>
        testlog.debug("mat: " + mat)
        testlog.debug("mp : " + mp)
        assert(mat.equalsIgnoreModifyTime(mp))

      case pj: MonitorJoined =>
        fail("Unexpected response from the monitor: " + pj)
      case pl: MonitorLeft =>
        fail("Unexpected response from the monitor: " + pl)
      case nd: NoData =>
        fail("Unexpected response from the monitor: " + nd)
      case m: UpdateChicago =>
        fail("Unexpected response from the monitor: " + m)
      case m: UpdateChicagoRound =>
        fail("Unexpected response from the monitor: " + m)
      case m: UpdateChicagoHand =>
        fail("Unexpected response from the monitor: " + m)
      case m: UpdateRubber =>
        fail("Unexpected response from the monitor: " + m)
      case m: UpdateRubberHand =>
        fail("Unexpected response from the monitor: " + m)
    }
  }

  def testJoin(wsClient: WSProbe): AnyVal = {
    (wsClient.expectMessage() match {
      case TextMessage.Strict(s) =>
        val dp = DuplexProtocol.fromString(s)
        dp match {
          case DuplexProtocol.Unsolicited(data) => data
          case _ =>
            fail("Unexpected response from the monitor: " + dp)
        }
      case tm: TextMessage =>
        val s = collect(tm.textStream)(_ + _)
        val dp = DuplexProtocol.fromString(s)
        dp match {
          case DuplexProtocol.Unsolicited(data) => data
          case _ =>
            fail("Unexpected response from the monitor: " + dp)
        }
      case x: BinaryMessage =>
        val data = collect(x.dataStream)(_ ++ _)
        fail("Unexpected response from the monitor: " + x.getClass.getName)
    }) match {
      case uteam: UpdateDuplicateTeam =>
        testlog.debug("Ignored unexpected response from the monitor: " + uteam)
        true
      case upict: UpdateDuplicatePicture =>
        testlog.debug("Ignored unexpected response from the monitor: " + upict)
        true
      case upict: UpdateDuplicatePictures =>
        testlog.debug("Ignored unexpected response from the monitor: " + upict)
        true
      case uboard: UpdateDuplicateHand =>
        fail("Unexpected response from the monitor: " + uboard)
      case UpdateDuplicate(mp) =>
        fail("Unexpected response from the monitor: " + mp)

      case upict: UpdateIndividualDuplicatePicture =>
        fail("Unexpected response from the monitor: " + upict)
      case upict: UpdateIndividualDuplicatePictures =>
        fail("Unexpected response from the monitor: " + upict)
      case uboard: UpdateIndividualDuplicateHand =>
        fail("Unexpected response from the monitor: " + uboard)
      case UpdateIndividualDuplicate(mp) =>
        fail("Unexpected response from the monitor: " + mp)

      case pl: MonitorLeft =>
        fail("Unexpected response from the monitor: " + pl)
      case pj: MonitorJoined =>
        testlog.debug("Got the join: " + pj)
      case nd: NoData =>
        fail("Unexpected response from the monitor: " + nd)
      case m: UpdateChicago =>
        fail("Unexpected response from the monitor: " + m)
      case m: UpdateChicagoRound =>
        fail(s"Unexpected response from the monitor: $m")
      case m: UpdateChicagoHand =>
        fail(s"Unexpected response from the monitor: $m")
      case m: UpdateRubber =>
        fail("Unexpected response from the monitor: " + m)
      case m: UpdateRubberHand =>
        fail("Unexpected response from the monitor: " + m)
    }
  }

  def testLeft(wsClient: WSProbe): AnyVal = {
    (wsClient.expectMessage() match {
      case TextMessage.Strict(s) =>
        val dp = DuplexProtocol.fromString(s)
        dp match {
          case DuplexProtocol.Unsolicited(data) => data
          case _ =>
            fail("Unexpected response from the monitor: " + dp)
        }
      case tm: TextMessage =>
        val s = collect(tm.textStream)(_ + _)
        val dp = DuplexProtocol.fromString(s)
        dp match {
          case DuplexProtocol.Unsolicited(data) => data
          case _ =>
            fail("Unexpected response from the monitor: " + dp)
        }
      case x: BinaryMessage =>
        val data = collect(x.dataStream)(_ ++ _)
        fail("Unexpected response from the monitor: " + x.getClass.getName)
    }) match {
      case uteam: UpdateDuplicateTeam =>
        testlog.debug("Ignored unexpected response from the monitor: " + uteam)
        true
      case upict: UpdateDuplicatePicture =>
        testlog.debug("Ignored unexpected response from the monitor: " + upict)
        true
      case upict: UpdateDuplicatePictures =>
        testlog.debug("Ignored unexpected response from the monitor: " + upict)
        true
      case uboard: UpdateDuplicateHand =>
        fail("Unexpected response from the monitor: " + uboard)
      case UpdateDuplicate(mp) =>
        fail("Unexpected response from the monitor: " + mp)

      case upict: UpdateIndividualDuplicatePicture =>
        fail("Unexpected response from the monitor: " + upict)
      case upict: UpdateIndividualDuplicatePictures =>
        fail("Unexpected response from the monitor: " + upict)
      case uboard: UpdateIndividualDuplicateHand =>
        fail("Unexpected response from the monitor: " + uboard)
      case UpdateIndividualDuplicate(mp) =>
        fail("Unexpected response from the monitor: " + mp)

      case pl: MonitorLeft =>
        testlog.debug("Got the left: " + pl)
      case pj: MonitorJoined =>
        fail("Unexpected response from the monitor: " + pj)
      case nd: NoData =>
        fail("Unexpected response from the monitor: " + nd)
      case m: UpdateChicago =>
        fail("Unexpected response from the monitor: " + m)
      case m: UpdateChicagoRound =>
        fail(s"Unexpected response from the monitor: $m")
      case m: UpdateChicagoHand =>
        fail(s"Unexpected response from the monitor: $m")
      case m: UpdateRubber =>
        fail("Unexpected response from the monitor: " + m)
      case m: UpdateRubberHand =>
        fail("Unexpected response from the monitor: " + m)
    }
  }

  def terminateWebsocket(wsClient: WSProbe): Unit = {
    import scala.concurrent.duration._
    import scala.language.postfixOps
    wsClient.inProbe.within(10 seconds) {
      wsClient.sendCompletion()
//      testLeft(wsClient)
      wsClient.expectCompletion()
    }

  }

  import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._

  var createdM1: Option[IndividualDuplicate] = None
  it should "return a IndividualDuplicate json object for match 1 for POST request to /v1/rest/individualduplicates" in withListener(
    listenerstatus => {
      Post(
        "/v1/rest/individualduplicates?default",
        IndividualDuplicate.create(IndividualDuplicate.id("I1"))
      ).withAttributes(remoteAddress) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe Created
        mediaType mustBe MediaTypes.`application/json`
        header("Location") match {
          case Some(h) =>
            h.value() mustBe "http://example.com/v1/rest/individualduplicates/I1"
          case None =>
            fail("Location header was not found in response")
        }
        val md = responseAs[IndividualDuplicate]
        createdM1 = Some(md)
        md.players.length mustBe 8
        for (id <- 1 to 21) {
          val board = md.getBoard(IndividualBoard.id(id))
          assert(board.isDefined, s"- Board $id was not found")
          val b = board.get
          assert(
            b.hands.size == 2,
            s"- Board $id did not have 2 hands, there were ${b.hands.size}"
          )
        }
        assert(!listenerstatus.lastCreate.isEmpty)
        assert(listenerstatus.lastUpdate.isEmpty)
        assert(listenerstatus.lastDelete.isEmpty)
      }
    }
  )

  it should "return a IndividualDuplicate json object for match 1 for PUT request to /v1/rest/individualduplicates/I1" in withListener(
    listenerstatus => {
      import scala.concurrent.duration._
      import scala.language.postfixOps
      val wsClient = WSProbe()
      WS("/v1/individual/ws", wsClient.flow, Protocol.DuplicateBridge :: Nil)
        .addAttributes(remoteAddress) ~> myRouteWithLogging ~>
        check {
          isWebSocketUpgrade mustBe true
          wsClient.inProbe.within(10 seconds) {
            testJoin(wsClient)
            // val cleanmd = new BridgeServiceInMemory("test").fillBoards(
            //   IndividualDuplicate.create(IndividualDuplicate.id("I1"))
            // )
            // testUpdate(wsClient,cleanmd)
          }
          wsClient.send(StartMonitorIndividualDuplicate(IndividualDuplicate.id("I1")))
          wsClient.inProbe.within(10 seconds) {
            testUpdate(wsClient, createdM1.get)
          }
          testlog.debug(
            "TestDuplicateRestSpec: updating I1 with monitoring active"
          )
          Put(
            "/v1/rest/individualduplicates/I1",
            BridgeServiceTesting.testingIndividualMatch
          ).withAttributes(remoteAddress) ~> myRouteWithLogging ~> check {
            handled mustBe true
            status mustBe NoContent
//          mediaType mustBe MediaTypes.`application/json`
//          assert( responseAs[IndividualDuplicate].equalsIgnoreModifyTime( BridgeServiceTesting.testingIndividualMatch ))
            assert(listenerstatus.lastCreate.isEmpty)
            assert(!listenerstatus.lastUpdate.isEmpty)
            assert(
              listenerstatus.lastUpdate.get.equalsIgnoreModifyTime(
                BridgeServiceTesting.testingIndividualMatch
              )
            )
            assert(listenerstatus.lastDelete.isEmpty)
          }
          wsClient.inProbe.within(10 seconds) {
//          testIgnoreJoinLookForUpdate(wsClient,BridgeServiceTesting.testingIndividualMatch)
//          testJoin(wsClient)
            testUpdate(wsClient, BridgeServiceTesting.testingIndividualMatch)
          }
          testlog.debug(
            "TestDuplicateRestSpec: finished updating I1 with monitoring active"
          )
          Get("/v1/rest/individualduplicates/I1").withAttributes(
            remoteAddress
          ) ~> myRouteWithLogging ~> check {
            handled mustBe true
            status mustBe OK
            mediaType mustBe MediaTypes.`application/json`
            assert(
              responseAs[IndividualDuplicate].equalsIgnoreModifyTime(
                BridgeServiceTesting.testingIndividualMatch
              )
            )
          }
          terminateWebsocket(wsClient)
        }
    }
  )

  it should "monitor should return match I1 after the Join message" in withListener(
    listenerstatus => {
      import scala.concurrent.duration._
      import scala.language.postfixOps
      val wsClient = WSProbe()
      WS("/v1/individual/ws", wsClient.flow, Protocol.DuplicateBridge :: Nil)
        .addAttributes(remoteAddress) ~> myRouteWithLogging ~>
        check {
          isWebSocketUpgrade mustBe true
          wsClient.inProbe.within(10 seconds) {
            testJoin(wsClient)
          }
//        wsClient.inProbe.within(10 seconds) {
//          testUpdate(wsClient,BridgeServiceTesting.testingIndividualMatch)
//        }
          terminateWebsocket(wsClient)
        }
    }
  )

  it should "return a IndividualDuplicate json object for match 1 for GET requests to /v1/rest/individualduplicates/I1" in withListener(
    listenerstatus => {
      Get("/v1/rest/individualduplicates/I1").withAttributes(
        remoteAddress
      ) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe OK
        mediaType mustBe MediaTypes.`application/json`
        assert(
          responseAs[IndividualDuplicate].equalsIgnoreModifyTime(
            BridgeServiceTesting.testingIndividualMatch
          )
        )
        assert(listenerstatus.lastCreate.isEmpty)
        assert(listenerstatus.lastUpdate.isEmpty)
        assert(listenerstatus.lastDelete.isEmpty)
      }
    }
  )

  it should "return a not found for match 2 for GET requests to /v1/rest/individualduplicates/I2" in {
    Get("/v1/rest/individualduplicates/I2").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage(
        "Did not find resource /individualduplicates/I2"
      )
    }
  }

  behavior of "MyService REST for names"

  it should "return a list of names for GET requests to /v1/rest/names" in {
    Get("/v1/rest/names").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      val resp = responseAs[List[String]]
      testlog.debug("names are " + resp)
      resp mustBe List("Ellen", "Ethan", "Nancy", "Norman", "Sally", "Sam", "Wayne", "Wilma")
    }
  }

  behavior of "MyService REST for duplicate boards"

  it should "return a Board json object for board 1 for GET requests to /v1/rest/individualduplicates/I1/boards/B1" in {
    Get("/v1/rest/individualduplicates/I1/boards/B1").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert(
        responseAs[IndividualBoard].equalsIgnoreModifyTime(
          BridgeServiceTesting.testingIndividualMatch.getBoard(IndividualBoard.id(1)).get
        )
      )
    }
  }

  it should "return a Board json object for board 2 for GET requests to /v1/rest/individualduplicates/I1/boards/B2" in {
    Get("/v1/rest/individualduplicates/I1/boards/B2").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert(
        responseAs[IndividualBoard].equalsIgnoreModifyTime(
          BridgeServiceTesting.testingIndividualMatch.getBoard(IndividualBoard.id(2)).get
        )
      )
    }
  }

  it should "return a Board json object for board 3 for GET requests to /v1/rest/individualduplicates/I1/boards/B3" in {
    Get("/v1/rest/individualduplicates/I1/boards/B3").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert(
        responseAs[IndividualBoard].equalsIgnoreModifyTime(
          IndividualBoard(IndividualBoard.id(3), false, true, South.pos, List())
        )
      )
    }
  }

  it should "return a not found for board 4 for GET requests to /v1/rest/individualduplicates/I1/boards/B4" in {
    Get("/v1/rest/individualduplicates/I1/boards/B4").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage(
        "Did not find resource /individualduplicates/I1/boards/B4"
      )
    }
  }

  it should "return a not found for match 2 for GET requests to /v1/rest/individualduplicates/I2/boards/B1" in {
    Get("/v1/rest/individualduplicates/I2/boards/B1").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage(
        "Did not find resource /individualduplicates/I2"
      )
    }
  }

  behavior of "MyService REST for duplicate hands"

  it should "return a hand json object for hand 1 for GET requests to /v1/rest/individualduplicates/I1/boards/B1/hands/p8" in {
    Get("/v1/rest/individualduplicates/I1/boards/B1/hands/p8").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      withClue("response is " + response) { status mustBe OK }
      mediaType mustBe MediaTypes.`application/json`
      assert(
        responseAs[IndividualDuplicateHand].equalsIgnoreModifyTime(
          IndividualDuplicateHand(
            Some(
              Hand.create(
                "p8",
                7,
                Spades.suit,
                Doubled.doubled,
                North.pos,
                false,
                false,
                true,
                7
              )
            ),
            Table.id(1),
            1,
            IndividualBoard.id(1),
            8,1,5,7
          )
        )
      )
    }
  }

  it should "return a not found for hand 3 for GET requests to /v1/rest/individualduplicates/I1/boards/B1/hands/T4" in {
    Get("/v1/rest/individualduplicates/I1/boards/B1/hands/T4").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage(
        "Did not find resource /individualduplicates/I1/boards/B1/hands/T4"
      )
    }
  }

  it should "return a not found for board 4 for GET requests to /v1/rest/individualduplicates/I1/boards/B4/hands/T1" in {
    Get("/v1/rest/individualduplicates/I1/boards/B4/hands/T1").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage(
        "Did not find resource /individualduplicates/I1/boards/B4"
      )
    }
  }

  it should "return a not found for match 2 for GET requests to /v1/rest/individualduplicates/I2/boards/B1" in {
    Get("/v1/rest/individualduplicates/I2/boards/B1").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage(
        "Did not find resource /individualduplicates/I2"
      )
    }
  }

  it should "return a hand json object for POST requests to /v1/rest/individualduplicates/I1/boards/B2/hands" in withListener(
    listenerstatus => {
      val hand = IndividualDuplicateHand(
        Some(
          Hand.create(
            "p8",
            7,
            Spades.suit,
            Doubled.doubled,
            North.pos,
            false,
            false,
            false,
            1
          )
        ),
        Table.id(1),
        1,
        IndividualBoard.id(2),
        8,1,5,7
      )
      Post("/v1/rest/individualduplicates/I1/boards/B2/hands", hand).withAttributes(
        remoteAddress
      ) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe Created
        header("Location") match {
          case Some(h) =>
            h.value() mustBe "http://example.com/v1/rest/individualduplicates/I1/boards/B2/hands/p8"
          case None =>
            fail("Location header was not found in response")
        }
        mediaType mustBe MediaTypes.`application/json`
        assert(responseAs[IndividualDuplicateHand].equalsIgnoreModifyTime(hand))
        assert(!listenerstatus.lastCreate.isEmpty)
        assert(listenerstatus.lastUpdate.isEmpty)
        val newMatch =
          BridgeServiceTesting.testingIndividualMatch.updateHand(IndividualBoard.id(2), hand)
        testlog.debug("newMatch is " + newMatch)
        testlog.debug("fromList is " + listenerstatus.lastCreate.get)
        assert(listenerstatus.lastCreate.get.equalsIgnoreModifyTime(newMatch))
        assert(listenerstatus.lastDelete.isEmpty)
      }
    }
  )

  it should "return a deleted for Delete requests to /v1/rest/individualduplicates/I1/boards/B2/hands/p8" in withListener(
    listenerstatus => {
      var m1: Option[IndividualDuplicate] = None
      Get("/v1/rest/individualduplicates/I1").withAttributes(
        remoteAddress
      ) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe OK
        mediaType mustBe MediaTypes.`application/json`
        val r = responseAs[IndividualDuplicate]
        m1 = Some(r)
      }
      Delete("/v1/rest/individualduplicates/I1/boards/B2/hands/p8").withAttributes(
        remoteAddress
      ) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe NoContent
//      mediaType mustBe MediaTypes.`application/json`
//      responseAs[RestMessage] mustBe RestMessage("Deleted Hand T3")
        assert(listenerstatus.lastCreate.isEmpty)
        assert(listenerstatus.lastUpdate.isEmpty)
//      assert( listenerstatus.lastUpdate.get.equalsIgnoreModifyTime( BridgeServiceTesting.testingIndividualMatch ))
        assert(!listenerstatus.lastDelete.isEmpty)
        listenerstatus.lastDelete.get.id mustBe BridgeServiceTesting.testingIndividualMatch.id

        val m1withhanddeleted = m1.get.updateBoard(
          m1.get.getBoard(IndividualBoard.id(2)).get.deleteHand(IndividualDuplicateHand.id(8))
        )
        assert(
          listenerstatus.lastDelete.get.equalsIgnoreModifyTime(
            m1withhanddeleted
          )
        )
      }
    }
  )

  behavior of "MyService REST for duplicate when using delete"

  it should "return a deleted for Delete requests to /v1/rest/individualduplicates/I1" in withListener(
    listenerstatus => {
      var m1: Option[IndividualDuplicate] = None
      Get("/v1/rest/individualduplicates/I1").withAttributes(
        remoteAddress
      ) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe OK
        mediaType mustBe MediaTypes.`application/json`
        val r = responseAs[IndividualDuplicate]
        m1 = Some(r)
      }
      Delete("/v1/rest/individualduplicates/I1").withAttributes(
        remoteAddress
      ) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe NoContent
//      mediaType mustBe MediaTypes.`application/json`
//      responseAs[RestMessage] mustBe RestMessage("Deleted IndividualDuplicate I1")
        assert(listenerstatus.lastCreate.isEmpty)
        assert(listenerstatus.lastUpdate.isEmpty)
        assert(!listenerstatus.lastDelete.isEmpty)
        listenerstatus.lastDelete.get.id mustBe BridgeServiceTesting.testingIndividualMatch.id
        assert(listenerstatus.lastDelete.get.equalsIgnoreModifyTime(m1.get))
      }
    }
  )

  behavior of "MyService REST for names at end"

  it should "return a list of names for GET requests to /v1/rest/names" in {
    Get("/v1/rest/names").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      val resp = responseAs[List[String]]
      testlog.debug("names are " + resp)
      resp mustBe List()
    }
  }

  behavior of "MyService REST for individual duplicates"

  it should "return a IndividualDuplicate json object for a POST request to /v1/rest/individualduplicates?default" in withListener(
    listenerstatus => {
      Post(
        "/v1/rest/individualduplicates?default",
        IndividualDuplicate.create(IndividualDuplicate.id("I1"))
      ).withAttributes(remoteAddress) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe Created
        mediaType mustBe MediaTypes.`application/json`
        val md = responseAs[IndividualDuplicate]
        md.boards.length mustBe 21
        for (id <- 1 to 21) {
          val board = md.getBoard(IndividualBoard.id(id))
          assert(board.isDefined, s"- Board $id was not found")
          val b = board.get
          assert(
            b.hands.size == 2,
            s"- Board $id did not have 2 hands, there were ${b.hands.size}"
          )
        }
        assert(!listenerstatus.lastCreate.isEmpty)
        assert(listenerstatus.lastUpdate.isEmpty)
        assert(listenerstatus.lastDelete.isEmpty)
      }
    }
  )

}

object TestIndividualDuplicateRestSpecImplicits {
  implicit class WebSocketSender(private val wsClient: WSProbe) extends AnyVal {

    def send(msg: ToServerMessage): Unit = {
      val data = Send(msg)
      wsClient.sendMessage(DuplexProtocol.toString(data))
    }
  }

  implicit class WrapHttpRequest(private val req: HttpRequest) extends AnyVal {
    def addAttributes(map: Map[AttributeKey[_], _]): HttpRequest = {
      val old = req.attributes
      req.withAttributes(old ++ map)
    }
  }

}
