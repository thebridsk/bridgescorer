package com.github.thebridsk.bridge.server

import scala.concurrent.duration.Duration

import org.rogach.scallop.ValueConverter
import org.rogach.scallop.singleArgConverter

import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.utilities.main.Subcommand
import scala.reflect.io.Path
import scala.reflect.io.Directory
import java.nio.file.Files
import com.github.thebridsk.bridge.server.backend.BridgeService
import scala.concurrent.Await
import org.rogach.scallop.ScallopOption

/**
  * This is the update subcommand.
  *
  * This downloads the new code, then run the install command on the new code.
  */
object CollectLogs extends Subcommand("diagnostics", "collectlogs") {

  val logger: Logger = Logger(CollectLogs.getClass.getName)

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  import com.github.thebridsk.utilities.main.Converters._

  val defaultZip: Path = Path("logs.zip")
  val defaultStore: Path = Path("./store")

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

  val optionZip: ScallopOption[Path] = opt[Path](
    "zip",
    short = 'z',
    descr =
      s"the name of the output zipfile.  If it exists, it will be overwritten.  Default: ${defaultZip}",
    argName = "zipfilename",
    default = Some(defaultZip)
  )
  val optionStore: ScallopOption[Path] = opt[Path](
    "store",
    short = 's',
    descr = s"The store directory, default=${defaultStore}",
    argName = "dir",
    default = Some(defaultStore)
  )

  val optionDiagnosticDir: ScallopOption[Path] = opt[Path](
    "diagnostics",
    short = 'd',
    required = true,
    descr =
      "The directory that contains the log files.  All .log files in directory may be collected for diagnostic purposes.",
    argName = "dir",
    default = None
  )

  def executeSubcommand(): Int = {
    implicit val ec: scala.concurrent.ExecutionContext =
      scala.concurrent.ExecutionContext.global

    val zipfile = optionZip.toOption.getOrElse(defaultZip)
    val store = optionStore.toOption.getOrElse(defaultStore)

    val diagDir = optionDiagnosticDir.toOption
      .map(p => p.toDirectory)
      .orElse(Some(Directory(".")))

    val storeservice = BridgeService(store, diagnosticDirectory = diagDir)

    val f = storeservice.doExport(
      Files.newOutputStream(zipfile.jfile.toPath()),
      None,
      true
    )

    Await.result(f, Duration.Inf) match {
      case Right(value) =>
        0
      case Left((statuscode, restmessage)) =>
        logger.severe(s"Error writing diagnostics: ${restmessage.msg}")
        1
    }

  }

}
