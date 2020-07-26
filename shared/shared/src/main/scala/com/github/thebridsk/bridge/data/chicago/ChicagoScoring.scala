package com.github.thebridsk.bridge.data.chicago

import com.github.thebridsk.bridge.data._
import com.github.thebridsk.bridge.data.bridge.DuplicateBridge.ScoreHand
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.data.bridge.East
import com.github.thebridsk.bridge.data.bridge.South
import com.github.thebridsk.bridge.data.bridge.West

/**
  * @author werewolf
  */
class ChicagoScoring(val chicago: MatchChicago) {
  val rounds = chicago.rounds.map { r =>
    RoundScoring(r)
  }

  val gamesPerRound = chicago.gamesPerRound

  /**
    * (players, totals, byRounds): (List[String], List[Int], List[List[Int]].
    * players is an array of strings with player names.
    * totals is an array with index same as players.
    * byRounds is a matrix, first index is same as rounds,
    *                       second index is same as players
    */
  val (players, totals, byRounds) = calculate

  /**
    * Sorted by totals, then player names
    */
  def sortedResults = {
    val tosort: List[Int] = (0 until players.length).toList
    tosort
      .sortWith { (left, right) =>
        totals(left).compareTo(totals(right)) match {
          case x: Int if x > 0 => true
          case x: Int if x < 0 => false
          case x: Int if x == 0 =>
            players(left).compareTo(players(right)) < 0
        }
      }
      .map { i =>
        (players(i), totals(i))
      }
      .unzip
  }

  override def toString() = {
    "ChicagoScoring( chicago=" + chicago +
      "\n                players=" + players.mkString(", ") +
      "\n                totals =" + totals.mkString(", ") +
      "\n                byRounds=" + byRounds
      .map(e => e.mkString("{", ", ", "}"))
      .mkString(", ") +
      ")"
  }

  private def getArrayInt(initial: Int = 0) = {
    val a = new Array[Int](chicago.players.length)
    for (i <- 0 until a.length) a(i) = initial
    a
  }

  /**
    * @return (players, totals, byRounds): (Array[Int], Array[Array[Int]].
    * players is an array of strings with player names.
    * totals is an array with index same as players.
    * byRounds is a matrix, first index is same as rounds,
    *                       second index is same as players
    */
  private def calculate: (List[String], List[Int], List[List[Int]]) = {
    val totals = getArrayInt()
    val len = rounds.length
    var byRounds: List[List[Int]] = Nil
    val players = chicago.players

    if (players.find(s => s == null).isEmpty) {
      val playerMap =
        Array.tabulate(players.length)(i => (players(i) -> i)).toMap

      for (i <- 0 until len) {
        val scores = getArrayInt(-1)
        for (p <- 0 until rounds(i).players.length) {
          val playerInRound = rounds(i).players(p)

          val pMe = playerMap(playerInRound)
          totals(pMe) += rounds(i).totals(p)
          scores(pMe) = rounds(i).totals(p)
        }
        byRounds = scores.toList :: byRounds
      }
    }

    (players.toList, totals.toList, byRounds.reverse)
  }

  def addRound(r: Round) = ChicagoScoring(chicago.addRound(r))

  /**
    * modify an existing hand
    * @param ir - the round, the round must exist. values are 0, 1, ...
    * @param ih - the hand, if the hand doesn't exist, then it will addHandToLastRound. values are 0, 1, ...
    * @param h - the new hand
    */
  def modifyHand(ir: Int, ih: Int, h: Hand) = {
    ChicagoScoring(chicago.modifyHand(ir, ih, h))
  }

  def setGamesPerRound(gamesInRound: Int) = {
    ChicagoScoring(chicago.setGamesPerRound(gamesInRound))
  }

  def setId(id: MatchChicago.Id) = {
    ChicagoScoring(chicago.setId(id))
  }

  def numberPlayers = players.length

  def isConvertableToChicago5 = chicago.isConvertableToChicago5

  /**
    * @return is a map with the index are the people sitting out,
    * and the value is list of fixtures.
    */
  def getRemainingPossibleFixtures = {
    val fixtures = getAllFixtures.filter(f => {
      chicago.rounds
        .find(r => {
          f.hasPair(r.north, r.south) || f.hasPair(r.east, r.west)
        })
        .isEmpty
    })

    val sitouts = fixtures.map(f => f.extra).toSet
    val r =
      sitouts.map(s => (s, fixtures.filter(f => f.extra == s).toList)).toMap
    r
  }

  def getAllFixtures = {
    ChicagoScoring.getAllFixtures(players: _*)
  }

  def getFixturesSoFar = {
    val pathSoFar = rounds.map(r => {
      val extra = players.find(p => !r.players.contains(p)).getOrElse("")
      ChicagoScoring.createFixture(
        r.round.north,
        r.round.south,
        r.round.east,
        r.round.west,
        extra
      )
    })
    pathSoFar
  }

  /**
    * Get all possible next fixtures
    * @return map with index is the player sitting out and value of all possible player pairings
    */
  def getNextPossibleFixtures: Map[String, Set[ChicagoScoring.Fixture]] = {
    if (players.length == 4) return Map()
    var pathSoFar = getFixturesSoFar
    while (pathSoFar.length >= players.length) pathSoFar =
      pathSoFar.drop(players.length)
    val allFixtures = ChicagoScoring.getAllFixtures(players: _*)
    val ret = players
      .flatMap(p => {
        val paths = ChicagoScoring.calculatePaths(allFixtures, pathSoFar, p)
        val nexts = ChicagoScoring.calculateNextPossible(pathSoFar, paths)
        if (nexts.isEmpty) Seq()
        else {
          Seq((p, nexts))
        }
      })
      .toMap
    ret
  }
}

object ChicagoScoring {
  def apply(chicago: MatchChicago) = new ChicagoScoring(chicago)

  def getAllFixtures(players: String*) = {
    val len = players.length

    val all =
      for (n <- 0 until len;
           s <- n + 1 until len;
           e <- s + 1 until len;
           w <- e + 1 until len) yield {
        val north = players(n)
        val south = players(s)
        val east = players(e)
        val west = players(w)
        val playing = List(north, south, east, west)
        val extras = players.filter(p => !playing.contains(p))
        createMix(north, south, east, west, extras.head)
      }
    all.flatten
  }

  def createMix(
      north: String,
      south: String,
      east: String,
      west: String,
      extra: String
  ) = {
    val x: Seq[Fixture] = Seq(
      createFixture(north, south, east, west, extra),
      createFixture(north, east, south, west, extra),
      createFixture(north, west, south, east, extra)
    )
    x
  }

  def createFixture(
      north: String,
      south: String,
      east: String,
      west: String,
      extra: String
  ) = {
    var n = north
    var s = south
    var e = east
    var w = west

    if (n > s) {
      n = south
      s = north
    }
    if (e > w) {
      e = west
      w = east
    }
    if (n > e) {
      Fixture(e, w, n, s, extra)
    } else {
      Fixture(n, s, e, w, extra)
    }
  }

  /**
    * @constructor
    * The following constraints must apply:
    *   north < south
    *   east < west
    *   north < east
    */
  case class Fixture private (
      north: String,
      south: String,
      east: String,
      west: String,
      extra: String
  ) {

    def hasPair(p1: String, p2: String) = {
      (p1 == north && p2 == south) || (p2 == north && p1 == south) ||
      (p1 == east && p2 == west) || (p2 == east && p1 == west)
    }

    /**
      * True if there is a player mentioned in both players and extra
      */
    def extraOverlap(player: String) = {
      extra == player
    }
  }

  /**
    * Return the fixtures that can be used next
    * @param pathSoFar the fixtures that have been played already
    * @param possiblePaths the fixture path to complete a match
    * @return the fixtures that are valid for the next round
    */
  def calculateNextPossible(
      pathSoFar: Seq[Fixture],
      possiblePaths: List[List[Fixture]]
  ): Set[Fixture] = {
    possiblePaths.map(path => path.drop(pathSoFar.length).head).toSet
  }

  /**
    * determine if a candidate fixture can be used given a path
    * @param candidate
    * @param pathSoFar the fixtures that have been played
    * @return true if it can be used
    */
  def canUseFixture(candidate: Fixture, pathSoFar: Seq[Fixture]) = {
    val r =
      if (pathSoFar.isEmpty) true
      else if (pathSoFar.head.extraOverlap(candidate.extra)) false
      else {
        pathSoFar.find { f =>
          {
            candidate.extra == f.extra ||
            candidate.hasPair(f.north, f.south) || candidate.hasPair(
              f.east,
              f.west
            )
          }
        }.isEmpty
      }
//    if (!r) logger.info("Can't use "+candidate+" in "+pathSoFar) else  logger.info("Can use "+candidate+" in "+pathSoFar)
    r
  }

  /**
    * Calculate all the paths given the next person to sit out
    * @param allFixtures all the possible fixtures
    * @param pathSoFar the fixtures that have been played so far
    * @param nextOut the player sitting out next
    * @return the fixture paths that start with pathSoFar and have the next with nextOut sitting out
    */
  def calculatePaths(
      allFixtures: Seq[Fixture],
      pathSoFar: List[Fixture],
      nextOut: String
  ) = {
    if (pathSoFar.find(f => f.extra == nextOut).isEmpty) {
      val s = allFixtures.head
      val n = 5
      val fixturesWithNextOut = allFixtures.filter(f => f.extra == nextOut)
      val x = fixturesWithNextOut
        .filter(candidate => canUseFixture(candidate, pathSoFar))
        .map(f => {
          val pSoFar = pathSoFar ::: List(f)
          if (pSoFar.size == n) {
            List(pSoFar)
          } else {
            val validRemaining =
              allFixtures.filter(candidate => canUseFixture(candidate, pSoFar))
            calculateRemainingPath(n, pSoFar, validRemaining)
          }
        })
        .flatten
        .filter { l =>
          !l.isEmpty
        }
        .toList
      x
    } else {
      List()
    }
  }

  /**
    * Returns all the fixture paths that are possible
    * @param allFixtures all the fixtures
    * @return a list of all the fixture paths that are possible
    */
  def calculatePaths(allFixtures: Seq[Fixture]): List[List[Fixture]] = {
    val s = allFixtures.head
    val n = 5
    val pathSoFar = List(allFixtures.head) // head is most recent
    val validRemaining =
      allFixtures.filter(candidate => canUseFixture(candidate, pathSoFar))
    calculateRemainingPath(n, pathSoFar, validRemaining)
  }

  /**
    * A recursive function to help calculate the fixture path
    * @param target the number of players
    * @param pathSoFar the fixtures that have been played so far
    * @param validRemaining the fixtures that still could be played
    * @return the fixture paths that start with pathSoFar
    */
  def calculateRemainingPath(
      target: Int,
      pathSoFar: List[Fixture],
      validRemaining: Seq[Fixture]
  ): List[List[Fixture]] = {
    val x = validRemaining
      .map(vr => {
        val pSoFar = pathSoFar ::: List(vr)
        if (pSoFar.length == target) {
          List(pSoFar)
        } else {
          val vRemaining =
            validRemaining.filter(candidate => canUseFixture(candidate, pSoFar))
          calculateRemainingPath(target, pSoFar, vRemaining)
        }
      })
      .flatten
      .filter { l =>
        !l.isEmpty
      }
      .toList
//    if (x.isEmpty) logger.info("Found dead end: "+pathSoFar)
    x
  }

}
