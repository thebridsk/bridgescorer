package com.github.thebridsk.bridge.data


import com.github.thebridsk.bridge.data.SystemTime.Timestamp
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema

/**
  * @author werewolf
  */
@Schema(
  title = "Round - A round in a chicago match",
  description = "A round in a chicago match"
)
case class Round(
    @Schema(description = "The round ID", required = true)
    id: String,
    @Schema(description = "The north player for the round", required = true)
    north: String,
    @Schema(description = "The south player for the round", required = true)
    south: String,
    @Schema(description = "The east player for the round", required = true)
    east: String,
    @Schema(description = "The west player for the round", required = true)
    west: String,
    @Schema(
      description = "The first dealer",
      required = true,
      `type` = "enum",
      allowableValues = Array("N", "S", "E", "W")
    )
    dealerFirstRound: String,
    @ArraySchema(
      minItems = 0,
      schema = new Schema(implementation = classOf[Hand]),
      uniqueItems = false,
      arraySchema = new Schema(
        description = "The played hands in the round.",
        required = true
      )
    )
    hands: List[Hand],
    @Schema(
      description =
        "When the duplicate hand was created, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    created: Timestamp,
    @Schema(
      description =
        "When the duplicate hand was last updated, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    updated: Timestamp
) {

  def setId(newId: String, forCreate: Boolean): Round = {
    val time = SystemTime.currentTimeMillis()
    copy(
      id = newId,
      created = if (forCreate) time; else created,
      updated = time
    )
  }

  def copyForCreate(id: String): Round = {
    val time = SystemTime.currentTimeMillis()
    val xhands = hands.map { e =>
      e.copyForCreate(e.id)
    }.toList
    copy(id = id, created = time, updated = time, hands = xhands)

  }

  def getHand(id: String): Option[Hand] = hands.find(h => h.id == id)

  def addHand(h: Hand): Round = {
    if (h.id.toInt == hands.length) {
      copy(hands = hands ::: List(h), updated = SystemTime.currentTimeMillis())
    } else {
      throw new IllegalArgumentException(
        s"Trying to add hand ${h} to round ${id}, with ${hands.length} hands"
      )
    }
  }

  def setHands(hs: Map[String, Hand]): Round = {
    val newh = hs.values.toList.sortBy(h => h.id.toInt)
    copy(hands = newh)
  }

  /**
    * Updates a hand.  If the hand does not exist in the round
    * @param h
    * @throws IllegalArgumentException if hand does not exist already.
    */
  def updateHand(h: Hand): Round = {
    var found = false
    val newh = hands.map(
      hh =>
        if (hh.id == h.id) {
          found = true; h
        } else hh
    )
    if (found) {
      copy(hands = newh)
    } else {
      throw new IllegalArgumentException(
        s"Update not allowed, hand ${h.id} in round ${id}, with ${hands.length} hands"
      )
    }
  }

  def deleteHand(hid: String): Round = {
    val last = hands.length - 1
    if (hid.toInt == last) {
      copy(hands = hands.take(last), updated = SystemTime.currentTimeMillis())
    } else {
      throw new IllegalArgumentException(
        s"Trying to delete hand ${hid} to round ${id}, with ${hands.length} hands"
      )
    }
  }

  def partnerOf(p: String): String = p match {
    case `north` => south
    case `south` => north
    case `east`  => west
    case `west`  => east
    case _       => null
  }

  def modifyPlayersNoTime(map: Map[String, String]): Option[Round] = {
    val n = map.get(north).getOrElse(north)
    val s = map.get(south).getOrElse(south)
    val e = map.get(east).getOrElse(east)
    val w = map.get(west).getOrElse(west)
    if (n.equals(north) && s.equals(south) && e.equals(east) && w.equals(west)) {
      None
    } else {
      Some(copy(north = n, south = s, east = e, west = w))
    }

  }

  def modifyPlayers(map: Map[String, String]): Round = {
    val n = map.get(north).getOrElse(north)
    val s = map.get(south).getOrElse(south)
    val e = map.get(east).getOrElse(east)
    val w = map.get(west).getOrElse(west)
    copy(
      north = n,
      south = s,
      east = e,
      west = w,
      updated = SystemTime.currentTimeMillis()
    )
  }

  def players: List[String] = north :: south :: east :: west :: Nil
}

object Round {
  def create(
      id: String,
      north: String,
      south: String,
      east: String,
      west: String,
      dealerFirstRound: String,
      hands: List[Hand]
  ): Round =
    new Round(id, north, south, east, west, dealerFirstRound, hands, 0, 0)
      .copyForCreate(id)

}
