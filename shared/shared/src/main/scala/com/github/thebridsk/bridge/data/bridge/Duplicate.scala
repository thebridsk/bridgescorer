package com.github.thebridsk.bridge.data.bridge

import scala.language.implicitConversions

import com.github.thebridsk.bridge.data.Hand

/**
  * @author werewolf
  */
object DuplicateBridge {

  case class DuplicateScore(ns: Int, ew: Int)

  class ScoreHand(
      private val sid: String,
      private val scontractTricks: ContractTricks,
      private val scontractSuit: ContractSuit,
      private val scontractDoubled: ContractDoubled,
      private val sdeclarer: PlayerPosition,
      private val snsvul: Vulnerability,
      private val sewvul: Vulnerability,
      private val smadeContract: MadeOrDown,
      private val stricks: Int
  ) extends RubberBridge.ScoreHand(
        sid,
        scontractTricks,
        scontractSuit,
        scontractDoubled,
        sdeclarer,
        snsvul,
        sewvul,
        smadeContract,
        stricks
      ) {

    private val gameBonus =
      if (madeOrDown == Made && contractTricks.tricks > 0 && tricks > 0)
        if (isGame) if (isDeclarerVulnerable) 500; else 300; else 50;
      else 0;

    val score: DuplicateScore = {
      val nsscore = (above + below + gameBonus) * (if (nsScored) 1; else -1)
      val ewscore = -nsscore
      DuplicateScore(nsscore, ewscore)
    }

    val explainList: List[String] =
      explainBelow ::: explainAbove ::: (gameBonus match {
        case 0 =>
          Nil
        case 50 =>
          "Partial " + gameBonus :: Nil
        case _ =>
          "Game " + gameBonus :: Nil
      })

    override def explain: String = explainList.mkString(", ")

    override def totalScore: String =
      if (score.ns >= 0) "NS " + score.ns; else "EW " + score.ew

    override def totalScore(
        north: String,
        south: String,
        east: String,
        west: String
    ): String = {
      if (score.ns == 0) "0"
      else if (score.ns >= 0) s"NS ${score.ns} $north-$south"
      else s"EW ${score.ew} $east-$west"
    }

    override def totalScoreNoPos(
        north: String,
        south: String,
        east: String,
        west: String
    ): String = {
      if (score.ns == 0) "0"
      else if (score.ns >= 0) s"${score.ns} $north-$south"
      else s"${score.ew} $east-$west"
    }
  }

  object ScoreHand {
    def apply(
        id: String,
        contractTricks: ContractTricks,
        contractSuit: ContractSuit,
        contractDoubled: ContractDoubled,
        declarer: PlayerPosition,
        nsvul: Vulnerability,
        ewvul: Vulnerability,
        madeContract: MadeOrDown,
        tricks: Int
    ): ScoreHand = {
      new ScoreHand(
        id,
        contractTricks,
        contractSuit,
        contractDoubled,
        declarer,
        nsvul,
        ewvul,
        madeContract,
        tricks
      )
    }

    def apply(hand: BridgeHand): ScoreHand = {
      new ScoreHand(
        hand.id,
        hand.contractTricks,
        hand.contractSuit,
        hand.contractDoubled,
        hand.declarer,
        hand.nsVul,
        hand.ewVul,
        if (hand.madeContract) Made; else Down,
        hand.tricks
      )
    }

    def apply(hand: Hand): ScoreHand = {
      new ScoreHand(
        hand.id,
        ContractTricks(hand.contractTricks),
        ContractSuit(hand.contractSuit),
        ContractDoubled(hand.contractDoubled),
        PlayerPosition(hand.declarer),
        Vulnerability(hand.nsVul),
        Vulnerability(hand.ewVul),
        MadeOrDown(hand.madeContract),
        hand.tricks
      )
    }

  }

  implicit def handToScoreHand(hand: Hand): ScoreHand = ScoreHand(hand)
  implicit def scoreHandToHand(hand: ScoreHand): Hand = hand.asHand()

}
