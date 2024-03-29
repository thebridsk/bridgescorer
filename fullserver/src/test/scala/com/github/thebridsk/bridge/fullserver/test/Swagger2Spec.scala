package com.github.thebridsk.bridge.fullserver.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.server.test.backend.BridgeServiceTesting
import com.github.thebridsk.bridge.server.service.MyService
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling._
import com.github.thebridsk.bridge.server.rest.ServerPort
import akka.http.scaladsl.model.headers.HttpEncodings
import akka.http.scaladsl.coding.Coders.Gzip
import akka.http.scaladsl.coding.Coders.Deflate
import akka.http.scaladsl.coding.Coders.NoCoding
import scala.concurrent.duration._
import scala.reflect.ClassTag
import akka.http.scaladsl.unmarshalling.Unmarshal
import scala.concurrent.Await
import akka.http.scaladsl.util.FastFuture._
import com.github.thebridsk.utilities.file.FileIO
import java.io.File
import org.scalactic.source.Position
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.bridge.server.test.RoutingSpec

class Swagger2Spec
    extends AnyFlatSpec
    with ScalatestRouteTest
    with Matchers
    with MyService
    with RoutingSpec {
  val restService = new BridgeServiceTesting

  val httpport = 8080
  override def ports: ServerPort = ServerPort(Option(httpport), None)

  // scalafix:off
  implicit lazy val actorSystem = system
  implicit lazy val actorExecutor = executor
  implicit lazy val actorMaterializer = materializer
  // scalafix:on

  TestStartLogging.startLogging()

//  behavior of "The server for swagger"

//  it should "find web/swagger-ui/index.html as a resource" in {
//    val theClassLoader = getClass.getClassLoader
//    val theResource = theClassLoader.getResource("web/swagger-ui/index.html")
//    theResource must not be null
//  }

  behavior of "the Swagger Server api"

  // disabled because of vulnerability https://github.com/swagger-api/swagger-ui/issues/4872
  // it should "return the /v1/docs/ should be a redirect" in {
  //   Get("/v1/docs/").withAttributes(
  //     remoteAddress
  //   ) ~> myRouteWithLogging ~> check {
  //     status mustBe PermanentRedirect
  //     header("Location") match {
  //       case Some(httpheader) =>
  //         httpheader
  //           .value() mustBe "/public/swagger-ui-dist/index.html.gz?url=/v1/api-docs/swagger.yaml&validatorUrl="
  //       case None =>
  //         fail("Did not get location header")
  //     }
  //   }
  // }

  def decodeResponse(response: HttpResponse): HttpResponse = {
    val decoder = response.encoding match {
      case HttpEncodings.gzip =>
        Gzip
      case HttpEncodings.deflate =>
        Deflate
      case HttpEncodings.identity =>
        NoCoding
      case x =>
        fail(s"Unknown encoding ${x}")
    }
    decoder.decodeMessage(response)
  }

  def httpResponseAs[T: FromResponseUnmarshaller: ClassTag](
      response: HttpResponse
  )(implicit timeout: Duration = 1.second): T = {
    def msg(e: Throwable) =
      s"Could not unmarshal response to type '${implicitly[ClassTag[T]]}' for `responseAs` assertion: $e\n\nResponse was: $response"
    Await.result(
      Unmarshal(response).to[T].fast.recover[T] {
        case error => failTest(msg(error))
      },
      timeout
    )
  }

  it should "return the /public/apidocs.html" in {
    Get("/public/apidocs.html").withAttributes(remoteAddress) ~> addHeader(
      `Accept-Encoding`(HttpEncodings.gzip)
    ) ~> myRouteWithLogging ~> check {
      status mustBe OK
      header("Content-Encoding") match {
        case Some(ce) =>
          ce.value() mustBe "gzip"
        case None =>
          fail("Did not get content-encoding header")
      }
      val decoded = decodeResponse(response)
      httpResponseAs[String](
        decoded
      ) must include regex """(?s)<html[ >].*<script[ >].*swagger-ui.*</script>.*</html>"""
    }
  }

  it should "return the /public/apidocs.html.gz" in {
    Get("/public/apidocs.html.gz").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      status mustBe OK
      header("Content-Encoding") match {
        case Some(ce) =>
          ce.value() mustBe "gzip"
        case None =>
          fail("Did not get content-encoding header")
      }
      val decoded = decodeResponse(response)
      httpResponseAs[String](
        decoded
      ) must include regex """(?s)<html[ >].*<script[ >].*swagger-ui.*</script>.*</html>"""
    }
  }

  it should "return the /public/swagger-ui-dist/index.html" in {
    Get("/public/swagger-ui-dist/index.html").withAttributes(
      remoteAddress
    ) ~> addHeader(
      `Accept-Encoding`(HttpEncodings.gzip)
    ) ~> myRouteWithLogging ~> check {
      status mustBe OK
      header("Content-Encoding") match {
        case Some(ce) =>
          ce.value() mustBe "gzip"
        case None =>
          fail("Did not get content-encoding header")
      }
      val decoded = decodeResponse(response)
      httpResponseAs[String](
        decoded
      ) must include regex """(?s)<html[ >].*<script[ >].*swagger-ui.*</script>.*</html>"""
    }
  }

  it should "return the /public/swagger-ui-dist/index.html.gz" in {
    Get("/public/swagger-ui-dist/index.html.gz").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
      status mustBe OK
      header("Content-Encoding") match {
        case Some(ce) =>
          ce.value() mustBe "gzip"
        case None =>
          fail("Did not get content-encoding header")
      }
      val decoded = decodeResponse(response)
      httpResponseAs[String](
        decoded
      ) must include regex """(?s)<html[ >].*<script[ >].*swagger-ui.*</script>.*</html>"""
    }
  }

  it should "return the swagger.yaml /v1/api-docs" in {
    Get("/v1/api-docs").withAttributes(
      remoteAddress
    ) ~> myRouteWithLogging ~> check {
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
  def time[R](block: => R): (Long, R) = {
    val t0 = System.nanoTime()
    val result = block // call-by-name
    val t1 = System.nanoTime()
    val nanos = t1 - t0
    (nanos, result)
  }

  it should "return the swagger.yaml from /v1/api-docs/swagger.yaml and should not contain the string 'Function1'" in {
    Get("/v1/api-docs/swagger.yaml").withAttributes(remoteAddress) ~> addHeader(
      `Accept-Encoding`(HttpEncodings.gzip)
    ) ~> myRouteWithLogging ~> check {
      status mustBe OK
      val swagger = httpResponseAs[String](decodeResponse(response))
      FileIO.writeFile(new File("target/swagger.yaml"), swagger)
      swagger must include regex "(?s)Scorekeeper for a Duplicate bridge, Chicago bridge, and Rubber bridge\\."
      withClue(
        """Found Function1[RequestContextFutureRouteResult],
          |most likely because an @ApiOperation annotation is missing response attribute
          |Start server, goto swagger docs, and search logs for 'Function1'
          |or a getX or setX method in a model class
          |add '@ApiModelProperty(hidden = true)' to method
          |""".stripMargin
      ) {
        swagger must not include ("""Function1""")
        swagger must not include ("""Function1RequestContextFutureRouteResult""")
      }
    }
  }

  private object ItVerbStringTest {
    // can't extend AnyVal, ItVerbString is a nested class of trait FlatSpecLike
    implicit class ItVerbStringWrapper(val itVerb: ItVerbString) {
      def whenFileExists(
          testFun: => Any /* Assertion */
      )(implicit pos: Position): Unit = {
        if (srcfile.isFile()) itVerb.in(testFun)
        else itVerb.ignore(testFun)
      }
    }
  }

  import ItVerbStringTest._

  val srcfile = new File("src/main/public/apidocs.html")

  it should "find /v1/api-docs/swagger.yaml in apidocs.html and replace it with /public/swagger.yaml" whenFileExists {

    val destfile = new File("target/apidocs.html")
    val apidocs = FileIO.readFile(srcfile)
    val newapidocs =
      apidocs.replaceAll("/v1/api-docs/swagger.yaml", "/public/swagger.yaml")
    apidocs must not be (newapidocs)
    FileIO.writeFile(destfile, newapidocs)
  }
}
