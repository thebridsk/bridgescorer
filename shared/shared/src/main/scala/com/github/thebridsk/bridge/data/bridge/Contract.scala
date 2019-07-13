package com.github.thebridsk.bridge.data.bridge

import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.RubberHand

/**
  * @author werewolf
  */
case class Contract(
    id: String,
    contractTricks: ContractTricks,
    contractSuit: ContractSuit,
    contractDoubled: ContractDoubled,
    declarer: PlayerPosition,
    nsVul: Vulnerability,
    ewVul: Vulnerability,
    madeOrDown: MadeOrDown,
    tricks: Int,
    honor: Option[Int],
    honorPlayer: Option[PlayerPosition],
    scoringSystem: ScoringSystem,
    scorer: Option[Either[
      com.github.thebridsk.bridge.data.bridge.RubberBridge.ScoreHand,
      com.github.thebridsk.bridge.data.bridge.DuplicateBridge.ScoreHand
    ]],
    table: Int = 0,
    board: Int = 0,
    north: String = "nsPlayer1",
    south: String = "nsPlayer2",
    east: String = "ewPlayer1",
    west: String = "ewPlayer2",
    dealer: PlayerPosition
) {

  def toBridgeHand() =
    BridgeHand(
      id,
      contractTricks,
      contractSuit,
      contractDoubled,
      declarer,
      nsVul,
      ewVul,
      madeOrDown,
      tricks
    )
  def toHand(): com.github.thebridsk.bridge.data.Hand = toBridgeHand()
  def toRubberHand() =
    RubberHand(
      id,
      toHand,
      honor.getOrElse(0),
      honorPlayer.getOrElse(North).pos,
      0,
      0
    )
  def toDuplicate() =
    com.github.thebridsk.bridge.data.bridge.DuplicateBridge.ScoreHand(
      id,
      contractTricks,
      contractSuit,
      contractDoubled,
      declarer,
      nsVul,
      ewVul,
      madeOrDown,
      tricks
    )
  def toRubber() = com.github.thebridsk.bridge.data.bridge.RubberBridge.ScoreHand(toRubberHand)
  def withScoring() = {
    try {
      val s = Some(scoringSystem match {
        case _: Duplicate =>
          Right(toDuplicate())
        case _: Chicago =>
          Right(toDuplicate())
        case _: Rubber =>
          Left(toRubber())
      })
      copy(scorer = s)
    } catch {
      case t: Throwable =>
//          println( "Contract.withScoring exception "+t )
        copy(scorer = None)
    }
  }

  def getTrickRange() = BridgeHand.getTricksRange(madeOrDown, contractTricks)

  def clear() =
    Contract(
      id,
      ContractTricks(0),
      NoTrump,
      NotDoubled,
      North,
      nsVul,
      ewVul,
      Made,
      1,
      None,
      None,
      scoringSystem,
      None,
      table,
      board,
      north,
      south,
      east,
      west,
      dealer
    ).withScoring()
}

object Contract {
  def create(
      hand: Hand,
      scoringSystem: ScoringSystem,
      scorer: Option[Either[
        com.github.thebridsk.bridge.data.bridge.RubberBridge.ScoreHand,
        com.github.thebridsk.bridge.data.bridge.DuplicateBridge.ScoreHand
      ]],
      table: Int = 0,
      board: Int = 0,
      north: String = "nsPlayer1",
      south: String = "nsPlayer2",
      east: String = "ewPlayer1",
      west: String = "ewPlayer2",
      dealer: PlayerPosition = North
  ) = {
    new Contract(
      hand.id,
      ContractTricks(hand.contractTricks),
      ContractSuit(hand.contractSuit),
      ContractDoubled(hand.contractDoubled),
      PlayerPosition(hand.declarer),
      Vulnerability(hand.nsVul),
      Vulnerability(hand.ewVul),
      MadeOrDown(hand.madeContract),
      hand.tricks,
      None,
      None,
      scoringSystem,
      scorer,
      table,
      board,
      north,
      south,
      east,
      west,
      dealer
    )
  }

  def getPlayerPosition(pos: String) =
    try {
      Some(PlayerPosition(pos))
    } catch {
      case _: Exception => None
    }

  def createRubber(
      rubberhand: RubberHand,
      scoringSystem: ScoringSystem,
      scorer: Option[com.github.thebridsk.bridge.data.bridge.RubberBridge.ScoreHand],
      table: Int = 0,
      board: Int = 0,
      north: String = "nsPlayer1",
      south: String = "nsPlayer2",
      east: String = "ewPlayer1",
      west: String = "ewPlayer2",
      dealer: PlayerPosition = North
  ) = {
    val hand = rubberhand.hand
    new Contract(
      hand.id,
      ContractTricks(hand.contractTricks),
      ContractSuit(hand.contractSuit),
      ContractDoubled(hand.contractDoubled),
      PlayerPosition(hand.declarer),
      Vulnerability(hand.nsVul),
      Vulnerability(hand.ewVul),
      MadeOrDown(hand.madeContract),
      hand.tricks,
      Some(rubberhand.honors),
      getPlayerPosition(rubberhand.honorsPlayer),
      scoringSystem,
      scorer match {
        case Some(s) => Some(Left(s))
        case None    => None
      },
      table,
      board,
      north,
      south,
      east,
      west,
      dealer
    )
  }

}
