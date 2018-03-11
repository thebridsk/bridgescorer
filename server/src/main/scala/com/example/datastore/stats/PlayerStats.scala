package com.example.datastore.stats

import java.io.PrintStream
import com.example.data.Id
import com.example.data.MatchDuplicate


/**
 * @param player
 * @param histogram map of tricks to counter.  tricks is the number of tricks from contract.  0 is made exactly contract.  >0 made contract with that many extra tricks.  <0 contract is down.
 * @param
 */
case class PlayerStats( player: String, declarer: Boolean, histogram: Map[Int,Int] = Map(), handsPlayed: Int = 0 ) {
  def add( other: PlayerStats ) = {
    if (player != other.player || declarer != other.declarer) throw new Exception("player or declarer is not the same")
    val np = handsPlayed + other.handsPlayed
    val h = other.histogram.foldLeft(histogram) { (ac,v) =>
      val newv = ac.get(v._1).map( oldv => oldv + v._2 ).getOrElse(v._2)
      ac + (v._1 -> newv)
    }
    copy(histogram=h, handsPlayed=np)
  }

  def csvHeader = {
    val r = (-13 to 6).map { i =>
      if (i<0) s"Down ${-i}"
      else if (i==0) "Made"
      else s"Made +${i}"
    }
    s"""Player,Declarer,handsPlayed,${r.mkString(",")}"""
  }

  def toCsv() = {
    val r = (-13 to 6).map { i => histogram.get(i).getOrElse(0) }

    s""""${player}",${declarer},${handsPlayed},${r.mkString(",")}"""
  }

  def toCsvPercent() = {
    val r = (-13 to 6).map { i => histogram.get(i).getOrElse(0) }

    s""""${player}",${declarer},${handsPlayed},${r.map(v=> f"${100.0*v/handsPlayed}%.2f" ).mkString(",")}"""
  }
}

object PlayerStats {

  case class GameStat( player: String, declarer: Boolean, result: Int )

  def csvHeader = PlayerStats("",true).csvHeader

  def statsToCsv( stats: Map[Boolean,List[PlayerStats]], percent: Boolean = false )(implicit out: PrintStream) = {
    val tocsv: PlayerStats=>String = if (percent) ds=>ds.toCsvPercent() else ds=>ds.toCsv()
    (true::false::Nil).foreach { declarer =>
      stats.get(declarer).foreach { raw =>
        val sts = raw.sortWith((l,r) => l.player<r.player)
        out.println( if (declarer) "Declarer" else "Defender" )
        out.println( PlayerStats.csvHeader )
        sts.foreach { ds => out.println( tocsv(ds) ) }
      }
    }

    out.flush()
  }

  def statsToCsvPercent( stats: Map[Boolean,List[PlayerStats]] )(implicit out: PrintStream) = {
    statsToCsv(stats, true)
  }

  def stats( dups: Map[Id.MatchDuplicate,MatchDuplicate] ) = {
    val results = dups.values.flatMap { dup =>
      dup.allPlayedHands.flatMap { dh =>
        val nsTeam = dup.getTeam(dh.nsTeam).get
        val ewTeam = dup.getTeam(dh.ewTeam).get
        val (declarer,defender) = dh.played.head.declarer match {
          case "N" | "S" => (nsTeam,ewTeam)
          case "E" | "W" => (ewTeam,nsTeam)
        }
        if (dh.played.head.contractTricks == 0) {
          Nil
        } else {
          val r = if (dh.played.head.madeContract) dh.played.head.tricks-dh.played.head.contractTricks
                  else -dh.played.head.tricks
          GameStat( declarer.player1, true, r )::
          GameStat( declarer.player2, true, r )::
          GameStat( defender.player1, false, r )::
          GameStat( defender.player2, false, r )::
          Nil
        }
      }
    }
    val counters = results.groupBy( gs => gs.declarer).map { entry =>
      val (key, value) = entry
      (key -> value.groupBy( gs => gs.player).map { entry2 =>
          val (key2, value2) = entry2
          (key2 -> value2.groupBy( gs => gs.result).map { entry3 =>
            val (key3, value3) = entry3
            (key3 -> value3 )
          })
      })
    }
    val stats = counters.map { decentry =>
      val (declarer, decmap) = decentry
      ( declarer -> decmap.map { decplayentry =>
          val (player, decplaymap) = decplayentry
          decplaymap.map { decplayresult =>
            val (result, x) = decplayresult
            val played = x.size
            PlayerStats(player,declarer,Map( result -> played ), played)
          }.foldLeft(PlayerStats(player,declarer)) { (ac,v) =>
            ac.add(v)
          }
        }.toList
      )
    }
    stats
  }

}
