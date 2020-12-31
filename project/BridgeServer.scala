import sbt.Fork
import java.io.File
import sbt.ForkOptions
import java.net.URL
import java.net.HttpURLConnection
import java.io.InputStream
import sbt.Logger
import java.nio.file.Files
import java.io.IOException
import java.net.SocketTimeoutException
import scala.io.Source
import java.net.InetAddress

/**
  * Manages the starting of the BridgeScoreKeeper server for testing.
  *
  * System properties and environment variables:
  *
  * - **TestServerListen** - the protocol, host and port to listen on.
  *                          the port is ignored.  Syntax is a URL.
  * - **TestServerURL**    - the base URL for the server.  Must end in "/"
  *                          The default value is the **TestServerListen** value.
  *
  * @param assemblyJar the location and name of the bridgescorekeeper jar file
  * @param port port to use when starting the server, overrides the value given in **TestServerListen** value.
  */
class BridgeServer( val assemblyJar: String, val port: Int = 8082) {
  import BridgeServer._

  var thread: Option[Thread] = None
  var workingDirectory: Option[File] = None

  val lock = new Object
  @volatile var done = false

  def startServer( log: Logger, serverLogfile: String = "serverInTests.%u.log" ) = {
    def createTempDirectory() = {
      val dir = _root_.java.nio.file.Files.createTempDirectory("moretests").toFile
      log.info( "Temporary directory is "+dir )
      Some(dir)
    }

    workingDirectory = createTempDirectory() // Some(baseDirectory.value)

    thread = Some(new Thread("MoreTests Server") {
      override
      def run(): Unit = {
        val hostname = testServerListen.getHost()
        val ipAddr = InetAddress.getByName(hostname)
        val interface = ipAddr.getHostAddress()
        runjava(
          log,
          List(
            "-jar", assemblyJar,
            "--logfile", serverLogfile,
            "start",
            "--port", port.toString,
            "--interface", interface
          ),
          workingDirectory
        )
        lock.synchronized {
          done = true
          lock.notifyAll()
        }
      }
    })
    thread.foreach { t => {
      t.start
      lock.synchronized {
        if (!done) {
          lock.wait(10000L)
        }
        if (done) {
          stopServer(log)
          throw new Exception("Server terminated")
        }
      }
    }}
  }

  def startedServer = thread.isDefined

  private def flushAndCloseInputStream( is: InputStream ) = {
    var len = 0
    val buf = new Array[Byte](1024)
    while ( { len=is.read(buf) ; len > 0 } ) len = 0
    is.close()
  }

  def stopServer( log: Logger ) = {
    thread.foreach { t => {
      if (t.isAlive()) {
        val url = new URL(s"${getBaseUrl}v1/shutdown?doit=yes")
        val conn = url.openConnection().asInstanceOf[HttpURLConnection]
        conn.setRequestMethod("POST")
        conn.connect()
        val status = conn.getResponseCode
        if (status >=200 && status < 300) {
          flushAndCloseInputStream(conn.getInputStream)
        } else {
          log.warn("Error shutting down server, status code is "+status)
          flushAndCloseInputStream(conn.getInputStream)
        }
        conn.disconnect()
//        runjava( "-jar"::assemblyJar::"shutdown"::"--port"::port::Nil, workingDirectory )
        t.join( 30000 )
      }
      thread = None
    } }
  }

  /**
    * Normalize the URL.
    *
    * Add protocol http if none is specified,
    * use port 8081 if none specified
    *
    * This must match same function in TestServer.scala
    */
  private def normalizeURL(url: String) = {
    val u = new URL(url)
    val port = {
      val p = u.getPort()
      if (p < 0) 8081
      else p
    }
    new URL(
      Option(u.getProtocol()).getOrElse("http"),
      u.getHost(),
      port,
      u.getFile()
    )
  }

  def getProp(name: String): Option[String] = {
    sys.props.get(name) match {
      case Some(s) =>
        Some(s)
      case None    =>
        sys.env.get(name)
          .map { s =>
            s
          }.orElse {
            None
          }
    }
  }

  /**
    * Get the value of a URL system property or environment variable.
    *
    * If both system property and environment variable is set, the
    * system property value is used.
    *
    * Normalized the URL, this adds protocol http if none is specified,
    * use port 8081 if none specified.
    *
    * Checks for protocol being http or https
    *
    * optionally checks the file part of the URL to make sure it ends in a "/"
    *
    * @param name the name of the environment variable or system property
    * @param defaultValue the value to use if none is set
    * @param ignoreFile if true, then don't check if file part ends in "/"
    *
    * @throws Exception if the URL is not valid.
    *
    * This must match same function in TestServer.scala
    */
  private def getPropURL(
      name: String,
      defaultValue: String,
      ignoreFile: Boolean = false
  ): URL = {
    val v = getProp(name).getOrElse(defaultValue)
    try {
      val url = normalizeURL(v)
      val protocol = url.getProtocol()
      if (protocol != "http" && protocol != "https") {
        throw new Exception(s"${name} must be http or https, found ${protocol}: ${testServerURL}")
      }
      if (!ignoreFile && url.getFile() != "/") {
        throw new Exception(s"${name} must end in '/'")
      }
      url
    } catch {
      case x: Exception =>
        throw new Exception(s"Value ${name} is not a valid URL: ${v}\n${x}",x)
    }
  }

  private def endsInSlash( v: String ) = {
    if (v.endsWith("/")) v
    else s"$v/"
  }

  val testServerListen: URL = getPropURL("TestServerListen", "localhost", true)
  val testServerURL: URL = getPropURL("TestServerURL", endsInSlash(testServerListen.toString()))

  val useServerListen: URL = new URL(
    testServerListen.getProtocol(),
    testServerListen.getHost(),
    port,
    testServerListen.getFile()
  )
  val useServerURL: URL = new URL(
    testServerURL.getProtocol(),
    testServerURL.getHost(),
    port,
    testServerURL.getFile()
  )

  def getBaseUrl(): String = useServerURL.toString()

  def getTestDefine() = {
    if (startedServer) {
      s"-DTestServerListen=${useServerListen}"::s"-DTestServerURL=${useServerURL}"::"-DTestServerStart=false"::Nil
    } else {
      Nil
    }
  }

  def runWithServer[A]( log: Logger, serverLogfile: String = "serverInTests.%u.log" )( f: => A ) = {
    startServer(log,serverLogfile)
    waitForServer(log)
    try {
      f
    } finally {
      stopServer(log)
    }
  }

  def get(
      url: String,
      connectTimeout: Int = 5000,
      readTimeout: Int = 5000,
      requestMethod: String = "GET") =
  {
    import java.net.{URL, HttpURLConnection}
    val connection = (new URL(url)).openConnection.asInstanceOf[HttpURLConnection]
    connection.disconnect()
    connection.setConnectTimeout(connectTimeout)
    connection.setReadTimeout(readTimeout)
    connection.setRequestMethod(requestMethod)
    val inputStream = connection.getInputStream
    val content = try {
      Source.fromInputStream(inputStream).mkString
    } finally {
      inputStream.close
    }
    content

  }

  def waitForServer( log: Logger ): Unit = {

    var lastError: Option[Exception] = None
    var i = 3
    while (i>0) {
      Thread.sleep(3000L)
      try {
        get( getBaseUrl())
        log.debug("Server is up and running")
        return
      } catch {
        case x: IOException =>
          log.info(s"Server not running: $x")
          lastError = Some(x)
        case x: SocketTimeoutException =>
          log.info(s"Server not running: $x")
          lastError = Some(x)
      }
    }
    throw new Exception(s"Server did not start: ${lastError.get}")
  }
}


object BridgeServer {

  def runjava( logger: Logger, cmd: List[String], workingDirectory: Option[File], envVars: Option[Map[String,String]] = None ): Unit = {
    new MyProcess(Some(logger)).java( cmd, workingDirectory, envVars )
  }

  /**
   * Find a file given the name.  If the name
   * @param targetname the name of the file to find.
   * @param suffix a suffix that replaces the extension of name to search for an alternate if name does not exist.
   */
  def findFile( targetname: File, suffix: String ): String = {
    val filename = targetname.toString
    if ( targetname.isFile() ) filename
    else {
      val name = filename.replace(if ("/" == File.separator) "\\" else "/", File.separator)
      if (name.endsWith(suffix)) throw new Error( "File does not exist: "+name )
      else {
        val nnamex = name.substring(0, name.length()-4)+suffix
        val idot = targetname.getName().lastIndexOf(".")
        val nname = if (idot < 0) name+suffix
        else {
          val lname = targetname.getName().length()
          name.substring(0, name.length()-lname+idot-1)+suffix
        }
        if (new File(nname).isFile() ) nname
        else throw new Error( "File does not exist: "+name )
      }
    }
  }

  /**
   * returns the names of the assembly jar and test jar for the project.
   * @param targetDirectory the directory that contains the jar files
   * @param assemblyJar the name of the assembly jar file
   * @param assemblyTestJar the name of the test jar file
   * @return a 2 tuple, the first entry is the assembly jar file name, the second is the test jar file name.
   */
  def findBridgeJars(
    targetDirectory: File,
    assemblyJar: String,
    assemblyTestJar: String
  ): (String, String) = {
    val assemblyjar = BridgeServer.findFile(
      new File(targetDirectory, assemblyJar),
      "-SNAPSHOT.jar"
    )
    val testjar = BridgeServer.findFile(
      new File(targetDirectory, assemblyTestJar),
      "-SNAPSHOT.jar"
    )
    (assemblyjar,testjar)
  }
}
