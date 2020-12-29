package com.github.thebridsk.bridge.data

import com.github.thebridsk.bridge.data.SystemTime.Timestamp

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
  name = "IndividualBoard",
  title = "IndividualBoard - Represents a board of a duplicate match.",
  description =
    "A board from a duplicate match, contains all the played hands on the board."
)
case class IndividualBoardV1 private (
    @Schema(
      description = "The ID of the board",
      required = true,
      implementation = classOf[String]
    )
    id: IndividualBoard.Id,
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
    hands: List[IndividualDuplicateHandV1],
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
  def equalsIgnoreModifyTime(other: IndividualBoardV1): Boolean =
    id == other.id &&
      nsVul == other.nsVul &&
      ewVul == other.ewVul &&
      dealer == other.dealer && equalsInHands(other)

  def equalsInHands(other: IndividualBoardV1, throwit: Boolean = false): Boolean = {
    if (hands.length == other.hands.length) {
      hands.find { t1 =>
        // this function must return true if t1 is NOT in other.team
        val rc = other.hands.find { t2 =>
          t1.id == t2.id && t1.equalsIgnoreModifyTime(t2)
        }.isEmpty
        if (!rc && throwit)
          throw new Exception(
            "IndividualBoardV1 other did not have hand equal to: " + t1
          )
        rc
      }.isEmpty
    } else {
      if (throwit)
        throw new Exception(
          "IndividualBoardV1 hands don't have same key: " + hands.map { t =>
            t.id
          } + " " + other.hands.map { t =>
            t.id
          }
        )
      false
    }
  }

  def setId(newId: IndividualBoard.Id, forCreate: Boolean): IndividualBoardV1 = {
    val time = SystemTime.currentTimeMillis()
    copy(
      id = newId,
      created = if (forCreate) time; else created,
      updated = time
    )
  }

  def playedHands: List[IndividualDuplicateHandV1] = hands.filter(dh => dh.wasPlayed)

  def timesPlayed: Int = hands.filter(dh => dh.wasPlayed).size

  /**
    * @param player
    * @return a hand that was played by the specified player
    */
  def handPlayedByPlayer(player: Int): Option[IndividualDuplicateHandV1] =
    hands.collectFirst {
      case hand: IndividualDuplicateHandV1 if hand.isPlayer(player) && hand.played.isDefined => hand
    }

  /**
    * @param player
    * @return true if board was played by the specified player
    */
  def wasPlayedByPlayer(player: Int): Boolean = !handPlayedByPlayer(player).isEmpty

  /**
    * @param players
    * @return true if all players played the board.
    */
  def wasPlayedByPlayers(players: List[Int]): Boolean = {
    players
      .find(!wasPlayedByPlayer(_))
      .isEmpty
  }

  def copyForCreate(id: IndividualBoard.Id): IndividualBoardV1 = {
    val time = SystemTime.currentTimeMillis()
    val xhands = hands.map(e => e.copyForCreate(e.id))
    copy(
      id = id,
      created = time,
      updated = time,
      hands = xhands.sortWith(IndividualDuplicateHandV1.sort)
    )
  }

  @Schema(hidden = true)
  def getHand(handid: IndividualDuplicateHand.Id): Option[IndividualDuplicateHand] = {
    hands.find(idh => idh.id == handid)
  }

  def updateHand(hand: IndividualDuplicateHandV1): IndividualBoardV1 = {
    val nb = hands.map { b =>
      if (b.id == hand.id) (true, hand) else (false, b)
    }
    val nb1 = nb.foldLeft((false, List[IndividualDuplicateHandV1]()))((ag, b) =>
      (ag._1 || b._1, b._2 :: ag._2)
    )
    val nb2 = if (nb1._1) nb1._2 else hand :: nb1._2
    copy(
      hands = nb2.sortWith(IndividualDuplicateHandV1.sort),
      updated = SystemTime.currentTimeMillis()
    )
  }
  def updateHand(handId: IndividualDuplicateHand.Id, hand: Hand): IndividualBoardV1 =
    getHand(handId) match {
      case Some(dh) => updateHand(dh.updateHand(hand))
      case None =>
        throw new IndexOutOfBoundsException("Hand " + handId + " not found")
    }

  def setHands(nhands: List[IndividualDuplicateHand]): IndividualBoardV1 = {
    copy(hands = nhands, updated = SystemTime.currentTimeMillis())
  }

  def deleteHand(handId: IndividualDuplicateHand.Id): IndividualBoardV1 = {
    val nb = hands.filter(h => h.id != handId)
    copy(hands = nb, updated = SystemTime.currentTimeMillis())

  }

  @Schema(hidden = true)
  def getBoardInSet: BoardInSet =
    BoardInSet(id.toInt, nsVul, ewVul, dealer)

  @Schema(hidden = true)
  def convertToCurrentVersion: IndividualBoardV1 = this

}

trait IdIndividualBoard

object IndividualBoardV1 extends HasId[IdIndividualBoard]("B") {

  def sort(l: IndividualBoardV1, r: IndividualBoardV1): Boolean =
    l.id < r.id

  def apply(
      id: IndividualBoard.Id,
      nsVul: Boolean,
      ewVul: Boolean,
      dealer: String,
      hands: List[IndividualDuplicateHandV1],
      created: Timestamp = SystemTime.UseCurrent,
      updated: Timestamp = SystemTime.UseCurrent
  ): IndividualBoardV1 = {
    val (cre,upd) = SystemTime.defaultTime(created,updated)
    new IndividualBoardV1(
      id,
      nsVul,
      ewVul,
      dealer,
      hands.sortWith(IndividualDuplicateHand.sort),
      cre,
      upd
    )
  }

}
