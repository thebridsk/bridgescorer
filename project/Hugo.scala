
import sbt.Logger
import java.io.{ File => JFile }
import scala.reflect.io.Directory

object Hugo {

  def run( log: Logger, docsDir: JFile, targetDir: JFile, longversion: String, shortversion: String ): Unit = {

    val myproc = new MyProcess(Some(log))
    val cmd = List("hugo", "-D", "--destination", targetDir.getCanonicalPath)

    log.info(s"In Directory ${docsDir}")
    log.info(s"Starting ${cmd.mkString(" ")}")

    val addenvp = Map(
      "HUGO_BRIDGESCORERVERSIONLONG" -> longversion,
      "HUGO_BRIDGESCORERVERSION" -> shortversion
    )
    val proc = myproc.start(docsDir, addenvp, cmd: _* )
    val rc = proc.waitFor()
    if (rc == 0) {
      log.debug( "Success: "+cmd.mkString(" ") )
    } else {
      log.error( s"Error ${rc}: "+cmd.mkString(" ") )
      throw new Exception(s"Error ${rc}: "+cmd.mkString(" "))
    }
  }

  def runServer( log: Logger, docsDir: JFile, longversion: String, shortversion: String ): Unit = {

    val myproc = new MyProcess(Some(log))
    val cmd = List("hugo", "-D", "server" )

    log.info(s"In Directory ${docsDir}")
    log.info(s"Starting ${cmd.mkString(" ")}")

    val addenvp = Map("HUGO_BRIDGESCORERVERSIONLONG" -> longversion, "HUGO_BRIDGESCORERVERSION" -> shortversion)
    val proc = myproc.start(docsDir, addenvp, cmd: _* )
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
      if (f.extension == "png") {
        log.debug( s"Hugo.gotGeneratedImages: found image $f")
        return true
      }
    }

    log.debug( s"Hugo.gotGeneratedImages: did not find any images in $gendir")
    false
  }
}
