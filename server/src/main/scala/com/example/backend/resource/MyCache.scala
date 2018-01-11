package com.example.backend.resource

import scala.concurrent.Future
import utils.logging.Logger
import scala.concurrent.duration._

class CreateKeyFailed[K]( result: Result[K] ) extends Exception

object MyCache {
  val log = Logger[MyCache[_,_]]
}

import MyCache._
import akka.http.caching.scaladsl.CachingSettings
import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.Cache

/**
 * @param cacheInitialCapacity
 * @param cacheMaxCapacity
 * @param cacheTimeToLive
 * @param cacheTimeToIdle this value must be less than cacheTimeToLive
 */
class MyCache[K,V](
               val cacheInitialCapacity: Int = 5,
               val cacheMaxCapacity: Int = 100,
               val cacheTimeToLive: Duration = Duration.Inf,
               val cacheTimeToIdle: Duration = Duration.Inf,
             ) {

  val defaultCachingSettings = CachingSettings("{}")
  val lfuCacheSettings =
    defaultCachingSettings.lfuCacheSettings
      .withInitialCapacity(cacheInitialCapacity)
      .withMaxCapacity(cacheMaxCapacity)
      .withTimeToLive(cacheTimeToLive)
      .withTimeToIdle(cacheTimeToIdle)
  val cachingSettings = defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)

  val lfuCache: Cache[K,V] = LfuCache(cachingSettings)

  def get(key: K): Option[Future[V]] = lfuCache.get(key)

  /**
   * @param key
   * @param checkCurrent a function that gets the current value in the cache,
   * and returns a tuple2.  The first value is whether to refresh the value or not,
   * the second value is a future to the value.   If refresh is false, then
   * the returned value must be the future from the cached value.  This function executes
   * while holding the cache lock, it must not block.
   */
  def read( key: K,
            checkCurrent: Option[Future[V]]=>(Boolean,Future[V])
          ): Future[V] = {
    synchronized {
      val oldv = lfuCache.get(key)
      val (refresh, future) = checkCurrent( oldv )
      if (refresh) {
        lfuCache.remove(key)
        lfuCache.apply(key,()=>future)
      } else {
        if (oldv.isEmpty) lfuCache.apply(key, ()=>future)
        else future
      }
    }
  }

  /**
   * Refresh the value in the cache
   * @param key
   * @param block to obtain the new value
   */
  def refresh(key: K, block: () => Future[V] ): Future[V] = synchronized {
    lfuCache.remove(key)
    lfuCache.apply(key,block)
  }

  def keys: Set[K] = lfuCache.keys

  def remove( key: K ): Unit = lfuCache.remove(key)

  /**
   * @param key
   * @param condition a function that gets the current value in the cache, must return a tuple2 of
   * two Booleans.  The first is true if the entry should be deleted, false if the entry can remain.
   * The second is the return code.  This function MUST not block.
   * @return the second boolean from the return of condition
   */
  def conditionalRemove( key: K, condition: Option[Future[V]]=>(Boolean,Boolean) ): Boolean = synchronized {
    val (delete, ret) = condition( lfuCache.get(key))
    if (delete) lfuCache.remove(key)
    ret
  }

  /**
   * Update the cache with a new value
   * @param key the key of the entry to update
   * @param generateNewValue a function that returns the new value.  it has one argument, the current value in the cache.
   * @param currentValue the current value to pass to generateNewValue function.  If none, then the value in the cache is used.
   * @return a future the new value
   */
  def update( key: K, generateNewValue: Option[Future[V]]=>Future[V], currentValue: Option[()=>Future[V]] = None ): Future[V] = {
    synchronized {
      log.finer(s"Cache update ${key} starting")
      val oldv = currentValue.map( f => f()).orElse(lfuCache.get(key))
      oldv match {
        case Some(f) =>
          log.finer(s"Cache update ${key} got future to current value ${Implicits.toObjectId(f)}")
        case None =>
          log.finer(s"Cache update ${key} did not get a future to current value")
      }
      lfuCache.remove(key)
      log.finer(s"Cache update ${key} removed old value from cache")
      val r = lfuCache.apply( key, ()=> generateNewValue(oldv) )
      log.finer(s"Cache update ${key} added new future to cache")
      r
    }
  }

  /**
   * Delete a value
   * @param key the key of the entry to delete
   * @param currentValue the current value to return as the old value.  If none, then the value in the cache is used.
   * @param futureReq a function that gets the old value and returns the value that should be returned for future requests
   * @return a future the old value
   */
  def delete(
              key: K,
              currentValue: Option[()=>Future[V]],
              deleteFun: Option[Future[V]]=>Future[V],
              futureReq: Future[V]=>Future[V]
            ): Future[V] = {
    synchronized {
      val oldv = currentValue.map( f => f()).orElse(lfuCache.get(key))
      lfuCache.remove(key)
      val oldf = deleteFun(oldv)
      val f = futureReq( oldf )
      lfuCache.apply(key,()=>f)
      oldf
    }
  }

  def create( key: K, block: ()=>Future[V] ): Future[V] = {
    synchronized {
      lfuCache.apply(key, block)
    }
  }
}
