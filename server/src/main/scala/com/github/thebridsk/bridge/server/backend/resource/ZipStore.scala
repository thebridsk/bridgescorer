package com.github.thebridsk.bridge.server.backend.resource

import scala.concurrent.duration._
import akka.http.scaladsl.model.StatusCodes
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.data.VersionedInstance
import scala.reflect.io.Directory
import com.github.thebridsk.utilities.logging.Logger
import scala.annotation.tailrec
import java.io.IOException
import scala.jdk.CollectionConverters._
import ZipStore.log
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.github.thebridsk.bridge.data.Id
import java.util.zip.ZipFile
import Implicits._
import scala.reflect.io.File
import java.io.InputStream

import MetaData.MetaDataFile
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import java.io.OutputStreamWriter
import java.io.OutputStream
import scala.util.Using

object ZipStore {
  val log = Logger[ZipStore[_, _]]()

  def apply[VId, VType <: VersionedInstance[VType, VType, VId]](
      name: String,
      zipfile: ZipFileForStore,
      cacheInitialCapacity: Int = 5,
      cacheMaxCapacity: Int = 100,
      cacheTimeToLive: Duration = 10.minutes,
      cacheTimeToIdle: Duration = 9.minutes
  )(
      implicit
      cachesupport: StoreSupport[VId, VType],
      execute: ExecutionContext
  ): ZipStore[VId, VType] = {
    new ZipStore(
      name,
      zipfile,
      cacheInitialCapacity,
      cacheMaxCapacity,
      cacheTimeToLive,
      cacheTimeToIdle
    )
  }
}

object ZipStoreInternal {

  val storedir = "store/"

  def metadataDir[VId]( id: VId, resourceName: String ) = {
    s"${storedir}${resourceName}.${id}/"
  }

  def toMetaDataFile( dir: String, filename: String ): MetaDataFile = {
    val l = dir.length()
    filename.drop( l )
  }

  def toFilename( dir: String, mdf: MetaDataFile ): String = {
    s"${dir}${mdf}"
  }

  /**
    * Writes the store contents in export format to the output stream.
    * The zip output stream is NOT finished.
    *
    * @param zip the zip output stream
    * @param store the store to export
    * @param filter the filter, None means everything, otherwise list of Ids to export
    * @return a future to a result that has a list of all the Ids of entities that are exported.
    */
  def exportStore[TId, T <: VersionedInstance[T, T, TId]](
      zip: ZipOutputStream,
      store: Store[TId, T],
      filter: Option[List[String]]
  )(
    implicit ec: ExecutionContext
  ): Future[Result[List[String]]] = {
    store.readAll().map { rmap =>
      rmap match {
        case Right(map) =>
          Result(
            map
              .filter { entry =>
                val (id, v) = entry
                filter
                  .map { f =>
                    f.contains(id.toString())
                  }
                  .getOrElse(true)
              }
              .map { entry =>
                val (id, v) = entry
                val dir = metadataDir(id,store.support.resourceName)
                val name =
                  s"${storedir}${store.support.resourceName}.${id}${store.support.getWriteExtension()}"
                val content = store.support.toJSON(v)
                zip.putNextEntry(new ZipEntry(name))
                val out = new OutputStreamWriter(zip, "UTF8")
                out.write(content)
                out.flush
                store.persistent.listFiles(id) match {
                  case Left(err) =>
                    // ignore errors
                  case Right(files) =>
                    files.foreach { mdf =>
                      store.persistent.read(id,mdf) match {
                        case Left(value) =>
                          // ignore errors
                        case Right(input) =>
                          Using.resource(input) { is =>
                            zip.putNextEntry( new ZipEntry(s"${dir}${mdf}"))
                            copy(is,zip)
                          }
                      }
                    }
                }
                zip.flush
                id.toString()
              }
              .toList
          )
        case Left((statusCode, msg)) =>
          Result(statusCode, msg)
      }
    }
  }

  def copy( in: InputStream, out: OutputStream ): Unit = {
    val buf = new Array[Byte](1024*1024)
    while (true) {
      val l = in.read(buf)
      if (l <= 0) return
      out.write(buf,0,l)
    }
  }

}

import ZipStoreInternal._

class ZipPersistentSupport[VId, VType <: VersionedInstance[VType, VType, VId]](
    val zipfile: ZipFileForStore
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
      val pattern = (s"""${storedir}${resourceName}\\.([^./]+)\\..*""").r

    val keys = zipfile
      .entries()
      .map { zipentry =>
        zipentry.getName
      }
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
    storeIsReadOnly.logit("ZipPersistentSupport.createInPersistent")
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
          zipfile.readFileSafe(f) match {
            case Some(v) =>
              val (goodOnDisk, vt) = support.fromJSON(v)
              Option(vt)
            case None =>
              None
          }
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
  ): Future[Result[VType]] = {
    storeIsReadOnly.logit("ZipPersistentSupport.putToPersistent")
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
    storeIsReadOnly.logit("ZipPersistentSupport.deleteFromPersistent")
  }

  def readFilenames(id: VId) = {
    support.getReadExtensions().map { e =>
      s"${storedir}${resourceName}.${id}${e}"
    }
  }

  /**
   * List all the files for the specified match, all returned filenames are relative to the store directory for specified match.
   * To read the file, the read method must be used on this object.
   */
  override
  def listFiles( id: VId ): Result[Iterator[MetaDataFile]] = {
    self.synchronized {
      val dir = metadataDir(id,resourceName)
      val list: Iterator[MetaDataFile] = zipfile.entries().flatMap { ze =>
        if (ze.getName().startsWith(dir)) {
          toMetaDataFile( dir, ze.getName())::Nil
        } else {
          Nil
        }
      }
      Result(list)
    }
  }

  /**
   * List all the files for the specified match that match the filter, all returned filenames are relative to the store directory for specified match.
   * To read the file, the read method must be used on this object.
   */
  override
  def listFilesFilter( id: VId )( filter: MetaDataFile=>Boolean ): Result[Iterator[MetaDataFile]] = {
    listFiles(id) match {
      case Left(value) => Left(value)
      case Right(files) =>
        val f = files.filter( filter(_))
        Right(f)
    }
  }

  /**
   * Write the specified source file to the target file, the target file is relative to the store directory for specified match.
   */
  override
  def write( id: VId, sourceFile: File, targetFile: MetaDataFile ): Result[Unit] = StoreIdMeta.resultNotSupported

  /**
   * Write the specified source file to the target file, the target file is relative to the store directory for specified match.
   */
  override
  def write( id: VId, source: InputStream, targetFile: MetaDataFile ): Result[Unit] = StoreIdMeta.resultNotSupported

  /**
   * read the specified file, the file is relative to the store directory for specified match.
   */
  override
  def read( id: VId, file: MetaDataFile ): Result[InputStream] = {
    val filename = toFilename( metadataDir(id,resourceName), file)
    zipfile.getInputStream(filename) match {
      case None =>
        Result((StatusCodes.NotFound,RestMessage("metadata file not found")))
      case Some(is) =>
        Result(is)
    }
  }

  /**
   * delete the specified file, the file is relative to the store directory for specified match.
   */
  override
  def delete( id: VId, file: MetaDataFile ): Result[Unit] = StoreIdMeta.resultNotSupported

  /**
   * delete all the metadata files for the match
   */
  override
  def deleteAll( id: VId ): Result[Unit] = StoreIdMeta.resultNotSupported

}

object ZipPersistentSupport {
  def apply[VId, VType <: VersionedInstance[VType, VType, VId]](
      zipfile: ZipFileForStore
  )(
      implicit
      support: StoreSupport[VId, VType],
      execute: ExecutionContext
  ): ZipPersistentSupport[VId, VType] = {
    new ZipPersistentSupport(zipfile)
  }
}

class ZipStore[VId, VType <: VersionedInstance[VType, VType, VId]](
    name: String,
    val zipfile: ZipFileForStore,
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
      new ZipPersistentSupport[VId, VType](zipfile),
      cacheInitialCapacity,
      cacheMaxCapacity,
      cacheTimeToLive,
      cacheTimeToIdle
    ) {}
