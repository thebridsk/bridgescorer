package com.github.thebridsk.bridge.data

import com.github.thebridsk.bridge.data.bridge.DuplicateBridge
import com.github.thebridsk.bridge.data.SystemTime.Timestamp
import com.github.thebridsk.bridge.data.bridge.DuplicateBridge.DuplicateScore

import scala.annotation.meta._
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A hand from a duplicate match")
case class DuplicateHandV1(
    @Schema(
      description = "The played hand.  The key must be the string \"hand\".",
      required = true
    )
    played: Map[String, Hand],
    @Schema(
      description = "The table id of where the hand is played",
      required = true
    )
    table: String,
    @Schema(
      description = "The round the hand is played in",
      required = true,
      minimum = "1"
    )
    round: Int,
    @Schema(description = "The board id", required = true)
    board: Id.DuplicateBoard,
    @Schema(
      description =
        "The team id of the team playing NS.  This is also the id of the DuplicateHand",
      required = true
    )
    nsTeam: Id.Team,
    @Schema(
      description = "true if player 1 of the NS team is the north player",
      required = true
    )
    nIsPlayer1: Boolean,
    @Schema(description = "The team id of the team playing EW", required = true)
    ewTeam: Id.Team,
    @Schema(
      description = "true if player 1 of the EW team is the east player",
      required = true
    )
    eIsPlayer1: Boolean,
    @Schema(
      description =
        "When the duplicate hand was created, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    created: Timestamp,
    @Schema(
      description =
        "When the duplicate hand was last updated, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    updated: Timestamp
) {

  def equalsIgnoreModifyTime(other: DuplicateHandV1) =
    table == other.table &&
      round == other.round &&
      board == other.board &&
      nsTeam == other.nsTeam &&
      nIsPlayer1 == other.nIsPlayer1 &&
      ewTeam == other.ewTeam &&
      eIsPlayer1 == other.eIsPlayer1 &&
      handEquals(other)

  def hand: Option[Hand] = played.get(DuplicateHandV1.handField)

  def wasPlayed = played.contains(DuplicateHandV1.handField)

  def handEquals(other: DuplicateHandV1) = {
    hand match {
      case Some(h) =>
        other.hand match {
          case Some(oh) =>
            h.equalsIgnoreModifyTime(oh)
          case _ =>
            false
        }
      case _ =>
        other.hand match {
          case Some(oh) =>
            false
          case _ =>
            true
        }
    }
  }

  def setId(newId: Id.DuplicateHand, forCreate: Boolean) = {
    val time = SystemTime.currentTimeMillis()
    copy(
      nsTeam = newId,
      created = if (forCreate) time; else created,
      updated = time
    )
  }

  def isNSTeam(team: Id.Team) = team == nsTeam
  def isEWTeam(team: Id.Team) = team == ewTeam

  def isTeam(team: Id.Team) = isNSTeam(team) || isEWTeam(team)

  def score = hand match {
    case Some(h) => DuplicateBridge.ScoreHand(h).score
    case _       => DuplicateScore(0, 0)
  }

  def id: Id.DuplicateHand = nsTeam

  def copyForCreate(id: Id.DuplicateHand) = {
    val time = SystemTime.currentTimeMillis()
    copy(
      nsTeam = id,
      created = time,
      updated = time,
      played = getMapFromHand(hand)
    )
  }

  private def getMapFromHand(original: Option[Hand]): Map[String, Hand] =
    original match {
      case Some(h) => Map(DuplicateHandV1.handField -> h.copyForCreate(h.id))
      case _       => Map()
    }

  def updateHand(newhand: Hand) =
    copy(
      played = Map(DuplicateHandV1.handField -> newhand),
      updated = SystemTime.currentTimeMillis()
    )

  def setPlayer1North(flag: Boolean) =
    copy(nIsPlayer1 = flag, updated = SystemTime.currentTimeMillis())
  def setPlayer1East(flag: Boolean) =
    copy(eIsPlayer1 = flag, updated = SystemTime.currentTimeMillis())

  @Schema(hidden = true)
  def convertToCurrentVersion() = {
    DuplicateHandV2(
      hand.toList,
      table,
      round,
      board,
      nsTeam,
      nIsPlayer1,
      ewTeam,
      eIsPlayer1,
      created,
      updated
    )
  }

}

object DuplicateHandV1 {
  def create(
      hand: Option[Hand],
      table: String,
      round: Int,
      board: Id.DuplicateBoard,
      nsTeam: Id.Team,
      ewTeam: Id.Team
  ) = {
    val time = SystemTime.currentTimeMillis()
    val nh = hand.map(h => handField -> h).toMap
    new DuplicateHandV1(
      nh,
      table,
      round,
      board,
      nsTeam,
      true,
      ewTeam,
      true,
      time,
      time
    )
  }

  def create(
      hand: Hand,
      table: String,
      round: Int,
      board: Id.DuplicateBoard,
      nsTeam: Id.Team,
      ewTeam: Id.Team
  ) = {
    val time = SystemTime.currentTimeMillis()
    new DuplicateHandV1(
      Map(handField -> hand),
      table,
      round,
      board,
      nsTeam,
      true,
      ewTeam,
      true,
      time,
      time
    )
  }

  def create(
      table: String,
      round: Int,
      board: Id.DuplicateBoard,
      nsTeam: Id.Team,
      ewTeam: Id.Team
  ) = {
    val time = SystemTime.currentTimeMillis()
    new DuplicateHandV1(
      Map(),
      table,
      round,
      board,
      nsTeam,
      true,
      ewTeam,
      true,
      time,
      time
    )
  }

  val handField = "hand"
}
