package com.github.thebridsk.bridge.data.bridge

import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.DuplicateSummaryDetails
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.data.Board

case class DuplicateException(message: String) extends Exception(message)

sealed trait DuplicateViewPerspective

case object PerspectiveDirector extends DuplicateViewPerspective
case class PerspectiveTable(teamId1: Team.Id, teamId2: Team.Id)
    extends DuplicateViewPerspective
case object PerspectiveComplete extends DuplicateViewPerspective

class MatchDuplicateScore private (
    val duplicate: MatchDuplicate,
    val perspective: DuplicateViewPerspective
) {
  import MatchDuplicateScore._

  val id = duplicate.id

  val created = duplicate.created
  val updated = duplicate.updated

  val boards: Map[Board.Id, BoardScore] =
    duplicate.boards.map(b => (b.id -> BoardScore(b, perspective))).toMap

  val sortedBoards: List[BoardScore] =
    boards.values.toList.sortWith((b1, b2) => b1.id < b2.id)

  val alldone: Boolean = !boards.values.exists(b => !b.allplayed)

  def isStarted: Boolean = {
    val ts = duplicate.teams.exists(t => t.areBothPlayersSet())
    ts || boards.values.exists(bb => bb.anyplayed)
  }

  val teams = duplicate.teams

  def getTeam(tid: Team.Id): Option[Team] = duplicate.getTeam(tid)

  val teamScores: Map[Team.Id, Double] = duplicate.teams
    .map(team =>
      (team.id -> {
        var points: Double = 0
        boards.values.foreach { b =>
          {
            points += {
              b.scores().get(team.id) match {
                case Some(tbs) => tbs.points
                case None      => 0
              }
            }
          }
        }
        points
      })
    )
    .toMap

  val teamImps: Map[Team.Id, Double] = duplicate.teams
    .map(team =>
      (team.id -> {
        var points: Double = 0
        boards.values.foreach { b =>
          {
            points += {
              b.scores().get(team.id) match {
                case Some(tbs) => tbs.imps
                case None      => 0
              }
            }
          }
        }
        points
      })
    )
    .toMap

  val places: List[Place] = {
    val m = teamScores.groupBy(e => e._2).map { e =>
      val (points, teams) = e
      points -> teams.keys.map(tid => getTeam(tid).get).toList
    }
    val sorted = m.toList.sortWith((e1, e2) => e1._1 > e2._1)
    var place = 1
    sorted.map(e => {
      val (points, ts) = e
      val p = place
      place += ts.size
      Place(p, points, ts)
    })
  }

  val placesImps: List[Place] = {
    val m = teamImps.groupBy(e => e._2).map { e =>
      val (points, teams) = e
      points -> teams.keys.map(tid => getTeam(tid).get).toList
    }
    val sorted = m.toList.sortWith((e1, e2) => e1._1 > e2._1)
    var place = 1
    sorted.map(e => {
      val (points, ts) = e
      val p = place
      place += ts.size
      Place(p, points, ts)
    })
  }

  def placeByWinnerSet(winnerset: List[Team.Id]): List[Place] = {
    places.flatMap(p => {
      val pteam = p.teams.filter(t => winnerset.contains(t.id))
      if (pteam.isEmpty) Nil
      else p.copy(teams = pteam) :: Nil
    })
  }

  def placeImpByWinnerSet(winnerset: List[Team.Id]): List[Place] = {
    placesImps.flatMap(p => {
      val pteam = p.teams.filter(t => winnerset.contains(t.id))
      if (pteam.isEmpty) Nil
      else p.copy(teams = pteam) :: Nil
    })
  }

  val tables: Map[Table.Id, List[Round]] = {
    import scala.collection.mutable
    val tables: mutable.Map[Table.Id, mutable.Map[Int, Round]] = mutable.Map()

    boards.foreach {
      case (bid, board) =>
        board.board.hands.foreach { hand =>
          {
            val table = hand.table
            val roundmap = tables.get(table) match {
              case Some(map) => map
              case None =>
                val map = mutable.Map[Int, Round]()
                tables += (table -> map)
                map
            }
            val round = hand.round
            val ns = duplicate.getTeam(hand.nsTeam).get
            val ew = duplicate.getTeam(hand.ewTeam).get
            val newround = roundmap.get(round) match {
              case Some(r) =>
                if (ns != r.ns || ew != r.ew)
                  throw DuplicateException(
                    "NS and EW don't match for hand on table " + hand.table + " board " + hand.board + " with other hands in round " + round
                  )
                r.copy(boards = (board :: r.boards))
              case None =>
                Round(table, round, ns, ew, List(board))
            }
            roundmap += (round -> newround)
          }
        }
    }

    tables.map {
      case (table, rounds) => {
        (table -> List(
          rounds.values.toList
            .sortWith((r1, r2) => r1.round.compareTo(r2.round) < 0)
            .toList: _*
        ).map { r =>
          r.copy(
            boards = r.boards.sortWith((r1, r2) => r1.id < r2.id)
          )
        })
      }
    }.toMap
  }

  def getRound(table: Table.Id, round: Int): Option[Round] = {
    tables.get(table) match {
      case Some(rounds) =>
        rounds.find { r =>
          r.round == round
        }
      case None => None
    }
  }

  def getRoundForAllTables(round: Int): List[Round] = {
    tables.flatMap { entry =>
      val (table, rounds) = entry
      rounds.find { r =>
        r.round == round
      }
    }.toList
  }

  def allRounds: List[Int] = {
    tables
      .flatMap { entry =>
        val (table, rounds) = entry
        rounds.map(r => r.round)
      }
      .toList
      .distinct
  }

  def matchHasRelay: Boolean = {
    allRounds.find { ir =>
      val all =
        getRoundForAllTables(ir).flatMap(r => r.boards.map(bs => bs.board.id))
      val distinct = all.distinct
      all.length != distinct.length
    }.isDefined
  }

  /**
    * @returns table IDs
    */
  def tableRoundRelay(itable: Table.Id, iround: Int): List[Table.Id] = {
    val allRounds = getRoundForAllTables(iround)
    val otherRounds = allRounds.filter(r => r.table != itable)
    val otherBoards = otherRounds.flatMap(r => r.boards.map(bs => bs.board.id))
    allRounds
      .find(r => r.table == itable)
      .map { table =>
        val relays = table.boards
          .flatMap { bs =>
            val bid = bs.board.id
            otherRounds.flatMap { r =>
              r.boards.find(bs => bs.board.id == bid).map(bs => r.table)
            }
          }
          .distinct
          .sorted
        relays
      }
      .getOrElse(Nil)
  }

  /**
    * Get all the table Ids in sort order.
    */
  def getTableIds: List[Table.Id] = {
    tables.keys
      .map { id =>
        id.asInstanceOf[Table.Id]
      }
      .toSet
      .toList
      .sorted
  }

  def getBoardSet = duplicate.boardset

  def getMovement = duplicate.movement

  /**
    * Get the winner sets.  From each set a winner should be declared.
    * @return a list of the winner sets.  A winner set is a list of team Ids.
    */
  def getWinnerSets: List[List[Team.Id]] = {
    // key is a team, value are the opponents of key
    val winnersets =
      scala.collection.mutable.Map[Team.Id, List[Team.Id]]()
    def addTeam(t1: Team.Id, t2: Team.Id) = {
      val cur = winnersets.get(t1) match {
        case Some(l) => l
        case None    => Nil
      }
      val next = if (cur.contains(t2)) cur else t2 :: cur
      winnersets.put(t1, next)
    }
    def add(h: DuplicateHand) = {
      addTeam(h.nsTeam, h.ewTeam)
      addTeam(h.ewTeam, h.nsTeam)
    }
    duplicate.boards.foreach(b => b.hands.foreach(h => add(h)))
    val sets =
      winnersets.values.map(l => l.sorted).toSeq.distinct.toList
    if (sets.size != 2) {
      List(duplicate.teams.map(t => t.id).toList.sorted)
    } else {
      val List(s1, s2) = sets
      if (
        s1.filter(k => s2.contains(k)).isEmpty && s2
          .filter(k => s1.contains(k))
          .isEmpty
      ) {
        // the two lists don't have any common entries
        // check if everyone in a set was at the same position
        val allSamePos = duplicate.boards
          .map(b => {
            val (ns, ew) = b.hands.map(h => (h.nsTeam, h.ewTeam)).unzip
            if (ns.contains(s1.head)) {
              val s1AllNS = s1.map(p => ns.contains(p)).find(n => !n).isEmpty
              val s2AllEW = s2.map(p => ew.contains(p)).find(n => !n).isEmpty
              s1AllNS && s2AllEW
            } else {
              val s2AllNS = s2.map(p => ns.contains(p)).find(n => !n).isEmpty
              val s1AllEW = s1.map(p => ew.contains(p)).find(n => !n).isEmpty
              s2AllNS && s1AllEW
            }
          })
          .find(ok => !ok)
          .isEmpty

        if (allSamePos) sets
        else List(duplicate.teams.map(t => t.id).toList.sorted)
      } else {
        // the two sets have at least one entry in common
        List(duplicate.teams.map(t => t.id).toList.sorted)
      }
    }
  }

  def getDetails: List[DuplicateSummaryDetails] = {
    duplicate.boards
      .flatMap { b =>
        b.hands.flatMap { dh =>
          dh.played.flatMap { h =>
            val (declarer, defender) = h.declarer match {
              case "N" | "S" =>
                (dh.nsTeam, dh.ewTeam)
              case "E" | "W" =>
                (dh.ewTeam, dh.nsTeam)
            }
            if (h.contractTricks == 0) {
              DuplicateSummaryDetails.passed(
                declarer
              ) :: DuplicateSummaryDetails
                .passed(defender) :: Nil
            } else {
              if (h.madeContract) {
                DuplicateSummaryDetails.made(
                  declarer
                ) :: DuplicateSummaryDetails
                  .allowedMade(defender) :: Nil
              } else {
                DuplicateSummaryDetails.down(
                  declarer
                ) :: DuplicateSummaryDetails
                  .tookDown(defender) :: Nil
              }
            }
          }
        }
      }
      .foldLeft(Map[Team.Id, DuplicateSummaryDetails]()) { (ac, v) =>
        val c = ac.get(v.team).getOrElse(DuplicateSummaryDetails.zero(v.team))
        ac + (v.team -> (c.add(v)))
      }
      .toList
      .sortBy { e =>
        e._1
      }
      .map { e =>
        e._2
      }
  }

  def isMP = duplicate.isMP
  def isIMP = duplicate.isIMP

}

object MatchDuplicateScore {

  case class TeamScore(team: Team.Id, points: Double)

  case class Round(
      table: Table.Id,
      round: Int,
      ns: Team,
      ew: Team,
      boards: List[BoardScore]
  ) {
    def allUnplayedOnTable: Boolean =
      boards.find { bs =>
        bs.hasTeamPlayed(ns.id)
      }.isEmpty
    def complete: Boolean =
      boards
        .filter { bs =>
          bs.board.getHand(ns.id).isDefined
        }
        .find { bs =>
          !bs.hasTeamPlayed(ns.id)
        }
        .isEmpty

    /**
      * @return a tuple 2.  The first is the played boards, the second is the unplayed boards.
      */
    def playedAndUnplayedBoards: (List[BoardScore], List[BoardScore]) = {
      boards.partition { b =>
        b.hasTeamPlayed(ns.id)
      }
    }

    override def toString(): String =
      s"""[Round table=${table}, round=${round}, ns=${ns.id}, ew=${ew.id}, boards=${boards
        .mkString("\n  ", "\n  ", "")}]"""
  }

  case class Place(place: Int, score: Double, teams: List[Team])

  def apply(duplicate: MatchDuplicate, perspective: DuplicateViewPerspective) =
    new MatchDuplicateScore(duplicate, perspective)
}
