package com.example.data

import io.swagger.annotations._
import scala.annotation.meta._
import com.example.data.SystemTime.Timestamp

/**
 * @author werewolf
 */
@ApiModel(description = "The result of playing a hand")
case class RubberHand(
    @(ApiModelProperty @field)(value="The ID of a hand", required=true)
    id: String,
    @(ApiModelProperty @field)(value="the hand", required=true)
    hand: Hand,
    @(ApiModelProperty @field)(value="The number of honor points given", required=true)
    honors: Int,
    @(ApiModelProperty @field)(value="The player that got the honors points", required=true)
    honorsPlayer: String,
    created: Timestamp,
    @(ApiModelProperty @field)(value="when the duplicate hand was last updated", required=true)
    updated: Timestamp ) {

  def equalsIgnoreModifyTime( other: RubberHand ) = {
    other.id == id &&
    other.honors == honors &&
    other.honorsPlayer == honorsPlayer &&
    other.hand.equalsIgnoreModifyTime(hand)
  }

  def setId( newId: String, forCreate: Boolean ) = {
      val time = SystemTime.currentTimeMillis()
      copy(id=newId, hand=hand.setId(newId, forCreate), created=if (forCreate) time; else created, updated=time)
    }

  def copyForCreate(id: String) = {
    val time = SystemTime.currentTimeMillis()
    copy( id=id, hand=hand.copyForCreate(id), created=time, updated=time )
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
  def create(id: String,
             hand: Hand,
             honors: Int,
             honorsPlayer: String,
             created: Timestamp,
             updated: Timestamp) =
         new RubberHand(id,hand,honors,honorsPlayer,created,updated)

}
