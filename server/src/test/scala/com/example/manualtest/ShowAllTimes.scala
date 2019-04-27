package com.example.manualtest

import utils.main.Main
import com.example.backend.resource.FileIO
import scala.language.postfixOps
import com.example.data.MatchChicago
import java.text.SimpleDateFormat
import com.example.data.Round
import com.example.data.Hand
import com.example.data.MatchDuplicate
import com.example.data.Board
import com.example.data.DuplicateHand
import com.example.data.MatchRubber
import com.example.data.RubberHand
import com.example.data.SystemTime.Timestamp
import com.example.backend.BridgeServiceFileStoreConverters
import com.example.backend.MatchChicagoCacheStoreSupport
import com.example.backend.MatchRubberCacheStoreSupport
import com.example.backend.MatchDuplicateCacheStoreSupport

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

  val sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS")

  def timestamp( msg: String, time: Timestamp ) = {
    if (time == 0) {
      println(msg)
    } else {
      println(msg+sdf.format(time.toLong))
    }
  }
}
