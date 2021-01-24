package com.github.thebridsk.bridge.data

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema

/**
  * <pre><code>
  * {
  *   "name": "2TablesArmonk",
  *   "short": "Armonk 2 Tables",
  *   "description": "2 tables, 18 hands, used by Armonk bridge group",
  *   "hands": [
  *     {
  *       "table": "1",
  *       "round": "1",
  *       "NS": "1",
  *       "EW": "2",
  *       "board": "1"
  *     },
  *     ...
  *   ]
  * }
  * </code></pre>
  */
@Schema(
  name = "Movement",
  title = "Movement - A movement for a duplicate bridge match",
  description = "A movements for a duplicate bridge match"
)
case class MovementV1(
    @Schema(description = "The name of the movement", required = true)
    name: MovementV1.Id,
    @Schema(
      description = "A short description of the movement",
      required = true
    )
    short: String,
    @Schema(
      description = "A longer description of the movement",
      required = true
    )
    description: String,
    @Schema(
      description = "The number of teams in the movement",
      required = true
    )
    numberTeams: Int,
    @ArraySchema(
      minItems = 1,
      uniqueItems = true,
      schema = new Schema(
        implementation = classOf[HandInTable],
        description =
          "A description of a round on a table, identifies NS and EW teams, and boards to play."
      ),
      arraySchema = new Schema(
        description = "All the round descriptions on all the tables.",
        required = true
      )
    )
    hands: List[HandInTable],
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
) extends VersionedInstance[MovementV1, MovementV1, MovementV1.Id]
  with MovementBase
{

  def id = name
  def nameAsString: String = name.id

  @Schema(hidden = true)
  override def getMovementId: Option[Movement.Id] = Some(name)

  @Schema(hidden = true)
  private def optional(flag: Boolean, fun: Movement => Movement) = {
    if (flag) fun(this)
    else this
  }

  def setId(
      newId: MovementV1.Id,
      forCreate: Boolean,
      dontUpdateTime: Boolean = false
  ): Movement = {
    val time = SystemTime.currentTimeMillis()
    copy(name = newId)
      .optional(
        forCreate,
        _.copy(creationTime = Some(time), updateTime = Some(time))
      )
      .optional(!dontUpdateTime, _.copy(updateTime = Some(time)))
  }

  def convertToCurrentVersion: (Boolean, MovementV1) = (true, this)

  def readyForWrite: MovementV1 = this

  def wherePlayed(board: Int): List[BoardPlayed] = {
    hands.flatMap { h =>
      if (h.boards.contains(board))
        BoardPlayed(board, h.table, h.round, h.ns, h.ew) :: Nil
      else Nil
    }
  }

  @Schema(hidden = true)
  def getBoards: List[Int] = {
    hands.flatMap(h => h.boards).distinct.sorted
  }
  @Schema(hidden = true)
  def getRoundForAllTables(round: Int): List[HandInTable] = {
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
  def getTables: List[List[HandInTable]] = {
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

trait IdMovement

object MovementV1 extends HasId[IdMovement]("", true) {
  def default: Id = Movement.id("2TablesArmonk")

  def standard: Id = Movement.id("Howell04T2B18")
}

case class BoardPlayed(board: Int, table: Int, round: Int, ns: Int, ew: Int)

@Schema(
  title = "HandInTable - Information about a round at a table",
  description = "Contains the NS and EW teams and boards in a round at a table"
)
case class HandInTable(
    @Schema(description = "The table number", minimum = "1", required = true)
    table: Int,
    @Schema(description = "The round number", minimum = "1", required = true)
    round: Int,
    @Schema(description = "The NS team number ", minimum = "1", required = true)
    ns: Int,
    @Schema(description = "The EW team number ", minimum = "1", required = true)
    ew: Int,
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
