package com.github.thebridsk.bridge.server.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.server.backend.BridgeService
import com.github.thebridsk.utilities.logging.Logger
import java.util.logging.Level
import com.github.thebridsk.bridge.server.backend.BridgeServiceInMemory
import com.github.thebridsk.bridge.server.test.backend.BridgeServiceTesting
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore
import com.github.thebridsk.bridge.data.bridge.PerspectiveDirector
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.Id
import scala.concurrent.Await
import scala.concurrent.duration._

class TestWinnerSets extends AnyFlatSpec with Matchers {

  val testlog = Logger[TestWinnerSets]()

  val restService = new BridgeServiceTesting

  val movements = restService.movements.syncStore
  val boardsets = restService.boardSets.syncStore


  behavior of "TestWinnerSets"

  def getDup( movementid: String ) = {
    val move = movements.read(movementid) match {
      case Right(m) => m
      case Left((statuscode,msg)) =>
        testlog.info(s"Unable to find $movementid: (${statuscode}) ${msg.msg}")
        fail(s"Unable to find $movementid: (${statuscode}) ${msg.msg}")
    }

    Await.result(restService.fillBoards(MatchDuplicate.create(), "StandardBoards", movementid),30.seconds) match {
      case Right(m) => m
      case Left((statuscode,msg)) =>
        testlog.info(s"Unable to create MatchDuplicate with movement $movementid: (${statuscode}) ${msg.msg}")
        fail(s"Unable to create MatchDuplicate with movement $movementid: (${statuscode}) ${msg.msg}")
    }
  }

  it should "have two winner set in Mitchell3Table" in {
    val dup = getDup("Mitchell3Table")
    val mds = MatchDuplicateScore(dup, PerspectiveDirector)
    val ws = mds.getWinnerSets

    ws.size mustBe 2
    ws must contain ( List("T1","T2","T3") )
    ws must contain ( List("T4","T5","T6") )

    val pl1 = mds.placeByWinnerSet(ws.head)
    pl1.size mustBe 1
    pl1.head.teams.size mustBe 3

    val pl2 = mds.placeByWinnerSet(ws.tail.head)
    pl2.size mustBe 1
    pl2.head.teams.size mustBe 3

  }

  def swapNSEWInHand( hand: DuplicateHand ): DuplicateHand = {
//    println( s"swapping NS and EW in hand ${hand}")
    hand.copy( nsTeam=hand.ewTeam, ewTeam=hand.nsTeam)
  }

  def swapNSEWInBoardOnTable( board: Board, round: Int, table: Id.Table ): Board = {
//    println( s"swapping NS and EW in board ${board}")
    val nhs = board.hands.map( h => if (h.table==table && h.round==round) swapNSEWInHand(h) else h)
    board.copy(hands=nhs)
  }

  def swapNSEWInHandsInRoundOnTable( dup: MatchDuplicate, round: Int, table: Id.Table ): MatchDuplicate = {
//    println( s"swapping NS and EW in MatchDuplicate ${dup}")
    val nbs = dup.boards.map( b => swapNSEWInBoardOnTable(b, round, table))
    dup.copy(boards=nbs)
  }

  it should "have one winner set in Mitchell3Table with NS swapped with EW in one round on one table" in {
    val dup = getDup("Mitchell3Table")
    val ndup = swapNSEWInHandsInRoundOnTable(dup, 1, "1")

    withClue( "dup and ndup must be different" ) {
      dup.equalsIgnoreModifyTime(ndup) mustBe false
    }

    val mds = MatchDuplicateScore(ndup, PerspectiveDirector)
    val ws = mds.getWinnerSets

//    println( s"dup=${dup}" )
//    println( s"ndup=${ndup}" )

    withClue(s"winner sets are ${ws}") {
      ws.size mustBe 1
      ws.head mustBe List("T1","T2","T3", "T4","T5","T6")
    }
  }

  it should "have one winner set in 2TablesArmonk" in {
    val dup = getDup("2TablesArmonk")
    val ws = MatchDuplicateScore(dup, PerspectiveDirector).getWinnerSets

    ws.size mustBe 1
    ws.head mustBe List("T1","T2","T3","T4")
  }

  it should "have one winner set in Howell3TableNoRelay" in {
    val dup = getDup("Howell3TableNoRelay")
    val ws = MatchDuplicateScore(dup, PerspectiveDirector).getWinnerSets

    ws.size mustBe 1
    ws.head mustBe List("T1","T2","T3","T4","T5","T6")
  }

}
