package com.github.thebridsk.bridge.data

import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.data.SystemTime.Timestamp

import scala.annotation.meta._
import io.swagger.v3.oas.annotations.media.Schema

/**
  * @author werewolf
  *
  * @param id the board number
  * @param nsVul true if NS is vulnerable
  * @param ewVul true if EW is vulnerable
  * @param dealer the dealer on the board
  * @param hands map nsTeam -> DuplicateHand
  */
@Schema(
  title = "A board from a duplicate match",
  description = "A board from a duplicate match"
)
case class BoardV1(
    @Schema(
      description = "The ID of the board",
      required = true,
      implementation = classOf[String]
    )
    id: Id.DuplicateBoard,
    @Schema(
      description = "True if NS is vulnerable on the board",
      required = true
    )
    nsVul: Boolean,
    @Schema(
      description = "True if EW is vulnerable on the board",
      required = true
    )
    ewVul: Boolean,
    @Schema(
      description = "the dealer for the board",
      required = true,
      allowableValues = Array("N", "S", "E", "W"),
      `type` = "enum"
    )
    dealer: String,
    @Schema(
      description =
        "The duplicate hands for the board, the key is the team ID of the NS team.",
      required = true
    )
    hands: Map[Id.DuplicateHand, DuplicateHandV1],
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

  def equalsIgnoreModifyTime(other: BoardV1) =
    id == other.id &&
      nsVul == other.nsVul &&
      ewVul == other.ewVul &&
      dealer == other.dealer && equalsInHands(other)

  def equalsInHands(other: BoardV1) = {
    if (hands.keySet == other.hands.keySet) {
      hands.keys.find { key =>
        {
          // this function returns true if the values in the two maps are not equal
          hands.get(key) match {
            case Some(me) =>
              other.hands.get(key) match {
                case Some(ome) => !me.equalsIgnoreModifyTime(ome)
                case None      => true
              }
            case None =>
              other.hands.get(key) match {
                case Some(ome) =>
                  true
                case None =>
                  false
              }
          }
        }
      }.isEmpty
    } else {
      false
    }
  }

  def setId(newId: Id.DuplicateBoard, forCreate: Boolean) = {
    val time = SystemTime.currentTimeMillis()
    copy(
      id = newId,
      created = if (forCreate) time; else created,
      updated = time
    )
  }

  def timesPlayed = hands.filter(dh => dh._2.wasPlayed).size

  def handPlayedByTeam(team: Id.Team) = hands.values.collectFirst {
    case hand: DuplicateHandV1 if hand.isTeam(team) => hand
  }

  def wasPlayedByTeam(team: Id.Team) = !handPlayedByTeam(team).isEmpty

  def handTeamPlayNS(team: Id.Team) = hands.values.collectFirst {
    case hand: DuplicateHandV1 if hand.isNSTeam(team) => hand
  }

  def didTeamPlayNS(team: Id.Team) = !handTeamPlayNS(team).isEmpty

  def handTeamPlayEW(team: Id.Team) = hands.values.collectFirst {
    case hand: DuplicateHandV1 if hand.isEWTeam(team) => hand
  }

  def didTeamPlayEW(team: Id.Team) = !handTeamPlayEW(team).isEmpty

  def teamScore(team: Id.Team) =
    handPlayedByTeam(team) match {
      case Some(teamHand) =>
        def getNSTeam(hand: DuplicateHandV1) = hand.nsTeam
        def getEWTeam(hand: DuplicateHandV1) = hand.ewTeam
        def getNSScore(hand: DuplicateHandV1) = hand.score.ns
        def getEWScore(hand: DuplicateHandV1) = hand.score.ew
        val teamPlayedNS = teamHand.nsTeam == team
        teamScorePrivate(
          team,
          if (teamPlayedNS) getNSScore(teamHand); else getEWScore(teamHand),
          if (teamPlayedNS) getNSScore _; else getEWScore _,
          if (teamPlayedNS) getNSTeam _; else getEWTeam _
        )

      case _ => 0
    }

  private[this] def teamScorePrivate(
      team: Id.Team,
      score: Int,
      getScoreFromHand: (DuplicateHandV1) => Int,
      getTeam: (DuplicateHandV1) => Id.Team
  ) = {
    hands.values
      .filter(hand => getTeam(hand) != team)
      .map(hand => {
        val otherscore = getScoreFromHand(hand)
        if (score == otherscore) 0.5f
        else if (score > otherscore) 1.0f
        else 0.0f
      })
      .reduce(_ + _)
  }

  def copyForCreate(id: Id.DuplicateBoard) = {
    val time = SystemTime.currentTimeMillis()
    val xhands = hands.map(e => (e._1 -> e._2.copyForCreate(e._1))).toMap
    copy(id = id, created = time, updated = time, hands = xhands)
  }

  def updateHand(hand: DuplicateHandV1): BoardV1 =
    copy(
      hands = hands + (hand.id -> hand),
      updated = SystemTime.currentTimeMillis()
    )
  def updateHand(handId: Id.DuplicateHand, hand: Hand): BoardV1 =
    hands.get(handId) match {
      case Some(dh) => updateHand(dh.updateHand(hand))
      case None =>
        throw new IndexOutOfBoundsException("Hand " + handId + " not found")
    }

  def setHands(hands: Map[Id.DuplicateHand, DuplicateHandV1]) = {
    copy(hands = hands, updated = SystemTime.currentTimeMillis())
  }

  def deleteHand(handId: Id.DuplicateHand) = {
    val nb = hands - id
    copy(hands = nb, updated = SystemTime.currentTimeMillis())

  }

  @Schema(hidden = true)
  def getBoardInSet =
    BoardInSet(Id.boardIdToBoardNumber(id).toInt, nsVul, ewVul, dealer)

  @Schema(hidden = true)
  def convertToCurrentVersion =
    BoardV2(
      id,
      nsVul,
      ewVul,
      dealer,
      hands.values.map(h => h.convertToCurrentVersion).toList,
      created,
      updated
    )

}

object BoardV1 {
  def create(
      id: Id.DuplicateBoard,
      nsVul: Boolean,
      ewVul: Boolean,
      dealer: String,
      hands: Map[Id.DuplicateHand, DuplicateHandV1]
  ) = {
    val time = SystemTime.currentTimeMillis()
    new BoardV1(id, nsVul, ewVul, dealer, hands, time, time)
  }
}
