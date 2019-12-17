package com.github.thebridsk.bridge.server.backend.resource

import scala.concurrent.duration._
import akka.http.scaladsl.model.StatusCodes
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.data.VersionedInstance
import scala.reflect.io.Directory
import com.github.thebridsk.utilities.logging.Logger
import scala.annotation.tailrec
import java.io.IOException

import FileStore.log
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.utilities.file.FileIO
import scala.reflect.io.File
import scala.reflect.io.Streamable.Bytes
import scala.reflect.io.Path
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.io.InputStream
import com.github.thebridsk.bridge.server.backend.resource.MetaData.MetaDataFile
import java.io.FileOutputStream
import resource.Using

object FileStore {
  val log = Logger[FileStore[_, _]]

  def apply[VId, VType <: VersionedInstance[VType, VType, VId]](
      name: String,
      directory: Directory,
      cacheInitialCapacity: Int = 5,
      cacheMaxCapacity: Int = 100,
      cacheTimeToLive: Duration = 10.minutes,
      cacheTimeToIdle: Duration = 9.minutes
  )(
      implicit
      cachesupport: StoreSupport[VId, VType],
      execute: ExecutionContext
  ): FileStore[VId, VType] = {
    new FileStore(
      name,
      directory,
      cacheInitialCapacity,
      cacheMaxCapacity,
      cacheTimeToLive,
      cacheTimeToIdle
    )
  }
}

class FilePersistentSupport[VId, VType <: VersionedInstance[VType, VType, VId]](
    val directory: Directory
)(
    implicit
    support: StoreSupport[VId, VType],
    execute: ExecutionContext
) extends PersistentSupport[VId, VType] {

  self =>

  val resourceName = support.resourceName

  /**
    * Get all the IDs from persistent storage
    */
  def getAllIdsFromPersistent(): Set[VId] = {
    val pattern = (resourceName + "\\.([^./]+)\\..*").r
//    val pattern = s"""${resourceName}\.([^.]+)\..*""".r

    val keys = directory.files
      .map { path => path.name }
      .flatMap {
        case pattern(sid) =>
          support.stringToId(sid)
        case _ =>
          None
      }
      .toSet

    log.finer(s"getAllIdsFromPersistent for ${resourceURI} returning ${keys}")

    keys
  }

  /**
    * Create an entry in the persistent store.
    * @param v the value to create.  The id field is ignored and will be assigned.
    * @return a future to the stored value, with the correct ID.
    */
  def createInPersistent(
      useId: Option[VId],
      v: VType,
      dontUpdateTimes: Boolean = false
  ): Future[Result[VType]] = {
    Future {
      synchronized {
        (useId match {
          case Some(i) => Result(i)
          case None =>
            generateNextId(v)
        }) match {
          case Right(id) =>
            val nv = v.setId(id, true, dontUpdateTimes).readyForWrite()
            write(id, nv)
          case Left(error) =>
            Result(error)
        }
      }
    }
  }

  /**
    * Read a resource from the persistent store
    * @param id
    * @return the result containing the resource or an error
    */
  def getFromPersistent(
      id: VId
  ): Future[Result[VType]] = {
    Future(read(id))
  }

  /**
    * Read a resource from the persistent store
    * @param id
    * @return the result containing the resource or an error
    */
  def read(id: VId): Result[VType] = self.synchronized {

    @tailrec
    def read(list: List[String]): Result[VType] = {
      if (list.isEmpty) notFound(id)
      else {
        val f = list.head

        val ovt = try {
          val v = FileIO.readFileSafe(f)
          val (goodOnDisk, vt) = support.fromJSON(v)
          if (!goodOnDisk) {
            val nf = writeFilename(id)
            log.warning(
              s"Writing current version to disk for file=${f} -> ${nf}"
            )
            write(id, vt)
            if (nf != f) { // did the extension change?
              FileIO.deleteFileSafe(f) // delete old file if extension changed
            }
          }
          Option(vt)
        } catch {
          case x: Exception =>
            log.info(s"Unable to read ${f}: ${x}")
            None
        }
        ovt match {
          case Some(vt) => Result(vt)
          case None     => read(list.tail)
        }
      }
    }

    read(readFilenames(id))
  }

  /**
    * Write a resource to the persistent store
    * @param id
    * @param v
    * @return the result containing the resource or an error
    */
  def putToPersistent(
      id: VId,
      v: VType
  ): Future[Result[VType]] =
    Future(self.synchronized {
      write(id, v)
    })

  private def write(id: VId, v: VType) = {
    try {
      FileIO.writeFileSafe(writeFilename(id), support.toJSON(v.readyForWrite()))
      Result(v)
    } catch {
      case e: IOException =>
        log.severe(s"Error writing ${id} to disk", e)
        internalError
    }
  }

  /**
    * Write a resource to the persistent store
    * @param id
    * @param v
    * @return the result containing the old resource or an error
    */
  def deleteFromPersistent(
      id: VId,
      cacheValue: Option[VType]
  ): Future[Result[VType]] = {
    Future {
      val r = cacheValue.map(v => Result(v)).getOrElse(read(id))
      r.foreach { v => // we have an existing value
        FileIO.deleteFileSafe(writeFilename(id))
      }
      r
    }
  }

  def readFilenames(id: VId) = {
    support.getReadExtensions().map { e =>
      (directory / (resourceName + "." + id + e)).toString()
    }
  }

  def writeFilename(id: VId) = {
    (directory / (resourceName + "." + id + support.getWriteExtension()))
      .toString()
  }

  private def newfilename(filename: String) = FileIO.newfilename(filename)

  /**
   * Get the metadata directory.  This does not create the metadata directory.
   */
  private def getMetadataDir( id: VId ): Directory = {
    (directory / (resourceName + "." + id)).toDirectory
  }

  /**
   * Get the metadata directory, creates the directory if it doesn't exist.
   */
  private def alwaysGetMetadataDir( id: VId ): Directory = {
    val dir = getMetadataDir(id)
    if (!dir.isDirectory) dir.jfile.mkdirs()
    dir
  }

  private def toMetadataFile( path: File, relativeTo: Directory ): MetaDataFile = {
    val f = path.toString()
    val d = relativeTo.toString()
    if (f.startsWith(d)) f.substring(d.length()+1)
    else f
  }

  /**
   * Get the File object for the metadata file.  This does not create the metadata directory.
   */
  private def toFileFromMetadataFile( id: VId, file: MetaDataFile ): File = {
    (getMetadataDir(id) / file).toFile
  }

  /**
   * List all the files for the specified match
   */
  override
  def listFiles( id: VId ): Result[Iterator[MetaDataFile]] = {
    val dir = getMetadataDir(id)
    if (dir.isDirectory) {
      val it = dir.files.map { f => toMetadataFile(f,dir) }
      Result(it)
    } else {
      Result( List().iterator)
    }
  }

  /**
   * List all the files for the specified match that match the filter
   */
  override
  def listFilesFilter( id: VId )( filter: MetaDataFile=>Boolean ): Result[Iterator[MetaDataFile]] = {
    val dir = getMetadataDir(id)
    if (dir.isDirectory) {
      val it = dir.files.map { f => toMetadataFile(f,dir) }.filter(filter)
      Result(it)
    } else {
      Result( List().iterator)
    }
  }

  /**
   * Write the specified source file to the target file, the target file is relative to the store directory.
   */
  override
  def write( id: VId, sourceFile: File, targetFile: MetaDataFile ): Result[Unit] = {
    val f = toFileFromMetadataFile(id,targetFile)
    f.parent.jfile.mkdirs
    Files.copy(sourceFile.jfile.toPath(), f.jfile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    Result.unit
  }

  /**
   * Write the specified source file to the target file, the target file is relative to the store directory for specified match.
   */
  override
  def write( id: VId, source: InputStream, targetFile: MetaDataFile ): Result[Unit] = {
    val f = toFileFromMetadataFile(id,targetFile)
    f.parent.jfile.mkdirs
    val out = new FileOutputStream(f.jfile)
    Using.bufferedOutputStream(new FileOutputStream(f.jfile)) { out =>
      ZipStoreInternal.copy(source,out)
    }
    Result.unit
  }

  /**
   * read the specified file, the file is relative to the store directory.
   */
  override
  def read( id: VId, file: MetaDataFile ): Result[InputStream] = {
    val f = toFileFromMetadataFile(id,file)
    if (f.isFile) {
      val is = new FileInputStream(f.jfile)
      Result(is)
    } else {
      Result(StatusCodes.NotFound,s"metadata file $file not found in resource $id")
    }
  }

  /**
   * delete the specified file, the file is relative to the store directory for specified match.
   */
  override
  def delete( id: VId, file: MetaDataFile ): Result[Unit] = {
    val f = toFileFromMetadataFile(id,file)
    if (f.exists) f.delete()
    Result.unit
  }

  /**
   * delete all the metadata files for the match
   */
  override
  def deleteAll( id: VId ): Result[Unit] = {
    val d = getMetadataDir(id)
    FileIO.deleteDirectory(d.jfile.toPath(),None)
    Result.unit
  }

}

object FilePersistentSupport {
  def apply[VId, VType <: VersionedInstance[VType, VType, VId]](
      directory: Directory
  )(
      implicit
      support: StoreSupport[VId, VType],
      execute: ExecutionContext
  ): FilePersistentSupport[VId, VType] = {
    new FilePersistentSupport(directory)
  }
}

class FileStore[VId, VType <: VersionedInstance[VType, VType, VId]](
    name: String,
    val directory: Directory,
    cacheInitialCapacity: Int = 5,
    cacheMaxCapacity: Int = 100,
    cacheTimeToLive: Duration = 10.minutes,
    cacheTimeToIdle: Duration = 9.minutes
)(
    implicit
    cachesupport: StoreSupport[VId, VType],
    execute: ExecutionContext
) extends Store[VId, VType](
      name,
      new FilePersistentSupport[VId, VType](directory),
      cacheInitialCapacity,
      cacheMaxCapacity,
      cacheTimeToLive,
      cacheTimeToIdle
    ) {}
