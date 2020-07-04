package com.github.thebridsk.bridge.server.util

import java.io.File
import java.io.FileOutputStream
import com.github.thebridsk.utilities.logging.Logger
import java.io.FileNotFoundException
import java.security.KeyStore
import scala.util.Using
import java.io.FileInputStream
import java.security.cert.X509Certificate
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.Certificate
import java.net.NetworkInterface
import java.net.Inet4Address

trait GenerateSSLKeys

object GenerateSSLKeys {

  val logger = Logger[GenerateSSLKeys]()

  val proc = new MyProcess()

  def getMarkerFile( dir: Option[File] = None ) = {
    getFullFile( dir, "GenerateSSLKeys.Marker.txt" )
  }

  def makerFileExists( dir: Option[File] = None ) = {
    getMarkerFile(dir).isFile()
  }

  def deleteMarkerFile( dir: Option[File] = None ) = {
    getMarkerFile(dir).delete()
  }

  def getFile( workingDirectory: Option[File], file: String ) = {
    new File( new File(file).getName )
  }

  def getFullFile( workingDirectory: Option[File], file: String ): File = {
    val f = new File(workingDirectory.getOrElse(new File(".")), file)
    f.getAbsoluteFile.getCanonicalFile
  }

  def getFullFile( workingDirectory: Option[File], file: File ): File = {
    val f = new File(workingDirectory.getOrElse(new File(".")), file.toString)
    f.getAbsoluteFile.getCanonicalFile
  }

  def deleteKeys(
      workingDirectory: Option[File] = None
  ) = {
    val d = workingDirectory.getOrElse(new File("."))
    logger.info( s"Deleting keys directory ${d.toString}")
    MyFileUtils.deleteDirectory( d.toPath(), None )
    d.mkdirs()
  }

  def showCert(
    certChain: List[Certificate]
  ) = {
    import scala.jdk.CollectionConverters._
    certChain.zipWithIndex.foreach { case (cert,i) =>
      // return true if the certificate is NOT valid
      cert match {
        case c: X509Certificate =>
          val name = c.getSubjectX500Principal().getName()
          logger.info(
            s"""Certificate[$i]
                |  subject DN = ${c.getSubjectDN().getName()}
                |  not after = ${c.getNotAfter()}
                |  not before = ${c.getNotBefore()}
                |  SAN = ${Option(c.getSubjectAlternativeNames()).map( _.asScala.mkString).getOrElse("")}
                |  issuer DN = ${c.getIssuerDN()}
              """.stripMargin
          )
        case c =>
          logger.info(
            s"""Certificate[$i]
                |  Unknown certificate class ${c.getClass().getName()}
              """.stripMargin
          )
      }
    }
  }

  /**
    * Validate the server private certificate.
    * This checks the valid dates, and the IP address of the server.
    *
    * @param alias
    * @param keystore
    * @param storepass
    * @param workingDirectory
    * @param showCerts
    * @return true - the certificate is valid
    */
  def validateCert(
    alias: String,
    keystore: String,
    storepass: String,
    workingDirectory: Option[File],
    showCerts: Boolean
  ): Boolean = {
    import scala.jdk.CollectionConverters._
    getCertificatesFromKeystore(alias,keystore,storepass,workingDirectory) match {
      case Some( certs: List[_]) =>
        if (showCerts) showCert(certs)
        certs.zipWithIndex.find { case (cert,i) =>
          // return true if the certificate is NOT valid
          cert match {
            case c: X509Certificate =>
              val name = c.getSubjectX500Principal().getName()
              try {
                c.checkValidity()
                if (i == 0) {
                  val machineip = NetworkInterface.getNetworkInterfaces().asScala.filter { ni =>
                    !ni.isLoopback() && ni.getInetAddresses().hasMoreElements()
                  }.flatMap { ni =>
                    ni.getInetAddresses().asScala.flatMap { ia =>
                      if (ia.isInstanceOf[Inet4Address]) ia.getHostAddress()::Nil
                      else Nil
                    }
                  }.toList.distinct
                  logger.fine(s"Found machine IPs: ${machineip.mkString(" ")}")

                  val certIPs = getSAN(c)
                  val rc = certIPs.find { ip =>
                    machineip.contains(ip)
                  }.isEmpty
                  if (rc) logger.warning(s"IPs ${machineip.mkString("[", " ", "]")} not found in certificate: ${certIPs.mkString("[", " ", "]")}")
                  rc
                } else {
                  false
                }
              } catch {
                case x @ (_: CertificateExpiredException | _: CertificateNotYetValidException) =>
                  logger.warning( s"Certificate not valid today: $name" )
                  true
              }
            case _ =>
              logger.warning( s"Unknown certification class ${cert.getClass.getName}:\n${cert}")
              true
          }
        }.isEmpty
      case cert =>
        logger.warning( s"Certificate not found: $keystore $alias")
        false
    }
  }

  /**
    * Get the certificate chain for an alias
    *
    * @param alias
    * @param keystore
    * @param storepass
    * @param workingDirectory
    * @return None if not found or keystore does not exist.
    */
  def getCertificatesFromKeystore(
    alias: String,
    keystore: String,
    storepass: String,
    workingDirectory: Option[File],
  ): Option[List[Certificate]] = {
    val keyStore = KeyStore.getInstance("JKS");
    val file = GenerateSSLKeys.getFullFile( workingDirectory, keystore)
    if (file.isFile()) {
      Using.resource(new FileInputStream( file )) { f =>
        keyStore.load( f, storepass.toCharArray());
      }
      Option(keyStore.getCertificateChain( alias ).toList)
    } else {
      logger.warning( s"File $file does not exist or is not a file")
      None
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
      logger.fine( s"  SAN ${l.map( o => s"${o.getClass.getSimpleName}(${o})").mkString(" ")}")
      l match {
        case (SAN_IP)::(s: String)::Nil if s != "127.0.0.1" =>
          s::Nil
        case _ =>
          logger.fine( s"Ignoring SAN entry: ${l.mkString(" ")}")
          Nil
      }
    }.toList
  }

}

import GenerateSSLKeys._

case class RootCAInfo(
    alias: String,
    dname: String,
    keystore: File,
    cert: File,
    keypass: String,
    storepass: String,
    truststore: Option[File],
    trustpass: Option[String],
    workingDirectory: Option[File],
    good: Boolean,
    verbose: List[String],
    validityCA: String
) {
  override
  def toString() = {
    s"""RootCA(Alias=$alias, dname="$dname", keystore=$keystore, cert=$cert,"""+
      s""" truststore=$truststore, keypass=*****, storepass=*****, trustpass=${trustpass.map( p => "***").getOrElse("<None>")}),"""+
      s""" workingDirectory=${workingDirectory}, good=${good}, verbose=${verbose}, validityCA=$validityCA"""
  }

  def checkMarkerFile() = {
    if (makerFileExists( workingDirectory )) copy( good = true )
    else this
  }

  def deleteOldServerCerts() = {
    getFullFile(workingDirectory, keystore).delete()
    getFullFile(workingDirectory, cert).delete()
    truststore.foreach( f => getFullFile(workingDirectory,f).delete())
    getMarkerFile(workingDirectory).delete()
    this
  }

  /**
    * Generates a root CA certificate.
    * Three files are generated,
    *   ${rootca}.jks
    *   ${rootca}.crt
    *   ${truststore}.jks
    *
    * workingDirectory must be the directory that will get the newly generated keys.
    * rootca must not have any path elements.
    *
    * @param validityCA the number of days the server certificate is valid for
    *
    */
  def generateRootCA(): RootCAInfo = {

    if (!good) {

      // Create a self signed key pair root CA certificate
      val gencmd = List(
          "-genkeypair",
          "-alias", alias,
          "-dname", dname,
          "-keystore", keystore.toString,
          "-keyalg", "RSA",
          "-keysize", "4096",
          "-ext", "KeyUsage:critical=keyCertSign",
          "-ext", "BasicConstraints:critical=ca:true",
          "-validity", s"$validityCA"
      ):::verbose
      proc.keytool(
        cmd = gencmd:::List( "-keypass", keypass, "-storepass", storepass ),
        workingDirectory = workingDirectory,
        env = None,
        printcmd = Some(gencmd:::List( "-keypass", "***", "-storepass", "***" )),
      )

      // Export the exampleCA public certificate as exampleca.crt so that it can be used in trust stores.
      val exportcmd = List(
          "-export",
          "-alias", alias,
          "-file", cert.toString,
          "-keystore", keystore.toString
      ):::verbose
      proc.keytool(
        cmd = exportcmd:::List( "-keypass", keypass, "-storepass", storepass ),
        workingDirectory = workingDirectory,
        env = None,
        printcmd = Some(exportcmd:::List( "-keypass", "***", "-storepass", "***" )),
      )
    }

    this
  }

  /**
    * @param info root CA info from generateRootCA call
    * @param truststore the filename prefix for the trust store, relative to workingDirectory
    * @param trustpass the password for the newly created trust store
    * @param workingDirectory working directory to use, rootca and truststore prefix is
    *                         relative to this directory.  None means use current directory.
    * @param good if true, the keys are good and won't be generated
    * @param verbose if true add the -v option to keytool commands
    * @return updated root CA info object
    */
  def trustRootCA(
    truststore: String,
    trustpass: String,
  ) = {

    val info = copy( truststore = Some(getFile(workingDirectory,s"${truststore}.jks")), trustpass = Some(trustpass))

    if (!good) {

      // Create a JKS keystore that trusts the example CA, with the default password.
      val trustcmd = List(
          "-import",
          "-alias", info.alias,
          "-file", info.cert.toString,
          "-storetype", "JKS",
          "-keystore", info.truststore.get.toString
      ):::info.verbose
      proc.keytool(
        cmd = trustcmd:::List( "-storepass", info.trustpass.get ),
        workingDirectory = workingDirectory,
        env = None,
        printcmd = Some(trustcmd:::List( "-storepass", "***" )),
        stdin = Some("yes\n")
      )
    }

    info
  }

}

object RootCAInfo {

  /**
    *
    * workingDirectory must be the directory that will get the newly generated keys.
    * rootca must not have any path elements.
    *
    * @param alias the alias for the CA private key to use
    * @param rootca the filename prefix for all the server files, must not have path elements
    * @param dname
    * @param keypass the password for the private key
    * @param storepass the password for the keystore
    * @param workingDirectory the working directory, None means current working directory, the default
    * @param good the keys are good, default true
    * @param verbose verbose when executing commands, default false
    * @param validityCA the validity for the CA certificate in days, default 367
    */
  def apply(
      alias: String,
      rootca: String,
      dname: String,
      keypass: String,
      storepass: String,
      workingDirectory: Option[File] = None,
      good: Boolean = false,
      verbose: Boolean = false,
      validityCA: String = "367",
      truststoreprefix: Option[String] = None,
      trustpass: Option[String] = None
  ) = {

    val v = if (verbose) List("-v") else List()

    new RootCAInfo(
        alias = alias,
        dname = dname,
        keystore = getFile(workingDirectory,s"${rootca}.jks"),
        cert = getFile(workingDirectory,s"${rootca}.crt"),
        keypass = keypass,
        storepass = storepass,
        truststore = truststoreprefix.map( p => getFile(workingDirectory, s"${p}.jks")),
        trustpass = trustpass,
        workingDirectory = workingDirectory,
        good = good,
        verbose = v,
        validityCA = validityCA
    )

  }

}

/**
  *
  * @constructor
  * @param alias the alias of the certificate in keystore
  * @param keystore the filename for the keystore for server private certificate
  * @param csr the filename for the CSR file for server certificate
  * @param cert the filename for the server certificate
  * @param dname
  * @param keypass the password for the newly generated server key
  * @param storepass the password for the newly generated keystore
  * @param pkcs the filename for the server private certificate in PKCS#12
  * @param keyfile the filename for the server private certificate, used by nginx
  * @param workingDirectory the working directory to use, all files will be created in this directory
  * @param good the keys are good, don't run any commands
  * @param verbose the verbose flag to add
  * @param validityServer the number of days the server certificate is valid for
  */
case class ServerInfo(
    alias: String,
    dname: String,
    keystore: File,
    csr: File,
    cert: File,
    keypass: String,
    storepass: String,
    pkcs: File,
    keyfile: File,
    workingDirectory: Option[File],
    good: Boolean,
    verbose: List[String],
    validityServer: String
) {

  override
  def toString() = {
    s"""ServerInfo(Alias=$alias, dname="$dname", keystore=$keystore, csr=$csr, cert=$cert,"""+
    s""" keypass=*****, storepass=*****, pkcs=$pkcs), keyfile=$keyfile, workingDirectory=${workingDirectory}"""+
    s""" good=$good, verbose=$verbose, validityServer=$validityServer"""
  }

  def checkMarkerFile() = {
    if (makerFileExists( workingDirectory )) copy( good = true )
    else this
  }

  def deleteOldServerCerts() = {
    getFullFile(workingDirectory, keystore).delete()
    getFullFile(workingDirectory, csr).delete()
    getFullFile(workingDirectory, cert).delete()
    getFullFile(workingDirectory, pkcs).delete()
    getFullFile(workingDirectory, keyfile).delete()
    getMarkerFile(workingDirectory).delete()
    this
  }

  /**
    * Generate the server.jks file with private key, create a CSR to be signed by CA.
    *
    * @return the server info object
    */
  def generateServerCSR(  ) = {

    if (!good) {

      // Create a self signed key pair root CA certificate
      val gencmd = List(
          "-genkeypair",
          "-alias", alias,
          "-dname", dname,
          "-keystore", keystore.toString,
          "-keyalg", "RSA",
          "-keysize", "4096",
          "-validity", s"$validityServer",
      ):::verbose
      proc.keytool(
        cmd = gencmd:::List( "-keypass", keypass, "-storepass", storepass ),
        workingDirectory = workingDirectory,
        env = None,
        printcmd = Some(gencmd:::List( "-keypass", "***", "-storepass", "***" )),
      )

      // Create a certificate signing request for example.com
      val certreqcmd = List(
          "-certreq",
          "-alias", alias,
          "-file", csr.toString,
          "-keystore", keystore.toString,
      ):::verbose
      proc.keytool(
        cmd = certreqcmd:::List( "-keypass", keypass, "-storepass", storepass ),
        workingDirectory = workingDirectory,
        env = None,
        printcmd = Some(certreqcmd:::List( "-keypass", "***", "-storepass", "***" )),
      )
    }
    this
  }

  /**
    * Generate the server.crt signed by CA
    *
    * @param rootcaKeyStore the path to keystore that contains the CA private key, relative to working directory.
    * @param rootcaKeystorePass the keystore password
    * @param rootcaAlias the alias of the CA private key
    * @param rootcaKeypass the password of the CA private key
    * @return the server info object
    */
  def generateServerCert(
      rootcaKeyStore: File,
      rootcaKeystorePass: String,
      rootcaAlias: String,
      rootcaKeypass: String,
      ip: List[String] = Nil,
  ) = {

    if (!good) {

      // Tell exampleCA to sign the example.com certificate. Note the extension is on the request, not the
      // original certificate.
      // Technically, keyUsage should be digitalSignature for DHE or ECDHE, keyEncipherment for RSA.
      val gencertcmd = List(
          "-gencert",
          "-alias", rootcaAlias,
          "-keystore", rootcaKeyStore.toString,
          "-infile", csr.toString,
          "-outfile", cert.toString,
          "-ext", "KeyUsage:critical=digitalSignature,keyEncipherment",
          "-ext", "EKU=serverAuth",
          // "-ext", "SAN=DNS:localhost,IP:127.0.0.1",
          "-ext", s"SAN=DNS:localhost,IP:127.0.0.1${ip.map( i => s"IP:$i" ).mkString(",",",","")}",
          "-rfc",
          "-validity", "385"
      ):::verbose
      proc.keytool(
        cmd = gencertcmd:::List( "-keypass", rootcaKeypass, "-storepass", rootcaKeystorePass ),
        workingDirectory = workingDirectory,
        env = None,
        printcmd = Some(gencertcmd:::List( "-keypass", "***", "-storepass", "***" )),
      )

    }
    this
  }

  /**
    * Import the CA public key and server signed cert into server keystore.
    *
    * @param rootcaPublicAlias the alias for the CA public key in newly generated keystore
    * @param rootcaPublicCert the public certificate for the CA key
    * @return the server info object
    */
  def importServerCert(
      rootcaPublicAlias: String,
      rootcaPublicCert: String,
  ) = {

    if (!good) {

      // Tell examplebridgescorekeeper.jks it can trust exampleca as a signer.
      val importcacmd = List(
          "-import",
          "-alias", rootcaPublicAlias,
          "-keystore", keystore.toString,
          "-storetype", "JKS",
          "-file", rootcaPublicCert
      ):::verbose
      proc.keytool(
        cmd = importcacmd:::List( "-storepass", storepass ),
        workingDirectory = workingDirectory,
        env = None,
        printcmd = Some(importcacmd:::List( "-storepass", "***" )),
        stdin = Some("yes\n")
      )

      // Import the signed certificate back into examplebridgescorekeeper.jks
      val importsignedcmd = List(
          "-import",
          "-alias", alias,
          "-keystore", keystore.toString,
          "-storetype", "JKS",
          "-file", cert.toString
      ):::verbose
      proc.keytool(
        cmd = importsignedcmd:::List( "-storepass", storepass ),
        workingDirectory = workingDirectory,
        env = None,
        printcmd = Some(importsignedcmd:::List( "-storepass", "***" ))
      )

    }
    this
  }

  /**
    * Export the server cert
    *
    * @return the server info object
    */
  def exportServerCert() = {

    if (!good) {

      val exportcertcmd = List(
          "-export",
          "-alias", alias,
          "-keystore", keystore.toString,
          "-storetype", "JKS",
          "-file", cert.toString,
          "-rfc"
      ):::verbose
      proc.keytool(
        cmd = exportcertcmd:::List( "-keypass", keypass, "-storepass", storepass ),
        workingDirectory = workingDirectory,
        env = None,
        printcmd = Some(exportcertcmd:::List( "-keypass", "***", "-storepass", "***" )),
      )
    }
    this
  }

  /**
    * Generate the server PKCS#12
    *
    * @return the server info object
    */
  def generateServerPKCS() = {

    if (!good) {

      // Create a PKCS#12 keystore containing the public and private keys.
      val pkcscmd = List(
          "-importkeystore",
          "-srcalias", alias,
          "-srckeystore", keystore.toString,
          "-srcstoretype", "JKS",
          "-destkeystore", pkcs.toString,
          "-deststoretype", "PKCS12"
      ):::verbose
      proc.keytool(
        cmd = pkcscmd:::List( "-srcstorepass", storepass, "-destkeypass", keypass, "-deststorepass", storepass ),
        workingDirectory = workingDirectory,
        env = None,
        printcmd = Some(pkcscmd:::List( "-srcstorepass", "***", "-destkeypass", "***", "-deststorepass", "***" )),
      )

    }
    this
  }

  /**
    * Generate the private server key for use with nginx.
    * Requires openssl to be installed.  On windows, requires WSL with openssl installed.
    *
    * @return the server info object
    */
  def generateServerKey() = {

    if (!good) {

      // Export the bridgescorekeeper private key for use in nginx.  Note this requires the use of OpenSSL.
      val opensslpckscmd = List(
          "openssl",
          "pkcs12",
          "-nocerts",
          "-nodes",
          "-in", pkcs.getName.toString,
          "-out", keyfile.getName.toString
      )
      val printcmd = opensslpckscmd:::List( "-passout", "pass:***", "-passin", "pass:***" )
      val process = proc.bash(
        cmd = opensslpckscmd:::List( "-passout", s"pass:${keypass}", "-passin", s"pass:${keypass}" ),
        workingDirectory = workingDirectory,
        addEnvp = Map(),
        printcmd = Some(printcmd),
      )
      val rc = process.waitFor()
      if (rc != 0) throw new Error(s"Failed, with rc=${rc} running bash ${printcmd.mkString(" ")}")

    }
    this
  }

  /**
    * Generate the private server key for use with nginx.
    * Requires openssl to be installed.  On windows, requires WSL with openssl installed.
    *
    * @param info
    * @param good if true, the keys are good and won't be generated, default is false
    * @param verbose if true add the -v option to keytool commands, default is false
    * @return the server info object
    */
  def generateMarkerFile() = {

    if (!good) {
      val marker = getMarkerFile( workingDirectory )
      val markerf = new FileOutputStream( marker )
      try {
        markerf.write(0)
        markerf.flush()
      } finally {
        markerf.close()
      }
    }
    this
  }


}

object ServerInfo {

  /**
    * Generates the a server info object.
    * Filenames created are in workingDirectory:
    *   ${server}.jks
    *   ${server}.csr
    *   ${server}.crt
    *   ${server}.p12
    *   ${server}.key
    *
    * workingDirectory must be the directory that will get the newly generated keys.
    * server must not have any path elements.
    *
    * @param alias the alias of the certificate in keystore
    * @param server the filename prefix for the generated files, relative to working directory, must not have path elements.
    * @param dname
    * @param keypass the password for the newly generated server key
    * @param storepass the password for the newly generated keystore
    * @param workingDirectory working directory to use, server prefix is relative to this directory.  None means use current directory.
    *                         default: current working directory
    * @param good if true, the keys are good and won't be generated, default false
    * @param verbose if true add the -v option to keytool commands, default false
    * @param validityServer the number of days the server certificate is valid for, default 366
    */
  def apply(
      alias: String,
      server: String,
      dname: String,
      keypass: String,
      storepass: String,
      workingDirectory: Option[File] = None,
      good: Boolean = false,
      verbose: Boolean = false,
      validityServer: String = "366"
  ): ServerInfo = {
    val v = if (verbose) List("-v") else List()

    new ServerInfo(
      alias = alias,
      dname = dname,
      keystore = getFile(workingDirectory,s"${server}.jks"),
      csr = getFile(workingDirectory,s"${server}.csr"),
      cert = getFile(workingDirectory,s"${server}.crt"),
      keypass = keypass,
      storepass = storepass,
      pkcs = getFile(workingDirectory,s"${server}.p12"),
      keyfile = getFile(workingDirectory,s"${server}.key"),
      workingDirectory = workingDirectory,
      good = good,
      verbose = v,
      validityServer = validityServer
    )
  }

}
