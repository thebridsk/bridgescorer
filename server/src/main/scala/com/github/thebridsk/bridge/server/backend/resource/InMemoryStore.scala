package com.github.thebridsk.bridge.server.backend.resource

import com.github.thebridsk.bridge.data.VersionedInstance
import akka.http.scaladsl.model.StatusCodes
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.utilities.logging.Logger
import scala.concurrent.duration._

import InMemoryStore.log
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import Implicits._
import com.github.thebridsk.bridge.data.Id

class InMemoryPersistent[VId, VType <: VersionedInstance[VType, VType, VId]](
    implicit
    support: StoreSupport[VId, VType],
    execute: ExecutionContext
) extends PersistentSupport[VId, VType] {

  self =>

  val valuesInPersistent = {
    import collection.JavaConverters._
    new java.util.concurrent.ConcurrentHashMap[VId, VType]().asScala
  }

  val deletedKeys = collection.mutable.Set[VId]()

  def clearPersistent = valuesInPersistent.clear()
  def add(v: VType) = {
    if (!support.isIdFromValue) addId(v.id)
    valuesInPersistent += v.id -> v
  }
  def internalAdd(v: VType) = {
    valuesInPersistent += v.id -> v
  }
  def get(id: VId) = valuesInPersistent.get(id)
  def remove(id: VId) = {
    deletedKeys += id
    valuesInPersistent.remove(id)
  }

  /**
    * Get all the IDs from persistent storage
    */
  def getAllIdsFromPersistent(): Set[VId] = {
    self.synchronized {
      log.fine(s"""getAllIdsFromPersistent""")
      valuesInPersistent.keySet.toSet ++ deletedKeys
    }
  }

  /**
    * Create an entry in the persistent store.
    * @param useId use this Id if specified.
    * @param v the value to create.  The id field is ignored and will be assigned.
    * @return a future to the stored value, with the correct ID.
    */
  override def createInPersistent(
      useId: Option[VId],
      v: VType,
      dontUpdateTimes: Boolean = false
  ): Future[Result[VType]] = {
    log.fine(s"""createInPersistent useId=${useId}, v=${v}""")
    Future {
      self.synchronized {
        useId match {
          case Some(id) =>
            val nv = v.setId(id, true, dontUpdateTimes).readyForWrite()
            add(nv)
            Result(nv)
          case None =>
            generateNextId(v) match {
              case Right(id) =>
                val nv = v.setId(id, true, false)
                add(nv)
                Result(nv)
              case Left(error) =>
                Result(error)
            }
        }
      }
    }.logit("createInPersistent")
  }

  /**
    * Read a resource from the persistent store
    * @param id
    * @return the result containing the resource or an error
    */
  override def getFromPersistent(
      id: VId
  ): Future[Result[VType]] = {
    log.fine(s"""getFromPersistent id=${id}""")
    Future {
      get(id) match {
        case Some(v) => Result(v)
        case None    => notFound(id)
      }
    }
  }

  /**
    * Write a resource to the persistent store
    * @param id
    * @param v
    * @return the result containing the resource or an error
    */
  override def putToPersistent(
      id: VId,
      v: VType
  ): Future[Result[VType]] = {
    log.fine(s"""putToPersistent id=${id}, v=${v}""")
    Future {
      val nv = v.setId(id, false, false).readyForWrite()
      internalAdd(nv)
      Result(nv)
    }
  }

  /**
    * Write a resource to the persistent store
    * @param id
    * @param v
    * @return the result containing the old resource or an error
    */
  override def deleteFromPersistent(
      id: VId,
      cacheValue: Option[VType]
  ): Future[Result[VType]] = {
    log.fine(s"""deleteFromPersistent id=${id}, cacheValue=${cacheValue}""")
    Future {
      self.synchronized {
        cacheValue match {
          case Some(ov) =>
            remove(id)
            Result(ov)
          case None =>
            remove(id) match {
              case Some(ov) =>
                Result(ov)
              case None =>
                notFound(id)
            }
        }
      }
    }
  }

}

object InMemoryPersistent {
  def apply[VId, VType <: VersionedInstance[VType, VType, VId]](
      implicit
      support: StoreSupport[VId, VType],
      execute: ExecutionContext
  ): InMemoryPersistent[VId, VType] = {
    new InMemoryPersistent
  }
}

class InMemoryStore[VId, VType <: VersionedInstance[VType, VType, VId]](
    name: String,
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
      new InMemoryPersistent[VId, VType],
      cacheInitialCapacity,
      cacheMaxCapacity,
      cacheTimeToLive,
      cacheTimeToIdle
    ) {}

object InMemoryStore {

  val log = Logger[InMemoryStore[_, _]]

  def apply[VId, VType <: VersionedInstance[VType, VType, VId]](
      name: String,
      cacheInitialCapacity: Int = 5,
      cacheMaxCapacity: Int = 100,
      cacheTimeToLive: Duration = 10.minutes,
      cacheTimeToIdle: Duration = 9.minutes
  )(
      implicit
      cachesupport: StoreSupport[VId, VType],
      execute: ExecutionContext
  ): InMemoryStore[VId, VType] = {
    new InMemoryStore(
      name,
      cacheInitialCapacity,
      cacheMaxCapacity,
      cacheTimeToLive,
      cacheTimeToIdle
    )
  }
}
