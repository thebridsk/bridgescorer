package com.github.thebridsk.bridge.server.test

import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.sample.TestMatchDuplicate
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.server.util.SystemTimeJVM
import org.scalatest.flatspec.AnyFlatSpec
import com.github.thebridsk.bridge.data.bridge.PerspectiveTable
import com.github.thebridsk.bridge.data.bridge.PerspectiveDirector
import com.github.thebridsk.bridge.server.backend.BridgeServiceInMemory
import scala.concurrent.ExecutionContext
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.Board

class TestDuplicateScore extends AnyFlatSpec with Matchers {

  SystemTimeJVM()

  behavior of "a played match"

  val dupid = MatchDuplicate.id(1)
  val md = TestMatchDuplicate.getPlayedMatch(dupid)

//  board 7 played by 1 and 3
//  board 8 played by 2 and 4

  val team1 = Team.id(1)
  val team2 = Team.id(2)
  val team3 = Team.id(3)
  val team4 = Team.id(4)

  it should "score from T1 and T2 perspective" in {
    val score = MatchDuplicateScore( md, PerspectiveTable(team1, team2) )
    score.teamScores mustBe TestMatchDuplicate.getTeamScore()
    val b7 = score.boards.get(Board.id(7)).get
    b7.hasTeamPlayed(team1) mustBe true
    b7.hasTeamPlayed(team2) mustBe false
    b7.hasTeamPlayed(team3) mustBe true
    b7.hasTeamPlayed(team4) mustBe false
    b7.scores().get(team1).get.hidden mustBe true
    b7.scores().get(team2).get.hidden mustBe false
    b7.scores().get(team3).get.hidden mustBe true
    b7.scores().get(team4).get.hidden mustBe false
    b7.scores(false).get(team1).get.hidden mustBe true
    b7.scores(false).get(team2).get.hidden mustBe false
    b7.scores(false).get(team3).get.hidden mustBe true
    b7.scores(false).get(team4).get.hidden mustBe false
    val b8 = score.boards.get(Board.id(8)).get
    b8.hasTeamPlayed(team1) mustBe false
    b8.hasTeamPlayed(team2) mustBe true
    b8.hasTeamPlayed(team3) mustBe false
    b8.hasTeamPlayed(team4) mustBe true
    b8.scores().get(team1).get.hidden mustBe false
    b8.scores().get(team2).get.hidden mustBe true
    b8.scores().get(team3).get.hidden mustBe false
    b8.scores().get(team4).get.hidden mustBe true
    b8.scores(false).get(team1).get.hidden mustBe false
    b8.scores(false).get(team2).get.hidden mustBe true
    b8.scores(false).get(team3).get.hidden mustBe false
    b8.scores(false).get(team4).get.hidden mustBe true
  }

  it should "score from T3 and T4 perspective" in {
    val score = MatchDuplicateScore( md, PerspectiveTable(team3, team4 ))
    score.teamScores mustBe TestMatchDuplicate.getTeamScore()
    val b7 = score.boards.get(Board.id(7)).get
    b7.hasTeamPlayed(team1) mustBe true
    b7.hasTeamPlayed(team2) mustBe false
    b7.hasTeamPlayed(team3) mustBe true
    b7.hasTeamPlayed(team4) mustBe false
    b7.scores().get(team1).get.hidden mustBe true
    b7.scores().get(team2).get.hidden mustBe false
    b7.scores().get(team3).get.hidden mustBe true
    b7.scores().get(team4).get.hidden mustBe false
    b7.scores(false).get(team1).get.hidden mustBe true
    b7.scores(false).get(team2).get.hidden mustBe false
    b7.scores(false).get(team3).get.hidden mustBe true
    b7.scores(false).get(team4).get.hidden mustBe false
    val b8 = score.boards.get(Board.id(8)).get
    b8.hasTeamPlayed(team1) mustBe false
    b8.hasTeamPlayed(team2) mustBe true
    b8.hasTeamPlayed(team3) mustBe false
    b8.hasTeamPlayed(team4) mustBe true
    b8.scores().get(team1).get.hidden mustBe false
    b8.scores().get(team2).get.hidden mustBe true
    b8.scores().get(team3).get.hidden mustBe false
    b8.scores().get(team4).get.hidden mustBe true
    b8.scores(false).get(team1).get.hidden mustBe false
    b8.scores(false).get(team2).get.hidden mustBe true
    b8.scores(false).get(team3).get.hidden mustBe false
    b8.scores(false).get(team4).get.hidden mustBe true
  }

//  board 7 played by 1 and 3
//  board 8 played by 2 and 4

  it should "score from T1 and T3 perspective" in {
    val score = MatchDuplicateScore( md, PerspectiveTable(team1, team3 ))
    score.teamScores mustBe TestMatchDuplicate.getTeamScore()
    val b7 = score.boards.get(Board.id(7)).get
    b7.hasTeamPlayed(team1) mustBe true
    b7.hasTeamPlayed(team2) mustBe false
    b7.hasTeamPlayed(team3) mustBe true
    b7.hasTeamPlayed(team4) mustBe false
    b7.scores().get(team1).get.hidden mustBe true
    b7.scores().get(team2).get.hidden mustBe false
    b7.scores().get(team3).get.hidden mustBe true
    b7.scores().get(team4).get.hidden mustBe false
    b7.scores().get(team1).get.score mustBe 0
    b7.scores().get(team2).get.score mustBe 0
    b7.scores().get(team3).get.score mustBe 0
    b7.scores().get(team4).get.score mustBe 0
    b7.scores(false).get(team1).get.hidden mustBe false
    b7.scores(false).get(team2).get.hidden mustBe false
    b7.scores(false).get(team3).get.hidden mustBe false
    b7.scores(false).get(team4).get.hidden mustBe false
    b7.scores(false).get(team1).get.score mustBe -650
    b7.scores(false).get(team2).get.score mustBe 0
    b7.scores(false).get(team3).get.score mustBe 650
    b7.scores(false).get(team4).get.score mustBe 0
    val b8 = score.boards.get(Board.id(8)).get
    b8.hasTeamPlayed(team1) mustBe false
    b8.hasTeamPlayed(team2) mustBe true
    b8.hasTeamPlayed(team3) mustBe false
    b8.hasTeamPlayed(team4) mustBe true
    b8.scores().get(team1).get.hidden mustBe false
    b8.scores().get(team2).get.hidden mustBe true
    b8.scores().get(team3).get.hidden mustBe false
    b8.scores().get(team4).get.hidden mustBe true
    b8.scores().get(team1).get.score mustBe 0
    b8.scores().get(team2).get.score mustBe 0
    b8.scores().get(team3).get.score mustBe 0
    b8.scores().get(team4).get.score mustBe 0
    b8.scores(false).get(team1).get.hidden mustBe false
    b8.scores(false).get(team2).get.hidden mustBe true
    b8.scores(false).get(team3).get.hidden mustBe false
    b8.scores(false).get(team4).get.hidden mustBe true
    b8.scores(false).get(team1).get.score mustBe 0
    b8.scores(false).get(team2).get.score mustBe 0
    b8.scores(false).get(team3).get.score mustBe 0
    b8.scores(false).get(team4).get.score mustBe 0
  }

//  board 7 played by 1 and 3
//  board 8 played by 2 and 4

  it should "score from T2 and T4 perspective" in {
    val score = MatchDuplicateScore( md, PerspectiveTable(team2, team4 ))
    score.teamScores mustBe TestMatchDuplicate.getTeamScore()
    val b7 = score.boards.get(Board.id(7)).get
    b7.hasTeamPlayed(team1) mustBe true
    b7.hasTeamPlayed(team2) mustBe false
    b7.hasTeamPlayed(team3) mustBe true
    b7.hasTeamPlayed(team4) mustBe false
    b7.scores().get(team1).get.hidden mustBe true
    b7.scores().get(team2).get.hidden mustBe false
    b7.scores().get(team3).get.hidden mustBe true
    b7.scores().get(team4).get.hidden mustBe false
    b7.scores().get(team1).get.score mustBe 0
    b7.scores().get(team2).get.score mustBe 0
    b7.scores().get(team3).get.score mustBe 0
    b7.scores().get(team4).get.score mustBe 0
    b7.scores(false).get(team1).get.hidden mustBe true
    b7.scores(false).get(team2).get.hidden mustBe false
    b7.scores(false).get(team3).get.hidden mustBe true
    b7.scores(false).get(team4).get.hidden mustBe false
    b7.scores(false).get(team1).get.score mustBe 0
    b7.scores(false).get(team2).get.score mustBe 0
    b7.scores(false).get(team3).get.score mustBe 0
    b7.scores(false).get(team4).get.score mustBe 0
    val b8 = score.boards.get(Board.id(8)).get
    b8.hasTeamPlayed(team1) mustBe false
    b8.hasTeamPlayed(team2) mustBe true
    b8.hasTeamPlayed(team3) mustBe false
    b8.hasTeamPlayed(team4) mustBe true
    b8.scores().get(team1).get.hidden mustBe false
    b8.scores().get(team2).get.hidden mustBe true
    b8.scores().get(team3).get.hidden mustBe false
    b8.scores().get(team4).get.hidden mustBe true
    b8.scores().get(team1).get.score mustBe 0
    b8.scores().get(team2).get.score mustBe 0
    b8.scores().get(team3).get.score mustBe 0
    b8.scores().get(team4).get.score mustBe 0
    b8.scores(false).get(team1).get.hidden mustBe false
    b8.scores(false).get(team2).get.hidden mustBe false
    b8.scores(false).get(team3).get.hidden mustBe false
    b8.scores(false).get(team4).get.hidden mustBe false
    b8.scores(false).get(team1).get.score mustBe 0
    b8.scores(false).get(team2).get.score mustBe 450
    b8.scores(false).get(team3).get.score mustBe 0
    b8.scores(false).get(team4).get.score mustBe -450
  }

  behavior of "creating a new match"

  it should "not throw any exceptions" in {
    implicit val ec = ExecutionContext.global
    new BridgeServiceInMemory("test").fillBoards(MatchDuplicate.create(MatchDuplicate.id(3))).map { _ match {
      case Right(m) =>
        val s = MatchDuplicateScore(m, PerspectiveDirector )
        s.tables.size mustBe 2
      case Left((code,msg)) =>
        fail( "Did not fill boards: "+code+" "+msg.msg )
    }}
  }
}
