package com.github.thebridsk.bridge.server.backend.resource

import com.github.thebridsk.utilities.logging.Logger
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import com.github.thebridsk.bridge.server.backend.resource.Implicits._
import Resource._
import com.github.thebridsk.source.SourcePosition

object Resource {

  val log: Logger = Logger[Resource[_, _]]()

}

class Resource[VId, VType](
    val resourceURI: String,
    val resources: Resources[VId, VType],
    val id: VId
)(
    implicit
    execute: ExecutionContext
) {

  /**
    * Read the resource
    * @param context the change context for this operation
    * @return a future to the resource
    */
  def read()(implicit pos: SourcePosition): Future[Result[VType]] =
    resources.read(id).logit(s"Read ${resourceURI}")

  /**
    * Update the resource
    * @param updater a function to update old value to new value
    * @param context the change context for this operation
    * @return a future to the new value
    */
  def update[T, R](
      updator: Updator[VType, T, R]
  )(
      implicit
      pos: SourcePosition
  ): Future[Result[R]] =
    resources.update(id, updator).logit(s"update with updator ${resourceURI}")

  /**
    * Update the resource
    * @param updater a function to update old value to new value
    * @param context the change context for this operation
    * @return a future to the new value
    */
  def update(
      newvalue: VType,
      context: ChangeContext = ChangeContext()
  )(
      implicit
      pos: SourcePosition
  ): Future[Result[VType]] = {
    log.fine(s"Updating resource ${resourceURI}: ${newvalue}")
    resources
      .update(
        id,
        new Updator[VType, VType, VType] {
          override val changeContext = context

          def updater(value: VType): Result[(VType, VType)] = {
            Result((newvalue, newvalue))
          }

          def responder(resp: Result[(VType, VType)]): Result[VType] = {
            resp.map(e => e._1)
          }

        }
      )
      .logit(s"Update ${resourceURI}")
  }

  /**
    * Delete the resource
    * @param context the change context for this operation
    * @return a future to the old values
    */
  def delete(changeContext: ChangeContext = ChangeContext())(
      implicit
      pos: SourcePosition
  ): Future[Result[VType]] =
    resources.delete(id, changeContext).logit(s"Delete ${resourceURI}")

  /**
    * @param nestedResource the nested resource to get
    * @param context the change context for this operation
    * @return a future to the nested Resources
    */
  def nestedResource[NVId, NVType](
      nestedResource: NestedResourceSupport[VType, NVId, NVType]
  ): NestedResources[VId, VType, NVId, NVType] = {
    new NestedResources(this, nestedResource)
  }

}
