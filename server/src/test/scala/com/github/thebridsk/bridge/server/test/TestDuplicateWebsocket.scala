package com.github.thebridsk.bridge.server.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.server.service.MyService
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.data.bridge.South
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.server.test.backend.BridgeServiceTesting
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest._
import java.net.InetAddress
import akka.http.scaladsl.model.MediaTypes
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.bridge.Spades
import com.github.thebridsk.bridge.data.bridge.Doubled
import com.github.thebridsk.bridge.server.backend.BridgeServiceInMemory
import com.github.thebridsk.bridge.data.websocket.Protocol
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateDuplicateTeam
import com.github.thebridsk.bridge.data.websocket.Protocol.UpdateDuplicateHand
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol
import akka.event.Logging
import com.github.thebridsk.bridge.server.rest.ServerPort
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.data.websocket.Protocol.StartMonitorDuplicate
import com.github.thebridsk.bridge.server.test.util.WebsocketClient
import com.github.thebridsk.bridge.server.test.util.WebsocketClientImplicits
import com.github.thebridsk.bridge.server.backend.StoreMonitor
import com.github.thebridsk.bridge.server.backend.StoreMonitor.ReceivedMessage
import com.github.thebridsk.bridge.server.backend.StoreMonitor.NewParticipant
import akka.actor.Actor
import com.github.thebridsk.bridge.server.backend.resource.StoreListener
import com.github.thebridsk.bridge.server.backend.resource.ChangeContext
import com.github.thebridsk.bridge.server.backend.resource.CreateChangeContext
import com.github.thebridsk.bridge.server.backend.resource.UpdateChangeContext
import com.github.thebridsk.bridge.server.backend.resource.DeleteChangeContext
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.AttributeKey

class TestDuplicateWebsocket
    extends AnyFlatSpec
    with ScalatestRouteTest
    with Matchers
    with MyService
    with BeforeAndAfterAll
    with RoutingSpec {

  implicit val me: TestDuplicateWebsocket = this

  import WebsocketClientImplicits._
  import scala.concurrent.duration._
  import scala.language.postfixOps

  TestStartLogging.startLogging()

  val restService = new BridgeServiceInMemory("test")

  val httpport = 8080
  override def ports: ServerPort = ServerPort(Option(httpport), None)

  implicit lazy val actorSystem = system //scalafix:ok ExplicitResultTypes
  implicit lazy val actorExecutor = executor //scalafix:ok ExplicitResultTypes
  implicit lazy val actorMaterializer =
    materializer //scalafix:ok ExplicitResultTypes

  implicit lazy val testlog: LoggingAdapter =
    Logging(actorSystem, classOf[TestDuplicateWebsocket])

  val remoteAddress1: Map[AttributeKey[_], Any] =
    remoteAddress(InetAddress.getLocalHost, 11111)
  val remoteAddress2: Map[AttributeKey[_], Any] =
    remoteAddress(InetAddress.getLocalHost, 22222)
  val remoteAddress3: Map[AttributeKey[_], Any] =
    remoteAddress(InetAddress.getLocalHost, 33333)

  var client1: WebsocketClient = null
  var client2: WebsocketClient = null
  var client3: WebsocketClient = null

  val team1: Team.Id = Team.id(1)
  val team2: Team.Id = Team.id(2)
  val team3: Team.Id = Team.id(3)
  val team4: Team.Id = Team.id(4)

  override def beforeAll(): Unit = {
    client1 = new WebsocketClient
    client2 = new WebsocketClient
    client3 = new WebsocketClient
  }

  override def afterAll(): Unit = {
    client1.terminate()
    client1 = null
    client2.terminate()
    client2 = null
    client3.terminate()
    client3 = null
  }

  def withHook[T](hook: Hook)(f: => T): T = {
    class HookM(hook: Hook) {
      StoreMonitor.setTestHook(hook.hook _)
      def close() = StoreMonitor.unsetTestHook()
    }
    val h = new HookM(hook)
    try {
      f
    } catch {
      case x: Throwable =>
        testlog.error("withHook execution exception", x)
        throw x
    } finally {
      h.close()
    }
  }

  trait Hook {
    def hook(actor: Actor, msg: Any): Unit
  }

  behavior of "MyService REST for duplicate"

  class ListenerStatus extends StoreListener {

    var lastCreate: Option[MatchDuplicate] = None
    var lastUpdate: Option[MatchDuplicate] = None
    var lastDelete: Option[MatchDuplicate] = None

    def reset(): Unit = {
      lastCreate = None
      lastUpdate = None
      lastDelete = None
    }

    def getMatchDuplicate(context: ChangeContext): Option[MatchDuplicate] = {
      log
      testlog.debug("Got a change: " + context)
      val d = context.changes.headOption match {
        case Some(cd) =>
          (cd match {
            case CreateChangeContext(newValue, parentField) => newValue
            case UpdateChangeContext(newValue, parentField) => newValue
            case DeleteChangeContext(oldValue, parentField) => oldValue
          }) match {
            case md: MatchDuplicate => Some(md)
            case _                  => None
          }
        case None => None
      }
      testlog.debug("Found change: " + d)
      d
    }

    override def create(context: ChangeContext): Unit = {
      lastCreate = getMatchDuplicate(context);
      testlog.debug("Got a create change: " + context)
    }
    override def update(context: ChangeContext): Unit = {
      lastUpdate = getMatchDuplicate(context);
      testlog.debug("Got a update change: " + context)
    }
    override def delete(context: ChangeContext): Unit = {
      lastDelete = getMatchDuplicate(context);
      testlog.debug("Got a delete change: " + context)
    }

  }

  def withListener(f: ListenerStatus => Unit): Unit = {
    val status = new ListenerStatus
    try {
      restService.duplicates.addListener(status)
      f(status)
    } finally {
      restService.duplicates.removeListener(status)
    }
  }

  import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._

  var createdM1: Option[MatchDuplicate] = None
  it should "return a MatchDuplicate json object for match 1 for POST request to /v1/rest/duplicates" in withListener(
    listenerstatus => {
      Post(
        "/v1/rest/duplicates?default",
        MatchDuplicate.create(MatchDuplicate.id(1))
      ).withAttributes(remoteAddress) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe Created
        mediaType mustBe MediaTypes.`application/json`
        header("Location") match {
          case Some(h) =>
            h.value() mustBe "http://example.com/v1/rest/duplicates/M1"
          case None =>
            fail("Location header was not found in response")
        }
        val md = responseAs[MatchDuplicate]
        createdM1 = Some(md)
        for (id <- 1 to 4) {
          val tid = Team.id(id)
          assert(
            md.getTeam(tid).get.equalsIgnoreModifyTime(Team.create(tid, "", ""))
          )
        }
        for (id <- 1 to 18) {
          val board = md.getBoard(Board.id(id))
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

  it should "return a MatchDuplicate json object for match 1 for PUT request to /v1/rest/duplicates/M1" in withListener(
    listenerstatus => {
      import scala.concurrent.duration._
      import scala.language.postfixOps

      var gotJoin = false
      var gotStartMonitor = false

      def process(msg: Protocol.ToServerMessage) = {
        testlog.info(s"""withHook got $msg""")
        msg match {
          case _: StartMonitorDuplicate => gotStartMonitor = true
          case _                        =>
        }
      }

      testlog.info("setting up withHook")

      withHook(new Hook {
        def hook(actor: Actor, msg: Any) = {
          msg match {
            case NewParticipant(name, subscriber) =>
              gotJoin = true
            case ReceivedMessage(senderid, message) =>
              DuplexProtocol.fromString(message) match {
                case DuplexProtocol.Send(data)              => process(data)
                case DuplexProtocol.Request(data, seq, ack) => process(data)
                case _                                      =>
              }
            case _ =>
          }
        }
      }) {
        testlog.info("withHook starting")
        client3.connect(remoteAddress3, myRouteWithLogging)
        client1.connect(remoteAddress1, myRouteWithLogging)
        client3.testJoin
        testlog.info(
          s"withHook gotJoin=$gotJoin gotStartMonitor=$gotStartMonitor"
        )
        gotJoin mustBe true
        gotStartMonitor mustBe false
        client1.send(StartMonitorDuplicate(MatchDuplicate.id(1)))
        client1.within(10 seconds) {
          client1.testUpdate(createdM1.get)
        }
        testlog.info(
          s"withHook gotJoin=$gotJoin gotStartMonitor=$gotStartMonitor"
        )
        gotStartMonitor mustBe true
        Put(
          "/v1/rest/duplicates/M1",
          BridgeServiceTesting.testingMatch
        ).withAttributes(remoteAddress) ~> myRouteWithLogging ~> check {
          handled mustBe true
          status mustBe NoContent
//        mediaType mustBe MediaTypes.`application/json`
//        responseAs[MatchDuplicate].equalsIgnoreModifyTime( BridgeServiceTesting.testingMatch, true )
          assert(listenerstatus.lastCreate.isEmpty)
          assert(!listenerstatus.lastUpdate.isEmpty)
          try {
            listenerstatus.lastUpdate.get
              .equalsIgnoreModifyTime(BridgeServiceTesting.testingMatch, true)
          } catch {
            case x: Exception =>
              println(s"lastUpdate=${listenerstatus.lastUpdate.get}")
              println(s"testingMatch=${BridgeServiceTesting.testingMatch}")
              throw x
          }
          assert(listenerstatus.lastDelete.isEmpty)
        }
        client1.within(10 seconds) {
          client1.testUpdate(BridgeServiceTesting.testingMatch)
        }
        Get("/v1/rest/duplicates/M1").withAttributes(
          remoteAddress
        ) ~> myRouteWithLogging ~> check {
          handled mustBe true
          status mustBe OK
          mediaType mustBe MediaTypes.`application/json`
          responseAs[MatchDuplicate]
            .equalsIgnoreModifyTime(BridgeServiceTesting.testingMatch, true)
        }
        WebsocketClient.ensureNoMessage(true, client1, client3)
        testlog.info("withHook done")
      }
    }
  )

  it should "return a not found for match 2 for GET requests to /v1/rest/duplicates/M2" in {
    Get("/v1/rest/duplicates/M2").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage(
        "Did not find resource /duplicates/M2"
      )
    }
    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

  it should "return a MatchDuplicate json object for match 2 for POST request to /v1/rest/duplicates" in withListener(
    listenerstatus => {
      Post(
        "/v1/rest/duplicates?default",
        MatchDuplicate.create(MatchDuplicate.id(2))
      ).withAttributes(remoteAddress) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe Created
        mediaType mustBe MediaTypes.`application/json`
        val md = responseAs[MatchDuplicate]
        header("Location") match {
          case Some(h) =>
            h.value() mustBe s"http://example.com/v1/rest/duplicates/${md.id.id}"
          case None =>
            fail("Location header was not found in response")
        }
        client2.connect(remoteAddress2, myRouteWithLogging)
        client1.within(10 seconds) { client1.testJoin }
        client3.within(10 seconds) { client3.testJoin }

        client2.send(StartMonitorDuplicate(md.id))
        client2.within(10 seconds) {
          client2.testUpdate(md)
        }
      }
      WebsocketClient.ensureNoMessage(true, client1, client2, client3)
    }
  )

  it should "return a MatchDuplicate json object for match 1 for GET requests to /v1/rest/duplicates/M1" in withListener(
    listenerstatus => {
      Get("/v1/rest/duplicates/M1").withAttributes(
        remoteAddress
      ) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe OK
        mediaType mustBe MediaTypes.`application/json`
        responseAs[MatchDuplicate]
          .equalsIgnoreModifyTime(BridgeServiceTesting.testingMatch, true)
        assert(listenerstatus.lastCreate.isEmpty)
        assert(listenerstatus.lastUpdate.isEmpty)
        assert(listenerstatus.lastDelete.isEmpty)
      }
      WebsocketClient.ensureNoMessage(true, client1, client2, client3)
    }
  )

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
      resp mustBe List(
        "Ellen",
        "Ethan",
        "Nancy",
        "Norman",
        "Sally",
        "Sam",
        "Wayne",
        "Wilma"
      )
    }
    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

  behavior of "MyService REST for duplicate boards"

  it should "return a Board json object for board 1 for GET requests to /v1/rest/duplicates/M1/boards/B1" in {
    Get("/v1/rest/duplicates/M1/boards/B1").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert(
        responseAs[Board].equalsIgnoreModifyTime(
          BridgeServiceTesting.testingMatch.getBoard(Board.id(1)).get
        )
      )
    }
    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

  it should "return a Board json object for board 2 for GET requests to /v1/rest/duplicates/M1/boards/B2" in {
    Get("/v1/rest/duplicates/M1/boards/B2").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert(
        responseAs[Board].equalsIgnoreModifyTime(
          BridgeServiceTesting.testingMatch.getBoard(Board.id(2)).get
        )
      )
    }
    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

  it should "return a Board json object for board 3 for GET requests to /v1/rest/duplicates/M1/boards/B3" in {
    Get("/v1/rest/duplicates/M1/boards/B3").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert(
        responseAs[Board].equalsIgnoreModifyTime(
          Board.create(Board.id(3), false, true, South.pos, List())
        )
      )
    }
    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

  it should "return a not found for board 4 for GET requests to /v1/rest/duplicates/M1/boards/B4" in {
    Get("/v1/rest/duplicates/M1/boards/B4").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage(
        "Did not find resource /duplicates/M1/boards/B4"
      )
    }
    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

  it should "return a not found for match 4 for GET requests to /v1/rest/duplicates/M4/boards/B1" in {
    Get("/v1/rest/duplicates/M4/boards/B1").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage(
        "Did not find resource /duplicates/M4"
      )
    }
    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

  behavior of "MyService REST for duplicate teams"

  var t: Team = null

  it should "return a Team json object for team 1 for GET requests to /v1/rest/duplicates/M1/teams/T1" in {
    Get("/v1/rest/duplicates/M1/teams/T1").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      t = responseAs[Team]
      assert(
        t.equalsIgnoreModifyTime(
          BridgeServiceTesting.testingMatch.getTeam(team1).get
        )
      )
    }
    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

  it should "return a Team json object for team 1 for PUT requests to /v1/rest/duplicates/M1/teams/T1" in {
    val nt = t.setPlayers("Fred", "George")
    Put("/v1/rest/duplicates/M1/teams/T1", nt).withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NoContent
//      mediaType mustBe MediaTypes.`application/json`
//      assert(responseAs[Team].equalsIgnoreModifyTime(nt))
    }
    client1.within(5 seconds) {
      client1.expectUnsolicitedMessage match {
        case UpdateDuplicateTeam(did, team) =>
          assert(nt.equalsIgnoreModifyTime(team))
        case x =>
          testlog.info(s"Unexpected unsolicited message $x")
          fail(s"Unexpected unsolicited message $x")
      }
    }
    Get("/v1/rest/duplicates/M1/teams/T1").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert(responseAs[Team].equalsIgnoreModifyTime(nt))
    }
    Put("/v1/rest/duplicates/M1/teams/T1", t).withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NoContent
    }
    Get("/v1/rest/duplicates/M1/teams/T1").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert(responseAs[Team].equalsIgnoreModifyTime(t))
    }
    client1.within(5 seconds) {
      client1.expectUnsolicitedMessage match {
        case UpdateDuplicateTeam(did, team) =>
          assert(t.equalsIgnoreModifyTime(team))
        case x =>
          testlog.info(s"Unexpected unsolicited message $x")
          fail(s"Unexpected unsolicited message $x")
      }
    }
    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

  it should "return a Team json object for team 2 for GET requests to /v1/rest/duplicates/M1/teams/T2" in {
    Get("/v1/rest/duplicates/M1/teams/T2").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert(
        responseAs[Team].equalsIgnoreModifyTime(
          BridgeServiceTesting.testingMatch.getTeam(team2).get
        )
      )
    }
    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

  it should "return a not found for team 5 for GET requests to /v1/rest/duplicates/M1/teams/T5" in {
    Get("/v1/rest/duplicates/M1/teams/T5").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage(
        "Did not find resource /duplicates/M1/teams/T5"
      )
    }
    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

  it should "return a not found for match 3 for GET requests to /v1/rest/duplicates/M3/teams/B1" in {
    Get("/v1/rest/duplicates/M3/teams/T9").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage(
        "Did not find resource /duplicates/M3"
      )
    }
    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

  behavior of "MyService REST for duplicate hands"

  it should "return a hand json object for hand 1 for GET requests to /v1/rest/duplicates/M1/boards/B1/hands/T1" in {
    Get("/v1/rest/duplicates/M1/boards/B1/hands/T1").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      withClue("response is " + response) { status mustBe OK }
      mediaType mustBe MediaTypes.`application/json`
      assert(
        responseAs[DuplicateHand].equalsIgnoreModifyTime(
          DuplicateHand.create(
            Hand.create(
              "H1",
              7,
              Spades.suit,
              Doubled.doubled,
              North.pos,
              false,
              false,
              true,
              7
            ),
            Table.id(1),
            1,
            Board.id(1),
            team1,
            team2
          )
        )
      )
    }
    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

  it should "return a not found for hand 3 for GET requests to /v1/rest/duplicates/M1/boards/B1/hands/T4" in {
    Get("/v1/rest/duplicates/M1/boards/B1/hands/T4").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage(
        "Did not find resource /duplicates/M1/boards/B1/hands/T4"
      )
    }
    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

  it should "return a not found for board 4 for GET requests to /v1/rest/duplicates/M1/boards/B4/hands/T1" in {
    Get("/v1/rest/duplicates/M1/boards/B4/hands/T1").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage(
        "Did not find resource /duplicates/M1/boards/B4"
      )
    }
    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

  it should "return a not found for match 4 for GET requests to /v1/rest/duplicates/M4/boards/B1" in {
    Get("/v1/rest/duplicates/M4/boards/B1").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage(
        "Did not find resource /duplicates/M4"
      )
    }
    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

  it should "return a hand json object for POST requests to /v1/rest/duplicates/M1/boards/B2/hands" in withListener(
    listenerstatus => {
      val hand = DuplicateHand.create(
        Hand.create(
          team3.id,
          7,
          Spades.suit,
          Doubled.doubled,
          North.pos,
          false,
          false,
          false,
          1
        ),
        Table.id(2),
        2,
        Board.id(2),
        team3,
        team4
      )
      Post("/v1/rest/duplicates/M1/boards/B2/hands", hand).withAttributes(
        remoteAddress
      ) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe Created
        header("Location") match {
          case Some(h) =>
            h.value() mustBe "http://example.com/v1/rest/duplicates/M1/boards/B2/hands/T3"
          case None =>
            fail("Location header was not found in response")
        }
        mediaType mustBe MediaTypes.`application/json`
        assert(responseAs[DuplicateHand].equalsIgnoreModifyTime(hand))
        assert(!listenerstatus.lastCreate.isEmpty)
        assert(listenerstatus.lastUpdate.isEmpty)
        val newMatch =
          BridgeServiceTesting.testingMatch.updateHand(Board.id(2), hand)
        testlog.debug("newMatch is " + newMatch)
        testlog.debug("fromList is " + listenerstatus.lastCreate.get)
        assert(listenerstatus.lastCreate.get.equalsIgnoreModifyTime(newMatch))
        assert(listenerstatus.lastDelete.isEmpty)
      }
      client1.within(5 seconds) {
        client1.expectUnsolicitedMessage match {
          case UpdateDuplicateHand(did, h) =>
            assert(hand.equalsIgnoreModifyTime(h))
          case x =>
            testlog.info(s"Unexpected unsolicited message $x")
            fail(s"Unexpected unsolicited message $x")
        }
      }
      WebsocketClient.ensureNoMessage(true, client1, client2, client3)
    }
  )

  it should "return a deleted for Delete requests to /v1/rest/duplicates/M1/boards/B2/hands/T3" in withListener(
    listenerstatus => {
      var m1: Option[MatchDuplicate] = None
      Get("/v1/rest/duplicates/M1").withAttributes(
        remoteAddress
      ) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe OK
        mediaType mustBe MediaTypes.`application/json`
        val r = responseAs[MatchDuplicate]
        m1 = Some(r)
      }
      Delete("/v1/rest/duplicates/M1/boards/B2/hands/T3").withAttributes(
        remoteAddress
      ) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe NoContent
//      mediaType mustBe MediaTypes.`application/json`
//      responseAs[RestMessage] mustBe RestMessage("Deleted Hand T3")
        assert(listenerstatus.lastCreate.isEmpty)
        assert(listenerstatus.lastUpdate.isEmpty)
//      assert( listenerstatus.lastUpdate.get.equalsIgnoreModifyTime( BridgeServiceTesting.testingMatch ))
        assert(!listenerstatus.lastDelete.isEmpty)
        listenerstatus.lastDelete.get.id mustBe BridgeServiceTesting.testingMatch.id

        val m1withhanddeleted =
          m1.get.updateBoard(m1.get.getBoard(Board.id(2)).get.deleteHand(team3))
        assert(
          listenerstatus.lastDelete.get.equalsIgnoreModifyTime(
            m1withhanddeleted
          )
        )
      }
      WebsocketClient.ensureNoMessage(true, client1, client2, client3)
    }
  )

  behavior of "MyService REST for duplicate when using delete"

  it should "return a deleted for Delete requests to /v1/rest/duplicates/M1" in withListener(
    listenerstatus => {
      var m1: Option[MatchDuplicate] = None
      Get("/v1/rest/duplicates/M1").withAttributes(
        remoteAddress
      ) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe OK
        mediaType mustBe MediaTypes.`application/json`
        val r = responseAs[MatchDuplicate]
        m1 = Some(r)
      }
      Delete("/v1/rest/duplicates/M1").withAttributes(
        remoteAddress
      ) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe NoContent
//      mediaType mustBe MediaTypes.`application/json`
//      responseAs[RestMessage] mustBe RestMessage("Deleted MatchDuplicate M1")
        assert(listenerstatus.lastCreate.isEmpty)
        assert(listenerstatus.lastUpdate.isEmpty)
        assert(!listenerstatus.lastDelete.isEmpty)
        listenerstatus.lastDelete.get.id mustBe BridgeServiceTesting.testingMatch.id
        assert(listenerstatus.lastDelete.get.equalsIgnoreModifyTime(m1.get))
      }
      WebsocketClient.ensureNoMessage(true, client1, client2, client3)
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
    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

  behavior of "MyService REST for DuplicateSumary"

  it should "return a MatchDuplicate json object for a POST request to /v1/rest/duplicates?default" in withListener(
    listenerstatus => {
      Post(
        "/v1/rest/duplicates?default",
        MatchDuplicate.create(MatchDuplicate.id(1))
      ).withAttributes(remoteAddress) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe Created
        mediaType mustBe MediaTypes.`application/json`
        val md = responseAs[MatchDuplicate]
        for (id <- 1 to 4) {
          val tid = Team.id(id)
          assert(
            md.getTeam(tid).get.equalsIgnoreModifyTime(Team.create(tid, "", ""))
          )
        }
        for (id <- 1 to 18) {
          val board = md.getBoard(Board.id(id))
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
      WebsocketClient.ensureNoMessage(true, client1, client2, client3)
    }
  )

  it should "return a MatchDuplicate json object for a POST request to /v1/rest/duplicates?boards=StandardBoards&movements=Mitchell3Table" in withListener(
    listenerstatus => {
      Post(
        "/v1/rest/duplicates?boards=StandardBoards&movements=Mitchell3Table",
        MatchDuplicate.create(MatchDuplicate.id(1))
      ).withAttributes(remoteAddress) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe Created
        mediaType mustBe MediaTypes.`application/json`
        val md = responseAs[MatchDuplicate]
        for (id <- 1 to 6) {
          val tid = Team.id(id)
          assert(
            md.getTeam(tid).get.equalsIgnoreModifyTime(Team.create(tid, "", ""))
          )
        }
        for (id <- 1 to 18) {
          val board = md.getBoard(Board.id(id))
          assert(board.isDefined, s"- Board $id was not found")
          val b = board.get
          assert(
            b.hands.size == 3,
            s"- Board $id did not have 3 hands, there were ${b.hands.size}"
          )
        }
        assert(!listenerstatus.lastCreate.isEmpty)
        assert(listenerstatus.lastUpdate.isEmpty)
        assert(listenerstatus.lastDelete.isEmpty)
      }
      WebsocketClient.ensureNoMessage(true, client1, client2, client3)
    }
  )

  it should "return a MatchDuplicate json object for a POST request to /v1/rest/duplicates?boards=StandardBoards&movements=Howell3TableNoRelay" in withListener(
    listenerstatus => {
      Post(
        "/v1/rest/duplicates?boards=StandardBoards&movements=Howell3TableNoRelay",
        MatchDuplicate.create(MatchDuplicate.id(1))
      ).withAttributes(remoteAddress) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe Created
        mediaType mustBe MediaTypes.`application/json`
        val md = responseAs[MatchDuplicate]
        for (id <- 1 to 6) {
          val tid = Team.id(id)
          assert(
            md.getTeam(tid).get.equalsIgnoreModifyTime(Team.create(tid, "", ""))
          )
        }
        for (id <- 1 to 20) {
          val board = md.getBoard(Board.id(id))
          assert(board.isDefined, s"- Board $id was not found")
          val b = board.get
          assert(
            b.hands.size == 3,
            s"- Board $id did not have 3 hands, there were ${b.hands.size}"
          )
        }
        assert(!listenerstatus.lastCreate.isEmpty)
        assert(listenerstatus.lastUpdate.isEmpty)
        assert(listenerstatus.lastDelete.isEmpty)
      }
      WebsocketClient.ensureNoMessage(true, client1, client2, client3)
    }
  )

  it should "return a list of duplicate summaries for GET requests to /v1/rest/duplicatesummaries" in {
    Get("/v1/rest/duplicatesummaries").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      val resp = responseAs[List[DuplicateSummary]]
      resp.length must be(4)

//      println("resp is: "+resp)
    }
    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

  it should "return a list of match duplicates for GET requests to /v1/rest/duplicates" in {
    Get("/v1/rest/duplicates").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      val resp = responseAs[List[MatchDuplicate]]
      resp.length must be(4)

//      println("resp is: "+resp)
    }
    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

  it should "show participants leave" in {

    client1.terminate()

    client2.testLeft
    client3.testLeft

    client2.terminate()
    client3.testLeft

    WebsocketClient.ensureNoMessage(true, client1, client2, client3)
  }

}
