package com.github.thebridsk.bridge.data.bridge.individual

import com.github.thebridsk.bridge.data.IndividualBoard
import com.github.thebridsk.bridge.data.IndividualDuplicate
import com.github.thebridsk.bridge.data.IndividualDuplicateHand
import com.github.thebridsk.bridge.data.bridge.DuplicateBridge.ScoreHand

object IndividualBoardScore {

  def apply(
      duplicate: IndividualDuplicate,
      board: IndividualBoard,
      perspective: IndividualDuplicateViewPerspective
  ): IndividualBoardScore = {
    new IndividualBoardScore(duplicate, board, perspective)
  }
}

class IndividualBoardScore(
    val duplicate: IndividualDuplicate,
    val board: IndividualBoard,
    val perspective: IndividualDuplicateViewPerspective
) {

  /**
    * the total number of players.
    *
    * The players are numbered starting at 1 to *numberOfPlayers* inclusive.
    */
  def numberOfPlayers: Int = duplicate.players.length

  /**
    * @param i the index, one based, must be 0 < *i* <= [[numberOfPlayers]]
    * @return the players name
    */
  def player(i: Int): String = duplicate.players(i-1)

  case class Result(
      hide: Boolean,
      hand: IndividualDuplicateHand,
      player: Int,
      isNS: Boolean,
      nsScore: Int,
      ewScore: Int,
      nsMP: Int = 0,
      ewMP: Int = 0,
      nsIMP: Float = 0,
      ewIMP: Float = 0
  ) {

    def played: Boolean = hand.played.isDefined

    def withHide(f: Boolean) = copy(hide = f)

    def withPlayer(player: Int, isNS: Boolean) =
      copy(player = player, isNS = isNS)
  }

  val totalToPlayed = board.hands.length

  val playerToResult: Map[Int, Result] = {
    val results = board.hands.map { h =>
      h.played.map { hand =>
        val sh = ScoreHand(hand)
        Result(
          false,
          h,
          0,
          true,
          sh.score.ns,
          sh.score.ew,
        )
      }.getOrElse( Result(false,h,0,true,0,0,0,0))
    }.groupBy(_.played)

    val played = results.get(true).getOrElse(List())
    val notplayed = results.get(false).getOrElse(List())

    val all = addPoints(played):::notplayed

    import IndividualDuplicateViewPerspective._
    val r = perspective match {
      case PerspectiveDirector =>
        all
      case PerspectiveComplete =>
        if (played.length == totalToPlayed) all
        else all.map(_.withHide(true))
      case per: PerspectiveTable =>
        val players = per.players(duplicate)
        if (board.wasPlayedByPlayers(players)) all
        else all.map(_.withHide(true))
    }

    r.flatMap { r =>
      List(
        r.hand.north -> r.withPlayer(r.hand.north, true),
        r.hand.south -> r.withPlayer(r.hand.south, true),
        r.hand.east -> r.withPlayer(r.hand.east, false),
        r.hand.west -> r.withPlayer(r.hand.west, false)
      )
    }.toMap

  }

  private def addPoints(results: List[Result]): List[Result] = {
    results.map(s => {
      var nsMP = 0
      var ewMP = 0
      var nsIMP = 0.0f
      var ewIMP = 0.0f
      var nteams = 0
      results
        .filter(t => s != t)
        .foreach { t =>
          nsMP += (if (t.nsScore == s.nsScore) 1;
                else if (t.nsScore < s.nsScore) 2;
                else 0)
          val nsdiff = s.nsScore - t.nsScore
          nsIMP += (
            if (nsdiff < 0) -IMPScoring.getIMPs(-nsdiff)
            else IMPScoring.getIMPs(nsdiff)
          )

          ewMP += (if (t.ewScore == s.ewScore) 1;
                else if (t.ewScore < s.ewScore) 2;
                else 0)
          val ewdiff = s.ewScore - t.ewScore
          ewIMP += (
            if (ewdiff < 0) -IMPScoring.getIMPs(-ewdiff)
            else IMPScoring.getIMPs(ewdiff)
          )

          nteams += 1
        }
      val nsIMPs: Float = if (nteams == 0) 0.0f else nsIMP / nteams
      val ewIMPs: Float = if (nteams == 0) 0.0f else ewIMP / nteams
      s.copy(
        nsMP = nsMP,
        ewMP = ewMP,
        nsIMP = nsIMPs,
        ewIMP = ewIMPs
      )
    })
  }

  /**
    *
    *
    * @param player the index, one based, must be 0 < *i* <= [[numberOfPlayers]]
    * @return the result for the specified player.  None if they don't play this board.
    */
  def getResult(player: Int): Option[Result] = {
    playerToResult.get(player)
  }
}
