package com.example.data.duplicate.suggestion

import com.example.data.DuplicateSummary
import com.example.data.DuplicateSummaryDetails
import utils.logging.Logger
import scala.collection.mutable.ListBuffer

/**
 * Constructor
 * @param player1
 * @param player2
 * @param played the number of times this pair played
 * @param won the number of times this pair has won
 * @param points the number of points scored by this pair
 * @param totalPoints the total number of points the team could have won.
 */
case class PairData( player1: String, player2: String, played: Int, won: Int, wonPts: Double, points: Double, totalPoints: Double, incompleteGames: Int, details: Option[DuplicateSummaryDetails] ) {
  import PairsData._

  def normalize = {
    val (p1,p2) = key(player1,player2)
    copy( player1=p1, player2=p2 )
  }

  def swapNames = copy(player1=player2, player2=player1 )

  def isNormalized = (player1,player2) == key(player1,player2)

  def add( win: Boolean, winPts: Double, pts: Double, totpts: Double, det: Option[DuplicateSummaryDetails] ) = {
    val ds = det.map { d =>
      Some(details.map { cd => cd.add(d) }.getOrElse(d))
    }.getOrElse(details)
    copy( played=played+1, won=won+(if (win) 1 else 0), wonPts=wonPts+winPts, points=points+pts, totalPoints=totalPoints+totpts, details=ds)
  }

  def addIncomplete = copy( incompleteGames=incompleteGames+1)

  def getkey = (player1,player2)

  def pointsPercent = points/totalPoints*100.0
  def winPercent = won.toDouble/played*100.0
  def winPtsPercent = wonPts.toDouble/played*100.0

  def contains( player: String ) = player1==player || player2==player

  /**
   * Add this PairData to the specified PairData.
   * @param pd the other PairData.  This PairData must have player1 as one of its players.
   * @return the sum.  player2 contains the players that were added together.  eg: "p1,p2,p3"
   */
  def addPairData( pd: PairData ) = {
    if (!pd.contains(player1)) throw new IllegalArgumentException("Player 1 must be one of the players in pd")
    val pdOtherPlayer = if (player1 == pd.player1) pd.player2 else pd.player1
    val p2 = if (player2=="") pdOtherPlayer else player2+","+pdOtherPlayer
    val ds = pd.details.map { d =>
      Some(details.map { cd => cd.add(d) }.getOrElse(d))
    }.getOrElse(details)
    PairData( player1, p2,
              played = played+pd.played,
              won = won+pd.won,
              wonPts = wonPts+pd.wonPts,
              points = points+pd.points,
              totalPoints = totalPoints+pd.totalPoints,
              incompleteGames = incompleteGames+pd.incompleteGames,
              details = ds )
  }
}

object PairsData {
  val log = Logger("bridge.PairsData")

  def key( player1: String, player2: String ) = if (player1 < player2) (player2,player1) else (player1,player2)

  def apply( pastgames: List[DuplicateSummary] ) = new PairsData(pastgames)
}

/**
 * @param pastgames all the past games, will be sorted by created field
 */
class PairsData( pastgames: List[DuplicateSummary] ) {

  import PairsData._

  /**
   * data the pairs data in a map.  The key is a tuple2 of the two players names, alphabetical order.
   * players is a list of all players found in alphabetical order.
   */
  val (data,players): (Map[ (String,String), PairData ], List[String]) = {

    val tdata = collection.mutable.Map[ (String,String), PairData ]()
    val list = new ListBuffer[String]()

    def addPerson( p: String ): Unit = {
      if (!list.contains(p)) list += p
    }

    def add( player1: String, player2: String, win: Boolean, winPts: Double, pts: Double, totpts: Double, incomplete: Boolean, details: Option[DuplicateSummaryDetails] ): Unit = {
      addPerson(player1)
      addPerson(player2)
      val pp = key(player1,player2)
      val newpd = tdata.get(pp).getOrElse( PairData(pp._1,pp._2,0,0,0,0,0,0,None).normalize )
      val reallynewpd = if (incomplete) {
        newpd.addIncomplete
      } else {
        newpd.add(win, winPts, pts, totpts, details)
      }
      tdata += newpd.getkey -> reallynewpd
    }

    pastgames.foreach { ds =>
      val incomplete = !ds.finished
      val total = ds.teams.map( dse => dse.result ).foldLeft(0.0)((ac,v)=>ac+v)/2
      val numberWinners = ds.teams.filter { dse => dse.place==1 }.length
      ds.teams.foreach { dse =>
        val pts = dse.result
        val won = dse.place == 1
        val wonPts = if (won && numberWinners != 0) {
          1.0/numberWinners
        } else {
          0.0
        }
        val details = dse.details
        add(dse.team.player1,dse.team.player2,won,wonPts,pts,total,incomplete,details)
      }
    }

    (tdata.toMap, list.toList.sorted )
  }

  /**
   * Returns the [[PairData]] object for the two specified players
   * @param player1
   * @param player2
   */
  def get( player1: String, player2: String ) = {
    data.get(key(player1,player2))
  }

  /**
   * Returns a [[PairData]] object that contains the sum of all PairData object
   * that involve the specified player.
   * @param player
   */
  def get( player: String, playerFilter: Option[List[String]] ) = {
    val pds = data.values.filter { pd =>
      pd.contains(player) && playerFilter.map( f => f.contains(pd.player1) && f.contains(pd.player2)).getOrElse(true)
    }
    pds.foldLeft(PairData(player,"",0,0,0,0,0,0, None)) { (ac,v) =>
      ac.addPairData(v)
    }
  }
}

trait ColorBy {
  val name: String
  def value( pd: PairData ): Double
}
object ColorByWon extends ColorBy { val name = "Won"; def value( pd: PairData ): Double = pd.won }
object ColorByWonPct extends ColorBy { val name = "Won Percent"; def value( pd: PairData ): Double = pd.winPercent }
object ColorByWonPts extends ColorBy { val name = "Won Points"; def value( pd: PairData ): Double = pd.wonPts }
object ColorByWonPtsPct extends ColorBy { val name = "Won Points Percent"; def value( pd: PairData ): Double = pd.winPtsPercent }
object ColorByPointsPct extends ColorBy { val name = "Points Percent"; def value( pd: PairData ): Double = pd.pointsPercent }

object ColorByPlayed extends ColorBy { val name = "Played"; def value( pd: PairData ): Double = pd.played }

class Stat( val colorBy: ColorBy ) {
  private var number: Int = 0
  private var total: Double = 0
  private var vmax: Double = Double.MinValue
  private var vmin: Double = Double.MaxValue

  def add( pds: PairsData ): Stat = {
    add(pds.data.values)
    this
  }

  def add( pds: Iterable[PairData] ): Stat = {
    pds.foreach( pd => add(colorBy.value(pd),pd.played))
    this
  }

  def add( v: Double, n: Int ): Unit = {
    number += n
    total += v*n
    vmax = Math.max( vmax, v )
    vmin = Math.min( vmin, v )
  }

  def max = vmax
  def min = vmin
  def ave = if (number == 0) 0.0 else total/number

  def size( pd: PairData, sizemin: Int, sizemax: Int ): Int = {
    val v = colorBy.value(pd)
    if (max == min) sizemax
    else ((v-min)*(sizemax-sizemin)/(max-min) + sizemin).toInt
  }

  /**
   * determines the distance from ave.
   * @param pd
   * @param sizemin must be greater than 0
   * @param sizemax must be greater than sizemin
   * @return tuple2.  the first entry is a boolean, true indicates above average.
   * The second is the distance from average (smin - smax).  zero indicates average.
   */
  def sizeAve( pd: PairData, sizemin: Int, sizemax: Int ): (Boolean,Int) = {
    val v = colorBy.value(pd)

    if (min == max) (true,0)
    else if (v == ave) (true,0)
    else if (v < ave) {
      (false,((v-min)*(sizemax-sizemin)/(ave-min) + sizemin).toInt)
    } else {
      (true,((v-ave)*(sizemax-sizemin)/(max-ave) + sizemin).toInt)
    }

  }
}

object Stat {

  def add( pds: PairsData, filter: Option[List[String]], stats: Stat* ): Unit = {
    addPairs(pds.data.values, filter, stats:_*)
  }

  def addPairs( pds: Iterable[PairData], filter: Option[List[String]], stats: Stat* ): Unit = {
    pds.foreach { pd =>
      if (filter.map( f => f.contains(pd.player1) && f.contains(pd.player2)).getOrElse(true) ) {
        stats.foreach { s =>
          s.add(s.colorBy.value(pd), pd.played)
        }
      }
    }
  }

  def addPairsPlayer1( pds: Iterable[PairData], filter: Option[List[String]], stats: Stat* ): Unit = {
    pds.foreach { pd =>
      if (filter.map( f => f.contains(pd.player1)).getOrElse(true) ) {
        stats.foreach { s =>
          if (pd.played != 0) s.add(s.colorBy.value(pd), pd.played)
        }
      }
    }
  }

}

import PairsData.log
import com.example.data.DuplicateSummaryDetails
import com.example.data.DuplicateSummaryDetails
import com.example.data.DuplicateSummaryDetails

/**
 * @param pds
 * @param colorBy
 * @param filter if specified will only show result with these players.
 */
class PairsDataSummary( pds: PairsData, colorBy: ColorBy, filter: Option[List[String]], extraColorBy: ColorBy* ) {

  val players = pds.players
  val playerFilter = filter.getOrElse(players)

  val playerTotals = playerFilter.map { player =>
                                        player -> pds.get(player,filter)
                                      }.toMap

  /**
   * Returns the [[PairData]] object for the two specified players
   * @param player1
   * @param player2
   */
  def get( player1: String, player2: String ) = {
    if (playerFilter.contains(player1) && playerFilter.contains(player2)) {
      pds.get(player1, player2)
    } else {
      None
    }
  }

  val extraStatsPlayer = extraColorBy.map( e => new Stat(e) ).toList
  val colorStatPlayerTotals = new Stat(colorBy)



  Stat.addPairsPlayer1(playerTotals.values, filter, colorStatPlayerTotals::extraStatsPlayer :_*)

  val extraStats = extraColorBy.map( e => new Stat(e) ).toList
  val colorStat = new Stat(colorBy)
  Stat.add(pds, filter, colorStat::extraStats :_*)

  log.fine( f"player ${extraColorBy.map(e => f"${e.name}").mkString(",")} min=${extraStatsPlayer.map(e => f"${e.min}%.0f").mkString(",")} max=${extraStatsPlayer.map(e => f"${e.max}%.0f").mkString(",")} ave=${extraStatsPlayer.map(e => f"${e.ave}%.0f").mkString(",")}" )
  log.fine( f"player ${colorBy.name} min=${colorStatPlayerTotals.min}%.0f max=${colorStatPlayerTotals.max}%.0f ave=${colorStatPlayerTotals.ave}%.2f" )

  log.fine( f"total ${extraColorBy.map(e => f"${e.name}").mkString(",")} min=${extraStats.map(e => f"${e.min}%.0f").mkString(",")} max=${extraStats.map(e => f"${e.max}%.0f").mkString(",")} ave=${extraStats.map(e => f"${e.ave}%.0f").mkString(",")}" )
  log.fine( f"total ${colorBy.name} min=${colorStat.min}%.2f max=${colorStat.max}%.2f ave=${colorStat.ave}%.2f" )
}
