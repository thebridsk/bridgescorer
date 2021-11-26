package com.github.thebridsk.bridge.data.bridge.individual

import com.github.thebridsk.bridge.data.IndividualDuplicate
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.data.IndividualBoard

sealed trait IndividualDuplicateViewPerspective {
  def score( md: IndividualDuplicate): IndividualDuplicateScore =
    IndividualDuplicateScore(md,this)
}

object IndividualDuplicateViewPerspective {
  case object PerspectiveComplete extends IndividualDuplicateViewPerspective
  case object PerspectiveDirector extends IndividualDuplicateViewPerspective
  case class PerspectiveTable(table: Table.Id, round: Int)
      extends IndividualDuplicateViewPerspective {
    /**
      * @param duplicate
      * @return player indexes that play on table in round.
      */
    def players(duplicate: IndividualDuplicate) = {
      duplicate.playersOnTableInRound(table, round)
        .map(dh => List(dh.north, dh.south, dh.east, dh.west))
        .getOrElse(List())
    }
  }
}

object IndividualDuplicateScore {
  def apply(
    duplicate: IndividualDuplicate,
    perspective: IndividualDuplicateViewPerspective
  ): IndividualDuplicateScore = {
    new IndividualDuplicateScore(duplicate, perspective)
  }

  case class Score(
    player: Int,
    mp: Int = 0,
    imp: Float = 0
  ) {
    def add(mps: Int, imps: Float): Score =
      copy(mp = mp + mps, imp = imp + imps)
  }

  case class Place(place: Int, score: Double, players: List[String])

  implicit class WrapDuplicate(val dup: IndividualDuplicate) extends AnyVal {
    def getPlayerName(p: Int) = {
      val s = dup.getPlayer(p)
      if (s == "") s"$p"
      else s
    }
  }


}

class IndividualDuplicateScore(
    val duplicate: IndividualDuplicate,
    val perspective: IndividualDuplicateViewPerspective
) {
  import IndividualDuplicateScore._

  /**
    * Sorted board score objects
    */
  val boardScores: List[IndividualBoardScore] =
    duplicate.boards
      .sortWith((l,r) => l.id < r.id)
      .map(IndividualBoardScore(duplicate,_,perspective))

  def getBoardScore(board: Int): Option[IndividualBoardScore] = {
    val bid = IndividualBoard.id(board)
    boardScores.find(_.board.id == bid)
  }

  val scores: Map[Int, Score] = {
    boardScores.foldLeft(Map[Int, Score]()) { (result, board) =>
      board.playerToResult.values
        .foldLeft(result) { (r, res) =>
          val cur = r.get(res.player).getOrElse(Score(res.player))
          val next = if (res.played && !res.hide) {
            if (res.isNS) cur.add(res.nsMP, res.nsIMP)
            else cur.add(res.ewMP, res.ewIMP)
          } else {
            cur
          }
          r + (res.player -> next)
        }
    }
  }

  private def fixPlaces(list: List[Place]): List[Place] = {
    val sorted = list.sortWith((l,r) => l.score>r.score)
    var place = 1
    sorted.map { p =>
      val pnew = p.copy(place=place)
      place += p.players.size
      pnew
    }
  }

  lazy val placesMP: List[Place] = {
    val m = scores.groupBy(e => e._2.mp).map { e =>
      val (points, players) = e
      Place(0, points, players.keys.map(tid => duplicate.getPlayer(tid)).toList)
    }
    fixPlaces(m.toList)
  }

  lazy val placesImps: List[Place] = {
    val m = scores.groupBy(e => e._2.imp).map { e =>
      val (points, players) = e
      Place(0, points, players.keys.map(tid => duplicate.getPlayer(tid)).toList)
    }
    fixPlaces(m.toList)
  }

  def placeMPByWinnerSet(winnerset: List[Int]): List[Place] = {
    val ws = winnerset.map(duplicate.getPlayer(_))
    fixPlaces(
      placesMP.flatMap { p =>
        val pteam = p.players.filter(t => ws.contains(t))
        if (pteam.isEmpty) Nil
        else p.copy(players = pteam) :: Nil
      }
    )
  }

  def placeImpByWinnerSet(winnerset: List[Int]): List[Place] = {
    val ws = winnerset.map(duplicate.getPlayer(_))
    fixPlaces(
      placesImps.flatMap { p =>
        val pteam = p.players.filter(t => ws.contains(t))
        if (pteam.isEmpty) Nil
        else p.copy(players = pteam) :: Nil
      }
    )
  }

  def isAtRoundEnd: Boolean = {
    duplicate.boards.flatMap(_.hands)
      .groupBy(_.round)
      .map { e2 =>
        val (round, lhinr) = e2
        val (allPlayed,atLeastOnePlayed,allUnplayed) = lhinr.foldLeft((true,false,true)) { (ac,v) =>
          val played = v.played.isDefined
          (
            ac._1 && played,
            ac._2 || played,
            ac._3 && !played
          )
        }
        (round, allPlayed, allPlayed||allUnplayed)
      }
      .find(!_._3)
      .isEmpty
  }

  def isAllDone: Boolean = {
    duplicate.boards.find { b =>
      b.hands.find { h =>
        h.played.isEmpty
      }.isDefined
    }.isEmpty
  }

  def getBoardsInRound(round: Int, table: Table.Id): List[IndividualBoardScore] = {
    boardScores.filter { board =>
      board.board.hands.find { h =>
        h.table == table && h.round == round
      }.isDefined
    }
  }

  def getDetails: List[IndividualDuplicateSummaryDetails] = {
    duplicate.boards
      .flatMap { b =>
        b.hands.flatMap { dh =>
          dh.played.toList.flatMap { h =>
            val (declarer1, declarer2, defender1, defender2) = h.declarer match {
              case "N" | "S" =>
                (
                  duplicate.getPlayer(dh.north),
                  duplicate.getPlayer(dh.south),
                  duplicate.getPlayer(dh.east),
                  duplicate.getPlayer(dh.west)
                )
              case "E" | "W" =>
                (
                  duplicate.getPlayer(dh.east),
                  duplicate.getPlayer(dh.west),
                  duplicate.getPlayer(dh.north),
                  duplicate.getPlayer(dh.south)
                )
            }
            if (h.contractTricks == 0) {
              IndividualDuplicateSummaryDetails.passed(declarer1) ::
              IndividualDuplicateSummaryDetails.passed(declarer2) ::
              IndividualDuplicateSummaryDetails.passed(defender1) ::
              IndividualDuplicateSummaryDetails.passed(defender2) ::
              Nil
            } else {
              if (h.madeContract) {
                IndividualDuplicateSummaryDetails.made(declarer1) ::
                IndividualDuplicateSummaryDetails.made(declarer2) ::
                IndividualDuplicateSummaryDetails.allowedMade(defender1) ::
                IndividualDuplicateSummaryDetails.allowedMade(defender2) ::
                Nil
              } else {
                IndividualDuplicateSummaryDetails.down(declarer1) ::
                IndividualDuplicateSummaryDetails.down(declarer2) ::
                IndividualDuplicateSummaryDetails.tookDown(defender1) ::
                IndividualDuplicateSummaryDetails.tookDown(defender2) ::
                Nil
              }
            }
          }
        }
      }
      .foldLeft(Map[String, IndividualDuplicateSummaryDetails]()) { (ac, v) =>
        val c = ac.get(v.player).getOrElse(IndividualDuplicateSummaryDetails.zero(v.player))
        ac + (v.player -> (c.add(v)))
      }
      .toList
      .sortBy { e =>
        e._1
      }
      .map { e =>
        e._2
      }
  }

  /**
    * Returns all the winner sets.
    *
    * A set of players never play against each other on the same table.
    * Examples are when using Mitchell movements where all the NS play against all the EW.
    */
  def getWinnerSets(): List[List[Int]] = {
    List(
      (1 to duplicate.players.length).toList
    )
  }
}
