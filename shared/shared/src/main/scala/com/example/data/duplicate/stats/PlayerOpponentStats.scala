package com.example.data.duplicate.stats

import com.example.data.Id
import com.example.data.MatchDuplicate
import com.example.data.bridge.MatchDuplicateScore
import com.example.data.bridge.PerspectiveComplete
import com.example.data.bridge.BoardScore
import com.example.data.Team
import com.example.data.bridge.TeamBoardScore

/**
  * The stats against an opponent.
  * The primary player, PP, is from the enclosing PlayerOpponentsStat.
  *
  * All the stats are the primary player against the opponent.
  * The stats are always from the primary player's view.
  *
  * @constructor
  * @param opponent the opponent for these stats
  * @param matchesPlayed the number of matches PP and opponent were opponents.
  * @param matchesBeat The number of times PP beat opponent
  * @param matchesTied the number of times PP and opponent tied
  * @param totalMP The total number of MP that PP and opponent played against each other
  * @param wonMP the number of MP that PP won over opponent
  */
case class PlayerOpponentStat(
    player: String,
    opponent: String,
    matchesPlayed: Int,
    matchesBeat: Int,
    matchesTied: Int,
    totalMP: Int,
    wonMP: Int
) {

  def this(
      player: String,
      opponent: String,
      totalMP: Int,
      wonMP: Int
  ) {
    this(player, opponent, 0, 0, 0, totalMP, wonMP)
  }

  /**
    * Add in another opponents stats.  The stats SHOULD have the same primary player.
    */
  def add(other: PlayerOpponentStat) = {
    PlayerOpponentStat(
      player,
      s"$opponent ${other.opponent}",
      matchesPlayed + other.matchesPlayed,
      matchesBeat + other.matchesBeat,
      matchesTied + other.matchesTied,
      totalMP + other.totalMP,
      wonMP + other.wonMP
    )
  }

  /**
    * Add in more stats from the same opponent.
    *
    * assert opponent == other.opponent
    */
  def sum(other: PlayerOpponentStat) = {
    PlayerOpponentStat(
      player,
      opponent,
      matchesPlayed + other.matchesPlayed,
      matchesBeat + other.matchesBeat,
      matchesTied + other.matchesTied,
      totalMP + other.totalMP,
      wonMP + other.wonMP
    )
  }
}

/**
  *
  * @constructor
  * @param player the primary player, PP, name for these stats
  * @param opponents the stats for a given opponent
  */
case class PlayerOpponentsStat(
    player: String,
    opponents: List[PlayerOpponentStat]
) {

  /**
    * Add in more stats from the same opponent.
    *
    * assert player == other.player
    * sum only when opponents[i].opponent == other.opponents[j].opponent
    */
  def sum(other: PlayerOpponentsStat) = {
    val thisOpponents = opponents.map(op => op.opponent)
    val otherOpponents = other.opponents.map(op => op.opponent)
    val allOpponents = (thisOpponents ::: otherOpponents).distinct

    PlayerOpponentsStat(
      player,
      allOpponents.map { opp =>
        val thisOp = opponents.find(op => op.opponent == opp)
        val otherOp = other.opponents.find(op => op.opponent == opp)
        thisOp
          .map(top => otherOp.map(oop => oop.sum(top)).getOrElse(top))
          .getOrElse(otherOp.get)
      }
    )
  }

  def getPlayer(name: String) = opponents.find(pos => pos.opponent == name)

  def playerTotal() = {
    opponents.foldLeft(PlayerOpponentStat(player, "", 0, 0, 0, 0, 0)) {
      (ac, v) =>
        ac.add(v)
    }
  }

  def sort() = {
    copy(opponents = opponents.sortWith((l, r) => l.opponent < r.opponent))
  }

  override def toString() = {
    player + ": " + playerTotal.copy(opponent = "") +
      opponents.mkString("\n  ", "\n  ", "")
  }
}

case class PlayersOpponentsStats(
    players: List[PlayerOpponentsStat]
) {

  def getPlayer(name: String) = players.find(pos => pos.player == name)

  def sort() =
    copy(
      players =
        players.sortWith((l, r) => l.player < r.player).map(s => s.sort())
    )

  def sum(other: PlayersOpponentsStats) = {
    val thisPlayers = players.map(op => op.player)
    val otherPlayers = other.players.map(op => op.player)
    val allPlayers = (thisPlayers ::: otherPlayers).distinct

    PlayersOpponentsStats(
      allPlayers.map { play =>
        val thisPl = players.find(op => op.player == play)
        val otherPl = other.players.find(op => op.player == play)
        thisPl
          .map(top => otherPl.map(oop => oop.sum(top)).getOrElse(top))
          .getOrElse(otherPl.get)
      }
    )
  }

  def addStat(stat: PlayerOpponentsStat) = {
    var added = false;
    val pl = players.map { pos =>
      if (pos.player == stat.player) {
        added = true
        pos.sum(stat)
      } else {
        pos
      }
    }
    val pl1 = if (added) pl else stat :: pl
    copy(
      players = pl1
    )
  }

  def getPlayers() = {
    players.map(s => s.player)
  }

  override def toString() = {
    players.mkString("\n")
  }
}

object PlayersOpponentsStats {

  def getBothStat(
      totalMatchPoints: Int,
      teamscores: Map[Id.Team, TeamBoardScore],
      team1: Team,
      team2: Team
  ) = {
    getStat(totalMatchPoints, teamscores, team1, team2) ::: getStat(
      totalMatchPoints,
      teamscores,
      team2,
      team1
    )
  }

  def getStat(
      totalMatchPoints: Int,
      teamscores: Map[Id.Team, TeamBoardScore],
      team1: Team,
      team2: Team
  ) = {
    PlayerOpponentsStat(
      team1.player1,
      new PlayerOpponentStat(
        team1.player1,
        team2.player1,
        totalMatchPoints,
        teamscores.get(team1.id).get.points.toInt
      ) ::
        new PlayerOpponentStat(
          team1.player1,
          team2.player2,
          totalMatchPoints,
          teamscores.get(team1.id).get.points.toInt
        ) ::
        Nil
    ) ::
      PlayerOpponentsStat(
        team1.player2,
        new PlayerOpponentStat(
          team1.player2,
          team2.player1,
          totalMatchPoints,
          teamscores.get(team1.id).get.points.toInt
        ) ::
          new PlayerOpponentStat(
            team1.player2,
            team2.player2,
            totalMatchPoints,
            teamscores.get(team1.id).get.points.toInt
          ) ::
          Nil
      ) ::
      Nil
  }

  def getBothTeamStat(
      teamscores: Map[Id.Team, Double],
      team1: Team,
      team2: Team
  ) = {
    getTeamStat(teamscores, team1, team2) ::: getTeamStat(
      teamscores,
      team2,
      team1
    )
  }

  def getTeamStat(
      teamscores: Map[Id.Team, Double],
      team1: Team,
      team2: Team
  ) = {

    val t1s = teamscores.get(team1.id).get
    val t2s = teamscores.get(team2.id).get

    val (tied, t1won, t2won) =
      if (t1s == t2s) (1, 0, 0) else if (t1s < t2s) (0, 0, 1) else (0, 1, 0)

    PlayerOpponentsStat(
      team1.player1,
//                                              played,beat,tied
      new PlayerOpponentStat(team1.player1, team2.player1, 1, t1won, tied, 0, 0) ::
        new PlayerOpponentStat(
          team1.player1,
          team2.player2,
          1,
          t1won,
          tied,
          0,
          0
        ) ::
        Nil
    ) ::
      PlayerOpponentsStat(
        team1.player2,
        new PlayerOpponentStat(
          team1.player2,
          team2.player1,
          1,
          t1won,
          tied,
          0,
          0
        ) ::
          new PlayerOpponentStat(
            team1.player2,
            team2.player2,
            1,
            t1won,
            tied,
            0,
            0
          ) ::
          Nil
      ) ::
      Nil
  }

  def stats(
      dups: Map[Id.MatchDuplicate, MatchDuplicate]
  ): PlayersOpponentsStats = {
    val ss = dups.values
      .flatMap { md =>
        val score = MatchDuplicateScore(md, PerspectiveComplete)
        if (score.alldone) {

          val teams = md.teams.map(t => (t.id, t)).toMap

          val list = score.boards.values
            .flatMap { b =>
              val teamscores = b.scores()
              if (b.board.hands.length == 2) {
                // same board opponents
                // different board same position are opponents
                // calculate MP and MPtotal

                val h1ns = b.board.hands.head.nsTeam
                val h1ew = b.board.hands.head.ewTeam
                val h2ns = b.board.hands.tail.head.nsTeam
                val h2ew = b.board.hands.tail.head.ewTeam

                getBothStat(
                  2,
                  teamscores,
                  teams.get(h1ns).get,
                  teams.get(h1ew).get
                ) :::
                  getBothStat(
                    2,
                    teamscores,
                    teams.get(h1ns).get,
                    teams.get(h2ns).get
                  ) :::
                  getBothStat(
                    2,
                    teamscores,
                    teams.get(h2ew).get,
                    teams.get(h2ns).get
                  ) :::
                  getBothStat(
                    2,
                    teamscores,
                    teams.get(h2ew).get,
                    teams.get(h1ew).get
                  )

              } else {
                // not same team are opponents
                // calculate MP and MPtotal
                // TODO: implement for more than 2 tables
                Nil
              }
            }
            .foldLeft(PlayersOpponentsStats(Nil)) { (ac, v) =>
              ac.addStat(v)
            }
          // calculate matchPlayed, matchBeat, matchTied
          val teamscores = score.teamScores

          val teamlist = md.teams
            .flatMap { t1 =>
              md.teams.flatMap { t2 =>
                if (t1 == t2) Nil
                else {
                  getTeamStat(teamscores, t1, t2)
                }
              }
            }
            .foldLeft(list) { (ac, v) =>
              ac.addStat(v)
            }

          teamlist ::
            Nil
        } else {
          Nil
        }

      }
      .foldLeft(PlayersOpponentsStats(Nil)) { (ac, v) =>
        ac.sum(v)
      }

    ss
  }
}
