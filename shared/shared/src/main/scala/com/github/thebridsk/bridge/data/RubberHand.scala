package com.github.thebridsk.bridge.data

import com.github.thebridsk.bridge.data.SystemTime.Timestamp
import io.swagger.v3.oas.annotations.media.Schema

/**
  * @author werewolf
  */
@Schema(
  title = "RubberHand - The result of playing a hand",
  description = "The result of playing a hand"
)
case class RubberHand(
    @Schema(description = "The ID of a hand", required = true)
    id: String,
    @Schema(description = "the played hand", required = true)
    hand: Hand,
    @Schema(description = "The number of honor points given", required = true)
    honors: Int,
    @Schema(
      description =
        "The player that got the honors points, Must be specified if honors not equal to 0, ignored otherwise",
      allowableValues = Array("N", "S", "E", "W"),
      required = false
    )
    honorsPlayer: Option[String],
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

  def equalsIgnoreModifyTime(other: RubberHand): Boolean = {
    other.id == id &&
    other.honors == honors &&
    other.honorsPlayer == honorsPlayer &&
    other.hand.equalsIgnoreModifyTime(hand)
  }

  def setId(newId: String, forCreate: Boolean): RubberHand = {
    val time = SystemTime.currentTimeMillis()
    copy(
      id = newId,
      hand = hand.setId(newId, forCreate),
      created = if (forCreate) time; else created,
      updated = time
    )
  }

  def copyForCreate(id: String): RubberHand = {
    val time = SystemTime.currentTimeMillis()
    copy(id = id, hand = hand.copyForCreate(id), created = time, updated = time)
  }

  def contractTricks = hand.contractTricks
  def contractSuit = hand.contractSuit
  def contractDoubled = hand.contractDoubled
  def declarer = hand.declarer
  def nsVul = hand.nsVul
  def ewVul = hand.ewVul
  def madeContract = hand.madeContract
  def tricks = hand.tricks

}

object RubberHand {
  def create(
      id: String,
      hand: Hand,
      honors: Int,
      honorsPlayer: Option[String],
      created: Timestamp,
      updated: Timestamp
  ) =
    new RubberHand(id, hand, honors, honorsPlayer, created, updated)

}
