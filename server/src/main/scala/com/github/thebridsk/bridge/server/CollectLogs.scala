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
import com.github.thebridsk.bridge.server.backend.BridgeService
import scala.concurrent.Await

/**
  * This is the update subcommand.
  *
  * This downloads the new code, then run the install command on the new code.
  */
object CollectLogs extends Subcommand("diagnostics", "collectlogs") {

  val logger = Logger(CollectLogs.getClass.getName)

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  import com.github.thebridsk.utilities.main.Converters._

  val defaultZip = Path("logs.zip")
  val defaultStore = Path("./store")

  descr(
    "Collects the logs and other diagnostic information"
  )

  banner(s"""
Collects the logs and other diagnostic information.

Syntax:
  scala ${Server.getClass.getName} ${name} options
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
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

    val zipfile = optionZip.toOption.getOrElse(defaultZip)
    val store = optionStore.toOption.getOrElse(defaultStore)

    val diagDir = optionDiagnosticDir.toOption
      .map(p => p.toDirectory)
      .orElse(Some(Directory(".")))

    val storeservice = BridgeService(store, diagnosticDirectory = diagDir)

    val f = storeservice.export(
      Files.newOutputStream(zipfile.jfile.toPath()),
      None,
      true
    )

    Await.result(f, Duration.Inf) match {
      case Right(value) =>
        0
      case Left((statuscode,restmessage)) =>
        logger.severe(s"Error writing diagnostics: ${restmessage.msg}")
        1
    }

  }

}
