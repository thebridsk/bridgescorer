package com.example.data.chicago

import com.example.data._
import com.example.data.bridge.DuplicateBridge.ScoreHand
import com.example.data.bridge.PlayerPosition
import com.example.data.bridge.North
import com.example.data.bridge.East
import com.example.data.bridge.South
import com.example.data.bridge.West

class RoundScoring(val round: Round) {
  val hands = round.hands.map { h =>
    ScoreHand(h)
  }.toArray
  val dealerFirstRound = PlayerPosition(round.dealerFirstRound)

  val players = Array(round.north, round.south, round.east, round.west)

  val dealerOrder = {
    var before: List[PlayerPosition] = Nil
    var after = RoundScoring.definedDealerOrder

    while (after.headOption.isDefined && after.head != dealerFirstRound) {
      val current = after.head
      before = current :: before
      after = after.tail
    }
    before = before.reverse
    (after ::: before).toArray
  }

  def dealerForHand(hand: Int) = {
    val h = (hand - 1) % 4
    dealerOrder(h)
  }

  /**
    * totals - array of player totals.  the index corresponds to the index in players
    * byHands - the first index is the hand (same as hands index), the second
    * is player (same as players index)
    */
  val (totals, byHands) = calculate()

  private def calculate(): (Array[Int], Array[Array[Int]]) = {
    val totals = Array(0, 0, 0, 0)
    val len = hands.length
    var byHands: List[Array[Int]] = Nil

    for (i <- 0 until len) {
      val nsscore = if (hands(i).nsScored) hands(i).score.ns; else 0
      val ewscore = if (hands(i).nsScored) 0; else hands(i).score.ew

      val scores = Array(nsscore, nsscore, ewscore, ewscore)
      byHands = scores :: byHands
      totals(0) += nsscore
      totals(1) += nsscore
      totals(2) += ewscore
      totals(3) += ewscore

    }
    (totals, byHands.reverse.toArray)

  }
}

object RoundScoring {
  def apply(round: Round) = new RoundScoring(round)

  val definedDealerOrder = North :: East :: South :: West :: Nil
}
