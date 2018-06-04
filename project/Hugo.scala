
import sbt.Logger
import java.io.File

object Hugo {

  def run( log: Logger, docsDir: File, targetDir: File ): Unit = {

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
}
