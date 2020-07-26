package com.github.thebridsk.bridge.fullserver.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.server.test.backend.BridgeServiceTesting
import com.github.thebridsk.bridge.server.service.MyService
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server.RouteResult.Rejected
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.LogEntry
import akka.event.Logging
import com.github.thebridsk.bridge.server.rest.ServerPort
import com.github.thebridsk.bridge.server.version.VersionServer
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Route

class TestGetFromResource extends AnyFlatSpec with ScalatestRouteTest with Matchers with MyService {
  val restService = new BridgeServiceTesting

  val httpport = 8080
  override
  def ports: ServerPort = ServerPort( Option(httpport), None )

  val webJarLocationForServer = "META-INF/resources/webjars/bridgescorer-fullserver/"

  // scalafix:off
  implicit lazy val actorSystem = system
  implicit lazy val actorExecutor = executor
  implicit lazy val actorMaterializer = materializer
  // scalafix:on

  lazy val testlog: LoggingAdapter = Logging(actorSystem, classOf[TestGetFromResource])

  behavior of "Server"

  val version = VersionServer.version

  it should "find index.html as a resource" in {
    val theClassLoader = getClass.getClassLoader
    val theResource = theClassLoader.getResource(webJarLocationForServer+version+"/index.html")
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

  val logroute: Route = {
    logRequestResult(myTestLog _) {
      // route
      get {
        pathPrefix("public") {
          pathEnd {
            getFromResource(webJarLocationForServer+version+"/index.html")
          }
        }
      }
    }
  }

  val route: Route = {
    get {
      pathPrefix("public") {
        pathEnd {
          getFromResource(webJarLocationForServer+version+"/index.html")
        }
      }
    }
  }
}
