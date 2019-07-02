package com.example.data.duplicate.stats

import java.io.PrintStream
import com.example.data.Id
import com.example.data.MatchDuplicate
import com.example.data.Hand

/**
  * @param contract
  * @param contractType the value field of a ContractType object.
  * @param histogram map of tricks to counter.  tricks is the number of tricks from contract.  0 is made exactly contract.  >0 made contract with that many extra tricks.  <0 contract is down.
  * @param handsPlayed
  */
case class ContractStat(
    contract: String,
    contractType: String,
    histogram: List[CounterStat] = List(),
    handsPlayed: Int = 0
) {

  def add(other: ContractStat) = {
    if (contract != other.contract)
      throw new Exception("contract is not the same")
    val np = handsPlayed + other.handsPlayed

    val h = other.histogram.foldLeft(histogram) { (ac, v) =>
      CounterStat.addTo(v, ac)
    }

    copy(histogram = h, handsPlayed = np)
  }

  def csvHeader(min: Int = -13, max: Int = 6) = {
    val r = (min to max).map { i =>
      if (i < 0) s"Down ${-i}"
      else if (i == 0) "Made"
      else s"Made +${i}"
    }
    s"""Contract,type,handsPlayed,${r.mkString(",")}"""
  }

  def toCsv(min: Int = -13, max: Int = 6, percent: Boolean = false) = {
    val h = histogram.map(cs => (cs.tricks -> cs.counter)).toMap
    val r = (min to max).map { i =>
      val c = h.get(i).getOrElse(0)
      if (percent) f"${100.0 * c / handsPlayed}%.2f"
      else c.toString()
    }

    s""""${contract},${contractType},${handsPlayed},${r.mkString(",")}"""
  }

  def toCsvPercent(min: Int = -13, max: Int = 6) = {
    toCsv(min, max, true)
  }

  def normalize = copy(histogram = histogram.sortBy(x => x.tricks))

  def parseContract = ContractStats.parseContract(contract)

  def isTotal = contract.startsWith("T")
}

case class ContractStats(
    val data: List[ContractStat],
    min: Int,
    max: Int
)

object ContractStats {

  /**
    * @param contract
    * @param contractType the value field of a ContractType object.
    * @param result
    */
  case class GameStat(contract: String, contractType: String, result: Int)

  def csvHeader(min: Int = -13, max: Int = 6) =
    ContractStat("", ContractTypePassed.value).csvHeader(min, max)

  case class Contract(
      total: String,
      tricks: String,
      suit: String,
      doubled: String
  ) {

    def isTotal = total == "T"

    def isPassedOut = suit == "P"

  }

  private val patternContract = """([T]?)(\d)([CDHSN])([*]*)""".r

  /**
    * @param c the contract string from a ContractStat object
    * @return a Tuple4 of Strings.
    *   _1 - "T" - if this is a total for contract without regards to doubling
    *        ""  - if this is a contract with doubling
    *   _2 - the number of tricks as a string
    *   _3 - the first letter of the suit or
    *          "P" - passed out
    *          "Z" - no trump
    *   _4 - ""   - not doubled
    *        "*"  - doubled
    *        "**" - redoubled
    * If the contract is not valid, then ("","","", c ) is returned.
    */
  def parseContract(c: String) = {
    c match {
      case "PassedOut" => Contract("", "0", "P", "")
      case patternContract(a, t, s, d) =>
        val ss = if (s == "N") "Z" else s
        Contract(a, t, ss, d)
      case _ => Contract("", "", "", c)
    }
  }

  def compare(l: ContractStat, r: ContractStat) = {
    val lc = l.parseContract
    val rc = r.parseContract
    if (lc.total != rc.total) lc.total < rc.total
    else if (lc.tricks != rc.tricks) lc.tricks < rc.tricks
    else if (lc.suit != rc.suit) lc.suit < rc.suit
    else lc.doubled < rc.doubled
  }

  def statsToCsv(stats: ContractStats, percent: Boolean = false)(
      implicit out: PrintStream
  ) = {
    val tocsv: (ContractStat, Int, Int) => String =
      if (percent)(ds, min, max) => ds.toCsvPercent(min, max)
      else (ds, min, max) => ds.toCsv(min, max)
    val sts = stats.data
    out.println(ContractStats.csvHeader(stats.min, stats.max))
    sts.foreach { ds =>
      out.println(tocsv(ds, stats.min, stats.max))
    }
    out.flush()
  }

  def statsToCsvPercent(stats: ContractStats)(implicit out: PrintStream) = {
    statsToCsv(stats, true)
  }

  def stats(
      dups: Map[Id.MatchDuplicate, MatchDuplicate],
      aggregateDouble: Boolean = true
  ) = {
    val results = dups.values.flatMap { dup =>
      dup.allPlayedHands.flatMap { dh =>
        if (dh.played.head.contractTricks == 0) {
          GameStat("PassedOut", ContractTypePassed.value, 0) :: Nil
        } else {
          val hand = dh.played.head
          val ct = ContractType(hand)
          val r =
            if (dh.played.head.madeContract)
              dh.played.head.tricks - dh.played.head.contractTricks
            else -dh.played.head.tricks
          val ss = GameStat(dh.played.head.contract, ct.value, r) :: Nil
          if (aggregateDouble) {
            GameStat(
              s"T${dh.played.head.contractTricks}${dh.played.head.contractSuit}",
              ct.value,
              r
            ) :: ss
          } else {
            ss
          }
        }
      }
    }
    val d = results
      .groupBy(gs => gs.contract)
      .map { entry =>
        val (contract, value) = entry
        val ct = value.headOption
          .map(gs => gs.contractType)
          .getOrElse(ContractTypePassed.value)
        val stat = value
          .groupBy(gs => gs.result)
          .map { entry =>
            val (result, played) = entry
            ContractStat(
              contract,
              ct,
              List(CounterStat(result, played.size)),
              played.size
            )
          }
          .foldLeft(ContractStat(contract, ct)) { (ac, v) =>
            ac.add(v)
          }
        stat.copy(histogram = stat.histogram.sortBy(s => s.tricks))
      }
      .toList
      .sortWith(compare)

    val (min, max) = d
      .flatMap { cs =>
        cs.histogram.map(c => c.tricks)
      }
      .foldLeft((0, 0)) { (ac, v) =>
        (Math.min(ac._1, v), Math.max(ac._2, v))
      }

    ContractStats(d, min, max)
  }

}
