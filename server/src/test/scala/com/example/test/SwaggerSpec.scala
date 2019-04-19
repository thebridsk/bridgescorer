package com.example.test

import org.scalatest.Finders
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import com.example.test.backend.BridgeServiceTesting
import com.example.service.MyService
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Flow
import org.scalatest._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.unmarshalling._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.RemoteAddress.IP
import java.net.InetAddress
import com.example.rest.ServerPort
import akka.http.scaladsl.model.headers.HttpEncodings
import akka.http.scaladsl.coding.Gzip
import akka.http.scaladsl.coding.Deflate
import akka.http.scaladsl.coding.NoCoding
import scala.concurrent.duration._
import scala.reflect.ClassTag
import akka.http.scaladsl.unmarshalling.Unmarshal
import scala.concurrent.Await
import akka.http.scaladsl.util.FastFuture._

class SwaggerSpec extends FlatSpec with ScalatestRouteTest with MustMatchers with MyService {
  val restService = new BridgeServiceTesting

  val httpport = 8080
  override
  def ports = ServerPort( Option(httpport), None )

  implicit lazy val actorSystem = system
  implicit lazy val actorExecutor = executor
  implicit lazy val actorMaterializer = materializer

  TestStartLogging.startLogging()

//  behavior of "The server for swagger"

//  it should "find web/swagger-ui/index.html as a resource" in {
//    val theClassLoader = getClass.getClassLoader
//    val theResource = theClassLoader.getResource("web/swagger-ui/index.html")
//    theResource must not be null
//  }

  behavior of "the Swagger Server api"

  val remoteAddress = `Remote-Address`( IP( InetAddress.getLocalHost, Some(12345) ))

  it should "return the /v1/docs/ should be a redirect" in {
    Get("/v1/docs/") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe PermanentRedirect
      header("Location") match {
        case Some(httpheader) =>
          httpheader.value() mustBe "/public/swagger-ui-dist/index.html.gz?url=/v1/api-docs/swagger.yaml&validatorUrl="
        case None =>
          fail("Did not get location header")
      }
    }
  }

  def decodeResponse(response: HttpResponse): HttpResponse = {
    val decoder = response.encoding match {
      case HttpEncodings.gzip ⇒
        Gzip
      case HttpEncodings.deflate ⇒
        Deflate
      case HttpEncodings.identity ⇒
        NoCoding
      case x =>
        fail(s"Unknown encoding ${x}")
    }
    decoder.decodeMessage(response)
  }

  def httpResponseAs[T: FromResponseUnmarshaller: ClassTag]( response: HttpResponse )(implicit timeout: Duration = 1.second): T = {
    def msg(e: Throwable) = s"Could not unmarshal response to type '${implicitly[ClassTag[T]]}' for `responseAs` assertion: $e\n\nResponse was: $response"
    Await.result(Unmarshal(response).to[T].fast.recover[T] { case error ⇒ failTest(msg(error)) }, timeout)
  }

  it should "return the /public/apidocs.html" in {
    Get("/public/apidocs.html") ~> addHeader(`Accept-Encoding`(HttpEncodings.gzip)) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe OK
      header("Content-Encoding") match {
        case Some(ce) =>
          ce.value() mustBe "gzip"
        case None =>
          fail("Did not get content-encoding header")
      }
      val decoded = decodeResponse(response)
      httpResponseAs[String](decoded) must include regex """(?s)<html[ >].*<script[ >].*swagger-ui.*</script>.*</html>"""
    }
  }

  it should "return the /public/apidocs.html.gz" in {
    Get("/public/apidocs.html.gz") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe OK
      header("Content-Encoding") match {
        case Some(ce) =>
          ce.value() mustBe "gzip"
        case None =>
          fail("Did not get content-encoding header")
      }
      val decoded = decodeResponse(response)
      httpResponseAs[String](decoded) must include regex """(?s)<html[ >].*<script[ >].*swagger-ui.*</script>.*</html>"""
    }
  }

  it should "return the /public/swagger-ui-dist/index.html" in {
    Get("/public/swagger-ui-dist/index.html") ~> addHeader(`Accept-Encoding`(HttpEncodings.gzip)) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe OK
      header("Content-Encoding") match {
        case Some(ce) =>
          ce.value() mustBe "gzip"
        case None =>
          fail("Did not get content-encoding header")
      }
      val decoded = decodeResponse(response)
      httpResponseAs[String](decoded) must include regex """(?s)<html[ >].*<script[ >].*swagger-ui.*</script>.*</html>"""
    }
  }

  it should "return the /public/swagger-ui-dist/index.html.gz" in {
    Get("/public/swagger-ui-dist/index.html.gz") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe OK
      header("Content-Encoding") match {
        case Some(ce) =>
          ce.value() mustBe "gzip"
        case None =>
          fail("Did not get content-encoding header")
      }
      val decoded = decodeResponse(response)
      httpResponseAs[String](decoded) must include regex """(?s)<html[ >].*<script[ >].*swagger-ui.*</script>.*</html>"""
    }
  }

  it should "return the swagger.yaml /v1/api-docs" in {
    Get("/v1/api-docs") ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe PermanentRedirect
      header("Location") match {
        case Some(httpheader) =>
          httpheader.value() mustBe "/v1/api-docs/swagger.yaml"
        case None =>
          fail("Did not get location header")
      }
    }
  }

  /**
   * @return (timeInNanos, result)
   */
  def time[R](block: => R) = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    val nanos = t1 - t0
    (nanos,result)
  }

  it should "return the swagger.yaml from /v1/api-docs/swagger.yaml and should not contain the string 'Function1'" in {
    Get("/v1/api-docs/swagger.yaml") ~> addHeader(`Accept-Encoding`(HttpEncodings.gzip)) ~> addHeader(remoteAddress) ~> myRouteWithLogging ~> check {
      status mustBe OK
      val swagger = httpResponseAs[String](decodeResponse(response))
      swagger must include regex "(?s)Scorekeeper for a Duplicate bridge, Chicago bridge, and Rubber bridge\\."
      withClue("""Found Function1[RequestContextFutureRouteResult],
                 |most likely because an @ApiOperation annotation is missing response attribute
                 |Start server, goto swagger docs, and search logs for 'Function1'
                 |or a getX or setX method in a model class
                 |add '@ApiModelProperty(hidden = true)' to method
                 |""".stripMargin) {
        swagger must not include ("""Function1""")
        swagger must not include ("""Function1RequestContextFutureRouteResult""")
      }
    }
  }
}
