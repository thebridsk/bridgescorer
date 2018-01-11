package com.example

import scala.concurrent.duration.Duration
import scala.language.postfixOps

import org.rogach.scallop.ValueConverter
import org.rogach.scallop.singleArgConverter

import utils.logging.Logger
import utils.main.Subcommand
import scala.reflect.io.Path
import scala.reflect.io.Directory
import java.net.URL
import java.io.{File => JFile}
import java.util.jar.JarFile
import java.io.IOException
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.Writer
import java.io.OutputStream
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.{Path => JPath}
import java.nio.file.StandardCopyOption
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import scala.reflect.io.File
import java.nio.file.Paths
import com.example.version.VersionServer

/**
 * This is the update subcommand.
 *
 * This downloads the new code, then run the install command on the new code.
 */
object CollectLogs extends Subcommand("collectlogs") {

  val logger = Logger( CollectLogs.getClass.getName )

  implicit def dateConverter: ValueConverter[Duration] = singleArgConverter[Duration](Duration(_))

  import utils.main.Converters._

  val defaultZip = Path("logs.zip")
  val defaultStore = Path("./store")

  descr("Not implemented")

  banner(s"""
Collects the logs and other diagnostic information.

Syntax:
  scala ${Server.getClass.getName} collectlogs options
Options:""")

  footer(s"""
The server should NOT be running.  The logs should be in the current directory.
""")

  val optionZip = opt[Path]("zip", short='z', descr=s"the name of the output zipfile.  If it exists, it will be overwritten.  Default: ${defaultZip}", argName="zipfilename", default=Some(defaultZip))
  val optionStore = opt[Path]("store", short='s', descr=s"The store directory, default=${defaultStore}", argName="dir", default=Some(defaultStore))

  def executeSubcommand(): Int = {

    val zipfile = optionZip.toOption.getOrElse(defaultZip)
    val store = optionStore.toOption.getOrElse(defaultStore)

    val currentDir = Directory(".")
    val logfiles = currentDir.files.filter( f => f.extension == "log")

    val storefiles = Directory(store).files

    zip(zipfile,logfiles,storefiles)

    0
  }

  def version = {
    VersionServer.toString.getBytes("UTF8")
  }

  private def zip(out: Path, logfiles: Iterator[File], storefiles: Iterator[File] ) = {
    import resource._

    for ( zip <- managed( new ZipOutputStream(Files.newOutputStream(Paths.get(out.toString))) ) ) {
      {
        val nameInZip = "version.txt"
        val ze = new ZipEntry(nameInZip)
        println(s"Adding version info => ${ze.getName}")
        zip.putNextEntry(ze)
        zip.write(version)
        zip.closeEntry()
      }
      logfiles.foreach { file =>
        val nameInZip = file.name.toString
        val ze = new ZipEntry(nameInZip)
        println(s"Adding ${file} => ${ze.getName}")
        zip.putNextEntry(ze)
        Files.copy(Paths.get(file.toString), zip)
        zip.closeEntry()
      }
      storefiles.foreach { file =>
        val nameInZip = "store/"+file.name.toString
        val ze = new ZipEntry(nameInZip)
        println(s"Adding ${file} => ${ze.getName}")

        zip.putNextEntry(ze)
        Files.copy(Paths.get(file.toString), zip)
        zip.closeEntry()
      }
    }
  }

}
