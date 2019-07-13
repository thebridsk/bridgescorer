package com.github.thebridsk.bridge.test

import com.github.thebridsk.bridge.test.backend.BridgeServiceTesting
import com.github.thebridsk.bridge.service.MyService
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
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RejectionHandler
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.MethodRejection
import akka.event.Logging
import java.net.InetAddress
import akka.http.scaladsl.model.RemoteAddress.IP
import akka.http.scaladsl.model.headers.`Remote-Address`
import com.github.thebridsk.bridge.Server
import org.rogach.scallop.exceptions.IncompleteBuildException
import com.github.thebridsk.bridge.data.ServerURL
import com.github.thebridsk.bridge.rest.ServerPort
import com.github.thebridsk.bridge.data.MatchRubber

class TestRubber extends FlatSpec with ScalatestRouteTest with MustMatchers with MyService {
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

  import com.github.thebridsk.bridge.rest.UtilsPlayJson._

  it should "return an empty list for /rubbers" in {
    Get("/v1/rest/rubbers") ~> addHeader(remoteAddress) ~> Route.seal { myRouteWithLogging } ~> check {
      status mustBe OK
      responseAs[ List[MatchRubber] ] mustBe Nil
    }
  }

  var createdId: Option[MatchRubber] = None
  it should "allow the creation of a rubber match" in {
    val mr = MatchRubber("","","","","","",Nil,0,0)
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
}
