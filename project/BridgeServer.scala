import sbt.Fork
import java.io.File
import sbt.ForkOptions
import java.net.URL
import java.net.HttpURLConnection
import java.io.InputStream
import sbt.Logger
import java.nio.file.Files


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
        val url = new URL("http://localhost:"+port+"/v1/shutdown?doit=yes")
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

  def getTestDefine() = {
    if (startedServer) {
      "-DUseBridgeScorerURL=http://localhost:"+port::Nil
    } else {
      Nil
    }
  }

  def runWithServer[A]( log: Logger, serverLogfile: String = "serverInTests.%u.log" )( f: => A ) = {
    startServer(log,serverLogfile)
    try {
      f
    } finally {
      stopServer(log)
    }
  }
}


object BridgeServer {

  def runjava( log: Logger, cmd: List[String], workingDirectory: Option[File] ): Unit = {
    log.info( "Running java "+cmd.mkString(" ") )
    val rc = Fork.java( ForkOptions().withWorkingDirectory( workingDirectory), cmd )
    if (rc != 0) throw new Error("Failed running java "+cmd.mkString(" "))
  }

  /**
   * Find a file given the name.
   * @param suffix a suffix that replaces the last 4 chars of name to search for an alternate if name does not exist.
   */
  def findFile( filename: String, suffix: String ) = {
    val name = filename.replace(if ("/" == File.separator) "\\" else "/", File.separator)
    if ( new File(name).isFile() ) name
    else {
      if (name.endsWith(suffix)) throw new Error( "File does not exist: "+name )
      else {
        val nname = name.substring(0, name.length()-4)+suffix
        if (new File(nname).isFile() ) nname
        else throw new Error( "File does not exist: "+name )
      }
    }
  }
}
