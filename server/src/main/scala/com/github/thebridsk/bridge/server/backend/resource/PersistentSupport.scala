package com.github.thebridsk.bridge.server.backend.resource

import akka.http.scaladsl.model.StatusCodes
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.data.VersionedInstance
import scala.concurrent.Future
import scala.reflect.io.File
import java.io.InputStream


/**
 * @constructor
 * @param id
 * @param timestamp A timestamp with the format "yyyy-MM-dd-HH-mm-ss"
 * @param persistentMeta A string defined by the persistentSupport instantance
 */
case class StoreIdMeta[VId]( id: VId, timestamp: String, persistentMeta: String, persistentMeta2: String )

object StoreIdMeta {

  val timestampFormat = "yyyy-MM-dd-HH-mm-ss"

  val resultNotSupported: Result[Nothing] = Result(StatusCodes.BadRequest, RestMessage("Metadata is not supported for store"))

  val notSupported: Future[Result[Nothing]] = Future.successful( resultNotSupported )

}

object MetaData {

  /**
   * The filename
   */
  type MetaDataFile = String

}

import MetaData._

trait MetaData[VId] {

  /**
   * List all the files for the specified match, all returned filenames are relative to the store directory for specified match.
   * To read the file, the read method must be used on this object.
   */
  def listFiles( id: VId ): Result[Iterator[MetaDataFile]] = StoreIdMeta.resultNotSupported

  /**
   * List all the files for the specified match that match the filter, all returned filenames are relative to the store directory for specified match.
   * To read the file, the read method must be used on this object.
   */
  def listFilesFilter( id: VId )( filter: MetaDataFile=>Boolean ): Result[Iterator[MetaDataFile]] = StoreIdMeta.resultNotSupported

  /**
   * Write the specified source file to the target file, the target file is relative to the store directory for specified match.
   */
  def write( id: VId, sourceFile: File, targetFile: MetaDataFile ): Result[Unit] = StoreIdMeta.resultNotSupported

  /**
   * Write the specified source file to the target file, the target file is relative to the store directory for specified match.
   */
  def write( id: VId, source: InputStream, targetFile: MetaDataFile ): Result[Unit] = StoreIdMeta.resultNotSupported

  /**
   * read the specified file, the file is relative to the store directory for specified match.
   */
  def read( id: VId, file: MetaDataFile ): Result[InputStream] = StoreIdMeta.resultNotSupported

  /**
   * delete the specified file, the file is relative to the store directory for specified match.
   */
  def delete( id: VId, file: MetaDataFile ): Result[Unit] = StoreIdMeta.resultNotSupported

  /**
   * delete all the metadata files for the match
   */
  def deleteAll( id: VId ): Result[Unit] = StoreIdMeta.resultNotSupported

}

trait StoreMetaData[VId] {

  /**
   * List all the files for the specified match, all returned filenames are relative to the store directory for specified match.
   * To read the file, the read method must be used on this object.
   */
  def listFiles( id: VId ): Future[Result[Iterator[MetaDataFile]]] = StoreIdMeta.notSupported

  /**
   * List all the files for the specified match that match the filter, all returned filenames are relative to the store directory for specified match.
   * To read the file, the read method must be used on this object.
   */
  def listFilesFilter( id: VId )( filter: MetaDataFile=>Boolean ): Future[Result[Iterator[MetaDataFile]]] = StoreIdMeta.notSupported

  /**
   * Write the specified source file to the target file, the target file is relative to the store directory for specified match.
   */
  def write( id: VId, sourceFile: File, targetFile: MetaDataFile ): Future[Result[Unit]] = StoreIdMeta.notSupported

  /**
   * read the specified file, the file is relative to the store directory for specified match.
   */
  def read( id: VId, file: MetaDataFile ): Future[Result[InputStream]] = StoreIdMeta.notSupported

  /**
   * delete the specified file, the file is relative to the store directory for specified match.
   */
  def delete( id: VId, file: MetaDataFile ): Future[Result[Unit]] = StoreIdMeta.notSupported

  /**
   * delete all the metadata files for the match
   */
  def deleteAll( id: VId ): Future[Result[Unit]] = StoreIdMeta.notSupported

}

abstract class PersistentSupport[
    VId <: Comparable[VId],
    VType <: VersionedInstance[VType, VType, VId]
](
    implicit
    val support: StoreSupport[VId, VType]
) extends MetaData[VId] {

  val resourceURI: String = support.resourceURI

  private var maxId: Option[VId] = None

  /**
    * Get all the IDs from persistent storage
    */
  def getAllIdsFromPersistent(): Set[VId]

  /**
    * Get all the IDs from persistent storage
    */
  def getOrderedListOfIds(): Seq[StoreIdMeta[VId]] = Nil

  /**
    * Returns the number of matches in this store
    */
  def size(): Int = getAllIdsFromPersistent().size

  /**
    * Generate the next ID for a new instance
    * @param v the new instance that will be created
    * @param persistent
    * @return the next ID
    */
  def generateNextId(v: VType): Result[VId] = synchronized {
    import Implicits._

    val id = maxId.orElse {
      val current = getAllIdsFromPersistent()
      current.foldLeft(PersistentSupport.zero[VId]) { (ac, vid) =>
        val mm = ac.map(
          m => if (support.idSupport.compare( m, vid ) < 0) vid else m
        )
        mm.orElse(Some(vid))
      }
    }

    id.map { i =>
        val n = support.idSupport.toNumber(i)
        val nid = support.idSupport.toId(n+1)
        maxId = Some(nid)
        Result(nid)
      }
      .getOrElse(Result(support.idSupport.toId(1)))
      .logit("generateNextId")
  }

  /**
    * Add a new ID to the store, this add is done out of band.
    * @param id
    */
  def addId(id: VId): Unit = synchronized {
    val curMaxId = maxId.orElse {
      val current = getAllIdsFromPersistent()
      current.foldLeft(PersistentSupport.zero[VId]) { (ac, vid) =>
        val mm = ac.map(
          m => if (support.idSupport.compare(m, vid) < 0) vid else m
        )
        mm.orElse(Some(vid))
      }
    }
    val newMax = curMaxId match {
      case Some(curId) =>
        if (support.idSupport.compare(curId, id) < 0) id else curId
      case None =>
        id
    }
    maxId = Some(newMax)
  }

  /**
    * Create an entry in the persistent store.
    * @param useId use this Id if specified.
    * @param v the value to create.  The id field is ignored and will be assigned.
    * @return a future to the stored value, with the correct ID.
    */
  def createInPersistent(
      useId: Option[VId],
      v: VType,
      dontUpdateTimes: Boolean = false
  ): Future[Result[VType]]

  /**
    * Read a resource from the persistent store
    * @param id
    * @return the result containing the resource or an error
    */
  def getFromPersistent(
      id: VId
  ): Future[Result[VType]]

  /**
    * Write a resource to the persistent store
    * @param id
    * @param v
    * @return the result containing the resource or an error
    */
  def putToPersistent(
      id: VId,
      v: VType
  ): Future[Result[VType]]

  /**
    * Write a resource to the persistent store
    * @param id
    * @param v
    * @return the result containing the old resource or an error
    */
  def deleteFromPersistent(
      id: VId,
      cacheValue: Option[VType]
  ): Future[Result[VType]]

  /**
    * The response to put in the cache after deleting a value.  This is to ensure that the delete completes before the next operation starts.
    * @param id
    */
  def resultAfterDelete(id: VId): Result[VType] = notFound(id)

  /**
    * The response to put in the cache after deleting a value.  This is to ensure that the delete completes before the next operation starts.
    * @param id
    * @param cachedValue the value in the cache at the start of the operation
    * @return true means read it again.  false means value is ok
    */
  def readCheckForDelete(id: VId, cachedValue: Result[VType]) = false

  def notFound(id: VId): Result[Nothing] =
    Result(StatusCodes.NotFound, s"Did not find resource $resourceURI/${support.idSupport.toString(id)}")
  def internalError: Result[Nothing] =
    Result(StatusCodes.InternalServerError, RestMessage("Internal error"))
  def storeIsReadOnly: Future[Result[Nothing]] =
    Result.future(StatusCodes.BadRequest, RestMessage("Store is read only"))
  def alreadyExists(id: VId): Future[Result[Nothing]] =
    Result.future(
      StatusCodes.BadRequest,
      s"Resource already exists ${resourceURI}/${id}"
    )
}

object PersistentSupport {
  def zero[VId]: Option[VId] = None
}
