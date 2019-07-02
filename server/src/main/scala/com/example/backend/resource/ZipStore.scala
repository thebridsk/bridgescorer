package com.example.backend.resource

import scala.concurrent.duration._
import akka.http.scaladsl.model.StatusCodes
import com.example.data.RestMessage
import com.example.data.VersionedInstance
import scala.reflect.io.Directory
import utils.logging.Logger
import scala.annotation.tailrec
import java.io.IOException
import scala.collection.JavaConverters._
import ZipStore.log
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.example.data.Id
import java.util.zip.ZipFile
import Implicits._

object ZipStore {
  val log = Logger[ZipStore[_, _]]

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
    val pattern = (s"""store/${resourceName}\\.([^.]+)\\..*""").r

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
      "store/" + resourceName + "." + id + e
    }
  }

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
