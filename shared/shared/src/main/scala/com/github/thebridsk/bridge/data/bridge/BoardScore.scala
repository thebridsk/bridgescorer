package com.github.thebridsk.bridge.data.bridge

import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.bridge.DuplicateBridge.ScoreHand
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.Team

case class ContractForScore(
    contract: String,
    declarer: String,
    made: Option[String],
    down: Option[String]
)

/**
  * The score for a team on a board
  * @param teamId the team that played the board
  * @param isNS was the team north-south
  * @param played has it been played
  * @param hidden has the score and points been hidden
  * @param score the score if played
  * @param points the duplicate points scored
  */
case class TeamBoardScore(
    teamId: Team.Id,
    isNS: Boolean,
    played: Boolean,
    hidden: Boolean,
    score: Int,
    points: Double,
    contract: Option[ContractForScore],
    opponent: Option[Team.Id],
    imps: Double
) {
  def showScore: String =
    if (played) {
      if (hidden) "?"
      else score.toString
    } else {
      ""
    }

  def showPoints: String =
    if (played) {
      if (hidden) "?"
      else points.toString
    } else {
      ""
    }

  def showImps: String =
    if (played) {
      if (hidden) "?"
      else f"${imps}%.1f"
    } else {
      ""
    }

  def getPoints: Either[String, Double] =
    if (played) {
      if (hidden) Left("?")
      else Right(points)
    } else {
      Left("")
    }

  def getImps: Either[String, Double] =
    if (played) {
      if (hidden) Left("?")
      else Right(imps)
    } else {
      Left("")
    }

  def showContract: String =
    if (played) {
      if (hidden) "?"
      else
        contract match {
          case Some(c) => c.contract
          case None    => ""
        }
    } else {
      ""
    }
  def showDeclarer: String =
    if (played) {
      if (hidden) "?"
      else
        contract match {
          case Some(c) => c.declarer
          case None    => ""
        }
    } else {
      ""
    }
  def showMade: String =
    if (played) {
      if (hidden) "?"
      else
        contract match {
          case Some(c) => c.made.getOrElse("")
          case None    => ""
        }
    } else {
      ""
    }
  def showDown: String =
    if (played) {
      if (hidden) "?"
      else
        contract match {
          case Some(c) => c.down.getOrElse("")
          case None    => ""
        }
    } else {
      ""
    }
}

object TeamBoardScore {
  def apply(
      teamId: Team.Id,
      played: Boolean,
      hidden: Boolean,
      score: Int,
      points: Double
  ) =
    new TeamBoardScore(
      teamId,
      false,
      played,
      hidden,
      score,
      points,
      None,
      None,
      0
    )
  def apply(
      teamId: Team.Id,
      played: Boolean,
      hidden: Boolean,
      score: Int,
      points: Double,
      opponent: Team.Id
  ) =
    new TeamBoardScore(
      teamId,
      true,
      played,
      hidden,
      score,
      points,
      None,
      Some(opponent),
      0
    )
  def apply(
      teamId: Team.Id,
      played: Boolean,
      hidden: Boolean,
      score: Int,
      points: Double,
      opponent: Team.Id,
      contract: ContractForScore
  ) =
    new TeamBoardScore(
      teamId,
      true,
      played,
      hidden,
      score,
      points,
      Some(contract),
      Some(opponent),
      0
    )
}

/**
  * Score the board from the perspective of the table that has team1 and team2.
  * The scores are only shown if both teams have played the board.
  */
class BoardScore(val board: Board, perspective: DuplicateViewPerspective) {

  /**
    * allplayed is true if all hands have been played
    * anyplayed is true if some, but not all have been played
    * nsScores
    * ewScores
    */
  private val (internalallplayed, internalanyplayed, nsScores, ewScores): (
      Boolean,
      Boolean,
      List[TeamBoardScore],
      List[TeamBoardScore]
  ) = {
    var ns: List[TeamBoardScore] = Nil
    var ew: List[TeamBoardScore] = Nil
    var allplay = true
    var anyplay = false

    for (h <- board.hands) {
      if (h.wasPlayed) {
        anyplay = true
        val sh = ScoreHand(h.hand.get)
        val contract = ContractForScore(
          sh.contractAsString("Vul", ""),
          sh.getDeclarerForScoring,
          sh.getMadeForScoring,
          sh.getDownForScoring
        )
        ns = TeamBoardScore(
          h.nsTeam,
          true,
          false,
          sh.score.ns,
          0,
          h.ewTeam,
          contract
        ) :: ns
        ew = TeamBoardScore(h.ewTeam, true, false, sh.score.ew, 0) :: ew
      } else {
        allplay = false
        ns = TeamBoardScore(h.nsTeam, false, false, 0, 0, h.ewTeam) :: ns
        ew = TeamBoardScore(h.ewTeam, false, false, 0, 0) :: ew
      }
    }
    (allplay, !allplay && anyplay, calculatePoints(ns), calculatePoints(ew))
  }

  val allplayed = internalallplayed
  val anyplayed = internalanyplayed

  private def internalScores =
    (nsScores ::: ewScores).map(s => (s.teamId -> s)).toMap

  def findOpponent(teamid: Team.Id): Option[Team.Id] = {
    board.hands.find { hand =>
      hand.nsTeam == teamid || hand.ewTeam == teamid
    } match {
      case Some(hand) =>
        if (hand.nsTeam == teamid) Some(hand.ewTeam)
        else Some(hand.nsTeam)
      case None => None
    }
  }

  def hasTeamPlayed(teamid: Team.Id): Boolean =
    internalScores.get(teamid) match {
      case Some(score) => score.played
      case _           => false
    }

  def hasTeamPlayed(teamid: Option[Team.Id]): Boolean =
    teamid match {
      case Some(tid) => hasTeamPlayed(tid)
      case None      => false
    }

  def isHidden(ignoreTableSize: Boolean = true): Boolean =
    !allplayed && (perspective match {
      case PerspectiveComplete => true
      case PerspectiveDirector => false
      case PerspectiveTable(team1, team2) =>
        val rc = (ignoreTableSize && board.hands.size == 2) || !hasTeamPlayed(
          team1
        ) || !hasTeamPlayed(team2)
//      BoardScore.log.finest( s"BoardScore.isHidden rc=${rc} PerspectiveTable(${team1},${team2}) board.hand.size=${board.hands.size} ignore=${ignoreTableSize}" )
        rc
    })

  def scores(ignoreTableSize: Boolean = true): Map[Team.Id, TeamBoardScore] =
    if (!isHidden(ignoreTableSize)) {
      internalScores
    } else {
      internalScores.map({
        case (team, score) =>
          if (score.played)
            (
              team,
              score.copy(hidden = true, score = 0, points = 0, contract = None)
            )
          else (team, score)
      })
    }

  def id = board.id

  override def toString(): String = {
    scores(false).values
      .map(tbs => tbs.toString())
      .mkString(
        s"[BoardScore board=${board.id} allplayed=${allplayed} anyplayed=${anyplayed}\n  ",
        "\n  ",
        "]"
      )
  }

  private def calculatePoints(
      scores: List[TeamBoardScore]
  ): List[TeamBoardScore] = {
    scores.map(s => {
      if (s.played) {
        var p = 0.0
        var imps = 0.0
        var nteams = 0
        scores
          .filter { t =>
            s != t && t.played
          }
          .foreach { t =>
            p += (if (t.score == s.score) 1;
                  else if (t.score < s.score) 2;
                  else 0)
            val diff = s.score - t.score
            val i =
              if (diff < 0) -BoardScore.getIMPs(-diff)
              else BoardScore.getIMPs(diff)
            imps += i
            nteams += 1
          }
        val ii = if (nteams == 0) 0.0 else imps / nteams
        s.copy(points = p, imps = ii)
      } else {
        s
      }
    })
  }

  def showVul: String =
    if (board.nsVul) {
      if (board.ewVul) {
        "Both Vul"
      } else {
        "NS Vul"
      }
    } else {
      if (board.ewVul) {
        "EW Vul"
      } else {
        "Neither Vul"
      }
    }
}

object BoardScore {
  def apply(board: Board, perspective: DuplicateViewPerspective) =
    new BoardScore(board, perspective)

  val log: Logger = Logger("bridge.BoardScore")

//  https://www.bridgehands.com/I/IMP.htm
//
//    Difference
//  in points    IMPs
//  20-40        1
//  50-80        2
//  90-120       3
//  130-160      4
//  170-210      5
//  220-260      6
//  270-310      7
//  320-360      8
//  370-420      9
//  430-490     10
//  500-590     11
//  600-740     12
//  750-890     13
//  900-1090    14
//  1100-1290   15
//  1300-1490   16
//  1500-1740   17
//  1750-1990   18
//  2000-2240   19
//  2250-2490   20
//  2500-2990   21
//  3000-3490   22
//  3500-3990   23
//  4000+       24

  case class IMPEntry(min: Int, max: Int, IMP: Int)

  val IMPTable: List[IMPEntry] = List(
    IMPEntry(0, 10, 0),
    IMPEntry(20, 40, 1),
    IMPEntry(50, 80, 2),
    IMPEntry(90, 120, 3),
    IMPEntry(130, 160, 4),
    IMPEntry(170, 210, 5),
    IMPEntry(220, 260, 6),
    IMPEntry(270, 310, 7),
    IMPEntry(320, 360, 8),
    IMPEntry(370, 420, 9),
    IMPEntry(430, 490, 10),
    IMPEntry(500, 590, 11),
    IMPEntry(600, 740, 12),
    IMPEntry(750, 890, 13),
    IMPEntry(900, 1090, 14),
    IMPEntry(1100, 1290, 15),
    IMPEntry(1300, 1490, 16),
    IMPEntry(1500, 1740, 17),
    IMPEntry(1750, 1990, 18),
    IMPEntry(2000, 2240, 19),
    IMPEntry(2250, 2490, 20),
    IMPEntry(2500, 2990, 21),
    IMPEntry(3000, 3490, 22),
    IMPEntry(3500, 3990, 23),
    IMPEntry(4000, 100000, 24)
  )

  def getIMPs(points: Int): Int = {
    IMPTable
      .find { entry =>
        entry.min <= points && entry.max >= points
      }
      .map { entry =>
        entry.IMP
      }
      .getOrElse(0)

  }

}
