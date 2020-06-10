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
import com.github.thebridsk.bridge.data.SystemTime.Timestamp
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStoreConverters
import com.github.thebridsk.bridge.server.backend.MatchChicagoCacheStoreSupport
import com.github.thebridsk.bridge.server.backend.MatchRubberCacheStoreSupport
import com.github.thebridsk.bridge.server.backend.MatchDuplicateCacheStoreSupport
import java.time.format.DateTimeFormatter
import java.time.Instant

object ShowAllTimes extends Main {

  val optionFilename = trailArg[String]("filename", "The match filename", required=true, default=None, hidden=false)

  val converters = new BridgeServiceFileStoreConverters(true)
  import converters._

  def execute(): Int = {

    val json = try {
      FileIO.readFileSafe(optionFilename())
    } catch {
      case x: Exception =>
        println("Unable to read file")
        throw x
    }

    val patternChicago = """.*/MatchChicago\.[^/]*"""r
    val patternDuplicate = """.*/MatchDuplicate\.[^/]*"""r
    val patternRubber = """.*/MatchRubber\.[^/]*"""r

    optionFilename().replace('\\', '/') match {
      case patternChicago() =>
        val (good,chi) = new MatchChicagoCacheStoreSupport(true).fromJSON(json)
        showTimestamps(chi)
      case patternDuplicate() =>
        val (good,dup) = new MatchDuplicateCacheStoreSupport(true).fromJSON(json)
        showTimestamps(dup)
      case patternRubber() =>
        val (good,rub) = new MatchRubberCacheStoreSupport(true).fromJSON(json)
        showTimestamps(rub)
      case _ =>
        println("Can't determine type of match from "+optionFilename())
    }

    0
  }

  def showTimestamps( rub: MatchRubber ) = {
    println("Rubber "+rub.id)
    timestamp("  created: ",rub.created)
    rub.hands.foreach(h => showRubberHand(h))
    timestamp("  updated: ",rub.updated)
  }

  def showRubberHand(h: RubberHand) = {
    timestamp(s"    ${h.id} created: ",h.created)
    timestamp(s"    ${h.id} updated: ",h.updated)
  }

  def showTimestamps( dup: MatchDuplicate ) = {
    println("Duplicate "+dup.id)
    timestamp("  created: ",dup.created)
    dup.boards.foreach(b => showDuplicateBoard(b))
    timestamp("  updated: ",dup.updated)
  }

  def showDuplicateBoard(b: Board) = {
    timestamp(s"  ${b.id} created: ",b.created)
    b.hands.foreach(h=>showDuplicateHand(h))
    timestamp(s"  ${b.id} updated: ",b.updated)
  }

  def showDuplicateHand(h: DuplicateHand) = {
    timestamp(s"    ${h.id} created: ",h.created)
    timestamp(s"    ${h.id} updated: ",h.updated)
  }

  def showTimestamps( chi: MatchChicago ) = {
    println("Chicago "+chi.id)
    timestamp("  created: ",chi.created)
    chi.rounds.foreach(r => showChicagoRound(r))
    timestamp("  updated: ",chi.updated)
  }

  def showChicagoRound( r: Round ) = {
    timestamp(s"  ${r.id} created: ", r.created)
    r.hands.foreach(h=>showChicagoHand(h))
    timestamp(s"  ${r.id} updated: ", r.updated)
  }

  def showChicagoHand( h: Hand ) = {
    timestamp(s"    ${h.id} created: ", h.created)
    timestamp(s"    ${h.id} updated: ", h.updated)
  }

  val sdf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss.SSS")

  def timestamp( msg: String, time: Timestamp ) = {
    if (time == 0) {
      println(msg)
    } else {
      println(msg+sdf.format( Instant.ofEpochMilli(time.toLong)))
    }
  }
}
