package com.github.thebridsk.bridge.data.duplicate.stats

import java.io.PrintStream
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.bridge.NotDoubled

object PlayerDoubledStats {

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

  def csvHeader(min: Int = -13, max: Int = 6) =
    PlayerStat("", true, "").csvHeader(min, max)

  def statsToCsv(stats: PlayerStats, percent: Boolean = false)(
      implicit out: PrintStream
  ) = {
    val tocsv: (PlayerStat, Int, Int) => String =
      if (percent)(ds, min, max) => ds.toCsvPercent(min, max)
      else (ds, min, max) => ds.toCsv(min, max)
    (stats.declarer :: stats.defender :: Nil).foreach { sts =>
      out.println(PlayerStats.csvHeader(stats.min, stats.max))
      sts.foreach { ds =>
        out.println(tocsv(ds, stats.min, stats.max))
      }
    }

    out.flush()
  }

  def statsToCsvPercent(stats: PlayerStats)(implicit out: PrintStream) = {
    statsToCsv(stats, true)
  }

  def getContractType(hand: Hand): Option[ContractType] = {
    if (hand.contractDoubled == NotDoubled.doubled) None
    else {
      val ct =
        if (hand.contractTricks == 7) ContractTypeGrandSlam
        else if (hand.contractTricks == 6) ContractTypeSlam
        else {
          hand.contractSuit match {
            case "N" =>
              if (hand.contractTricks >= 3) ContractTypeGame
              else if (hand.contractTricks == 2) ContractTypeDoubledToGame
              else ContractTypePartial
            case "S" | "H" =>
              if (hand.contractTricks >= 4) ContractTypeGame
              else if (hand.contractTricks >= 2) ContractTypeDoubledToGame
              else ContractTypePartial
            case "D" | "C" =>
              if (hand.contractTricks >= 5) ContractTypeGame
              else if (hand.contractTricks >= 3) ContractTypeDoubledToGame
              else ContractTypePartial
            case _ =>
              ContractTypePartial
          }
        }
      Some(ct)
    }
  }

  def stats(dups: Map[MatchDuplicate.Id, MatchDuplicate]) = {
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
          getContractType(dh.played.head) match {
            case Some(ct) =>
              val r =
                if (dh.played.head.madeContract)
                  dh.played.head.tricks - dh.played.head.contractTricks
                else -dh.played.head.tricks
              GameStat(declarer.player1, true, ct, r) ::
                GameStat(declarer.player2, true, ct, r) ::
                GameStat(defender.player1, false, ct, r) ::
                GameStat(defender.player2, false, ct, r) ::
                Nil
            case None =>
              Nil
          }
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
