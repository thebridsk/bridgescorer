

object GenerateSSLKey {

  case class RootCAInfo(
      alias: String,
      dname: String,
      keystore: File,
      cert: File,
      keypass: String,
      storepass: String,
  ) {
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
    * @param keypass
    * @param storepass
    * @param workingDirectory working directory to use, rootca prefix is relative to this directory.  None means use current directory.
    */
  def generateRootCA(
      logger: Logger,
      alias: String,
      rootca: String,
      dname: String,
      keypass: String,
      storepass: String,
      workingDirectory: Option[File] = None
  ): RootCAInfo = {
    val proc = new MyProcess( Option(logger))

    val info = RootCAInfo( alias, dname, s"${rootca}.jks", s"${rootca}.crt", keypass, storepass )

    // Create a self signed key pair root CA certificate
    val gencmd = List( "-genkeypair", "-v", "-alias", alias, "-dname", dname, "-keystore", info.keystore, "-keyalg", "RSA", "-keysize", "4096", "-ext", """KeyUsage:critical="keyCertSign"""", "-ext", """BasicConstraints:critical="ca:true"""", "-validity", "9999" )
    proc.keytool(
      cmd = gencmd:::List( "-keypass", keypass, "-storepass", storepass ),
      workingDirectory = workingDirectory,
      env = None,
      printcmd = gencmd:::List( "-keypass", "***", "-storepass", "***" ),
    )

    // Export the exampleCA public certificate as exampleca.crt so that it can be used in trust stores.
    val exportcmd = List( "-export", "-v", "-alias", alias, "-file", info.cert, "-keystore", info.keystore )
    proc.keytool(
      cmd = exportcmd:::List( "-keypass", keypass, "-storepass", storepass ),
      workingDirectory = workingDirectory,
      env = None,
      printcmd = exportcmd:::List( "-keypass", "***", "-storepass", "***" ),
    )

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
  ) {
    def toString() = {
      s"""ServerInfo(Alias=$alias, dname="$dname", keystore=$keystore, csr=$crt, cert=$cert, keypass=*****, storepass=*****)"""
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
    * @param server the filename prefix for the generated files
    * @param dname
    * @param keypass
    * @param storepass
    * @param workingDirectory working directory to use, server prefix is relative to this directory.  None means use current directory.
    */
  def generateServer(
      logger: Logger,
      alias: String,
      server: String,
      dname: String,
      keypass: String,
      storepass: String,
      rootcaKeyStore: String,
      rootcaKeystorePass: String,
      workingDirectory: Option[File] = None
  ): RootCAInfo = {
    val proc = new MyProcess( Option(logger))

    val info = ServerInfo( alias, dname, s"${server}.jks", s"${server}.csr", s"${server}.crt", keypass, storepass )

    // Create a self signed key pair root CA certificate
    val gencmd = List( "-genkeypair", "-v", "-alias", alias, "-dname", dname, "-keystore", info.keystore, "-keyalg", "RSA", "-keysize", "4096", "-validity", "385" )
    proc.keytool(
      cmd = gencmd:::List( "-keypass", keypass, "-storepass", storepass ),
      workingDirectory = workingDirectory,
      env = None,
      printcmd = gencmd:::List( "-keypass", "***", "-storepass", "***" ),
    )

    // Create a certificate signing request for example.com
    val certreqcmd = List( "-certreq", "-v", "-alias", alias, "-file", info.csr, "-keystore", info.keystore )
    proc.keytool(
      cmd = certreqcmd:::List( "-keypass", keypass, "-storepass", storepass ),
      workingDirectory = workingDirectory,
      env = None,
      printcmd = certreqcmd:::List( "-keypass", "***", "-storepass", "***" ),
    )

    // Tell exampleCA to sign the example.com certificate. Note the extension is on the request, not the
    // original certificate.
    // Technically, keyUsage should be digitalSignature for DHE or ECDHE, keyEncipherment for RSA.
    val gencertcmd = List( "-gencert", "-v", "-alias", alias, "-keystore", rootcaKeyStore, "-infile example.com.csr -outfile example.com.crt -ext KeyUsage:critical="digitalSignature,keyEncipherment" -ext EKU="serverAuth" -ext SAN="DNS:example.com" -rfc" )
    proc.keytool(
      cmd = gencertcmd:::List( "-keypass", keypass, "-storepass", storepass ),
      workingDirectory = workingDirectory,
      env = None,
      printcmd = gencertcmd:::List( "-keypass", "***", "-storepass", "***" ),
    )

    info
  }
}
