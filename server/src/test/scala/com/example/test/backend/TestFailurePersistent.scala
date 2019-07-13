package com.github.thebridsk.bridge.test.backend

import com.github.thebridsk.bridge.data.VersionedInstance
import com.github.thebridsk.bridge.backend.resource.StoreSupport
import com.github.thebridsk.bridge.backend.resource.PersistentSupport
import com.github.thebridsk.bridge.backend.resource.Result
import com.github.thebridsk.utilities.logging.Logger
import TestFailurePersistent.log
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import com.github.thebridsk.bridge.backend.resource.Implicits._
import akka.http.scaladsl.model.StatusCodes
import com.github.thebridsk.bridge.data.Id
import scala.concurrent.duration.Duration
import com.github.thebridsk.bridge.backend.resource.Store
import com.github.thebridsk.bridge.backend.resource.InMemoryPersistent

object TestFailurePersistent {

  def apply[VId,VType <: VersionedInstance[VType,VType,VId]]()(
                    implicit
                      support: StoreSupport[VId, VType],
                      execution: ExecutionContext
                  ) = {
    new TestFailurePersistent[VId,VType]
  }

  val log = Logger[TestFailurePersistent[_,_]]
}

class TestFailurePersistent[VId,VType <: VersionedInstance[VType,VType,VId]](
    implicit
      support: StoreSupport[VId, VType],
      execution: ExecutionContext
  ) extends InMemoryPersistent[VId,VType] {

  var failGetAllIds: Boolean = false
  var failRead: Boolean = false
  var failWrite: Boolean = false
  var failDelete: Boolean = false

  var failResultRead: Option[Result[VType]] = None
  var failResultWrite: Option[Result[VType]] = None
  var failResultDelete: Option[Result[VType]] = None

  /**
   * Get all the IDs from persistent storage
   */
  override
  def getAllIdsFromPersistent(): Set[VId] = {
    log.fine(s"""getAllIdsFromPersistent failGetAllIds=${failGetAllIds}""")
    if (failGetAllIds) throw new Exception("Failure in persistent store!")
    super.getAllIdsFromPersistent()
  }

  /**
   * Create an entry in the persistent store.
   * @param useId use this Id if specified.
   * @param v the value to create.  The id field is ignored and will be assigned.
   * @return a future to the stored value, with the correct ID.
   */
  override
  def createInPersistent(
                          useId: Option[VId],
                          v: VType,
                          dontUpdateTimes: Boolean = false
                        ): Future[Result[VType]] = {
    log.fine(s"""createInPersistent failWrite=${failWrite}, failResultWrite=${failResultWrite}, useId=${useId}, v=${v}""")
    if (failWrite) {
      Future {
        throw new Exception("Failure writing to persistent store!")
      }
    } else if (failResultWrite.isDefined) {
      failResultWrite.get.toFuture
    } else {
      super.createInPersistent(useId, v, dontUpdateTimes)
    }
  }

  /**
   * Read a resource from the persistent store
   * @param id
   * @return the result containing the resource or an error
   */
  override
  def getFromPersistent(
                         id: VId
                       ): Future[Result[VType]] = {
    log.fine(s"""getFromPersistent failRead=${failRead}, failResultRead=${failResultRead}, id=${id}""")
    if (failRead) {
      Future {
        throw new Exception("Failure reading from persistent store!")
      }
    } else if (failResultRead.isDefined) {
      failResultRead.get.toFuture
    } else {
      super.getFromPersistent(id)
    }
  }

  /**
   * Write a resource to the persistent store
   * @param id
   * @param v
   * @return the result containing the resource or an error
   */
  override
  def putToPersistent(
                       id: VId,
                       v: VType
                     ): Future[Result[VType]] = {
    log.fine(s"""putToPersistent failWrite=${failWrite}, failResultWrite=${failResultWrite}, id=${id}, v=${v}""")
    if (failWrite) Future {
      Thread.sleep(1000)
      throw new Exception("Failure writing to persistent store!")
    } else if (failResultWrite.isDefined) {
      failResultWrite.get.toFuture
    } else {
      super.putToPersistent(id, v)
    }
  }

  /**
   * Write a resource to the persistent store
   * @param id
   * @param v
   * @return the result containing the old resource or an error
   */
  override
  def deleteFromPersistent(
                       id: VId,
                       cacheValue: Option[VType]
                     ): Future[Result[VType]] =
  {
    log.fine(s"""deleteFromPersistent failDelete=${failDelete}, failResultDelete=${failResultDelete}, id=${id}, cacheValue=${cacheValue}""")
    Future {
      if (failDelete) {
        throw new Exception("Failure deleting from persistent store!")
      } else if (failResultDelete.isDefined) {
        failResultDelete
      } else {
        if (cacheValue.isEmpty && failRead) {
          throw new Exception("Failure reading from persistent store!")
        } else if (cacheValue.isEmpty && failResultRead.isDefined) {
          failResultRead
        } else {
          None
        }
      }
    }.flatMap { o =>
      o match {
        case Some(r) => r.toFuture
        case None =>
          super.deleteFromPersistent(id, cacheValue)
      }
    }
  }

}

class TestFailureStore[VId,VType <: VersionedInstance[VType,VType,VId]] private (
                    name: String,
                    val testFailurePersistent: TestFailurePersistent[VId,VType],
                    cacheInitialCapacity: Int = 5,
                    cacheMaxCapacity: Int = 100,
                    cacheTimeToLive: Duration = Duration.Inf,
                    cacheTimeToIdle: Duration = Duration.Inf,
                  )(
                    implicit
                      cachesupport: StoreSupport[VId,VType],
                      execute: ExecutionContext
                  ) extends Store[VId,VType]( name,
                                              testFailurePersistent,
                                              cacheInitialCapacity,
                                              cacheMaxCapacity,
                                              cacheTimeToLive,
                                              cacheTimeToIdle
                                            ) {
}

object TestFailureStore {
  def apply[VId,VType <: VersionedInstance[VType,VType,VId]](
                    name: String,
                    testFailurePersistent: TestFailurePersistent[VId,VType] = null,
                    cacheInitialCapacity: Int = 5,
                    cacheMaxCapacity: Int = 100,
                    cacheTimeToLive: Duration = Duration.Inf,
                    cacheTimeToIdle: Duration = Duration.Inf,
                  )(
                    implicit
                      cachesupport: StoreSupport[VId,VType],
                      execute: ExecutionContext
                  ) = {
    new TestFailureStore(name, Option(testFailurePersistent).getOrElse( TestFailurePersistent[VId,VType]()), cacheInitialCapacity, cacheMaxCapacity, cacheTimeToLive, cacheTimeToIdle )
  }
}
