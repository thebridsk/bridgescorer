package com.example.manualtest

import utils.main.Main
import scala.reflect.io.Path
import com.example.backend.BridgeServiceFileStore
import utils.logging.Logger
import com.example.data.DuplicateSummary
import com.example.data.duplicate.suggestion.DuplicateSuggestionsCalculation
import com.example.data.duplicate.suggestion.PairsData
import com.example.data.duplicate.suggestion.PairData
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object ShowSuggestions extends Main {

  import utils.main.Converters._

  val log = Logger( ShowSuggestions.getClass.getName )

  val cmdName = {
        val x = ShowSuggestions.getClass.getName
        "scala "+x.substring(0, x.length()-1)
  }

  banner(s"""
Show the suggested pairings

Syntax:
  ${cmdName} [options] names...
Options:""")

  val optionStore = opt[Path]("store",
                              short='s',
                              descr="The store directory, default=./store",
                              argName="dir",
                              default=Some("./store"))

  val optionNeverPair = opt[List[String]]("neverpair",
                                  short='n',
                                  descr="never pair players.  Value is player1,player2",
                                  argName="pair",
                                  default=None)

  val names = trailArg[List[String]]( name="names",
                                      required=true,
                                      descr="the players that are playing next game")

  def namesFilter(n: List[String])( pd: PairData ) = {
    n.contains(pd.player1) && n.contains(pd.player2)
  }

  val patternPair = """([^,]+),([^,]+)""".r

  def execute(): Int = {
    val storedir = optionStore().toDirectory
    log.info(s"Using datastore ${storedir}")
    val datastore = new BridgeServiceFileStore( storedir )

    Await.result( datastore.getDuplicateSummaries(), 30.seconds) match {
      case Right( summaries ) =>
        val n = names.getOrElse(List() ).take(8)
        val neverPair = optionNeverPair.toOption.map( lnp => lnp.flatMap { pair =>
          pair match {
            case patternPair(p1,p2) =>
              (p1,p2)::Nil
            case _ =>
              log.severe(s"""Never pair option not valid, ignoring: ${pair}""")
              Nil
          }
        })
        showSuggestions( summaries, n, neverPair )
        val pairsData = new PairsData(summaries)
        showPairMatrix(pairsData, names.toOption)
        val lpd = pairsData.data.values.filter(namesFilter(names.getOrElse(List() ))).toList
        showPairData( lpd.sortWith((l,r) => l.played<r.played), "Sorted by Win Percentage" )
        showPairData( lpd.sortWith((l,r) => l.winPercent>r.winPercent), "Sorted by Win Percentage" )
        showPairData( lpd.sortWith((l,r) => l.pointsPercent>r.pointsPercent), "Sorted by Points Percentage" )
        0
      case Left((status,msg)) =>
        log.severe(s"Error getting duplicate summary: ${status} ${msg}")
        1
    }

  }

  def showPairData( lpd: List[PairData], msg: String ) = {
    log.info(
      lpd.map { pd =>
        f"""${pd.player1}%10s ${pd.player2}%10s: ${pd.played}%2d ${pd.pointsPercent}%.2f ${pd.winPercent}%.2f"""
      }.mkString(s"${msg}:\n  ","\n  ","")
    )
  }

  def showPairMatrix( pd: PairsData, nn: Option[List[String]] = None ) = {
    val players = pd.players.filter( p => nn.map(l => l.contains(p)).getOrElse(true))
    log.info( s"           ${players.map(s=>f"${"          ".take((21-s.length())/2)+s}%-21s").mkString(" ")}")
    for ( r <- players) {
      log.info(
        (for (c <- players ) yield {
          if (r == c) "          x  "
          else {
            pd.get(r, c) match {
              case Some(d) =>
                val per = d.pointsPercent
                val perw = d.winPercent
                f"""(${d.played}%2d-$perw%6.2f%%-$per%6.2f%%)"""
              case None =>
                "          -  "
            }
          }
        }).map(s => f"${s}%-21s").mkString(f"${r}%10s "," ","")
      )
    }
  }

  def showSuggestions( summaries: List[DuplicateSummary], n: List[String], neverPair: Option[List[(String,String)]] ) = {
    val ds = new DuplicateSuggestionsCalculation( summaries, n, neverPair )
    log.info(s"Suggestions:")
    ds.suggest.zipWithIndex.foreach { e =>
      val (sug,i) = e
      val s = sug.players.sortWith { (l,r) =>
                            if (l.lastPlayed==r.lastPlayed) l.timesPlayed<r.timesPlayed
                            else l.lastPlayed<r.lastPlayed
                          }.map( p => f"${p.player1}%8s-${p.player2}%-8s (${p.lastPlayed}%2d,${p.timesPlayed}%2d)").mkString(", ")
      log.info(f"  ${i}%3d: ${s}%s")
    }
  }
}
