package com.example.data.duplicate.stats

import com.example.data.Id
import com.example.data.MatchDuplicate
import com.example.data.Hand
import com.example.data.bridge.ContractSuit
import com.example.data.bridge.DuplicateBridge.ScoreHand
import utils.logging.Logger
import com.example.data.Team


/*
 * For a duplicate board, determine category of contract (pass, partial, game, slam, grand slam).
 *
 * if played by same side categorize them by
 *
 *      same type, lowertype, highertype
 *
 * Example:
 *    contract  NS    EW
 *    NS 4S   team1 team2
 *    NS 3S   team3 team4
 *
 * team1 highertype, team3 lowertype
 *
 * Determine result
 *
 *   For highertype
 *      - good (highertype made)
 *      - bad (highertype down, lowertype made)
 *      - neutral (all other)
 *
 * if played by different sides
 *
 * Example:
 *    contract  NS    EW
 *    NS 4S   team1 team2
 *    EW 3C   team3 team4
 *
 * Determine result
 *   if higher contract made  team1 good, team3 bad    (bad bidding by team3)
 *   if higher contract down, with lower score then other contract - team1 good, team3 bad  (good save by team1)
 *   if higher contract down, with higher score then other contract - team1 bad, team3 good (bad save by team1)
 *   all others neutral
 *
 */


object PlayerComparisonStat {
  type StatType = Int

  val SameSide: StatType = 1
  val Competitive: StatType = 2
  val PassedOut: StatType = 3

  def zero( player: String, stattype: StatType ) = PlayerComparisonStat(player,stattype)
  def aggressivegood( player: String, stattype: StatType ) = PlayerComparisonStat(player,stattype, aggressivegood=1)
  def aggressivebad( player: String, stattype: StatType ) = PlayerComparisonStat(player,stattype, aggressivebad=1)
  def aggressiveneutral( player: String, stattype: StatType ) = PlayerComparisonStat(player,stattype, aggressiveneutral=1)
  def passivegood( player: String, stattype: StatType ) = PlayerComparisonStat(player,stattype, passivegood=1)
  def passivebad( player: String, stattype: StatType ) = PlayerComparisonStat(player,stattype, passivebad=1)
  def passiveneutral( player: String, stattype: StatType ) = PlayerComparisonStat(player,stattype, passiveneutral=1)

}

import PlayerComparisonStat._

case class PlayerComparisonStat(
    player: String,
    stattype: StatType,
    aggressivegood: Int = 0,
    aggressivebad: Int = 0,
    aggressiveneutral: Int = 0,
    passivegood: Int = 0,
    passivebad: Int = 0,
    passiveneutral: Int = 0
) {

  def add( stat: PlayerComparisonStat ) = {
    if (player != stat.player || stattype != stat.stattype) throw new IllegalArgumentException("Player and/or sameside not the same")
    copy(
        aggressivegood = aggressivegood+stat.aggressivegood,
        aggressivebad = aggressivebad+stat.aggressivebad,
        aggressiveneutral = aggressiveneutral+stat.aggressiveneutral,
        passivegood = passivegood+stat.passivegood,
        passivebad = passivebad+stat.passivebad,
        passiveneutral = passiveneutral+stat.passiveneutral
    )
  }

  def forTotals = copy( player = "Totals" )
}

case class PlayerComparisonStats( data: List[PlayerComparisonStat] )

object PlayerComparisonStats {

  val log = Logger("bridge.PlayerComparisonStats")

  implicit class WrapperCompareContractHand( val hand: Hand ) extends AnyVal with Ordered[Hand] {

    /**
     * Result of comparing two hands using just contract tricks and contract suit.
     *
     * Returns `x` where:
     *   - `x < 0` when `hand < other`
     *   - `x == 0` when `hand == other`
     *   - `x > 0` when  `hand > other`
     *
     */
    def compare( other: Hand ): Int = {
      val t = hand.contractTricks.compare(other.contractTricks)
      if (t == 0) {
        if (hand.contractTricks == 0) 0
        else ContractSuit.compare(hand.contractSuit, other.contractSuit)
      } else {
        t
      }
    }

  }

  private def sameSide(
      aggressiveHand: Hand,
      aggressiveTeam: Team,
      passiveHand: Hand,
      passiveTeam: Team
  ) = {
    if (aggressiveHand.madeContract) {
      // 2 good, 1 bad
      PlayerComparisonStat.passivebad(passiveTeam.player1, SameSide)::
      PlayerComparisonStat.passivebad(passiveTeam.player2, SameSide)::
      PlayerComparisonStat.aggressivegood(aggressiveTeam.player1, SameSide)::
      PlayerComparisonStat.aggressivegood(aggressiveTeam.player2, SameSide)::
      Nil
    } else if (passiveHand.madeContract) {
      // 1 good, 2 bad
      PlayerComparisonStat.passivegood(passiveTeam.player1, SameSide)::
      PlayerComparisonStat.passivegood(passiveTeam.player2, SameSide)::
      PlayerComparisonStat.aggressivebad(aggressiveTeam.player1, SameSide)::
      PlayerComparisonStat.aggressivebad(aggressiveTeam.player2, SameSide)::
      Nil
    } else {
      // neutral
      PlayerComparisonStat.passiveneutral(passiveTeam.player1, SameSide)::
      PlayerComparisonStat.passiveneutral(passiveTeam.player2, SameSide)::
      PlayerComparisonStat.aggressiveneutral(aggressiveTeam.player1, SameSide)::
      PlayerComparisonStat.aggressiveneutral(aggressiveTeam.player2, SameSide)::
      Nil
    }
  }

  def competitiveAuction(
      aggressiveHand: Hand,
      aggressiveDeclarer: Team,
      passiveDealer: String,     // "NS" or "EW"
      passiveHand: Hand,
      passiveDefender: Team
  ) = {
    // 1 is lower contract, 2 is higher
    if (aggressiveHand.madeContract) {
      // 2 good, 1 bad
      PlayerComparisonStat.passivebad(passiveDefender.player1, Competitive)::
      PlayerComparisonStat.passivebad(passiveDefender.player2, Competitive)::
      PlayerComparisonStat.aggressivegood(aggressiveDeclarer.player1, Competitive)::
      PlayerComparisonStat.aggressivegood(aggressiveDeclarer.player2, Competitive)::
      Nil
    } else if (!passiveHand.madeContract) {
        // 1 good, 2 bad
        PlayerComparisonStat.passivegood(passiveDefender.player1, Competitive)::
        PlayerComparisonStat.passivegood(passiveDefender.player2, Competitive)::
        PlayerComparisonStat.aggressivebad(aggressiveDeclarer.player1, Competitive)::
        PlayerComparisonStat.aggressivebad(aggressiveDeclarer.player2, Competitive)::
        Nil
    } else {
      // h2 down, h1 made
      val (scorePassive, scoreAggressive) = if (passiveDealer=="NS") {
        (ScoreHand(passiveHand).score.ns, ScoreHand(aggressiveHand).score.ns)
      } else {
        (ScoreHand(passiveHand).score.ew, ScoreHand(aggressiveHand).score.ew)
      }
      if (scoreAggressive>scorePassive) {
        // 2 good, 1 bad
        PlayerComparisonStat.passivebad(passiveDefender.player1, Competitive)::
        PlayerComparisonStat.passivebad(passiveDefender.player2, Competitive)::
        PlayerComparisonStat.aggressivegood(aggressiveDeclarer.player1, Competitive)::
        PlayerComparisonStat.aggressivegood(aggressiveDeclarer.player2, Competitive)::
        Nil
      } else if (scoreAggressive < scorePassive) {
        // 1 good, 2 bad
        PlayerComparisonStat.passivegood(passiveDefender.player1, Competitive)::
        PlayerComparisonStat.passivegood(passiveDefender.player2, Competitive)::
        PlayerComparisonStat.aggressivebad(aggressiveDeclarer.player1, Competitive)::
        PlayerComparisonStat.aggressivebad(aggressiveDeclarer.player2, Competitive)::
        Nil
      } else {
        // neutral
        PlayerComparisonStat.passiveneutral(passiveDefender.player1, Competitive)::
        PlayerComparisonStat.passiveneutral(passiveDefender.player2, Competitive)::
        PlayerComparisonStat.aggressiveneutral(aggressiveDeclarer.player1, Competitive)::
        PlayerComparisonStat.aggressiveneutral(aggressiveDeclarer.player2, Competitive)::
        Nil
      }
    }
  }

  /**
   * @param aggressiveDealer
   * @param aggressiveHand
   * @param aggressiveDeclarer
   * @param passiveTeam The team at the passive table that is sitting in the same sets as the aggressive declarer
   *
   * Note: the passive hand is the passed out hand
   */
  def auctionWithPassedOut(
      aggressiveDealer: String,     // "NS" or "EW"
      aggressiveHand: Hand,
      aggressiveDeclarer: Team,
      passiveTeam: Team
  ) = {
    val scoreAggressive = if (aggressiveDealer=="NS") {
      ScoreHand(aggressiveHand).score.ns
    } else {
      ScoreHand(aggressiveHand).score.ew
    }

    if (scoreAggressive < 0) {
      PlayerComparisonStat.passivegood(passiveTeam.player1, PassedOut)::
      PlayerComparisonStat.passivegood(passiveTeam.player2, PassedOut)::
      PlayerComparisonStat.aggressivebad(aggressiveDeclarer.player1, PassedOut)::
      PlayerComparisonStat.aggressivebad(aggressiveDeclarer.player2, PassedOut)::
      Nil
    } else {
      PlayerComparisonStat.passivebad(passiveTeam.player1, PassedOut)::
      PlayerComparisonStat.passivebad(passiveTeam.player2, PassedOut)::
      PlayerComparisonStat.aggressivegood(aggressiveDeclarer.player1, PassedOut)::
      PlayerComparisonStat.aggressivegood(aggressiveDeclarer.player2, PassedOut)::
      Nil
    }

  }

  def stats( dups: Map[Id.MatchDuplicate,MatchDuplicate] ) = {

    val result = dups.values.flatMap { dup =>
      dup.boards.flatMap { board =>
        val hands = board.playedHands()
        hands.map { dh =>
          dh.played.head.declarer match {
            case "N" => "NS"
            case "S" => "NS"
            case "E" => "EW"
            case "W" => "EW"
            case _ => ""
          }
        } match {
          case List(d1, d2) =>
            val dh1 = hands.head
            val dh2 = hands.tail.head
            val h1 = dh1.played.head
            val h2 = dh2.played.head
            val ct1 = ContractType(h1)
            val ct2 = ContractType(h2)
            val c = ct1.compare(ct2)
            if (h1.contractTricks == 0 || h2.contractTricks == 0) {
              if (h1.contractTricks == 0) {
                val (t1,t2) = if (d2 == "NS") {
                  (dup.getTeam(dh1.nsTeam).get,dup.getTeam(dh2.nsTeam).get)
                } else {
                  (dup.getTeam(dh1.ewTeam).get,dup.getTeam(dh2.ewTeam).get)
                }
                auctionWithPassedOut(d2, h2, t2, t1)
              } else {
                val (t1,t2) = if (d1 == "NS") {
                  (dup.getTeam(dh1.nsTeam).get,dup.getTeam(dh2.nsTeam).get)
                } else {
                  (dup.getTeam(dh1.ewTeam).get,dup.getTeam(dh2.ewTeam).get)
                }
                auctionWithPassedOut(d1, h1, t1, t2)
              }
            } else if (d1 == d2) {
              // contract was played by same side
              val (t1,t2) = if (d1 == "NS") {
                (dup.getTeam(dh1.nsTeam).get,dup.getTeam(dh2.nsTeam).get)
              } else {
                (dup.getTeam(dh1.ewTeam).get,dup.getTeam(dh2.ewTeam).get)
              }
              if (c == 0) {
                Nil
              } else {
                if (c < 0) {
                  // 1 is lower contract, 2 is higher contract
                  sameSide(h2, t2, h1, t1)
                } else {
                  // 2 is lower contract, 1 is higher contract
                  sameSide(h1, t1, h2, t2)
                }
              }
            } else {
              // contract was played by different sides
              val (t1dec,t1def,t2dec,t2def) = if (d1 == "NS") {
                (dup.getTeam(dh1.nsTeam).get,dup.getTeam(dh1.ewTeam).get,dup.getTeam(dh2.ewTeam).get,dup.getTeam(dh2.nsTeam).get)
              } else {
                (dup.getTeam(dh1.ewTeam).get,dup.getTeam(dh1.nsTeam).get,dup.getTeam(dh2.nsTeam).get,dup.getTeam(dh2.ewTeam).get)
              }
              if (c == 0) {
                // neutral
                Nil
              } else if (c < 0) {
                // 1 is lower contract, 2 is higher
                competitiveAuction(h2, t2dec, d1, h1, t1def)
              } else {
                // 2 is lower contract, 1 is higher
                competitiveAuction(h1, t1dec, d2, h2, t2def)
              }
            }
          case _ =>
            // ignore for now
            Nil
        }
      }
    }
    val r =
    result.groupBy(pcs => (pcs.player,pcs.stattype)).map { entry =>
      val ((player,stattype), list ) = entry
      list.foldLeft(PlayerComparisonStat.zero(player,stattype)) { (ac,v) =>
        ac.add(v)
      }
    }.toList
    val pcss = PlayerComparisonStats(r)
    log.fine( pcss.toString() )
    pcss
  }

}