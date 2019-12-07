package com.github.thebridsk.bridge.server.test

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.server.service.MyService
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.server.test.backend.BridgeServiceTesting
import com.github.thebridsk.bridge.server.backend.BridgeService
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
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.data.bridge.East
import com.github.thebridsk.bridge.data.bridge.South
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.server.rest.ServerPort
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.data.MatchDuplicate


class TestBoardSetsAndHands extends FlatSpec with ScalatestRouteTest with MustMatchers with MyService {
  val restService = new BridgeServiceTesting

  val httpport = 8080
  override
  def ports = ServerPort( Option(httpport), None )

  implicit lazy val actorSystem = system
  implicit lazy val actorExecutor = executor
  implicit lazy val actorMaterializer = materializer

  val remoteAddress = `Remote-Address`( IP( InetAddress.getLocalHost, Some(12345) ))

  var boardsetArmonkBoards: Option[BoardSet] = None
  var originalNumberBoardSets: Int = 0

  TestStartLogging.startLogging()

  import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._

  behavior of "MyService REST for BoardSet"

  it should "return a table json object for boardset ArmonkBoards for GET requests to /v1/rest/boardsets/ArmonkBoards" in {
    Get("/v1/rest/boardsets/ArmonkBoards") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe `application/json`
      boardsetArmonkBoards = Some(responseAs[BoardSet])
      boardsetArmonkBoards match {
        case Some(bs) =>
          bs.boards.size mustBe 18
          bs.boards(0).id mustBe 1
          bs.boards(0).nsVul mustBe false
          bs.boards(0).ewVul mustBe false
          bs.boards(0).dealer mustBe "N"
          bs.boards(1).id mustBe 2
          bs.boards(1).nsVul mustBe true
          bs.boards(1).ewVul mustBe true
          bs.boards(1).dealer mustBe "W"
        case None =>
          fail( "Must receive a BoardSet" )
      }

    }
  }

  it should "return a json array of two boardset json object for GET requests to /v1/rest/boardsets" in {
    Get("/v1/rest/boardsets") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe OK
      mediaType mustBe `application/json`
      val r = responseAs[Array[BoardSet]]
      originalNumberBoardSets = r.length
    }
  }

  it should "return a boardset json object for POST request to /v1/rest/boardsets with BoardSet json" in {
    Post("/v1/rest/boardsets", boardsetArmonkBoards.get.copy(name="change")) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe Created
      mediaType mustBe `application/json`
      val resp = responseAs[BoardSet]
      resp.name mustBe "change"
    }
  }

  it should "return a json array of three boardset json object for GET requests to /v1/rest/boardsets" in {
    Get("/v1/rest/boardsets") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe OK
      mediaType mustBe `application/json`
      val r = responseAs[Array[BoardSet]]
      r.length mustBe (originalNumberBoardSets+1)
    }
  }

  it should "return a table json object for PUT request to /v1/rest/boardsets/ArmonkBoards with BoardSet json" in {
    Put("/v1/rest/boardsets/ArmonkBoards", boardsetArmonkBoards.get.copy(description="change")) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe NoContent
//      mediaType mustBe `application/json`
//      val resp = responseAs[BoardSet]
//      resp.description mustBe "change"
    }
    Get("/v1/rest/boardsets/ArmonkBoards" ) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe OK
      mediaType mustBe `application/json`
      val resp = responseAs[BoardSet]
      resp.description mustBe "change"
    }
  }

  it should "return a json array of three boardset json object for GET requests to /v1/rest/boardsets, the second time" in {
    Get("/v1/rest/boardsets") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe OK
      mediaType mustBe `application/json`
      val r = responseAs[Array[BoardSet]]
      r.length mustBe (originalNumberBoardSets+1)
    }
  }

  it should "return a boardset json object for boardset ArmonkBoards for GET requests to /v1/rest/boardsets/ArmonkBoards" in {
    Get("/v1/rest/boardsets/ArmonkBoards") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe `application/json`
      val bs = responseAs[BoardSet]
      bs.description mustBe "change"
      bs.boards.size mustBe 18
      bs.boards(0).id mustBe 1
      bs.boards(0).nsVul mustBe false
      bs.boards(0).ewVul mustBe false
      bs.boards(0).dealer mustBe "N"
      bs.boards(1).id mustBe 2
      bs.boards(1).nsVul mustBe true
      bs.boards(1).ewVul mustBe true
      bs.boards(1).dealer mustBe "W"

    }
  }

  it should "return a success for DELETE request to /v1/rest/boardsets/change, and get /v1/rest/boardsets returns two BoardSet objects" in {
    Delete("/v1/rest/boardsets/change") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe NoContent
    }
    Get("/v1/rest/boardsets") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe OK
      mediaType mustBe `application/json`
      val r = responseAs[Array[BoardSet]]
      r.length mustBe originalNumberBoardSets
    }
  }

  it should "return a success for DELETE request to /v1/rest/boardsets/ArmonkBoards, and get /v1/rest/boardsets returns two BoardSet objects, and still return an ArmonkBoards " in {
    Delete("/v1/rest/boardsets/ArmonkBoards") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe NoContent
    }
    Get("/v1/rest/boardsets") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe OK
      mediaType mustBe `application/json`
      val r = responseAs[Array[BoardSet]]
      r.length mustBe originalNumberBoardSets         // only deletes the changed one, the original is in read only storage
    }
    Get("/v1/rest/boardsets/ArmonkBoards") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe `application/json`
      val bs = responseAs[BoardSet]
      bs.description must not be "change"
    }
  }

  var Movement2TablesArmonk: Option[Movement] = None
  var originalNumberMovement: Int = 0

  behavior of "MyService REST for Movement"

  it should "return a table json object for Movement 2TablesArmonk for GET requests to /v1/rest/movements/2TablesArmonk" in {
    Get("/v1/rest/movements/2TablesArmonk") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe `application/json`
      Movement2TablesArmonk = Some(responseAs[Movement])
      Movement2TablesArmonk match {
        case Some(bs) =>
          bs.numberTeams mustBe 4
          bs.hands.size mustBe 12
          bs.hands(0).table mustBe 1
          bs.hands(0).round mustBe 1
          bs.hands(0).ns mustBe 1
          bs.hands(0).ew mustBe 2
          bs.hands(0).boards must contain allOf ( 1, 2, 3 )
          bs.hands(1).table mustBe 1
          bs.hands(1).round mustBe 2
          bs.hands(1).ns mustBe 1
          bs.hands(1).ew mustBe 2
          bs.hands(1).boards must contain allOf ( 4, 5, 6 )
        case None =>
          fail( "Must receive a Movement" )
      }

    }
  }

  it should "return a json array of one Movement json object for GET requests to /v1/rest/movements" in {
    Get("/v1/rest/movements") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe OK
      mediaType mustBe `application/json`
      val r = responseAs[Array[Movement]]
      originalNumberMovement = r.length
    }
  }

  it should "return a Movement json object for POST request to /v1/rest/movements with Movement json" in {
    Post("/v1/rest/movements", Movement2TablesArmonk.get.copy(name="change")) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe Created
      mediaType mustBe `application/json`
      val resp = responseAs[Movement]
      resp.name mustBe "change"
    }
  }

  it should "return a json array of two Movement json object for GET requests to /v1/rest/movements" in {
    Get("/v1/rest/movements") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe OK
      mediaType mustBe `application/json`
      val r = responseAs[Array[Movement]]
      r.length mustBe (originalNumberMovement+1)
    }
  }

  it should "return a table json object for PUT request to /v1/rest/movements/2TablesArmonk with Movement json" in {
    Put("/v1/rest/movements/2TablesArmonk", Movement2TablesArmonk.get.copy(description="change")) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe NoContent
//      mediaType mustBe `application/json`
//      val resp = responseAs[Movement]
//      resp.description mustBe "change"
    }
    Get("/v1/rest/movements/2TablesArmonk" ) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe OK
      mediaType mustBe `application/json`
      val resp = responseAs[Movement]
      resp.description mustBe "change"
    }
  }

  it should "return a json array of three Movement json object for GET requests to /v1/rest/movements/, the second time" in {
    Get("/v1/rest/movements") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe OK
      mediaType mustBe `application/json`
      val r = responseAs[Array[Movement]]
      r.length mustBe (originalNumberMovement+1)
    }
  }

  it should "return a Movement json object for Movement 2TablesArmonk for GET requests to /v1/rest/movements/2TablesArmonk" in {
    Get("/v1/rest/movements/2TablesArmonk") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe `application/json`
      val bs = responseAs[Movement]
      bs.description mustBe "change"
      bs.numberTeams mustBe 4
      bs.hands.size mustBe 12
      bs.hands(0).table mustBe 1
      bs.hands(0).round mustBe 1
      bs.hands(0).ns mustBe 1
      bs.hands(0).ew mustBe 2
      bs.hands(0).boards must contain allOf ( 1, 2, 3 )
      bs.hands(1).table mustBe 1
      bs.hands(1).round mustBe 2
      bs.hands(1).ns mustBe 1
      bs.hands(1).ew mustBe 2
      bs.hands(1).boards must contain allOf ( 4, 5, 6 )

    }
  }

  it should "return a success for DELETE request to /v1/rest/movements/change, and get /v1/rest/movements returns one Movement objects" in {
    Delete("/v1/rest/movements/change") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe NoContent
    }
    Get("/v1/rest/movements") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe OK
      mediaType mustBe `application/json`
      val r = responseAs[Array[Movement]]
      r.length mustBe originalNumberMovement
    }
  }

  it should "return a success for DELETE request to /v1/rest/movements/2TablesArmonk, and get /v1/rest/movements returns one Movement objects, and still return an 2TablesArmonk " in {
    Delete("/v1/rest/movements/2TablesArmonk") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe NoContent
    }
    Get("/v1/rest/movements") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe OK
      mediaType mustBe `application/json`
      val r = responseAs[Array[Movement]]
      r.length mustBe originalNumberMovement         // only deletes the changed one, the original is in read only storage
    }
    Get("/v1/rest/movements/2TablesArmonk") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      handled mustBe true
      status mustBe OK
      mediaType mustBe `application/json`
      val bs = responseAs[Movement]
      bs.description must not be "change"
    }
  }

  it should "return a match duplicate for POST request to /v1/rest/duplicates that is identical to the one sent" in {
    val boards = (Board.create( "B1", false, false, "N", List() )::Nil)
    val md = MatchDuplicate.create("").copy(teams=MatchDuplicate.createTeams(4)).copy(boards=boards)
    Post("/v1/rest/duplicates", md) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe Created
      mediaType mustBe `application/json`
      val resp = responseAs[MatchDuplicate]
      resp.teams.size mustBe md.teams.size
      resp.boards.size mustBe md.boards.size
      assert( resp.equalsIgnoreModifyTime(md.copy(id=resp.id)),"response must be equal to what was sent" )
      Delete("/v1/rest/duplicates/"+resp.id) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
        status mustBe NoContent
      }
    }
  }

  var defaultMatchDuplicate: Option[MatchDuplicate] = None
  it should "return a match duplicate for POST request to /v1/rest/duplicates?default with 4 teams and 18 boards" in {
    val md = MatchDuplicate.create("")
    Post("/v1/rest/duplicates?default", md) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe Created
      mediaType mustBe `application/json`
      val resp = responseAs[MatchDuplicate]
      resp.teams.size mustBe 4
      resp.boards.size mustBe 18
      resp.getBoard("B2").get.dealer mustBe "W"
      resp.getBoard("B2").get.ewVul mustBe true
      defaultMatchDuplicate = Some(resp)
      Delete("/v1/rest/duplicates/"+resp.id) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
        status mustBe NoContent
      }
    }
  }

  it should "return a match duplicate for POST request to /v1/rest/duplicates?boards=ArmonkBoards&movements=2TablesArmonk with 4 teams and 18 boards identical to default, except id" in {
    val md = MatchDuplicate.create("")
    Post("/v1/rest/duplicates?boards=ArmonkBoards&movements=2TablesArmonk", md) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe Created
      mediaType mustBe `application/json`
      val resp = responseAs[MatchDuplicate]
      resp.teams.size mustBe 4
      resp.boards.size mustBe 18
      resp.getBoard("B2").get.dealer mustBe "W"
      resp.getBoard("B2").get.ewVul mustBe true
      assert(resp.equalsIgnoreModifyTime(defaultMatchDuplicate.get.copy(id=resp.id)),"Response must be identical to default response")
      Delete("/v1/rest/duplicates/"+resp.id) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
        status mustBe NoContent
      }
    }
  }

  it should "return a match duplicate for POST request to /v1/rest/duplicates?boards=ArmonkBoards with 4 teams and 18 boards identical to default, except id" in {
    val md = MatchDuplicate.create("")
    Post("/v1/rest/duplicates?boards=ArmonkBoards", md) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe Created
      mediaType mustBe `application/json`
      val resp = responseAs[MatchDuplicate]
      resp.teams.size mustBe 4
      resp.boards.size mustBe 18
      resp.getBoard("B2").get.dealer mustBe "W"
      resp.getBoard("B2").get.ewVul mustBe true
      assert(resp.equalsIgnoreModifyTime(defaultMatchDuplicate.get.copy(id=resp.id)),"Response must be identical to default response")
      Delete("/v1/rest/duplicates/"+resp.id) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
        status mustBe NoContent
      }
    }
  }

  it should "return a match duplicate for POST request to /v1/rest/duplicates?movements=2TablesArmonk with 4 teams and 18 boards identical to default, except id" in {
    val md = MatchDuplicate.create("")
    Post("/v1/rest/duplicates?movements=2TablesArmonk", md) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe Created
      mediaType mustBe `application/json`
      val resp = responseAs[MatchDuplicate]
      resp.teams.size mustBe 4
      resp.boards.size mustBe 18
      resp.getBoard("B2").get.dealer mustBe "W"
      resp.getBoard("B2").get.ewVul mustBe true
      assert(resp.equalsIgnoreModifyTime(defaultMatchDuplicate.get.copy(id=resp.id)),"Response must be identical to default response")
      Delete("/v1/rest/duplicates/"+resp.id) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
        status mustBe NoContent
      }
    }
  }

  it should "return a match duplicate for POST request to /v1/rest/duplicates?boards=StandardBoards&movements=2TablesArmonk with 4 teams and 18 boards that is different from the default" in {
    val md = MatchDuplicate.create("")
    Post("/v1/rest/duplicates?boards=StandardBoards&movements=2TablesArmonk", md) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe Created
      mediaType mustBe `application/json`
      val resp = responseAs[MatchDuplicate]
      resp.teams.size mustBe 4
      resp.boards.size mustBe 18
      resp.getBoard("B2").get.dealer mustBe "E"
      resp.getBoard("B2").get.ewVul mustBe false
      assert(!resp.equalsIgnoreModifyTime(defaultMatchDuplicate.get.copy(id=resp.id)),"Response must be different from the default response")
      Delete("/v1/rest/duplicates/"+resp.id) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
        status mustBe NoContent
      }
    }
  }

  it should "return a match duplicate for POST request to /v1/rest/duplicates?boards=xxx&movements=2TablesArmonk a bad request" in {
    val md = MatchDuplicate.create("")
    Post("/v1/rest/duplicates?boards=xxx&movements=2TablesArmonk", md) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe BadRequest
    }
  }

  it should "return a match duplicate for POST request to /v1/rest/duplicates?boards=ArmonkBoards&movements=xxxx a bad request" in {
    val md = MatchDuplicate.create("")
    Post("/v1/rest/duplicates?boards=StandardBoards&movements=xxx", md) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe BadRequest
    }
  }
}
