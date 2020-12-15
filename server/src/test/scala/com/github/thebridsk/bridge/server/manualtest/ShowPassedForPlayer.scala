package com.github.thebridsk.bridge.server.manualtest

import com.github.thebridsk.utilities.main.Main
import scala.reflect.io.Path
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStore
import com.github.thebridsk.bridge.server.backend.resource.SyncStore
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.data.bridge.South
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerComparisonStat
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerComparisonStats
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.MatchDuplicate
import org.rogach.scallop.ScallopOption
import com.github.thebridsk.utilities.main.MainConf

class ShowPassedForPlayerConf extends MainConf {

  import com.github.thebridsk.utilities.main.Converters._

  val optionStore: ScallopOption[Path] = opt[Path](
    "store",
    short = 's',
    descr = "The store directory, default=./store",
    argName = "dir",
    default = Some("./store")
  )

  val optionPlayer: ScallopOption[String] = trailArg[String](
    name = "player",
    descr = "the player involved in passed contracts",
    default = None,
    required = true
  )

}

object ShowPassedForPlayer extends Main[ShowPassedForPlayerConf] {

  case class PassedBoard(
      id: MatchDuplicate.Id,
      team: Team.Id,
      board: Board,
      played: Boolean,
      good: Boolean
  )

  import config._

  def execute(): Int = {

    val storedir = optionStore().toDirectory
    logger.info(s"Using datastore ${storedir}")
    val datastore = new SyncStore(
      new BridgeServiceFileStore(storedir).duplicates
    )

    val p = optionPlayer()

    val (pas, passedBoardsWithPlayer) = datastore.readAll() match {
      case Left(err) => (Nil, Nil)
      case Right(map) =>
        val pbwp = map.values.flatMap { md =>
          val oteam = md.teams.find(t => p == t.player1 || p == t.player2)
          oteam
            .map { team =>
              md.boards.flatMap { board =>
                val passedHand = board.hands.find(h =>
                  !h.played.isEmpty && h.played.head.contractTricks == 0
                )
                if (passedHand.isDefined) {
                  val playedHand =
                    board.hands.find(h => h.played.head.contractTricks != 0)
                  val playerPlayed = board.hands.find(h =>
                    h.nsTeam == team.id || h.ewTeam == team.id
                  )
                  val played = playerPlayed.head == playedHand.head
                  val playedByNS =
                    playedHand.head.hand.head.declarer == "N" || playedHand.head.hand.head.declarer == "S"
                  val playerWasNS = playerPlayed.head.nsTeam == team.id
                  val good = if (played) {
                    if (playedByNS == playerWasNS) {
                      playedHand.head.played.head.madeContract
                    } else {
                      !playedHand.head.played.head.madeContract
                    }
                  } else {
                    if (playedByNS != playerWasNS) {
                      playedHand.head.played.head.madeContract
                    } else {
                      !playedHand.head.played.head.madeContract
                    }
                  }
                  PassedBoard(md.id, team.id, board, played, good) :: Nil
                } else {
                  Nil
                }
              }
            }
            .getOrElse(Nil)
        }.toList
        val stats = PlayerComparisonStats
          .stats(map)
          .data
          .filter(ps =>
            ps.player == p && ps.stattype == PlayerComparisonStat.PassedOut
          )
        (stats, pbwp)
    }

    logger.info(pas.toString)
    logger.info("")

    logger.info(
      s"Player ${p} was involved in ${passedBoardsWithPlayer.length} boards with passed out hands"
    )
    val grouped = passedBoardsWithPlayer.groupBy { pb =>
      (pb.played, pb.good)
    }
    grouped.values.foreach { group =>
      group.foreach { pb =>
        val g = if (pb.good) "good" else "bad"
        val pp = if (pb.played) "played" else "passed"
        logger.info(s"""-- ${g} -- ${pp} ----------------""")
        logger.info(
          s"""Player was on team ${pb.team} in match ${pb.id} on board ${pb.board.id}"""
        )
        pb.board.hands.foreach { dh =>
          val pl = if (pb.team == dh.nsTeam || pb.team == dh.ewTeam) {
            "* "
          } else {
            "  "
          }
          if (dh.hand.head.contractTricks == 0) {
            logger.info(
              s"""  ${pl}Passed hand, teams ${dh.nsTeam} ${dh.ewTeam}"""
            )
          } else {
            val dec = dh.hand.head.declarer
            val (declarer, defender) =
              if (dec == North.pos || dec == South.pos) (dh.nsTeam, dh.ewTeam)
              else (dh.ewTeam, dh.nsTeam)
            val cmd = if (dh.hand.head.madeContract) "made" else "down"
            logger.info(
              s"""  ${pl}${dh.hand.head.contract} ${cmd} by ${declarer}, defended by ${defender}"""
            )
          }
        }
      }
    }
    Thread.sleep(1000L)
    0
  }

  def playerStats(): Unit = {}
}
