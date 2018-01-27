package com.example.backend

import com.example.backend.resource.NestedResourceSupport
import com.example.backend.resource.Result
import com.example.data.MatchDuplicate
import com.example.data.Board
import com.example.data.Team
import com.example.data.Id
import akka.http.scaladsl.model.StatusCodes
import com.example.data.RestMessage
import com.example.backend.resource.StoreSupport
import com.example.backend.resource.VersionedInstanceJson
import com.example.backend.resource.PersistentSupport
import com.example.data.BoardSet
import com.example.data.DuplicateHand
import com.example.data.Movement
import com.example.data.VersionedInstance
import com.example.data.MatchChicago
import com.example.data.MatchRubber
import com.example.data.MatchDuplicateResult
import com.example.backend.resource.Resource
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class GenericIdCacheStoreSupport[VId,VType <: VersionedInstance[VType,VType,VId]](
                   idprefix: String,
                   resourceName: String,
                   resourceURI: String,
                   readOnly: Boolean,
                   useIdFromValue: Boolean = false,
                   dontUpdateTime: Boolean = false
                 )(
                   implicit
                     instanceJson: VersionedInstanceJson[VId,VType]
                 ) extends
                     StoreSupport[VId,VType](idprefix,resourceName,resourceURI,readOnly,useIdFromValue,dontUpdateTime) {

  def stringToId(s: String): Option[VId] = {
    Some(s.asInstanceOf[VId])
  }

}

class MatchDuplicateCacheStoreSupport(
                   readOnly: Boolean,
                   useIdFromValue: Boolean = false,
                   dontUpdateTime: Boolean = false
                 )(
                   implicit
                     instanceJson: VersionedInstanceJson[Id.MatchDuplicate,MatchDuplicate]
                 ) extends
                 GenericIdCacheStoreSupport[Id.MatchDuplicate,MatchDuplicate]("M","MatchDuplicate","/duplicates",readOnly,useIdFromValue,dontUpdateTime)

class MatchDuplicateResultCacheStoreSupport(
                   readOnly: Boolean,
                   useIdFromValue: Boolean = false,
                   dontUpdateTime: Boolean = false
                 )(
                   implicit
                     instanceJson: VersionedInstanceJson[Id.MatchDuplicateResult,MatchDuplicateResult]
                 ) extends
                 GenericIdCacheStoreSupport[Id.MatchDuplicateResult,MatchDuplicateResult]("E","MatchDuplicateResult","/duplicateresults",readOnly,useIdFromValue,dontUpdateTime)

class MatchChicagoCacheStoreSupport(
                   readOnly: Boolean,
                   useIdFromValue: Boolean = false,
                   dontUpdateTime: Boolean = false
                 )(
                   implicit
                     instanceJson: VersionedInstanceJson[Id.MatchChicago,MatchChicago]
                 ) extends
                 GenericIdCacheStoreSupport[Id.MatchChicago,MatchChicago]("C","MatchChicago","/chicagos",readOnly,useIdFromValue,dontUpdateTime)

class MatchRubberCacheStoreSupport(
                   readOnly: Boolean,
                   useIdFromValue: Boolean = false,
                   dontUpdateTime: Boolean = false
                 )(
                   implicit
                     instanceJson: VersionedInstanceJson[String,MatchRubber]
                 ) extends
                 GenericIdCacheStoreSupport[String,MatchRubber]("R","MatchRubber","/rubbers",readOnly,useIdFromValue,dontUpdateTime)

class GenericIdFromInstanceCacheStoreSupport[VId,VType <: VersionedInstance[VType,VType,VId]](
                   resourceName: String,
                   resourceURI: String,
                   readOnly: Boolean,
                   dontUpdateTime: Boolean = false
                 )(
                   implicit
                     instanceJson: VersionedInstanceJson[VId,VType]
                 ) extends
                 StoreSupport[VId,VType]("",resourceName,resourceURI,readOnly,true,dontUpdateTime) {

  /**
   * @param v the value to create
   * @param persistent the persistent store
   * @return a future to the result with the value
   */
  override
  def createInPersistent( v: VType, persistent: PersistentSupport[VId,VType], dontUpdateTimes: Boolean = false ): Future[Result[VType]] = {
    persistent.createInPersistent(Some(v.id), v, dontUpdateTimes)
  }

  /**
   * Check whether the IDs for this store are obtained from the values (VType),
   * or whether they are generated.
   * @return true the id is obtained from the value.  false an id is generated.
   */
  override
  def isIdFromValue = true

  def stringToId(s: String): Option[VId] = {
    Some(s.asInstanceOf[VId])
  }

}

class BoardSetCacheStoreSupport(
                   readOnly: Boolean,
                   dontUpdateTime: Boolean = false
                 )(
                   implicit
                     instanceJson: VersionedInstanceJson[String,BoardSet]
                 ) extends
                 GenericIdFromInstanceCacheStoreSupport[String,BoardSet]("Boardset","/boardsets",readOnly,dontUpdateTime) {

}

class MovementCacheStoreSupport(
                   readOnly: Boolean,
                   dontUpdateTime: Boolean = false
                 )(
                   implicit
                     instanceJson: VersionedInstanceJson[String,Movement]
                 ) extends
                 GenericIdFromInstanceCacheStoreSupport[String,Movement]("Movement","/movements",readOnly,dontUpdateTime) {

}

object DuplicateTeamsNestedResource extends NestedResourceSupport[MatchDuplicate, Id.Team, Team] {
  val resourceURI = "teams"

  def getResources( parent: MatchDuplicate, parentResource: String ): Result[Map[Id.Team,Team]] =
    Result(parent.teams.map(t => t.id -> t).toMap)

  def getResource( parent: MatchDuplicate, parentResource: String, id: Id.Team ): Result[Team] =
    parent.getTeam(id).
           map( t => Result(t) ).
           getOrElse( notFound(parentResource, id) )

  def updateResources( parent: MatchDuplicate, parentResource: String, map: Map[Id.Team,Team] ): Result[MatchDuplicate] =
    Result( parent.setTeams(map.values.toList))

  def updateResource( parent: MatchDuplicate, parentResource: String, id: Id.Team, value: Team ): Result[(MatchDuplicate,Team)] = {
    val t = value.setId(id, false)
    Result( (parent.updateTeam(t),t) )
  }

  def createResource( parent: MatchDuplicate, parentResource: String, value: Team ): Result[(MatchDuplicate,Team)] =
    Result( (parent.updateTeam(value), value) )

  def deleteResource( parent: MatchDuplicate, parentResource: String, id: Id.Team ): Result[(MatchDuplicate,Team)] = {
    parent.getTeam(id).
           map( t => Result( (parent.deleteTeam(id), t) ) ).
           getOrElse( notFound(parentResource, id) )
  }
}

object DuplicateBoardsNestedResource extends NestedResourceSupport[MatchDuplicate, Id.DuplicateBoard, Board] {
  val resourceURI = "boards"

  def getResources( parent: MatchDuplicate, parentResource: String ): Result[Map[Id.DuplicateBoard,Board]] =
    Result(parent.boards.map(t => t.id -> t).toMap)

  def getResource( parent: MatchDuplicate, parentResource: String, id: Id.DuplicateBoard ): Result[Board] =
    parent.getBoard(id).
           map( t => Result(t) ).
           getOrElse( notFound(parentResource, id) )

  def updateResources( parent: MatchDuplicate, parentResource: String, map: Map[Id.DuplicateBoard,Board] ): Result[MatchDuplicate] =
    Result( parent.setBoards(map.values.toList))

  def updateResource( parent: MatchDuplicate, parentResource: String, id: Id.DuplicateBoard, value: Board ): Result[(MatchDuplicate,Board)] = {
    val t = value.setId(id, false)
    Result( (parent.updateBoard(t),t) )
  }

  def createResource( parent: MatchDuplicate, parentResource: String, value: Board ): Result[(MatchDuplicate,Board)] =
    Result( (parent.updateBoard(value), value) )

  def deleteResource( parent: MatchDuplicate, parentResource: String, id: Id.DuplicateBoard ): Result[(MatchDuplicate,Board)] = {
    parent.getBoard(id).
           map( t => Result( (parent.deleteBoard(id), t) ) ).
           getOrElse( notFound(parentResource, id) )
  }
}

object DuplicateHandsNestedResource extends NestedResourceSupport[Board, Id.Team, DuplicateHand] {
  val resourceURI = "hands"

  def getResources( parent: Board, parentResource: String ): Result[Map[Id.Team,DuplicateHand]] =
    Result(parent.hands.map(t => t.id -> t).toMap)

  def getResource( parent: Board, parentResource: String, id: Id.Team ): Result[DuplicateHand] =
    parent.getHand(id).
           map( t => Result(t) ).
           getOrElse( notFound(parentResource, id) )

  def updateResources( parent: Board, parentResource: String, map: Map[Id.Team,DuplicateHand] ): Result[Board] =
    Result( parent.setHands(map.values.toList))

  def updateResource( parent: Board, parentResource: String, id: Id.Team, value: DuplicateHand ): Result[(Board,DuplicateHand)] = {
    val t = value.setId(id, false)
    Result( (parent.updateHand(t),t) )
  }

  def createResource( parent: Board, parentResource: String, value: DuplicateHand ): Result[(Board,DuplicateHand)] =
    Result( (parent.updateHand(value), value) )

  def deleteResource( parent: Board, parentResource: String, id: Id.Team ): Result[(Board,DuplicateHand)] = {
    parent.getHand(id).
           map( t => Result( (parent.deleteHand(id), t) ) ).
           getOrElse( notFound(parentResource, id) )
  }
}

object BridgeNestedResources {

  implicit class WrapMatchDuplicateResource( val r: Resource[Id.MatchDuplicate,MatchDuplicate] ) extends AnyVal {
    def resourceBoards( implicit execute: ExecutionContext ) = r.nestedResource( DuplicateBoardsNestedResource )
    def resourceTeams( implicit execute: ExecutionContext ) = r.nestedResource( DuplicateTeamsNestedResource )
  }

  implicit class WrapBoardResource( val r: Resource[Id.DuplicateBoard,Board] ) extends AnyVal {
    def resourceHands( implicit execute: ExecutionContext ) = r.nestedResource( DuplicateHandsNestedResource )
  }

}

class BridgeResources( yaml: Boolean=true,
                       readOnly: Boolean=false,
                       useIdFromValue: Boolean = false,
                       dontUpdateTime: Boolean = false
                     ) {

  val converters = new BridgeServiceFileStoreConverters(yaml)
  import converters._

  implicit val matchDuplicateCacheStoreSupport = new MatchDuplicateCacheStoreSupport(readOnly,useIdFromValue,dontUpdateTime)
  implicit val matchDuplicateResultCacheStoreSupport = new MatchDuplicateResultCacheStoreSupport(readOnly,useIdFromValue,dontUpdateTime)
  implicit val matchChicagoCacheStoreSupport = new MatchChicagoCacheStoreSupport(readOnly,useIdFromValue,dontUpdateTime)
  implicit val matchRubberCacheStoreSupport = new MatchRubberCacheStoreSupport(readOnly,useIdFromValue,dontUpdateTime)
  implicit val boardSetCacheStoreSupport = new BoardSetCacheStoreSupport(readOnly,dontUpdateTime)
  implicit val movementCacheStoreSupport = new MovementCacheStoreSupport(readOnly,dontUpdateTime)

}

object BridgeResources {
  def apply( yaml: Boolean=true,
             readOnly: Boolean=false,
             useIdFromValue: Boolean = false,
             dontUpdateTime: Boolean = false
           ) = new BridgeResources(yaml,readOnly,useIdFromValue,dontUpdateTime)
}
