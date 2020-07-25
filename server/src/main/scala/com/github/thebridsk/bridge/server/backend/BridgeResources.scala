package com.github.thebridsk.bridge.server.backend

import com.github.thebridsk.bridge.server.backend.resource.NestedResourceSupport
import com.github.thebridsk.bridge.server.backend.resource.Result
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.server.backend.resource.StoreSupport
import com.github.thebridsk.bridge.server.backend.resource.VersionedInstanceJson
import com.github.thebridsk.bridge.server.backend.resource.PersistentSupport
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.data.VersionedInstance
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.server.backend.resource.Resource
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.github.thebridsk.bridge.data.Round
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.RubberHand
import com.github.thebridsk.bridge.server.backend.resource.IdSupport
import com.github.thebridsk.bridge.data.HasId

class GenericIdCacheStoreSupport[TId, VType <: VersionedInstance[
  VType,
  VType,
  Id[TId]
]](
    hasId: HasId[TId],
    resourceName: String,
    resourceURI: String,
    readOnly: Boolean,
    useIdFromValue: Boolean = false,
    dontUpdateTime: Boolean = false
)(
    implicit
    instanceJson: VersionedInstanceJson[Id[TId], VType],
) extends StoreSupport[Id[TId], VType](
      new IdSupport[Id[TId]] {

        override
        def compare( idthis: Id[TId], idthat: Id[TId] ): Int = idthis.compareTo(idthat)

        override
        def toId( i: Int ): Id[TId] = hasId.id(i)

        override
        def toNumber( id: Id[TId] ): Int = id.toInt

        override
        def toString( id: Id[TId] ): String = id.id

      },
      resourceName,
      resourceURI,
      readOnly,
      useIdFromValue,
      dontUpdateTime
    ) {

  def stringToId(s: String): Option[Id[TId]] = {
    Some(hasId.id(s))
  }

}

class MatchDuplicateCacheStoreSupport(
    readOnly: Boolean,
    useIdFromValue: Boolean = false,
    dontUpdateTime: Boolean = false
)(
    implicit
    instanceJson: VersionedInstanceJson[MatchDuplicate.Id, MatchDuplicate]
) extends GenericIdCacheStoreSupport(
      MatchDuplicate,
      "MatchDuplicate",
      "/duplicates",
      readOnly,
      useIdFromValue,
      dontUpdateTime
    )

class MatchDuplicateResultCacheStoreSupport(
    readOnly: Boolean,
    useIdFromValue: Boolean = false,
    dontUpdateTime: Boolean = false
)(
    implicit
    instanceJson: VersionedInstanceJson[
      MatchDuplicateResult.Id,
      MatchDuplicateResult
    ]
) extends GenericIdCacheStoreSupport(
      MatchDuplicateResult,
      "MatchDuplicateResult",
      "/duplicateresults",
      readOnly,
      useIdFromValue,
      dontUpdateTime
    )

class MatchChicagoCacheStoreSupport(
    readOnly: Boolean,
    useIdFromValue: Boolean = false,
    dontUpdateTime: Boolean = false
)(
    implicit
    instanceJson: VersionedInstanceJson[MatchChicago.Id, MatchChicago]
) extends GenericIdCacheStoreSupport(
      MatchChicago,
      "MatchChicago",
      "/chicagos",
      readOnly,
      useIdFromValue,
      dontUpdateTime
    )

class MatchRubberCacheStoreSupport(
    readOnly: Boolean,
    useIdFromValue: Boolean = false,
    dontUpdateTime: Boolean = false
)(
    implicit
    instanceJson: VersionedInstanceJson[MatchRubber.Id, MatchRubber]
) extends GenericIdCacheStoreSupport(
      MatchRubber,
      "MatchRubber",
      "/rubbers",
      readOnly,
      useIdFromValue,
      dontUpdateTime
    )

class GenericIdFromInstanceCacheStoreSupport[TId, VType <: VersionedInstance[
  VType,
  VType,
  Id[TId]
]](
    hasId: HasId[TId],
    resourceName: String,
    resourceURI: String,
    readOnly: Boolean,
    dontUpdateTime: Boolean = false
)(
    implicit
    instanceJson: VersionedInstanceJson[Id[TId], VType]
) extends StoreSupport[Id[TId], VType](
      new IdSupport[Id[TId]] {

        override
        def compare( idthis: Id[TId], idthat: Id[TId] ): Int = idthis.compareTo(idthat)

        override
        def toString( id: Id[TId] ): String = id.id

      },
      resourceName,
      resourceURI,
      readOnly,
      true,
      dontUpdateTime
    ) {

  /**
    * @param v the value to create
    * @param persistent the persistent store
    * @return a future to the result with the value
    */
  override def createInPersistent(
      v: VType,
      persistent: PersistentSupport[Id[TId], VType],
      dontUpdateTimes: Boolean = false
  ): Future[Result[VType]] = {
    persistent.createInPersistent(Some(v.id), v, dontUpdateTimes)
  }

  /**
    * Check whether the IDs for this store are obtained from the values (VType),
    * or whether they are generated.
    * @return true the id is obtained from the value.  false an id is generated.
    */
  override def isIdFromValue = true

  def stringToId(s: String): Option[Id[TId]] = {
    Some(hasId.id(s))
  }

}

class BoardSetCacheStoreSupport(
    readOnly: Boolean,
    dontUpdateTime: Boolean = false
)(
    implicit
    instanceJson: VersionedInstanceJson[BoardSet.Id, BoardSet]
) extends GenericIdFromInstanceCacheStoreSupport(
      BoardSet,
      "Boardset",
      "/boardsets",
      readOnly,
      dontUpdateTime
    ) {}

class MovementCacheStoreSupport(
    readOnly: Boolean,
    dontUpdateTime: Boolean = false
)(
    implicit
    instanceJson: VersionedInstanceJson[Movement.Id, Movement]
) extends GenericIdFromInstanceCacheStoreSupport(
      Movement,
      "Movement",
      "/movements",
      readOnly,
      dontUpdateTime
    ) {}

object DuplicateTeamsNestedResource
    extends NestedResourceSupport[MatchDuplicate, Team.Id, Team] {
  val resourceURI = "teams"

  def getResources(
      parent: MatchDuplicate,
      parentResource: String
  ): Result[Map[Team.Id, Team]] =
    Result(parent.teams.map(t => t.id -> t).toMap)

  def getResource(
      parent: MatchDuplicate,
      parentResource: String,
      id: Team.Id
  ): Result[Team] =
    parent
      .getTeam(id)
      .map(t => Result(t))
      .getOrElse(notFound(parentResource, id))

  def updateResources(
      parent: MatchDuplicate,
      parentResource: String,
      map: Map[Team.Id, Team]
  ): Result[MatchDuplicate] =
    Result(parent.setTeams(map.values.toList))

  def updateResource(
      parent: MatchDuplicate,
      parentResource: String,
      id: Team.Id,
      value: Team
  ): Result[(MatchDuplicate, Team)] = {
    val t = value.setId(id, false)
    Result((parent.updateTeam(t), t))
  }

  def createResource(
      parent: MatchDuplicate,
      parentResource: String,
      value: Team
  ): Result[(MatchDuplicate, Team)] =
    Result((parent.updateTeam(value), value))

  def deleteResource(
      parent: MatchDuplicate,
      parentResource: String,
      id: Team.Id
  ): Result[(MatchDuplicate, Team)] = {
    parent
      .getTeam(id)
      .map(t => Result((parent.deleteTeam(id), t)))
      .getOrElse(notFound(parentResource, id))
  }
}

object DuplicateBoardsNestedResource
    extends NestedResourceSupport[MatchDuplicate, Board.Id, Board] {
  val resourceURI = "boards"

  def getResources(
      parent: MatchDuplicate,
      parentResource: String
  ): Result[Map[Board.Id, Board]] =
    Result(parent.boards.map(t => t.id -> t).toMap)

  def getResource(
      parent: MatchDuplicate,
      parentResource: String,
      id: Board.Id
  ): Result[Board] =
    parent
      .getBoard(id)
      .map(t => Result(t))
      .getOrElse(notFound(parentResource, id))

  def updateResources(
      parent: MatchDuplicate,
      parentResource: String,
      map: Map[Board.Id, Board]
  ): Result[MatchDuplicate] =
    Result(parent.setBoards(map.values.toList))

  def updateResource(
      parent: MatchDuplicate,
      parentResource: String,
      id: Board.Id,
      value: Board
  ): Result[(MatchDuplicate, Board)] = {
    val t = value.setId(id, false)
    Result((parent.updateBoard(t), t))
  }

  def createResource(
      parent: MatchDuplicate,
      parentResource: String,
      value: Board
  ): Result[(MatchDuplicate, Board)] =
    Result((parent.updateBoard(value), value))

  def deleteResource(
      parent: MatchDuplicate,
      parentResource: String,
      id: Board.Id
  ): Result[(MatchDuplicate, Board)] = {
    parent
      .getBoard(id)
      .map(t => Result((parent.deleteBoard(id), t)))
      .getOrElse(notFound(parentResource, id))
  }
}

object DuplicateHandsNestedResource
    extends NestedResourceSupport[Board, Team.Id, DuplicateHand] {
  val resourceURI = "hands"

  def getResources(
      parent: Board,
      parentResource: String
  ): Result[Map[Team.Id, DuplicateHand]] =
    Result(parent.hands.map(t => t.id -> t).toMap)

  def getResource(
      parent: Board,
      parentResource: String,
      id: Team.Id
  ): Result[DuplicateHand] =
    parent
      .getHand(id)
      .map(t => Result(t))
      .getOrElse(notFound(parentResource, id))

  def updateResources(
      parent: Board,
      parentResource: String,
      map: Map[Team.Id, DuplicateHand]
  ): Result[Board] =
    Result(parent.setHands(map.values.toList))

  def updateResource(
      parent: Board,
      parentResource: String,
      id: Team.Id,
      value: DuplicateHand
  ): Result[(Board, DuplicateHand)] = {
    val t = value.setId(id, false)
    Result((parent.updateHand(t), t))
  }

  def createResource(
      parent: Board,
      parentResource: String,
      value: DuplicateHand
  ): Result[(Board, DuplicateHand)] =
    Result((parent.updateHand(value), value))

  def deleteResource(
      parent: Board,
      parentResource: String,
      id: Team.Id
  ): Result[(Board, DuplicateHand)] = {
    parent
      .getHand(id)
      .map(t => Result((parent.deleteHand(id), t)))
      .getOrElse(notFound(parentResource, id))
  }
}

object ChicagoRoundNestedResource
    extends NestedResourceSupport[MatchChicago, String, Round] {
  val resourceURI = "hands"

  def getResources(
      parent: MatchChicago,
      parentResource: String
  ): Result[Map[String, Round]] =
    Result(parent.rounds.map(t => t.id -> t).toMap)

  def getResource(
      parent: MatchChicago,
      parentResource: String,
      id: String
  ): Result[Round] =
    parent.rounds
      .find(r => r.id == id)
      .map(t => Result(t))
      .getOrElse(notFound(parentResource, id))

  def updateResources(
      parent: MatchChicago,
      parentResource: String,
      map: Map[String, Round]
  ): Result[MatchChicago] =
    Result(parent.setRounds(map))

  def updateResource(
      parent: MatchChicago,
      parentResource: String,
      id: String,
      value: Round
  ): Result[(MatchChicago, Round)] = {
    val t = value.setId(id, false)
    try {
      if (parent.getRound(id).isDefined) {
        val mc = parent.updateRound(t)
        Result((mc, t))
      } else {
        notFound(parentResource, id)
      }
    } catch {
      case x: IllegalArgumentException =>
        badRequest(parentResource, id, x.getMessage)
    }
  }

  def createResource(
      parent: MatchChicago,
      parentResource: String,
      value: Round
  ): Result[(MatchChicago, Round)] = {
    try {
      val mc = parent.addRound(value)
      Result((mc, value))
    } catch {
      case x: IllegalArgumentException =>
        badRequest(parentResource, value.id, x.getMessage)
    }
  }

  def deleteResource(
      parent: MatchChicago,
      parentResource: String,
      id: String
  ): Result[(MatchChicago, Round)] = {
    parent
      .getRound(id)
      .map { t =>
        try {
          val mc = parent.deleteRound(id)
          Result((mc, t))
        } catch {
          case x: IllegalArgumentException =>
            badRequest(parentResource, id, x.getMessage)
        }
      }
      .getOrElse(notFound(parentResource, id))
  }
}

object ChicagoRoundHandNestedResource
    extends NestedResourceSupport[Round, String, Hand] {
  val resourceURI = "hands"

  def getResources(
      parent: Round,
      parentResource: String
  ): Result[Map[String, Hand]] =
    Result(parent.hands.map(t => t.id -> t).toMap)

  def getResource(
      parent: Round,
      parentResource: String,
      id: String
  ): Result[Hand] =
    parent.hands
      .find(r => r.id == id)
      .map(t => Result(t))
      .getOrElse(notFound(parentResource, id))

  def updateResources(
      parent: Round,
      parentResource: String,
      map: Map[String, Hand]
  ): Result[Round] =
    Result(parent.setHands(map))

  def updateResource(
      parent: Round,
      parentResource: String,
      id: String,
      value: Hand
  ): Result[(Round, Hand)] = {
    val t = value.setId(id, false)
    try {
      if (parent.getHand(id).isDefined) {
        val mc = parent.updateHand(t)
        Result((mc, t))
      } else {
        notFound(parentResource, id)
      }
    } catch {
      case x: IllegalArgumentException =>
        badRequest(parentResource, id, x.getMessage)
    }
  }

  def createResource(
      parent: Round,
      parentResource: String,
      value: Hand
  ): Result[(Round, Hand)] = {
    try {
      val mc = parent.addHand(value)
      Result((mc, value))
    } catch {
      case x: IllegalArgumentException =>
        badRequest(parentResource, value.id, x.getMessage)
    }
  }

  def deleteResource(
      parent: Round,
      parentResource: String,
      id: String
  ): Result[(Round, Hand)] = {
    parent
      .getHand(id)
      .map { t =>
        try {
          val mc = parent.deleteHand(id)
          Result((mc, t))
        } catch {
          case x: IllegalArgumentException =>
            badRequest(parentResource, id, x.getMessage)
        }
      }
      .getOrElse(notFound(parentResource, id))
  }
}

object RubberHandNestedResource
    extends NestedResourceSupport[MatchRubber, String, RubberHand] {
  val resourceURI = "hands"

  def getResources(
      parent: MatchRubber,
      parentResource: String
  ): Result[Map[String, RubberHand]] =
    Result(parent.hands.map(t => t.id -> t).toMap)

  def getResource(
      parent: MatchRubber,
      parentResource: String,
      id: String
  ): Result[RubberHand] =
    parent.hands
      .find(r => r.id == id)
      .map(t => Result(t))
      .getOrElse(notFound(parentResource, id))

  def updateResources(
      parent: MatchRubber,
      parentResource: String,
      map: Map[String, RubberHand]
  ): Result[MatchRubber] =
    Result(parent.setHands(map))

  def updateResource(
      parent: MatchRubber,
      parentResource: String,
      id: String,
      value: RubberHand
  ): Result[(MatchRubber, RubberHand)] = {
    val t = value.setId(id, false)
    try {
      if (parent.getHand(id).isDefined) {
        val mc = parent.updateHand(t)
        Result((mc, t))
      } else {
        notFound(parentResource, id)
      }
    } catch {
      case x: IllegalArgumentException =>
        badRequest(parentResource, id, x.getMessage)
    }
  }

  def createResource(
      parent: MatchRubber,
      parentResource: String,
      value: RubberHand
  ): Result[(MatchRubber, RubberHand)] = {
    try {
      val mc = parent.addHand(value)
      Result((mc, value))
    } catch {
      case x: IllegalArgumentException =>
        badRequest(parentResource, value.id, x.getMessage)
    }
  }

  def deleteResource(
      parent: MatchRubber,
      parentResource: String,
      id: String
  ): Result[(MatchRubber, RubberHand)] = {
    parent
      .getHand(id)
      .map { t =>
        try {
          val mc = parent.deleteHand(id)
          Result((mc, t))
        } catch {
          case x: IllegalArgumentException =>
            badRequest(parentResource, id, x.getMessage)
        }
      }
      .getOrElse(notFound(parentResource, id))
  }
}

object BridgeNestedResources {

  implicit class WrapMatchDuplicateResource(
      private val r: Resource[MatchDuplicate.Id, MatchDuplicate]
  ) extends AnyVal {
    def resourceBoards(implicit execute: ExecutionContext) =
      r.nestedResource(DuplicateBoardsNestedResource)
    def resourceTeams(implicit execute: ExecutionContext) =
      r.nestedResource(DuplicateTeamsNestedResource)
  }

  implicit class WrapBoardResource(private val r: Resource[Board.Id, Board])
      extends AnyVal {
    def resourceHands(implicit execute: ExecutionContext) =
      r.nestedResource(DuplicateHandsNestedResource)
  }

  implicit class WrapMatchChicagoResource(
      private val r: Resource[MatchChicago.Id, MatchChicago]
  ) extends AnyVal {
    def resourceRounds(implicit execute: ExecutionContext) =
      r.nestedResource(ChicagoRoundNestedResource)
  }

  implicit class WrapMatchChicagoRoundResource(private val r: Resource[String, Round])
      extends AnyVal {
    def resourceHands(implicit execute: ExecutionContext) =
      r.nestedResource(ChicagoRoundHandNestedResource)
  }

  implicit class WrapMatchRubberResource(private val r: Resource[MatchRubber.Id, MatchRubber])
      extends AnyVal {
    def resourceHands(implicit execute: ExecutionContext) =
      r.nestedResource(RubberHandNestedResource)
  }

}

class BridgeResources(
    yaml: Boolean = true,
    readOnly: Boolean = false,
    useIdFromValue: Boolean = false,
    dontUpdateTime: Boolean = false
) {

  val converters = new BridgeServiceFileStoreConverters(yaml)
  import converters._

  implicit val matchDuplicateCacheStoreSupport =
    new MatchDuplicateCacheStoreSupport(
      readOnly,
      useIdFromValue,
      dontUpdateTime
    )
  implicit val matchDuplicateResultCacheStoreSupport =
    new MatchDuplicateResultCacheStoreSupport(
      readOnly,
      useIdFromValue,
      dontUpdateTime
    )
  implicit val matchChicagoCacheStoreSupport =
    new MatchChicagoCacheStoreSupport(readOnly, useIdFromValue, dontUpdateTime)
  implicit val matchRubberCacheStoreSupport =
    new MatchRubberCacheStoreSupport(readOnly, useIdFromValue, dontUpdateTime)
  implicit val boardSetCacheStoreSupport =
    new BoardSetCacheStoreSupport(readOnly, dontUpdateTime)
  implicit val movementCacheStoreSupport =
    new MovementCacheStoreSupport(readOnly, dontUpdateTime)

}

object BridgeResources {
  def apply(
      yaml: Boolean = true,
      readOnly: Boolean = false,
      useIdFromValue: Boolean = false,
      dontUpdateTime: Boolean = false
  ) = new BridgeResources(yaml, readOnly, useIdFromValue, dontUpdateTime)
}
