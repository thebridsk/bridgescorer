package com.github.thebridsk.bridge.server.test

import org.scalatest.Finders
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.server.test.backend.BridgeServiceTesting
import com.github.thebridsk.bridge.server.service.MyService
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.headers.`Remote-Address`
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Flow
import org.scalatest._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.unmarshalling.FromResponseUnmarshaller
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.RemoteAddress.IP
import java.net.InetAddress
import com.github.thebridsk.utilities.logging.Config
import com.github.thebridsk.utilities.classpath.ClassPath
import java.util.logging.LogManager
import java.util.logging.Logger
import com.github.thebridsk.utilities.logging.FileHandler
import com.github.thebridsk.utilities.logging.FileFormatter
import java.util.logging.Level
import com.github.thebridsk.utilities.logging.RedirectOutput
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.DuplicateHandV2
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerComparisonStats
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerComparisonStat
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement

object TestPlayerComparisonStats {

  val testlog = com.github.thebridsk.utilities.logging.Logger[TestPlayerComparisonStats]()
}

/**
 * Test class to start the logging system
 */
class TestPlayerComparisonStats extends AnyFlatSpec with ScalatestRouteTest with Matchers {
  import TestPlayerComparisonStats._

  val team1 = Team.id(1)
  val team2 = Team.id(2)
  val team3 = Team.id(3)
  val team4 = Team.id(4)

  behavior of "the player comparison stats test"

  val defaultMD = MatchDuplicate(
                    id = MatchDuplicate.id(1),
                    teams = List(
                           Team.create(team1, "Alice", "Alan"),
                           Team.create(team2, "Betty", "Bob"),
                           Team.create(team3, "Cathy", "Carl"),
                           Team.create(team4, "Diana", "Dave")
                        ),
                    boards = List(
                               Board(
                                  id = Board.id(1),
                                  nsVul = false,
                                  ewVul = false,
                                  dealer = "N",
                                  hands = List(
                                        DuplicateHandV2.create(
                                             hand = Hand.create(
                                                            id = team1.id,
                                                            contractTricks = 3,
                                                            contractSuit = "S",
                                                            contractDoubled = "N",
                                                            declarer = "N",
                                                            nsVul = false,
                                                            ewVul = false,
                                                            madeContract = true,
                                                            tricks = 4
                                                     ),
                                             table = Table.id(1),
                                             round = 1,
                                             board = Board.id(1),
                                             nsTeam = team1,
                                             ewTeam = team2
                                        ),
                                        DuplicateHandV2.create(
                                             hand = Hand.create(
                                                            id = team3.id,
                                                            contractTricks = 4,
                                                            contractSuit = "S",
                                                            contractDoubled = "N",
                                                            declarer = "N",
                                                            nsVul = false,
                                                            ewVul = false,
                                                            madeContract = true,
                                                            tricks = 4
                                                     ),
                                             table = Table.id(2),
                                             round = 2,
                                             board = Board.id(1),
                                             nsTeam = team3,
                                             ewTeam = team4
                                        )
                                      ),
                                  created = 0,
                                  updated = 0
                               )
                             ),
                    boardset = BoardSet.idNul,
                    movement = Movement.idNul,
                    created = 0,
                    updated = 0
                  )

  def testStat( stats: PlayerComparisonStats, player: String, stattype: PlayerComparisonStat.StatType, good: Int, bad: Int, neutral: Int ) = {
    stats.data.find( s => s.player == player ).map { s =>
      s.stattype mustBe stattype
      s.aggressivegood mustBe good
      s.passivebad mustBe bad
      s.passiveneutral mustBe neutral
    }.getOrElse( fail( s"Did not find ${player}" ) )
  }

  TestStartLogging.startLogging()

  it should "Stats for game" in {
    val stats = PlayerComparisonStats.stats( Map( defaultMD.id -> defaultMD ) )

    import PlayerComparisonStat._

    testStat(stats, "Alice", SameSide, 0, 1, 0 )
    testStat(stats, "Alan", SameSide, 0, 1, 0 )
    testStat(stats, "Cathy", SameSide, 1, 0, 0 )
    testStat(stats, "Carl", SameSide, 1, 0, 0 )
//    testStat(stats, "Betty", SameSide, 0, 0, 0 )
//    testStat(stats, "Bob", SameSide, 0, 0, 0 )
//    testStat(stats, "Diana", SameSide, 0, 0, 0 )
//    testStat(stats, "Dave", SameSide, 0, 0, 0 )

  }
}
