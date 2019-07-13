package com.github.thebridsk.bridge.data

import scala.annotation.meta._

import com.github.thebridsk.bridge.data.SystemTime.Timestamp
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.Hidden

/**
  * @author werewolf
  */
@Schema(
  name = "MatchRubber",
  title = "MatchRubber - A rubber bridge match",
  description = "A rubber bridge match"
)
case class MatchRubberV1(
    @Schema(description = "The round ID", required = true)
    id: String,
    @Schema(description = "The north player", required = true)
    north: String,
    @Schema(description = "The south player", required = true)
    south: String,
    @Schema(description = "The east player", required = true)
    east: String,
    @Schema(description = "The west player", required = true)
    west: String,
    @Schema(
      description = "The first dealer",
      required = true,
      `type` = "enum",
      allowableValues = Array("N", "S", "E", "W")
    )
    dealerFirstHand: String,
    @ArraySchema(
      minItems = 0,
      uniqueItems = false,
      schema = new Schema(
        description = "A played hand",
        required = true,
        implementation = classOf[RubberHand]
      ),
      arraySchema = new Schema(
        description = "All the hands played in the rubber match.",
        required = true
      )
    )
    hands: List[RubberHand],
    @Schema(
      description =
        "When the match rubber was created, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    created: Timestamp,
    @Schema(
      description =
        "When the match rubber was last updated, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    updated: Timestamp,
    @Schema(
      description =
        "best match in main store when importing, never written to store",
      required = false,
      implementation = classOf[RubberBestMatch]
    )
    bestMatch: Option[RubberBestMatch] = None
) extends VersionedInstance[MatchRubberV1, MatchRubberV1, String] {

  def equalsIgnoreModifyTime(other: MatchRubberV1) = {
    other.id == id &&
    other.north == north &&
    other.south == south &&
    other.east == east &&
    other.west == west &&
    other.dealerFirstHand == dealerFirstHand &&
    (other.hands zip hands)
      .map(e => e._1.equalsIgnoreModifyTime(e._2))
      .find(b => !b)
      .isEmpty

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

  def copyForCreate(id: String) = {
    val time = SystemTime.currentTimeMillis()
    val xhands = hands.map { e =>
      e.copyForCreate(e.id)
    }.toList
    copy(id = id, created = time, updated = time, hands = xhands)

  }

  def setPlayers(north: String, south: String, east: String, west: String) =
    copy(
      north = north,
      south = south,
      east = east,
      west = west,
      updated = SystemTime.currentTimeMillis()
    )

  def gotAllPlayers() = {
    north != null && north != "" && south != null && south != "" && east != null && east != "" && west != null && west != ""
  }

  /**
    * @param pos the first dealer, N, S, E, W
    */
  @Hidden
  def setFirstDealer(pos: String) =
    copy(dealerFirstHand = pos, updated = SystemTime.currentTimeMillis())

  /**
    * Will set the id of the hand to the next ID.
    */
  def addHand(h: RubberHand) = {
    val hh = h.setId(hands.length.toString(), false)
    copy(
      hands = (hh :: (hands.reverse)).reverse,
      updated = SystemTime.currentTimeMillis()
    )
  }

  def getHand(id: String) = {
    hands.find { h =>
      h.id == id
    }
  }

  def setHands(newhands: Map[String, RubberHand]) = {
    val nhs = newhands.values.toList.sortBy(h => h.id.toInt)
    copy(hands = nhs)
  }

  def updateHand(nh: RubberHand) = {
    val newhs = hands.map { h =>
      if (h.id == nh.id) nh
      else h
    }
    copy(hands = newhs)
  }

  def deleteHand(hid: String) = {
    val last = hands.length - 1
    if (hid.toInt != last) {
      throw new IllegalArgumentException(
        s"Trying to delete $hid from $id, can only delete last hand: $last"
      )
    }
    copy(hands = hands.take(last))
  }

  def partnerOf(p: String) = p match {
    case `north` => south
    case `south` => north
    case `east`  => west
    case `west`  => east
    case _       => null
  }

  /**
    * The hand with the same id as the specified hand will get updated
    * If the id is not in hands, then hands will not be updated, but the updated time will be changed.
    */
  def modifyHand(h: RubberHand) = {
    val hs = hands.map { hh =>
      if (hh.id == h.id) h else hh
    }
    copy(hands = hs, updated = SystemTime.currentTimeMillis())
  }

  /**
    * Modify the player names according to the specified name map.
    * The timestamp is not changed.
    */
  def modifyPlayers(nameMap: Map[String, String]) = {

    def getName(p: String) = nameMap.get(p).getOrElse(p)

    val n = getName(north)
    val s = getName(south)
    val e = getName(east)
    val w = getName(west)

    if (n.equals(north) && s.equals(south) && e.equals(east) && w.equals(west)) {
      None
    } else {
      Some(copy(north = n, south = s, east = e, west = w))
    }
  }

  def convertToCurrentVersion() = (true, this)

  def readyForWrite() = copy(bestMatch = None)

  def addBestMatch(bm: RubberBestMatch) = copy(bestMatch = Option(bm))
}

object MatchRubberV1 {
  def apply(
      id: String,
      north: String,
      south: String,
      east: String,
      west: String,
      dealerFirstHand: String,
      hands: List[RubberHand]
  ) = {
    val time = SystemTime.currentTimeMillis()
    new MatchRubberV1(
      id,
      north,
      south,
      east,
      west,
      dealerFirstHand,
      hands,
      time,
      time
    )
  }
}

@Schema(
  title = "RubberBestMatch - The best match in the main store",
  description = "The best match in the main store."
)
case class RubberBestMatch(
    @Schema(description = "How similar the matches are", required = true)
    sameness: Double,
    @Schema(
      description =
        "The ID of the MatchRubber in the main store that is the best match, none if no match",
      required = true,
      `type` = "string"
    )
    id: Option[String],
    @ArraySchema(
      schema = new Schema(
        `type` = "string",
        description = "A field that is different"
      ),
      arraySchema = new Schema(
        description = "The fields that are different",
        required = true
      )
    )
    differences: Option[List[String]]
) {

  def determineDifferences(l: List[String]) = {
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

  def htmlTitle = {
    differences.map { l =>
      if (l.isEmpty) "Same"
      else determineDifferences(l).mkString("Differences:\n", "\n", "")
    }
  }

}

object RubberBestMatch {

  def noMatch = new RubberBestMatch(-1, None, None)

  def apply(id: String, diff: Difference) = {
    new RubberBestMatch(diff.percentSame, Some(id), Some(diff.differences))
  }
}
