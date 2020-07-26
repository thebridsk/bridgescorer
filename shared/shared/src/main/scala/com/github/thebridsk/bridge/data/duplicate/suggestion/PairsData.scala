package com.github.thebridsk.bridge.data.duplicate.suggestion

import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.data.DuplicateSummaryDetails
import com.github.thebridsk.utilities.logging.Logger
import scala.collection.mutable.ListBuffer
import com.github.thebridsk.bridge.data.duplicate.stats.Statistic

/**
  * Constructor
  * @param player1
  * @param player2
  * @param played the number of times this pair played
  * @param won the number of times this pair has won
  * @param points the number of points scored by this pair
  * @param totalPoints the total number of points the team could have won.
  */
case class PairData(
    player1: String,
    player2: String,
    played: Int,
    won: Int,
    wonPts: Double,
    points: Double,
    totalPoints: Double,
    incompleteGames: Int,
    details: Option[DuplicateSummaryDetails],
    wonImp: Int,
    wonImpPts: Double,
    imp: Double,
    playedMP: Int,
    playedIMP: Int,
    maxMPPercent: Double
) {
  import PairsData._

  def normalize: PairData = {
    val (p1, p2) = key(player1, player2)
    copy(player1 = p1, player2 = p2)
  }

  def swapNames: PairData = copy(player1 = player2, player2 = player1)

  def isNormalized: Boolean = (player1, player2) == key(player1, player2)

  def add(
      win: Boolean,
      winPts: Double,
      pts: Double,
      totpts: Double,
      det: Option[DuplicateSummaryDetails],
      winImp: Boolean,
      winImpPts: Double,
      aimp: Double,
      playMP: Boolean,
      maxMPPer: Double
  ): PairData = {
    val ds = det
      .map { d =>
        Some(
          details
            .map { cd =>
              cd.add(d)
            }
            .getOrElse(d)
        )
      }
      .getOrElse(details)
    val (pmp, pimp) = if (playMP) (1, 0) else (0, 1)
    copy(
      played = played + 1,
      playedMP = playedMP + pmp,
      playedIMP = playedIMP + pimp,
      won = won + (if (win) 1 else 0),
      wonPts = wonPts + winPts,
      points = points + pts,
      totalPoints = totalPoints + totpts,
      details = ds,
      wonImp = wonImp + (if (winImp) 1 else 0),
      wonImpPts = wonImpPts + winImpPts,
      imp = imp + aimp,
      maxMPPercent = Math.max(maxMPPercent, maxMPPer)
    )
  }

  def addIncomplete: PairData = copy(incompleteGames = incompleteGames + 1)

  def getkey: (String, String) = (player1, player2)

  def pointsPercent: Double =
    if (totalPoints == 0) 0.0 else points / totalPoints * 100.0
  def winPercent: Double =
    if (played == 0) 0.0 else (won.toDouble + wonImp) / played * 100.0
  def winPtsPercent: Double =
    if (played == 0) 0.0 else (wonPts.toDouble + wonImpPts) / played * 100.0
  def avgIMP: Double = if (playedIMP == 0) 0.0 else imp / playedIMP

  def contains(player: String): Boolean = player1 == player || player2 == player

  /**
    * Add this PairData to the specified PairData.
    * @param pd the other PairData.  This PairData must have player1 as one of its players.
    * @return the sum.  player2 contains the players that were added together.  eg: "p1,p2,p3"
    */
  def addPairData(pd: PairData): PairData = {
    if (!pd.contains(player1))
      throw new IllegalArgumentException(
        "Player 1 must be one of the players in pd"
      )
    val pdOtherPlayer = if (player1 == pd.player1) pd.player2 else pd.player1
    val p2 = if (player2 == "") pdOtherPlayer else player2 + "," + pdOtherPlayer
    val ds = pd.details
      .map { d =>
        Some(
          details
            .map { cd =>
              cd.add(d)
            }
            .getOrElse(d)
        )
      }
      .getOrElse(details)
    PairData(
      player1,
      p2,
      played = played + pd.played,
      won = won + pd.won,
      wonPts = wonPts + pd.wonPts,
      wonImp = wonImp + pd.wonImp,
      wonImpPts = wonImpPts + pd.wonImpPts,
      points = points + pd.points,
      imp = imp + pd.imp,
      totalPoints = totalPoints + pd.totalPoints,
      incompleteGames = incompleteGames + pd.incompleteGames,
      details = ds,
      playedMP = playedMP + pd.playedMP,
      playedIMP = playedIMP + pd.playedIMP,
      maxMPPercent = Math.max(maxMPPercent, pd.maxMPPercent)
    )
  }
}

object PairsData {
  val log: Logger = Logger("bridge.PairsData")

  def key(player1: String, player2: String): (String, String) =
    if (player1 < player2) (player2, player1) else (player1, player2)

  def apply(
      pastgames: List[DuplicateSummary],
      calc: CalculationType = CalculationAsPlayed
  ) = new PairsData(pastgames, calc)

}

sealed trait CalculationType

case object CalculationAsPlayed extends CalculationType
case object CalculationIMP extends CalculationType
case object CalculationMP extends CalculationType

/**
  * @param pastgames all the past games, will be sorted by created field
  */
class PairsData(
    val pastgames: List[DuplicateSummary],
    val calc: CalculationType = CalculationAsPlayed
) {

  import PairsData._

  /**
    * data the pairs data in a map.  The key is a tuple2 of the two players names, alphabetical order.
    * players is a list of all players found in alphabetical order.
    */
  val (data, players): (Map[(String, String), PairData], List[String]) = {

    val tdata = collection.mutable.Map[(String, String), PairData]()
    val list = new ListBuffer[String]()

    def addPerson(p: String): Unit = {
      if (!list.contains(p)) list += p
    }

    def add(
        player1: String,
        player2: String,
        win: Boolean,
        winPts: Double,
        pts: Double,
        totpts: Double,
        incomplete: Boolean,
        details: Option[DuplicateSummaryDetails],
        winImp: Boolean,
        winImpPts: Double,
        imp: Double,
        playMP: Boolean
    ): Unit = {
      addPerson(player1)
      addPerson(player2)
      val pp = key(player1, player2)
      val newpd = tdata
        .get(pp)
        .getOrElse(
          PairData(pp._1, pp._2, 0, 0, 0, 0, 0, 0, None, 0, 0, 0, 0, 0, 0.0).normalize
        )
      val reallynewpd = if (incomplete) {
        newpd.addIncomplete
      } else {
        val max = if (playMP && totpts != 0) {
          pts / totpts * 100
        } else {
          0
        }
        newpd.add(
          win,
          winPts,
          pts,
          totpts,
          details,
          winImp,
          winImpPts,
          imp,
          playMP,
          max
        )
      }
      tdata += newpd.getkey -> reallynewpd
    }

    /* *
     * @return tuple2
     *           showMP: Boolean
     *           showIMP: Boolean
     */
    def useStat(
        playedMP: Boolean,
        playedIMP: Boolean,
        hasMP: Boolean,
        hasIMP: Boolean
    ): (Boolean, Boolean) = {
      calc match {
        case CalculationAsPlayed =>
          (playedMP, playedIMP)
        case CalculationIMP =>
          (false, hasIMP)
        case CalculationMP =>
          (hasMP, false)
      }
    }

    pastgames.foreach { ds =>
      val playedMP = ds.isMP
      val playedIMP = ds.isIMP

      val total = ds.teams
        .map(dse => dse.result.getOrElse(0.0))
        .foldLeft(0.0)((ac, v) => ac + v) / 2
      val numberWinners = ds.teams.filter { dse =>
        dse.hasMp && dse.getPlaceMp == 1
      }.length
      val numberWinnersImp = ds.teams.filter { dse =>
        dse.hasImp && dse.getPlaceImp == 1
      }.length
      ds.teams.foreach { dse =>
        val hasMp = dse.hasMp
        val hasImp = dse.hasImp

        val (showMp, showImp, playMP, incomplete) =
          if (!ds.finished) (false, false, false, true)
          else {
            calc match {
              case CalculationAsPlayed =>
                (playedMP, playedIMP, playedMP, !hasMp && !hasImp)
              case CalculationIMP =>
                (false, hasImp, !hasImp, !hasImp)
              case CalculationMP =>
                (hasMp, false, hasMp, !hasMp)
            }
          }

        val pts = if (showMp) dse.getResultMp else 0.0
        val won = if (showMp) dse.getPlaceMp == 1 else false
        val wonPts = if (won && numberWinners != 0) {
          1.0 / numberWinners
        } else {
          0.0
        }
        val totalMP = if (showMp) {
          dse.details
            .map { det =>
              det.total * 2.0
            }
            .getOrElse(total)
        } else {
          0.0
        }

        val imp = if (showImp) dse.getResultImp else 0.0
        val wonImp = if (showImp) dse.getPlaceImp == 1 else false
        val wonImpPts = if (wonImp && numberWinnersImp != 0) {
          1.0 / numberWinnersImp
        } else {
          0.0
        }
        val details = dse.details
        add(
          dse.team.player1,
          dse.team.player2,
          won,
          wonPts,
          pts,
          totalMP,
          incomplete,
          details,
          wonImp,
          wonImpPts,
          imp,
          playMP
        )
      }
    }

    (tdata.toMap, list.toList.sorted)
  }

  /**
    * Returns the [[PairData]] object for the two specified players
    * @param player1
    * @param player2
    */
  def get(player1: String, player2: String): Option[PairData] = {
    data.get(key(player1, player2))
  }

  /**
    * Returns a [[PairData]] object that contains the sum of all PairData object
    * that involve the specified player.
    * @param player
    */
  def get(player: String, playerFilter: Option[List[String]]): PairData = {
    val (r, p) = data.values
      .filter { pd =>
        pd.contains(player) && playerFilter
          .map(f => f.contains(pd.player1) && f.contains(pd.player2))
          .getOrElse(true)
      }
      .foldLeft(
        (
          PairData(player, "", 0, 0, 0, 0, 0, 0, None, 0, 0, 0, 0, 0, 0.0),
          List[String]()
        )
      ) { (ac, v) =>
        (
          ac._1.addPairData(v),
          (if (v.player1 == player) v.player2 else v.player1) :: ac._2
        )
      }
    r.copy(player2 = p.sorted.mkString(","))
  }
}

trait ColorBy {
  val name: String
  def value(pd: PairData): Double
  def n(pd: PairData): Int
}
object ColorByWon extends ColorBy {
  val name = "Won";
  def value(pd: PairData): Double = pd.won
  def n(pd: PairData): Int = pd.played
}
object ColorByWonPct extends ColorBy {
  val name = "Won Percent";
  def value(pd: PairData): Double = pd.winPercent
  def n(pd: PairData): Int = pd.played
}
object ColorByWonPts extends ColorBy {
  val name = "Won Points";
  def value(pd: PairData): Double = pd.wonPts
  def n(pd: PairData): Int = pd.played
}
object ColorByWonPtsPct extends ColorBy {
  val name = "Won Points Percent";
  def value(pd: PairData): Double = pd.winPtsPercent
  def n(pd: PairData): Int = pd.played
}
object ColorByPointsPct extends ColorBy {
  val name = "Pts%";
  def value(pd: PairData): Double = pd.pointsPercent
  def n(pd: PairData): Int = pd.playedMP
}
object ColorByIMP extends ColorBy {
  val name = "avgIMP";
  def value(pd: PairData): Double = pd.avgIMP
  def n(pd: PairData): Int = pd.playedIMP
}

object ColorByPlayed extends ColorBy {
  val name = "Played";
  def value(pd: PairData): Double = pd.played
  def n(pd: PairData): Int = pd.played
}
object ColorByPlayedMP extends ColorBy {
  val name = "PlayedMP";
  def value(pd: PairData): Double = pd.playedMP
  def n(pd: PairData): Int = pd.playedMP
}
object ColorByPlayedIMP extends ColorBy {
  val name = "PlayedIMP";
  def value(pd: PairData): Double = pd.playedIMP
  def n(pd: PairData): Int = pd.playedIMP
}

class Stat(val colorBy: ColorBy) extends Statistic(colorBy.name) {

  def add(pds: PairsData): Stat = {
    add(pds.data.values)
    this
  }

  def add(pds: Iterable[PairData]): Stat = {
    pds.foreach(pd => add(colorBy.value(pd), pd.played))
    this
  }

  def size(pd: PairData, sizemin: Int, sizemax: Int): Int = {
    scale(colorBy.value(pd), sizemin, sizemax)
  }

  def valueInRange(pd: PairData): Boolean = {
    inRange(colorBy.value(pd))
  }

  /**
    * determines the distance from ave.
    * @param pd
    * @param sizemin must be greater than 0
    * @param sizemax must be greater than sizemin
    * @return tuple2.  the first entry is a boolean, true indicates above average.
    * The second is the distance from average (smin - smax).  zero indicates average.
    */
  def sizeAve(pd: PairData, sizemin: Int, sizemax: Int): (Boolean, Int) = {
    scaleAve(colorBy.value(pd), sizemin, sizemax)
  }

  /**
    * determines the distance from ave.
    * @param pd
    * @param sizemin must be greater than 0
    * @param sizemax must be greater than sizemin
    * @return tuple2.  the first entry is a boolean, true indicates above average.
    * The second is the distance from average, 0-1.  zero indicates average. a 1 indicates min or max
    */
  def sizeAveAsFraction(pd: PairData): (Boolean, Double) = {
    scaleAveAsFraction(colorBy.value(pd))
  }
}

object Stat {

  def add(pds: PairsData, filter: Option[List[String]], stats: Stat*): Unit = {
    addPairs(pds.data.values, filter, stats: _*)
  }

  def addPairs(
      pds: Iterable[PairData],
      filter: Option[List[String]],
      stats: Stat*
  ): Unit = {
    pds.foreach { pd =>
      if (filter
            .map(f => f.contains(pd.player1) && f.contains(pd.player2))
            .getOrElse(true)) {
        stats.foreach { s =>
          s.add(s.colorBy.value(pd), s.colorBy.n(pd))
        }
      }
    }
  }

  def addPairsPlayer1(
      pds: Iterable[PairData],
      filter: Option[List[String]],
      stats: Stat*
  ): Unit = {
    pds.foreach { pd =>
      if (filter.map(f => f.contains(pd.player1)).getOrElse(true)) {
        stats.foreach { s =>
          if (pd.played != 0) s.add(s.colorBy.value(pd), pd.played)
        }
      }
    }
  }

}

import PairsData.log
import com.github.thebridsk.bridge.data.DuplicateSummaryDetails

/**
  * @param pds
  * @param colorBy
  * @param filter if specified will only show result with these players.
  * @param displayOnly if true, all stats are used, players only only filtered when stats is displayed,
  *                    if false, only filtered players stats are used.
  */
class PairsDataSummary(
    pds: PairsData,
    colorBy: ColorBy,
    filter: Option[List[String]],
    displayOnly: Boolean,
    extraColorBy: ColorBy*
) {

  val players = pds.players
  val playerFilter: List[String] = filter.getOrElse(players)

  val playerTotals: Map[String,PairData] = playerFilter.map { player =>
    player -> pds.get(player, if (displayOnly) None else filter)
  }.toMap

  /**
    * Returns the [[PairData]] object for the two specified players
    * @param player1
    * @param player2
    */
  def get(player1: String, player2: String): Option[PairData] = {
    if (playerFilter.contains(player1) && playerFilter.contains(player2)) {
      pds.get(player1, player2)
    } else {
      None
    }
  }

  val extraStatsPlayerTotals: List[Stat] = extraColorBy.map(e => new Stat(e)).toList
  val colorStatPlayerTotals = new Stat(colorBy)

  Stat.addPairsPlayer1(
    playerTotals.values,
    filter,
    colorStatPlayerTotals :: extraStatsPlayerTotals: _*
  )

  val extraStats: List[Stat] = extraColorBy.map(e => new Stat(e)).toList
  val colorStat = new Stat(colorBy)
  Stat.add(pds, filter, colorStat :: extraStats: _*)

  log.fine(
    f"player ${extraColorBy.map(e => f"${e.name}").mkString(",")} min=${extraStatsPlayerTotals
      .map(e => f"${e.min}%.0f")
      .mkString(",")} max=${extraStatsPlayerTotals
      .map(e => f"${e.max}%.0f")
      .mkString(",")} ave=${extraStatsPlayerTotals.map(e => f"${e.ave}%.0f").mkString(",")}"
  )
  log.fine(
    f"player ${colorBy.name} min=${colorStatPlayerTotals.min}%.0f max=${colorStatPlayerTotals.max}%.0f ave=${colorStatPlayerTotals.ave}%.2f"
  )

  log.fine(
    f"total ${extraColorBy.map(e => f"${e.name}").mkString(",")} min=${extraStats
      .map(e => f"${e.min}%.0f")
      .mkString(",")} max=${extraStats
      .map(e => f"${e.max}%.0f")
      .mkString(",")} ave=${extraStats.map(e => f"${e.ave}%.0f").mkString(",")}"
  )
  log.fine(
    f"total ${colorBy.name} min=${colorStat.min}%.2f max=${colorStat.max}%.2f ave=${colorStat.ave}%.2f"
  )
}
