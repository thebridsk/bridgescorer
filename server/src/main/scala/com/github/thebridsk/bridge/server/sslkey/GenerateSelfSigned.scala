package com.github.thebridsk.bridge.sslkey

import com.github.thebridsk.utilities.main.Subcommand
import com.github.thebridsk.utilities.logging.Logger
import org.rogach.scallop._
import scala.concurrent.duration.Duration
import com.github.thebridsk.bridge.server.util.RootCAInfo
import com.github.thebridsk.bridge.server.util.ServerInfo

trait GenerateSelfSigned

object GenerateSelfSigned extends Subcommand("generateselfsigned") {
  import SSLKeyCommands.optionKeyDir

  val log = Logger[GenerateSelfSigned]()

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))


  descr("Generate a CA certificate and a Server certificate")

  banner(s"""
Generate a CA certificate and a Server certificate

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

  val optionNginx = toggle(
    name = "nginx",
    noshort = true,
    descrNo = "Don't generate certificate for nginx",
    descrYes = "generate certificate for nginx",
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

  val optionServer = opt[String](
    "server",
    short = 's',
    descr = "base filename for server certificate files",
    required = true,
  )

  val optionAlias = opt[String](
    "alias",
    short = 'a',
    descr = "server certificate alias in keystore",
    required = true,
  )

  val optionDname = opt[String](
    "dname",
    short = 'd',
    descr = "server dname",
    required = true,
  )

  val optionKeypass = opt[String](
    "keypass",
    noshort = true,
    descr = "password for server private certificate",
    required = true,
  )

  val optionStorepass = opt[String](
    "storepass",
    noshort = true,
    descr = "password for server keystore",
    required = true,
  )

  val optionValidityCA = opt[Int](
    "validityCA",
    noshort = true,
    descr = "the validity of the CA certificate in days, default 1 year",
    default = Some(367),
    validate = { days => days > 0 }
  )

  val optionValidityServer = opt[Int](
    "validityServer",
    noshort = true,
    descr = "the validity of the server certificate in days, default 1 year",
    default = Some(367),
    validate = { days => days > 0 }
  )

  val patternIP = """(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})""".r

  val optionIP = opt[List[String]](
    name = "ip",
    noshort = true,
    descr = "list of IP addresses to add to server certificate, comma separated.  127.0.0.1 is always added",
    default = Some(List()),
    validate = { l =>
      log.warning(s"Validating ip option: ${l.mkString(" ")}")
      l.find { s => s match {
        case patternIP(ip) => false    // this is good, but we are looking for a bad entry
        case _ => true
      } }.isEmpty
    }        // empty means no bad entries were found
  )( listArgConverter( s => s ))

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

      val server = ServerInfo(
        workingDirectory = rootca.workingDirectory,
        alias = optionAlias(),
        server = optionServer(),
        dname = optionDname(),
        keypass = optionKeypass(),
        storepass = optionStorepass(),
        good = false,
        verbose = optionVerbose(),
        validityServer = optionValidityServer().toString,
      )

      if (optionClean()) {
        rootca.deleteOldServerCerts()
        server.deleteOldServerCerts()
      }
      if (!workingDirectory.isDirectory) workingDirectory.createDirectory()

      val ca = rootca.generateRootCA()

      val trust = ca.trustRootCA(optionTruststore(), optionTrustPW())

      val serv = server.
                    generateServerCSR().
                    generateServerCert(
                      rootcaAlias = ca.alias,
                      rootcaKeypass = ca.keypass,
                      rootcaKeyStore = ca.keystore,
                      rootcaKeystorePass = ca.storepass,
                      ip = optionIP()
                    ).
                    importServerCert(ca.alias, ca.cert.toString).
                    exportServerCert().
                    generateServerPKCS()

      val serv2 = if (optionNginx()) serv.generateServerKey()
                  else serv

      serv2.generateMarkerFile()

      0
    } catch {
      case x: Exception =>
        log.severe("Error generating selfsigned certificate", x)
        1
    }

  }
}
