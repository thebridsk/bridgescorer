package com.github.thebridsk.bridge.server.test

import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.sample.TestMatchDuplicate
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.SystemTime
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore
import org.scalatest.MustMatchers
import com.github.thebridsk.bridge.server.util.SystemTimeJVM
import org.scalatest.FlatSpec
import com.github.thebridsk.bridge.server.backend.BridgeService
import com.github.thebridsk.bridge.data.bridge.PerspectiveTable
import com.github.thebridsk.bridge.data.bridge.PerspectiveDirector
import com.github.thebridsk.bridge.server.backend.BridgeServiceInMemory
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

class TestDuplicateScore extends FlatSpec with MustMatchers {

  SystemTimeJVM()

  behavior of "a played match"

  val dupid: Id.MatchDuplicate = "M1"
  val md = TestMatchDuplicate.getPlayedMatch(dupid)

//  board 7 played by 1 and 3
//  board 8 played by 2 and 4

  it should "score from T1 and T2 perspective" in {
    val score = MatchDuplicateScore( md, PerspectiveTable("T1", "T2") )
    score.teamScores mustBe TestMatchDuplicate.getTeamScore()
    val b7 = score.boards.get("B7").get
    b7.hasTeamPlayed("T1") mustBe true
    b7.hasTeamPlayed("T2") mustBe false
    b7.hasTeamPlayed("T3") mustBe true
    b7.hasTeamPlayed("T4") mustBe false
    b7.scores().get("T1").get.hidden mustBe true
    b7.scores().get("T2").get.hidden mustBe false
    b7.scores().get("T3").get.hidden mustBe true
    b7.scores().get("T4").get.hidden mustBe false
    b7.scores(false).get("T1").get.hidden mustBe true
    b7.scores(false).get("T2").get.hidden mustBe false
    b7.scores(false).get("T3").get.hidden mustBe true
    b7.scores(false).get("T4").get.hidden mustBe false
    val b8 = score.boards.get("B8").get
    b8.hasTeamPlayed("T1") mustBe false
    b8.hasTeamPlayed("T2") mustBe true
    b8.hasTeamPlayed("T3") mustBe false
    b8.hasTeamPlayed("T4") mustBe true
    b8.scores().get("T1").get.hidden mustBe false
    b8.scores().get("T2").get.hidden mustBe true
    b8.scores().get("T3").get.hidden mustBe false
    b8.scores().get("T4").get.hidden mustBe true
    b8.scores(false).get("T1").get.hidden mustBe false
    b8.scores(false).get("T2").get.hidden mustBe true
    b8.scores(false).get("T3").get.hidden mustBe false
    b8.scores(false).get("T4").get.hidden mustBe true
  }

  it should "score from T3 and T4 perspective" in {
    val score = MatchDuplicateScore( md, PerspectiveTable("T3", "T4" ))
    score.teamScores mustBe TestMatchDuplicate.getTeamScore()
    val b7 = score.boards.get("B7").get
    b7.hasTeamPlayed("T1") mustBe true
    b7.hasTeamPlayed("T2") mustBe false
    b7.hasTeamPlayed("T3") mustBe true
    b7.hasTeamPlayed("T4") mustBe false
    b7.scores().get("T1").get.hidden mustBe true
    b7.scores().get("T2").get.hidden mustBe false
    b7.scores().get("T3").get.hidden mustBe true
    b7.scores().get("T4").get.hidden mustBe false
    b7.scores(false).get("T1").get.hidden mustBe true
    b7.scores(false).get("T2").get.hidden mustBe false
    b7.scores(false).get("T3").get.hidden mustBe true
    b7.scores(false).get("T4").get.hidden mustBe false
    val b8 = score.boards.get("B8").get
    b8.hasTeamPlayed("T1") mustBe false
    b8.hasTeamPlayed("T2") mustBe true
    b8.hasTeamPlayed("T3") mustBe false
    b8.hasTeamPlayed("T4") mustBe true
    b8.scores().get("T1").get.hidden mustBe false
    b8.scores().get("T2").get.hidden mustBe true
    b8.scores().get("T3").get.hidden mustBe false
    b8.scores().get("T4").get.hidden mustBe true
    b8.scores(false).get("T1").get.hidden mustBe false
    b8.scores(false).get("T2").get.hidden mustBe true
    b8.scores(false).get("T3").get.hidden mustBe false
    b8.scores(false).get("T4").get.hidden mustBe true
  }

//  board 7 played by 1 and 3
//  board 8 played by 2 and 4

  it should "score from T1 and T3 perspective" in {
    val score = MatchDuplicateScore( md, PerspectiveTable("T1", "T3" ))
    score.teamScores mustBe TestMatchDuplicate.getTeamScore()
    val b7 = score.boards.get("B7").get
    b7.hasTeamPlayed("T1") mustBe true
    b7.hasTeamPlayed("T2") mustBe false
    b7.hasTeamPlayed("T3") mustBe true
    b7.hasTeamPlayed("T4") mustBe false
    b7.scores().get("T1").get.hidden mustBe true
    b7.scores().get("T2").get.hidden mustBe false
    b7.scores().get("T3").get.hidden mustBe true
    b7.scores().get("T4").get.hidden mustBe false
    b7.scores().get("T1").get.score mustBe 0
    b7.scores().get("T2").get.score mustBe 0
    b7.scores().get("T3").get.score mustBe 0
    b7.scores().get("T4").get.score mustBe 0
    b7.scores(false).get("T1").get.hidden mustBe false
    b7.scores(false).get("T2").get.hidden mustBe false
    b7.scores(false).get("T3").get.hidden mustBe false
    b7.scores(false).get("T4").get.hidden mustBe false
    b7.scores(false).get("T1").get.score mustBe -650
    b7.scores(false).get("T2").get.score mustBe 0
    b7.scores(false).get("T3").get.score mustBe 650
    b7.scores(false).get("T4").get.score mustBe 0
    val b8 = score.boards.get("B8").get
    b8.hasTeamPlayed("T1") mustBe false
    b8.hasTeamPlayed("T2") mustBe true
    b8.hasTeamPlayed("T3") mustBe false
    b8.hasTeamPlayed("T4") mustBe true
    b8.scores().get("T1").get.hidden mustBe false
    b8.scores().get("T2").get.hidden mustBe true
    b8.scores().get("T3").get.hidden mustBe false
    b8.scores().get("T4").get.hidden mustBe true
    b8.scores().get("T1").get.score mustBe 0
    b8.scores().get("T2").get.score mustBe 0
    b8.scores().get("T3").get.score mustBe 0
    b8.scores().get("T4").get.score mustBe 0
    b8.scores(false).get("T1").get.hidden mustBe false
    b8.scores(false).get("T2").get.hidden mustBe true
    b8.scores(false).get("T3").get.hidden mustBe false
    b8.scores(false).get("T4").get.hidden mustBe true
    b8.scores(false).get("T1").get.score mustBe 0
    b8.scores(false).get("T2").get.score mustBe 0
    b8.scores(false).get("T3").get.score mustBe 0
    b8.scores(false).get("T4").get.score mustBe 0
  }

//  board 7 played by 1 and 3
//  board 8 played by 2 and 4

  it should "score from T2 and T4 perspective" in {
    val score = MatchDuplicateScore( md, PerspectiveTable("T2", "T4" ))
    score.teamScores mustBe TestMatchDuplicate.getTeamScore()
    val b7 = score.boards.get("B7").get
    b7.hasTeamPlayed("T1") mustBe true
    b7.hasTeamPlayed("T2") mustBe false
    b7.hasTeamPlayed("T3") mustBe true
    b7.hasTeamPlayed("T4") mustBe false
    b7.scores().get("T1").get.hidden mustBe true
    b7.scores().get("T2").get.hidden mustBe false
    b7.scores().get("T3").get.hidden mustBe true
    b7.scores().get("T4").get.hidden mustBe false
    b7.scores().get("T1").get.score mustBe 0
    b7.scores().get("T2").get.score mustBe 0
    b7.scores().get("T3").get.score mustBe 0
    b7.scores().get("T4").get.score mustBe 0
    b7.scores(false).get("T1").get.hidden mustBe true
    b7.scores(false).get("T2").get.hidden mustBe false
    b7.scores(false).get("T3").get.hidden mustBe true
    b7.scores(false).get("T4").get.hidden mustBe false
    b7.scores(false).get("T1").get.score mustBe 0
    b7.scores(false).get("T2").get.score mustBe 0
    b7.scores(false).get("T3").get.score mustBe 0
    b7.scores(false).get("T4").get.score mustBe 0
    val b8 = score.boards.get("B8").get
    b8.hasTeamPlayed("T1") mustBe false
    b8.hasTeamPlayed("T2") mustBe true
    b8.hasTeamPlayed("T3") mustBe false
    b8.hasTeamPlayed("T4") mustBe true
    b8.scores().get("T1").get.hidden mustBe false
    b8.scores().get("T2").get.hidden mustBe true
    b8.scores().get("T3").get.hidden mustBe false
    b8.scores().get("T4").get.hidden mustBe true
    b8.scores().get("T1").get.score mustBe 0
    b8.scores().get("T2").get.score mustBe 0
    b8.scores().get("T3").get.score mustBe 0
    b8.scores().get("T4").get.score mustBe 0
    b8.scores(false).get("T1").get.hidden mustBe false
    b8.scores(false).get("T2").get.hidden mustBe false
    b8.scores(false).get("T3").get.hidden mustBe false
    b8.scores(false).get("T4").get.hidden mustBe false
    b8.scores(false).get("T1").get.score mustBe 0
    b8.scores(false).get("T2").get.score mustBe 450
    b8.scores(false).get("T3").get.score mustBe 0
    b8.scores(false).get("T4").get.score mustBe -450
  }

  behavior of "creating a new match"

  it should "not throw any exceptions" in {
    implicit val ec = ExecutionContext.global
    new BridgeServiceInMemory("test").fillBoards(MatchDuplicate.create("M3")).map { _ match {
      case Right(m) =>
        val s = MatchDuplicateScore(m, PerspectiveDirector )
        s.tables.size mustBe 2
      case Left((code,msg)) =>
        fail( "Did not fill boards: "+code+" "+msg.msg )
    }}
  }
}
