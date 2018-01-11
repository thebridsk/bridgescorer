package com.example.test

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import com.example.data.Board
import com.example.data.Table
import com.example.service.MyService
import com.example.data.Hand
import com.example.data.bridge.North
import com.example.data.bridge.East
import com.example.data.bridge.South
import com.example.data.MatchDuplicate
import com.example.test.backend.BridgeServiceTesting
import com.example.backend.BridgeService
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
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
import com.example.data.DuplicateHand
import com.example.data.bridge.Spades
import com.example.data.bridge.Doubled
import com.example.backend.BridgeServiceInMemory
import akka.http.scaladsl.testkit.WSProbe
import com.example.data.websocket.Protocol
import akka.http.scaladsl.model.ws.TextMessage
import akka.testkit.TestKit
import akka.testkit.TestKitBase
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.http.scaladsl.model.ws.BinaryMessage
import com.example.data.Team
import com.example.data.RestMessage
import com.example.data.websocket.Protocol.UpdateDuplicateTeam
import com.example.data.websocket.Protocol.UpdateDuplicateHand
import com.example.data.websocket.Protocol.UpdateDuplicate
import com.example.data.websocket.Protocol.MonitorJoined
import com.example.data.websocket.Protocol.MonitorLeft
import com.example.data.websocket.DuplexProtocol
import com.example.data.websocket.Protocol.NoData
import akka.event.Logging
import com.example.rest.ServerPort
import com.example.data.DuplicateSummary
import akka.http.scaladsl.testkit.RouteTest
import com.example.data.websocket.Protocol.ToServerMessage
import com.example.data.websocket.DuplexProtocol.Send
import com.example.data.websocket.Protocol.StartMonitor
import com.example.data.websocket.DuplexProtocol.DuplexMessage
import com.example.test.util.WebsocketClient
import com.example.test.util.WebsocketClientImplicits
import akka.event.LoggingAdapter
import java.io.Closeable
import com.example.backend.StoreMonitor
import com.example.backend.StoreMonitor.ReceivedMessage
import com.example.backend.StoreMonitor.NewParticipant
import akka.actor.Actor
import com.example.test.selenium.TestServer
import com.example.backend.resource.StoreListener
import com.example.backend.resource.ChangeContext
import com.example.backend.resource.CreateChangeContext
import com.example.backend.resource.UpdateChangeContext
import com.example.backend.resource.DeleteChangeContext

class TestDuplicateWebsocket extends FlatSpec with ScalatestRouteTest with MustMatchers with MyService with BeforeAndAfterAll {

  implicit val me = this

  import WebsocketClientImplicits._
  import scala.concurrent.duration._
  import scala.language.postfixOps

  TestStartLogging.startLogging()

  val restService = new BridgeServiceInMemory

  val httpport = 8080
  override
  def ports = ServerPort( Option(httpport), None )

  implicit lazy val actorSystem = system
  implicit lazy val actorExecutor = executor
  implicit lazy val actorMaterializer = materializer

  implicit lazy val testlog = Logging(actorSystem, classOf[TestDuplicateWebsocket])

  val remoteAddress  = `Remote-Address`( IP( InetAddress.getLocalHost, Some(12345) ))
  val remoteAddress1 = `Remote-Address`( IP( InetAddress.getLocalHost, Some(11111) ))
  val remoteAddress2 = `Remote-Address`( IP( InetAddress.getLocalHost, Some(22222) ))
  val remoteAddress3 = `Remote-Address`( IP( InetAddress.getLocalHost, Some(33333) ))

  var client1: WebsocketClient = null
  var client2: WebsocketClient = null
  var client3: WebsocketClient = null

  override
  def beforeAll() = {
    client1 = new WebsocketClient
    client2 = new WebsocketClient
    client3 = new WebsocketClient
  }

  override
  def afterAll() = {
    client1.terminate()
    client1 = null
    client2.terminate()
    client2 = null
    client3.terminate()
    client3 = null
  }

  def withHook[T]( hook: Hook )( f: => T ): T = {
    class HookM( hook: Hook ) {
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
    def hook( actor: Actor, msg: Any )
  }

  behavior of "MyService REST for duplicate"

  class ListenerStatus extends StoreListener {

    var lastCreate: Option[MatchDuplicate] = None
    var lastUpdate: Option[MatchDuplicate] = None
    var lastDelete: Option[MatchDuplicate] = None

    def reset() = {
      lastCreate = None
      lastUpdate = None
      lastDelete = None
    }

    def getMatchDuplicate( context: ChangeContext ) = {
      log
      testlog.debug("Got a change: "+context)
      val d = context.changes.headOption match {
        case Some(cd) =>
          (cd match {
            case CreateChangeContext(newValue, parentField) => newValue
            case UpdateChangeContext(newValue, parentField) => newValue
            case DeleteChangeContext(oldValue, parentField) => oldValue
          }) match {
            case md: MatchDuplicate => Some(md)
            case _ => None
          }
        case None => None
      }
      testlog.debug("Found change: "+d)
      d
    }

    override def create( context: ChangeContext ): Unit = {lastCreate = getMatchDuplicate(context); testlog.debug("Got a create change: "+context)}
    override def update( context: ChangeContext ): Unit = {lastUpdate = getMatchDuplicate(context); testlog.debug("Got a update change: "+context)}
    override def delete( context: ChangeContext ): Unit = {lastDelete = getMatchDuplicate(context); testlog.debug("Got a delete change: "+context)}

  }

  def withListener( f: ListenerStatus=>Unit ) = {
    val status = new ListenerStatus
    try {
      restService.duplicates.addListener(status)
      f(status)
    }
    finally {
      restService.duplicates.removeListener(status)
    }
  }

  import com.example.rest.UtilsPlayJson._

  var createdM1: Option[MatchDuplicate] = None
  it should "return a MatchDuplicate json object for match 1 for POST request to /v1/rest/duplicates" in withListener( listenerstatus=> {
    Post("/v1/rest/duplicates?default", MatchDuplicate.create("M1")) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe Created
      mediaType mustBe MediaTypes.`application/json`
      header("Location") match {
        case Some(h) =>
          h.value() mustBe "http://example.com/duplicates/M1"
        case None =>
          fail("Location header was not found in response")
      }
      val md = responseAs[MatchDuplicate]
      createdM1 = Some(md)
      for( id <- 1 to 4){
        val tid = "T"+id
        assert( md.getTeam(tid).get.equalsIgnoreModifyTime( Team.create(tid,"","")) )
      }
      for( id <- 1 to 18){
        val board = md.getBoard("B"+id)
        assert( board.isDefined, s"- Board $id was not found" )
        val b = board.get
        assert( b.hands.size == 2, s"- Board $id did not have 2 hands, there were ${b.hands.size}" )
      }
      assert( !listenerstatus.lastCreate.isEmpty )
      assert( listenerstatus.lastUpdate.isEmpty )
      assert( listenerstatus.lastDelete.isEmpty )
    }
  })

  it should "return a MatchDuplicate json object for match 1 for PUT request to /v1/rest/duplicates/M1" in withListener( listenerstatus=> {
    import scala.concurrent.duration._
    import scala.language.postfixOps

    var gotJoin = false
    var gotStartMonitor = false

    def process( msg: Protocol.ToServerMessage ) = {
      testlog.info(s"""withHook got $msg""")
      msg match {
        case _: StartMonitor => gotStartMonitor = true
        case _ =>
      }
    }

    testlog.info("setting up withHook")

    withHook( new Hook {
      def hook( actor: Actor, msg: Any ) = {
        msg match {
          case NewParticipant(name, subscriber) =>
            gotJoin = true
          case ReceivedMessage(senderid, message) =>
            DuplexProtocol.fromString(message) match {
              case DuplexProtocol.Send(data) => process(data)
              case DuplexProtocol.Request(data, seq, ack) => process(data)
              case _ =>
            }
          case _ =>
        }
      }
    } ) {
      testlog.info("withHook starting")
      client3.connect(remoteAddress3, myRouteWithLogging)
      client1.connect(remoteAddress1, myRouteWithLogging)
      client3.testJoin()
      testlog.info(s"withHook gotJoin=$gotJoin gotStartMonitor=$gotStartMonitor")
      gotJoin mustBe true
      gotStartMonitor mustBe false
      client1.send(StartMonitor("M1"))
      client1.within(10 seconds) {
        client1.testUpdate(createdM1.get)
      }
      testlog.info(s"withHook gotJoin=$gotJoin gotStartMonitor=$gotStartMonitor")
      gotStartMonitor mustBe true
      Put("/v1/rest/duplicates/M1", BridgeServiceTesting.testingMatch) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe NoContent
//        mediaType mustBe MediaTypes.`application/json`
//        responseAs[MatchDuplicate].equalsIgnoreModifyTime( BridgeServiceTesting.testingMatch, true )
        assert( listenerstatus.lastCreate.isEmpty )
        assert( !listenerstatus.lastUpdate.isEmpty )
        try {
          listenerstatus.lastUpdate.get.equalsIgnoreModifyTime(BridgeServiceTesting.testingMatch, true )
        } catch {
          case x: Exception =>
            println( s"lastUpdate=${listenerstatus.lastUpdate.get}" )
            println( s"testingMatch=${BridgeServiceTesting.testingMatch}" )
            throw x
        }
        assert( listenerstatus.lastDelete.isEmpty )
      }
      client1.within(10 seconds) {
        client1.testUpdate(BridgeServiceTesting.testingMatch)
      }
      Get("/v1/rest/duplicates/M1" ) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
        handled mustBe true
        status mustBe OK
        mediaType mustBe MediaTypes.`application/json`
        responseAs[MatchDuplicate].equalsIgnoreModifyTime( BridgeServiceTesting.testingMatch, true )
      }
      WebsocketClient.ensureNoMessage(true,client1,client3)
      testlog.info("withHook done")
    }
  })

  it should "return a not found for match 2 for GET requests to /v1/rest/duplicates/M2" in {
    Get("/v1/rest/duplicates/M2") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage("Did not find resource /duplicates/M2")
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }

  it should "return a MatchDuplicate json object for match 2 for POST request to /v1/rest/duplicates" in withListener( listenerstatus=> {
    Post("/v1/rest/duplicates?default", MatchDuplicate.create("M2")) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe Created
      mediaType mustBe MediaTypes.`application/json`
      val md = responseAs[MatchDuplicate]
      header("Location") match {
        case Some(h) =>
          h.value() mustBe s"http://example.com/duplicates/${md.id}"
        case None =>
          fail("Location header was not found in response")
      }
      client2.connect(remoteAddress2, myRouteWithLogging)
      client1.within(10 seconds) { client1.testJoin() }
      client3.within(10 seconds) { client3.testJoin() }

      client2.send(StartMonitor(s"${md.id}"))
      client2.within(10 seconds) {
        client2.testUpdate(md)
      }
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  })

  it should "return a MatchDuplicate json object for match 1 for GET requests to /v1/rest/duplicates/M1" in withListener( listenerstatus=> {
    Get("/v1/rest/duplicates/M1") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      responseAs[MatchDuplicate].equalsIgnoreModifyTime( BridgeServiceTesting.testingMatch, true )
      assert( listenerstatus.lastCreate.isEmpty )
      assert( listenerstatus.lastUpdate.isEmpty )
      assert( listenerstatus.lastDelete.isEmpty )
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  })

  behavior of "MyService REST for names"

  it should "return a list of names for GET requests to /v1/rest/names" in {
    Get("/v1/rest/names") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      val resp = responseAs[List[String]]
      testlog.debug("names are "+resp)
      resp mustBe List("Ellen", "Ethan", "Nancy", "Norman", "Sally", "Sam", "Wayne", "Wilma")
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }

  behavior of "MyService REST for duplicate boards"

  it should "return a Board json object for board 1 for GET requests to /v1/rest/duplicates/M1/boards/B1" in {
    Get("/v1/rest/duplicates/M1/boards/B1") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert(responseAs[Board].equalsIgnoreModifyTime(BridgeServiceTesting.testingMatch.getBoard("B1").get))
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }

  it should "return a Board json object for board 2 for GET requests to /v1/rest/duplicates/M1/boards/B2" in {
    Get("/v1/rest/duplicates/M1/boards/B2") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert(responseAs[Board].equalsIgnoreModifyTime(BridgeServiceTesting.testingMatch.getBoard("B2").get))
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }

  it should "return a Board json object for board 3 for GET requests to /v1/rest/duplicates/M1/boards/B3" in {
    Get("/v1/rest/duplicates/M1/boards/B3") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert(responseAs[Board].equalsIgnoreModifyTime(Board.create("B3", false, true, South.pos, List())))
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }

  it should "return a not found for board 4 for GET requests to /v1/rest/duplicates/M1/boards/B4" in {
    Get("/v1/rest/duplicates/M1/boards/B4") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage("Did not find resource /duplicates/M1/boards/B4")
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }

  it should "return a not found for match 4 for GET requests to /v1/rest/duplicates/M4/boards/B1" in {
    Get("/v1/rest/duplicates/M4/boards/B1") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage("Did not find resource /duplicates/M4")
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }

  behavior of "MyService REST for duplicate teams"

  var t: Team = null

  it should "return a Team json object for team 1 for GET requests to /v1/rest/duplicates/M1/teams/T1" in {
    Get("/v1/rest/duplicates/M1/teams/T1") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      t = responseAs[Team]
      assert(t.equalsIgnoreModifyTime(BridgeServiceTesting.testingMatch.getTeam("T1").get))
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }

  it should "return a Team json object for team 1 for PUT requests to /v1/rest/duplicates/M1/teams/T1" in {
    val nt = t.setPlayers("Fred", "George")
    Put("/v1/rest/duplicates/M1/teams/T1", nt) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NoContent
//      mediaType mustBe MediaTypes.`application/json`
//      assert(responseAs[Team].equalsIgnoreModifyTime(nt))
    }
    client1.within(5 seconds) {
      client1.expectUnsolicitedMessage match {
        case UpdateDuplicateTeam(did, team) =>
          assert( nt.equalsIgnoreModifyTime(team) )
        case x =>
          testlog.info( s"Unexpected unsolicited message $x" )
          fail( s"Unexpected unsolicited message $x" )
      }
    }
    Get("/v1/rest/duplicates/M1/teams/T1" ) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert(responseAs[Team].equalsIgnoreModifyTime(nt))
    }
    Put("/v1/rest/duplicates/M1/teams/T1", t) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NoContent
    }
    Get("/v1/rest/duplicates/M1/teams/T1" ) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert(responseAs[Team].equalsIgnoreModifyTime(t))
    }
    client1.within(5 seconds) {
      client1.expectUnsolicitedMessage match {
        case UpdateDuplicateTeam(did, team) =>
          assert( t.equalsIgnoreModifyTime(team) )
        case x =>
          testlog.info( s"Unexpected unsolicited message $x" )
          fail( s"Unexpected unsolicited message $x" )
      }
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }

  it should "return a Team json object for team 2 for GET requests to /v1/rest/duplicates/M1/teams/T2" in {
    Get("/v1/rest/duplicates/M1/teams/T2") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert(responseAs[Team].equalsIgnoreModifyTime(BridgeServiceTesting.testingMatch.getTeam("T2").get))
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }

  it should "return a not found for team 5 for GET requests to /v1/rest/duplicates/M1/teams/T5" in {
    Get("/v1/rest/duplicates/M1/teams/T5") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage("Did not find resource /duplicates/M1/teams/T5")
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }

  it should "return a not found for match 3 for GET requests to /v1/rest/duplicates/M3/teams/B1" in {
    Get("/v1/rest/duplicates/M3/teams/B1") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage("Did not find resource /duplicates/M3")
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }

  behavior of "MyService REST for duplicate hands"

  it should "return a hand json object for hand 1 for GET requests to /v1/rest/duplicates/M1/boards/B1/hands/T1" in {
    Get("/v1/rest/duplicates/M1/boards/B1/hands/T1") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      withClue( "response is "+response) { status mustBe OK }
      mediaType mustBe MediaTypes.`application/json`
      assert(responseAs[DuplicateHand].equalsIgnoreModifyTime(DuplicateHand.create( Hand.create("H1",7,Spades.suit, Doubled.doubled, North.pos,
                                                             false,false,true,7),
                                                        "1", 1, "B1", "T1", "T2")))
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }

  it should "return a not found for hand 3 for GET requests to /v1/rest/duplicates/M1/boards/B1/hands/T4" in {
    Get("/v1/rest/duplicates/M1/boards/B1/hands/T4") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage("Did not find resource /duplicates/M1/boards/B1/hands/T4")
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }

  it should "return a not found for board 4 for GET requests to /v1/rest/duplicates/M1/boards/B4/hands/T1" in {
    Get("/v1/rest/duplicates/M1/boards/B4/hands/T1") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage("Did not find resource /duplicates/M1/boards/B4")
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }

  it should "return a not found for match 4 for GET requests to /v1/rest/duplicates/M4/boards/B1" in {
    Get("/v1/rest/duplicates/M4/boards/B1") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage("Did not find resource /duplicates/M4")
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }

  it should "return a hand json object for POST requests to /v1/rest/duplicates/M1/boards/B2/hands" in withListener( listenerstatus=> {
    val hand = DuplicateHand.create( Hand.create("T3",7,Spades.suit, Doubled.doubled, North.pos,
                                                             false,false,false,1),
                                                        "2", 2, "B2", "T3", "T4")
    Post("/v1/rest/duplicates/M1/boards/B2/hands", hand) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe Created
      header("Location") match {
        case Some(h) =>
          h.value() mustBe "http://example.com/duplicates/M1/boards/B2/hands/T3"
        case None =>
          fail("Location header was not found in response")
      }
      mediaType mustBe MediaTypes.`application/json`
      assert( responseAs[DuplicateHand].equalsIgnoreModifyTime( hand ))
      assert( !listenerstatus.lastCreate.isEmpty )
      assert( listenerstatus.lastUpdate.isEmpty )
      val newMatch = BridgeServiceTesting.testingMatch.updateHand("B2", hand)
      testlog.debug("newMatch is "+newMatch)
      testlog.debug("fromList is "+listenerstatus.lastCreate.get)
      assert( listenerstatus.lastCreate.get.equalsIgnoreModifyTime( newMatch ))
      assert( listenerstatus.lastDelete.isEmpty )
    }
    client1.within(5 seconds) {
      client1.expectUnsolicitedMessage match {
        case UpdateDuplicateHand(did, h) =>
          assert( hand.equalsIgnoreModifyTime(h) )
        case x =>
          testlog.info( s"Unexpected unsolicited message $x" )
          fail( s"Unexpected unsolicited message $x" )
      }
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  })

  it should "return a deleted for Delete requests to /v1/rest/duplicates/M1/boards/B2/hands/T3" in withListener( listenerstatus=> {
    var m1: Option[MatchDuplicate] = None
    Get("/v1/rest/duplicates/M1") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      val r = responseAs[MatchDuplicate]
      m1 = Some(r)
    }
    Delete("/v1/rest/duplicates/M1/boards/B2/hands/T3") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NoContent
//      mediaType mustBe MediaTypes.`application/json`
//      responseAs[RestMessage] mustBe RestMessage("Deleted Hand T3")
      assert( listenerstatus.lastCreate.isEmpty )
      assert( listenerstatus.lastUpdate.isEmpty )
//      assert( listenerstatus.lastUpdate.get.equalsIgnoreModifyTime( BridgeServiceTesting.testingMatch ))
      assert( !listenerstatus.lastDelete.isEmpty )
      listenerstatus.lastDelete.get.id mustBe BridgeServiceTesting.testingMatch.id

      val m1withhanddeleted = m1.get.updateBoard(m1.get.getBoard("B2").get.deleteHand("T3"))
      assert( listenerstatus.lastDelete.get.equalsIgnoreModifyTime( m1withhanddeleted ))
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  })

  behavior of "MyService REST for duplicate when using delete"

  it should "return a deleted for Delete requests to /v1/rest/duplicates/M1" in withListener( listenerstatus=> {
    var m1: Option[MatchDuplicate] = None
    Get("/v1/rest/duplicates/M1") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      val r = responseAs[MatchDuplicate]
      m1 = Some(r)
    }
    Delete("/v1/rest/duplicates/M1") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NoContent
//      mediaType mustBe MediaTypes.`application/json`
//      responseAs[RestMessage] mustBe RestMessage("Deleted MatchDuplicate M1")
      assert( listenerstatus.lastCreate.isEmpty )
      assert( listenerstatus.lastUpdate.isEmpty )
      assert( !listenerstatus.lastDelete.isEmpty )
      listenerstatus.lastDelete.get.id mustBe BridgeServiceTesting.testingMatch.id
      assert( listenerstatus.lastDelete.get.equalsIgnoreModifyTime( m1.get ))
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  })

  behavior of "MyService REST for names at end"

  it should "return a list of names for GET requests to /v1/rest/names" in {
    Get("/v1/rest/names") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      val resp = responseAs[List[String]]
      testlog.debug("names are "+resp)
      resp mustBe List()
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }

  behavior of "MyService REST for DuplicateSumary"

  it should "return a MatchDuplicate json object for a POST request to /v1/rest/duplicates?default" in withListener( listenerstatus=> {
    Post("/v1/rest/duplicates?default", MatchDuplicate.create("M1")) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe Created
      mediaType mustBe MediaTypes.`application/json`
      val md = responseAs[MatchDuplicate]
      for( id <- 1 to 4){
        val tid = "T"+id
        assert( md.getTeam(tid).get.equalsIgnoreModifyTime( Team.create(tid,"","")) )
      }
      for( id <- 1 to 18){
        val board = md.getBoard("B"+id)
        assert( board.isDefined, s"- Board $id was not found" )
        val b = board.get
        assert( b.hands.size == 2, s"- Board $id did not have 2 hands, there were ${b.hands.size}" )
      }
      assert( !listenerstatus.lastCreate.isEmpty )
      assert( listenerstatus.lastUpdate.isEmpty )
      assert( listenerstatus.lastDelete.isEmpty )
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  })

  it should "return a MatchDuplicate json object for a POST request to /v1/rest/duplicates?boards=StandardBoards&movements=Mitchell3Table" in withListener( listenerstatus=> {
    Post("/v1/rest/duplicates?boards=StandardBoards&movements=Mitchell3Table", MatchDuplicate.create("M1")) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe Created
      mediaType mustBe MediaTypes.`application/json`
      val md = responseAs[MatchDuplicate]
      for( id <- 1 to 6){
        val tid = "T"+id
        assert( md.getTeam(tid).get.equalsIgnoreModifyTime( Team.create(tid,"","")) )
      }
      for( id <- 1 to 18){
        val board = md.getBoard("B"+id)
        assert( board.isDefined, s"- Board $id was not found" )
        val b = board.get
        assert( b.hands.size == 3, s"- Board $id did not have 3 hands, there were ${b.hands.size}" )
      }
      assert( !listenerstatus.lastCreate.isEmpty )
      assert( listenerstatus.lastUpdate.isEmpty )
      assert( listenerstatus.lastDelete.isEmpty )
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  })

  it should "return a MatchDuplicate json object for a POST request to /v1/rest/duplicates?boards=StandardBoards&movements=Howell3TableNoRelay" in withListener( listenerstatus=> {
    Post("/v1/rest/duplicates?boards=StandardBoards&movements=Howell3TableNoRelay", MatchDuplicate.create("M1")) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe Created
      mediaType mustBe MediaTypes.`application/json`
      val md = responseAs[MatchDuplicate]
      for( id <- 1 to 6){
        val tid = "T"+id
        assert( md.getTeam(tid).get.equalsIgnoreModifyTime( Team.create(tid,"","")) )
      }
      for( id <- 1 to 20){
        val board = md.getBoard("B"+id)
        assert( board.isDefined, s"- Board $id was not found" )
        val b = board.get
        assert( b.hands.size == 3, s"- Board $id did not have 3 hands, there were ${b.hands.size}" )
      }
      assert( !listenerstatus.lastCreate.isEmpty )
      assert( listenerstatus.lastUpdate.isEmpty )
      assert( listenerstatus.lastDelete.isEmpty )
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  })

  it should "return a list of duplicate summaries for GET requests to /v1/rest/duplicatesummaries" in {
    Get("/v1/rest/duplicatesummaries") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      val resp = responseAs[List[DuplicateSummary]]
      resp.length must be (4)

//      println("resp is: "+resp)
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }

  it should "return a list of match duplicates for GET requests to /v1/rest/duplicates" in {
    Get("/v1/rest/duplicates") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      val resp = responseAs[List[MatchDuplicate]]
      resp.length must be (4)

//      println("resp is: "+resp)
    }
    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }

  it should "show participants leave" in {

    client1.terminate()

    client2.testLeft
    client3.testLeft

    client2.terminate()
    client3.testLeft

    WebsocketClient.ensureNoMessage(true,client1,client2,client3)
  }
}
