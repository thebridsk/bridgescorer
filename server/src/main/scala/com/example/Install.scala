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
import utils.classpath.ClassPath
import java.nio.file.Paths
import com.example.util.GitHub
import com.example.version.VersionServer
import com.example.util.Version

/**
 * This is the update subcommand.
 *
 * This downloads the new code, then run the install command on the new code.
 */
object UpdateInstall extends Subcommand("update") {

  val logger = Logger( UpdateInstall.getClass.getName )

  implicit def dateConverter: ValueConverter[Duration] = singleArgConverter[Duration](Duration(_))

  descr("Not implemented")

  banner(s"""
Updates the installed server code

Syntax:
  scala ${Server.getClass.getName} update options
Options:""")

  footer(s"""
The old server must NOT be running.
""")

  def executeSubcommand(): Int = {
    val github = new GitHub( "thebridsk/bridgescorer" )
    val r =
    github.getLatestVersion().map { githubv =>
      val myversion = Version.create( VersionServer.version )

      logger.info( s"Latest version is ${githubv}, running version is ${myversion}" )
      myversion < githubv
    }.flatMap { needToUpdate =>
      if (needToUpdate) {
        logger.info( s"Need to update to latest version" )
        Right( needToUpdate )
      } else {
        Left( "No need to update" )
      }
    }.flatMap { needToUpdate =>
      github.downloadLatestAsset(".", "bridgescorer-server-assembly") match {
        case Right((file,sha)) =>
          logger.info(s"Downloaded new version: ${file} ${github.shaAlgorithm} ${sha}")
          Right(file)
        case Left(error) =>
          Left(error)
      }
    }
    r match {
      case Right(v) =>
        0
      case Left(error) =>
        logger.warning(s"Update not performed: ${error}")
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

  val logger = Logger( Install.getClass.getName )

  val filesForWindows = "server.bat"::"collectlogs.bat"::Nil
  val filesForLinux = "server"::"collectlogs"::Nil

  implicit def dateConverter: ValueConverter[Duration] = singleArgConverter[Duration](Duration(_))

  import utils.main.Converters._

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

  def executeSubcommand(): Int = {
    val target = Directory(Path(".").toCanonical)

    if (target.isDirectory) {
      logger.info(s"""Installing to ${target}""")
    } else {
      logger.severe(s"Target must be a directory: ${target}")
    }

    if (isWindows()) {
      filesForWindows.foreach { fw =>
        writeFile(target, fw)
      }
    } else if (isMac() || isLinux()) {
      filesForLinux.foreach { fl =>
        writeFile(target, fl)
        (target/fl).toFile.setExecutable(true, true)
      }
    }
    0
  }

  def getOsName() = {
    sys.env.get("OS_OVERRIDE") match {
      case Some(s) => s
      case None => sys.props.getOrElse("os.name", "oops").toLowerCase()
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
  def writeFile( tdir: Directory, fileToCopy: String ) = {

    val outfile = tdir/fileToCopy
    Option(getClass.getClassLoader.getResourceAsStream(fileToCopy)) match {
      case Some(in) =>
        import resource._
        for ( min <- managed(in) ) {
          Files.copy(min, Paths.get(outfile.toString), StandardCopyOption.REPLACE_EXISTING)
        }
      case None =>
        logger.warning(s"Did not find ${fileToCopy}")
    }
  }
}

/**
 * This is the Cleanup subcommand.
 *
 * Cleans up after an install.
 */
object InstallCleanup extends Subcommand("clean") {

  val logger = Logger( InstallCleanup.getClass.getName )

  implicit def dateConverter: ValueConverter[Duration] = singleArgConverter[Duration](Duration(_))

  descr("Cleanup after an install")

  banner(s"""
Cleanup after an install

Syntax:
  scala ${Server.getClass.getName} cleanup options
Options:""")

  footer(s"""
The server running the install command must terminate shortly after starting this command.
""")

  def executeSubcommand(): Int = {
    0
  }

}
