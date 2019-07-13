package com.github.thebridsk.bridge.data

import com.github.thebridsk.bridge.data.SystemTime.Timestamp
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore
import com.github.thebridsk.bridge.data.bridge.PerspectiveComplete

import scala.annotation.meta._
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.Hidden

@Schema(
  title = "DuplicateSummaryDetails - Team stats in a match",
  description = "Details about a team in a match"
)
case class DuplicateSummaryDetails(
    @Schema(description = "The id of the team", required = true)
    team: Id.Team,
    @Schema(
      description = "The number of times the team was declarer",
      required = true,
      minimum = "0"
    )
    declarer: Int = 0,
    @Schema(
      description = "The number of times the team made the contract as declarer",
      required = true,
      minimum = "0"
    )
    made: Int = 0,
    @Schema(
      description = "The number of times the team went down as declarer",
      required = true,
      minimum = "0"
    )
    down: Int = 0,
    @Schema(
      description = "The number of times the team defended the contract",
      required = true,
      minimum = "0"
    )
    defended: Int = 0,
    @Schema(
      description =
        "The number of times the team took down the contract as defenders",
      required = true,
      minimum = "0"
    )
    tookDown: Int = 0,
    @Schema(
      description =
        "The number of times the team allowed the contract to be made as defenders",
      required = true,
      minimum = "0"
    )
    allowedMade: Int = 0,
    @Schema(
      description = "The number of times the team passed out a game",
      required = true,
      minimum = "0"
    )
    passed: Int = 0
) {

  def add(v: DuplicateSummaryDetails) = {
    copy(
      declarer = declarer + v.declarer,
      made = made + v.made,
      down = down + v.down,
      defended = defended + v.defended,
      tookDown = tookDown + v.tookDown,
      allowedMade = allowedMade + v.allowedMade,
      passed = passed + v.passed
    )
  }

  def percentMade = if (declarer == 0) 0.0 else made * 100.0 / declarer
  def percentDown = if (declarer == 0) 0.0 else down * 100.0 / declarer
  def percentAllowedMade =
    if (defended == 0) 0.0 else allowedMade * 100.0 / defended
  def percentTookDown = if (defended == 0) 0.0 else tookDown * 100.0 / defended

  def percentDeclared = if (total == 0) 0.0 else declarer * 100.0 / total
  def percentDefended = if (total == 0) 0.0 else defended * 100.0 / total
  def percentPassed = if (total == 0) 0.0 else passed * 100.0 / total

  /**
    * Returns the total number of hands played by the team.
    */
  def total = declarer + defended + passed
}

object DuplicateSummaryDetails {
  def zero(team: Id.Team) = new DuplicateSummaryDetails(team)
  def passed(team: Id.Team) = new DuplicateSummaryDetails(team, passed = 1)
  def made(team: Id.Team) =
    new DuplicateSummaryDetails(team, declarer = 1, made = 1)
  def down(team: Id.Team) =
    new DuplicateSummaryDetails(team, declarer = 1, down = 1)
  def allowedMade(team: Id.Team) =
    new DuplicateSummaryDetails(team, defended = 1, allowedMade = 1)
  def tookDown(team: Id.Team) =
    new DuplicateSummaryDetails(team, defended = 1, tookDown = 1)
}

@Schema(
  title = "DuplicateSummaryEntry - The summary of a team in a duplicate match",
  description = "The summary of a team in a duplicate match"
)
case class DuplicateSummaryEntry(
    @Schema(description = "The team", required = true)
    team: Team,
    @Schema(
      description = "The points the team scored when using MP scoring",
      required = false,
      `type` = "number",
      format = "double"
    )
    result: Option[Double],
    @Schema(
      description = "The place the team finished in when using MP scoring",
      required = false,
      `type` = "integer",
      format = "int32"
    )
    place: Option[Int],
    @Schema(
      description = "Details about the team",
      required = false,
      implementation = classOf[DuplicateSummaryDetails]
    )
    details: Option[DuplicateSummaryDetails] = None,
    @Schema(
      description = "The IMPs the team scored",
      required = false,
      `type` = "number",
      format = "double"
    )
    resultImp: Option[Double] = None,
    @Schema(
      description = "The place using IMPs the team finished in",
      required = false,
      `type` = "integer",
      format = "int32"
    )
    placeImp: Option[Int] = None
) {
  def id = team.id

  @Hidden
  def getResultMp = result.getOrElse(0.0)
  @Hidden
  def getPlaceMp = place.getOrElse(1)

  def getResultImp = resultImp.getOrElse(0.0)
  def getPlaceImp = placeImp.getOrElse(1)

  def hasImp = resultImp.isDefined && placeImp.isDefined
  def hasMp = result.isDefined && place.isDefined
}

@Schema(
  name = "BestMatch",
  title = "BestMatch - Identifies the best match in the main store.",
  description = "Identifies the best match in the main store."
)
case class BestMatch(
    @Schema(
      title = "How similar the matches are",
      description =
        "How similar the matches are, percent of fields that are the same.",
      required = true
    )
    sameness: Double,
    @Schema(
      title = "The ID of the matching match duplicate",
      description =
        "The ID of the MatchDuplicate in the main store that is the best match, none if no match",
      required = false
    )
    id: Option[Id.MatchDuplicate],
    @ArraySchema(
      minItems = 0,
      uniqueItems = true,
      schema = new Schema(
        `type` = "string",
        description = "A field that is different"
      ),
      arraySchema = new Schema(
        description = "All the fields that are different.",
        required = false
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

object BestMatch {

  def noMatch = new BestMatch(-1, None, None)

  def apply(id: Id.MatchDuplicate, diff: Difference) = {
    new BestMatch(diff.percentSame, Some(id), Some(diff.differences))
  }
}

@Schema(
  title =
    "DuplicateSummary - A summary of duplicate matches that have been played.",
  description = "The summary of duplicate matches that have been played"
)
case class DuplicateSummary(
    @Schema(
      description = "The ID of the MatchDuplicate being summarized",
      required = true
    )
    id: Id.MatchDuplicate,
    @Schema(description = "True if the match is finished", required = true)
    finished: Boolean,
    @ArraySchema(
      minItems = 0,
      schema = new Schema(implementation = classOf[DuplicateSummaryEntry]),
      uniqueItems = true,
      arraySchema =
        new Schema(description = "The scores of the teams.", required = true)
    )
    teams: List[DuplicateSummaryEntry],
    @Schema(
      description = "The number of boards in the match",
      required = true,
      minimum = "1"
    )
    boards: Int,
    @Schema(
      description = "The number of tables in the match",
      required = true,
      minimum = "2"
    )
    tables: Int,
    @Schema(description = "True if this is only the results", required = true)
    onlyresult: Boolean,
    @Schema(
      description =
        "When the duplicate match was created, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    created: Timestamp,
    @Schema(
      description =
        "When the duplicate match was last updated, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    updated: Timestamp,
    @Schema(
      description = "the best match in the main store",
      required = false,
      implementation = classOf[BestMatch]
    )
    bestMatch: Option[BestMatch] = None,
    @Schema(
      description = "the scoring method used, default is MP",
      allowableValues = Array("MP", "IMP"),
      implementation = classOf[String],
      required = false
    )
    scoringmethod: Option[String] = None
) {

  def players() =
    teams.flatMap { t =>
      Seq(t.team.player1, t.team.player2)
    }.toList
  def playerPlaces() =
    teams.flatMap { t =>
      Seq((t.team.player1 -> t.getPlaceMp), (t.team.player2 -> t.getPlaceMp))
    }.toMap
  def playerScores() =
    teams.flatMap { t =>
      Seq((t.team.player1 -> t.getResultMp), (t.team.player2 -> t.getResultMp))
    }.toMap
  def playerPlacesImp() =
    teams.flatMap { t =>
      Seq((t.team.player1 -> t.getPlaceImp), (t.team.player2 -> t.getPlaceImp))
    }.toMap
  def playerScoresImp() =
    teams.flatMap { t =>
      Seq(
        (t.team.player1 -> t.getResultImp),
        (t.team.player2 -> t.getResultImp)
      )
    }.toMap

  def hasMpScores =
    teams.headOption
      .map(t => t.place.isDefined && t.result.isDefined)
      .getOrElse(false)
  def hasImpScores =
    teams.headOption
      .map(t => t.placeImp.isDefined && t.resultImp.isDefined)
      .getOrElse(false)

  def idAsDuplicateResultId = id.asInstanceOf[Id.MatchDuplicateResult]

  def containsPair(p1: String, p2: String) = {
    teams.find { dse =>
      (dse.team.player1 == p1 && dse.team.player2 == p2) || (dse.team.player1 == p2 && dse.team.player2 == p1)
    }.isDefined
  }

  /**
    * @return true if all players played in game
    */
  def containsPlayer(name: String*) = {
    name.find { p =>
      // return true if player did not play
      teams.find { dse =>
        // true if p is one of the players
        dse.team.player1 == p || dse.team.player2 == p
      }.isEmpty
    }.isEmpty
  }

  /**
    * Modify the player names according to the specified name map.
    * The timestamp is not changed.
    * @return None if the names were not changed.  Some() with the modified object
    */
  def modifyPlayers(nameMap: Map[String, String]) = {
    val (nteams, modified) = teams
      .map { t =>
        t.team.modifyPlayers(nameMap) match {
          case Some(nt) => (t.copy(team = nt), true)
          case None     => (t, false)
        }
      }
      .foldLeft((List[DuplicateSummaryEntry](), false)) { (ac, v) =>
        (ac._1 ::: List(v._1), ac._2 || v._2)
      }
    if (modified) {
      Some(copy(teams = nteams))
    } else {
      None
    }
  }

  import MatchDuplicateV3._
  @Hidden
  def isMP =
    scoringmethod
      .map { sm =>
        sm == MatchPoints
      }
      .getOrElse(true)
  @Hidden
  def isIMP =
    scoringmethod
      .map { sm =>
        sm == InternationalMatchPoints
      }
      .getOrElse(false)

}

object DuplicateSummary {
  def create(md: MatchDuplicate): DuplicateSummary = {
    val score = MatchDuplicateScore(md, PerspectiveComplete)
    val places = score.places.flatMap { p =>
      p.teams.map { t =>
        (t.id -> p.place)
      }.toList
    }.toMap
    val placesImps = score.placesImps.flatMap { p =>
      p.teams.map { t =>
        (t.id -> p.place)
      }.toList
    }.toMap
    val details = score
      .getDetails()
      .map { d =>
        d.team -> d
      }
      .toMap
    val t = md.teams.map { team =>
      DuplicateSummaryEntry(
        team,
        Some(score.teamScores(team.id)),
        Some(places(team.id)),
        details.get(team.id),
        score.teamImps.get(team.id),
        placesImps.get(team.id)
      )
    }.toList
    DuplicateSummary(
      md.id,
      score.alldone,
      t,
      md.boards.size,
      md.teams.size / 2,
      false,
      md.created,
      md.updated,
      None,
      md.scoringmethod
    )
  }

  def create(md: MatchDuplicateResult): DuplicateSummary = {
    val mdr = md.fixPlaces()
    val boards = mdr.getBoards()
    val tables = mdr.getTables()
    DuplicateSummary(
      mdr.id,
      !mdr.notfinished.getOrElse(false),
      mdr.results.flatten,
      boards,
      tables,
      true,
      mdr.played,
      mdr.played,
      None,
      Some(md.scoringmethod)
    )
  }
}
