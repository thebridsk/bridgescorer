package com.github.thebridsk.bridge.server.backend.resource

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes
import com.github.thebridsk.bridge.data.RestMessage
import scala.concurrent.Promise
import scala.util.Success
import scala.util.Failure
import com.github.thebridsk.utilities.logging.Logger

import Store._
import com.github.thebridsk.bridge.data.VersionedInstance
import com.github.thebridsk.source.SourcePosition
import Implicits._
import scala.reflect.io.File
import java.io.InputStream
import com.github.thebridsk.bridge.server.backend.resource.MetaData.MetaDataFile

object Store {
  val log: Logger = Logger[Store[_, _]]()
}

private class ContextSeq {
  private var seq: Int = 0

  def next: Int = synchronized {
    seq = seq+1
    seq
  }
}

/**
  * Provides an API to a resource store.
  * The resource store consists of a cache and the specified persistent store.
  * The cache is used for all reads, and on cache misses, the persistent store is called.
  * All manipulations of resources are done with [[Future]]s.  The operation returns a
  * [[Future]] to the [[Result]].
  *
  * Change operations on a resource are serialized by using the [[Future]].  The second operation
  * will block on the [[Future]] returned from the first operation.  This [[Future]] is stored in the
  * cache.
  *
  * A read operation will just return the [[Future]] from the cache, if available.  Otherwise a [[Future]]
  * from the persistent store is obtained.
  *
  * Usage:
  *   val store: Store[VId,VType] = ...
  *
  *   val id: VId = ...
  *   val value: VType = ...
  *
  *   store.update(id,value).onComplete { t =>
  *     t match {
  *       case Success( Right( updatedValue )) =>
  *         // the value was successfully updated
  *       case Success( Left((statusCode,msg)) =>
  *         // an error occurred.  statusCode is an HTML statuscode.  msg contains text describing the error.
  *       case Failure( ex ) =>
  *         // an exception occurred during processing.
  *     }
  *   }
  *
  * @param VId the type of the ID used to identify a resource.
  * @param VType the type of the resource object.  This must be a subclass of [[VersionedInstance]]
  *
  * @constructor
  * @param persistent the persistent store
  * @param cacheInitialCapacity
  * @param cacheMaxCapacity
  * @param cacheTimeToLive
  * @param cacheTimeToIdle this value must be less than cacheTimeToLive
  * @param execute an execution context.
  */
abstract class Store[VId <: Comparable[VId], VType <: VersionedInstance[
  VType,
  VType,
  VId
]](
    val name: String,
    val persistent: PersistentSupport[VId, VType],
    val cacheInitialCapacity: Int = 5,
    val cacheMaxCapacity: Int = 100,
    val cacheTimeToLive: Duration = Duration.Inf,
    val cacheTimeToIdle: Duration = Duration.Inf
)(implicit
    execute: ExecutionContext
) extends Resources[VId, VType]
    with StoreListenerManager {

  private val contextSeq = new ContextSeq()

  def idToString(id: VId): String = persistent.support.idSupport.toString(id)

  /** The URI prefix that identifies the resources served by this store */
  val resourceURI = persistent.resourceURI

  implicit private val self: Store[VId, VType] = this

  val metaData: StoreMetaData[VId] = new StoreMetaData[VId] {

    override def listFiles(id: VId): Future[Result[Iterator[MetaDataFile]]] =
      Future { persistent.listFiles(id) }

    override def listFilesFilter(
        id: VId
    )(filter: MetaDataFile => Boolean): Future[Result[Iterator[MetaDataFile]]] =
      Future { persistent.listFilesFilter(id)(filter) }

    override def write(
        id: VId,
        sourceFile: File,
        targetFile: MetaDataFile
    ): Future[Result[Unit]] =
      Future { persistent.write(id, sourceFile, targetFile) }

    override def read(
        id: VId,
        file: MetaDataFile
    ): Future[Result[InputStream]] = Future { persistent.read(id, file) }

    override def delete(id: VId, file: MetaDataFile): Future[Result[Unit]] =
      Future { persistent.delete(id, file) }

    override def deleteAll(id: VId): Future[Result[Unit]] =
      Future { persistent.deleteAll(id) }

  }

  private class CacheResult(
      id: VId,
      result: Option[Future[Result[VType]]],
      deleted: Boolean,
      refresh: Boolean
  ) {

    def this(id: VId, result: Future[Result[VType]]) =
      this(id, Some(result), false, false)

    def this(id: VId, deleted: Boolean, refresh: Boolean) =
      this(id, None, deleted, refresh)

    implicit def toFuture: Future[Result[VType]] = {
      if (refresh) {
        cache
          .refresh(id, () => persistent.getFromPersistent(id))
          .logit(
            s"Store ${name}: Refreshing resource ${resourceURI}/${idToString(id)}"
          )
      } else if (deleted) {
        cache.remove(id)
        persistent.notFound(id).toFuture
      } else {
        result match {
          case Some(f) => f
          case None =>
            persistent.notFound(id).toFuture
        }
      }
    }
  }

  private val cache = new MyCache[VId, Result[VType]](
    cacheInitialCapacity,
    cacheMaxCapacity,
    cacheTimeToLive,
    cacheTimeToIdle
  )

  private def cacheKeys: Set[VId] = cache.keys

  private val cacheLock = new Object

  def support: StoreSupport[VId, VType] = persistent.support

  /**
    * Create a new value in the collection
    * @param newvalue the value for the created resource.
    *                 The 'id' field may be ignored, the [[StoreSupport]] object will determine
    *                 whether the id field is used, or one is generated.
    * @param context the change context for this operation
    * @param pos a [[SourcePosition]] object to identify the caller.
    * @return a future to the new value with updated 'id' field
    */
  def createChild(
      newvalue: VType,
      changeContext: ChangeContext = ChangeContext()
  )(implicit
      pos: SourcePosition
  ): Future[Result[VType]] = {
    createChildImpl(newvalue, false, changeContext)
  }

  /**
    * Import a value into the collection.  The times on the value will NOT be updated.
    * @param newvalue the value for the created resource.
    *                 The 'id' field may be ignored, the [[StoreSupport]] object will determine
    *                 whether the id field is used, or one is generated.
    * @param context the change context for this operation
    * @param pos a [[SourcePosition]] object to identify the caller.
    * @return a future to the new value with updated 'id' field
    */
  def importChild(
      newvalue: VType,
      changeContext: ChangeContext = ChangeContext()
  )(implicit
      pos: SourcePosition
  ): Future[Result[VType]] = {
    createChildImpl(newvalue, true, changeContext)
  }

  /**
    * Import a value into the collection.  The times on the value will NOT be updated.
    * @param newvalue the value for the created resource.
    *                 The 'id' field may be ignored, the [[StoreSupport]] object will determine
    *                 whether the id field is used, or one is generated.
    * @param dontUpdateTimes true and the created, updated times on the value will not be updated.
    * @param context the change context for this operation
    * @param pos a [[SourcePosition]] object to identify the caller.
    * @return a future to the new value with updated 'id' field
    */
  private def createChildImpl(
      newvalue: VType,
      dontUpdateTimes: Boolean = false,
      changeContext: ChangeContext = ChangeContext()
  )(implicit
      pos: SourcePosition
  ): Future[Result[VType]] = {
    log.fine(
      s"Store ${name}: Creating child ${resourceURI}: ${newvalue}, dontUpdateTimes=${dontUpdateTimes} called from ${pos.line}"
    )

    support
      .createInPersistent(newvalue, persistent, dontUpdateTimes)
      .flatMap { r =>
        r match {
          case Right(v) =>
            val f = cache.create(v.id, { () =>
              changeContext.setSeq(contextSeq.next)
              Result.future(v)
            })
            f.map { t =>
              t match {
                case Right(r) =>
                  notify(
                    changeContext
                      .create(r, s"${support.resourceURI}/${idToString(v.id)}")
                  )
                  Result(r)
                case Left(error) =>
                  Result(error)
              }
            }
          case Left(error) =>
            Result.future(error)
        }
      }
      .logit(s"Store ${name}: CreateChild ${resourceURI}")
  }

  def size()(implicit
      pos: SourcePosition
  ): Future[Int] = {

    Future {
      persistent.size()
    }

  }

  /**
    * Read all the Ids in the collection
    */
  def getAllIds()(implicit
      pos: SourcePosition
  ): Future[Result[Set[VId]]] = {
    Future {
      val ids = persistent.getAllIdsFromPersistent()
      Result(ids)
    }
  }

  /**
    * Read all the values in the collection
    * @param pos a [[SourcePosition]] object to identify the caller.
    * @return a future to the resources
    */
  def readAll()(implicit
      pos: SourcePosition
  ): Future[Result[Map[VId, VType]]] = {
    val futures = persistent.getAllIdsFromPersistent().map { id =>
      read(id)
    }

    (Future
      .foldLeft(futures)(Map[VId, VType]()) { (ac, v) =>
        v match {
          case Right(vt) =>
            ac + (vt.id -> vt)
          case Left(error) =>
            log.warning(
              s"Store ${name}: readAll(${resourceURI}) ignoring error ${error}"
            )
            ac
        }
      })
      .map(m => Result(m))
      .logit(s"Store ${name}: ReadAll ${resourceURI}")

  }

  /**
    * Not Implemented
    * Update the collection with new values
    * @param newvalue
    * @param context the change context for this operation
    * @param pos a [[SourcePosition]] object to identify the caller.
    * @return a future to the new values
    */
  def updateAll(
      newvalue: Map[VId, VType],
      changeContext: ChangeContext = ChangeContext()
  )(implicit
      pos: SourcePosition
  ): Future[Result[Map[VId, VType]]] = {
    // not implemented
    Promise
      .successful(
        Result(
          StatusCodes.NotImplemented,
          RestMessage("updateAll is not implemented")
        )
      )
      .future
      .logit(s"Store ${name}: UpdateAll ${resourceURI}")
  }

  /**
    * Not Implemented
    * Delete the collection
    * @param context the change context for this operation
    * @param pos a [[SourcePosition]] object to identify the caller.
    * @return a future to the old values
    */
  def deleteAll(
      changeContext: ChangeContext = ChangeContext()
  )(implicit
      pos: SourcePosition
  ): Future[Result[Map[VId, VType]]] = {
    // not implemented
    Promise
      .successful(
        Result(
          StatusCodes.NotImplemented,
          RestMessage("deleteAll is not implemented")
        )
      )
      .future
      .logit(s"Store ${name}: DeleteAll ${resourceURI}")
  }

  /**
    * Get the value that is cached
    * @param id the ID of the resource
    * @return the cached value.  This will NOT go to the persistent store to obtain the value.
    */
  def getCached(id: VId): Option[Future[Result[VType]]] = {
    cache.get(id)
  }

  /**
    * Read the resource
    * @param id the ID of the resource
    * @param pos a [[SourcePosition]] object to identify the caller.
    * @return a future to the resource
    */
  def read(id: VId)(implicit
      pos: SourcePosition
  ): Future[Result[VType]] = {
    log.fine(
      s"Store ${name}: Reading resource ${resourceURI}/${idToString(id)} called from ${pos.line}"
    )
    val rf = cache.read(
      id,
      { oldv =>
        oldv match {
          case Some(f) =>
            val completed = f.isCompleted
            val ret = if (completed) {
              f.value match {
                case Some(t) =>
                  t match {
                    case Success(r) =>
                      if (persistent.readCheckForDelete(id, r)) {
                        (true, persistent.getFromPersistent(id))
                      } else {
                        (false, f)
                      }
                    case Failure(ex) =>
                      (true, persistent.getFromPersistent(id))
                  }
                case None =>
                  // shouldn't happen, future is complete
                  (false, f)
              }
            } else {
              (false, f)
            }
            ret
          case None =>
            (false, persistent.getFromPersistent(id))
        }
      }
    )
    rf.transform { tryR =>
      tryR match {
        case Success(Right(v)) =>
        case Success(Left(error)) =>
          cache.remove(id)
        case Failure(ex) =>
          cache.remove(id)
      }
      tryR
    }.logit(s"Store ${name}: Read ${resourceURI}/${idToString(id)}")

  }

  /**
    * Update the resource
    * @param id the ID of the resource
    * @param newvalue the new value
    * @param context the change context for this operation
    * @param pos a [[SourcePosition]] object to identify the caller.
    * @return a future to the new value
    */
  def update(
      id: VId,
      newvalue: VType,
      changeContext: ChangeContext = ChangeContext()
  )(implicit
      pos: SourcePosition
  ): Future[Result[VType]] = {
    select(id)
      .update(newvalue, changeContext)
      .logit(s"Store ${name}: Update ${resourceURI}/${idToString(id)}")
  }

  /**
    * Update the resource
    * @param T the type of a helper object to generate the response
    * @param R the type of the response
    * @param id the ID of the resource
    * @param updater a function to update old value to new value
    * @param pos a [[SourcePosition]] object to identify the caller.
    * @return a future to the new value
    */
  def update[T, R](id: VId, updator: Updator[VType, T, R])(implicit
      pos: SourcePosition
  ): Future[Result[R]] = {
    log.fine(
      s"Store ${name}: Updating resource ${resourceURI} with an updator called from ${pos.line}"
    )
    var tt: Option[T] = None
    val rfut = cache.update(
      id,
      { ooldf =>
        updator.setSeq(contextSeq.next)
        val oldf =
          ooldf.get // this can't be None, the third argument gets this value
        log.fine(
          s"Store ${name}: Updating resource ${resourceURI} waiting for old value from ${Implicits
            .toObjectId(oldf)}, complete=${oldf.isCompleted}"
        )
        oldf.flatMap { rold =>
          rold match {
            case Right(oldv) =>
              val upr = updator.updater(oldv)
              upr match {
                case Right((newv, t)) =>
                  tt = Some(t)
                  persistent.putToPersistent(id, newv).map { rnv =>
                    rnv match {
                      case Right(nv) =>
                        updator.prepend(
                          ChangeContext.update(
                            nv,
                            s"$resourceURI/${Resources.vidToString(id)}"
                          )
                        )
                        notify(updator.changeContext)
                        Right(nv)
                      case Left(e) =>
                        Result(e)
                    }
                  }
                case Left(e) =>
                  Result.future(e)
              }
            case Left(e) =>
              Result.future(e)
          }
        }
      },
      Some({ () =>
        log.finest(
          s"Store ${name}: in update for ${resourceURI}/${idToString(id)} reading current value"
        )
        read(id)
      })
    )

    rfut
      .map { r =>
        r match {
          case Right(nv1) =>
            val x = updator.responder(Right((nv1, tt.get)))
            // notify(updator.changeContext)
            x
          case Left(error) =>
            Result(error)
        }
      }
      .logit(s"Store ${name}: update ${resourceURI}/${idToString(id)}")

  }

  /**
    * Delete the resource
    * @param id the ID of the resource
    * @param context the change context for this operation
    * @param pos a [[SourcePosition]] object to identify the caller.
    * @return a future to the old values
    */
  def delete(id: VId, changeContext: ChangeContext = ChangeContext())(implicit
      pos: SourcePosition
  ): Future[Result[VType]] = {

    log.fine(
      s"Store ${name}: deleting resource $resourceURI/${idToString(id)} called from ${pos.line}"
    )

    cache
      .delete(
        id,
        None, // currentValue (get from cache)
        { oldf => // do the delete and return a future to old value
          log.fine(
            s"Store ${name}: Delete in persistent store for resource $resourceURI/${idToString(id)}"
          )
          (oldf match {
            case Some(old) =>
              log.fine(
                s"Store ${name}: Delete in persistent store for resource $resourceURI/${idToString(id)}, waiting for old value"
              )
              old.flatMap { rold =>
                log.fine(
                  s"Store ${name}: Delete in persistent store for resource $resourceURI/${idToString(id)}, old value is ${rold}"
                )
                rold match {
                  case Right(oldv) =>
                    Future.foldLeft(
                      List(
                        persistent.deleteFromPersistent(id, Some(oldv)).map {
                          ro =>
                            changeContext.setSeq(contextSeq.next)
                            ro match {
                              case Right(o) =>
                                notify(
                                  changeContext.delete(
                                    o,
                                    s"$resourceURI/${idToString(id)}"
                                  )
                                )
                                Right(Some(o))
                              case Left(x) => Left(x)
                            }
                        },
                        Future {
                          persistent.deleteAll(id) match {
                            case Right(o) => Right(None)
                            case Left((statuscode, msg))
                                if statuscode == StatusCodes.BadRequest =>
                              Right(None)
                            case Left(x) => Left(x)
                          }
                        }
                      )
                    )(
                      Result[VType](
                        (StatusCodes.InternalServerError, RestMessage(""))
                      )
                    ) { (ac, v) =>
                      v match {
                        case Right(Some(vv)) => Result(vv)
                        case Right(None) =>
                          ac match {
                            case Left((istatus, _))
                                if istatus == StatusCodes.InternalServerError =>
                              Left(
                                (
                                  istatus,
                                  RestMessage(
                                    "Partial success, metadata deleted"
                                  )
                                )
                              )
                            case Right(av) => Result(av)
                            case Left((astatus, amsg)) =>
                              Left(
                                (
                                  astatus,
                                  RestMessage(
                                    s"Partial success, metadata deleted, ${amsg.msg}"
                                  )
                                )
                              )
                          }
                        case Left((status, msg)) =>
                          ac match {
                            case Left((istatus, _))
                                if istatus == StatusCodes.InternalServerError =>
                              Left((status, msg))
                            case Right(av) =>
                              Result(
                                (
                                  status,
                                  RestMessage(
                                    s"Partial success, match deleted, failure: ${msg.msg}"
                                  )
                                )
                              )
                            case Left((astatus, amsg)) =>
                              Result(
                                (
                                  status,
                                  RestMessage(s"${amsg.msg} and ${msg.msg}")
                                )
                              )
                          }
                      }
                    }
                  case Left(e) =>
                    Result.future(e)
                }
              }
            case None =>
              log.fine(
                s"Store ${name}: Delete in persistent store for resource $resourceURI/${idToString(id)}, no old value"
              )
              persistent.deleteFromPersistent(id, None).map { roldv =>
                roldv match {
                  case Right(oldv) =>
                    notify(
                      changeContext
                        .delete(oldv, s"$resourceURI/${idToString(id)}")
                    )
                    Result(oldv)
                  case Left(error) =>
                    Result(error)
                }
              }
          }).logit(
            s"Store ${name}: deleted resource in persistent store $resourceURI/${idToString(id)}"
          )
        },
        { oldf => // return the future that should be added to the cache
          oldf.map { old =>
            persistent.resultAfterDelete(id)
          }
        }
      )
      .logit(s"Store ${name}: delete ${resourceURI}/${idToString(id)}")

  }

  /**
    * Clear the error in the cache for the specified resource.
    * Does nothing if the cache entry does not exist or there is no error.
    * The future in the cache MUST be complete
    * @param id
    * @return true if there was no error or no value
    * false if a failure was cleared.
    * @throws StoreException if future has not completed
    */
  def clearError(id: VId): Boolean = {
    cache.conditionalRemove(
      id,
      { ofroldv =>
        ofroldv match {
          case Some(froldv) =>
            froldv.value match {
              case Some(troldv) =>
                troldv match {
                  case Success(Right(v)) =>
                    log.fine(
                      s"Store ${name}: clearError on ${resourceURI}/${idToString(id)}: no error"
                    )
                    (false, true)
                  case Success(Left(error)) =>
                    log.fine(
                      s"Store ${name}: clearError on ${resourceURI}/${idToString(id)}: cleared ${error}"
                    )
                    (true, false)
                  case Failure(ex) =>
                    log.fine(
                      s"Store ${name}: clearError on ${resourceURI}/${idToString(id)}: cleared failure ${ex}",
                      ex
                    )
                    (true, false)
                }
              case None =>
                log.fine(
                  s"Store ${name}: clearError on ${resourceURI}/${idToString(id)}: future not complete"
                )
                throw new StoreException(
                  s"clearError on ${resourceURI}/${idToString(id)}: future not complete"
                )
            }
          case None =>
            log.fine(
              s"Store ${name}: clearError on ${resourceURI}/${idToString(id)}: no value"
            )
            (false, true)
        }
      }
    )
  }

  def syncStore = new SyncStore(this)
}

class StoreException(msg: String) extends Exception(msg)
