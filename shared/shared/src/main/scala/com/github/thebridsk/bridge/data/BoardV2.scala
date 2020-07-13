package com.github.thebridsk.bridge.data

import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.data.SystemTime.Timestamp

import scala.annotation.meta._
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema

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
  name = "Board",
  title = "Board - Represents a board of a duplicate match.",
  description =
    "A board from a duplicate match, contains all the played hands on the board."
)
case class BoardV2 private (
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
    @ArraySchema(
      minItems = 0,
      uniqueItems = true,
      schema = new Schema(implementation = classOf[DuplicateHandV2]),
      arraySchema = new Schema(
        description = "All duplicate hands for the board",
        required = true
      )
    )
    hands: List[DuplicateHandV2],
    @Schema(
      description =
        "When the board was created, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    created: Timestamp,
    @Schema(
      description =
        "When the board was last updated, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    updated: Timestamp
) {
  def equalsIgnoreModifyTime(other: BoardV2) =
    id == other.id &&
      nsVul == other.nsVul &&
      ewVul == other.ewVul &&
      dealer == other.dealer && equalsInHands(other)

  def equalsInHands(other: BoardV2, throwit: Boolean = false) = {
    if (hands.length == other.hands.length) {
      hands.find { t1 =>
        // this function must return true if t1 is NOT in other.team
        val rc = other.hands.find { t2 =>
          t1.id == t2.id && t1.equalsIgnoreModifyTime(t2)
        }.isEmpty
        if (!rc && throwit)
          throw new Exception(
            "MatchDuplicateV3 other did not have hand equal to: " + t1
          )
        rc
      }.isEmpty
    } else {
      if (throwit)
        throw new Exception(
          "MatchDuplicateV3 hands don't have same key: " + hands.map { t =>
            t.id
          } + " " + other.hands.map { t =>
            t.id
          }
        )
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

  def playedHands = hands.filter(dh => dh.wasPlayed)

  def timesPlayed = hands.filter(dh => dh.wasPlayed).size

  def handPlayedByTeam(team: Team.Id) = hands.collectFirst {
    case hand: DuplicateHandV2 if hand.isTeam(team) => hand
  }

  def wasPlayedByTeam(team: Team.Id) = !handPlayedByTeam(team).isEmpty

  def handTeamPlayNS(team: Team.Id) = hands.collectFirst {
    case hand: DuplicateHandV2 if hand.isNSTeam(team) => hand
  }

  def didTeamPlayNS(team: Team.Id) = !handTeamPlayNS(team).isEmpty

  def handTeamPlayEW(team: Team.Id) = hands.collectFirst {
    case hand: DuplicateHandV2 if hand.isEWTeam(team) => hand
  }

  def didTeamPlayEW(team: Team.Id) = !handTeamPlayEW(team).isEmpty

  def teamScore(team: Team.Id) =
    handPlayedByTeam(team) match {
      case Some(teamHand) =>
        def getNSTeam(hand: DuplicateHandV2) = hand.nsTeam
        def getEWTeam(hand: DuplicateHandV2) = hand.ewTeam
        def getNSScore(hand: DuplicateHandV2) = hand.score.ns
        def getEWScore(hand: DuplicateHandV2) = hand.score.ew
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
      team: Team.Id,
      score: Int,
      getScoreFromHand: (DuplicateHandV2) => Int,
      getTeam: (DuplicateHandV2) => Team.Id
  ) = {
    hands
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
    val xhands = hands.map(e => e.copyForCreate(e.id))
    copy(
      id = id,
      created = time,
      updated = time,
      hands = xhands.sortWith(BoardV2.sort)
    )
  }

  def updateHand(hand: DuplicateHandV2): BoardV2 = {
    val nb = hands.map { b =>
      if (b.id == hand.id) (true, hand) else (false, b)
    }
    val nb1 = nb.foldLeft((false, List[DuplicateHandV2]()))(
      (ag, b) => (ag._1 || b._1, b._2 :: ag._2)
    )
    val nb2 = if (nb1._1) nb1._2 else hand :: nb1._2
    copy(
      hands = nb2.sortWith(BoardV2.sort),
      updated = SystemTime.currentTimeMillis()
    )
  }
  def updateHand(handId: Team.Id, hand: Hand): BoardV2 =
    getHand(handId) match {
      case Some(dh) => updateHand(dh.updateHand(hand))
      case None =>
        throw new IndexOutOfBoundsException("Hand " + handId + " not found")
    }

  def setHands(nhands: List[DuplicateHandV2]) = {
    copy(hands = nhands, updated = SystemTime.currentTimeMillis())
  }

  def deleteHand(handId: Team.Id) = {
    val nb = hands.filter(h => h.id != handId)
    copy(hands = nb, updated = SystemTime.currentTimeMillis())

  }

  @Schema(hidden = true)
  def getHand(handId: Team.Id) = {
    hands.find(h => h.id == handId)
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
      hands.map(h => h.convertToCurrentVersion),
      created,
      updated
    )

}

trait IdBoard

object BoardV2 extends HasId[IdBoard]("B") {
  def create(
      id: Id.DuplicateBoard,
      nsVul: Boolean,
      ewVul: Boolean,
      dealer: String,
      hands: List[DuplicateHandV2]
  ) = {
    val time = SystemTime.currentTimeMillis()
    BoardV2(id, nsVul, ewVul, dealer, hands, time, time)
  }

  def sort(l: DuplicateHandV2, r: DuplicateHandV2) =
    l.id < r.id

  def apply(
      id: Id.DuplicateBoard,
      nsVul: Boolean,
      ewVul: Boolean,
      dealer: String,
      hands: List[DuplicateHandV2],
      created: Timestamp,
      updated: Timestamp
  ) = {
    new BoardV2(
      id,
      nsVul,
      ewVul,
      dealer,
      hands.sortWith(sort),
      created,
      updated
    )
  }

}
