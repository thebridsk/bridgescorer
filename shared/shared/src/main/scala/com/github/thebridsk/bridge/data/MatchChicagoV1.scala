package com.github.thebridsk.bridge.data

import com.github.thebridsk.bridge.data.SystemTime.Timestamp
import io.swagger.v3.oas.annotations.media.Schema

/**
  * A match of chicago
  * @param gamesPerRound ==0 unknown, 4, 6, or 8
  */
@Schema(description = "A chicago match, version 1 (old version)")
case class MatchChicagoV1(
    id: MatchChicago.Id,
    player1: String,
    player2: String,
    player3: String,
    player4: String,
    rounds: List[Round],
    gamesPerRound: Int,
    created: Timestamp,
    updated: Timestamp
) extends VersionedInstance[MatchChicago, MatchChicagoV1, MatchChicago.Id] {

  def setId(
      newId: MatchChicago.Id,
      forCreate: Boolean,
      dontUpdateTime: Boolean = false
  ): MatchChicagoV1 = {
    if (dontUpdateTime) {
      copy(id = newId)
    } else {
      val time = SystemTime.currentTimeMillis()
      copy(
        id = newId,
        created = if (forCreate) time; else created,
        updated = time
      )
    }
  }

  def copyForCreate(id: MatchChicago.Id): MatchChicagoV1 = {
    val time = SystemTime.currentTimeMillis()
    val xrounds = rounds.map { e =>
      e.copyForCreate(e.id)
    }.toList
    copy(id = id, created = time, updated = time, rounds = xrounds)

  }

  def addRound(r: Round): MatchChicagoV1 = {
    val n = copy(
      rounds = (r.copyForCreate(r.id) :: (rounds.reverse)).reverse,
      updated = SystemTime.currentTimeMillis()
    )
    if (player1 == null || player1.length() == 0) {
      n.copy(
        player1 = r.north,
        player2 = r.south,
        player3 = r.east,
        player4 = r.west
      )
    } else {
      n
    }

  }

  def modifyRound(r: Round): MatchChicagoV1 = {
    if (rounds.isEmpty) addRound(r)
    else {
      var mod = false
      val rs = rounds.map { oldr =>
        if (oldr.id == r.id) {
          mod = true
          r
        } else {
          oldr
        }
      }
      val newr = if (mod) {
        rs
      } else {
        (r :: (rounds.reverse)).reverse
      }
      copy(rounds = newr, updated = SystemTime.currentTimeMillis())
    }
  }

  def setPlayers(
      nplayer1: String,
      nplayer2: String,
      nplayer3: String,
      nplayer4: String
  ): MatchChicagoV1 = {
    val map = Map(
      player1 -> nplayer1,
      player2 -> nplayer2,
      player3 -> nplayer3,
      player4 -> nplayer4
    )
    val rs = rounds.map { r =>
      r.modifyPlayers(map)
    }
    copy(
      player1 = nplayer1,
      player2 = nplayer2,
      player3 = nplayer3,
      player4 = nplayer4,
      rounds = rs,
      updated = SystemTime.currentTimeMillis()
    )
  }

  def setGamesPerRound(ngamesPerRound: Int): MatchChicagoV1 =
    copy(
      gamesPerRound = ngamesPerRound,
      updated = SystemTime.currentTimeMillis()
    )

  def addHandToLastRound(h: Hand): MatchChicagoV1 = {
    val revrounds = rounds.reverse
    val last = revrounds.head
    val revbefore = revrounds.tail

    val nlast = last.addHand(h)
    val nrounds = (nlast :: revbefore).reverse
    copy(rounds = nrounds, updated = SystemTime.currentTimeMillis())
  }

  /**
    * modify an existing hand
    * @param ir - the round, the round must exist. values are 0, 1, ...
    * @param ih - the hand, if the hand doesn't exist, then it will addHandToLastRound. values are 0, 1, ...
    * @param h - the new hand
    */
  def modifyHand(ir: Int, ih: Int, h: Hand): MatchChicagoV1 = {
    val rs = rounds.toArray
    val round = rs(ir)
    val hs = round.hands.toArray
    if (ih < hs.length) {
      hs(ih) = h
      rs(ir) =
        round.copy(hands = hs.toList, updated = SystemTime.currentTimeMillis())
      copy(rounds = rs.toList, updated = SystemTime.currentTimeMillis())
    } else {
      addHandToLastRound(h)
    }
  }

  /**
    * Set the Id of this match
    * @param id the new ID of the match
    */
  def setId(id: MatchChicago.Id): MatchChicagoV1 = {
    copy(id = id, updated = SystemTime.currentTimeMillis())
  }

  def convertToCurrentVersion: (Boolean, MatchChicago) = {
    (
      false,
      MatchChicago(
        id,
        player1 :: player2 :: player3 :: player4 :: Nil,
        rounds,
        gamesPerRound,
        false,
        created,
        updated
      ).convertToCurrentVersion._2
    )
  }

  def readyForWrite: MatchChicagoV1 = this

}

object MatchChicagoV1 {
  def apply(
      id: MatchChicago.Id,
      player1: String,
      player2: String,
      player3: String,
      player4: String,
      rounds: List[Round],
      gamesPerRound: Int
  ) =
    new MatchChicagoV1(
      id,
      player1,
      player2,
      player3,
      player4,
      rounds,
      gamesPerRound,
      0,
      0
    )
}
