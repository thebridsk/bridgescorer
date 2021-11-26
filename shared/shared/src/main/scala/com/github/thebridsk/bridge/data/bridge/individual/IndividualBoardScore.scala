package com.github.thebridsk.bridge.data.bridge.individual

import com.github.thebridsk.bridge.data.IndividualBoard
import com.github.thebridsk.bridge.data.IndividualDuplicate
import com.github.thebridsk.bridge.data.IndividualDuplicateHand
import com.github.thebridsk.bridge.data.bridge.DuplicateBridge.ScoreHand
import com.github.thebridsk.bridge.data.util.Strings

object IndividualBoardScore {

  def apply(
      duplicate: IndividualDuplicate,
      board: IndividualBoard,
      perspective: IndividualDuplicateViewPerspective
  ): IndividualBoardScore = {
    new IndividualBoardScore(duplicate, board, perspective)
  }

  /**
    *
    *
    * @param hide the result should be hidden
    * @param hand the duplicate hand
    * @param scorehand the score of the hand if it was played
    * @param player the playerNumber if this Result was obtained from IndividualBoardScore.playerToResult.  0 otherwise.
    * @param isNS if this Result was obtained from IndividualBoardScore.playerToResult indicates which side the player was on.  undefined otherwise.
    * @param nsScore
    * @param ewScore
    * @param nsMP
    * @param ewMP
    * @param nsIMP
    * @param ewIMP
    */
  case class Result(
      hide: Boolean,
      hand: IndividualDuplicateHand,
      scorehand: Option[ScoreHand],
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

    def showContract: String = scorehand.map { sh =>
      if (hide) Strings.checkmark
      else sh.contractAsString(vul = "Vul", notvul = "")
    }.getOrElse("")

    def showDeclarer: String = scorehand.map { sh =>
      if (hide) Strings.checkmark
      else sh.declarer.pos
    }.getOrElse("")

    def showMade: String = scorehand.map { sh =>
      if (hide) Strings.checkmark
      else if (sh.madeOrDown.made) sh.tricks.toString()
      else ""
    }.getOrElse("")

    def showDown: String = scorehand.map { sh =>
      if (hide) Strings.checkmark
      else if (sh.madeOrDown.made) ""
      else sh.tricks.toString()
    }.getOrElse("")

    def showNSScore: String = scorehand.map { sh =>
      if (hide) Strings.checkmark
      else nsScore.toString()
    }.getOrElse("")

    def showEWScore: String = scorehand.map { sh =>
      if (hide) Strings.checkmark
      else ewScore.toString()
    }.getOrElse("")

    def showNSMP: String = scorehand.map { sh =>
      if (hide) Strings.checkmark
      else nsMP.toString()
    }.getOrElse("")

    def showEWMP: String = scorehand.map { sh =>
      if (hide) Strings.checkmark
      else ewMP.toString()
    }.getOrElse("")

    def showNSIMP: String = scorehand.map { sh =>
      if (hide) Strings.checkmark
      else f"${nsIMP}%.1f"
    }.getOrElse("")

    def showEWIMP: String = scorehand.map { sh =>
      if (hide) Strings.checkmark
      else f"${ewIMP}%.1f"
    }.getOrElse("")

  }
}

class IndividualBoardScore(
    val duplicate: IndividualDuplicate,
    val board: IndividualBoard,
    val perspective: IndividualDuplicateViewPerspective
) {
  import IndividualBoardScore._

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

  val totalToPlayed = board.hands.length

  /**
    * * playerToResult is a map playerNumber => Result.
    * * allplayed is true if all hands on board have been played.
    * * anyplayed is true if at least one hand was played.
    * * all is all the unique Result objects for the board.  There is one per table.
    */
  val (playerToResult, allplayed, anyplayed, all): (Map[Int, Result], Boolean, Boolean, List[Result]) = {
    val results = board.hands.map { h =>
      h.played.map { hand =>
        val sh = ScoreHand(hand)
        Result(
          false,
          h,
          Some(sh),
          0,
          true,
          sh.score.ns,
          sh.score.ew,
        )
      }.getOrElse( Result(false,h,None,0,true,0,0,0,0))
    }.groupBy(_.played)

    val played = results.get(true).getOrElse(List())
    val notplayed = results.get(false).getOrElse(List())

    val allhands = (addPoints(played):::notplayed).sortWith((l,r) => l.hand.north < r.hand.north)

    import IndividualDuplicateViewPerspective._
    val r = perspective match {
      case PerspectiveDirector =>
        allhands
      case PerspectiveComplete =>
        if (played.length == totalToPlayed) allhands
        else allhands.map(_.withHide(true))
      case per: PerspectiveTable =>
        val players = per.players(duplicate)
        if (board.wasPlayedByPlayers(players)) allhands
        else allhands.map(_.withHide(true))
    }

    (
      r.flatMap { r =>
        List(
          r.hand.north -> r.withPlayer(r.hand.north, true),
          r.hand.south -> r.withPlayer(r.hand.south, true),
          r.hand.east -> r.withPlayer(r.hand.east, false),
          r.hand.west -> r.withPlayer(r.hand.west, false)
        )
      }.toMap,
      notplayed.isEmpty,
      !played.isEmpty,
      r
    )

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
