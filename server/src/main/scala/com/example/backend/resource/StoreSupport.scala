package com.example.backend.resource

import com.example.data.VersionedInstance
import akka.http.scaladsl.model.StatusCodes
import com.example.data.RestMessage
import scala.concurrent.Future

object StoreSupport {
  val readOnlyStoreError =
    Result(StatusCodes.BadRequest, RestMessage("Store is read only"))
}

abstract class StoreSupport[VId, VType <: VersionedInstance[VType, VType, VId]](
    val idprefix: String,
    val resourceName: String,
    val resourceURI: String,
    val readOnly: Boolean,
    val useIdFromValue: Boolean = false,
    val dontUpdateTime: Boolean = false
)(
    implicit
    val instanceJson: VersionedInstanceJson[VId, VType]
) {

  /**
    * @param v the value to create
    * @param persistent the persistent store
    * @return a future to the result with the value
    */
  def createInPersistent(
      v: VType,
      persistent: PersistentSupport[VId, VType],
      dontUpdateTimes: Boolean = false
  ): Future[Result[VType]] = {
    persistent.createInPersistent(None, v, dontUpdateTimes)
  }

  /**
    * Check whether the IDs for this store are obtained from the values (VType),
    * or whether they are generated.
    * @return true the id is obtained from the value.  false an id is generated.
    */
  def isIdFromValue = false

  /**
    * Set the id for a value
    * @param id
    * @param v
    * @param forCreate this is for creating a resource.  This indicates that the created field should be updated.
    * @return the value with the updated id
    */
  def setId(id: VId, v: VType, forCreate: Boolean): VType =
    v.setId(id, forCreate, dontUpdateTime)

  /**
    * Get the ID for a value
    * @param v
    * @return the Id from the value.
    */
  def getId(v: VType): VId = v.id

  /**
    * Convert a value to a JSON string.
    * @param v the value
    * @return a string that contains the JSON representation of the value.
    */
  def toJSON(v: VType): String = instanceJson.toJson(v)

  /**
    * Convert a JSON string to the VType object
    * @param s the JSON string
    * @return a 2 tuple
    *         boolean - true if store is good
    *         VType   - the converted object
    */
  def fromJSON(s: String): (Boolean, VType) = instanceJson.parse(s)

  /**
    * Returns the file extensions to look for when reading resources.
    */
  def getReadExtensions() = instanceJson.getReadExtensions()

  /**
    * Returns the file extension to use when writing resources.
    */
  def getWriteExtension() = instanceJson.getWriteExtension()

  /**
    * Returns the Id object that the specified string identifies.
    * @param s the string representation of an Id
    * @return returns Some(id) if the string represents a valid Id.  None otherwise.
    */
  def stringToId(s: String): Option[VId]
}
