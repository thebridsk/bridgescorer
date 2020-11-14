package com.github.thebridsk.bridge.server.backend.resource

import scala.reflect.io.File
import java.util.zip.ZipFile
import scala.jdk.CollectionConverters._
import java.util.zip.ZipEntry
import scala.io.Source
import com.github.thebridsk.utilities.logging.Logger

import ZipFileForStore.log
import java.io.InputStream

class ZipFileForStore(
    val zipfilename: File
) {

  private val zipfile = new ZipFile(zipfilename.toString)

  /**
    * Returns all the entries in the zip file
    */
  def entries(): Iterator[ZipEntry] = {
    zipfile.entries().asScala
  }

  /**
    * Returns the input stream of a zip entry
    * @param zipentry
    * @return None if an error occurred
    */
  def getInputStream(zipentry: ZipEntry): Option[InputStream] = {
    try {
      Option(zipfile.getInputStream(zipentry))
    } catch {
      case x: Exception =>
        log.info(
          s"Error getting file ${zipentry.getName} from ${zipfilename}",
          x
        )
        None
    }
  }

  /**
    * Returns the input stream of a zip entry
    * @param zipentry
    * @return None if an error occurred
    */
  def getInputStream(zipentry: String): Option[InputStream] = {
    Option(zipfile.getEntry(zipentry)) match {
      case None => None
      case Some(ze) =>
        getInputStream(ze)
    }
  }

  def readFileSafe(filename: String): Option[String] = {
    Option(zipfile.getEntry(filename)).flatMap { zipentry =>
      getInputStream(zipentry).flatMap { is =>
        try {
          Option(Source.fromInputStream(is, "UTF8").mkString)
        } catch {
          case x: Exception =>
            log.info(
              s"Error reading file ${zipentry.getName} from ${zipfilename}",
              x
            )
            None
        } finally {
          is.close()
        }
      }
    }
  }

  def close(): Unit = {
    zipfile.close()
  }
}

object ZipFileForStore {

  val log: Logger = Logger[ZipFileForStore]()

}
