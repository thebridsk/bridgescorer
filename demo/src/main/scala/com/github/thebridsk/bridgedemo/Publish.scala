package com.github.thebridsk.bridgedemo

import java.io.File
import com.github.thebridsk.bridgedemo.utils.MyFileUtils
import scala.util.Using
import java.util.jar.JarInputStream
import java.io.FileInputStream
import java.util.jar.JarEntry
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import com.github.thebridsk.utilities.main.Main
import scala.reflect.io.Path
import com.github.thebridsk.utilities.logging.Logger

class Publish

object Publish extends Main {

  import com.github.thebridsk.utilities.main.Converters._

  val log = Logger[Publish]()

  val optionJar = trailArg[Path](name="jar", descr="Jar file", default=None, required=true)
  val optionTarget = trailArg[Path](name="target", descr="target directory, will delete and create directory", default=Some("target/demo"))

  class Exit( msg: String ) extends Exception(msg)

  def execute(): Int = {
    // println(ClassPath.showProperties())
    val jarfile = optionJar().toFile.jfile
    val targetdir = optionTarget().toFile.jfile
    val version = getVersionFromJar(jarfile)
    val count = extract(jarfile,version,targetdir)
    writeFile(new File("index.html"), new File(targetdir, "index.html"))
    writeFile(new File("../fullserver/target/swagger.yaml"), new File(targetdir, "public/swagger.yml"))
    log.info( s"Copied ${count+2} files")
    0
  }

  val patternJarFileName = """bridgescorekeeper-server-(\d+\.\d+(?:\.\d+)?-(?:SNAPSHOT-)?[0-9a-f]{40}(?:-SNAPSHOT)?(?:-.+?)?)\.jar""".r
  def getVersionFromJar( jarfile: File ): String = {
    jarfile.getName.toString match {
      case patternJarFileName(version) =>
        log.info(s"version is $version")
        version
      case name => throw new Exception( s"Unable to determine version from $jarfile")
    }
  }

  class JarIterator(jar: JarInputStream ) extends Iterator[JarEntry] {

    private var fNext: JarEntry = null
    private var eof = false

    def hasNext: Boolean = {
      if (!eof && fNext == null) {
        fNext = jar.getNextJarEntry()
        if (fNext == null) eof = true
      }
      fNext != null
    }

    def next(): JarEntry = {
      if (fNext == null) hasNext
      if (fNext == null) throw new NoSuchElementException
      val n = fNext
      fNext = null
      n
    }
  }

  val patternBridgeScoreKeeper = """META-INF/resources/webjars/bridgescorekeeper/([^/]+)/(.*)""".r
  val patternFullServer = """META-INF/resources/webjars/bridgescorer-fullserver/([^/]+)/(.*)""".r

  def extract( jarfile: File, version: String, targetdir: File ): Int = {

    MyFileUtils.deleteDirectory( targetdir.toPath(), None)

    targetdir.mkdirs()

    val publicdir = new File( targetdir, "public" )

    Using.resource( new JarInputStream( new FileInputStream(jarfile) ) ) { jar =>
      new JarIterator(jar).filter(n => !n.isDirectory && !n.getName.endsWith(".gz")).foldLeft(0) { case (count, e) =>
        e.getName() match {
          case patternBridgeScoreKeeper(fileversion,relativePath) if fileversion == version =>
            if (!relativePath.endsWith("/.keep")) {
              val targetFile = new File(targetdir, relativePath)
              writeFile(jar, targetFile)
              count+1
            } else {
              count
            }

          case patternFullServer(fileversion,relativePath) if fileversion == version =>
            if (!relativePath.endsWith(".js.map") && !relativePath.endsWith(".css.map")) {
              val targetFile = new File(publicdir, relativePath)
              writeFile(jar, targetFile)
              count+1
            } else {
              count
            }

          case n =>
            log.finer(s"Ignoring ${e.getName}")
            count
        }

      }
    }
  }

  def writeFile( jarfile: JarInputStream, targetFile: File ) = {
    targetFile.getParentFile.mkdirs()

    log.info(s"Writing ${targetFile} from Jar file")

    Files.copy(jarfile, targetFile.toPath, StandardCopyOption.REPLACE_EXISTING)

  }

  def writeFile( source: File, targetFile: File ) = {
    targetFile.getParentFile.mkdirs()

    log.info(s"Writing ${targetFile} from file ${source}")

    Files.copy(source.toPath, targetFile.toPath, StandardCopyOption.REPLACE_EXISTING)

  }
}
