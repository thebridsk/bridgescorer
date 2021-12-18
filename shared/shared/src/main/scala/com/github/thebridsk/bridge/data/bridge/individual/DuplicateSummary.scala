package com.github.thebridsk.bridge.data.bridge.individual

import com.github.thebridsk.bridge.data.SystemTime.Timestamp
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.Hidden
import scala.reflect.ClassTag
import com.github.thebridsk.bridge.data.HasId
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.data.IndividualDuplicate
import com.github.thebridsk.bridge.data.Difference
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.Id

@Schema(
  title = "DuplicateSummaryEntry - The summary of a player in a duplicate match",
  description = "The summary of a player in a duplicate match"
)
case class DuplicateSummaryEntry(
    @Schema(description = "The player", required = true)
    player: String,
    @Schema(
      description = "The points the player scored when using MP scoring",
      required = false,
      `type` = "number",
      format = "double"
    )
    result: Option[Int],
    @Schema(
      description = "The place the player finished in when using MP scoring",
      required = false,
      `type` = "integer",
      format = "int32"
    )
    place: Option[Int],
    @Schema(
      description = "Details about the player",
      required = false,
      implementation = classOf[IndividualDuplicateSummaryDetails]
    )
    details: Option[IndividualDuplicateSummaryDetails] = None,
    @Schema(
      description = "The IMPs the player scored",
      required = false,
      `type` = "number",
      format = "double"
    )
    resultImp: Option[Double] = None,
    @Schema(
      description = "The place using IMPs the player finished in",
      required = false,
      `type` = "integer",
      format = "int32"
    )
    placeImp: Option[Int] = None
) {

  @Hidden
  def getResultMp: Int = result.getOrElse(0)
  @Hidden
  def getPlaceMp: Int = place.getOrElse(1)

  @Hidden
  def getResultImp: Double = resultImp.getOrElse(0.0)
  @Hidden
  def getPlaceImp: Int = placeImp.getOrElse(1)

  def hasImp: Boolean = resultImp.isDefined && placeImp.isDefined
  def hasMp: Boolean = result.isDefined && place.isDefined
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
    id: Option[DuplicateSummary.Id],
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

  def determineDifferences(l: List[String]): List[String] = {
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

  def htmlTitle: Option[String] = {
    differences.map { l =>
      if (l.isEmpty) "Same"
      else determineDifferences(l).mkString("Differences:\n", "\n", "")
    }
  }

}

object BestMatch {

  def noMatch = new BestMatch(-1, None, None)

  def apply(id: DuplicateSummary.Id, diff: Difference): BestMatch = {
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
    id: DuplicateSummary.Id,
    @Schema(description = "True if the match is finished", required = true)
    finished: Boolean,
    @ArraySchema(
      minItems = 0,
      schema = new Schema(implementation = classOf[DuplicateSummaryEntry]),
      uniqueItems = true,
      arraySchema =
        new Schema(description = "The scores of the teams.", required = true)
    )
    players: List[DuplicateSummaryEntry],
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
    onlyResult: Boolean,
    @Schema(description = "If length is zero, individual movement was used, otherwise list of teams.", required = true)
    teams: List[Team],
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

  def playerNames(): List[String] =
    players.map(_.player)
  def playerPlaces(): Map[String, Int] =
    players.map { p =>
      p.player -> p.getPlaceMp
    }.toMap
  def playerScores(): Map[String, Int] =
    players.map { p =>
      p.player -> p.getResultMp
    }.toMap
  def playerPlacesImp(): Map[String, Int] =
    players.map { p =>
      p.player -> p.getPlaceImp
    }.toMap
  def playerScoresImp(): Map[String, Double] =
    players.map { p =>
      p.player -> p.getResultImp
    }.toMap

  def hasMpScores: Boolean =
    players.headOption
      .map(p => p.place.isDefined && p.result.isDefined)
      .getOrElse(false)
  def hasImpScores: Boolean =
    players.headOption
      .map(p => p.placeImp.isDefined && p.resultImp.isDefined)
      .getOrElse(false)

  def idAsDuplicateResultId: Option[Id[MatchDuplicateResult.ItemType]] =
    id.toSubclass[MatchDuplicateResult.ItemType]
  def idAsDuplicateId: Option[Id[MatchDuplicate.ItemType]] =
    id.toSubclass[MatchDuplicate.ItemType]
  def idAsIndividualDuplicateId: Option[Id[IndividualDuplicate.ItemType]] =
    id.toSubclass[IndividualDuplicate.ItemType]

  def containsPair(p1: String, p2: String): Boolean = {
    teams.find { dse =>
      (dse.player1 == p1 && dse.player2 == p2) || (dse.player1 == p2 && dse.player2 == p1)
    }.isDefined
  }

  /**
    * @return true if all players played in game
    */
  def containsPlayer(name: String*): Boolean = {
    val names = playerNames()
    name.find { p => !names.contains(p) }.isEmpty
  }

  import MatchDuplicate._
  @Hidden
  def isMP: Boolean =
    scoringmethod
      .map { sm =>
        sm == MatchPoints
      }
      .getOrElse(true)
  @Hidden
  def isIMP: Boolean =
    scoringmethod
      .map { sm =>
        sm == InternationalMatchPoints
      }
      .getOrElse(false)

}

package object ids {
  type IdDuplicateSummary = com.github.thebridsk.bridge.data.IdDuplicateSummary
}
import ids.IdDuplicateSummary

object DuplicateSummary extends HasId[IdDuplicateSummary]("") {
  override def id(i: Int): Id = {
    throw new IllegalArgumentException(
      "DuplicateSummary Ids can not be generated, must use MatchDuplicate.Id, MatchDuplicateResult.Id, or IndividualDuplicate.Id"
    )
  }

  override def id(s: String): Id = {
    Id.parseId(s) match {
      case Some((p, i)) =>
        p match {
          case MatchDuplicate.prefix =>
            MatchDuplicate.id(s).asInstanceOf[Id]
          case MatchDuplicateResult.prefix =>
            MatchDuplicateResult.id(s).asInstanceOf[Id]
          case IndividualDuplicate.prefix =>
            IndividualDuplicate.id(s).asInstanceOf[Id]
          case _ =>
            throw new IllegalArgumentException(
              s"DuplicateSummary Id syntax is not valid: ${s}"
            )
        }
      case _ =>
        throw new IllegalArgumentException(
          s"DuplicateSummary Id syntax is not valid: ${s}"
        )
    }
  }

  def useId[T](
      id: DuplicateSummary.Id,
      fmd: MatchDuplicate.Id => T,
      fmdr: MatchDuplicateResult.Id => T,
      default: => T
  ): T = {
    id.toSubclass[MatchDuplicate.ItemType].map(fmd).getOrElse {
      id.toSubclass[MatchDuplicateResult.ItemType].map(fmdr).getOrElse(default)
    }
  }

  def useId[T](
      id: DuplicateSummary.Id,
      fmd: MatchDuplicate.Id => T,
      fmdr: MatchDuplicateResult.Id => T,
      fid: IndividualDuplicate.Id => T,
      default: => T
  ): T = {
    id.toSubclass[MatchDuplicate.ItemType].map(fmd).getOrElse {
      id.toSubclass[MatchDuplicateResult.ItemType].map(fmdr).getOrElse {
        id.toSubclass[IndividualDuplicate.ItemType].map(fid).getOrElse(default)
      }
    }
  }

  import com.github.thebridsk.bridge.data.{Id => DId}
  def runIf[T <: IdDuplicateSummary: ClassTag, R](
      id: DuplicateSummary.Id,
      default: => R
  )(f: DId[T] => R): R = {
    id.toSubclass[T].map(sid => f(sid)).getOrElse(default)
  }

  def create(md: MatchDuplicate): DuplicateSummary = {
    import com.github.thebridsk.bridge.data.bridge.PerspectiveComplete
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
    val details = score.getDetails.map { d =>
      d.team -> d
    }.toMap
    val t = md.teams.flatMap { team =>
      List(
        DuplicateSummaryEntry(
          team.player1,
          Some(score.teamScores(team.id).toInt),
          Some(places(team.id)),
          details.get(team.id).map { d =>
            IndividualDuplicateSummaryDetails(
              team.player1,
              d.declarer,
              d.made,
              d.down,
              d.defended,
              d.tookDown,
              d.allowedMade,
              d.passed
            )
          },
          score.teamImps.get(team.id),
          placesImps.get(team.id)
        ),
        DuplicateSummaryEntry(
          team.player2,
          Some(score.teamScores(team.id).toInt),
          Some(places(team.id)),
          details.get(team.id).map { d =>
            IndividualDuplicateSummaryDetails(
              team.player2,
              d.declarer,
              d.made,
              d.down,
              d.defended,
              d.tookDown,
              d.allowedMade,
              d.passed
            )
          },
          score.teamImps.get(team.id),
          placesImps.get(team.id)
        )
      )
    }.toList
    DuplicateSummary(
      md.id,
      score.alldone,
      t,
      md.boards.size,
      md.teams.size / 2,
      false,
      md.teams,
      md.created,
      md.updated,
      None,
      md.scoringmethod
    )
  }

  def create(md: MatchDuplicateResult): DuplicateSummary = {
    val mdr = md.fixPlaces
    val boards = mdr.getBoards
    val tables = mdr.getTables
    DuplicateSummary(
      mdr.id,
      !mdr.notfinished.getOrElse(false),
      mdr.results.flatten.flatMap { d =>
        List(
          DuplicateSummaryEntry(
            d.team.player1,
            d.result.map(_.toInt),
            d.place,
            d.details.map { dsd =>
              IndividualDuplicateSummaryDetails(
                d.team.player1,
                dsd.declarer,
                dsd.made,
                dsd.down,
                dsd.defended,
                dsd.tookDown,
                dsd.allowedMade,
                dsd.passed
              )
            },
            d.resultImp,
            d.placeImp
          ),
          DuplicateSummaryEntry(
            d.team.player2,
            d.result.map(_.toInt),
            d.place,
            d.details.map { dsd =>
              IndividualDuplicateSummaryDetails(
                d.team.player2,
                dsd.declarer,
                dsd.made,
                dsd.down,
                dsd.defended,
                dsd.tookDown,
                dsd.allowedMade,
                dsd.passed
              )
            },
            d.resultImp,
            d.placeImp
          )
        )
      },
      boards,
      tables,
      true,
      Nil, // teams
      mdr.played,
      mdr.played,
      None,
      Some(md.scoringmethod)
    )
  }

  def create(md: IndividualDuplicate): DuplicateSummary = {
    import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateViewPerspective.PerspectiveComplete
    val score = IndividualDuplicateScore(md, PerspectiveComplete)
    val places = score.placesMP.flatMap { p =>
      p.players.map { pp =>
        pp -> p.place
      }.toList
    }.toMap
    val placesImps = score.placesImps.flatMap { p =>
      p.players.map { pp =>
        pp -> p.place
      }.toList
    }.toMap
    val details = score.getDetails.map { d =>
      d.player -> d
    }.toMap
    val t = (1 to md.players.size).map { iplayer =>
      val name = md.getPlayer(iplayer)
      val sc = score.scores(iplayer)
      val placeMp = score.placesMP.find(_.players.contains(name)).map(_.place)
      val placeImp = score.placesImps.find(_.players.contains(name)).map(_.place)
      DuplicateSummaryEntry(
        name,
        Some(sc.mp),
        placeMp,
        None,
        Some(sc.imp),
        placeImp,
      )
    }.toList
    DuplicateSummary(
      md.id,
      score.isAllDone,
      t,
      md.boards.size,
      md.teams.size / 2,
      false,
      md.teams,
      md.created,
      md.updated,
      None,
      md.scoringmethod
    )
  }
}
