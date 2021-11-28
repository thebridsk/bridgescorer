package com.github.thebridsk.bridge.data

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema

/**
  * {{{
  * {
  *   "name": "2Tables",
  *   "short": "2 Tables Individual",
  *   "description": "2 tables, 18 hands",
  *   "numberPlayers": 8,
  *   "hands": [
  *     {
  *       "table": 1,
  *       "round": 1,
  *       "north": 1,
  *       "south": 3,
  *       "east":  3,
  *       "west":  4,
  *       "boards": [1, 2]
  *     },
  *     ...
  *   ]
  * }
  * }}}
  */
@Schema(
  name = "IndividualMovement",
  title = "IndividualMovement - A movement for a duplicate bridge match",
  description = "An individual movement for a duplicate bridge match"
)
case class IndividualMovementV1(
    @Schema(description = "The name of the individual movement", required = true)
    name: IndividualMovementV1.Id,
    @Schema(
      description = "A short description of the individual movement",
      required = true
    )
    short: String,
    @Schema(
      description = "A longer description of the individual movement",
      required = true
    )
    description: String,
    @Schema(
      description = "The number of players in the individual movement",
      required = true
    )
    numberPlayers: Int,
    @ArraySchema(
      minItems = 1,
      uniqueItems = true,
      schema = new Schema(
        implementation = classOf[IndividualHandInTable],
        description =
          "A description of a round on a table, identifies the players, and boards to play."
      ),
      arraySchema = new Schema(
        description = "All the round descriptions on all the tables.",
        required = true
      )
    )
    hands: List[IndividualHandInTable],
    @Schema(
      description = "true if movement can be deleted, default is false",
      required = false
    )
    deletable: Option[Boolean] = None,
    @Schema(
      description =
        "true if movement definition can be reset to the default, default is false",
      required = false
    )
    resetToDefault: Option[Boolean] = None,
    @Schema(
      description = "true if movement is disabled, default is false",
      required = false
    )
    disabled: Option[Boolean] = None,
    @Schema(
      description = "the creation time, default: unknown",
      required = false
    )
    creationTime: Option[SystemTime.Timestamp] = None,
    @Schema(
      description = "the last time the movement was updated, default: unknown",
      required = false
    )
    updateTime: Option[SystemTime.Timestamp] = None
) extends VersionedInstance[IndividualMovementV1, IndividualMovementV1, IndividualMovementV1.Id]
  with MovementBase
{

  def id = name
  def nameAsString: String = name.id

  @Schema(hidden = true)
  def getId: MovementBase.Id = name

  @Schema(hidden = true)
  override def getIndividualId: Option[IndividualMovement.Id] = Some(name)

  @Schema(hidden = true)
  private def optional(flag: Boolean, fun: IndividualMovement => IndividualMovement) = {
    if (flag) fun(this)
    else this
  }

  def setId(
      newId: IndividualMovementV1.Id,
      forCreate: Boolean,
      dontUpdateTime: Boolean = false
  ): IndividualMovement = {
    val time = SystemTime.currentTimeMillis()
    copy(name = newId)
      .optional(
        forCreate,
        _.copy(creationTime = Some(time), updateTime = Some(time))
      )
      .optional(!dontUpdateTime, _.copy(updateTime = Some(time)))
  }

  def convertToCurrentVersion: (Boolean, IndividualMovementV1) = (true, this)

  def readyForWrite: IndividualMovementV1 = this

  def wherePlayed(board: Int): List[IndividualBoardPlayed] = {
    hands.flatMap { h =>
      if (h.boards.contains(board))
        IndividualBoardPlayed(board, h.table, h.round, h.north, h.south, h.east, h.west) :: Nil
      else Nil
    }
  }

  @Schema(hidden = true)
  def getBoards: List[Int] = {
    hands.flatMap(h => h.boards).distinct.sorted
  }
  @Schema(hidden = true)
  def getRoundForAllTables(round: Int): List[IndividualHandInTable] = {
    hands.filter { r =>
      r.round == round
    }.toList
  }

  @Schema(hidden = true)
  def allRounds: List[Int] = {
    hands.map(r => r.round).distinct
  }

  @Schema(hidden = true)
  def matchHasRelay: Boolean = {
    allRounds.find { ir =>
      val all = getRoundForAllTables(ir).flatMap(r => r.boards)
      val distinct = all.distinct
      all.length != distinct.length
    }.isDefined
  }

  /**
    * @returns table IDs
    */
  @Schema(hidden = true)
  def tableRoundRelay(itable: Int, iround: Int): List[Int] = {
    val allRounds = getRoundForAllTables(iround)
    val otherRounds = allRounds.filter(r => r.table != itable)
    val otherBoards = otherRounds.flatMap(r => r.boards)
    allRounds
      .find(r => r.table == itable)
      .map { table =>
        val relays = table.boards
          .flatMap { bid =>
            otherRounds.flatMap { r =>
              r.boards.find(bs => bs == bid).map(bs => r.table)
            }
          }
          .distinct
          .sorted
        relays
      }
      .getOrElse(Nil)
  }

  @Schema(hidden = true)
  /**
    * Returns all the tables sorted by table number,
    * within each table all the rounds sorted by round number
    */
  def getTables: List[List[IndividualHandInTable]] = {
    hands
      .groupBy(_.table)
      .toList
      .sortWith((l, r) => l._1 < r._1)
      .map(_._2.sortWith((l, r) => l.round < r.round))
  }
  def tables: Int = getTables.size

  @Schema(hidden = true)
  def created: SystemTime.Timestamp = creationTime.getOrElse(0)

  @Schema(hidden = true)
  def updated: SystemTime.Timestamp = updateTime.getOrElse(0)

  @Schema(hidden = true)
  def isDeletable: Boolean = deletable.getOrElse(false)

  @Schema(hidden = true)
  def isDisabled: Boolean = disabled.getOrElse(false)

  @Schema(hidden = true)
  def isResetToDefault: Boolean = resetToDefault.getOrElse(false)

}

trait IdIndividualMovement extends IdMovementBase

object IndividualMovementV1 extends HasId[IdIndividualMovement]("", true) {
  def default: Id = IndividualMovement.id("Individual2Tables21Boards")
}

@Schema(
  title = "IndividualBoardPlayed - Information a board played in a round at a table",
  description = "Identifies the players for a board in a round at a table"
)
case class IndividualBoardPlayed(
    @Schema(description = "The board number ", minimum = "1", required = true)
    board: Int,
    @Schema(description = "The table number", minimum = "1", required = true)
    table: Int,
    @Schema(description = "The round number", minimum = "1", required = true)
    round: Int,
    @Schema(description = "The North player number", minimum = "1", required = true)
    north: Int,
    @Schema(description = "The South player number", minimum = "1", required = true)
    south: Int,
    @Schema(description = "The East player number", minimum = "1", required = true)
    east: Int,
    @Schema(description = "The West player number", minimum = "1", required = true)
    west: Int
)

@Schema(
  title = "IndividualHandInTable - Information about a round at a table",
  description = "Contains the players and boards in a round at a table"
)
case class IndividualHandInTable(
    @Schema(description = "The table number", minimum = "1", required = true)
    table: Int,
    @Schema(description = "The round number", minimum = "1", required = true)
    round: Int,
    @Schema(description = "The North player number", minimum = "1", required = true)
    north: Int,
    @Schema(description = "The South player number", minimum = "1", required = true)
    south: Int,
    @Schema(description = "The East player number", minimum = "1", required = true)
    east: Int,
    @Schema(description = "The West player number", minimum = "1", required = true)
    west: Int,
    @ArraySchema(
      minItems = 0,
      schema = new Schema(
        description = "The board number",
        minimum = "1",
        `type` = "number",
        format = "int32"
      ),
      arraySchema = new Schema(
        description =
          "The boards that are being played in this round at the table.",
        required = true
      )
    )
    boards: List[Int]
) {
  def tableid: Table.Id = Table.id(table)
}
