package com.github.thebridsk.bridge.fullserver.test

import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.bridge.server.test.backend.BridgeServiceTesting
import com.github.thebridsk.bridge.server.service.MyService
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server.Route
import akka.event.Logging
import com.github.thebridsk.bridge.data.ServerURL
import com.github.thebridsk.bridge.server.rest.ServerPort
import com.github.thebridsk.bridge.server.service.ResourceFinder
import com.github.thebridsk.bridge.server.service.ShutdownHook
import scala.concurrent.Future
import scala.concurrent.Promise
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.LogEntryV2
import com.github.thebridsk.bridge.server.test.util.TestServer
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.http.scaladsl.model.headers.HttpEncodings
import java.util.zip.GZIPInputStream
import java.io.ByteArrayInputStream
import scala.io.Source
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import akka.event.LoggingAdapter
import com.github.thebridsk.bridge.server.test.RoutingSpec

class MyServiceSpec
    extends AnyFlatSpec
    with ScalatestRouteTest
    with Matchers
    with MyService
    with RoutingSpec {
  val restService = new BridgeServiceTesting

  val httpport = 8080
  override def ports: ServerPort = ServerPort(Option(httpport), None)

  val webJarLocationForServer =
    "META-INF/resources/webjars/bridgescorer-fullserver/"

  // scalafix:off
  implicit lazy val actorSystem = system
  implicit lazy val actorExecutor = executor
  implicit lazy val actorMaterializer = materializer
  // scalafix:on

  val useFastOptOnly: Boolean =
    TestServer.getProp("OnlyBuildDebug").map(s => s.toBoolean).getOrElse(false)

  val useFullOptOnly: Boolean =
    TestServer.getProp("UseFullOpt").map(s => s.toBoolean).getOrElse(false)

  TestStartLogging.startLogging()

  val testlog: LoggingAdapter = Logging(system, "MyServiceSpec")

  behavior of "Server"

  val version = ResourceFinder.htmlResources.version

  val itOrIgnore: Object = if (TestServer.useProductionPage) ignore else it

  it should "find index.html as a resource" in {
    val theClassLoader = getClass.getClassLoader
    val theResource = theClassLoader.getResource(
      webJarLocationForServer + version + "/index.html"
    )
    theResource must not be null
  }

  it should "find bridgescorer-client-opt.js as a resource" in {
    assume(!useFastOptOnly)
    val theClassLoader = getClass.getClassLoader
    val theResource = theClassLoader.getResource(
      webJarLocationForServer + version + "/bridgescorer-client-opt.js"
    )
    theResource must not be null
  }

  it should "find bridgescorer-client-fastopt.js as a resource" in {
    assume(!useFullOptOnly)
    assume(!TestServer.useProductionPage)
    val theClassLoader = getClass.getClassLoader
    val theResource = theClassLoader.getResource(
      webJarLocationForServer + version + "/bridgescorer-client-fastopt.js"
    )
    theResource must not be null
  }

  behavior of "MyService for static pages"

  it should "return a redirect to /public/index.html for /" in {
    Get("/").withAttributes(remoteAddress) ~> Route.seal {
      myRouteWithLogging
    } ~> check {
      status mustBe PermanentRedirect
      header("Location") match {
        case Some(x) =>
          x.value mustBe "/public/index.html"
        case None => fail("Did not get a location header" + headers)
      }
    }
  }

  it should "return a redirect to /public/index.html for /public" in {
    Get("/public").withAttributes(remoteAddress) ~> Route.seal {
      myRouteWithLogging
    } ~> check {
      status mustBe PermanentRedirect
      header("Location") match {
        case Some(x) =>
          x.value mustBe "/public/index.html"
        case None => fail("Did not get a location header" + headers)
      }
    }
  }

  def getGzipBody(body: Array[Byte]): String = {
    val in = new GZIPInputStream(
      new ByteArrayInputStream(responseAs[Array[Byte]])
    )
    Source.fromInputStream(in, "utf-8").mkString
  }

  import akka.http.scaladsl.model.headers.`Content-Encoding`
  it should "return the index.html to /public/index.html" in {
    Get("/public/index.html").withAttributes(remoteAddress) ~> Route.seal {
      myRouteWithLogging
    } ~> check {
      status mustBe OK
      header("Content-Encoding") mustBe Some(
        `Content-Encoding`(HttpEncodings.gzip)
      )
      val sbody = getGzipBody(responseAs[Array[Byte]])
      sbody must include regex """(?s-)(?m-)<html.*bridgescorer-client-opt\.js.*</html>"""
    }
  }

  it should "return the index-fastopt.html to /html/index-fastopt.html" in {
    assume(!TestServer.useProductionPage)
    assume(!useFullOptOnly)
    Get("/public/index-fastopt.html").withAttributes(remoteAddress) ~> Route
      .seal { myRouteWithLogging } ~> check {
      status mustBe OK
      header("Content-Encoding") mustBe Some(
        `Content-Encoding`(HttpEncodings.gzip)
      )
      val sbody = getGzipBody(responseAs[Array[Byte]])
      sbody must include regex """(?s)<html>.*bridgescorer-client-fastopt\.js.*</html>"""
    }

  }

  {
    import scala.concurrent.duration._
    import akka.testkit.TestDuration
    import scala.language.postfixOps
    implicit val timeout = RouteTestTimeout(5.seconds dilated)

    it should "return bridgescorer-client-fastopt.js to /public/bridgescorer-client-fastopt.js" in {
      assume(!TestServer.useProductionPage)
      assume(!useFullOptOnly)

      Get("/public/bridgescorer-client-fastopt.js").withAttributes(
        remoteAddress
      ) ~> Route.seal { myRouteWithLogging } ~> check {
        status mustBe OK
        header("Content-Encoding") mustBe Some(
          `Content-Encoding`(HttpEncodings.gzip)
        )
        val sbody = getGzipBody(responseAs[Array[Byte]])
        sbody must include regex "(?s).*function.*"
      }
    }
  }

  it should "return bridgescorer-client-opt.js to /public/bridgescorer-client-opt.js" in {
    assume(!useFastOptOnly)
    Get("/public/bridgescorer-client-opt.js").withAttributes(
      remoteAddress
    ) ~> Route.seal { myRouteWithLogging } ~> check {
      status mustBe OK
      getGzipBody(responseAs[Array[Byte]]) must include regex "(?s).*function.*"
    }
  }

  it should "return webjars/react-widgets/dist/css/react-widgets.css using webjars" in {
    Get("/public/react-widgets/dist/css/react-widgets.css").withAttributes(
      remoteAddress
    ) ~> Route.seal { html } ~> check {
      status mustBe OK
      getGzipBody(
        responseAs[Array[Byte]]
      ) must include regex "(?s).*.rw-input.*"
    }
  }

  it should "return webjars/react-widgets/dist/css/react-widgets.css using myRouteWithLogging" in {
    Get("/public/react-widgets/dist/css/react-widgets.css").withAttributes(
      remoteAddress
    ) ~> Route.seal { myRouteWithLogging } ~> check {
      status mustBe OK
      getGzipBody(
        responseAs[Array[Byte]]
      ) must include regex "(?s).*.rw-input.*"
    }
  }

  behavior of "MyService"

  it should "return NotFound for requests to /xxxx" in {
    Get("/xxxx") ~> Route.seal { myRoute } ~> check {
      handled mustBe true
      status mustBe NotFound
    }
  }

  it should "return NotFound for requests to /html/../com/..." in {
    Get("/html/../com/github/thebridsk/bridge/server/Server.class") ~> Route
      .seal { myRoute } ~> check {
      handled mustBe true
      status mustBe NotFound
    }
  }

  it should "return NotFound for requests to /js/../com/..." in {
    Get("/js/../com/github/thebridsk/bridge/server/Server.class") ~> Route
      .seal { myRoute } ~> check {
      handled mustBe true
      status mustBe NotFound
    }
  }

  behavior of "MyService utilities"

//  implicit def myRejectionHandler =
//  RejectionHandler.newBuilder()
//    .handle { case o: Any =>
//      fail("Rejection: "+o.getClass.getName+": "+o)
//      complete(BadRequest)
//    }
//    .handleAll[MethodRejection] { methodRejections =>
//      val names = methodRejections.map(_.supported.name)
//      complete(MethodNotAllowed, s"Can't do that! Supported: ${names mkString " or "}!")
//    }
//    .handleNotFound { complete(NotFound, "Not here!x") }
//    .result()

  val logentry: LogEntryV2 = LogEntryV2(
    "MyServiceSpec:100",
    "logger",
    1000000,
    "I",
    "http://example.com/index.html",
    "A detailed message %s, %s",
    "",
    List("Hello", "World")
  )

  it should "return OK for POST request to /v1/logging/entry" in {
    import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._
    Post("/v1/logger/entry", logentry).withAttributes(
      remoteAddress
    ) ~> /* handleRejections(myRejectionHandler) { */ myRouteWithLogging /* } */ ~> check {
      status mustBe NoContent
//      responseAs[String] mustBe "OK"
    }
  }

  it should "return OK and a server URL for GET /v1/rest/serverurls" in {

    Get("/v1/rest/serverurls") ~> Route.seal { myRoute } ~> check {
      handled mustBe true
      status mustBe OK

      import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._
      val resp = responseAs[Array[ServerURL]]
      resp.length mustBe 1
      resp(0) mustBe getURL(httpport)

      resp(0).serverUrl.length must be >= 1

      val urlpattern = """http://([^:]+):(\d+)/""".r
      resp(0).serverUrl(0) match {
        case urlpattern(h, p) =>
          testlog.debug(
            "Host is " + h + ", port is " + p + ", URL=" + resp(0).serverUrl(0)
          )
          p mustBe httpport.toString
        case _ => fail("Returned URL is not expected URL syntax")
      }
    }
  }

  it should "shutdown the server when /v1/shutdown?doit=yes is called" in {
    class Hook extends ShutdownHook {
      import scala.concurrent.duration._
      import scala.language.postfixOps

      var called = false
      def terminateServerIn(duration: Duration = 10 seconds): Future[_] = {
        called = true
        val terminatePromise = Promise[String]()
        Future { terminatePromise.success("Terminate") }
      }

    }
    val shutdownHook = new Hook

    MyService.shutdownHook = Some(shutdownHook)
    Post("/v1/shutdown").withAttributes(remoteAddressLocal) ~> Route.seal {
      logRouteWithIp
    } ~> check {
      status mustBe BadRequest
      responseAs[String] mustBe "Request is missing secret"
      shutdownHook.called mustBe false
    }
    Post("/v1/shutdown") ~> Route.seal { logRouteWithIp } ~> check {
      status mustBe BadRequest
      responseAs[String] mustBe "Request is missing secret"
      shutdownHook.called mustBe false
    }
    Get("/v1/shutdown").withAttributes(remoteAddressLocal) ~> Route.seal {
      logRouteWithIp
    } ~> check {
      status mustBe MethodNotAllowed
      responseAs[String] mustBe "Can't do that! Supported: POST!"
      shutdownHook.called mustBe false
    }
    Post("/v1/shutdown?doit=yes").withAttributes(remoteAddress) ~> Route.seal {
      logRouteWithIp
    } ~> check {
      // request fails, not from loopback interface
      status mustBe BadRequest
      responseAs[String] mustBe "Request not from valid address"
      shutdownHook.called mustBe false
    }
    Post("/v1/shutdown?doit=yes").withAttributes(remoteAddressLocal) ~> Route.seal {
      logRouteWithIp
    } ~> check {
      status mustBe NoContent
      shutdownHook.called mustBe true
    }
  }

  def getURL(port: Int): ServerURL = {
    import java.net.NetworkInterface
    import scala.jdk.CollectionConverters._
    import java.net.Inet4Address

    val x = NetworkInterface.getNetworkInterfaces.asScala
      .filter { x => x.isUp() && !x.isLoopback() }
      .flatMap { ni => ni.getInetAddresses.asScala }
      .filter { x => x.isInstanceOf[Inet4Address] }
      .map { x => "http://" + x.getHostAddress + ":" + port + "/" }
      .toList

    ServerURL(x)

  }
}
