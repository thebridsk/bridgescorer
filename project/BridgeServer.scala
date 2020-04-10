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

class BridgeServer( val assemblyJar: String, val port: String = "8082") {
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
        runjava( log, "-jar"::assemblyJar::"--logfile"::serverLogfile::"start"::"--port"::port::Nil, workingDirectory )
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
        val url = new URL(s"${getBaseUrl}/v1/shutdown?doit=yes")
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

  def getBaseUrl() = s"http://localhost:$port"

  def getTestDefine() = {
    if (startedServer) {
      s"-DUseBridgeScorerURL=${getBaseUrl}"::Nil
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

  def runjava( logger: Logger, cmd: List[String], workingDirectory: Option[File] ): Unit = {
    new MyProcess(logger).java( cmd, workingDirectory )
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
