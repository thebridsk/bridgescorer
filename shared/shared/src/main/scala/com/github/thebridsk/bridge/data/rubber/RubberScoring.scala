package com.github.thebridsk.bridge.data.rubber

import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.data.bridge.RubberBridge.ScoreHand
import com.github.thebridsk.bridge.data.RubberHand
import com.github.thebridsk.bridge.data.bridge.PlayerPosition

class GameDone(msg: String = null, cause: Throwable = null)
    extends java.lang.Exception(msg, cause) {}

class GameScoring(
    val hands: List[RubberHand],
    val nsVul: Boolean,
    val ewVul: Boolean
) {
  val (nsAbove, nsBelow, ewAbove, ewBelow, scoredHands) = calculate

  val nsWon = nsBelow >= 100
  val ewWon = ewBelow >= 100

  val done = nsWon || ewWon

  /**
    * Add another hand to the game.
    * param h the hand to add
    * returns the new GameScoring object
    * throws GameDone if the game is already done
    */
  def add(h: RubberHand) = {
    if (done) throw new GameDone()
    new GameScoring((h :: hands.reverse).reverse, nsVul, ewVul)
  }

  /**
    * returns Tuple
    *            nsAbove: Int
    *            nsBelow: Int
    *            ewAbove: Int
    *            ewBelow: Int
    *            scoredHands: List[com.github.thebridsk.bridge.data.bridge.RubberBridge.ScoreHand]  // same index as hands
    *
    */
  private def calculate = {
    val sh = hands.map { h =>
      ScoreHand(h)
    }
    val (nsA, nsB, ewA, ewB) = sh
      .map { h =>
        h.getScores
      }
      .foldLeft((0, 0, 0, 0))(
        (a, v) => (a._1 + v._1, a._2 + v._2, a._3 + v._3, a._4 + v._4)
      )
    (nsA, nsB, ewA, ewB, sh)
  }

  /**
    * Get all the scores in the game
    * Returns a tuple of four lists of Ints.  The lists are:
    * nsAboveScores, nsBelowScores, ewAboveScores, ewBelowScores
    */
  def scores: (List[Int], List[Int], List[Int], List[Int]) = {

    val (na, nb, ea, eb) =
      scoredHands.foldLeft(
        (Nil: List[Int], Nil: List[Int], Nil: List[Int], Nil: List[Int])
      )((a, v) => {
        val sc = v.getScores
        (
          if (sc._1 != 0) sc._1 :: a._1 else a._1,
          if (sc._2 != 0) sc._2 :: a._2 else a._2,
          if (sc._3 != 0) sc._3 :: a._3 else a._3,
          if (sc._4 != 0) sc._4 :: a._4 else a._4
        )
      })
    (na, nb.reverse, ea, eb.reverse)
  }
}

class RubberScoring(val rubber: MatchRubber) {
  val (games, nsGamesWon, ewGamesWon) = calculate()

  val nsVul = nsGamesWon > 0
  val ewVul = ewGamesWon > 0

  val done = nsGamesWon == 2 || ewGamesWon == 2

  val (nsBelow, ewBelow, nsAbove, ewAbove) = {
    games
      .map { g =>
        (g.nsBelow, g.ewBelow, g.nsAbove, g.ewAbove)
      }
      .foldLeft((0, 0, 0, 0))(
        (a, v) => (a._1 + v._1, a._2 + v._2, a._3 + v._3, a._4 + v._4)
      )
  }

  val (nsBonus, ewBonus) = {
    if (nsGamesWon == 2) {
      if (ewGamesWon == 1) (500, 0) else (700, 0)
    } else if (ewGamesWon == 2) {
      if (nsGamesWon == 1) (0, 500) else (0, 700)
    } else {
      val r = if (nsGamesWon == 1 && ewGamesWon == 0) {
        (300, 0)
      } else if (nsGamesWon == 0 && ewGamesWon == 1) {
        (0, 300)
      } else {
        (0, 0)
      }
      if (!games.isEmpty) {
        val lastgame = games(games.size - 1)
        if (lastgame.done) {
          r
        } else {
          if (lastgame.nsBelow > 0 && lastgame.ewBelow == 0) {
            (r._1 + 100, r._2)
          } else if (lastgame.nsBelow == 0 && lastgame.ewBelow > 0) {
            (r._1, r._2 + 100)
          } else {
            r
          }
        }
      } else {
        r
      }
    }
  }

  val nsTotal = nsBelow + nsAbove + nsBonus
  val ewTotal = ewBelow + ewAbove + ewBonus

  val nsWon = nsTotal > ewTotal
  val ewWon = ewTotal > nsTotal

  val isTie = !nsWon && !ewWon

  private def calculate() = {
    var gs: List[GameScoring] = Nil
    var nsVul = false
    var ewVul = false
    var nsWins = 0
    var ewWins = 0
    var g = new GameScoring(Nil, nsVul, ewVul)
    val it = rubber.hands.iterator
    while (it.hasNext) {
      g = g.add(it.next())
      if (g.done) {
        if (g.nsWon) {
          nsVul = true
          nsWins = nsWins + 1
        } else if (g.ewWon) {
          ewVul = true
          ewWins = ewWins + 1
        }
        gs = g :: gs
        g = new GameScoring(Nil, nsVul, ewVul)
      }
    }
    val r = if (g.hands.isEmpty && (nsWins == 2 || ewWins == 2)) {
      gs.reverse
    } else {
      (g :: gs).reverse
    }
    (r, nsWins, ewWins)
  }

  /**
    * Get the results of all hands
    * Returns a tuple:
    *    nsAbove: List[Int]
    *    nsBelow: List[List[Int]]
    *    ewAbove: List[Int]
    *    ewBelow: List[List[Int]]
    */
  def totals = {
    val x = games.foldLeft(
      (
        Nil: List[Int],
        Nil: List[List[Int]],
        Nil: List[Int],
        Nil: List[List[Int]]
      )
    )((a, gs) => {
      val r = gs.scores
      (r._1 ::: a._1, r._2 :: a._2, r._3 ::: a._3, r._4 :: a._4)
    })
    (x._1, x._2.reverse, x._3, x._4.reverse)
  }

  def setPlayers(north: String, south: String, east: String, west: String) =
    new RubberScoring(rubber.setPlayers(north, south, east, west))

  /**
    * @param pos the first dealer, N, S, E, W
    */
  def setFirstDealer(pos: String) =
    new RubberScoring(rubber.setFirstDealer(pos))

  def getDealerForHand(id: String) = {
    var dealer = PlayerPosition(rubber.dealerFirstHand)
    var hands = rubber.hands
    while (hands.headOption.isDefined && hands.head.id != id) {
      dealer = dealer.nextDealer
      hands = hands.tail
    }
    dealer
  }

  def addHand(h: RubberHand) = new RubberScoring(rubber.addHand(h))

  def modifyHand(h: RubberHand) = new RubberScoring(rubber.modifyHand(h))
}

object RubberScoring {
  def apply(rubber: MatchRubber) = new RubberScoring(rubber)
}
