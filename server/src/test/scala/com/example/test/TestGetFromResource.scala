package com.example.test

import org.scalatest.Finders
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.example.test.backend.BridgeServiceTesting
import com.example.service.MyService
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.unmarshalling.FromResponseUnmarshaller
import akka.http.scaladsl.server.RouteResult.Rejected
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.LogEntry
import akka.event.Logging
import com.example.rest.ServerPort
import com.example.version.VersionServer

class TestGetFromResource extends FlatSpec with ScalatestRouteTest with MustMatchers with MyService {
  val restService = new BridgeServiceTesting

  val httpport = 8080
  override
  def ports = ServerPort( Option(httpport), None )

  implicit lazy val actorSystem = system
  implicit lazy val actorExecutor = executor
  implicit lazy val actorMaterializer = materializer

  lazy val testlog = Logging(actorSystem, classOf[TestDuplicateRestSpec])

  behavior of "Server"

  val version = VersionServer.version

  it should "find index.html as a resource" in {
    val theClassLoader = getClass.getClassLoader
    val theResource = theClassLoader.getResource("META-INF/resources/webjars/bridgescorer-server/"+version+"/index.html")
    theResource must not be null
  }

  behavior of "MyService js"

  it should "return the index.html" in {
    Get("/public") ~> route ~> check {
      status mustBe StatusCodes.OK
//      testlog.debug(responseAs[String])
      responseAs[String] must include regex """(?s)<html.*bridgescorer.*</html>"""
    }
  }

  it should "return the index.html, and log it" in {
    Get("/public") ~> logroute ~> check {
      status mustBe StatusCodes.OK
//      testlog.debug(responseAs[String])
      responseAs[String] must include regex """(?s)<html.*bridgescorer.*</html>"""
    }
  }

  def myTestLog( request: HttpRequest): Any => Option[LogEntry] = {
    case x: HttpResponse =>
      testlog.debug("Request("+request.method+" "+request.uri+") HttpResponse: "+x)
      None
    case Rejected(rejections) =>
      testlog.debug("Request("+request.method+" "+request.uri+") Rejections: "+rejections)
      None
    case x =>
      testlog.debug("Request("+request.method+" "+request.uri+") Unknown: "+x)
      None
  }

  val logroute = {
    logRequestResult(myTestLog _) {
      // route
      get {
        pathPrefix("public") {
          pathEnd {
            getFromResource("META-INF/resources/webjars/bridgescorer-server/"+version+"/index.html")
          }
        }
      }
    }
  }

  val route = {
    get {
      pathPrefix("public") {
        pathEnd {
          getFromResource("META-INF/resources/webjars/bridgescorer-server/"+version+"/index.html")
        }
      }
    }
  }
}
