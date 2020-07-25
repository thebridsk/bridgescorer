package com.github.thebridsk.bridge.sslkey

import com.github.thebridsk.utilities.main.Subcommand
import com.github.thebridsk.utilities.logging.Logger
import org.rogach.scallop._
import scala.concurrent.duration.Duration
import com.github.thebridsk.bridge.server.util.GenerateSSLKeys
import com.github.thebridsk.bridge.server.util.RootCAInfo
import com.github.thebridsk.bridge.server.util.ServerInfo
import java.security.cert.X509Certificate
import java.security.KeyStore
import java.io.FileInputStream
import scala.util.Using
import java.net.NetworkInterface
import java.net.Inet4Address

trait GenerateServerCert

object GenerateServerCert extends Subcommand("generateservercert") {
  import SSLKeyCommands.optionKeyDir

  val log = Logger[GenerateServerCert]()

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))


  descr("Generate a Server certificate")

  banner(s"""
Generate a Server certificate

Syntax:
  ${SSLKeyCommands.cmdName} ${name} [options]
Options:""")

  val optionForce = toggle(
    name = "force",
    noshort = true,
    descrNo = "fail if keys already exist",
    descrYes = "overwrite existing keys",
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
    descr = "server dname, must be specified if generating a new certificate",
    required = false,
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
    descr = "list of IP addresses to add to server certificate, may be specified multiple times.  127.0.0.1 and localhost is always added",
    default = Some(List()),
    validate = { l =>
//      log.warning(s"Validating ip option: ${l.mkString(" ")}")
      l.find { s => s match {
        case patternIP(ip) => false    // this is good, but we are looking for a bad entry
        case _ => true
      } }.isEmpty
    }        // empty means no bad entries were found
  )( listArgConverter( s => s ))

  val optionAddIP = opt[List[String]](
    name = "addip",
    noshort = true,
    descr = "list of IP addresses to add to existing server certificate, may be specified multiple times.",
    default = None,
    validate = { l =>
//      log.warning(s"Validating ip option: ${l.mkString(" ")}")
      l.find { s => s match {
        case patternIP(ip) => false    // this is good, but we are looking for a bad entry
        case _ => true
      } }.isEmpty
    }        // empty means no bad entries were found
  )( listArgConverter( s => s ))

  val optionAddMachineIP = toggle(
    name = "addmachineip",
    noshort = true,
    descrNo = "Don't add the machine IP address, default",
    descrYes = "add the machine IP address",
    default = Some(false)
  )

  mutuallyExclusive(optionAddIP, optionIP, optionAddMachineIP)
  mutuallyExclusive(optionAddIP, optionAddMachineIP, optionDname)

  def executeSubcommand(): Int = {

    try {
      val workingDirectory = optionKeyDir()
      if (!workingDirectory.isDirectory) workingDirectory.createDirectory()

      val rootcadname = {
        GenerateSSLKeys.getCertificatesFromKeystore(
          optionRootCAAlias(),
          s"${optionCA()}.jks",
          optionRootCAStorePW(),
          workingDirectory = Some(workingDirectory.jfile)
        ).flatMap { list =>
          list.head match {
            case c: X509Certificate =>
              Option(c.getSubjectDN().getName())
            case _ =>
              None
          }
        }
      }

      if (rootcadname.isEmpty) {
        log.severe("Unable to find CA certificate")
        return 1
      }

      val rootca = RootCAInfo(
        alias = optionRootCAAlias(),
        rootca = optionCA(),
        dname = rootcadname.get,
        keypass = optionRootCAKeyPW(),
        storepass = optionRootCAStorePW(),
        workingDirectory = Some(workingDirectory.jfile),
        good = false,
        verbose = optionVerbose(),
        validityCA = optionValidityCA().toString,
      )

      val dname = {
        optionDname.toOption.orElse {
          GenerateSSLKeys.getCertificatesFromKeystore(
            optionAlias(),
            s"${optionServer()}.jks",
            optionStorepass(),
            workingDirectory = Some(workingDirectory.jfile)
          ).flatMap { list =>
            list.head match {
              case c: X509Certificate =>
                Option(c.getSubjectDN().getName())
              case _ =>
                None
            }
          }
        }
      }

      if (dname.isEmpty) {
        log.severe("Unable to find Server certificate, --dname must be specified if server certificate does not exist")
        return 1
      }

      val server = ServerInfo(
        workingDirectory = rootca.workingDirectory,
        alias = optionAlias(),
        server = optionServer(),
        dname = dname.get,
        keypass = optionKeypass(),
        storepass = optionStorepass(),
        good = false,
        verbose = optionVerbose(),
        validityServer = optionValidityServer().toString,
      )

      val (generateCert,ips) = {
        if (optionAddMachineIP()) {
          import scala.jdk.CollectionConverters._
          val machineip = NetworkInterface.getNetworkInterfaces().asScala.filter { ni =>
            !ni.isLoopback() && ni.getInetAddresses().hasMoreElements()
          }.flatMap { ni =>
            ni.getInetAddresses().asScala.flatMap { ia =>
              if (ia.isInstanceOf[Inet4Address]) ia.getHostAddress()::Nil
              else Nil
            }
          }.toList.distinct
          log.info(s"Found machine IPs: ${machineip.mkString(" ")}")
          val original = getSANFromCertificate(server)
          val newlist = (original:::machineip).filter( _ != "127.0.0.1").distinct
          ( original.length != newlist.length, newlist )
        } else {
          optionAddIP.toOption.map { list =>
            val original = getSANFromCertificate(server)
            val newlist = (original:::list).filter( _ != "127.0.0.1").distinct
            ( original.length != newlist.length, newlist )
          }.getOrElse( (true,optionIP().filter( _ != "127.0.0.1").distinct))
        }
      }

      if (generateCert) {
        val serv = server.
                      deleteOldServerCerts().
                      generateServerCSR().
                      generateServerCert(
                        rootcaAlias = rootca.alias,
                        rootcaKeypass = rootca.keypass,
                        rootcaKeyStore = rootca.keystore,
                        rootcaKeystorePass = rootca.storepass,
                        ip = ips
                      ).
                      importServerCert(rootca.alias, rootca.cert.toString).
                      exportServerCert().
                      generateServerPKCS()

        val serv2 = if (optionNginx()) serv.generateServerKey()
                    else serv

        serv2.generateMarkerFile()
      } else {
        log.info( s"Certificate already has IP addresses: ${ips.mkString(" ")}")
      }

      0
    } catch {
      case x: Exception =>
        log.severe("Error generating server certificate", x)
        1
    }

  }

  def getSANFromCertificate( server: ServerInfo ) = {
    val keyStore = KeyStore.getInstance("JKS");
    val file = GenerateSSLKeys.getFullFile( server.workingDirectory, server.keystore)
    if (file.isFile()) {
      Using.resource(new FileInputStream( file )) { f =>
        keyStore.load( f, server.storepass.toCharArray());
      }

      keyStore.getCertificate( server.alias ) match {
        case cert: X509Certificate => getSAN(cert)
        case cert =>
          log.warning( s"Unknown certification class ${cert.getClass.getName}:\n${cert}")
          List()
      }
    } else {
      log.warning( s"File $file does not exist or is not a file")
      List()
    }
  }

  val SAN_IP = 7
  val SAN_DNS = 2

  /**
    *
    *
    * @param cert
    * @return a list of all IP address in the SAN, except for 127.0.0.1
    */
  def getSAN( cert: X509Certificate ): List[String] = {
    val collection = cert.getSubjectAlternativeNames()
    import scala.jdk.CollectionConverters._
    collection.asScala.flatMap { list =>
      val l = list.asScala.toList
      log.warning( s"  SAN ${l.map( o => s"${o.getClass.getSimpleName}(${o})").mkString(" ")}")
      l match {
        case (SAN_IP)::(s: String)::Nil if s != "127.0.0.1" =>
          s::Nil
        case _ =>
          log.info( s"Ignoring SAN entry: ${l.mkString(" ")}")
          Nil
      }
    }.toList
  }

}
