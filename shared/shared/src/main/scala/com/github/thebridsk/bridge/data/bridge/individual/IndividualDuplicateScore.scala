package com.github.thebridsk.bridge.data.bridge.individual

import com.github.thebridsk.bridge.data.IndividualDuplicate
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.data.IndividualBoard

sealed trait IndividualDuplicateViewPerspective

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

class IndividualDuplicateScore(
    val duplicate: IndividualDuplicate,
    val perspective: IndividualDuplicateViewPerspective
) {

  val boardScores: List[IndividualBoardScore] =
    duplicate.boards
      .sortWith((l,r) => l.id < r.id)
      .map(IndividualBoardScore(duplicate,_,perspective))

  def getBoardScore(board: Int): Option[IndividualBoardScore] = {
    val bid = IndividualBoard.id(board)
    boardScores.find(_.board.id == bid)
  }

  case class Score(
    player: Int,
    mp: Int = 0,
    imp: Float = 0
  ) {
    def add(mps: Int, imps: Float): Score =
      copy(mp = mp + mps, imp = imp + imps)
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
