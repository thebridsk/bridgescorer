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
import com.example.data.websocket.Protocol.StartMonitorDuplicate
import com.example.data.MatchDuplicateResult
import com.example.data.Id
import scala.reflect.ClassTag
import com.example.backend.resource.ChangeContext
import com.example.backend.resource.CreateChangeContext
import com.example.backend.resource.UpdateChangeContext
import com.example.backend.resource.DeleteChangeContext
import com.example.backend.resource.StoreListener
import com.example.backend.resource.Store
import com.example.data.VersionedInstance
import com.example.data.websocket.Protocol.UpdateRubber
import com.example.data.websocket.Protocol.UpdateChicago
import com.example.data.websocket.Protocol.UpdateChicagoRound
import com.example.data.websocket.Protocol.UpdateChicagoHand

class TestDuplicateRestSpec extends FlatSpecLike with ScalatestRouteTest with MustMatchers with MyService {

  import TestDuplicateRestSpecImplicits._

  TestStartLogging.startLogging()

  val restService = new BridgeServiceInMemory("test")

  val httpport = 8080
  override
  def ports = ServerPort( Option(httpport), None )

  implicit lazy val actorSystem = system
  implicit lazy val actorExecutor = executor
  implicit lazy val actorMaterializer = materializer

  lazy val testlog = Logging(actorSystem, classOf[TestDuplicateRestSpec])

  val remoteAddress = `Remote-Address`( IP( InetAddress.getLocalHost, Some(12345) ))

  behavior of "MyService REST for duplicate"

  class ListenerStatus[T](implicit classT: ClassTag[T]) extends StoreListener {

    var lastCreate: Option[T] = None
    var lastUpdate: Option[T] = None
    var lastDelete: Option[T] = None

    def reset() = {
      lastCreate = None
      lastUpdate = None
      lastDelete = None
    }

    def getMatchDuplicate( context: ChangeContext ): Option[T] = {
      log
      testlog.debug("Got a change: "+context)
      val d = context.changes.headOption match {
        case Some(cd) =>
          (cd match {
            case CreateChangeContext(newValue, parentField) => newValue
            case UpdateChangeContext(newValue, parentField) => newValue
            case DeleteChangeContext(oldValue, parentField) => oldValue
          }) match {
            case md: T => Some(md)
            case _ => None
          }
        case None => None
      }
      testlog.debug("Found change: "+d)
      d.map(a => a.asInstanceOf[T])
    }

    override def create( context: ChangeContext ): Unit = {lastCreate = getMatchDuplicate(context); testlog.debug("Got a create change: "+context)}
    override def update( context: ChangeContext ): Unit = {lastUpdate = getMatchDuplicate(context); testlog.debug("Got a update change: "+context)}
    override def delete( context: ChangeContext ): Unit = {lastDelete = getMatchDuplicate(context); testlog.debug("Got a delete change: "+context)}

  }

  def withListener( f: ListenerStatus[MatchDuplicate]=>Unit ): Unit = {
    withListener( restService.duplicates, f )
  }

  def withListener[Id,T <: VersionedInstance[T,T,Id]]( store: Store[Id,T], f: ListenerStatus[T]=>Unit )(implicit classT: ClassTag[T]): Unit = {
    val status = new ListenerStatus[T]
    try {
      store.addListener(status)
      f(status)
    }
    finally {
      store.removeListener(status)
    }
  }

  def testIgnoreJoinLookForUpdate( wsClient: WSProbe, mat: MatchDuplicate ) = {
      while ((wsClient.expectMessage() match {
        case TextMessage.Strict(s) =>
          val dp = DuplexProtocol.fromString(s)
          dp match {
            case DuplexProtocol.Unsolicited(data) => data
            case _ =>
              fail("Unexpected response from the monitor: "+dp)
          }
        case tm: TextMessage =>
          val s = collect(tm.textStream)(_ + _)
          val dp = DuplexProtocol.fromString(s)
          dp match {
            case DuplexProtocol.Unsolicited(data) => data
            case _ =>
              fail("Unexpected response from the monitor: "+dp)
          }
        case x: BinaryMessage =>
          val data = collect(x.dataStream)(_ ++ _)
          fail("Unexpected response from the monitor: "+x.getClass.getName)
      }) match {
        case uteam: UpdateDuplicateTeam =>
          testlog.debug("Ignored unexpected response from the monitor: "+uteam)
          true
        case uboard: UpdateDuplicateHand =>
          testlog.debug("Ignored unexpected response from the monitor: "+uboard)
          true
        case UpdateDuplicate(mp) =>
          assert( mat.equalsIgnoreModifyTime(mp) )
          false
        case uboard: UpdateChicagoRound =>
          testlog.debug("Ignored unexpected response from the monitor: "+uboard)
          true
        case uboard: UpdateChicagoHand =>
          testlog.debug("Ignored unexpected response from the monitor: "+uboard)
          true
        case pj: MonitorJoined =>
          testlog.debug ("Ignored Unexpected response from the monitor: "+pj)
          true
        case pl: MonitorLeft =>
          fail("Unexpected response from the monitor: "+pl)
        case nd: NoData =>
          fail("Unexpected response from the monitor: "+nd)
        case m: UpdateChicago =>
          fail("Unexpected response from the monitor: "+m)
        case m: UpdateRubber =>
          fail("Unexpected response from the monitor: "+m)
      }) {}
  }

  def testUpdate( wsClient: WSProbe, mat: MatchDuplicate ) = {
      (wsClient.expectMessage() match {
        case TextMessage.Strict(s) =>
          val dp = DuplexProtocol.fromString(s)
          dp match {
            case DuplexProtocol.Unsolicited(data) => data
            case _ =>
              fail("Unexpected response from the monitor: "+dp)
          }
        case tm: TextMessage =>
          val s = collect(tm.textStream)(_ + _)
          val dp = DuplexProtocol.fromString(s)
          dp match {
            case DuplexProtocol.Unsolicited(data) => data
            case _ =>
              fail("Unexpected response from the monitor: "+dp)
          }
        case x: BinaryMessage =>
          val data = collect(x.dataStream)(_ ++ _)
          fail("Unexpected response from the monitor: "+x.getClass.getName)
      }) match {
        case uteam: UpdateDuplicateTeam =>
          testlog.debug("Ignored unexpected response from the monitor: "+uteam)
          true
        case uboard: UpdateDuplicateHand =>
          testlog.debug("Ignored unexpected response from the monitor: "+uboard)
          true
        case UpdateDuplicate(mp) =>
          testlog.debug( "mat: "+mat )
          testlog.debug( "mp : "+mp )
          assert( mat.equalsIgnoreModifyTime(mp) )
        case pj: MonitorJoined =>
          fail("Unexpected response from the monitor: "+pj)
        case pl: MonitorLeft =>
          fail("Unexpected response from the monitor: "+pl)
        case nd: NoData =>
          fail("Unexpected response from the monitor: "+nd)
        case m: UpdateChicago =>
          fail("Unexpected response from the monitor: "+m)
        case m: UpdateChicagoRound =>
          fail("Unexpected response from the monitor: "+m)
        case m: UpdateChicagoHand =>
          fail("Unexpected response from the monitor: "+m)
        case m: UpdateRubber =>
          fail("Unexpected response from the monitor: "+m)
      }
  }

  def testJoin( wsClient: WSProbe ) = {
      (wsClient.expectMessage() match {
        case TextMessage.Strict(s) =>
          val dp = DuplexProtocol.fromString(s)
          dp match {
            case DuplexProtocol.Unsolicited(data) => data
            case _ =>
              fail("Unexpected response from the monitor: "+dp)
          }
        case tm: TextMessage =>
          val s = collect(tm.textStream)(_ + _)
          val dp = DuplexProtocol.fromString(s)
          dp match {
            case DuplexProtocol.Unsolicited(data) => data
            case _ =>
              fail("Unexpected response from the monitor: "+dp)
          }
        case x: BinaryMessage =>
          val data = collect(x.dataStream)(_ ++ _)
          fail("Unexpected response from the monitor: "+x.getClass.getName)
      }) match {
        case uteam: UpdateDuplicateTeam =>
          testlog.debug("Ignored unexpected response from the monitor: "+uteam)
          true
        case uboard: UpdateDuplicateHand =>
          fail("Unexpected response from the monitor: "+uboard)
        case UpdateDuplicate(mp) =>
          fail("Unexpected response from the monitor: "+mp)
        case pl: MonitorLeft =>
          fail("Unexpected response from the monitor: "+pl)
        case pj: MonitorJoined =>
          testlog.debug ("Got the join: "+pj )
        case nd: NoData =>
          fail("Unexpected response from the monitor: "+nd)
        case m: UpdateChicago =>
          fail("Unexpected response from the monitor: "+m)
        case m: UpdateChicagoRound =>
          fail(s"Unexpected response from the monitor: $m")
        case m: UpdateChicagoHand =>
          fail(s"Unexpected response from the monitor: $m")
        case m: UpdateRubber =>
          fail("Unexpected response from the monitor: "+m)
      }
  }

  def testLeft( wsClient: WSProbe ) = {
      (wsClient.expectMessage() match {
        case TextMessage.Strict(s) =>
          val dp = DuplexProtocol.fromString(s)
          dp match {
            case DuplexProtocol.Unsolicited(data) => data
            case _ =>
              fail("Unexpected response from the monitor: "+dp)
          }
        case tm: TextMessage =>
          val s = collect(tm.textStream)(_ + _)
          val dp = DuplexProtocol.fromString(s)
          dp match {
            case DuplexProtocol.Unsolicited(data) => data
            case _ =>
              fail("Unexpected response from the monitor: "+dp)
          }
        case x: BinaryMessage =>
          val data = collect(x.dataStream)(_ ++ _)
          fail("Unexpected response from the monitor: "+x.getClass.getName)
      }) match {
        case uteam: UpdateDuplicateTeam =>
          testlog.debug("Ignored unexpected response from the monitor: "+uteam)
          true
        case uboard: UpdateDuplicateHand =>
          fail("Unexpected response from the monitor: "+uboard)
        case UpdateDuplicate(mp) =>
          fail("Unexpected response from the monitor: "+mp)
        case pl: MonitorLeft =>
          testlog.debug ("Got the left: "+pl )
        case pj: MonitorJoined =>
          fail("Unexpected response from the monitor: "+pj)
        case nd: NoData =>
          fail("Unexpected response from the monitor: "+nd)
        case m: UpdateChicago =>
          fail("Unexpected response from the monitor: "+m)
        case m: UpdateChicagoRound =>
          fail(s"Unexpected response from the monitor: $m")
        case m: UpdateChicagoHand =>
          fail(s"Unexpected response from the monitor: $m")
        case m: UpdateRubber =>
          fail("Unexpected response from the monitor: "+m)
      }
  }

  def terminateWebsocket( wsClient: WSProbe ) = {
    import scala.concurrent.duration._
    import scala.language.postfixOps
    wsClient.inProbe.within(10 seconds) {
      wsClient.sendCompletion()
//      testLeft(wsClient)
      wsClient.expectCompletion()
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
    val wsClient = WSProbe()
    WS("/v1/ws", wsClient.flow, Protocol.DuplicateBridge::Nil) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~>
      check {
        isWebSocketUpgrade mustBe true
        wsClient.inProbe.within(10 seconds) {
          testJoin(wsClient)
          val cleanmd = new BridgeServiceInMemory("test").fillBoards(MatchDuplicate.create("M1"))
//          testUpdate(wsClient,cleanmd)
        }
        wsClient.send(StartMonitorDuplicate("M1"))
        wsClient.inProbe.within(10 seconds) {
          testUpdate(wsClient,createdM1.get)
        }
        Put("/v1/rest/duplicates/M1", BridgeServiceTesting.testingMatch) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
          handled mustBe true
          status mustBe NoContent
//          mediaType mustBe MediaTypes.`application/json`
//          assert( responseAs[MatchDuplicate].equalsIgnoreModifyTime( BridgeServiceTesting.testingMatch ))
          assert( listenerstatus.lastCreate.isEmpty )
          assert( !listenerstatus.lastUpdate.isEmpty )
          assert( listenerstatus.lastUpdate.get.equalsIgnoreModifyTime(BridgeServiceTesting.testingMatch))
          assert( listenerstatus.lastDelete.isEmpty )
        }
        wsClient.inProbe.within(10 seconds) {
//          testIgnoreJoinLookForUpdate(wsClient,BridgeServiceTesting.testingMatch)
//          testJoin(wsClient)
          testUpdate(wsClient,BridgeServiceTesting.testingMatch)
        }
        Get("/v1/rest/duplicates/M1") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
          handled mustBe true
          status mustBe OK
          mediaType mustBe MediaTypes.`application/json`
          assert( responseAs[MatchDuplicate].equalsIgnoreModifyTime( BridgeServiceTesting.testingMatch ))
        }
        terminateWebsocket(wsClient)
      }
  })

  it should "monitor should return match M1 after the Join message" in withListener( listenerstatus=> {
    import scala.concurrent.duration._
    import scala.language.postfixOps
    val wsClient = WSProbe()
    WS("/v1/ws", wsClient.flow, Protocol.DuplicateBridge::Nil) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~>
      check {
        isWebSocketUpgrade mustBe true
        wsClient.inProbe.within(10 seconds) {
          testJoin(wsClient)
        }
//        wsClient.inProbe.within(10 seconds) {
//          testUpdate(wsClient,BridgeServiceTesting.testingMatch)
//        }
        terminateWebsocket(wsClient)
      }
  })

  it should "return a MatchDuplicate json object for match 1 for GET requests to /v1/rest/duplicates/M1" in withListener( listenerstatus=> {
    Get("/v1/rest/duplicates/M1") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert( responseAs[MatchDuplicate].equalsIgnoreModifyTime( BridgeServiceTesting.testingMatch ))
      assert( listenerstatus.lastCreate.isEmpty )
      assert( listenerstatus.lastUpdate.isEmpty )
      assert( listenerstatus.lastDelete.isEmpty )
    }
  })

  it should "return a not found for match 2 for GET requests to /v1/rest/duplicates/M2" in {
    Get("/v1/rest/duplicates/M2") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage("Did not find resource /duplicates/M2")
    }
  }

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
  }

  behavior of "MyService REST for duplicate boards"

  it should "return a Board json object for board 1 for GET requests to /v1/rest/duplicates/M1/boards/B1" in {
    Get("/v1/rest/duplicates/M1/boards/B1") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert(responseAs[Board].equalsIgnoreModifyTime(BridgeServiceTesting.testingMatch.getBoard("B1").get))
    }
  }

  it should "return a Board json object for board 2 for GET requests to /v1/rest/duplicates/M1/boards/B2" in {
    Get("/v1/rest/duplicates/M1/boards/B2") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert(responseAs[Board].equalsIgnoreModifyTime(BridgeServiceTesting.testingMatch.getBoard("B2").get))
    }
  }

  it should "return a Board json object for board 3 for GET requests to /v1/rest/duplicates/M1/boards/B3" in {
    Get("/v1/rest/duplicates/M1/boards/B3") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert(responseAs[Board].equalsIgnoreModifyTime(Board.create("B3", false, true, South.pos, List())))
    }
  }

  it should "return a not found for board 4 for GET requests to /v1/rest/duplicates/M1/boards/B4" in {
    Get("/v1/rest/duplicates/M1/boards/B4") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage("Did not find resource /duplicates/M1/boards/B4")
    }
  }

  it should "return a not found for match 2 for GET requests to /v1/rest/duplicates/M2/boards/B1" in {
    Get("/v1/rest/duplicates/M2/boards/B1") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage("Did not find resource /duplicates/M2")
    }
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
  }

  it should "return a Team json object for team 1 for PUT requests to /v1/rest/duplicates/M1/teams/T1" in {
    val nt = t.setPlayers("Fred", "George")
    Put("/v1/rest/duplicates/M1/teams/T1", nt) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NoContent
    }
    Get("/v1/rest/duplicates/M1/teams/T1") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
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
  }

  it should "return a Team json object for team 2 for GET requests to /v1/rest/duplicates/M1/teams/T2" in {
    Get("/v1/rest/duplicates/M1/teams/T2") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      assert(responseAs[Team].equalsIgnoreModifyTime(BridgeServiceTesting.testingMatch.getTeam("T2").get))
    }
  }

  it should "return a not found for team 5 for GET requests to /v1/rest/duplicates/M1/teams/T5" in {
    Get("/v1/rest/duplicates/M1/teams/T5") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage("Did not find resource /duplicates/M1/teams/T5")
    }
  }

  it should "return a not found for match 2 for GET requests to /v1/rest/duplicates/M2/teams/B1" in {
    Get("/v1/rest/duplicates/M2/teams/B1") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage("Did not find resource /duplicates/M2")
    }
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
  }

  it should "return a not found for hand 3 for GET requests to /v1/rest/duplicates/M1/boards/B1/hands/T4" in {
    Get("/v1/rest/duplicates/M1/boards/B1/hands/T4") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage("Did not find resource /duplicates/M1/boards/B1/hands/T4")
    }
  }

  it should "return a not found for board 4 for GET requests to /v1/rest/duplicates/M1/boards/B4/hands/T1" in {
    Get("/v1/rest/duplicates/M1/boards/B4/hands/T1") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage("Did not find resource /duplicates/M1/boards/B4")
    }
  }

  it should "return a not found for match 2 for GET requests to /v1/rest/duplicates/M2/boards/B1" in {
    Get("/v1/rest/duplicates/M2/boards/B1") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe NotFound
      mediaType mustBe MediaTypes.`application/json`
      responseAs[RestMessage] mustBe RestMessage("Did not find resource /duplicates/M2")
    }
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
  })

  it should "return a list of duplicate summaries for GET requests to /v1/rest/duplicatesummaries" in {
    Get("/v1/rest/duplicatesummaries") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      testlog.info( s"resp is: ${response}")
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      val resp = responseAs[List[DuplicateSummary]]
      resp.length must be (3)
    }
  }

  it should "return a list of match duplicates for GET requests to /v1/rest/duplicates" in {
    Get("/v1/rest/duplicates") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe MediaTypes.`application/json`
      val resp = responseAs[List[MatchDuplicate]]
      resp.length must be (3)

//      println("resp is: "+resp)
    }
  }

  behavior of "MyService REST for DuplicateResult"

  it should "return a MatchDuplicateResult given a MatchDuplicate instance" in {
    Await.result( restService.createTestDuplicate(MatchDuplicate.create()), 30.seconds) match {
      case Right(dup) =>
        val mdr = MatchDuplicateResult.createFrom(dup)

        mdr.results.size mustBe 1
        mdr.results(0).size mustBe 4
      case Left((status,msg)) =>
        fail(s"unable to crete a match duplicate object ${status} ${msg}")
    }

  }

  it should "notify the listener when a MatchDuplicateResult is created" in withListener[Id.MatchDuplicateResult,MatchDuplicateResult]( restService.duplicateresults, listenerstatus => {
    val mdr = MatchDuplicateResult.create()
    restService.duplicateresults.syncStore.createChild( mdr ) match {
      case Right(md) =>
        assert( !listenerstatus.lastCreate.isEmpty )
        assert( listenerstatus.lastUpdate.isEmpty )
        assert( listenerstatus.lastDelete.isEmpty )
      case Left((status,msg)) =>
        fail(s"unable to crete a match duplicate result object ${status} ${msg}")
    }
  })

  it should "return a MatchDuplicateResult json object for a POST request to /v1/rest/duplicateresults?default=true&boards=ArmonkBoards&movements=Armonk2Tables" in withListener[Id.MatchDuplicateResult,MatchDuplicateResult]( restService.duplicateresults, listenerstatus => {
    Post("/v1/rest/duplicateresults?default=true&boards=ArmonkBoards&movements=Armonk2Tables", MatchDuplicateResult.create("E1")) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe Created
      mediaType mustBe MediaTypes.`application/json`
      val mdr = responseAs[MatchDuplicateResult]

      mdr.results.size mustBe 1
      mdr.results(0).size mustBe 4

      assert( !listenerstatus.lastCreate.isEmpty )
      assert( listenerstatus.lastUpdate.isEmpty )
      assert( listenerstatus.lastDelete.isEmpty )
    }
  })

}

object TestDuplicateRestSpecImplicits {
  implicit class WebSocketSender( val wsClient: WSProbe ) extends AnyVal {

    def send( msg: ToServerMessage ) = {
      val data = Send(msg)
      wsClient.sendMessage(DuplexProtocol.toString(data))
    }
  }
}
