
import sbt.Logger
import java.io.{ File => JFile }
import scala.reflect.io.Directory

object Hugo {

  def run( log: Logger, docsDir: JFile, targetDir: JFile ): Unit = {

    val myproc = new MyProcess(Some(log))
    val cmd = List("hugo", "-D", "--destination", targetDir.getCanonicalPath)

    log.info(s"In Directory ${docsDir}")
    log.info(s"Starting ${cmd.mkString(" ")}")

    val proc = myproc.start(docsDir, cmd: _* )
    val rc = proc.waitFor()
    if (rc == 0) {
      log.debug( "Success: "+cmd.mkString(" ") )
    } else {
      log.error( s"Error ${rc}: "+cmd.mkString(" ") )
      throw new Exception(s"Error ${rc}: "+cmd.mkString(" "))
    }
  }

  def gotGeneratedImages( log: Logger, docsDir: JFile ): Boolean = {
    val gendir = new Directory( new JFile( docsDir, "static/images/gen" ) )

    val iter = gendir.deepFiles
    while (iter.hasNext) {
      val f = iter.next()
      if (f.extension == "png") return true
    }

    false
  }
}
