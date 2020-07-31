package com.github.thebridsk.bridge.data.duplicate.stats

import io.swagger.v3.oas.annotations.media.Schema
import scala.reflect.ClassTag
import scala.collection.mutable
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.data.duplicate.stats.CalculatePlayerPlaces.ScoringMethod
import com.github.thebridsk.bridge.data.DuplicateSummaryEntry
import com.github.thebridsk.utilities.logging.Logger

@Schema(
  title = "PlayerPlaces - Player place stats",
  description = "Stats showing the number of times all player came in a place."
)
case class PlayerPlaces(
    @Schema(
      description = "The maximum number of teams in matches played",
      required = true
    )
    maxTeams: Int,
    @Schema(description = "all the players.", required = true)
    players: List[PlayerPlace]
)

@Schema(
  title = "PlayerPlace - Player place stats for one player",
  description = "Stats showing the number of times a player came in a place."
)
case class PlayerPlace(
    @Schema(description = "The name of the player", required = true)
    name: String,
    @Schema(
      description =
        "First index is place, place = i+1.  Second index is number of other teams tied.",
      required = true
    )
    place: List[List[Int]],
    @Schema(
      description = "Number of matches the player has played",
      required = true
    )
    total: Int,
    @Schema(
      description = "The maximum number of teams in a match this player played",
      required = true
    )
    maxTeams: Int
)

class TempPlayerPlaces(
    val name: String
) {
  import CalculatePlayerPlaces._

  private var fPlace: Array[Array[Int]] = Array()
  private var fTotal: Int = 0
  private var fMaxTeams: Int = 0

  def toPlayerPlaces: PlayerPlace = {
    val p = fPlace.map(l => l.toList).toList
    PlayerPlace(name, p, fTotal, fMaxTeams)
  }

  /**
    * Add a place result
    *
    * @param place the place the player came in, 1 = first, ...
    * @param otherTeams the number of other teams tied with player
    * @param nTeams the number of teams in the match
    */
  def addFinish(place: Int, otherTeams: Int, nTeams: Int): Unit = {
    fPlace = extendArray(fPlace, place, Array())
    val p = extendArray(fPlace(place - 1), otherTeams + 1, 0)
    p(otherTeams) += 1
    fPlace(place - 1) = p
    fTotal += 1
    fMaxTeams = Math.max(fMaxTeams, nTeams)
  }
}

object CalculatePlayerPlaces {

  val log: Logger = Logger[CalculatePlayerPlaces]()

  /**
    * Returns an extended array
    *
    * @param old the array to extend
    * @param newlen the new length of the array
    * @param pad the padding value to be applied to the extended array
    * @return an array of length newlen.
    */
  def extendArray[T](old: Array[T], newlen: Int, pad: => T)(
      implicit tag: ClassTag[T]
  ): Array[T] = {
    if (old.length < newlen) {
      val p = new Array[T](newlen)
      old.copyToArray(p)
      for (i <- old.length until newlen) {
        p(i) = pad
      }
      p
    } else {
      old
    }
  }

  sealed trait ScoringMethod {
    def isValid(d: DuplicateSummary): Boolean
    def getScoringMethod(d: DuplicateSummary): ScoringMethod = this
    def getPlace(d: DuplicateSummaryEntry): Option[Int]
  }

  object MPScoring extends ScoringMethod {
    def isValid(d: DuplicateSummary): Boolean = d.isMP || d.hasMpScores
    def getPlace(d: DuplicateSummaryEntry): Option[Int] = d.place
  }

  object IMPScoring extends ScoringMethod {
    def isValid(d: DuplicateSummary): Boolean = d.isIMP || d.hasImpScores
    def getPlace(d: DuplicateSummaryEntry): Option[Int] = d.placeImp
  }

  object AsPlayedScoring extends ScoringMethod {
    def isValid(d: DuplicateSummary) = true

    override def getScoringMethod(d: DuplicateSummary): ScoringMethod = {
      if (d.isMP) MPScoring
      else IMPScoring
    }

    // the following are never called
    def getPlace(d: DuplicateSummaryEntry): Option[Int] = None
  }
}

class CalculatePlayerPlaces(scoringmethod: ScoringMethod) {
  import CalculatePlayerPlaces._

  private val results: mutable.Map[String, TempPlayerPlaces] = mutable.Map()
  private var maxTeams: Int = 0

  @inline
  def add(d: MatchDuplicate): Unit = add(DuplicateSummary.create(d))
  @inline
  def add(d: MatchDuplicateResult): Unit = add(DuplicateSummary.create(d))

  def add(d: DuplicateSummary): Unit = {
    log.fine(s"Adding duplicate match ${d.id}")
    val sm = scoringmethod.getScoringMethod(d)

    if (d.finished && sm.isValid(d)) {
      maxTeams = Math.max(maxTeams, d.teams.length)
      val nPlaces = d.teams.foldLeft(Array[Int]()) { (acc, v) =>
        sm.getPlace(v)
          .map { p =>
            val a = extendArray(acc, p, 0)
            a(p - 1) += 1
            a
          }
          .getOrElse(acc)
      }

      def update(player: String, place: Int, others: Int) = {
        (results.get(player) match {
          case Some(tpp) => tpp
          case None =>
            val tpp = new TempPlayerPlaces(player)
            results += player -> tpp
            tpp
        }).addFinish(place, others, d.teams.length)
      }

      d.teams.foreach { dse =>
        sm.getPlace(dse) match {
          case Some(p) =>
            update(dse.team.player1, p, nPlaces(p - 1) - 1)
            update(dse.team.player2, p, nPlaces(p - 1) - 1)
          case None =>
        }
      }
    }
  }

  def finish(): PlayerPlaces = {
    val p = results.values
      .map(t => t.toPlayerPlaces)
      .toList
      .sortWith((l, r) => l.name.compare(r.name) < 0)
    val r = PlayerPlaces(maxTeams, p)
    log.fine(s"Finish, result ${r}")
    r
  }

}
