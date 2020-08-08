package com.github.thebridsk.bridge.data.duplicate.stats

import java.io.PrintStream
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.Hand

case class CounterStat(tricks: Int, counter: Int) {

  def add(other: CounterStat): CounterStat = {
    copy(counter = counter + other.counter)
  }
}

object CounterStat {

  def addTo(cs: CounterStat, list: List[CounterStat]): List[CounterStat] = {
    val i = list.indexWhere(x => x.tricks == cs.tricks)
    if (i < 0) {
      // not found
      cs :: list
    } else {
      val (before, at :: after) = list.splitAt(i)
      before ::: at.add(cs) :: after
    }
  }

}

sealed trait ContractType extends Ordered[ContractType] {
  val value: String

  def toString(): String

  def compare(other: ContractType): Int = {
    ContractType.getRank(this).compare(ContractType.getRank(other))
  }
}

object ContractType {

  def apply(hand: Hand): ContractType = {
    if (hand.contractTricks == 0) ContractTypePassed
    else if (hand.contractTricks == 7) ContractTypeGrandSlam
    else if (hand.contractTricks == 6) ContractTypeSlam
    else if (hand.contractTricks == 5) ContractTypeGame
    else if (
      hand.contractTricks == 4 && hand.contractSuit != "C" && hand.contractSuit != "D"
    )
      ContractTypeGame
    else if (hand.contractTricks == 3 && hand.contractSuit == "N")
      ContractTypeGame
    else ContractTypePartial
  }

  def getRank(ct: ContractType): Int = {
    ct match {
      case ContractTypePassed        => 0
      case ContractTypePartial       => 1
      case ContractTypeDoubledToGame => 2
      case ContractTypeGame          => 3
      case ContractTypeSlam          => 4
      case ContractTypeGrandSlam     => 5
      case _                         => -1
    }
  }
}

case object ContractTypeGrandSlam extends ContractType {
  val value = "GS"
  override def toString() = "Grand Slam"
}
case object ContractTypeSlam extends ContractType {
  val value = "S"
  override def toString() = "Slam"
}
case object ContractTypeGame extends ContractType {
  val value = "G"
  override def toString() = "Game"
}
case object ContractTypeDoubledToGame extends ContractType {
  val value = "D"
  override def toString() = "Doubled To Game"
}
case object ContractTypePartial extends ContractType {
  val value = "P"
  override def toString() = "Partial"
}
case object ContractTypePassed extends ContractType {
  val value = "Z"
  override def toString() = "Passed"
}
case object ContractTypeTotal extends ContractType {
  val value = "T"
  override def toString() = "Total"
}

/**
  * @param player
  * @param declarer true if player's team was declarer, false defender, false if hand was passed out.
  * @param contractType
  * @param histogram map of tricks to counter.  tricks is the number of tricks from contract.  0 is made exactly contract.  >0 made contract with that many extra tricks.  <0 contract is down.
  *                  empty map if contractType is passed out.
  * @param handsPlayed
  */
case class PlayerStat(
    player: String,
    declarer: Boolean,
    contractType: String,
    histogram: List[CounterStat] = List(),
    handsPlayed: Int = 0
) {
  def add(other: PlayerStat): PlayerStat = {
    if (
      player != other.player || declarer != other.declarer || contractType != other.contractType
    ) {
      throw new Exception("player or declarer is not the same")
    }
    val np = handsPlayed + other.handsPlayed

    val h = other.histogram.foldLeft(histogram) { (ac, v) =>
      CounterStat.addTo(v, ac)
    }

    copy(histogram = h, handsPlayed = np)
  }

  def csvHeader(min: Int = -13, max: Int = 6): String = {
    val r = (min to max).map { i =>
      if (i < 0) s"Down ${-i}"
      else if (i == 0) "Made"
      else s"Made +${i}"
    }
    s"""Player,Declarer,ContractType,handsPlayed,${r.mkString(",")}"""
  }

  def toCsv(min: Int = -13, max: Int = 6, percent: Boolean = false): String = {
    val h = histogram.map(cs => (cs.tricks -> cs.counter)).toMap
    val r = (min to max).map { i =>
      val c = h.get(i).getOrElse(0)
      if (percent) f"${100.0 * c / handsPlayed}%.2f"
      else c.toString()
    }

    s""""${player}",${declarer},${contractType},${handsPlayed},${r.mkString(
      ","
    )}"""
  }

  def toCsvPercent(min: Int = -13, max: Int = 6): String = {
    toCsv(min, max, true)
  }

  def normalize: PlayerStat = copy(histogram = histogram.sortBy(x => x.tricks))

  def getCounter(tricks: Int): Option[Int] =
    histogram.find(h => h.tricks == tricks).map(h => h.counter)
}

case class PlayerStats(
    declarer: List[PlayerStat],
    defender: List[PlayerStat],
    min: Int,
    max: Int
) {

  /**
    * Returns the declarer and defender stats for the player
    */
  def stats(player: String): (List[PlayerStat], List[PlayerStat]) = {
    (
      declarer.filter(ps => ps.player == player),
      defender.filter(ps => ps.player == player)
    )
  }

}

object PlayerStats {

  /**
    * @param player
    * @param declarer true if player's team was declarer, false defender.  false if hand was passed out.
    * @param contractType
    * @param result
    */
  case class GameStat(
      player: String,
      declarer: Boolean,
      contractType: ContractType,
      result: Int
  )

  def csvHeader(min: Int = -13, max: Int = 6): String =
    PlayerStat("", true, "").csvHeader(min, max)

  def statsToCsv(stats: PlayerStats, percent: Boolean = false)(implicit
      out: PrintStream
  ): Unit = {
    val tocsv: (PlayerStat, Int, Int) => String =
      if (percent) (ds, min, max) => ds.toCsvPercent(min, max)
      else (ds, min, max) => ds.toCsv(min, max)
    (stats.declarer :: stats.defender :: Nil).foreach { sts =>
      out.println(PlayerStats.csvHeader(stats.min, stats.max))
      sts.foreach { ds =>
        out.println(tocsv(ds, stats.min, stats.max))
      }
    }

    out.flush()
  }

  def statsToCsvPercent(stats: PlayerStats)(implicit out: PrintStream): Unit = {
    statsToCsv(stats, true)
  }

  def stats(dups: Map[MatchDuplicate.Id, MatchDuplicate]): PlayerStats = {
    val results = dups.values.flatMap { dup =>
      dup.allPlayedHands.flatMap { dh =>
        val nsTeam = dup.getTeam(dh.nsTeam).get
        val ewTeam = dup.getTeam(dh.ewTeam).get
        val (declarer, defender) = dh.played.head.declarer match {
          case "N" | "S" => (nsTeam, ewTeam)
          case "E" | "W" => (ewTeam, nsTeam)
        }
        if (dh.played.head.contractTricks == 0) {
          GameStat(declarer.player1, false, ContractTypePassed, 0) ::
            GameStat(declarer.player2, false, ContractTypePassed, 0) ::
            GameStat(defender.player1, false, ContractTypePassed, 0) ::
            GameStat(defender.player2, false, ContractTypePassed, 0) ::
            Nil
        } else {
          val ct = ContractType(dh.played.head)
          val r =
            if (dh.played.head.madeContract)
              dh.played.head.tricks - dh.played.head.contractTricks
            else -dh.played.head.tricks
          GameStat(declarer.player1, true, ct, r) ::
            GameStat(declarer.player2, true, ct, r) ::
            GameStat(defender.player1, false, ct, r) ::
            GameStat(defender.player2, false, ct, r) ::
            Nil
        }
      }
    }
    val counters = results.groupBy(gs => gs.declarer).map { entry =>
      val (declarer, value) = entry
      (declarer -> value.groupBy(gs => gs.player).map { entry2 =>
        val (player, value2) = entry2
        (player -> value2.groupBy(gs => gs.contractType).map { entry3 =>
          val (ct, value3) = entry3
          (ct -> value3.groupBy(gs => gs.result).map { entry4 =>
            val (result, value4) = entry4
            (result -> value4)
          })
        })
      })
    }
    val d = counters.map { decentry =>
      val (declarer, decmap) = decentry
      (declarer -> decmap
        .flatMap { decplayentry =>
          val (player, decplaymap) = decplayentry
          decplaymap.map { decplayresult =>
            val (ct, decplayctmap) = decplayresult
            val ps = decplayctmap
              .map { decplayresult =>
                val (result, x) = decplayresult
                val played = x.size
                PlayerStat(
                  player,
                  declarer,
                  ct.value,
                  List(CounterStat(result, played)),
                  played
                )
              }
              .foldLeft(PlayerStat(player, declarer, ct.value)) { (ac, v) =>
                ac.add(v)
              }
            ps.copy(histogram = ps.histogram.sortBy(s => s.tricks))
          }
        }
        .toList
        .sortBy(ps => ps.player))
    }

    val (min, max) = d
      .flatMap { args =>
        val (declarer, lps) = args
        lps.flatMap(ps => ps.histogram.map(cs => cs.tricks))
      }
      .foldLeft((0, 0)) { (ac, v) =>
        (Math.min(ac._1, v), Math.max(ac._2, v))
      }

    PlayerStats(d.getOrElse(true, List()), d.getOrElse(false, List()), min, max)
  }

}
