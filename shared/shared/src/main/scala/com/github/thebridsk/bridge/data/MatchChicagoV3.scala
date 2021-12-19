package com.github.thebridsk.bridge.data

import SystemTime.Timestamp
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
  * @param simpleRotation ==true - simple rotation, last dealer swaps with sitting out
  *                       ==false - fair rotation, sitting out swaps with person that has not sat out yet,
  *                                                left of incoming stays put, right and partner of incoming swap
  * @param created
  * @param updated
  */
@Schema(
  name = "MatchChicago",
  title = "MatchChicago - A chicago match",
  description = "A chicago match, version 3 (current version)"
)
case class MatchChicagoV3(
    @Schema(description = "The chicago ID", required = true)
    id: MatchChicago.Id,
    @ArraySchema(
      minItems = 4,
      uniqueItems = true,
      schema =
        new Schema(description = "A player", implementation = classOf[String]),
      arraySchema =
        new Schema(description = "All the players.", required = true)
    )
    players: List[String],
    @ArraySchema(
      minItems = 0,
      uniqueItems = true,
      schema =
        new Schema(description = "A round", implementation = classOf[Round]),
      arraySchema = new Schema(description = "All the rounds.", required = true)
    )
    rounds: List[Round],
    @Schema(
      description =
        "The number of games per round.  1 indicates fast rotation.  0 indicates normal rotation but number of hands in round has not been determined.",
      required = true,
      `type` = "enum",
      allowableValues = Array("0", "1", "4", "6", "8")
    )
    gamesPerRound: Int,
    @Schema(description = "Use simple rotation.", required = true)
    simpleRotation: Boolean,
    @Schema(
      description = "The creating date, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    created: Timestamp,
    @Schema(
      description = "The last update date, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    updated: Timestamp,
    @Schema(
      description =
        "best match in main store when importing, never written to store",
      required = false,
      implementation = classOf[ChicagoBestMatch]
    )
    bestMatch: Option[ChicagoBestMatch] = None
) extends VersionedInstance[MatchChicago, MatchChicagoV3, MatchChicago.Id] {

  if (players.length < 4 || players.length > 5) {
    throw new IllegalArgumentException("Must have 4 or 5 players")
  }

  @Schema(hidden = true)
  def setId(
      newId: MatchChicago.Id,
      forCreate: Boolean,
      dontUpdateTime: Boolean = false
  ): MatchChicagoV3 = {
    if (dontUpdateTime) {
      copy(id = newId)
    } else {
      val time = SystemTime.currentTimeMillis()
      copy(
        id = newId,
        // created=if (forCreate) time; else created,
        updated = time
      )
    }
  }

  def copyForCreate(id: MatchChicago.Id): MatchChicagoV3 = {
    val time = SystemTime.currentTimeMillis()
    val xrounds = rounds.map { e =>
      e.copyForCreate(e.id)
    }.toList
    copy(id = id, created = time, updated = time, rounds = xrounds)

  }

  def getRound(id: String): Option[Round] = {
    rounds.find(r => r.id == id)
  }

  def addRound(r: Round): MatchChicagoV3 = {
    if (r.id.toInt != rounds.length) {
      throw new IllegalArgumentException(
        s"Can only add next round, ${rounds.length}, trying to add ${r.id}"
      )
    }
    val n = copy(
      rounds = rounds ::: List(r.copyForCreate(r.id)),
      updated = SystemTime.currentTimeMillis()
    )
    n
  }

  def setRounds(rounds: Map[String, Round]): MatchChicago = {
    val rs = rounds.values.toList.sortBy(r => r.id.toInt)
    copy(
      rounds = rs,
      updated = SystemTime.currentTimeMillis()
    )
  }

  def updateRound(round: Round): MatchChicago = {
    var found = false
    val newrs = rounds.map { r =>
      if (r.id == round.id) {
        found = true;
        round
      } else {
        r
      }
    }
    if (found) {
      copy(
        rounds = newrs,
        updated = SystemTime.currentTimeMillis()
      )
    } else {
      throw new IllegalArgumentException(
        s"Round ${round.id} not found in match ${id}"
      )
    }
  }

  def deleteRound(id: String): MatchChicagoV3 = {
    val last = rounds.length - 1
    if (id.toInt != last) {
      throw new IllegalArgumentException(
        s"Can only delete last round, ${last}, trying to delete ${id}"
      )
    }
    val newrs = rounds.take(last)
    copy(
      rounds = newrs,
      updated = SystemTime.currentTimeMillis()
    )
  }

  def modifyRound(r: Round): MatchChicagoV3 = {
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
  def setPlayers(nplayers: String*): MatchChicagoV3 = {
    setPlayersList(nplayers.toList)
  }

  def hasPlayStarted: Boolean = {
    rounds.length > 1 || rounds.headOption
      .map(r => !r.hands.isEmpty)
      .getOrElse(false)
  }

  /**
    * Change the player names.
    * @param nplayers the new player names.  Must specify the same number as in the players field.
    */
  @Schema(hidden = true)
  def setPlayersList(nplayers: List[String]): MatchChicagoV3 = {
    if (nplayers.length != players.length && hasPlayStarted)
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

  /**
    * Modify the player names according to the specified name map.
    * The timestamp is not changed.
    */
  def modifyPlayers(nameMap: Map[String, String]): Option[MatchChicagoV3] = {

    def getName(n: String) = nameMap.get(n).getOrElse(n)

    val (rs, modified) = rounds
      .map { t =>
        t.modifyPlayersNoTime(nameMap) match {
          case Some(nt) => (nt, true)
          case None     => (t, false)
        }
      }
      .foldLeft((List[Round](), false)) { (ac, v) =>
        (ac._1 ::: List(v._1), ac._2 || v._2)
      }
    val (nps, pmodified) = players
      .map { p =>
        val np = getName(p)
        (np, !np.equals(p))
      }
      .foldLeft((List[String](), false)) { (ac, v) =>
        (ac._1 ::: List(v._1), ac._2 || v._2)
      }
    if (modified || pmodified) {
      Some(copy(
        rounds = rs,
        players = nps,
        updated = SystemTime.currentTimeMillis()
      ))
    } else {
      None
    }

  }

  @Schema(hidden = true)
  def isConvertableToChicago5: Boolean =
    players.length == 4 && rounds.length < 2

  def playChicago5(extraPlayer: String): MatchChicagoV3 = {
    if (!isConvertableToChicago5)
      throw new IllegalArgumentException("Number of players must be 4")
    val np = players ::: List(extraPlayer)
    copy(
      players = np,
      updated = SystemTime.currentTimeMillis()
    )
  }

  def setGamesPerRound(ngamesPerRound: Int): MatchChicagoV3 =
    copy(
      gamesPerRound = ngamesPerRound,
      updated = SystemTime.currentTimeMillis()
    )

  def addHandToLastRound(h: Hand): MatchChicagoV3 = {
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
  def modifyHand(ir: Int, ih: Int, h: Hand): MatchChicagoV3 = {
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
  def setId(id: MatchChicago.Id): MatchChicagoV3 = {
    copy(id = id, updated = SystemTime.currentTimeMillis())
  }

  /**
    * Is this a quintet match
    */
  @Schema(hidden = true)
  def isQuintet(): Boolean = {
    gamesPerRound == 1
  }

  /**
    * Start a match of quintet.
    * This can only be done if gamesPerRound is still 0 AND no rounds have been started.
    */
  @Schema(hidden = true)
  def setQuintet(simple: Boolean): MatchChicagoV3 = {
    if (gamesPerRound != 0 || !rounds.isEmpty) this
    setGamesPerRound(1).copy(
      simpleRotation = simple,
      updated = SystemTime.currentTimeMillis()
    )
  }

  def convertToCurrentVersion: (Boolean, MatchChicago) = {
    val (isNew, rs) = {
      rounds
        .map { r =>
          val (isNewV, hands) =
            r.hands.zipWithIndex
              .map { entry =>
                val (h, i) = entry
                if (h.id != i.toString()) {
                  (false, h.copy(id = i.toString()))
                } else {
                  (true, h)
                }
              }
              .foldLeft((true, List[Hand]())) { (ac, v) =>
                (ac._1 && v._1, ac._2 ::: List(v._2))
              }
          if (isNewV) (isNewV, r)
          else (isNewV, r.copy(hands = hands))
        }
        .foldLeft((true, List[Round]())) { (ac, v) =>
          (ac._1 && v._1, ac._2 ::: List(v._2))
        }
    }

    (isNew, if (isNew) this else copy(rounds = rs))
  }

  def readyForWrite: MatchChicagoV3 = copy(bestMatch = None)

  def addBestMatch(bm: ChicagoBestMatch): MatchChicagoV3 =
    copy(bestMatch = Option(bm))

}

trait IdMatchChicago

object MatchChicagoV3 extends HasId[IdMatchChicago]("C") {
  def apply(
      id: MatchChicago.Id,
      players: List[String],
      rounds: List[Round],
      gamesPerRound: Int,
      simpleRotation: Boolean
  ): MatchChicagoV3 = {
    val time = SystemTime.currentTimeMillis()
    new MatchChicagoV3(
      id,
      players,
      rounds,
      gamesPerRound,
      simpleRotation,
      time,
      time
    )
  }

}

@Schema(
  title = "ChicagoBestMatch - The best match in the main store",
  description = "The best match in the main store"
)
case class ChicagoBestMatch(
    @Schema(
      title = "How similar the matches are.",
      description = "The percentage of fields that are the same.",
      required = true
    )
    sameness: Double,
    @Schema(
      title = "The ID of the best match.",
      description =
        "The ID of the MatchChicago in the main store that is the best match, none if no match",
      required = true
    )
    id: Option[MatchChicago.Id],
    @ArraySchema(
      minItems = 0,
      uniqueItems = true,
      schema = new Schema(
        `type` = "string",
        description = "A field that is different"
      ),
      arraySchema =
        new Schema(description = "All the different fields.", required = false)
    )
    differences: Option[List[String]]
) {

  def determineDifferences(l: List[String]): List[String] = {
    val list = l
      .map { s =>
        val i = s.lastIndexOf(".")
        if (i < 0) ("", s)
        else (s.substring(0, i), s.substring(i + 1))
      }
      .foldLeft(Map[String, List[String]]()) { (ac, v) =>
        val (prefix, key) = v
        val cur = ac.get(prefix).getOrElse(List())
        val next = key :: cur
        val r = ac + (prefix -> next)
        r
      }

//    list.map{ s =>
//      val parts = s.split('.')
//      if (parts.isEmpty) s
//      else parts.head
//    }.distinct
    list
      .map { entry =>
        val (prefix, vkeys) = entry
        val keys = vkeys.sorted
        if (prefix == "") s"""${keys.mkString(" ")}"""
        else s"""${prefix} ${keys.mkString(" ")}"""
      }
      .toList
      .sorted
  }

  def htmlTitle: Option[String] = {
    differences.map { l =>
      if (l.isEmpty) "Same"
      else determineDifferences(l).mkString("Differences:\n", "\n", "")
    }
  }
}

object ChicagoBestMatch {

  def noMatch = new ChicagoBestMatch(-1, None, None)

  def apply(id: MatchChicago.Id, diff: Difference): ChicagoBestMatch = {
    new ChicagoBestMatch(diff.percentSame, Some(id), Some(diff.differences))
  }
}
