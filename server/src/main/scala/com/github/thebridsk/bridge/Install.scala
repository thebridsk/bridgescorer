package com.github.thebridsk.bridge

import scala.concurrent.duration.Duration
import scala.language.postfixOps

import org.rogach.scallop.ValueConverter
import org.rogach.scallop.singleArgConverter

import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.utilities.main.Subcommand
import scala.reflect.io.Path
import scala.reflect.io.Directory
import java.net.URL
import java.io.File
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
import com.github.thebridsk.utilities.classpath.ClassPath
import java.nio.file.Paths
import com.github.thebridsk.bridge.util.GitHub
import com.github.thebridsk.bridge.version.VersionServer
import com.github.thebridsk.bridge.util.Version

/**
  * This is the update subcommand.
  *
  * This downloads the new code, then run the install command on the new code.
  */
object UpdateInstall extends Subcommand("update") {

  val logger = Logger(UpdateInstall.getClass.getName)

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  descr("Downloads the latest version of the server, if necessary")

  banner(s"""Downloads the latest version of the server, if necessary from
https://github.com/thebridsk/bridgescorer/releases/latest

Syntax:
  ${Server.cmdName} update options
Options:""")

//  footer(s"""
//""")

  import Server.output

  def executeSubcommand(): Int = {
//    setConsoleLoggerToInfo()
    val github = new GitHub("thebridsk/bridgescorer")
    val r =
      github
        .getLatestReleaseObject()
        .filterOrElse(
          { release =>
            output(release.forTrace())
            val githubv = release.getVersion()
            val myversion = Version.create(VersionServer.version)

            output(
              s"Latest version is ${githubv}, running version is ${myversion}"
            )
            myversion < githubv
          },
          "No need to update"
        )
        .flatMap { release =>
          release.assets.find(a => a.name.endsWith(".jar")) match {
            case Some(asset) =>
              github.downloadFileAndCheckSHA(asset.browser_download_url, ".") match {
                case Right((file, sha)) =>
                  output(
                    s"Downloaded new version: ${file} ${github.shaAlgorithm} ${sha}"
                  )
                  Right(file)
                case Left(error) =>
                  Left(error)
              }
            case None =>
              Left("Did not find jar asset in release")
          }
        }
    r match {
      case Right(v) =>
        0
      case Left(error) =>
        output(s"Update not performed: ${error}")
        1
    }
  }

}

/**
  * This is the Install subcommand.
  *
  * Installs the server, then starts the new code with the "cleanup" command.
  */
object Install extends Subcommand("install") {

  val logger = Logger(Install.getClass.getName)

  val filesForWindows = "server.bat" :: "serverMemory.bat" ::
    "collectlogs.bat" :: "update.bat" ::
    "findServerJar.bat" :: Nil
  val filesForLinux = "server" :: "serverMemory" :: "collectlogs" :: "update" :: Nil

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  import com.github.thebridsk.utilities.main.Converters._

  descr("Install the jar used to run this command")

  banner(s"""
Installs the server code

Syntax:
  ${Server.cmdName} install
Options:""")

  footer(s"""
Copy the server jar file to the installation directory.  Then run the following command from the installation directory:
  java -jar jarfile install
""")

  import Server.output

  def executeSubcommand(): Int = {
    val target = Directory(Path(".").toCanonical)

    if (!target.isDirectory) {
      logger.severe(s"Target must be a directory: ${target}")
      1
    } else {

      if (isWindows()) {
        output(s"""Installing to ${target} for Windows""")
        filesForWindows.foreach { fw =>
          writeFile(target, fw)
        }
      } else if (isMac() || isLinux()) {
        output(s"""Installing to ${target} for MacOS or Linux""")
        filesForLinux.foreach { fl =>
          writeFile(target, fl)
          (target / fl).toFile.setExecutable(true, true)
        }
      }
      0
    }
  }

  def getOsName() = {
    sys.env.get("OS_OVERRIDE") match {
      case Some(s) => s
      case None    => sys.props.getOrElse("os.name", "oops").toLowerCase()
    }
  }

  def isWindows() = getOsName().contains("win")

  def isMac() = getOsName().contains("mac")

  def isLinux() = {
    val x = getOsName()
    x.contains("nix") || x.contains("nux")
  }

  /**
    * @param tdir - where to put the file
    * @param url - the URL of the jar file
    * @param f - the jar file that contains the file to copy
    * @param fileToCopy - the file to be copied out of the jar file into the tdir
    */
  def writeFile(tdir: Directory, fileToCopy: String) = {

    val outfile = tdir / fileToCopy
    Option(getClass.getClassLoader.getResourceAsStream(fileToCopy)) match {
      case Some(in) =>
        import resource._
        for (min <- managed(in)) {
          Files.copy(
            min,
            Paths.get(outfile.toString),
            StandardCopyOption.REPLACE_EXISTING
          )
        }
      case None =>
        logger.warning(s"Did not find ${fileToCopy}")
    }
  }
}