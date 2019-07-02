package com.example.data.bridge

import com.example.data.Hand

import scala.language.implicitConversions
import com.example.data.SystemTime

sealed abstract case class ContractSuit(suit: String)

object NoTrump extends ContractSuit("N")
object Spades extends ContractSuit("S")
object Hearts extends ContractSuit("H")
object Diamonds extends ContractSuit("D")
object Clubs extends ContractSuit("C")

object ContractSuit {
  def apply(suit: String) = suit match {
    case "N" => NoTrump
    case "S" => Spades
    case "H" => Hearts
    case "D" => Diamonds
    case "C" => Clubs
    case _ =>
      throw new IllegalArgumentException(
        "Unknown value for a contract suit: " + suit
      )
  }

  def getRank(suit: String) = suit match {
    case "N" => 5
    case "S" => 4
    case "H" => 3
    case "D" => 2
    case "C" => 1
    case _ =>
      throw new IllegalArgumentException(
        "Unknown value for a contract suit: " + suit
      )
  }

  def compare(suit1: String, suit2: String): Int = {
    getRank(suit1).compare(getRank(suit2))
  }
}

/**
  * @param tricks the number of tricks for the contract.
  *               0 indicates the hand was passed out
  */
case class ContractTricks(tricks: Int) {
  require(0 <= tricks && tricks <= 7)
}

object ContractTricks {
  implicit def contractTricksToInt(tricks: ContractTricks) = tricks.tricks
  implicit def intToContractTricks(tricks: Int) = ContractTricks(tricks)
}

object PassedOut extends ContractTricks(0)

sealed abstract case class ContractDoubled(doubled: String, forScore: String)

object NotDoubled extends ContractDoubled("N", "")
object Doubled extends ContractDoubled("D", "*")
object Redoubled extends ContractDoubled("R", "**")

object ContractDoubled {
  def apply(doubled: String) = doubled match {
    case "N" => NotDoubled
    case "D" => Doubled
    case "R" => Redoubled
    case _ =>
      throw new IllegalArgumentException(
        "Unknown value for a contract doubled: " + doubled
      )
  }
}

sealed abstract case class PlayerPosition(pos: String, name: String) {
  def nextDealer = PlayerPosition.nextDealer(this)
  def forDisplay = PlayerPosition.forDisplay(this)
  def left = PlayerPosition.left(this)
  def right = PlayerPosition.right(this)
  def partner = PlayerPosition.partner(this)

  def player(north: String, south: String, east: String, west: String) =
    this match {
      case North => north
      case East  => east
      case West  => west
      case South => south
      case _ =>
        throw new IllegalArgumentException(
          "Unknown value for a player position: " + this
        )
    }
}

object North extends PlayerPosition("N", "North")
object East extends PlayerPosition("E", "East")
object West extends PlayerPosition("W", "West")
object South extends PlayerPosition("S", "South")

object PlayerPosition {
  def apply(pos: String) = pos match {
    case "N" => North
    case "E" => East
    case "W" => West
    case "S" => South
    case _ =>
      throw new IllegalArgumentException(
        "Unknown value for a player position: " + pos
      )
  }

  def nextDealer(current: PlayerPosition) = left(current)
  def prevDealer(current: PlayerPosition) = right(current)

  def left(current: PlayerPosition) = current match {
    case North => East
    case East  => South
    case West  => North
    case South => West
    case _ =>
      throw new IllegalArgumentException(
        "Unknown value for a player position: " + current
      )
  }

  def right(current: PlayerPosition) = current match {
    case North => West
    case East  => North
    case West  => South
    case South => East
    case _ =>
      throw new IllegalArgumentException(
        "Unknown value for a player position: " + current
      )
  }

  def partner(current: PlayerPosition) = current match {
    case North => South
    case East  => West
    case West  => East
    case South => North
    case _ =>
      throw new IllegalArgumentException(
        "Unknown value for a player position: " + current
      )
  }

  def forDisplay(current: PlayerPosition) = current match {
    case North => "North"
    case East  => "East"
    case West  => "West"
    case South => "South"
    case _ =>
      throw new IllegalArgumentException(
        "Unknown value for a player position: " + current
      )
  }

  def fromDisplay(pos: String) = pos match {
    case "North" => North
    case "East"  => East
    case "West"  => West
    case "South" => South
    case _ =>
      throw new IllegalArgumentException(
        "Unknown value for a player position: " + pos
      )
  }
}

sealed abstract case class MadeOrDown(made: Boolean, forScore: String)

object Made extends MadeOrDown(true, "made")
object Down extends MadeOrDown(false, "down")

object MadeOrDown {
  def apply(made: Boolean) = if (made) Made; else Down
}

sealed abstract case class Vulnerability(vul: Boolean)

object Vul extends Vulnerability(true)
object NotVul extends Vulnerability(false)

object Vulnerability {
  def apply(vul: Boolean) = if (vul) Vul; else NotVul
}

abstract trait ScoringSystem
trait Duplicate extends ScoringSystem
trait Rubber extends ScoringSystem
trait Chicago extends ScoringSystem
trait Test extends ScoringSystem

object Duplicate extends Duplicate
object Rubber extends Rubber
object Chicago extends Chicago
object TestDuplicate extends Duplicate with Test
object TestRubber extends Rubber with Test
object TestChicago extends Chicago with Test

class BridgeHand(
    val id: String,
    val contractTricks: ContractTricks,
    val contractSuit: ContractSuit,
    val contractDoubled: ContractDoubled,
    val declarer: PlayerPosition,
    val nsVul: Vulnerability,
    val ewVul: Vulnerability,
    val madeOrDown: MadeOrDown,
    val tricks: Int
) {
  madeOrDown match {
    case Made =>
      if (contractTricks > tricks || tricks > 7) {
        throw new IllegalArgumentException(
          "For a made contract, tricks must be in range of [contractTricks,7], tricks=" + tricks + ", contract=" + contractTricks.tricks
        )
      }
    case Down =>
      if (1 > tricks || tricks > 6 + contractTricks.tricks) {
        throw new IllegalArgumentException(
          "For a down contract, tricks must be in range of [1,6+contractTricks], tricks=" + tricks + ", contract=" + contractTricks.tricks
        )
      }
  }

  def asHand() =
    Hand.create(
      id,
      contractTricks.tricks,
      contractSuit.suit,
      contractDoubled.doubled,
      declarer.pos,
      nsVul.vul,
      ewVul.vul,
      madeOrDown.made,
      tricks,
      SystemTime.currentTimeMillis(),
      SystemTime.currentTimeMillis()
    )

  def getTricksRange() = BridgeHand.getTricksRange(madeOrDown, contractTricks)
}

object BridgeHand {
  def apply(
      id: String,
      contractTricks: ContractTricks,
      contractSuit: ContractSuit,
      contractDoubled: ContractDoubled,
      declarer: PlayerPosition,
      nsVul: Vulnerability,
      ewVul: Vulnerability,
      madeContract: MadeOrDown,
      tricks: Int
  ) = {
    new BridgeHand(
      id,
      contractTricks,
      contractSuit,
      contractDoubled,
      declarer,
      nsVul,
      ewVul,
      madeContract,
      tricks
    )
  }

  def apply(hand: Hand) = {
    new BridgeHand(
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

  def getTricksRange(madeOrDown: MadeOrDown, contractTricks: ContractTricks) = {
    if (contractTricks.tricks == 0) {
      0 until 0
    } else {
      madeOrDown match {
        case Made =>
          contractTricks.tricks to 7
        case Down =>
          1 to 6 + contractTricks.tricks
      }
    }
  }
}
