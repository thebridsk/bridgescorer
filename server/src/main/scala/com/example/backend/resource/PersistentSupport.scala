package com.example.backend.resource

import akka.http.scaladsl.model.StatusCodes
import com.example.data.RestMessage
import com.example.data.VersionedInstance
import scala.concurrent.Future
import com.example.data.Id

abstract
class PersistentSupport[VId,VType <: VersionedInstance[VType,VType,VId]](
    implicit
      val support: StoreSupport[VId, VType]
    ) {


  val resourceURI: String = support.resourceURI

  private var cacheKeys: ()=>Set[VId] = ()=>Set()

  private var maxId: Option[VId] = None

  def setCacheKeysFunction( fun: ()=>Set[VId] ) = cacheKeys = fun

  /**
   * Get all the IDs from persistent storage
   */
  def getAllIdsFromPersistent(): Set[VId] = getAllIdsFromPersistent( cacheKeys )

  /**
   * Get all the IDs from persistent storage
   */
  def getAllIdsFromPersistent(cacheKeys: ()=>Set[VId]): Set[VId]

  /**
   * Generate the next ID for a new instance
   * @param v the new instance that will be created
   * @param persistent
   * @return the next ID
   */
  def generateNextId( v: VType ): Result[VId] = synchronized {
    import Implicits._

    val id = maxId.orElse {
      val current = getAllIdsFromPersistent()
      current.foldLeft(PersistentSupport.zero[VId]) { (ac,vid) =>
        val mm = ac.map( m => if ( Id.idComparer(m.toString(), vid.toString()) < 0 ) vid else m )
        mm.orElse(Some(vid))
      }
    }

    id.map { i =>
      val n = Id.genericIdToNumber(i.toString()).toInt
      val nid = s"${support.idprefix}${n+1}".asInstanceOf[VId]
      maxId = Some(nid)
      Result(nid)
    }.getOrElse( Result( s"${support.idprefix}1".asInstanceOf[VId] ) ).logit("generateNextId")
  }

  /**
   * Add a new ID to the store, this add is done out of band.
   * @param id
   */
  def addId( id: VId ): Unit = synchronized {
    val curMaxId = maxId.orElse {
      val current = getAllIdsFromPersistent()
      current.foldLeft(PersistentSupport.zero[VId]) { (ac,vid) =>
        val mm = ac.map( m => if ( Id.idComparer(m.toString(), vid.toString()) < 0 ) vid else m )
        mm.orElse(Some(vid))
      }
    }
    val newMax = curMaxId match {
      case Some(curId) =>
        if ( Id.idComparer(curId.toString(), id.toString()) < 0 ) id else curId
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
                          v: VType
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
                       id: VId, v: VType
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
  def resultAfterDelete( id: VId ): Result[VType] = notFound(id)


  /**
   * The response to put in the cache after deleting a value.  This is to ensure that the delete completes before the next operation starts.
   * @param id
   * @param cachedValue the value in the cache at the start of the operation
   * @return true means read it again.  false means value is ok
   */
  def readCheckForDelete( id: VId, cachedValue: Result[VType] ) = false

  def notFound( id: VId ) = Result(StatusCodes.NotFound,s"Did not find resource $resourceURI/$id")
  def internalError = Result(StatusCodes.InternalServerError,RestMessage("Internal error"))
  def storeIsReadOnly = Result.future(StatusCodes.BadRequest,RestMessage("Store is read only"))
  def alreadyExists( id: VId ) = Result.future(StatusCodes.BadRequest, s"Resource already exists ${resourceURI}/${id}")
}


object PersistentSupport {
  def zero[VId]: Option[VId] = None
}
