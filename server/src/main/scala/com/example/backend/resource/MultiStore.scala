package com.github.thebridsk.bridge.backend.resource

import scala.concurrent.duration._
import akka.http.scaladsl.model.StatusCodes
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.data.VersionedInstance
import scala.annotation.tailrec
import scala.reflect.io.Directory
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import com.github.thebridsk.bridge.backend.resource.Implicits._
import scala.util.Success
import scala.util.Failure
import com.github.thebridsk.utilities.logging.Logger

class MultiPersistentSupport[VId, VType <: VersionedInstance[VType, VType, VId]](
    val persistentStores: List[PersistentSupport[VId, VType]]
)(
    implicit
    support: StoreSupport[VId, VType],
    execute: ExecutionContext
) extends PersistentSupport[VId, VType] {

  self =>

  import MultiPersistentSupport.log

  /**
    * Get all the IDs from persistent storage
    */
  def getAllIdsFromPersistent(): Set[VId] = {
    persistentStores
      .map { ps =>
        ps.getAllIdsFromPersistent()
      }
      .foldLeft(Set[VId]())((ac, s) => ac ++ s)
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
    persistentStores.headOption
      .map { ps =>
        useId match {
          case Some(id) =>
            val found = self.synchronized {
              getAllIdsFromPersistent().find(p => p == id)
            }
            if (found.isEmpty) {
              persistentStores.head.createInPersistent(
                useId,
                v,
                dontUpdateTimes
              )
            } else {
              alreadyExists(id)
            }
          case None =>
            persistentStores.head.createInPersistent(useId, v, dontUpdateTimes)
        }
      }
      .getOrElse {
        log.warning(
          s"No persistent stores defined for $resourceURI/$useId, trying to create $v"
        )
        internalError.toFuture
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

    @tailrec
    def get(
        list: List[PersistentSupport[VId, VType]],
        future: Future[Result[VType]]
    ): Future[Result[VType]] = {
      if (list.isEmpty) {
        future.transform { t =>
          t.map { r =>
            if (r.isOk) r
            else notFound(id)
          }
        }
      } else {
        val nf = future.transformWith { t =>
          t match {
            case Success(Right(v)) =>
              Result.future(v)
            case Success(Left(error)) =>
              val ps = list.head
              ps.getFromPersistent(id)
            case Failure(ex) =>
              val ps = list.head
              ps.getFromPersistent(id)
          }
        }

        get(list.tail, nf)

      }
    }

    get(persistentStores, notFound(id).toFuture)

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
    persistentStores.headOption
      .map { ps =>
        ps.putToPersistent(id, v)
      }
      .getOrElse {
        log.warning(s"No persistent stores defined for $resourceURI/$id")
        internalError.toFuture
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
    persistentStores.headOption
      .map { ps =>
        ps.deleteFromPersistent(id, cacheValue)
      }
      .getOrElse {
        log.warning(s"No persistent stores defined for $resourceURI/$id")
        internalError.toFuture
      }
  }

  def retryAfterDeleteMsg(id: VId) =
    s"Resource $resourceURI/$id not found, retry"

  /**
    * The response to put in the cache after deleting a value.  This is to ensure that the delete completes before the next operation starts.
    * @param id
    */
  override def resultAfterDelete(id: VId): Result[VType] =
    Result(StatusCodes.RetryWith, RestMessage(retryAfterDeleteMsg(id)))

  /**
    * The response to put in the cache after deleting a value.  This is to ensure that the delete completes before the next operation starts.
    * @param id
    * @param cachedValue the value in the cache at the start of the operation
    * @return true means read it again.  false means value is ok
    */
  override def readCheckForDelete(id: VId, cachedValue: Result[VType]) =
    cachedValue match {
      case Left((statuscode, RestMessage(msg)))
          if statuscode == StatusCodes.RetryWith && msg == retryAfterDeleteMsg(
            id
          ) =>
        true
      case _ => false
    }

}

object MultiPersistentSupport {

  val log = Logger[MultiPersistentSupport[_, _]]

  /**
    * Create a persistent support object that is backed by a file persistent support and java resource persistent support.
    * The file persistent support is the primary, and all created resources are written to the file persistent support.
    * @param directory the directory for the file persistent support
    * @param resourcedirectory the resource directory for the java resource persistent.  Must end in '/'
    * @param masterfile the master file for the java resource persistent suppport.
    */
  def createFileAndResource[VId, VType <: VersionedInstance[VType, VType, VId]](
      directory: Directory,
      resourcedirectory: String,
      masterfile: String,
      loader: ClassLoader
  )(
      implicit
      cachesupport: StoreSupport[VId, VType],
      execute: ExecutionContext
  ): MultiPersistentSupport[VId, VType] = {
    apply(
      new FilePersistentSupport[VId, VType](directory),
      new JavaResourcePersistentSupport[VId, VType](
        resourcedirectory,
        masterfile,
        loader
      )
    )
  }

  /**
    * Create a persistent support object that is backed by a file persistent support and java resource persistent support.
    * The file persistent support is the primary, and all created resources are written to the file persistent support.
    * @param directory the directory for the file persistent support
    * @param resourcedirectory the resource directory for the java resource persistent.  Must end in '/'
    * @param masterfile the master file for the java resource persistent suppport.
    */
  def createInMemoryAndResource[VId, VType <: VersionedInstance[
    VType,
    VType,
    VId
  ]](
      resourcedirectory: String,
      masterfile: String,
      loader: ClassLoader
  )(
      implicit
      cachesupport: StoreSupport[VId, VType],
      execute: ExecutionContext
  ): MultiPersistentSupport[VId, VType] = {
    apply(
      new InMemoryPersistent[VId, VType](),
      new JavaResourcePersistentSupport[VId, VType](
        resourcedirectory,
        masterfile,
        loader
      )
    )
  }

  def apply[VId, VType <: VersionedInstance[VType, VType, VId]](
      persistents: PersistentSupport[VId, VType]*
  )(
      implicit
      cachesupport: StoreSupport[VId, VType],
      execute: ExecutionContext
  ): MultiPersistentSupport[VId, VType] = {
    new MultiPersistentSupport[VId, VType](persistents.toList)
  }

}

class MultiStore[VId, VType <: VersionedInstance[VType, VType, VId]](
    name: String,
    val persistentStores: MultiPersistentSupport[VId, VType],
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
      persistentStores,
      cacheInitialCapacity,
      cacheMaxCapacity,
      cacheTimeToLive,
      cacheTimeToIdle
    ) {}

object MultiStore {

  /**
    * Create a MultiStore object that is backed by a file persistent support and java resource persistent support.
    * The file persistent support is the primary, and all created resources are written to the file persistent support.
    * @param directory the directory for the file persistent support
    * @param resourcedirectory the resource directory for the java resource persistent.  Must end in '/'
    * @param masterfile the master file for the java resource persistent suppport.
    */
  def createFileAndResource[VId, VType <: VersionedInstance[VType, VType, VId]](
      name: String,
      directory: Directory,
      resourcedirectory: String,
      masterfile: String,
      loader: ClassLoader,
      cacheInitialCapacity: Int = 5,
      cacheMaxCapacity: Int = 100,
      cacheTimeToLive: Duration = 10.minutes,
      cacheTimeToIdle: Duration = 9.minutes
  )(
      implicit
      cachesupport: StoreSupport[VId, VType],
      execute: ExecutionContext
  ): MultiStore[VId, VType] = {
    val p = MultiPersistentSupport.createFileAndResource(
      directory,
      resourcedirectory,
      masterfile,
      loader
    )
    new MultiStore[VId, VType](
      name,
      p,
      cacheInitialCapacity,
      cacheMaxCapacity,
      cacheTimeToLive,
      cacheTimeToIdle
    )
  }

  /**
    * Create a MultiStore object that is backed by a file persistent support and java resource persistent support.
    * The file persistent support is the primary, and all created resources are written to the file persistent support.
    * @param directory the directory for the file persistent support
    * @param resourcedirectory the resource directory for the java resource persistent.  Must end in '/'
    * @param masterfile the master file for the java resource persistent suppport.
    */
  def createInMemoryAndResource[VId, VType <: VersionedInstance[
    VType,
    VType,
    VId
  ]](
      name: String,
      resourcedirectory: String,
      masterfile: String,
      loader: ClassLoader,
      cacheInitialCapacity: Int = 5,
      cacheMaxCapacity: Int = 100,
      cacheTimeToLive: Duration = Duration.Inf,
      cacheTimeToIdle: Duration = Duration.Inf
  )(
      implicit
      cachesupport: StoreSupport[VId, VType],
      execute: ExecutionContext
  ): MultiStore[VId, VType] = {
    val p = MultiPersistentSupport.createInMemoryAndResource(
      resourcedirectory,
      masterfile,
      loader
    )
    new MultiStore[VId, VType](
      name,
      p,
      cacheInitialCapacity,
      cacheMaxCapacity,
      cacheTimeToLive,
      cacheTimeToIdle
    )
  }

  def apply[VId, VType <: VersionedInstance[VType, VType, VId]](
      name: String,
      persistents: List[PersistentSupport[VId, VType]],
      cacheInitialCapacity: Int = 5,
      cacheMaxCapacity: Int = 100,
      cacheTimeToLive: Duration = 10.minutes,
      cacheTimeToIdle: Duration = 9.minutes
  )(
      implicit
      cachesupport: StoreSupport[VId, VType],
      execute: ExecutionContext
  ): MultiStore[VId, VType] = {
    val p = MultiPersistentSupport(persistents: _*)
    new MultiStore[VId, VType](
      name,
      p,
      cacheInitialCapacity,
      cacheMaxCapacity,
      cacheTimeToLive,
      cacheTimeToIdle
    )
  }

}
