package com.github.thebridsk.bridge.backend

import com.github.thebridsk.bridge.backend.resource.NestedResourceSupport
import com.github.thebridsk.bridge.backend.resource.Result
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.Id
import akka.http.scaladsl.model.StatusCodes
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.backend.resource.StoreSupport
import com.github.thebridsk.bridge.backend.resource.VersionedInstanceJson
import com.github.thebridsk.bridge.backend.resource.PersistentSupport
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.data.VersionedInstance
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.backend.resource.Resource
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.github.thebridsk.bridge.data.Round
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.RubberHand

class GenericIdCacheStoreSupport[VId, VType <: VersionedInstance[
  VType,
  VType,
  VId
]](
    idprefix: String,
    resourceName: String,
    resourceURI: String,
    readOnly: Boolean,
    useIdFromValue: Boolean = false,
    dontUpdateTime: Boolean = false
)(
    implicit
    instanceJson: VersionedInstanceJson[VId, VType]
) extends StoreSupport[VId, VType](
      idprefix,
      resourceName,
      resourceURI,
      readOnly,
      useIdFromValue,
      dontUpdateTime
    ) {

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
    instanceJson: VersionedInstanceJson[Id.MatchDuplicate, MatchDuplicate]
) extends GenericIdCacheStoreSupport[Id.MatchDuplicate, MatchDuplicate](
      "M",
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
      Id.MatchDuplicateResult,
      MatchDuplicateResult
    ]
) extends GenericIdCacheStoreSupport[
      Id.MatchDuplicateResult,
      MatchDuplicateResult
    ](
      "E",
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
    instanceJson: VersionedInstanceJson[Id.MatchChicago, MatchChicago]
) extends GenericIdCacheStoreSupport[Id.MatchChicago, MatchChicago](
      "C",
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
    instanceJson: VersionedInstanceJson[String, MatchRubber]
) extends GenericIdCacheStoreSupport[String, MatchRubber](
      "R",
      "MatchRubber",
      "/rubbers",
      readOnly,
      useIdFromValue,
      dontUpdateTime
    )

class GenericIdFromInstanceCacheStoreSupport[VId, VType <: VersionedInstance[
  VType,
  VType,
  VId
]](
    resourceName: String,
    resourceURI: String,
    readOnly: Boolean,
    dontUpdateTime: Boolean = false
)(
    implicit
    instanceJson: VersionedInstanceJson[VId, VType]
) extends StoreSupport[VId, VType](
      "",
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
      persistent: PersistentSupport[VId, VType],
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

  def stringToId(s: String): Option[VId] = {
    Some(s.asInstanceOf[VId])
  }

}

class BoardSetCacheStoreSupport(
    readOnly: Boolean,
    dontUpdateTime: Boolean = false
)(
    implicit
    instanceJson: VersionedInstanceJson[String, BoardSet]
) extends GenericIdFromInstanceCacheStoreSupport[String, BoardSet](
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
    instanceJson: VersionedInstanceJson[String, Movement]
) extends GenericIdFromInstanceCacheStoreSupport[String, Movement](
      "Movement",
      "/movements",
      readOnly,
      dontUpdateTime
    ) {}

object DuplicateTeamsNestedResource
    extends NestedResourceSupport[MatchDuplicate, Id.Team, Team] {
  val resourceURI = "teams"

  def getResources(
      parent: MatchDuplicate,
      parentResource: String
  ): Result[Map[Id.Team, Team]] =
    Result(parent.teams.map(t => t.id -> t).toMap)

  def getResource(
      parent: MatchDuplicate,
      parentResource: String,
      id: Id.Team
  ): Result[Team] =
    parent
      .getTeam(id)
      .map(t => Result(t))
      .getOrElse(notFound(parentResource, id))

  def updateResources(
      parent: MatchDuplicate,
      parentResource: String,
      map: Map[Id.Team, Team]
  ): Result[MatchDuplicate] =
    Result(parent.setTeams(map.values.toList))

  def updateResource(
      parent: MatchDuplicate,
      parentResource: String,
      id: Id.Team,
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
      id: Id.Team
  ): Result[(MatchDuplicate, Team)] = {
    parent
      .getTeam(id)
      .map(t => Result((parent.deleteTeam(id), t)))
      .getOrElse(notFound(parentResource, id))
  }
}

object DuplicateBoardsNestedResource
    extends NestedResourceSupport[MatchDuplicate, Id.DuplicateBoard, Board] {
  val resourceURI = "boards"

  def getResources(
      parent: MatchDuplicate,
      parentResource: String
  ): Result[Map[Id.DuplicateBoard, Board]] =
    Result(parent.boards.map(t => t.id -> t).toMap)

  def getResource(
      parent: MatchDuplicate,
      parentResource: String,
      id: Id.DuplicateBoard
  ): Result[Board] =
    parent
      .getBoard(id)
      .map(t => Result(t))
      .getOrElse(notFound(parentResource, id))

  def updateResources(
      parent: MatchDuplicate,
      parentResource: String,
      map: Map[Id.DuplicateBoard, Board]
  ): Result[MatchDuplicate] =
    Result(parent.setBoards(map.values.toList))

  def updateResource(
      parent: MatchDuplicate,
      parentResource: String,
      id: Id.DuplicateBoard,
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
      id: Id.DuplicateBoard
  ): Result[(MatchDuplicate, Board)] = {
    parent
      .getBoard(id)
      .map(t => Result((parent.deleteBoard(id), t)))
      .getOrElse(notFound(parentResource, id))
  }
}

object DuplicateHandsNestedResource
    extends NestedResourceSupport[Board, Id.Team, DuplicateHand] {
  val resourceURI = "hands"

  def getResources(
      parent: Board,
      parentResource: String
  ): Result[Map[Id.Team, DuplicateHand]] =
    Result(parent.hands.map(t => t.id -> t).toMap)

  def getResource(
      parent: Board,
      parentResource: String,
      id: Id.Team
  ): Result[DuplicateHand] =
    parent
      .getHand(id)
      .map(t => Result(t))
      .getOrElse(notFound(parentResource, id))

  def updateResources(
      parent: Board,
      parentResource: String,
      map: Map[Id.Team, DuplicateHand]
  ): Result[Board] =
    Result(parent.setHands(map.values.toList))

  def updateResource(
      parent: Board,
      parentResource: String,
      id: Id.Team,
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
      id: Id.Team
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
      val r: Resource[Id.MatchDuplicate, MatchDuplicate]
  ) extends AnyVal {
    def resourceBoards(implicit execute: ExecutionContext) =
      r.nestedResource(DuplicateBoardsNestedResource)
    def resourceTeams(implicit execute: ExecutionContext) =
      r.nestedResource(DuplicateTeamsNestedResource)
  }

  implicit class WrapBoardResource(val r: Resource[Id.DuplicateBoard, Board])
      extends AnyVal {
    def resourceHands(implicit execute: ExecutionContext) =
      r.nestedResource(DuplicateHandsNestedResource)
  }

  implicit class WrapMatchChicagoResource(
      val r: Resource[Id.MatchChicago, MatchChicago]
  ) extends AnyVal {
    def resourceRounds(implicit execute: ExecutionContext) =
      r.nestedResource(ChicagoRoundNestedResource)
  }

  implicit class WrapMatchChicagoRoundResource(val r: Resource[String, Round])
      extends AnyVal {
    def resourceHands(implicit execute: ExecutionContext) =
      r.nestedResource(ChicagoRoundHandNestedResource)
  }

  implicit class WrapMatchRubberResource(val r: Resource[String, MatchRubber])
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
