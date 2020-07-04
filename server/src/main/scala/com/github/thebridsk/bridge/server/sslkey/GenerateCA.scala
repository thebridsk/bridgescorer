package com.github.thebridsk.bridge.sslkey

import com.github.thebridsk.utilities.main.Subcommand
import com.github.thebridsk.utilities.logging.Logger
import org.rogach.scallop._
import scala.concurrent.duration.Duration
import scala.reflect.io.Path
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStore
import java.io.File
import java.io.Reader
import java.io.BufferedReader
import scala.io.Source
import scala.io.BufferedSource
import scala.util.Left
import java.io.InputStream
import com.github.thebridsk.bridge.server.backend.BridgeServiceInMemory
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.MatchRubber
import akka.http.scaladsl.model.StatusCodes
import scala.concurrent.Await
import scala.concurrent.duration._
import com.github.thebridsk.bridge.server.backend.resource.Store
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.github.thebridsk.bridge.data.VersionedInstance
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.server.backend.BridgeServiceZipStore
import com.github.thebridsk.bridge.server.util.GenerateSSLKeys
import com.github.thebridsk.bridge.server.util.RootCAInfo
import com.github.thebridsk.bridge.server.util.ServerInfo

trait GenerateCA

object GenerateCA extends Subcommand("generateca") {
  import SSLKeyCommands.optionKeyDir

  val log = Logger[GenerateCA]()

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  import com.github.thebridsk.utilities.main.Converters._

  descr("Generate a CA private certificate")

  banner(s"""
Generate a CA private certificate

Syntax:
  ${SSLKeyCommands.cmdName} ${name} [options]
Options:""")

  val optionClean = toggle(
    name = "clean",
    noshort = true,
    descrNo = "fail if keys already exist",
    descrYes = "erase key files before generating keys",
    default = Some(false)
  )

  val optionVerbose = toggle(
    name = "verbose",
    short = 'v',
    descrNo = "Don't add verbose option to commands",
    descrYes = "add verbose option to commands",
    default = Some(false)
  )

  val optionCA = opt[String](
    "ca",
    noshort = true,
    descr = "base filename for CA certificate files",
    required = true,
  )

  val optionRootCAAlias = opt[String](
    "caalias",
    noshort = true,
    descr = "alias for CA private certificate in CA keystore",
    required = true,
    default = Some("rootCA")
  )

  val optionRootCADname = opt[String](
    "cadname",
    noshort = true,
    descr = "DName for CA",
    required = true,
    default = Some("CN=BridgeScoreKeeperCA, OU=BridgeScoreKeeper, O=BridgeScoreKeeper, L=New York, ST=New York, C=US")
  )

  val optionRootCAStorePW = opt[String](
    "castorepw",
    noshort = true,
    descr = "Store PW for CA keystore",
    required = true,
  )

  val optionRootCAKeyPW = opt[String](
    "cakeypw",
    noshort = true,
    descr = "Private key PW",
    required = true,
  )

  val optionTruststore = opt[String](
    "truststore",
    noshort = true,
    descr = "base filename for truststore",
    required = true,
  )
  val optionTrustPW = opt[String](
    "trustpw",
    noshort = true,
    descr = "password for truststore",
    required = true,
  )

  val optionValidityCA = opt[Int](
    "validityCA",
    noshort = true,
    descr = "the validity of the CA certificate in days, default 1 year",
    default = Some(367),
    validate = { days => days > 0 }
  )

  def executeSubcommand(): Int = {

    try {
      val workingDirectory = optionKeyDir()

      val rootca = RootCAInfo(
        alias = optionRootCAAlias(),
        rootca = optionCA(),
        dname = optionRootCADname(),
        keypass = optionRootCAKeyPW(),
        storepass = optionRootCAStorePW(),
        workingDirectory = Some(workingDirectory.jfile),
        good = false,
        verbose = optionVerbose(),
        validityCA = optionValidityCA().toString,
        truststoreprefix = optionTruststore.toOption,
        trustpass = optionTrustPW.toOption
      )

      if (optionClean()) {
        rootca.deleteOldServerCerts()
      }
      if (!workingDirectory.isDirectory) workingDirectory.createDirectory()

      val ca = rootca.generateRootCA()

      val trust = ca.trustRootCA(optionTruststore(), optionTrustPW())

      0
    } catch {
      case x: Exception =>
        log.severe("Error generating CA certificate", x)
        1
    }

  }
}
