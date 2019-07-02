package com.example.backend.resource

import java.io.Reader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.BufferedReader
import scala.io.Codec
import scala.io.Source

import resource._
import java.io.Writer
import java.io.OutputStreamWriter
import java.io.FileOutputStream
import java.io.FileNotFoundException
import java.nio.file.Files
import java.io.File
import java.nio.file.Path
import java.nio.file.FileSystems
import java.nio.file.StandardCopyOption
import java.nio.file.NoSuchFileException
import java.io.IOException
import java.util.function.Predicate
import java.util.function.Consumer
import utils.logging.Logger
import scala.io.BufferedSource
import java.util.logging.Level

object FileIO {
  val log = Logger(getClass().getName)

  import scala.language.implicitConversions

  implicit val utf8 = Codec.UTF8

  implicit def getPath(filename: String): Path =
    FileSystems.getDefault.getPath(filename)

  implicit def getPath(filename: File): Path =
    FileSystems.getDefault.getPath(filename.toString())

  implicit def getFile(path: Path): File = path.toFile()

  implicit def getFile(filename: String): File = getPath(filename)

  val newsuffix = ".new"
  val newsuffixForPattern = "\\.new"

  def newfilename(filename: String) = filename + newsuffix

  def readFile(filename: File): String = {
    log.finest("Reading to file " + filename)
    var source: BufferedSource = null
    try {
      source = Source.fromFile(filename)
      source.mkString
    } catch {
      case e: Throwable =>
//        log.severe("Unable to read file "+filename, e)
        throw e
    } finally if (source != null) source.close()
  }

  private def getWriter(filename: File): Writer = {
    new OutputStreamWriter(new FileOutputStream(filename), utf8.charSet)
  }

  def writeFile(filename: File, data: String): Unit = {
    log.finest("Writing to file " + filename)
    try {
      for (out <- managed(getWriter(filename))) {
        out.write(data)
        out.flush()
      }
    } catch {
      case e: Throwable =>
        log.severe("Unable to write to file " + filename, e)
        throw e
    }
  }

  def deleteFile(path: String): Unit = deleteFile(getPath(path))

  def deleteFile(path: Path): Unit = {
    log.finest("Deleting file " + path)
    try Files.delete(path)
    catch {
      case e: NoSuchFileException =>
        log.fine(
          "attempting to delete a file, " + path + ", that doesn't exist, ignoring"
        )
      case e: Throwable =>
        log.severe("Unable to delete file " + path, e)
        throw e
    }
  }

  def moveFile(source: Path, dest: Path): Unit = {
    log.finest("Moving file " + source + " to " + dest)
    try Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING)
    catch {
      case e: IOException =>
        log.severe(s"Unable to move file $source to $dest, trying again", e)
        Thread.sleep(1000L)
        try Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING)
        catch {
          case e: IOException =>
            log.severe(s"Unable to move file $source to $dest", e)
            throw e
        }
    }
  }

  def copyFile(source: Path, dest: Path): Unit = {
    log.finest("Copying file " + source + " to " + dest)
    try Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING)
    catch {
      case e: IOException =>
        log.severe("Unable to copy file " + source + " to " + dest, e)
        throw e
    }
  }

  def deleteFileSafe(path: String): Unit = {
    try deleteFile(path)
    catch {
      case e: IOException =>
        try deleteFile(newfilename(path))
        catch {
          case e1: Throwable =>
            e.addSuppressed(e1)
        }
        throw e
    }
    deleteFile(newfilename(path))
  }

  def safeMoveFile(source: Path, dest: Path): Unit = {
    moveFile(source, dest)
  }

  def readFileSafe(filename: String): String = {
    try {
      val nf = newfilename(filename)
      val s = readFile(nf)
      try safeMoveFile(nf, filename)
      catch {
        case e: IOException =>
          log.warning(
            "Suppressing IOException on moving file " + nf + " to " + filename + ": " + e
          )
      }
      s
    } catch {
      case e: FileNotFoundException =>
        try readFile(filename)
        catch {
          case e1: IOException =>
            e1.addSuppressed(e)
            throw e1
        }
    }
  }

  def writeFileSafe(filename: String, data: String): Unit = {
    val file = getPath(filename)
    val newfile = getPath(newfilename(filename))
    try writeFile(newfile, data)
    catch {
      case e: IOException =>
        try deleteFile(newfile)
        catch {
          case e1: Throwable =>
            e.addSuppressed(e1)
        }
        throw e
    }
    safeMoveFile(newfile, file)
  }

  def onfiles(dir: File): Iterator[Path] = {
    import scala.collection.convert.ImplicitConversionsToScala._
    Files.list(dir).iterator()
  }

  def exists(path: String) = Files.exists(path)

  def mktree(path: Path) = Files.createDirectories(path)
}
