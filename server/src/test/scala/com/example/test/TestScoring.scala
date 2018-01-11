package com.example.test

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import org.scalatest.Matchers

import com.example.data.bridge._


class TestScoring extends FlatSpec with MustMatchers {

  import com.example.data.bridge.RubberBridge.{ScoreHand => RubberScore}
  import com.example.data.bridge.DuplicateBridge.{ScoreHand => DuplicateScore}

  behavior of "Duplicate Bridge Scoring"

  it should "In duplicate, north plays 3 No Trump not vulernable, Make 3 scores 400" in {
    val hand = DuplicateScore("",3,NoTrump,NotDoubled,North,NotVul,NotVul,Made,3)

    hand.score.ns mustBe 400
    hand.score.ew mustBe -400
    hand.explain mustBe "Tricks(3) 100, Game 300"
    hand.totalScore() mustBe "NS 400"
    hand.explainList mustBe "Tricks(3) 100"::"Game 300"::Nil
  }

  it should "In duplicate, east plays 2 spades doubled vulernable, Make 4 scores 400" in {
    val hand = DuplicateScore("",2,Spades,Doubled,East,NotVul,Vul,Made,4)

    hand.explainList mustBe "Tricks(2) 120"::"Overtricks(2) 400"::"Insult 50"::"Game 500"::Nil
    hand.explain mustBe "Tricks(2) 120, Overtricks(2) 400, Insult 50, Game 500"
    hand.totalScore() mustBe "EW 1070"

    hand.score.ew mustBe 1070
    hand.score.ns mustBe -1070
  }

  it should "In duplicate, north plays 1 No Trump redoubled vulernable, Make 7 scores 3160" in {
    val hand = DuplicateScore("",1,NoTrump,Redoubled,North,Vul,NotVul,Made,7)

    hand.score.ns mustBe 3160
    hand.score.ew mustBe -3160
    hand.totalScore() mustBe "NS 3160"
    hand.explainList mustBe "Tricks(1) 160"::"Overtricks(6) 2400"::"Insult 100"::"Game 500"::Nil
    hand.explain mustBe "Tricks(1) 160, Overtricks(6) 2400, Insult 100, Game 500"
  }

  it should "In duplicate, north plays 7 No Trump redoubled vulernable, Make 7 scores 2980" in {
    val hand = DuplicateScore("",7,NoTrump,Redoubled,North,Vul,NotVul,Made,7)

    hand.score.ns mustBe 2980
    hand.score.ew mustBe -2980
    hand.totalScore() mustBe "NS 2980"
    hand.explainList mustBe "Tricks(7) 880"::"Insult 100"::"GrandSlam 1500"::"Game 500"::Nil
    hand.explain mustBe "Tricks(7) 880, Insult 100, GrandSlam 1500, Game 500"
  }

  it should "In duplicate, north plays 7 Club redoubled not vulnerable, Down 1 scores 200" in {
    val hand = DuplicateScore("",7,Clubs,Redoubled,North,NotVul,NotVul,Down,1)

    hand.explainList mustBe "1 at 200"::Nil
    hand.explain mustBe "1 at 200"
    hand.score.ns mustBe -200
    hand.score.ew mustBe +200
    hand.totalScore() mustBe "EW 200"
  }

  behavior of "Rubber Bridge Scoring"

  it should "In rubber, east plays 3 No Trump not vulernable, Make 3 scores 100 below for east" in {
    val hand = RubberScore("",3,NoTrump,NotDoubled,East,NotVul,NotVul,Made,3)

    hand.nsScored mustBe false
    hand.below mustBe 100
    hand.above mustBe 0
    hand.explainBelow mustBe "Tricks(3) 100"::Nil
    hand.explainAbove mustBe Nil
    hand.honors mustBe 0

    hand.totalScore() mustBe "EW 0 / 100"
    hand.explain() mustBe "EW  / Tricks(3) 100"
    hand.getScores() mustBe (0,0,0,100)
  }

  it should "In rubber, north plays 2 spades doubled vulernable, Make 4 scores 400" in {
    val hand = RubberScore("",2,Spades,Doubled,North,Vul,NotVul,Made,4)

    hand.nsScored mustBe true
    hand.below mustBe 120
    hand.above mustBe 450

    hand.nsScored mustBe true
    hand.explainBelow mustBe "Tricks(2) 120"::Nil
    hand.explainAbove mustBe "Overtricks(2) 400"::"Insult 50"::Nil
    hand.honors mustBe 0
    hand.explainHonor mustBe ""

    hand.totalScore() mustBe "NS 450 / 120"
    hand.explain() mustBe "NS Overtricks(2) 400, Insult 50 / Tricks(2) 120"
    hand.getScores() mustBe (450,120,0,0)
  }

  it should "In rubber, north plays 2 spades doubled vulernable, with 100 honors, Make 4 scores 400" in {
    val hand = RubberScore("",2,Spades,Doubled,North,Vul,NotVul,Made,4,100,Some(North))

    hand.nsScored mustBe true
    hand.below mustBe 120
    hand.above mustBe 450

    hand.nsScored mustBe true
    hand.explainBelow mustBe "Tricks(2) 120"::Nil
    hand.explainAbove mustBe "Overtricks(2) 400"::"Insult 50"::Nil
    hand.nsScoredHonors mustBe true
    hand.explainHonor mustBe "Honors 100"

    hand.totalScore() mustBe "NS 550 / 120"
    hand.explain() mustBe "NS Overtricks(2) 400, Insult 50, Honors 100 / Tricks(2) 120"
    hand.getScores() mustBe (550,120,0,0)
  }

  it should "In rubber, north plays 2 spades doubled vulernable, with East 100 honors, Make 4 scores 400" in {
    val hand = RubberScore("",2,Spades,Doubled,North,Vul,NotVul,Made,4,100,Some(East))

    hand.nsScored mustBe true
    hand.below mustBe 120
    hand.above mustBe 450

    hand.nsScored mustBe true
    hand.explainBelow mustBe "Tricks(2) 120"::Nil
    hand.explainAbove mustBe "Overtricks(2) 400"::"Insult 50"::Nil
    hand.nsScoredHonors mustBe false
    hand.explainHonor mustBe "Honors 100"

    hand.totalScore() mustBe "NS 450 / 120, EW 100 / 0"
    hand.explain() mustBe "NS Overtricks(2) 400, Insult 50, EW Honors 100 / NS Tricks(2) 120"
    hand.getScores() mustBe (450,120,100,0)
  }
}
