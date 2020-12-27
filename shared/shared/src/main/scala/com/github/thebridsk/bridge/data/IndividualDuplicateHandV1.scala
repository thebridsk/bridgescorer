package com.github.thebridsk.bridge.data

import com.github.thebridsk.bridge.data.SystemTime.Timestamp
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  name = "DuplicateHand",
  title = "DuplicateHand - A hand from a duplicate match",
  description =
    "A hand from a duplicate match, it may or may not have been played."
)
case class IndividualDuplicateHandV1 private (
    @Schema(
      description = "the Id of the hand",
      required = true
    )
    id: IndividualDuplicateHand.Id,
    @Schema(
      description =
        "The played hand.  If missing indicates the hand has not been played.",
      required = false
    )
    played: Option[Hand],
    @Schema(
      description = "The table id of where the hand is played",
      required = true
    )
    table: Table.Id,
    @Schema(
      description = "The round the hand is played in",
      required = true,
      minimum = "1"
    )
    round: Int,
    @Schema(description = "The board id", required = true)
    board: IndividualBoard.Id,
    @Schema(
      description = "The north player.",
      required = true
    )
    north: Int,
    @Schema(
      description = "The south player.",
      required = true
    )
    south: Int,
    @Schema(
      description = "The east player.",
      required = true
    )
    east: Int,
    @Schema(
      description = "The west player.",
      required = true
    )
    west: Int,
    created: Timestamp,
    @Schema(
      description =
        "When the duplicate hand was last updated, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    updated: Timestamp
) {

  def equalsIgnoreModifyTime(other: IndividualDuplicateHandV1): Boolean =
    table == other.table &&
      round == other.round &&
      board == other.board &&
      north == other.north &&
      south == other.south &&
      east == other.east &&
      west == other.west &&
      handEquals(other)

  def wasPlayed: Boolean = !played.isEmpty

  def handEquals(other: IndividualDuplicateHandV1): Boolean = {
    played match {
      case Some(h) =>
        other.played match {
          case Some(oh) =>
            h.equalsIgnoreModifyTime(oh)
          case _ =>
            false
        }
      case _ =>
        other.played match {
          case Some(oh) =>
            false
          case _ =>
            true
        }
    }
  }

  def setId(newId: IndividualDuplicateHand.Id, forCreate: Boolean): IndividualDuplicateHandV1 = {
    val time = SystemTime.currentTimeMillis()
    copy(
      id = newId,
      created = if (forCreate) time; else created,
      updated = time
    )
  }

  def copyForCreate(id: IndividualDuplicateHand.Id): IndividualDuplicateHandV1 = {
    val time = SystemTime.currentTimeMillis()
    copy(
      id = id,
      created = time,
      updated = time,
      played = played
    )
  }

  def isPlayer(player: Int): Boolean = {
    player == north || player == south || player == east || player == west
  }

  def updateHand(newhand: Hand): IndividualDuplicateHandV1 =
    copy(played = Some(newhand), updated = SystemTime.currentTimeMillis())

  @Schema(hidden = true)
  def convertToCurrentVersion: IndividualDuplicateHandV1 = this

}

trait IdIndividualDuplicateHand

object IndividualDuplicateHandV1 extends HasId[IdIndividualDuplicateHand]("p", true) {

  /**
    * Create an IndividualDuplicateHand.
    * The id is determined from the north player.
    *
    * @param played
    * @param table
    * @param round
    * @param board
    * @param north
    * @param south
    * @param east
    * @param west
    * @param created
    * @param updated
    * @return the hand object.
    */
  def apply(
      played: Option[Hand],
      table: Table.Id,
      round: Int,
      board: IndividualBoard.Id,
      north: Int,
      south: Int,
      east: Int,
      west: Int,
      created: Timestamp = SystemTime.UseCurrent,
      updated: Timestamp = SystemTime.UseCurrent
  ): IndividualDuplicateHandV1 = {
    val (cre,upd) = SystemTime.defaultTime(created,updated)
    new IndividualDuplicateHandV1(
      IndividualDuplicateHand.id(north),
      played,
      table,
      round,
      board,
      north,
      south,
      east,
      west,
      cre,
      upd
    )
  }

  def create(
      table: Table.Id,
      round: Int,
      board: IndividualBoard.Id,
      north: Int,
      south: Int,
      east: Int,
      west: Int,
      created: Timestamp = SystemTime.UseCurrent,
      updated: Timestamp = SystemTime.UseCurrent
  ): IndividualDuplicateHandV1 = {
    IndividualDuplicateHandV1(
      None,
      table,
      round,
      board,
      north,
      south,
      east,
      west,
      created,
      updated
    )
  }

  def sort(l: IndividualDuplicateHandV1, r: IndividualDuplicateHandV1): Boolean = {
    l.id < r.id
  }
}
