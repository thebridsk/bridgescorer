package com.github.thebridsk.bridge.data.duplicate.suggestion

import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.SystemTime
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema

/**
  * Constructor
  * @param player1
  * @param player2
  * @param lastPlayed number of games since they played
  */
@Schema(
  title = "Pairing - A suggested pairing",
  description = "A suggested pairing and some stats about the pairing."
)
case class Pairing(
    @Schema(description = "The name of a player", required = true)
    player1: String,
    @Schema(description = "The name of a player", required = true)
    player2: String,
    @Schema(
      description = "The number of matches since they last played together.",
      required = true
    )
    lastPlayed: Int,
    @Schema(
      description = "The number of times the pair has played together.",
      required = true
    )
    timesPlayed: Int
) {

  def normalize =
    if (player1 < player2) this else copy(player1 = player2, player2 = player1)

  def key = (player1, player2)
}

/**
  * Constructor
  * @param players
  * @param min the min of lastPlayed
  * @param max the max of lastPlayed
  * @param random a random number
  *
  */

@Schema(
  title = "Suggestion - A suggested player pairings.",
  description = "A suggested player pairings."
)
case class Suggestion(
    @ArraySchema(
      minItems = 4,
      maxItems = 4,
      schema = new Schema(implementation = classOf[Pairing]),
      uniqueItems = true,
      arraySchema = new Schema(
        description = "The player pair, otherwise known as a team",
        required = true
      )
    )
    players: List[Pairing],
    @Schema(
      description =
        "The minimum number of matches that any of the pairs last played together",
      required = true
    )
    minLastPlayed: Int,
    @Schema(
      description =
        "The maximum number of matches that any of the pairs last played together",
      required = true
    )
    maxLastPlayed: Int,
    @Schema(
      description =
        "The maximum number of times that any of the pairs last played together",
      required = true
    )
    maxTimesPlayed: Int,
    @Schema(
      description =
        "The average number of times that the pairs last played together",
      required = true
    )
    avgLastPlayed: Double,
    @Schema(
      description = "The average number of times that the pairs played together",
      required = true
    )
    avgTimesPlayed: Double,
    @Schema(
      description = "The last time this suggested pairing played",
      required = true
    )
    lastPlayedAllTeams: Int,
    @Schema(
      description = "The number of times the same pairings played",
      required = true
    )
    countAllPlayed: Int,
    @Schema(
      description = "The weight of this pairing, the higher the better",
      required = true
    )
    weight: Double,
    @ArraySchema(
      minItems = 0,
      schema = new Schema(`type` = "number", format = "double"),
      uniqueItems = false,
      arraySchema = new Schema(
        description =
          "The calculated weights various comparisons, the higher the better",
        required = true
      )
    )
    weights: List[Double],
    @Schema(
      description = "A random number to make each suggestion unique",
      required = true
    )
    random: Int,
    @Schema(description = "A key to identify this pairing.", required = true)
    key: String
)

@Schema(
  title = "NeverPair - A pair of players that should not be paired.",
  description =
    "NeverPair - A pair of players that should not be paired when making a suggestion of pairings."
)
case class NeverPair(
    @Schema(description = "The name of a player", required = true)
    player1: String,
    @Schema(description = "The name of a player", required = true)
    player2: String
)

@Schema(
  title = "DuplicateSuggestions - Suggested player pairings.",
  description = "Data structure for requesting and recieving player pairings."
)
case class DuplicateSuggestions(
    @ArraySchema(
      minItems = 8,
      maxItems = 8,
      schema = new Schema(implementation = classOf[String]),
      uniqueItems = false,
      arraySchema = new Schema(
        description = "The players to pair, must be exactly 8 players",
        required = true
      )
    )
    players: List[String],
    @Schema(description = "The number of suggestions to return.")
    numberSuggestion: Int,
    @ArraySchema(
      minItems = 0,
      schema = new Schema(implementation = classOf[Suggestion]),
      uniqueItems = false,
      arraySchema =
        new Schema(description = "The top suggested pairings", required = false)
    )
    suggestions: Option[List[Suggestion]],
    @Schema(
      `type` = "number",
      format = "double",
      required = false,
      description = "Calculation time in ms"
    )
    calcTimeMillis: Option[Double],
    @Schema(`type` = "integer", format = "int32", required = false)
    history: Option[Int],
    @ArraySchema(
      minItems = 0,
      schema = new Schema(implementation = classOf[NeverPair]),
      uniqueItems = false,
      arraySchema = new Schema(
        description = "Players that should not be paired.",
        required = false
      )
    )
    neverPair: Option[List[NeverPair]]
)

object DuplicateSuggestions {
  def apply(
      players: List[String],
      numberSuggestion: Int = 10,
      suggestions: Option[List[Suggestion]] = None,
      calcTimeMillis: Option[Double] = None,
      history: Option[Int] = None,
      neverPair: Option[List[NeverPair]] = None
  ) = {
    new DuplicateSuggestions(
      players,
      numberSuggestion,
      suggestions,
      calcTimeMillis,
      history,
      neverPair
    )
  }

}

object DuplicateSuggestionsCalculation {
  val log = Logger("bridge.DuplicateSuggestionsCalculation")

  def getKey(player1: String, player2: String) =
    if (player1 < player2) (player1, player2) else (player2, player1)

  def calculate(
      input: DuplicateSuggestions,
      pastgames: List[DuplicateSummary]
  ) = {
    val start = SystemTime.currentTimeMillis()
    val calc = new DuplicateSuggestionsCalculation(
      pastgames,
      input.players,
      input.neverPair
    )
    val end = SystemTime.currentTimeMillis()
    val calctime = end - start
    input.copy(
      suggestions = Some(calc.suggest.take(input.numberSuggestion)),
      calcTimeMillis = Some(calctime),
      history = Some(pastgames.length)
    )
  }
}

class Stats {
  var sum: Double = 0.0
  var n: Int = 0

  def avg = if (n == 0) 0 else sum / n

  var min: Double = Double.MaxValue
  var max: Double = Double.MinValue

  def add(value: Double) = {
    sum += value
    n += 1
    min = Math.min(min, value)
    max = Math.max(max, value)
  }

  /**
    * Normalize for optimizing the max of value
    */
  def normalizeForMax(value: Double) = {
    if (max == min) 0
    else (value - min) / (max - min)
  }

  /**
    * Normalize for optimizing the min of value
    */
  def normalizeForMin(value: Double) = {
    if (max == min) 0
    else (max - value) / (max - min)
  }
}

/**
  * Stats are normalized in the following way:
  *
  * - wMinLastPlayed - LastPlayed min becomes 1, max becomes 0.
  * - wMaxLastPlayed - LastPlayed min becomes 0, max becomes 1.
  * - wMaxTimesPlayed - TimesPlayed min becomes 1, max becomes 0.
  * - wAve            - LastPlayed min becomes 0, max becomes 1.
  * - wAvePlayed      - TimesPlayed min becomes 1, max becomes 0.
  * - wLastAll        - Last time all played min becomes 0, max becomes 1.
  * - wLastAll10      - minimum last time same teams played can repeat, in games.
  */
trait Weights {

  val minLastAll = 40

  val wMinLastPlayed = 5 // weight for maximizing min last played of the four teams
  val wMaxLastPlayed = 4 // weight for maximizing max last played of the four teams
  val wMaxTimesPlayed = 6 // weight for minimizing max time played of the four teams
  val wAve = 2 // weight for maximizing ave last played of the four teams
  val wAvePlayed = 3 // weight for minimizing ave times played of the four teams
  val wLastAll = 0 // weight for maximizing last time same teams played
  val wLastAll10 = 40 // minimum last time same teams played that it can repeat

  def weightTotal =
    wMinLastPlayed + wMaxLastPlayed + wMaxTimesPlayed + wAve + wAvePlayed + wLastAll + wLastAll10

}

/**
  * Constructor
  * @param pastgames all the past games, will be sorted by created field
  * @param players the players, must be exactly 8 players
  */
class DuplicateSuggestionsCalculation(
    pastgames: List[DuplicateSummary],
    players: List[String],
    neverPair: Option[List[NeverPair]] = None,
    weights: Weights = new Weights() {}
) {

  val wTotal = weights.weightTotal;

  def isNeverPair(p1: String, p2: String) = {
    val np1 = NeverPair(p1, p2)
    val np2 = NeverPair(p2, p1)
    neverPair.map(np => np.contains(np1) || np.contains(np2)).getOrElse(false)
  }

  if (players.length != 8)
    throw new IllegalArgumentException("Must specify exactly 8 players")

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
    def addGame(p1: String, p2: String, last: Int) = {
      import math._
      val p = maps.get(getKey(p1, p2)) match {
        case Some(pair) =>
          pair.copy(
            lastPlayed = min(pair.lastPlayed, last),
            timesPlayed = pair.timesPlayed + 1
          )
        case None =>
          Pairing(p1, p2, last, 1).normalize
      }
      maps += p.key -> p
    }

    val sortedgames = pastgames.sortWith((l, r) => l.created > r.created)

    log.fine("Sorted Games")
    sortedgames.foreach(ds => log.fine((s"""  ${ds}""")))

    for (i1 <- 0 until len;
         i2 <- i1 + 1 until len) {
      val p1 = sortedPlayers(i1)
      val p2 = sortedPlayers(i2)

      // only count the matches where both players are playing
      sortedgames
        .filter { ds =>
          ds.containsPlayer(p1, p2)
        }
        .zipWithIndex
        .filter { e =>
          val (ds, i) = e
          ds.containsPair(p1, p2)
        }
        .foreach { e =>
          val (ds, i) = e
          addGame(p1, p2, i)
        }
    }

    maps.values.foreach { p =>
      statTeamLastPlayed.add(p.lastPlayed)
      statTeamTimesPlayed.add(p.timesPlayed)
    }

    trace(maps)

    def getPairing(p1: String, p2: String) = {
      val key = getKey(p1, p2)
      maps.get(key) match {
        case Some(p) => p
        case None =>
          val p = Pairing(p1, p2, numberGames, 0).normalize
          maps += p.key -> p
          p
      }
    }

    val statLastAll = new Stats
    val statCountAllPayed = new Stats
    val statAveLastPlayed = new Stats
    val statAveTimesPlayed = new Stats

    val sug = {
      val y = sortedPlayers.permutations
        .map { perm =>
          perm
            .grouped(2)
            .map { pp =>
              getPairing(pp(0), pp(1))
            }
            .toList
            .sortWith((l, r) => l.player1 < r.player1)
        }
        .filter { pairs =>
          val np = pairs.find { p =>
            isNeverPair(p.player1, p.player2)
          }
          np.isEmpty
        }
        .map { pairs =>
          val min =
            pairs.foldLeft(numberGames)((ac, p) => Math.min(ac, p.lastPlayed))
          val max = pairs.foldLeft(0)((ac, p) => Math.max(ac, p.lastPlayed))
          val maxPlayed =
            pairs.foldLeft(0)((ac, p) => Math.max(ac, p.timesPlayed))
          val avg = pairs.foldLeft(0.0)((ac, p) => ac + p.lastPlayed) / pairs.length
          val avgPlayed = pairs.foldLeft(0.0)((ac, p) => ac + p.timesPlayed) / pairs.length

          statAveLastPlayed.add(avg)
          statAveTimesPlayed.add(avgPlayed)

          val key = pairs
            .map { p =>
              s"${p.player1},${p.player2}"
            }
            .mkString(",")
          val listLastAll = sortedgames.zipWithIndex
            .filter { e =>
              val (g, i) = e
              pairs.find { p =>
                !g.containsPair(p.player1, p.player2)
              }.isEmpty
            }
            .map(e => e._2)
          val lastAll = listLastAll.headOption.getOrElse(
            Math.max(sortedgames.length, weights.minLastAll)
          )
          statLastAll.add(lastAll)
          val countAllPlayed = listLastAll.length
          statCountAllPayed.add(countAllPlayed)

          /**
            * The weight of the suggestion, higher is better
            */
          val weight: Double = 0

          Suggestion(
            pairs,
            min,
            max,
            maxPlayed,
            avg,
            avgPlayed,
            lastAll,
            countAllPlayed,
            weight,
            List(),
            0,
            key
          )
        }
        .toList

      val x = y.groupBy(_.key).mapValues(_.head).values.toList

      scala.util.Random.shuffle(x).zipWithIndex.map { e =>
        e._1.copy(random = e._2)
      }
    }

    val sug1 = sug.map { s =>
      // Weight, higher values means a good choice
      // want:
      //   minLastPlayed max
      //   maxLastPlayed max
      //   maxTimesPlayed min
      //   avgLastPlayed max
      //   avgTimesPlayed min
      //   lastPlayedAllTeams max
      val weightsL = List(
        statTeamLastPlayed
          .normalizeForMax(s.minLastPlayed) * weights.wMinLastPlayed / wTotal,
        statTeamLastPlayed
          .normalizeForMax(s.maxLastPlayed) * weights.wMaxLastPlayed / wTotal,
        statTeamTimesPlayed
          .normalizeForMin(s.maxTimesPlayed) * weights.wMaxTimesPlayed / wTotal,
        statAveLastPlayed
          .normalizeForMax(s.avgLastPlayed) * weights.wAve / wTotal,
        statAveTimesPlayed
          .normalizeForMin(s.avgTimesPlayed) * weights.wAvePlayed / wTotal,
        statLastAll
          .normalizeForMax(s.lastPlayedAllTeams) * weights.wLastAll / wTotal,
        (if (s.lastPlayedAllTeams < weights.minLastAll && pastgames.length > weights.minLastAll)
           0.0
         else
           statLastAll
             .normalizeForMax(s.lastPlayedAllTeams)) * weights.wLastAll / wTotal
      )
      val raw = weightsL.foldLeft(0.0)((ac, v) => ac + v)
      val weight = Some(raw)
        .map { w =>
          if (s.lastPlayedAllTeams < weights.minLastAll) w / 10 else w
        }
        .map { w =>
          // if played last time or time before, then reduce weight
          if (s.minLastPlayed < 2) raw / 8 else w
        }
        .map { w =>
          if (s.countAllPlayed <= statCountAllPayed.min + 1) w else w - 0.5
        }
        .get
      s.copy(weight = weight, weights = weightsL)
    }

    def normalizeLastPlayed(lp: Int) = statTeamLastPlayed.normalizeForMax(lp)
    def normalizeTimesPlayed(tp: Int) = statTeamTimesPlayed.normalizeForMin(tp)

    // blends minimizing timesPlayed and maximizing lastPlayed
    def normalize(lastPlayed: Int, timesPlayed: Int) = {
      val weightLastPlayed = 0.49 // between 0 and 1

      normalizeLastPlayed(lastPlayed) * weightLastPlayed + normalizeTimesPlayed(
        timesPlayed
      ) * (1 - weightLastPlayed)
    }

    def compareNormalized(l: Suggestion, r: Suggestion) =
      normalize(l.maxLastPlayed, l.maxTimesPlayed)
        .compareTo(normalize(r.maxLastPlayed, r.maxTimesPlayed))

    // sorting with max first
    def compareMin(l: Suggestion, r: Suggestion) =
      r.minLastPlayed.compareTo(l.minLastPlayed)
    // sorting with max first
    def compareMax(l: Suggestion, r: Suggestion) =
      r.maxLastPlayed.compareTo(l.maxLastPlayed)
    // sorting with max first
    def compareAvg(l: Suggestion, r: Suggestion) =
      r.avgLastPlayed.compareTo(l.avgLastPlayed)
    // sorting with min first
    def compareMaxPlayed(l: Suggestion, r: Suggestion) =
      l.maxTimesPlayed.compareTo(r.maxTimesPlayed)
    // sorting with min first
    def compareAvgPlayed(l: Suggestion, r: Suggestion) =
      l.avgTimesPlayed.compareTo(r.avgTimesPlayed)
    // sorting with min first
    def compareRandom(l: Suggestion, r: Suggestion) =
      l.random.compareTo(r.random)
    // sorting with max first
    def compareLastAll(l: Suggestion, r: Suggestion) =
      r.lastPlayedAllTeams.compareTo(l.lastPlayedAllTeams)

    // sorting with max first
    def compareWeight(l: Suggestion, r: Suggestion) =
      r.weight.compareTo(l.weight)

    val compfun: List[(Suggestion, Suggestion) => Int] = {
//      List(compareMin,compareMaxPlayed,compareMax,compareRandom)
//      List(compareMin,compareNormalized,compareRandom)
//      List(compareMin,compareMax,compareRandom)
//      List(compareLastAll,compareMin,compareAvg,compareNormalized,compareRandom)
      List(compareWeight)
    }

    def sortFun(l: Suggestion, r: Suggestion): Boolean = {
      for (c <- compfun) {
        val rc = c(l, r)
        if (rc != 0) return rc < 0
      }
      false
    }

    val rsug = sug1.sortWith(sortFun)

    log.fine(s"Suggestions:")
    log.info(
      "The numbers in parentheses are ( lasttime played together, number of times played together)"
    )
    log.info(
      "The numbers at the end are the weights (higher means more likely) => total - minlastplayed maxlastplayed timesplayed avelastplayed avetimesplayed lastall minlastall"
    )
    rsug.zipWithIndex.foreach { e =>
      val (sg, i) = e
      val s = sg.players
        .sortWith { (l, r) =>
          if (l.lastPlayed == r.lastPlayed) l.timesPlayed < r.timesPlayed
          else l.lastPlayed < r.lastPlayed
        }
        .map(
          p =>
            f"${p.player1}%8s-${p.player2}%-8s (${p.lastPlayed}%2d,${p.timesPlayed}%2d)"
        )
        .mkString(", ")
      val wts = sg.weights.map(w => f"${w}%6.4f").mkString(" ")
      log.info(f"  ${i + 1}%3d: ${s}%s ${sg.weight}%6.4f - ${wts}")
    }

    (rsug, maps.toMap)
  }

  def trace(aps: collection.mutable.Map[(String, String), Pairing]): Unit = {

    def getPairing(p1: String, p2: String) = {
      val key = if (p1 < p2) (p1, p2) else (p2, p1)
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
