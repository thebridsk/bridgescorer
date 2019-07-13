package com.github.thebridsk.bridge.manualtest

import com.github.thebridsk.bridge.data.duplicate.suggestion.Suggestion
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.DuplicateSummaryEntry
import com.github.thebridsk.bridge.data.duplicate.suggestion.DuplicateSuggestionsCalculation
import com.github.thebridsk.utilities.main.Main
import com.github.thebridsk.utilities.logging.Logger
import scala.reflect.io.Path
import com.github.thebridsk.bridge.backend.BridgeServiceFileStore
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import java.net.URLClassLoader
import java.io.File
import com.github.thebridsk.bridge.data.duplicate.suggestion.DuplicateSuggestions
import com.github.thebridsk.bridge.data.duplicate.suggestion.NeverPair

object Suggestion200 extends Main {

  val log = Logger(Suggestion200.getClass.getName)

  val defaultPlayers = List("A","B","C","D","E","F","G","H")

  val playedMatches = collection.mutable.ArrayBuffer[DuplicateSummary]()

  import com.github.thebridsk.utilities.main.Converters._

  val cmdName = {
    ((getClass.getClassLoader match {
      case loader: URLClassLoader =>
        // This doesn't work anymore.  In Java 9 with the modules classloader, the URLClassLoader is not used as
        // the application loader.  The application loader is now an internal JDK class and can't be inspected.
        val urls = loader.getURLs
        if (urls.size != 1) {
          logger.fine("Must have only one jar file in classpath")
          None
        } else {
          val url = urls(0)
          Some(url)
        }
      case _ =>
        logger.fine("Unable to determine the jar file")
        None
    }) match {
      case Some(url) =>
        if (url.getProtocol == "file") {
          Some( new File(url.getFile).getName )
        } else {
          None
        }
      case None => None
    }) match {
      case Some(jarname) =>
        "java -jar "+jarname
      case None =>
        val x = Suggestion200.getClass.getName
        "scala "+x.substring(0, x.length()-1)
    }
  }

  banner(s"""
Will run a number of rounds to show the suggestions

Syntax:
  ${cmdName} options
Options:""")

  val optionStore = opt[Path]("store",
                              short='s',
                              descr="The store directory, default is none",
                              argName="dir",
                              default=None)
  val optionRounds = opt[Int]("rounds",
                              short='r',
                              descr="The number of 105 match rounds to evaluate, default is 2",
                              argName="rounds",
                              default=Some(2))

  val optionNeverPair = opt[List[String]]("neverpair",
                                  short='n',
                                  descr="never pair players.  Value is player1,player2",
                                  argName="pair",
                                  default=None)

  val optionNames = trailArg[List[String]]("names",
                                           descr=s"The 8 names to use, default ${defaultPlayers.mkString(",")}",
                                           validate= _.length==8,
                                           required=false,
                                           default=Some(defaultPlayers))

  val patternPair = """([^,]+),([^,]+)""".r

  def execute() = {
    val neverPair = optionNeverPair.toOption.map( lnp => lnp.flatMap { pair =>
      pair match {
        case patternPair(p1,p2) =>
          NeverPair(p1,p2)::Nil
        case _ =>
          log.severe(s"""Never pair option not valid, ignoring: ${pair}""")
          Nil
      }
    })

    val duplicateSummary = optionStore.toOption.map { dir =>
      log.info(s"Using datastore ${dir}")
      val datastore = new BridgeServiceFileStore( dir.toDirectory )
      Await.result( datastore.getDuplicateSummaries(), 30.seconds ) match {
        case Right(ds) => ds
        case Left(error) =>
          log.warning(s"Error getting duplicate summaries from store ${dir}, ignoring: ${error}")
          List()
      }
    }.getOrElse(List())

    val initMatches = duplicateSummary.length
    log.info(s"Starting with ${initMatches} matches")
    val players = optionNames().sorted
    log.info(s"Using players ${players.mkString(", ")}")

    playedMatches ++= duplicateSummary.sortWith{ (l,r) => l.created<r.created }.zipWithIndex.map { e =>
      val (ds,i) = e
      ds.copy( created=i, updated=i)
    }

    val times = for ( i <- 1+initMatches to 105*optionRounds()) yield {
      val (ds,calcTime) = nextSuggestion(i,players,neverPair)
      playedMatches += ds
      (i,calcTime)
    }
    analyze(players, duplicateSummary.length)
    log.info(s"""Calculation times:\nmatch,timeInNanos${times.map(e=>e._1.toString()+","+e._2).mkString("\n","\n","")}""")
    0
  }

  def toString( s: Suggestion ) = {
    val x = s.players.sortWith{ (l,r) =>
      val lastPlayed = l.lastPlayed.compare(r.lastPlayed)
      if (lastPlayed == 0) {
        val timesPlayed = l.timesPlayed.compare(r.timesPlayed)
        timesPlayed < 0
      } else {
        lastPlayed < 0
      }
    }.map( p => f"${p.player1}-${p.player2} (${p.lastPlayed}%2d ${p.timesPlayed}%2d)")
    x.mkString("",", ",s"  lastPlayedAllTeams=${s.lastPlayedAllTeams}")
  }

  def toStringDetail( s: Suggestion ) = {
    val x = s.players.map( p => f"${p.player1}-${p.player2} (${p.lastPlayed}%2d ${p.timesPlayed}%2d)")
    val wghts = s.weights.map { v => f"$v%.5f" }.mkString("[", ", ", "]")
    x.mkString("",", ",f"  lastPlayedAllTeams=${s.lastPlayedAllTeams} minLastPlayed=${s.minLastPlayed}, maxLastPlayed=${s.maxLastPlayed}, maxTimesPlayed=${s.maxTimesPlayed}, avgLastPlayed=${s.avgLastPlayed}, avgTimesPlayed=${s.avgTimesPlayed}, lastPlayedAllTeams=${s.lastPlayedAllTeams}, weight=${s.weight}%.5f, weights=${wghts}, random=${s.random}")
  }

  def nextSuggestion( i: Int, players: List[String], neverPair: Option[List[NeverPair]] ) = {
    log.info( s"Calculating $i with ${playedMatches.length} games" )

    val input = DuplicateSuggestions(players, numberSuggestion=105, neverPair=neverPair)

    val start = System.nanoTime()
    playedMatches.foreach { m =>
      log.finest( s"  $m" )
    }

    val sug = DuplicateSuggestionsCalculation.calculate(input, playedMatches.toList )

    sug.suggestions.get./*take(50).*/foreach {ss =>
      log.fine( s"  ${toStringDetail(ss)}" )
    }
    val end = System.nanoTime()

    val diff = end-start

    toDuplicateSummary(i,sug.suggestions.get.head, diff)
  }

  def toDuplicateSummary( im: Int, sug: Suggestion, calcTime: Long ) = {
    log.info( s"  Taking ${toString(sug)} calcTime ${calcTime} nanos" )
    val teams = sug.players.zipWithIndex.map { e =>
      val (p,i) = e
      DuplicateSummaryEntry( Team(s"T${i+1}",p.player1,p.player2,0,0), Some(9.0), Some(1))
    }

    (DuplicateSummary(s"M${im}",true,teams,18,2,true,im,im),calcTime)
  }

  def analyze( players: List[String], ignore: Int ) = {
    val pm = playedMatches.toList.drop(ignore)
    val res = for (p1 <- 0 until 8;
         p2 <- p1+1 until 8
        ) yield {
      analyzePair( pm, players(p1), players(p2) )
    }
    log.info("Players gamesPlayed minGamesBetween maxGamesBetween")
    res.toList.sortWith( (l,r) => l.minGamesBetween<r.minGamesBetween ).foreach { p =>
      log.info( p.toString() )
    }
    analyzePermutations(pm)
  }

  def getKey( l: DuplicateSummaryEntry ) = {
    if (l.team.player1 < l.team.player2) (l.team.player1,l.team.player2) else (l.team.player2,l.team.player1)
  }

  def sortDSE( l: DuplicateSummaryEntry, r: DuplicateSummaryEntry ) = {
    val (lp,_) = getKey(l)
    val (rp,_) = getKey(r)
    lp < rp
  }

  def analyzePermutations(pm: List[DuplicateSummary]) = {
    val keys = pm.map { ds =>
      ds.teams.sortWith(sortDSE).map { dse =>
        val (l,r) = getKey(dse)
        s"${l},${r}"
      }.mkString(",")
    }
    val distinct = keys.distinct
    log.info(s"Found ${distinct.length} distinct pairings")
    distinct.foreach { key =>
      val c = keys.count( k => k==key )
      log.info(s"$key  $c")
    }
  }

  case class Pair( player1: String, player2: String, gamesPlayed: Int, minGamesBetween: Int, maxGamesBetween: Int ) {
    override
    def toString() = {
      f"""$player1%s-$player2%s $gamesPlayed%3d $minGamesBetween%3d $maxGamesBetween%3d"""
    }
  }

  def analyzePair( pm: List[DuplicateSummary], p1: String, p2: String ) = {

    val playedGames = pm.zipWithIndex.filter { e =>
      val (m,i) = e
      m.containsPair(p1, p2)
    }.map { e =>
      val (m,i) = e
      i
    }

    val pg1 = if (playedGames.length <= 1) pm.length::Nil else playedGames.drop(1):::List(pm.length+playedGames.head)

    log.fine( s"${p1}-${p2} ${playedGames}" )
    log.fine( s"${p1}-${p2} ${pg1}" )

    val gb = playedGames.zip(pg1).map(e => e._2 - e._1)
    val mingb = gb.foldLeft(Int.MaxValue)(math.min)
    val maxgb = gb.foldLeft(0)(math.max)
    log.fine( s"${p1}-${p2} mingb=$mingb maxgb=$maxgb ${gb}" )

    Pair(p1,p2,playedGames.length,mingb,maxgb)
  }
}
