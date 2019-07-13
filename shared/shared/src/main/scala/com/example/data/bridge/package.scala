package com.github.thebridsk.bridge.data

/**
  * @author werewolf
  */
package object bridge {

  val contractSuits = NoTrump :: Spades :: Hearts :: Diamonds :: Clubs :: Nil

  val contractTricks = ContractTricks(0) :: ContractTricks(1) :: ContractTricks(
    2
  ) :: ContractTricks(3) ::
    ContractTricks(4) :: ContractTricks(5) :: ContractTricks(6) :: ContractTricks(
    7
  ) :: Nil

  val contractDoubling = NotDoubled :: Doubled :: Redoubled :: Nil

  val vulnerabilities = NotVul :: Vul :: Nil

  import scala.language.implicitConversions

  implicit def handToBridgeHand(hand: Hand) = BridgeHand(hand)
  implicit def bridgeHandToHand(hand: BridgeHand) = hand.asHand()

}
