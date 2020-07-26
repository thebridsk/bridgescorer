package com.github.thebridsk.bridge.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.server.test.TestStartLogging
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.server.test.util.MonitorTCP
import org.scalatest.BeforeAndAfterAll
import com.github.thebridsk.bridge.server.util.MyProcess
import com.github.thebridsk.bridge.server.test.util.TestServer
import java.io.File

object CertTest {

  val testlog: Logger = Logger[CertTest]()
}

/**
 * Test going from the table view, by hitting a board button,
 * to the names view, to the hand view.
 * @author werewolf
 */
class CertTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  TestStartLogging.startLogging()


  override
  def beforeAll(): Unit = {

    MonitorTCP.nextTest()
  }

  lazy val jar: String = {
    TestServer.getProp("BridgeScoreKeeperJar").getOrElse( fail("Property BridgeScoreKeeperJar must be specified"))
  }

  lazy val keydir: String = {
    TestServer.getProp("BridgeScoreKeeperKeyDir").getOrElse( "key" )
  }

  val proc = new MyProcess

  val cwd = new File(".")

  behavior of "Certificates"

  it should "test the server certificate generation" in {

    withClue( "Creating new certificate without server IP") {
      val p = proc.exec(
        List(
          "java",
          "-jar", jar,
          "sslkey",
          "-d", keydir,
          "generateselfsigned",
          "-a", "bsk",
          "--ca", "examplebridgescorekeeperca",
          "--caalias", "ca",
          "--cadname", "CN=BridgeScoreKeeperCA, OU=BridgeScoreKeeper, O=BridgeScoreKeeper, L=New York, ST=New York, C=US",
          "--cakeypw", "abcdef",
          "--castorepw", "abcdef",
          "--dname", "CN=BridgeScoreKeeper, OU=BridgeScoreKeeper, O=BridgeScoreKeeper, L=New York, ST=New York, C=US",
          "--keypass", "abcdef",
          "--server", "examplebridgescorekeeper",
          "--storepass", "abcdef",
          "--trustpw", "abcdef",
          "--truststore", "examplebridgescorekeepertrust",
          "-v",
          "--nginx",
          "--clean",
        ),
        cwd
      )
      val rc = p.waitFor()

      rc mustBe 0
    }

    withClue( "First validity check must fail, server IP not in certificate") {

      val p = proc.exec(
        List(
          "java",
          "-jar", jar,
          "sslkey",
          "-d", keydir,
          "validatecert",
          "-a", "bsk",
          "-k", "examplebridgescorekeeper.jks",
          "-p", "abcdef",
          "-s"
        ),
        cwd
      )
      val rc = p.waitFor()

      rc mustBe 1
    }

    withClue( "Adding server IP to new certificate") {
      val p = proc.exec(
        List(
          "java",
          "-jar", jar,
          "sslkey",
          "-d", keydir,
          "generateservercert",
          "-a", "bsk",
          "--ca", "examplebridgescorekeeperca",
          "--caalias", "ca",
          "--cakeypw", "abcdef",
          "--castorepw", "abcdef",
          "--keypass", "abcdef",
          "--server", "examplebridgescorekeeper",
          "--storepass", "abcdef",
          "--trustpw", "abcdef",
          "--truststore", "examplebridgescorekeepertrust",
          "-v",
          "--nginx",
          "--addmachineip"
        ),
        cwd
      )
      val rc = p.waitFor()

      rc mustBe 0
    }

    withClue( "Second validity check must pass, server IP is in certificate") {
      val p = proc.exec(
        List(
          "java",
          "-jar", jar,
          "sslkey",
          "-d", keydir,
          "validatecert",
          "-a", "bsk",
          "-k", "examplebridgescorekeeper.jks",
          "-p", "abcdef",
          "-s"
        ),
        cwd
      )
      val rc = p.waitFor()

      rc mustBe 0
    }

  }

}
