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

trait ValidateCert

object ValidateCert extends Subcommand("validatecert") {
  import SSLKeyCommands.optionKeyDir

  val log = Logger[ValidateCert]()

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  import com.github.thebridsk.utilities.main.Converters._

  descr("Validate the private certificate on the server")

  banner(s"""
Validate the private certificate on the server

Syntax:
  ${SSLKeyCommands.cmdName} ${name} [options]
Options:""")

  val optionVerbose = toggle(
    name = "verbose",
    short = 'v',
    descrNo = "Don't add verbose option to commands",
    descrYes = "Add verbose option to commands",
    default = Some(false)
  )

  val optionAlias = opt[String](
    "alias",
    short = 'a',
    descr = "Alias for private certificate of server in keystore",
    required = true,
    default = None
  )

  val optionStorePW = opt[String](
    "storepw",
    short = 'p',
    descr = "Store PW for keystore",
    required = true,
  )

  val optionKeystore = opt[String](
    "keystore",
    short = 'k',
    descr = "Keystore filename",
    required = true,
  )

  val optionShow = toggle(
    name = "show",
    short = 's',
    descrNo = "do not show the certificate chain",
    descrYes = "show the certificate chain",
    default = Some(false)
  )

  def executeSubcommand(): Int = {

    try {
      val workingDirectory = optionKeyDir.toOption.map( _.jfile )
      val alias = optionAlias()
      val keystore = optionKeystore()
      val storepass = optionStorePW()

      if (GenerateSSLKeys.validateCert( alias, keystore, storepass, workingDirectory, optionShow.getOrElse(false) )) {
        log.info("Certificate is valid")
        0
      } else {
        log.info("Certificate is not valid")
        1
      }
    } catch {
      case x: Exception =>
        log.severe("Error validating private certificate", x)
        2
    }

  }
}
