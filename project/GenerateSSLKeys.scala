
import sbt.Logger
import java.io.File
import java.io.FileOutputStream

object GenerateSSLKey {

  def deleteKeys(
      logger: Logger,
      prefix: String,
      workingDirectory: Option[File]
  ) = {
    val d = new File(workingDirectory.getOrElse(new File(".")), prefix)
    logger.info( s"Deleting keys directory ${d.toString}")
    MyFileUtils.deleteDirectory( d.toPath(), None )
    d.mkdirs()
  }

  /**
    * Checks if the marker file exists.
    * if the marker file exists, then the keys are assumed to be good.
    * @param logger
    * @param prefix
    * @param workingDirectory
    * @return true if the keys directory was good
    */
  def checkKeys(
      logger: Logger,
      prefix: String,
      workingDirectory: Option[File]
  ) = {
    val d = getMarkerFile( new File(workingDirectory.getOrElse(new File(".")), prefix) )
    logger.info( s"Checking for marker file ${d.toString}")
    if (!d.isFile()) {
      deleteKeys(logger,prefix,workingDirectory)
      false
    } else {
      true
    }
  }

  case class RootCAInfo(
      alias: String,
      dname: String,
      keystore: File,
      cert: File,
      keypass: String,
      storepass: String,
  ) {
    override
    def toString() = {
      s"""RootCA(Alias=$alias, dname="$dname", keystore=$keystore, cert=$cert, keypass=*****, storepass=*****)"""
    }
  }

  /**
    * Generates the a root CA certificate.
    * Two files are generated,
    *   ${rootca}.jks
    *   ${rootca}.crt
    *
    * @param logger
    * @param alias the alias of the certificate in keystore
    * @param rootca the filename prefix for the generated files
    * @param dname
    * @param keypass the password for the newly generated private key
    * @param storepass the password for the newly generated keystore
    * @param workingDirectory working directory to use, rootca prefix is relative to this directory.  None means use current directory.
    * @param good if true, the keys are good and won't be generated
    */
  def generateRootCA(
      logger: Logger,
      alias: String,
      rootca: String,
      dname: String,
      keypass: String,
      storepass: String,
      workingDirectory: Option[File] = None,
      good: Boolean = false
  ): RootCAInfo = {
    val proc = new MyProcess( Option(logger))

    val wd = workingDirectory.getOrElse( new File("."))

    val info = RootCAInfo( alias, dname, getFile(wd,s"${rootca}.jks"), getFile(wd,s"${rootca}.crt"), keypass, storepass )

    if (!good) {
      // Create a self signed key pair root CA certificate
      val gencmd = List(
          "-genkeypair",
          "-v",
          "-alias", alias,
          "-dname", dname,
          "-keystore", info.keystore.toString,
          "-keyalg", "RSA",
          "-keysize", "4096",
          "-ext", """KeyUsage:critical="keyCertSign"""",
          "-ext", """BasicConstraints:critical="ca:true"""",
          "-validity", "9999"
      )
      proc.keytool(
        cmd = gencmd:::List( "-keypass", keypass, "-storepass", storepass ),
        workingDirectory = workingDirectory,
        env = None,
        printcmd = Some(gencmd:::List( "-keypass", "***", "-storepass", "***" )),
      )

      // Export the exampleCA public certificate as exampleca.crt so that it can be used in trust stores.
      val exportcmd = List(
          "-export",
          "-v",
          "-alias", alias,
          "-file", info.cert.toString,
          "-keystore", info.keystore.toString
      )
      proc.keytool(
        cmd = exportcmd:::List( "-keypass", keypass, "-storepass", storepass ),
        workingDirectory = workingDirectory,
        env = None,
        printcmd = Some(exportcmd:::List( "-keypass", "***", "-storepass", "***" )),
      )
    }

    info
  }

  case class ServerInfo(
      alias: String,
      dname: String,
      keystore: File,
      csr: File,
      cert: File,
      keypass: String,
      storepass: String,
      truststore: File,
      pkcs: File,
  ) {
    override
    def toString() = {
      s"""ServerInfo(Alias=$alias, dname="$dname", keystore=$keystore, csr=$csr, cert=$cert, keypass=*****, storepass=*****, truststore=$truststore, pkcs=$pkcs)"""
    }
  }

  /**
    * Generates the a root CA certificate.
    * Two files are generated,
    *   ${server}.jks
    *   ${server}.csr
    *   ${server}.crt
    *
    * @param logger
    * @param alias the alias of the certificate in keystore
    * @param server the filename prefix for the generated files, relative to working directory
    * @param dname
    * @param keypass the password for the newly generated server key
    * @param storepass the password for the newly generated keystore
    * @param rootcaPublicAlias the alias for the CA public key in newly generated keystore
    * @param rootcaPublicCert the public certificate for the CA key
    * @param rootcaKeyStore the path to keystore that contains the CA private key, relative to working directory.
    * @param rootcaKeystorePass the keystore password
    * @param rootcaAlias the alias of the CA private key
    * @param rootcaKeypass the password of the CA private key
    * @param trustStore the filename of the newly created trust store, relative to working directory.
    * @param trustPass the password for the newly created trust store
    * @param workingDirectory working directory to use, server prefix is relative to this directory.  None means use current directory.
    * @param good if true, the keys are good and won't be generated
    */
  def generateServer(
      logger: Logger,
      alias: String,
      server: String,
      dname: String,
      keypass: String,
      storepass: String,
      rootcaPublicAlias: String,
      rootcaPublicCert: String,
      rootcaKeyStore: String,
      rootcaKeystorePass: String,
      rootcaAlias: String,
      rootcaKeypass: String,
      trustStore: String,
      trustPass: String,
      workingDirectory: Option[File] = None,
      good: Boolean = false
  ): ServerInfo = {
    val proc = new MyProcess( Option(logger))

    val wd = workingDirectory.getOrElse( new File("."))

    val info = ServerInfo( alias, dname, getFile(wd,s"${server}.jks"), getFile(wd,s"${server}.csr"), getFile(wd,s"${server}.crt"), keypass, storepass, getFile(wd,trustStore), getFile(wd,s"${server}.p12") )

    if (!good) {
      // Create a self signed key pair root CA certificate
      val gencmd = List(
          "-genkeypair",
          "-v",
          "-alias", alias,
          "-dname", dname,
          "-keystore", info.keystore.toString,
          "-keyalg", "RSA",
          "-keysize", "4096",
          "-validity", "385"
      )
      proc.keytool(
        cmd = gencmd:::List( "-keypass", keypass, "-storepass", storepass ),
        workingDirectory = workingDirectory,
        env = None,
        printcmd = Some(gencmd:::List( "-keypass", "***", "-storepass", "***" )),
      )

      // Create a certificate signing request for example.com
      val certreqcmd = List(
          "-certreq",
          "-v",
          "-alias", alias,
          "-file", info.csr.toString,
          "-keystore", info.keystore.toString
      )
      proc.keytool(
        cmd = certreqcmd:::List( "-keypass", keypass, "-storepass", storepass ),
        workingDirectory = workingDirectory,
        env = None,
        printcmd = Some(certreqcmd:::List( "-keypass", "***", "-storepass", "***" )),
      )

      // Tell exampleCA to sign the example.com certificate. Note the extension is on the request, not the
      // original certificate.
      // Technically, keyUsage should be digitalSignature for DHE or ECDHE, keyEncipherment for RSA.
      val gencertcmd = List(
          "-gencert",
          "-v",
          "-alias", rootcaAlias,
          "-keystore", rootcaKeyStore,
          "-infile", info.csr.toString,
          "-outfile", info.cert.toString,
          "-ext", """KeyUsage:critical="digitalSignature,keyEncipherment"""",
          "-ext", """EKU="serverAuth"""",
          "-ext", """SAN="DNS:localhost,IP:127.0.0.1"""",
          "-rfc",
          "-validity", "385"
      )
      proc.keytool(
        cmd = gencertcmd:::List( "-keypass", rootcaKeypass, "-storepass", rootcaKeystorePass ),
        workingDirectory = workingDirectory,
        env = None,
        printcmd = Some(gencertcmd:::List( "-keypass", "***", "-storepass", "***" )),
      )

      // Tell examplebridgescorekeeper.jks it can trust exampleca as a signer.
      val importcacmd = List(
          "-import",
          "-v",
          "-alias", rootcaPublicAlias,
          "-keystore", info.keystore.toString,
          "-storetype", "JKS",
          "-file", rootcaPublicCert
      )
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
          "-v",
          "-alias", alias,
          "-keystore", info.keystore.toString,
          "-storetype", "JKS",
          "-file", info.cert.toString
      )
      proc.keytool(
        cmd = importsignedcmd:::List( "-storepass", storepass ),
        workingDirectory = workingDirectory,
        env = None,
        printcmd = Some(importsignedcmd:::List( "-storepass", "***" ))
      )

      // Export bridgescorekeeper's public certificate for use with nginx.
      val exportcertcmd = List(
          "-export",
          "-v",
          "-alias", alias,
          "-keystore", info.keystore.toString,
          "-storetype", "JKS",
          "-file", info.cert.toString,
          "-rfc"
      )
      proc.keytool(
        cmd = exportcertcmd:::List( "-keypass", keypass, "-storepass", storepass ),
        workingDirectory = workingDirectory,
        env = None,
        printcmd = Some(exportcertcmd:::List( "-keypass", "***", "-storepass", "***" )),
      )

      // Create a PKCS#12 keystore containing the public and private keys.
      val pkcscmd = List(
          "-importkeystore",
          "-v",
          "-srcalias", alias,
          "-srckeystore", info.keystore.toString,
          "-srcstoretype", "JKS",
          "-destkeystore", info.pkcs.toString,
          "-deststoretype", "PKCS12"
      )
      proc.keytool(
        cmd = pkcscmd:::List( "-srcstorepass", storepass, "-destkeypass", keypass, "-deststorepass", storepass ),
        workingDirectory = workingDirectory,
        env = None,
        printcmd = Some(pkcscmd:::List( "-srcstorepass", "***", "-destkeypass", "***", "-deststorepass", "***" )),
      )


      // Export the bridgescorekeeper private key for use in nginx.  Note this requires the use of OpenSSL.
      // openssl pkcs12 -nocerts -nodes -passout env:PW -passin env:PW -in examplebridgescorekeeper.p12 -out bridgescorekeeper.key

      // Create a JKS keystore that trusts the example CA, with the default password.
      val trustcmd = List(
          "-import",
          "-v",
          "-alias", rootcaAlias,
          "-file", rootcaPublicCert,
          "-storetype", "JKS",
          "-keystore", info.truststore.toString
      )
      proc.keytool(
        cmd = trustcmd:::List( "-storepass", trustPass ),
        workingDirectory = workingDirectory,
        env = None,
        printcmd = Some(trustcmd:::List( "-storepass", "***" )),
        stdin = Some("yes\n")
      )

      val marker = getMarkerFile( info.keystore.getParentFile() )
      val markerf = new FileOutputStream( marker )
      markerf.write(0)
      markerf.flush()
      markerf.close()
    }

    info
  }

  def getMarkerFile( dir: File ) = {
    new File( dir, "GenerateSSLKeys.Marker.txt" )
  }

  def getFile( workingDirectory: File, file: String ) = {
    val f = new File(workingDirectory, file)
    f.getAbsoluteFile.getCanonicalFile
  }

}
