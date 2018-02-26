package com.example.data.duplicate.suggestion

import com.example.data.DuplicateSummary
import utils.logging.Logger
import com.example.data.SystemTime

/**
 * Constructor
 * @param player1
 * @param player2
 * @param lastPlayed number of games since they played
 */
case class Pairing( player1: String, player2: String, lastPlayed: Int, timesPlayed: Int ) {

  def normalize = if (player1 < player2) this else copy( player1=player2, player2=player1 )

  def key = (player1,player2)
}

/**
 * Constructor
 * @param players
 * @param min the min of lastPlayed
 * @param max the max of lastPlayed
 * @param random a random number
 *
 */
case class Suggestion( players: List[Pairing],
                       minLastPlayed: Int,
                       maxLastPlayed: Int,
                       maxTimesPlayed: Int,
                       avgLastPlayed: Double,
                       avgTimesPlayed: Double,
                       lastPlayedAllTeams: Int,
                       countAllPlayed: Int,
                       weight: Double,
                       weights: List[Double],
                       random: Int,
                       key: String )

case class NeverPair( player1: String, player2: String )

case class DuplicateSuggestions( players: List[String],
                                 numberSuggestion: Int,
                                 suggestions: Option[List[Suggestion]],
                                 calcTimeMillis: Option[Double],
                                 history: Option[Int],
                                 neverPair: Option[List[NeverPair]]
                               ) {
}

object DuplicateSuggestions {
  def apply( players: List[String],
             numberSuggestion: Int = 10,
             suggestions: Option[List[Suggestion]] = None,
             calcTimeMillis: Option[Double] = None,
             history: Option[Int] = None,
             neverPair: Option[List[NeverPair]] = None
           ) = {
    new DuplicateSuggestions(players,numberSuggestion,suggestions,calcTimeMillis,history,neverPair)
  }

}

object DuplicateSuggestionsCalculation {
  val log = Logger("bridge.DuplicateSuggestionsCalculation")

  def getKey( player1: String, player2: String ) = if (player1 < player2) (player1,player2) else (player2,player1)

  def calculate( input: DuplicateSuggestions, pastgames: List[DuplicateSummary] ) = {
    val start = SystemTime.currentTimeMillis()
    val calc = new DuplicateSuggestionsCalculation( pastgames, input.players, input.neverPair )
    val end = SystemTime.currentTimeMillis()
    val calctime = end-start
    input.copy( suggestions = Some(calc.suggest.take(input.numberSuggestion)), calcTimeMillis=Some(calctime), history=Some(pastgames.length) )
  }
}

class Stats {
  var sum: Double = 0.0
  var n: Int = 0

  def avg = if (n == 0) 0 else sum/n

  var min: Double = Double.MaxValue
  var max: Double = Double.MinValue

  def add( value: Double ) = {
    sum += value
    n += 1
    min = Math.min(min,value)
    max = Math.max(max,value)
  }

  /**
   * Normalize for optimizing the max of value
   */
  def normalizeForMax( value: Double ) = {
    if (max == min) 0
    else (value-min)/(max-min)
  }


  /**
   * Normalize for optimizing the min of value
   */
  def normalizeForMin( value: Double ) = {
    if (max == min) 0
    else (max-value)/(max-min)
  }
}

/**
 * Constructor
 * @param pastgames all the past games, will be sorted by created field
 * @param players the players, must be exactly 8 players
 */
class DuplicateSuggestionsCalculation(
                           pastgames: List[DuplicateSummary],
                           players: List[String],
                           neverPair: Option[List[NeverPair]] = None
                         ) {

  def isNeverPair( p1: String, p2: String ) = {
    val np1 = NeverPair(p1,p2)
    val np2 = NeverPair(p2,p1)
    neverPair.map( np => np.contains(np1)||np.contains(np2) ).getOrElse(false)
  }

  if (players.length != 8) throw new IllegalArgumentException("Must specify exactly 8 players")

  import DuplicateSuggestionsCalculation._

  val sortedPlayers = players.sorted

  val numberGames = pastgames.length

  /**
   * an ordered list of suggestions
   */
  val (suggest, pairs): (List[Suggestion], Map[(String, String), Pairing]) = {
    val len = players.length

    val statTeamLastPlayed = new Stats
    val statTeamTimesPlayed = new Stats

    val maps = collection.mutable.Map[(String, String), Pairing]()
    def addGame( p1: String, p2: String, last: Int ) = {
      import math._
      val p = maps.get(getKey(p1, p2)) match {
        case Some(pair) =>
          pair.copy( lastPlayed=min(pair.lastPlayed, last), timesPlayed=pair.timesPlayed+1)
        case None =>
          Pairing( p1, p2, last, 1 ).normalize
      }
      maps += p.key -> p
    }

    val sortedgames = pastgames.sortWith((l,r) => l.created > r.created)

    log.fine("Sorted Games")
    sortedgames.foreach(ds => log.fine((s"""  ${ds}""")))

    for ( i1 <- 0 until len;
          i2 <- i1+1 until len ) {
      val p1 = sortedPlayers(i1)
      val p2 = sortedPlayers(i2)

      // only count the matches where both players are playing
      sortedgames.filter { ds => ds.containsPlayer(p1,p2) }.zipWithIndex.filter { e =>
        val (ds,i) = e
        ds.containsPair(p1, p2)
      }.foreach { e =>
        val (ds,i) = e
        addGame(p1, p2, i)
      }
    }

    maps.values.foreach { p =>
      statTeamLastPlayed.add(p.lastPlayed)
      statTeamTimesPlayed.add(p.timesPlayed)
    }

    trace(maps)

    def getPairing( p1: String, p2: String ) = {
      val key = getKey(p1,p2)
      maps.get(key) match {
        case Some(p) => p
        case None =>
          val p = Pairing(p1,p2, numberGames, 0).normalize
          maps += p.key->p
          p
      }
    }

    val statLastAll = new Stats
    val statCountAllPayed = new Stats
    val statAveLastPlayed = new Stats
    val statAveTimesPlayed = new Stats

    val minLastAll = 40

    val sug = {
      val y = sortedPlayers.permutations.map { perm =>
        perm.grouped(2).map { pp => getPairing(pp(0),pp(1)) }.toList.sortWith((l,r) => l.player1<r.player1)
      }.filter { pairs =>
        val np = pairs.find { p => isNeverPair(p.player1, p.player2)}
        np.isEmpty
      }.map { pairs =>
        val min = pairs.foldLeft(numberGames)((ac,p)=> Math.min(ac, p.lastPlayed))
        val max = pairs.foldLeft(0)((ac,p)=> Math.max(ac, p.lastPlayed))
        val maxPlayed = pairs.foldLeft(0)((ac,p)=> Math.max(ac, p.timesPlayed))
        val avg = pairs.foldLeft(0.0)((ac,p)=> ac+p.lastPlayed)/pairs.length
        val avgPlayed = pairs.foldLeft(0.0)((ac,p)=> ac+p.timesPlayed)/pairs.length

        statAveLastPlayed.add(avg)
        statAveTimesPlayed.add(avgPlayed)

        val key = pairs.map { p => s"${p.player1},${p.player2}" }.mkString(",")
        val listLastAll = sortedgames.zipWithIndex.filter { e =>
          val (g,i) = e
          pairs.find { p =>
            !g.containsPair(p.player1, p.player2)
          }.isEmpty
        }.map( e => e._2)
        val lastAll = listLastAll.headOption.getOrElse(Math.max(sortedgames.length,minLastAll))
        statLastAll.add(lastAll)
        val countAllPlayed = listLastAll.length
        statCountAllPayed.add(countAllPlayed)

        /**
         * The weight of the suggestion, higher is better
         */
        val weight: Double = 0

        Suggestion( pairs, min, max, maxPlayed, avg, avgPlayed, lastAll, countAllPlayed, weight, List(), 0, key )
      }.toList

      val x = y.groupBy(_.key).mapValues(_.head).values.toList

      scala.util.Random.shuffle(x).zipWithIndex.map { e => e._1.copy( random = e._2 ) }
    }

    val wMinLastPlayed = 5       // weight for maximizing min last played of the four teams
    val wMaxLastPlayed = 1       // weight for maximizing max last played of the four teams
    val wMaxTimesPlayed = 5      // weight for minimizing max time played of the four teams
    val wAve = 2                 // weight for maximizing ave last played of the four teams
    val wAvePlayed = 3           // weight for minimizing ave times played of the four teams
    val wLastAll = 0             // weight for maximizing last time same teams played
    val wLastAll10 = 40          // minimum last time same teams played that it can repeat

    val wTotal = wMinLastPlayed+wMaxLastPlayed+wMaxTimesPlayed+wAve+wAvePlayed+wLastAll+wLastAll10

    val sug1 = sug.map { s =>
      // Weight, higher values means a good choice
      // want:
      //   minLastPlayed max
      //   maxLastPlayed max
      //   maxTimesPlayed min
      //   avgLastPlayed max
      //   avgTimesPlayed min
      //   lastPlayedAllTeams max
      val weights = List( statTeamLastPlayed.normalizeForMax(s.minLastPlayed)*wMinLastPlayed/wTotal,
                          statTeamLastPlayed.normalizeForMax(s.maxLastPlayed)*wMaxLastPlayed/wTotal,
                          statTeamTimesPlayed.normalizeForMin(s.maxTimesPlayed)*wMaxTimesPlayed/wTotal,
                          statAveLastPlayed.normalizeForMax(s.avgLastPlayed)*wAve/wTotal,
                          statAveTimesPlayed.normalizeForMin(s.avgTimesPlayed)*wAvePlayed/wTotal,
                          statLastAll.normalizeForMax(s.lastPlayedAllTeams)*wLastAll/wTotal,
                          (if (s.lastPlayedAllTeams < minLastAll && pastgames.length > minLastAll ) 0.0 else statLastAll.normalizeForMax(s.lastPlayedAllTeams))*wLastAll/wTotal )
      val raw = weights.foldLeft(0.0)((ac,v)=>ac+v)
      val weight = Some(raw).map { w =>
        if (s.lastPlayedAllTeams < minLastAll) w/10 else w
      }.map { w =>
        // if played last time or time before, then reduce weight
        if (s.minLastPlayed < 2) raw/8 else w
      }.map { w =>
        if (s.countAllPlayed <= statCountAllPayed.min+1) w else w-0.5
      }.get
      s.copy(weight=weight, weights=weights)
    }

    def normalizeLastPlayed( lp: Int ) = statTeamLastPlayed.normalizeForMax(lp)
    def normalizeTimesPlayed( tp: Int ) = statTeamTimesPlayed.normalizeForMin(tp)

    // blends minimizing timesPlayed and maximizing lastPlayed
    def normalize( lastPlayed: Int, timesPlayed: Int ) = {
      val weightLastPlayed = 0.49     // between 0 and 1

      normalizeLastPlayed(lastPlayed)*weightLastPlayed+normalizeTimesPlayed(timesPlayed)*(1-weightLastPlayed)
    }

    def compareNormalized( l: Suggestion, r: Suggestion ) =
      normalize(l.maxLastPlayed,l.maxTimesPlayed ).compareTo( normalize(r.maxLastPlayed,r.maxTimesPlayed ) )

    // sorting with max first
    def compareMin( l: Suggestion, r: Suggestion ) = r.minLastPlayed.compareTo( l.minLastPlayed )
    // sorting with max first
    def compareMax( l: Suggestion, r: Suggestion ) = r.maxLastPlayed.compareTo( l.maxLastPlayed )
    // sorting with max first
    def compareAvg( l: Suggestion, r: Suggestion ) = r.avgLastPlayed.compareTo( l.avgLastPlayed )
    // sorting with min first
    def compareMaxPlayed( l: Suggestion, r: Suggestion ) = l.maxTimesPlayed.compareTo( r.maxTimesPlayed )
    // sorting with min first
    def compareAvgPlayed( l: Suggestion, r: Suggestion ) = l.avgTimesPlayed.compareTo( r.avgTimesPlayed )
    // sorting with min first
    def compareRandom( l: Suggestion, r: Suggestion ) = l.random.compareTo( r.random )
    // sorting with max first
    def compareLastAll( l: Suggestion, r: Suggestion ) = r.lastPlayedAllTeams.compareTo( l.lastPlayedAllTeams )

    // sorting with max first
    def compareWeight( l: Suggestion, r: Suggestion ) = r.weight.compareTo(l.weight)

    val compfun: List[(Suggestion,Suggestion)=>Int] = {
//      List(compareMin,compareMaxPlayed,compareMax,compareRandom)
//      List(compareMin,compareNormalized,compareRandom)
//      List(compareMin,compareMax,compareRandom)
//      List(compareLastAll,compareMin,compareAvg,compareNormalized,compareRandom)
      List(compareWeight)
    }

    def sortFun( l: Suggestion, r: Suggestion ): Boolean = {
      for (c <- compfun) {
        val rc = c(l,r)
        if (rc != 0) return rc<0
      }
      false
    }


    val rsug = sug1.sortWith( sortFun )

    log.fine(s"Suggestions:")
    log.info("The numbers in parentheses are ( lasttime played together, number of times played together)")
    log.info("The numbers at the end are the weights (higher means more likely) => total - minlastplayed maxlastplayed timesplayed avelastplayed avetimesplayed lastall minlastall")
    rsug.zipWithIndex.foreach { e =>
      val (sg,i) = e
      val s = sg.players.sortWith { (l,r) =>
                            if (l.lastPlayed==r.lastPlayed) l.timesPlayed<r.timesPlayed
                            else l.lastPlayed<r.lastPlayed
                          }.map( p => f"${p.player1}%8s-${p.player2}%-8s (${p.lastPlayed}%2d,${p.timesPlayed}%2d)").mkString(", ")
      val wts = sg.weights.map( w => f"${w}%6.4f" ).mkString(" ")
      log.info(f"  ${i+1}%3d: ${s}%s ${sg.weight}%6.4f - ${wts}")
    }

    ( rsug, maps.toMap )
  }

  def trace( aps: collection.mutable.Map[(String, String), Pairing] ): Unit = {

    def getPairing( p1: String, p2: String ) = {
      val key = if (p1<p2) (p1,p2) else (p2,p1)
      aps.get(key)
    }

    log.fine(s"""         ${sortedPlayers.mkString(" ")}""")
    sortedPlayers.foreach { p1 =>
      val s = sortedPlayers.map { p2 =>
        getPairing(p1, p2).map(p => p.lastPlayed.toString()).getOrElse("x")
      }
      log.fine(s"""${p1}: ${s.mkString(" ")}""")
    }
  }

}
