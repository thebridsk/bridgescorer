package com.github.thebridsk.bridge.server

import scala.concurrent.duration.Duration
import scala.language.postfixOps

import org.rogach.scallop.ValueConverter
import org.rogach.scallop.singleArgConverter

import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.utilities.main.Subcommand
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
import com.github.thebridsk.bridge.server.version.VersionServer
import com.github.thebridsk.bridge.data.version.VersionShared
import com.github.thebridsk.utilities.version.VersionUtilities
import scala.util.Using

/**
  * This is the update subcommand.
  *
  * This downloads the new code, then run the install command on the new code.
  */
object CollectLogs extends Subcommand("collectlogs") {

  val logger = Logger(CollectLogs.getClass.getName)

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  import com.github.thebridsk.utilities.main.Converters._

  val defaultZip = Path("logs.zip")
  val defaultStore = Path("./store")

  descr("Collects the logs and other diagnostic information")

  banner(s"""
Collects the logs and other diagnostic information.

Syntax:
  scala ${Server.getClass.getName} collectlogs options
Options:""")

  footer(s"""
The server should NOT be running.
""")

  val optionZip = opt[Path](
    "zip",
    short = 'z',
    descr =
      s"the name of the output zipfile.  If it exists, it will be overwritten.  Default: ${defaultZip}",
    argName = "zipfilename",
    default = Some(defaultZip)
  )
  val optionStore = opt[Path](
    "store",
    short = 's',
    descr = s"The store directory, default=${defaultStore}",
    argName = "dir",
    default = Some(defaultStore)
  )

  val optionDiagnosticDir = opt[Path](
    "diagnostics",
    short = 'd',
    required = true,
    descr =
      "The directory that contains the log files.  All .log files in directory may be collected for diagnostic purposes.",
    argName = "dir",
    default = None
  )

  def executeSubcommand(): Int = {

    val zipfile = optionZip.toOption.getOrElse(defaultZip)
    val store = optionStore.toOption.getOrElse(defaultStore)

    val diagDir = optionDiagnosticDir.toOption
      .map(p => p.toDirectory)
      .getOrElse(Directory("."))
    val logfiles =
      diagDir.files.filter(f => f.extension == "log" || f.extension == "csv")

    val storefiles = Directory(store).files

    zip(zipfile, logfiles, storefiles)

    0
  }

  private def zip(
      out: Path,
      logfiles: Iterator[File],
      storefiles: Iterator[File]
  ) = {

    Using.resource(
        new ZipOutputStream(Files.newOutputStream(Paths.get(out.toString)))
    ) { zip =>
      {
        val nameInZip = "version.txt"
        val ze = new ZipEntry(nameInZip)
        println(s"Adding version info => ${ze.getName}")
        zip.putNextEntry(ze)
        val v =
          s"""${VersionServer.toString}\n${VersionShared.toString}\n${VersionUtilities.toString}"""
        zip.write(v.getBytes("UTF8"))
        zip.closeEntry()
      }
      CollectLogs.copyResourceToZip(
        "com/github/thebridsk/bridge/bridgescorer/version/VersionBridgeScorer.properties",
        "VersionBridgeScorer.properties",
        zip
      )

      CollectLogs.copyResourceToZip(
        "com/github/thebridsk/bridge/utilities/version/VersionUtilities.properties",
        "VersionUtilities.properties",
        zip
      )
      logfiles.foreach { file =>
        val nameInZip = file.name.toString
        val ze = new ZipEntry("logs/" + nameInZip)
        println(s"Adding ${file} => ${ze.getName}")
        zip.putNextEntry(ze)
        Files.copy(Paths.get(file.toString), zip)
        zip.closeEntry()
      }
      storefiles.foreach { file =>
        val nameInZip = "store/" + file.name.toString
        val ze = new ZipEntry(nameInZip)
        println(s"Adding ${file} => ${ze.getName}")

        zip.putNextEntry(ze)
        Files.copy(Paths.get(file.toString), zip)
        zip.closeEntry()
      }
    }
  }

  /**
    * Copy the specified resource into the zip file with the given name.
    * If resource does not exist, then this is a noop.
    * @param resource the resource to load
    * @param nameInZip the name of the file in the zipfile.
    * @param zip the zip output stream
    */
  def copyResourceToZip(
      resource: String,
      nameInZip: String,
      zip: ZipOutputStream
  ) = {
    val cl = getClass.getClassLoader
    val instream = cl.getResourceAsStream(resource)
    if (instream != null) {
      Using.resource(instream) { in =>
        val ze = new ZipEntry(nameInZip)
        logger.fine(s"Adding version info => ${ze.getName}")
        zip.putNextEntry(ze)
        copy(in, zip)
        zip.closeEntry()
      }
    }
  }

  def copy(in: InputStream, out: OutputStream) = {
    val b = new Array[Byte](1024 * 1024)

    var count: Long = 0
    var rlen = 0
    while ({ rlen = in.read(b); rlen } > 0) {
      out.write(b, 0, rlen)
      count += rlen
    }
    count
  }

}
