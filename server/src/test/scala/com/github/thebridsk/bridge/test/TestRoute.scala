package com.github.thebridsk.bridge.test

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import akka.event.LoggingAdapter
import akka.event.Logging
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.`Remote-Address`
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Flow
import org.scalatest._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.unmarshalling.FromResponseUnmarshaller
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.RouteResult.Rejected
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.LogEntry
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.UnacceptedResponseEncodingRejection
import akka.http.scaladsl.server.directives.LoggingMagnet
import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.RemoteAddress.IP
import java.net.InetAddress
import com.github.thebridsk.utilities.logging.Logger
import java.util.logging.Level

object TestRoute {

  implicit class PimpString( val underlying: String ) extends AnyVal {
     def stripMarginWithNewline(newline: String) = underlying.stripMargin.replace("\r\n", "\n").replace("\n", newline)
  }

}

import TestRoute._

/**
 * Some test cases that show how to write tests
 * See http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0/scala/http/routing-dsl/testkit.html
 */
class TestRoute extends RoutingSpec {

  val testlog = Logger[TestRoute]

  testlog.fine(getClass.getName+":")

  var debugMsg = ""

  def resetDebugMsg(): Unit = { debugMsg = "" }

  implicit val log = new LoggingAdapter {
    def isErrorEnabled = true
    def isWarningEnabled = true
    def isInfoEnabled = true
    def isDebugEnabled = true

    def notifyError(message: String): Unit = { debugMsg += message + '\n' }
    def notifyError(cause: Throwable, message: String): Unit = { debugMsg += message + '\n' }
    def notifyWarning(message: String): Unit = { debugMsg += message + '\n' }
    def notifyInfo(message: String): Unit = { debugMsg += message + '\n' }
    def notifyDebug(message: String): Unit = { debugMsg += message + '\n' }
  }

  def logToDebug(s: String)( req: HttpRequest ): Unit = {
    toLog(s+": "+req.toString())
  }

  def toLog( s: String ): Unit = {
    debugMsg += s+'\n'
  }

  def logRequestDebug(s: String) = DebuggingDirectives.logRequest(LoggingMagnet(_ => logToDebug(s) _))

  behavior of "The 'logRequest' directive"

  it should "produce a proper log message for incoming requests" in {
    resetDebugMsg()
    Get("/hello") ~> logRequestDebug("1") { completeOk } ~> check {
      status mustBe StatusCodes.OK
      debugMsg mustBe "1: HttpRequest(HttpMethod(GET),http://example.com/hello,List(),HttpEntity.Strict(none/none,ByteString()),HttpProtocol(HTTP/1.1))\n"
    }
  }

  def respToString(res: Any): String = res match {
    case Complete(x) => x.toString
    case _           => "unknown response part "+res.getClass().getName
  }

  def logResultToDebug(s: String)(res: Any): Unit = toLog(s+": "+respToString(res))
  def logResultDebug(s: String) = DebuggingDirectives.logResult(LoggingMagnet(_ => logResultToDebug(s) _))

  behavior of "The 'logResult' directive"

  it should "produce a proper log message for outgoing responses" in {
    resetDebugMsg()
    Get("/hello") ~> logResultDebug("2") { completeOk } ~> check {
      status mustBe StatusCodes.OK
      debugMsg mustBe "2: HttpResponse(200 OK,List(),HttpEntity.Strict(none/none,ByteString()),HttpProtocol(HTTP/1.1))\n"
    }
  }

  def logReqRespToDebug(s: String)(req: HttpRequest)(res: Any): Unit = {
    toLog(s+": Response for\n"
          +"  Request : "+req.toString()+"\n"
          +"  Response: "+respToString(res)
        )
  }
  def logRequestResultDebug(s: String) = DebuggingDirectives.logRequestResult(LoggingMagnet(_ => logReqRespToDebug(s)))

  behavior of "The 'logRequestResponse' directive"

  it should "produce proper log messages for outgoing responses, thereby showing the corresponding request" in {
    resetDebugMsg()
    Get("/hello") ~> logRequestResultDebug("3") { route } ~> check {
      status mustBe StatusCodes.OK
      debugMsg mustBe """|3: Response for
                         |  Request : HttpRequest(HttpMethod(GET),http://example.com/hello,List(),HttpEntity.Strict(none/none,ByteString()),HttpProtocol(HTTP/1.1))
                         |  Response: HttpResponse(200 OK,List(),HttpEntity.Strict(none/none,ByteString()),HttpProtocol(HTTP/1.1))
                         |""".stripMarginWithNewline("\n")
    }
  }

  def route = {
    completeOk
  }

  import akka.http.scaladsl.model.headers.HttpEncodings._
  behavior of "the compressResponse()"
  it should "produce identity responses when accept encoding identity is used as an argument" in {
    Get("/") ~> `Accept-Encoding`(identity) ~> compressRoute ~> check {
      rejection === UnacceptedResponseEncodingRejection(gzip)
    }
  }
  it should "produce gzipped responses when accept encoding Gzip is used as an argument" in {
    Get("/") ~> compressRoute ~> check {
      status mustBe StatusCodes.OK
      header("Content-Encoding") mustBe Some(`Content-Encoding`(gzip))
    }
  }
  it should "produce gzipped responses when accept encoding Gzip, deflate is used as an argument" in {
    Get("/") ~> `Accept-Encoding`(gzip, deflate) ~> compressRoute ~> check {
      status mustBe StatusCodes.OK
      header("Content-Encoding") mustBe Some(`Content-Encoding`(gzip))
    }
  }
  it should "produce gzipped responses when accept encoding deflate is used as an argument" in {
    Get("/") ~> `Accept-Encoding`(deflate) ~> compressRoute ~> check {
      rejection === UnacceptedResponseEncodingRejection(gzip)
    }
  }


  behavior of "the compressResponse(Gzip)"

  it should "produce rejection when accept encoding identity is used as an argument" in {
    Get("/") ~> `Accept-Encoding`(identity) ~> compressGzipRoute ~> check {
      rejection === UnacceptedResponseEncodingRejection(gzip)
    }
  }
  it should "produce gzipped responses when accept encoding Gzip is used as an argument" in {
    Get("/") ~> compressGzipRoute ~> check {
      status mustBe StatusCodes.OK
      header("Content-Encoding") mustBe Some(`Content-Encoding`(gzip))
    }
  }
  it should "produce gzipped responses when accept encoding Gzip, deflate is used as an argument" in {
    Get("/") ~> `Accept-Encoding`(gzip, deflate) ~> compressGzipRoute ~> check {
      status mustBe StatusCodes.OK
      header("Content-Encoding") mustBe Some(`Content-Encoding`(gzip))
    }
  }
  it should "produce gzipped responses when accept encoding deflate, Gzip is used as an argument" in {
    Get("/") ~> `Accept-Encoding`(gzip, deflate) ~> compressGzipRoute ~> check {
      status mustBe StatusCodes.OK
      header("Content-Encoding") mustBe Some(`Content-Encoding`(gzip))
    }
  }
  it should "produce rejection when accept encoding deflate is used as an argument" in {
    Get("/") ~> `Accept-Encoding`(deflate) ~> compressGzipRoute ~> check {
      rejection === UnacceptedResponseEncodingRejection(gzip)
    }
  }

  behavior of "extracting client IP address"

  def extractRemote: PartialFunction[HttpHeader, String] = {
    case h: `Remote-Address` => h.toString()
    case x => x.getClass().toString()
  }
  def myExtractClientIP =
    logRequest(("myExtractClientIP", Logging.InfoLevel)) {
//      headerValueByName("Remote-Address") { ip =>
      headerValuePF(extractRemote) { ip =>
        complete(ip)
      } ~
      complete("oops")

    }

  val remoteAddress = `Remote-Address`( IP( InetAddress.getLocalHost, Some(12345) ))

  it should "return an OK for /" in {
    Get("/") ~> addHeader(remoteAddress) ~> Route.seal { myExtractClientIP } ~> check {
      status mustBe StatusCodes.OK
      responseAs[String] mustBe "Remote-Address: "+InetAddress.getLocalHost.getHostAddress+":12345"
    }
  }


  behavior of "the compress"

  it should "produce identity responses when accept encoding identity is used as an argument" in {
    Get("/") ~> `Accept-Encoding`(identity) ~> compress ~> check {
      status mustBe StatusCodes.OK
      header("Content-Encoding") mustBe None
    }
  }
  it should "produce gzipped responses when accept encoding Gzip is used as an argument" in {
    Get("/") ~> compress ~> check {
      status mustBe StatusCodes.OK
      header("Content-Encoding") mustBe Some(`Content-Encoding`(deflate))
    }
  }
  // akka http 2.0.1 produces deflate instead of the expected gzip when q values or omitted
  it should "produce gzipped responses when accept encoding Gzip, deflate is used as an argument" in {
    Get("/") ~> `Accept-Encoding`(gzip.withQValue(1), deflate.withQValue(0.5)) ~> compress ~> check {
      status mustBe StatusCodes.OK
      header("Content-Encoding") mustBe Some(`Content-Encoding`(gzip))
    }
  }
  it should "produce deflate responses when accept encoding deflate, Gzip is used as an argument" in {
    Get("/") ~> `Accept-Encoding`(deflate, gzip) ~> compress ~> check {
      status mustBe StatusCodes.OK
      header("Content-Encoding") mustBe Some(`Content-Encoding`(deflate))
    }
  }
  it should "produce deflate responses when accept encoding deflate is used as an argument" in {
    Get("/") ~> `Accept-Encoding`(deflate) ~> compress ~> check {
      status mustBe StatusCodes.OK
      header("Content-Encoding") mustBe Some(`Content-Encoding`(deflate))
    }
  }

  import akka.http.scaladsl.coding._
  val compressRoute = encodeResponseWith(Gzip) { complete("content") }
  val compressGzipRoute = encodeResponseWith(Gzip) { complete("content") }
  val compressDeflateRoute = encodeResponseWith(Deflate) { complete("content") }

  val compress = encodeResponseWith(Deflate, Gzip, NoCoding) { complete("content")}

}
