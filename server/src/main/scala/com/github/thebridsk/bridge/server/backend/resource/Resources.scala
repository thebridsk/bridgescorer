package com.github.thebridsk.bridge.server.backend.resource

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import com.github.thebridsk.utilities.logging.Logger
import Resources._
import org.scalactic.source.Position
import com.github.thebridsk.source.SourcePosition

object Resources {

  val log = Logger[Resources[_, _]]

}

abstract class Resources[VId, VType](
    implicit
    execute: ExecutionContext
) {
  val resourceURI: String

  /**
    * Create a new value in the collection
    * @param the new value, the 'id' field will be ignored
    * @param context the change context for this operation
    * @return a future to the new value with updated 'id' field
    */
  def createChild(
      newvalue: VType,
      changeContext: ChangeContext = ChangeContext()
  )(
      implicit
      pos: SourcePosition
  ): Future[Result[VType]]

  /**
    * Read all the values in the collection
    * @param context the change context for this operation
    * @return a future to the resources
    */
  def readAll()(
      implicit
      pos: SourcePosition
  ): Future[Result[Map[VId, VType]]]

  /**
    * Update the collection with new values
    * @param newvalue
    * @param context the change context for this operation
    * @return a future to the new values
    */
  def updateAll(
      newvalue: Map[VId, VType],
      changeContext: ChangeContext = ChangeContext()
  )(
      implicit
      pos: SourcePosition
  ): Future[Result[Map[VId, VType]]]

  /**
    * Delete the collection
    * @param context the change context for this operation
    * @return a future to the old values
    */
  def deleteAll(
      changeContext: ChangeContext = ChangeContext()
  )(
      implicit
      pos: SourcePosition
  ): Future[Result[Map[VId, VType]]]

  /**
    * select a resource
    * @param id the ID of the resource to select
    * @return a Resource object for the resource
    */
  def select(id: VId): Resource[VId, VType] = {
    new Resource(s"$resourceURI/$id", this, id)
  }

  /**
    * Read the resource
    * @param id the ID of the resource
    * @param context the change context for this operation
    * @return a future to the resource
    */
  def read(id: VId)(
      implicit
      pos: SourcePosition
  ): Future[Result[VType]]

  /**
    * Update the resource
    * @param T the type of a helper object to generate the response
    * @param R the type of the response
    * @param id the ID of the resource
    * @param updater a function to update old value to new value
    * @param context the change context for this operation
    * @return a future to the new value
    */
  def update[T, R](id: VId, updator: Updator[VType, T, R])(
      implicit
      pos: SourcePosition
  ): Future[Result[R]]

  /**
    * Delete the resource
    * @param id the ID of the resource
    * @param context the change context for this operation
    * @return a future to the old values
    */
  def delete(
      id: VId,
      changeContext: ChangeContext = ChangeContext()
  )(
      implicit
      pos: SourcePosition
  ): Future[Result[VType]]

}
