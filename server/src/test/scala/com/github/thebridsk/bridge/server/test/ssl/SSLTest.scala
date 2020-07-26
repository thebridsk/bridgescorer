package com.github.thebridsk.bridge.server.test.ssl

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import com.github.thebridsk.bridge.server.test.util.TestServer
import java.net.HttpURLConnection
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory
import java.security.SecureRandom
import java.io.FileInputStream
import com.github.thebridsk.bridge.server.test.util.EventuallyUtils
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.utilities.logging.Logger
import java.util.concurrent.TimeUnit
import org.scalatest.time.Span
import org.scalatest.time.Millis
import com.github.thebridsk.bridge.server.test.util.MonitorTCP
import java.net.URL

object SSLTest {

  val testlog: Logger = Logger[SSLTest]()
}

/**
 * Test going from the table view, by hitting a board button,
 * to the names view, to the hand view.
 * @author werewolf
 */
class SSLTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll with EventuallyUtils {
  import Eventually.{ patienceConfig => _, _ }

  TestStartLogging.startLogging()

  import SSLTest._

  import scala.concurrent.duration._

  val timeoutMillis = 15000
  val intervalMillis = 500

//  case class MyDuration( timeout: Long, units: TimeUnit )
  type MyDuration = Duration
  val MyDuration = Duration

  implicit val timeoutduration: FiniteDuration = MyDuration( 60, TimeUnit.SECONDS )

  val defaultPatienceConfig: PatienceConfig = PatienceConfig(timeout=scaled(Span(timeoutMillis, Millis)), interval=scaled(Span(intervalMillis,Millis)))
  implicit def patienceConfig: PatienceConfig = defaultPatienceConfig

  override
  def beforeAll(): Unit = {

    MonitorTCP.nextTest()
    try {
      TestServer.start(true)
    } catch {
      case e: Throwable =>
        testlog.severe( "Error starting test server", e)
        afterAll()
        throw e
    }
  }

  override
  def afterAll(): Unit = {
    TestServer.stop()
  }

  /**
    * Get the ssl context
    */
  def trustSSLContext(
      certPassword: Option[String] = None,
      certificate: Option[String] = None
  ): SSLContext = {
    val password = certPassword.getOrElse("abcdef").toCharArray // default NOT SECURE
    val context = SSLContext.getInstance("TLS")
    val ks = certificate match {
      case Some(cert) =>
        val ks = if (cert.endsWith(".jks")) {
          KeyStore.getInstance("JKS")
        } else {
          KeyStore.getInstance("PKCS12")
        }
        ks.load(new FileInputStream(cert), password)
        ks
      case None =>
        val ks = KeyStore.getInstance("JKS")
        ks
    }
    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)
    val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    trustManagerFactory.init(ks)
    context.init(
      keyManagerFactory.getKeyManagers(),
      trustManagerFactory.getTrustManagers,
      new SecureRandom
    )
    context
  }

  behavior of "the server using SSL connections"

  it should "redirect an http request and get the redirected data" in {

    try {
      val path = "v1/rest/serverversion"
      val url: URL = new URL(TestServer.hosturl+path)
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      conn.setInstanceFollowRedirects(false)
      conn.setRequestProperty("Accept-Encoding","gzip, deflate")
      val status = conn.getResponseCode
      status mustBe 308

      val location = new URL( conn.getHeaderField("Location") )
      location.getProtocol mustBe "https"
      val port = location.getPort()
      port mustBe TestServer.getHttpsPort
      location.getPath() mustBe s"/$path"

      val conninput = conn.getInputStream()
      val buf = new Array[Byte](1024)
      while ( conninput.read(buf) >= 0 ) {}
      conninput.close()

      testlog.info(s"Redirect location is $location")
      // Thread.sleep(60*1000L)

      val conns = location.openConnection().asInstanceOf[HttpsURLConnection]
      val socketFactory = trustSSLContext( Some("abcdef"), Some("key/examplebridgescorekeepertrust.jks")).getSocketFactory()
      // val socketFactory = SSLSocketFactory.getDefault().asInstanceOf[SSLSocketFactory]
      conns.setSSLSocketFactory(socketFactory)
      conns.setInstanceFollowRedirects(false)
      conns.setRequestProperty("Accept-Encoding","gzip, deflate")
      conns.getResponseCode mustBe 200

      val connsinput = conns.getInputStream()
      while ( connsinput.read(buf) > 0 ) {}
      connsinput.close()

    } catch {
      case x: Throwable =>
        testlog.severe("Error in SSLTest", x)
        throw x
    }

  }

}
