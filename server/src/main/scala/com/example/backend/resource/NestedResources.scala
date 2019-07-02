package com.example.backend.resource

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.example.backend.resource.Implicits._
import com.example.source.SourcePosition

class NestedResources[PVId, PVType, NVId, NVType](
    val parent: Resource[PVId, PVType],
    val nested: NestedResourceSupport[PVType, NVId, NVType]
)(
    implicit execute: ExecutionContext
) extends Resources[NVId, NVType] {
  val resourceURI = s"${parent.resourceURI}/${nested.resourceURI}"

  /**
    * Create a new value in the collection
    * @param the new value, the 'id' field will be ignored
    * @param context the change context for this operation
    * @return a future to the new value with updated 'id' field
    */
  def createChild(
      newvalue: NVType,
      changeContext: ChangeContext = ChangeContext()
  )(
      implicit caller: SourcePosition
  ): Future[Result[NVType]] = {
    parent
      .update(
        new Updator[PVType, NVType, NVType] {

          def updater(p: PVType): Result[(PVType, NVType)] = {
            nested.createResource(p, parent.resourceURI, newvalue).map { e =>
              prepend(ChangeContext.create(e._2, s"${resourceURI}"))
              e
            }
          }

          def responder(r: Result[(PVType, NVType)]): Result[NVType] = {
            r.map(e => e._2)
          }
        }
      )
      .logit(s"Create child ${resourceURI}")
  }

  /**
    * Read all the values in the collection
    * @param context the change context for this operation
    * @return a future to the resources
    */
  def readAll()(
      implicit caller: SourcePosition
  ): Future[Result[Map[NVId, NVType]]] = {
    parent
      .read()
      .map { r =>
        r.flatMap(p => nested.getResources(p, parent.resourceURI))
      }
      .logit(s"ReadAll ${resourceURI}")
  }

  /**
    * Update the collection with new values
    * @param updater a function to update old values to new values
    * @param context the change context for this operation
    * @return a future to the new values
    */
  def updateAll(
      newvalue: Map[NVId, NVType],
      changeContext: ChangeContext = ChangeContext()
  )(
      implicit caller: SourcePosition
  ): Future[Result[Map[NVId, NVType]]] = {
    parent.update(
      new Updator[PVType, PVType, Map[NVId, NVType]] {

        def updater(p: PVType): Result[(PVType, PVType)] = {
          nested.updateResources(p, parent.resourceURI, newvalue).map { np =>
            prepend(ChangeContext.update(newvalue, resourceURI))
            (np, np)
          }
        }

        def responder(
            r: Result[(PVType, PVType)]
        ): Result[Map[NVId, NVType]] = {
          r.flatMap(e => nested.getResources(e._1, parent.resourceURI))
        }
      }
    )
  }.logit(s"UpdateAll ${resourceURI}")

  /**
    * Delete the collection
    * @param context the change context for this operation
    * @return a future to the old values
    */
  def deleteAll(
      changeContext: ChangeContext = ChangeContext()
  )(
      implicit caller: SourcePosition
  ): Future[Result[Map[NVId, NVType]]] = {
    updateAll(Map(), changeContext).logit(s"DeleteAll ${resourceURI}")
  }

  /**
    * Read the resource
    * @param id the ID of the resource
    * @param context the change context for this operation
    * @return a future to the resource
    */
  def read(id: NVId)(
      implicit caller: SourcePosition
  ): Future[Result[NVType]] = {
    parent
      .read()
      .map { r =>
        r.flatMap(p => nested.getResource(p, parent.resourceURI, id))
      }
      .logit(s"Read ${resourceURI}/${id}")
  }

  /**
    * Update the resource
    * @param T the type of a helper object to generate the response
    * @param R the type of the response
    * @param id the ID of the resource
    * @param updater a function to update old value to new value
    * @param context the change context for this operation
    * @return a future to the new value
    */
  def update[T, R](id: NVId, updator: Updator[NVType, T, R])(
      implicit caller: SourcePosition
  ): Future[Result[R]] = {

    val u = new Updator[PVType, T, R] {
      override val changeContext = updator.changeContext

      def updater(p: PVType): Result[(PVType, T)] = {
        nested.getResource(p, parent.resourceURI, id).flatMap { ov =>
          updator.updater(ov).flatMap { e =>
            val (nv, t) = e
            nested.updateResource(p, parent.resourceURI, id, nv).map { e =>
              prepend(ChangeContext.update(e._2, s"${resourceURI}/$id"))
              (e._1, t)
            }
          }
        }
      }

      def responder(r: Result[(PVType, T)]): Result[R] = {
        r.flatMap { re =>
          val (p, t) = re
          nested.getResource(p, parent.resourceURI, id) match {
            case Right(l) => updator.responder(Right(l, t))
            case Left(e)  => Left(e)
          }
        }
      }
    }
    parent.update(u).logit(s"Update ${resourceURI}/${id}")
  }

  /**
    * Delete the resource
    * @param id the ID of the resource
    * @param context the change context for this operation
    * @return a future to the old values
    */
  def delete(id: NVId, changeContext: ChangeContext = ChangeContext())(
      implicit caller: SourcePosition
  ): Future[Result[NVType]] = {
    parent
      .update(
        new Updator[PVType, NVType, NVType] {

          def updater(p: PVType): Result[(PVType, NVType)] = {
            nested.deleteResource(p, parent.resourceURI, id).map { e =>
              val (np, ov) = e
              prepend(ChangeContext.delete(ov, s"${resourceURI}/${id}"))
              (np, ov)
            }
          }

          def responder(r: Result[(PVType, NVType)]): Result[NVType] = {
            r.map(e => (e._2))
          }
        }
      )
      .logit(s"Delete ${resourceURI}/${id}")
  }

}
