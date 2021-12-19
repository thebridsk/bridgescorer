package com.github.thebridsk.bridge.server.manualtest

import com.github.thebridsk.utilities.main.Main
import com.github.thebridsk.utilities.file.FileIO
import scala.language.postfixOps
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.Round
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.data.RubberHand
import com.github.thebridsk.utilities.time.SystemTime.Timestamp
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStoreConverters
import com.github.thebridsk.bridge.server.backend.MatchChicagoCacheStoreSupport
import com.github.thebridsk.bridge.server.backend.MatchRubberCacheStoreSupport
import com.github.thebridsk.bridge.server.backend.MatchDuplicateCacheStoreSupport
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId
import org.rogach.scallop.ScallopOption
import com.github.thebridsk.utilities.main.MainConf

class ShowAllTimesConf extends MainConf {

  val optionFilename: ScallopOption[String] = trailArg[String](
    "filename",
    "The match filename",
    required = true,
    default = None,
    hidden = false
  )

}

object ShowAllTimes extends Main[ShowAllTimesConf] {

  val converters = new BridgeServiceFileStoreConverters(true)
  import converters._

  import config._

  def execute(): Int = {

    val json =
      try {
        FileIO.readFileSafe(optionFilename())
      } catch {
        case x: Exception =>
          println("Unable to read file")
          throw x
      }

    val patternChicago = """.*/MatchChicago\.[^/]*""" r
    val patternDuplicate = """.*/MatchDuplicate\.[^/]*""" r
    val patternRubber = """.*/MatchRubber\.[^/]*""" r

    optionFilename().replace('\\', '/') match {
      case patternChicago() =>
        val (good, chi) = new MatchChicagoCacheStoreSupport(true).fromJSON(json)
        showTimestamps(chi)
      case patternDuplicate() =>
        val (good, dup) =
          new MatchDuplicateCacheStoreSupport(true).fromJSON(json)
        showTimestamps(dup)
      case patternRubber() =>
        val (good, rub) = new MatchRubberCacheStoreSupport(true).fromJSON(json)
        showTimestamps(rub)
      case _ =>
        println("Can't determine type of match from " + optionFilename())
    }

    0
  }

  def showTimestamps(rub: MatchRubber): Unit = {
    println("Rubber " + rub.id)
    timestamp("  created: ", rub.created)
    rub.hands.foreach(h => showRubberHand(h))
    timestamp("  updated: ", rub.updated)
  }

  def showRubberHand(h: RubberHand): Unit = {
    timestamp(s"    ${h.id} created: ", h.created)
    timestamp(s"    ${h.id} updated: ", h.updated)
  }

  def showTimestamps(dup: MatchDuplicate): Unit = {
    println("Duplicate " + dup.id)
    timestamp("  created: ", dup.created)
    dup.boards.foreach(b => showDuplicateBoard(b))
    timestamp("  updated: ", dup.updated)
  }

  def showDuplicateBoard(b: Board): Unit = {
    timestamp(s"  ${b.id} created: ", b.created)
    b.hands.foreach(h => showDuplicateHand(h))
    timestamp(s"  ${b.id} updated: ", b.updated)
  }

  def showDuplicateHand(h: DuplicateHand): Unit = {
    timestamp(s"    ${h.id} created: ", h.created)
    timestamp(s"    ${h.id} updated: ", h.updated)
  }

  def showTimestamps(chi: MatchChicago): Unit = {
    println("Chicago " + chi.id)
    timestamp("  created: ", chi.created)
    chi.rounds.foreach(r => showChicagoRound(r))
    timestamp("  updated: ", chi.updated)
  }

  def showChicagoRound(r: Round): Unit = {
    timestamp(s"  ${r.id} created: ", r.created)
    r.hands.foreach(h => showChicagoHand(h))
    timestamp(s"  ${r.id} updated: ", r.updated)
  }

  def showChicagoHand(h: Hand): Unit = {
    timestamp(s"    ${h.id} created: ", h.created)
    timestamp(s"    ${h.id} updated: ", h.updated)
  }

  val sdf: DateTimeFormatter = DateTimeFormatter
    .ofPattern("MM/dd/yyyy HH:mm:ss.SSS")
    .withZone(ZoneId.systemDefault())

  def timestamp(msg: String, time: Timestamp): Unit = {
    if (time == 0) {
      println(msg)
    } else {
      println(msg + sdf.format(Instant.ofEpochMilli(time.toLong)))
    }
  }
}
