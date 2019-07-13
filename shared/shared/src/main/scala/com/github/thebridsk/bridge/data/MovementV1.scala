package com.github.thebridsk.bridge.data

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema

/**
  * <pre><code>
  * {
  *   "name": "Armonk2Tables",
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
    name: String,
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
    hands: List[HandInTable]
) extends VersionedInstance[MovementV1, MovementV1, String] {

  def id = name

  def setId(
      newId: String,
      forCreate: Boolean,
      dontUpdateTime: Boolean = false
  ) = {
    copy(name = newId)
  }

  def convertToCurrentVersion() = (true, this)

  def readyForWrite() = this

  def wherePlayed(board: Int): List[BoardPlayed] = {
    hands.flatMap { h =>
      if (h.boards.contains(board))
        BoardPlayed(board, h.table, h.round, h.ns, h.ew) :: Nil
      else Nil
    }
  }

  @Schema(hidden = true)
  def getBoards = {
    hands.flatMap(h => h.boards).distinct.sorted
  }
  @Schema(hidden = true)
  def getRoundForAllTables(round: Int) = {
    hands.filter { r =>
      r.round == round
    }.toList
  }

  @Schema(hidden = true)
  def allRounds = {
    hands.map(r => r.round).distinct
  }

  @Schema(hidden = true)
  def matchHasRelay = {
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
  def tableRoundRelay(itable: Int, iround: Int) = {
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
)