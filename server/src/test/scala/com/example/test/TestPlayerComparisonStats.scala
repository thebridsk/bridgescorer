package com.example.test

import org.scalatest.Finders
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import com.example.test.backend.BridgeServiceTesting
import com.example.service.MyService
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
import _root_.utils.logging.Config
import _root_.utils.classpath.ClassPath
import java.util.logging.LogManager
import java.util.logging.Logger
import _root_.utils.logging.FileHandler
import _root_.utils.logging.FileFormatter
import java.util.logging.Level
import _root_.utils.logging.RedirectOutput
import com.example.data.MatchDuplicate
import com.example.data.Team
import com.example.data.BoardV2
import com.example.data.DuplicateHandV2
import com.example.data.Hand
import com.example.data.duplicate.stats.PlayerComparisonStats

object TestPlayerComparisonStats {

  val testlog = utils.logging.Logger[TestPlayerComparisonStats]
}

/**
 * Test class to start the logging system
 */
class TestPlayerComparisonStats extends FlatSpec with ScalatestRouteTest with MustMatchers {
  import TestPlayerComparisonStats._

  behavior of "the player comparison stats test"

  val defaultMD = MatchDuplicate(
                    id = "M1",
                    teams = List(
                           Team.create("T1", "Alice", "Alan"),
                           Team.create("T2", "Betty", "Bob"),
                           Team.create("T3", "Cathy", "Carl"),
                           Team.create("T4", "Diana", "Dave")
                        ),
                    boards = List(
                               BoardV2(
                                  id = "B1",
                                  nsVul = false,
                                  ewVul = false,
                                  dealer = "N",
                                  hands = List(
                                        DuplicateHandV2.create(
                                             hand = Hand.create(
                                                            id = "T1",
                                                            contractTricks = 3,
                                                            contractSuit = "S",
                                                            contractDoubled = "N",
                                                            declarer = "N",
                                                            nsVul = false,
                                                            ewVul = false,
                                                            madeContract = true,
                                                            tricks = 4
                                                     ),
                                             table = "1",
                                             round = 1,
                                             board = "B1",
                                             nsTeam = "T1",
                                             ewTeam = "T2"
                                        ),
                                        DuplicateHandV2.create(
                                             hand = Hand.create(
                                                            id = "T3",
                                                            contractTricks = 4,
                                                            contractSuit = "S",
                                                            contractDoubled = "N",
                                                            declarer = "N",
                                                            nsVul = false,
                                                            ewVul = false,
                                                            madeContract = true,
                                                            tricks = 4
                                                     ),
                                             table = "2",
                                             round = 2,
                                             board = "B1",
                                             nsTeam = "T3",
                                             ewTeam = "T4"
                                        )
                                      ),
                                  created = 0,
                                  updated = 0
                               )
                             ),
                    boardset = "",
                    movement = "",
                    created = 0,
                    updated = 0
                  )

  def testStat( stats: PlayerComparisonStats, player: String, sameside: Boolean, good: Int, bad: Int, neutral: Int ) = {
    stats.data.find( s => s.player == player ).map { s =>
      s.sameside mustBe sameside
      s.aggressivegood mustBe good
      s.passivebad mustBe bad
      s.passiveneutral mustBe neutral
    }.getOrElse( fail( s"Did not find ${player}" ) )
  }

  TestStartLogging.startLogging()

  it should "Stats for game" in {
    val stats = PlayerComparisonStats.stats( Map( "M1" -> defaultMD ) )

    testStat(stats, "Alice", true, 0, 1, 0 )
    testStat(stats, "Alan", true, 0, 1, 0 )
    testStat(stats, "Cathy", true, 1, 0, 0 )
    testStat(stats, "Carl", true, 1, 0, 0 )
//    testStat(stats, "Betty", true, 0, 0, 0 )
//    testStat(stats, "Bob", true, 0, 0, 0 )
//    testStat(stats, "Diana", true, 0, 0, 0 )
//    testStat(stats, "Dave", true, 0, 0, 0 )

  }
}
