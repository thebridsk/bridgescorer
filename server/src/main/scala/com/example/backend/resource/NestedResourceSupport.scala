package com.example.backend.resource

import akka.http.scaladsl.model.StatusCodes
import com.example.data.RestMessage

/**
 * @param PVType parent resource type
 * @param NVId type of ID of nested resource
 * @param NVType nested resource type
 */
trait NestedResourceSupport[PVType, NVId, NVType] {
  /** the name of the nested resource.  This does not include any part of the parent resourceURI */
  val resourceURI: String

  def getResources( parent: PVType, parentResource: String ): Result[Map[NVId,NVType]]
  def getResource( parent: PVType, parentResource: String, id: NVId ): Result[NVType]

  def updateResources( parent: PVType, parentResource: String, map: Map[NVId,NVType] ): Result[PVType]
  def updateResource( parent: PVType, parentResource: String, id: NVId, value: NVType ): Result[(PVType,NVType)]

  def createResource( parent: PVType, parentResource: String, value: NVType ): Result[(PVType,NVType)]
  def deleteResource( parent: PVType, parentResource: String, id: NVId ): Result[(PVType,NVType)]

  def notFound( parentResource: String, id: NVId ) =
    Result(StatusCodes.NotFound,RestMessage(s"Did not find resource ${parentResource}/${resourceURI}/$id") )

  def badRequest( parentResource: String, id: NVId, msg: String ) =
    Result(StatusCodes.BadRequest,RestMessage(s"Error on resource ${parentResource}/${resourceURI}/$id: $msg") )
}
