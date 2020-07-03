package com.github.thebridsk.bridge.data

import scala.annotation.meta._

import com.github.thebridsk.bridge.data.SystemTime.Timestamp
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema

/**
  * A match of chicago.
  * This can be either a 4 or 5 player chicago match with 4, 6, or 8 hands per round,
  * or a 5 player chicago match (quintet) with quick rotation (1 hand per round)
  *
  * @constructor
  * If playing quintet, then the gamesPerRound MUST be set when entering the names.
  *
  * @param id
  * @param players
  * @param rounds
  * @param gamesPerRound ==0 - unknown,
  *                      ==1 - quintet - quick rotation with 5 players
  *                      ==4, 6, or 8 - chicago with 4 or 5 players
  * @param created
  * @param updated
  */
@Schema(description = "A chicago match, version 2 (old version)")
case class MatchChicagoV2(
    @Schema(description = "The chicago ID", required = true)
    id: String,
    @ArraySchema(
      minItems = 4,
      uniqueItems = true,
      schema =
        new Schema(description = "A player", implementation = classOf[String])
    )
    players: List[String],
    @ArraySchema(
      minItems = 0,
      uniqueItems = true,
      schema =
        new Schema(description = "A round", implementation = classOf[Round])
    )
    rounds: List[Round],
    @Schema(
      description = "The number of games per round.",
      required = true,
      `type` = "enum",
      allowableValues = Array("0", "4", "6", "8")
    )
    gamesPerRound: Int,
    @Schema(description = "The creating date", required = true)
    created: Timestamp,
    @Schema(description = "The last update date", required = true)
    updated: Timestamp
) extends VersionedInstance[MatchChicago, MatchChicagoV2, String] {

  if (players.length < 4 || players.length > 5) {
    throw new IllegalArgumentException("Must have 4 or 5 players")
  }

  def setId(
      newId: String,
      forCreate: Boolean,
      dontUpdateTime: Boolean = false
  ) = {
    if (dontUpdateTime) {
      copy(id = newId)
    } else {
      val time = SystemTime.currentTimeMillis()
      copy(
        id = newId, /* created=if (forCreate) time; else created, */ updated =
          time
      )
    }
  }

  def copyForCreate(id: Id.MatchDuplicate) = {
    val time = SystemTime.currentTimeMillis()
    val xrounds = rounds.map { e =>
      e.copyForCreate(e.id)
    }.toList
    copy(id = id, created = time, updated = time, rounds = xrounds)

  }

  def addRound(r: Round) = {
    val n = copy(
      rounds = (r.copyForCreate(r.id) :: (rounds.reverse)).reverse,
      updated = SystemTime.currentTimeMillis()
    )
    n
  }

  def modifyRound(r: Round) = {
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

  /**
    * Change the player names.
    * @param nplayers the new player names.  Must specify the same number as in the players field.
    */
  def setPlayers(nplayers: String*): MatchChicagoV2 = {
    setPlayers(nplayers.toList)
  }

  /**
    * Change the player names.
    * @param nplayers the new player names.  Must specify the same number as in the players field.
    */
  def setPlayers(nplayers: List[String]): MatchChicagoV2 = {
    if (nplayers.length != players.length)
      throw new IllegalArgumentException(
        "Number of new player names must equal number of players"
      )
    val map = (players zip nplayers).toMap
    val rs = rounds.map { r =>
      r.modifyPlayers(map)
    }
    copy(
      players = nplayers,
      rounds = rs,
      updated = SystemTime.currentTimeMillis()
    )
  }

  def isConvertableToChicago5 = players.length == 4 && rounds.length < 2

  def playChicago5(extraPlayer: String) = {
    if (!isConvertableToChicago5)
      throw new IllegalArgumentException("Number of players must be 4")
    val np = players ::: List(extraPlayer)
    copy(players = np)
  }

  def setGamesPerRound(ngamesPerRound: Int) =
    copy(
      gamesPerRound = ngamesPerRound,
      updated = SystemTime.currentTimeMillis()
    )

  def addHandToLastRound(h: Hand) = {
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
  def modifyHand(ir: Int, ih: Int, h: Hand) = {
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
  def setId(id: String) = {
    copy(id = id, updated = SystemTime.currentTimeMillis())
  }

  /**
    * Is this a quintet match
    */
  def isQuintet() = {
    gamesPerRound == 1
  }

  /**
    * Start a match of quintet.
    * This can only be done if gamesPerRound is still 0 AND no rounds have been started.
    */
  def setQuintet() = {
    if (gamesPerRound != 0 || !rounds.isEmpty) this
    setGamesPerRound(1)
  }

  def convertToCurrentVersion = {
    (
      false,
      MatchChicago(id, players, rounds, gamesPerRound, false, created, updated)
        .convertToCurrentVersion
        ._2
    )
  }

  def readyForWrite = this

}

object MatchChicagoV2 {
  def apply(
      id: String,
      players: List[String],
      rounds: List[Round],
      gamesPerRound: Int
  ) = {
    val time = SystemTime.currentTimeMillis()
    new MatchChicagoV2(id, players, rounds, gamesPerRound, time, time)
  }
}
