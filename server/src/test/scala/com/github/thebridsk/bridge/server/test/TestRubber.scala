package com.github.thebridsk.bridge.server.test

import com.github.thebridsk.bridge.server.test.backend.BridgeServiceTesting
import com.github.thebridsk.bridge.server.service.MyService
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server.Route
import akka.event.Logging
import java.net.InetAddress
import akka.http.scaladsl.model.RemoteAddress.IP
import akka.http.scaladsl.model.headers.`Remote-Address`
import com.github.thebridsk.bridge.server.rest.ServerPort
import com.github.thebridsk.bridge.data.MatchRubber
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class TestRubber extends AnyFlatSpec with ScalatestRouteTest with Matchers with MyService {
  val restService = new BridgeServiceTesting

  val httpport = 8080
  override
  def ports = ServerPort( Option(httpport), None )

  implicit val actorSystem = system
  implicit val actorExecutor = executor
  implicit val actorMaterializer = materializer

  val testlog = Logging(system, "TestRubber")

  behavior of "MyService for rubber rest resource"

  val remoteAddress = `Remote-Address`( IP( InetAddress.getLocalHost, Some(12345) ))

  import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._

  it should "return an empty list for /rubbers" in {
    Get("/v1/rest/rubbers") ~> addHeader(remoteAddress) ~> Route.seal { myRouteWithLogging } ~> check {
      status mustBe OK
      responseAs[ List[MatchRubber] ] mustBe Nil
    }
  }

  var createdId: Option[MatchRubber] = None
  it should "allow the creation of a rubber match" in {
    val mr = MatchRubber(MatchRubber.idNul,"","","","","",Nil,0,0)
    Post("/v1/rest/rubbers", mr ) ~> addHeader(remoteAddress) ~> Route.seal { myRouteWithLogging } ~> check {
      status mustBe Created
      val resp = responseAs[ MatchRubber ]
      resp.id must not be ""
      createdId = Some(resp)
    }
  }

  it should "allow the query of the created rubber match" in {
    Get("/v1/rest/rubbers/"+createdId.get.id) ~> addHeader(remoteAddress) ~> Route.seal { myRouteWithLogging } ~> check {
      status mustBe OK
      val resp = responseAs[ MatchRubber ]
      resp mustBe createdId.get
    }
  }

  it should "test allOf from should matchers" in {
    List(1, 2, 3, 4, 5) must (contain.allOf(2, 3, 5) )
  }

}
