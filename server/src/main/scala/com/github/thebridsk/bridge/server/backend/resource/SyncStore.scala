package com.github.thebridsk.bridge.server.backend.resource

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.data.VersionedInstance

class SyncStore[VId, VType <: VersionedInstance[VType, VType, VId]](
    store: Store[VId, VType]
) {

  val resourceURI = store.resourceURI

  def support: StoreSupport[VId, VType] = store.support

  private def await[T](timeout: Duration)(fut: Future[T]) =
    Await.result(fut, timeout)

  val defaultTimeout = 30.seconds

  /**
    * Create a new value in the collection
    * @param the new value, the 'id' field will be ignored
    * @param timeout the timeout for the operation.  default 30 seconds.
    * @param context the change context for this operation
    * @return a future to the new value with updated 'id' field
    */
  def createChild(
      newvalue: VType,
      timeout: Duration = defaultTimeout,
      changeContext: ChangeContext = ChangeContext()
  ): Result[VType] = {
    await(timeout)(store.createChild(newvalue, changeContext))
  }

  /**
    * Read all the values in the collection
    * @param timeout the timeout for the operation.  default 30 seconds.
    * @return a future to the resources
    */
  def readAll(timeout: Duration = defaultTimeout): Result[Map[VId, VType]] = {
    await(timeout)(store.readAll())
  }

  /**
    * Not Implemented
    * Update the collection with new values
    * @param newvalue
    * @param timeout the timeout for the operation.  default 30 seconds.
    * @param context the change context for this operation
    * @return a future to the new values
    */
  def updateAll(
      newvalue: Map[VId, VType],
      timeout: Duration = defaultTimeout,
      changeContext: ChangeContext = ChangeContext()
  ): Result[Map[VId, VType]] = {
    await(timeout)(store.updateAll(newvalue, changeContext))
  }

  /**
    * Not Implemented
    * Delete the collection
    * @param timeout the timeout for the operation.  default 30 seconds.
    * @param context the change context for this operation
    * @return a future to the old values
    */
  def deleteAll(
      timeout: Duration = defaultTimeout,
      changeContext: ChangeContext = ChangeContext()
  ): Result[Map[VId, VType]] = {
    await(timeout)(store.deleteAll(changeContext))
  }

  /**
    * Read the resource
    * @param id the ID of the resource
    * @param timeout the timeout for the operation.  default 30 seconds.
    * @return a future to the resource
    */
  def read(id: VId, timeout: Duration = defaultTimeout): Result[VType] = {
    await(timeout)(store.read(id))
  }

  /**
    * Update the resource
    * @param id the ID of the resource
    * @param newvalue the new value
    * @param timeout the timeout for the operation.  default 30 seconds.
    * @param context the change context for this operation
    * @return a future to the new value
    */
  def update(
      id: VId,
      newvalue: VType,
      timeout: Duration = defaultTimeout,
      changeContext: ChangeContext = ChangeContext()
  ): Result[VType] = {
    await(timeout)(store.update(id, newvalue, changeContext))
  }

  /**
    * Update the resource.  Uses the default of 30 seconds for the timeout.
    * @param T the type of a helper object to generate the response
    * @param R the type of the response
    * @param id the ID of the resource
    * @param updater a function to update old value to new value
    * @return a future to the new value
    */
  def update[T, R](id: VId, updator: Updator[VType, T, R]): Result[R] = {
    await(defaultTimeout)(store.update(id, updator))
  }

  /**
    * Update the resource
    * @param T the type of a helper object to generate the response
    * @param R the type of the response
    * @param id the ID of the resource
    * @param updater a function to update old value to new value
    * @param timeout the timeout for the operation.  default 30 seconds.
    * @return a future to the new value
    */
  def update[T, R](
      id: VId,
      updator: Updator[VType, T, R],
      timeout: Duration
  ): Result[R] = {
    await(timeout)(store.update(id, updator))
  }

  /**
    * Delete the resource
    * @param id the ID of the resource
    * @param timeout the timeout for the operation.  default 30 seconds.
    * @param context the change context for this operation
    * @return a future to the old values
    */
  def delete(
      id: VId,
      timeout: Duration = defaultTimeout,
      changeContext: ChangeContext = ChangeContext()
  ): Result[VType] = {
    await(timeout)(store.delete(id, changeContext))
  }

}
