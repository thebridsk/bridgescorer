package com.example.datastore.stats

import java.io.PrintStream
import com.example.data.Id
import com.example.data.MatchDuplicate


/**
 * @param player
 * @param histogram map of tricks to counter.  tricks is the number of tricks from contract.  0 is made exactly contract.  >0 made contract with that many extra tricks.  <0 contract is down.
 * @param
 */
case class ContractStats( contract: String, histogram: Map[Int,Int] = Map(), handsPlayed: Int = 0 ) {
  def add( other: ContractStats ) = {
    if (contract != other.contract) throw new Exception("contract is not the same")
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
    s"""Contract,handsPlayed,${r.mkString(",")}"""
  }

  def toCsv() = {
    val r = (-13 to 6).map { i => histogram.get(i).getOrElse(0) }

    s""""${contract}",${handsPlayed},${r.mkString(",")}"""
  }

  def toCsvPercent() = {
    val r = (-13 to 6).map { i => histogram.get(i).getOrElse(0) }

    s""""${contract}",${handsPlayed},${r.map(v=> f"${100.0*v/handsPlayed}%.2f" ).mkString(",")}"""
  }
}

object ContractStats {

  case class GameStat( contract: String, result: Int )

  def csvHeader = ContractStats("").csvHeader

  private val patternContract = """([T]?)(\d)([CDHSN])([*]*)""".r
  private def parseContract( c: String ) = {
    c match {
      case "PassedOut" => ("","0", "P", "")
      case patternContract( a, t, s, d ) =>
        val ss = if (s == "N") "Z" else s
        (a, t,ss,d)
      case _ => ("","","",c)
    }
  }


  def compare( l: ContractStats, r: ContractStats ) = {
    val ( la, lt, ls, ld ) = parseContract( l.contract )
    val ( ra, rt, rs, rd ) = parseContract( r.contract )
    if (la != ra) la < ra
    else if (lt != rt) lt < rt
    else if (ls != rs) ls < rs
    else ld < rd
  }

  def statsToCsv( stats: List[ContractStats], percent: Boolean = false )(implicit out: PrintStream) = {
    val tocsv: ContractStats=>String = if (percent) ds=>ds.toCsvPercent() else ds=>ds.toCsv()
    val sts = stats.sortWith(compare)
    out.println( ContractStats.csvHeader )
    sts.foreach { ds => out.println( tocsv(ds) ) }
    out.flush()
  }

  def statsToCsvPercent( stats: List[ContractStats] )(implicit out: PrintStream) = {
    statsToCsv(stats, true)
  }

  def stats( dups: Map[Id.MatchDuplicate,MatchDuplicate] ) = {
    val results = dups.values.flatMap { dup =>
      dup.allPlayedHands.flatMap { dh =>
        if (dh.played.head.contractTricks == 0) {
          GameStat( "PassedOut", 0 )::Nil
        } else {
          val r = if (dh.played.head.madeContract) dh.played.head.tricks-dh.played.head.contractTricks
                  else -dh.played.head.tricks
          GameStat( dh.played.head.contract, r )::
          GameStat( s"T${dh.played.head.contractTricks}${dh.played.head.contractSuit}", r )::
          Nil
        }
      }
    }
    results.groupBy( gs => gs.contract).map { entry =>
      val (contract, value) = entry
      value.groupBy(gs => gs.result).map { entry =>
        val (result, played) = entry
        ContractStats( contract, Map( result->played.size ), played.size )
      }.foldLeft(ContractStats(contract)) { (ac,v) =>
        ac.add(v)
      }
    }.toList
  }

}
