package com.github.thebridsk.bridge.data.bridge

import scala.language.implicitConversions

import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.RubberHand

object RubberBridge {

  class ScoreHand(
      id: String,
      contractTricks: ContractTricks,
      contractSuit: ContractSuit,
      contractDoubled: ContractDoubled,
      declarer: PlayerPosition,
      nsVul: Vulnerability,
      ewVul: Vulnerability,
      madeContract: MadeOrDown,
      tricks: Int,
      val honors: Int = 0,
      honorsPlayer: Option[PlayerPosition] = None
  ) extends BridgeHand(
        id,
        contractTricks,
        contractSuit,
        contractDoubled,
        declarer,
        nsVul,
        ewVul,
        madeContract,
        tricks
      ) {

    if (honors == 100 && contractSuit == NoTrump)
      throw new IllegalArgumentException
    val isDeclarerNorthSouth: Boolean = declarer == North || declarer == South
    val isDeclarerEastWest: Boolean = !isDeclarerNorthSouth

    val (nsScored, above, below, isGame, explainAbove, explainBelow) =
      calculate

    val nsScoredHonors: Boolean = honorsPlayer match {
      case Some(p) if (p == North || p == South) => true
      case _                                     => false
    }

    @inline
    private def scoring(ns: Boolean) = if (ns) "NS"; else "EW"

    val explainHonor: String =
      if (honors == 0) ""
      else {
        "Honors " + honors
      }

    def explain: String = {
      def show(pos: String, list: List[String]) =
        if (list.isEmpty) "" else pos + " " + list.mkString(", ")
      val sc = scoring(nsScored)
      if (honors == 0) {
        sc + " " + explainAbove.mkString(", ") + " / " + explainBelow.mkString(
          ", "
        )
      } else {
        if (nsScored == nsScoredHonors) {
          val e = (explainHonor :: (explainAbove.reverse)).reverse
          sc + " " + e.mkString(", ") + " / " + explainBelow.mkString(", ")
        } else {
          show(sc, explainAbove) + ", " + scoring(
            nsScoredHonors
          ) + " " + explainHonor + " / " + show(
            sc,
            explainBelow
          )
        }
      }
    }

    def totalScore: String = {
      if (honors != 0) {
        if (nsScored == nsScoredHonors) {
          scoring(nsScored) + " " + (above + honors) + " / " + below
        } else {
          scoring(nsScored) + " " + above + " / " + below +
            ", " + scoring(nsScoredHonors) + " " + honors + " / 0"
        }
      } else {
        scoring(nsScored) + " " + above + " / " + below
      }
    }

    def scoring(
        ns: Boolean,
        north: String,
        south: String,
        east: String,
        west: String
    ): String = {
      if (ns) s"$north-$south" else s"$east-$west"
    }

    def totalScore(
        north: String,
        south: String,
        east: String,
        west: String
    ): String = {
      if (honors != 0) {
        if (nsScored == nsScoredHonors) {
          scoring(
            nsScored,
            north,
            south,
            east,
            west
          ) + " " + (above + honors) + " / " + below
        } else {
          scoring(
            nsScored,
            north,
            south,
            east,
            west
          ) + " " + above + " / " + below +
            ", " + scoring(
            nsScoredHonors,
            north,
            south,
            east,
            west
          ) + " " + honors + " / 0"
        }
      } else {
        scoring(
          nsScored,
          north,
          south,
          east,
          west
        ) + " " + above + " / " + below
      }
    }

    def totalScoreNoPos(
        north: String,
        south: String,
        east: String,
        west: String
    ): String = {
      totalScore(north, south, east, west)
    }

    /**
      * @return a tuple4
      *         nsAbove
      *         nsBelow
      *         ewAbove
      *         ewBelow
      */
    def getScores: (Int, Int, Int, Int) =
      /* (nsAbove,nsBelow,ewAbove,ewBelow) */ {
        if (honors == 0) {
          if (nsScored) (above, below, 0, 0)
          else (0, 0, above, below)
        } else {
          if (nsScored == nsScoredHonors) {
            if (nsScored) (above + honors, below, 0, 0)
            else (0, 0, above + honors, below)
          } else {
            if (nsScored) (above, below, honors, 0)
            else (honors, 0, above, below)
          }
        }
      }

    def isDeclarerVulnerable: Boolean =
      declarer match {
        case North | South => nsVul.vul
        case East | West   => ewVul.vul
      }

    val contractAndResultAsString: String =
      if (contractTricks.tricks == 0) "Passed Out";
      else
        contractTricks.tricks
          .toString() + contractSuit.suit + contractDoubled.forScore + " " +
          (if (isDeclarerVulnerable) "Vul";
           else "NotVul") + "   " + madeOrDown.forScore + " " + tricks

    def contractAsString(
        vul: String = "Vul",
        notvul: String = "NotVul"
    ): String = {
      if (contractTricks.tricks == 0) "Passed Out";
      else
        contractTricks.tricks
          .toString() + contractSuit.suit + contractDoubled.forScore + " " +
          (if (isDeclarerVulnerable) vul; else notvul)
    }

    def contractAsStringNoVul: String =
      if (contractTricks.tricks == 0) "Passed Out";
      else
        contractTricks.tricks
          .toString() + contractSuit.suit + contractDoubled.forScore

    def getDeclarerForScoring: String =
      if (contractTricks.tricks == 0) "-"; else declarer.pos

    def getMadeForScoring: Option[String] =
      if (contractTricks.tricks == 0) Some("-")
      else
        madeOrDown match {
          case Made => Some(tricks.toString); case Down => None
        }

    def getDownForScoring: Option[String] =
      if (contractTricks.tricks == 0) Some("-")
      else
        madeOrDown match {
          case Down => Some(tricks.toString); case Made => None
        }

    /**
      * @return a tuple with the following values:
      * <ol>
      * <li>Boolean - true for NS, false for EW
      * <li>Int - above line score (for rubber)
      * <li>Int - below line score (for rubber)
      * <li>Boolean - game
      * <li>List[String] - reason for score above line rubber
      * <li>List[String] - reason for score below line rubber
      * </ol>
      */
    private def calculateMade
        : (Boolean, Int, Int, Boolean, List[String], List[String]) = {
      def timesDoubling(
          firstTrickBonus: Int,
          trickValue: Int
      ): (Int, Int, Int, Int) =
        contractDoubled match {
          case NotDoubled => (firstTrickBonus, trickValue, trickValue, 0)
          case Doubled =>
            (
              firstTrickBonus * 2,
              trickValue * 2,
              if (isDeclarerVulnerable) 200; else 100,
              50
            )
          case Redoubled =>
            (
              firstTrickBonus * 4,
              trickValue * 4,
              if (isDeclarerVulnerable) 400; else 200,
              100
            )
        }

      val (firstTrickBonus, trickValue, overTricksValue, insultBonus) =
        contractSuit match {
          case Spades | Hearts  => timesDoubling(0, 30)
          case Diamonds | Clubs => timesDoubling(0, 20)
          case NoTrump          => timesDoubling(10, 30)
        }
      val tricksInContract = Math.min(tricks, contractTricks.tricks)
      val overTricks = tricks - tricksInContract
      val valFromContractTricks =
        firstTrickBonus + tricksInContract * trickValue
      val valFromOverTricks = overTricks * overTricksValue
      val game = valFromContractTricks >= 100
      val slamBonus = contractTricks.tricks match {
        case 6 => if (isDeclarerVulnerable) 750; else 500
        case 7 => if (isDeclarerVulnerable) 1500; else 1000
        case _ => 0
      }

      val reasonTricks =
        "Tricks(" + tricksInContract + ") " + valFromContractTricks :: Nil

      var reason: List[String] = Nil
      if (slamBonus > 0) {
        if (contractTricks.tricks == 7) {
          reason = "GrandSlam " + slamBonus :: reason
        } else {
          reason = "Slam " + slamBonus :: reason
        }
      }
      if (insultBonus > 0) reason = "Insult " + insultBonus :: reason
      if (overTricks > 0)
        reason = "Overtricks(" + overTricks + ") " + valFromOverTricks :: reason

      (
        /* ns scored */ isDeclarerNorthSouth,
        /* above line */ valFromOverTricks + insultBonus + slamBonus,
        /* below line */ valFromContractTricks,
        /* game */ game,
        /* reason above */ reason,
        /* reason below */ reasonTricks
      )
    }

    /**
      * @return a tuple with the following values:
      * <ol>
      * <li>Boolean - true for NS, false for EW
      * <li>Int - above line score (for rubber)
      * <li>Int - below line score (for rubber)
      * <li>Boolean - game
      * <li>List[String] - reason for score above line rubber
      * <li>List[String] - reason for score below line rubber
      * </ol>
      */
    private def calculateDown
        : (Boolean, Int, Int, Boolean, List[String], List[String]) = {
      val (firstTrickValue, secondThirdTrickValue, remainingTrickValue) = {
        contractDoubled match {
          case NotDoubled =>
            if (isDeclarerVulnerable) (100, 100, 100); else (50, 50, 50)
          case Doubled =>
            if (isDeclarerVulnerable) (200, 300, 300); else (100, 200, 300)
          case Redoubled =>
            if (isDeclarerVulnerable) (400, 600, 600); else (200, 400, 600)
        }
      }
      var n = tricks
      var tricks1 = 1
      var totalFirst = firstTrickValue
      n = n - 1
      var tricks23 = Math.min(n, 2)
      var total23 = tricks23 * secondThirdTrickValue
      n = n - 2
      var tricks4p = if (n > 0) n; else 0
      var total4p = tricks4p * remainingTrickValue

      if (remainingTrickValue == secondThirdTrickValue) {
        tricks23 += tricks4p
        total23 += total4p
        tricks4p = 0
        total4p = 0
      }

      if (secondThirdTrickValue == firstTrickValue) {
        tricks1 += tricks23
        totalFirst += total23
        tricks23 = 0
        total23 = 0
      }

      var reason: List[String] = Nil
      if (tricks4p > 0)
        reason = "" + tricks4p + " at " + remainingTrickValue :: reason
      if (tricks23 > 0)
        reason = "" + tricks23 + " at " + secondThirdTrickValue :: reason
      reason = "" + tricks1 + " at " + firstTrickValue :: reason
      (
        /* ns scored */ !isDeclarerNorthSouth,
        /* above line */ totalFirst + total23 + total4p,
        /* below line */ 0,
        /* game */ false,
        /* reason above */ reason,
        /* reason below */ Nil
      )
    }

    /**
      * @return a tuple with the following values:
      * <ol>
      * <li>Boolean - true for NS, false for EW
      * <li>Int - above line score (for rubber)
      * <li>Int - below line score (for rubber)
      * <li>Boolean - game
      * <li>List[String] - reason for score above line rubber
      * <li>List[String] - reason for score below line rubber
      * </ol>
      */
    private def calculate
        : (Boolean, Int, Int, Boolean, List[String], List[String]) =
      if (contractTricks.tricks == 0) {
        (true, 0, 0, false, Nil, "Passed out" :: Nil)
      } else {
        madeContract match {
          case Made =>
            calculateMade
          case Down => {
            calculateDown
          }
        }
      }

    override def toString() = toStringRubber

    def toStringRubber: String = {
      if (contractTricks.tricks == 0) "Passed out"
      else {
        val con =
          "" + contractTricks.tricks + contractSuit.suit + contractDoubled.forScore + " by " + declarer.pos
        val scoring = {
          if (nsScored) "NS " + above + "/" + below
          else "EW " + above + "/" + below
        }
        val made = madeContract.forScore + " " + tricks
        val res = "Result " + scoring
        val reason = explainAbove.mkString(", ") + " / " + explainBelow
          .mkString(", ")
        con + "   " + made + "   " + res + "   " + reason + (if (honors != 0)
                                                               ", " + explainHonor
                                                             else "")
      }
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
        tricks: Int,
        honors: Int = 0,
        honorsPlayer: Option[PlayerPosition] = None
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
        tricks,
        honors,
        honorsPlayer
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

    def getPlayerPos(pos: Option[String]): Option[PlayerPosition] = {
      pos.flatMap { p =>
        try {
          Some(PlayerPosition(p))
        } catch {
          case _: Exception => None
        }
      }
    }

    def apply(hand: RubberHand): ScoreHand = {
      new ScoreHand(
        hand.id,
        ContractTricks(hand.contractTricks),
        ContractSuit(hand.contractSuit),
        ContractDoubled(hand.contractDoubled),
        PlayerPosition(hand.declarer),
        Vulnerability(hand.nsVul),
        Vulnerability(hand.ewVul),
        MadeOrDown(hand.madeContract),
        hand.tricks,
        hand.honors,
        getPlayerPos(hand.honorsPlayer)
      )
    }

  }

  implicit def handToScoreHand(hand: Hand): ScoreHand = ScoreHand(hand)
  implicit def scoreHandToHand(hand: ScoreHand): Hand = hand.asHand()

}
