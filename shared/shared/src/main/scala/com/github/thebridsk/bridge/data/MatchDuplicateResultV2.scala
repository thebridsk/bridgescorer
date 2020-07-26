package com.github.thebridsk.bridge.data

import com.github.thebridsk.bridge.data.SystemTime.Timestamp

import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore
import com.github.thebridsk.bridge.data.bridge.PerspectiveComplete
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.Hidden

@Schema(
  name = "MatchDuplicateResult",
  title = "MatchDuplicateResult - the results of a match.",
  description =
    "The results of a match.  This is used when the scoring was done by paper and only the results are known."
)
case class MatchDuplicateResultV2 private (
    @Schema(description = "The ID of the MatchDuplicate", required = true)
    id: MatchDuplicateResult.Id,
    @ArraySchema(
      minItems = 0,
      uniqueItems = true,
      schema = new Schema(
        description = "A duplicate summary entry",
        implementation = classOf[DuplicateSummaryEntry]
      ),
      arraySchema = new Schema(
        description = "The results of the match, a list of winnersets." + "  Each winnerset is a list of DuplicateSummaryEntry objects that show the results of teams that competed against each other.",
        required = true
      )
    )
    results: List[List[DuplicateSummaryEntry]],
    @ArraySchema(
      schema = new Schema(
        implementation = classOf[BoardResults],
        description = "The results of one board"
      ),
      arraySchema = new Schema(
        description =
          "The board scores of the teams, a list of BoardResults objects",
        required = false
      )
    )
    boardresults: Option[List[BoardResults]],
    @Schema(description = "a comment", required = false)
    comment: Option[String],
    @Schema(
      description = "True if the match is not finished, default is false",
      `type` = "boolean",
      required = false
    )
    notfinished: Option[Boolean],
    @Schema(
      description =
        "when the duplicate match was played, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    played: Timestamp,
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
      description = "the scoring method used",
      `type` = "enum",
      allowableValues = Array("MP", "IMP"),
      required = true
    )
    scoringmethod: String
) extends VersionedInstance[
      MatchDuplicateResult,
      MatchDuplicateResultV2,
      MatchDuplicateResult.Id
    ] {

  def equalsIgnoreModifyTime(
      other: MatchDuplicateResultV2,
      throwit: Boolean = false
  ): Boolean =
    id == other.id &&
      equalsInResults(other, throwit)

  def equalsInResults(
      other: MatchDuplicateResultV2,
      throwit: Boolean = false
  ): Boolean = {
    if (results.length == other.results.length) {
      results
        .zip(other.results)
        .map { e =>
          val (left, right) = e
          left.find { t1 =>
            // this function must return true if t1 is NOT in other.team
            val rc = !right.contains(t1)
            if (rc && throwit)
              throw new Exception(
                "MatchDuplicateResultV2 other did not have result equal to: " + t1
              )
            rc
          }.isEmpty
        }
        .foldLeft(true)((ac, x) => ac && x)
    } else {
      if (throwit)
        throw new Exception(
          "MatchDuplicateResultV2 results don't winner sets: " + results + " " + other.results
        )
      false
    }
  }

  def setId(
      newId: MatchDuplicateResult.Id,
      forCreate: Boolean,
      dontUpdateTime: Boolean = false
  ): MatchDuplicateResultV2 = {
    if (dontUpdateTime) {
      copy(id = newId)
    } else {
      val time = SystemTime.currentTimeMillis()
      copy(
        id = newId, /* created=if (forCreate) time; else created, */ updated =
          time
      )
    }
  }

  def copyForCreate(id: MatchDuplicateResult.Id): MatchDuplicateResultV2 = {
    val time = SystemTime.currentTimeMillis()
    copy(id = id, created = time, updated = time)

  }

  @Schema(hidden = true)
  def getTables: Int = {
    results.flatten.length / 2
  }

  @Schema(hidden = true)
  def getBoards: Int = {
    val nTables = getTables
    val pointsPerBoard = nTables * (nTables - 1)
    if (pointsPerBoard == 0) 0
    else getTotalPoints / pointsPerBoard
  }

  @Schema(hidden = true)
  def getTotalPoints: Int = {
    results.flatten
      .map { r =>
        r.result.getOrElse(0.0)
      }
      .foldLeft(0.0)((ac, v) => ac + v)
      .toInt
  }

  def fixPlaces: MatchDuplicateResultV2 = {
    val places = results.map { winnerset =>
      val m = winnerset.groupBy(e => e.result.getOrElse(0.0)).map { e =>
        val (points, teams) = e
        points -> teams
      }
      val sorted = m.toList.sortWith((e1, e2) => e1._1 > e2._1)
      var place = 1
      sorted.flatMap { e =>
        val (points, ts) = e
        val fixed = ts.map(e => e.copy(place = Some(place)))
        place += ts.size
        fixed
      }
    }
    copy(results = places)
  }

  def fixPlacesImp: MatchDuplicateResultV2 = {
    val places = results.map { winnerset =>
      val m = winnerset.groupBy(e => e.resultImp.getOrElse(0.0)).map { e =>
        val (points, teams) = e
        points -> teams
      }
      val sorted = m.toList.sortWith((e1, e2) => e1._1 > e2._1)
      var place = 1
      sorted.flatMap { e =>
        val (points, ts) = e
        val fixed = ts.map(e => e.copy(placeImp = Some(place)))
        place += ts.size
        fixed
      }
    }
    copy(results = places)
  }

  @Schema(hidden = true)
  def fixupSummary: MatchDuplicateResultV2 = {
    boardresults match {
      case Some(l) =>
        this
      case None =>
        this
    }
  }

  @Schema(hidden = true)
  def fixup: MatchDuplicateResultV2 = {
    fixupSummary.fixPlaces.fixPlacesImp
  }

  @Schema(hidden = true)
  def getWinnerSets: List[List[Team.Id]] = {
    results.map(l => l.map(e => e.team.id))
  }

  @Schema(hidden = true)
  def placeByWinnerSet(
      winnerset: List[Team.Id]
  ): List[MatchDuplicateScore.Place] = {
    results.find(ws => ws.find(e => !winnerset.contains(e.team.id)).isEmpty) match {
      case Some(rws) =>
        rws
          .groupBy(e => e.place.getOrElse(1))
          .map { arg =>
            val (place, list) = arg
            val pts = list.head.result.getOrElse(0.0)
            val teams = list.map(dse => dse.team)
            MatchDuplicateScore.Place(place, pts, teams)
          }
          .toList
      case None =>
        throw new Exception(
          s"could not find winner set ${winnerset} in ${results}"
        )
    }
  }

  @Schema(hidden = true)
  def placeByWinnerSetIMP(
      winnerset: List[Team.Id]
  ): List[MatchDuplicateScore.Place] = {
    results.find(ws => ws.find(e => !winnerset.contains(e.team.id)).isEmpty) match {
      case Some(rws) =>
        rws
          .groupBy(e => e.placeImp.getOrElse(1))
          .map { arg =>
            val (place, list) = arg
            val pts = list.head.resultImp.getOrElse(0.0)
            val teams = list.map(dse => dse.team)
            MatchDuplicateScore.Place(place, pts, teams)
          }
          .toList
      case None =>
        throw new Exception(
          s"could not find winner set ${winnerset} in ${results}"
        )
    }
  }

  /**
    * Modify the player names according to the specified name map.
    * The timestamp is not changed.
    * @return None if the names were not changed.  Some() with the modified object
    */
  def modifyPlayers(nameMap: Map[String, String]): Option[MatchDuplicateResultV2] = {
    val (nresults, modified) = results
      .map { ws =>
        ws.map { t =>
            t.team.modifyPlayers(nameMap) match {
              case Some(nt) => (t.copy(team = nt), true)
              case None     => (t, false)
            }
          }
          .foldLeft((List[DuplicateSummaryEntry](), false)) { (ac, v) =>
            (ac._1 ::: List(v._1), ac._2 || v._2)
          }
      }
      .foldLeft((List[List[DuplicateSummaryEntry]](), false)) { (ac, v) =>
        (ac._1 ::: List(v._1), ac._2 || v._2)
      }
    if (modified) {
      Some(copy(results = nresults))
    } else {
      None
    }
  }

  import MatchDuplicateV3._
  @Hidden
  def isMP: Boolean = scoringmethod == MatchPoints
  @Hidden
  def isIMP: Boolean = scoringmethod == InternationalMatchPoints

  def convertToCurrentVersion: (Boolean, MatchDuplicateResultV2) = (true, this)

  def readyForWrite: MatchDuplicateResultV2 = this

}

trait IdMatchDuplicateResult extends IdDuplicateSummary

object MatchDuplicateResultV2 extends HasId[IdMatchDuplicateResult]("E") {

  def apply(
      id: MatchDuplicateResult.Id,
      results: List[List[DuplicateSummaryEntry]],
      boardresults: Option[List[BoardResults]],
      comment: Option[String],
      notfinished: Boolean,
      scoringmethod: String,
      played: Timestamp,
      created: Timestamp,
      updated: Timestamp
  ): MatchDuplicateResultV2 = {
    new MatchDuplicateResultV2(
      id,
      results,
      boardresults,
      comment,
      Some(notfinished),
      played,
      created,
      updated,
      scoringmethod
    ).fixup
  }

  def apply(
      id: MatchDuplicateResult.Id,
      results: List[List[DuplicateSummaryEntry]],
      boardresults: List[BoardResults],
      comment: Option[String],
      notfinished: Boolean,
      scoringmethod: String,
      played: Timestamp,
      created: Timestamp,
      updated: Timestamp
  ): MatchDuplicateResultV2 = {
    new MatchDuplicateResultV2(
      id,
      results,
      Option(boardresults),
      comment,
      Some(notfinished),
      played,
      created,
      updated,
      scoringmethod
    ).fixup
  }

  def apply(
      id: MatchDuplicateResult.Id,
      results: List[List[DuplicateSummaryEntry]],
      scoringmethod: String,
      played: Timestamp,
      created: Timestamp,
      updated: Timestamp
  ): MatchDuplicateResultV2 = {
    new MatchDuplicateResultV2(
      id,
      results,
      None,
      None,
      None,
      played,
      created,
      updated,
      scoringmethod
    ).fixup
  }

  def create(id: MatchDuplicateResult.Id = MatchDuplicateResult.idNul, scoringmethod: String = "MP"): MatchDuplicateResultV2 = {
    val time = SystemTime.currentTimeMillis()
    new MatchDuplicateResultV2(
      id,
      List(),
      None,
      None,
      None,
      time,
      time,
      time,
      scoringmethod
    ).fixup
  }

  def createFrom(
      md: MatchDuplicate,
      mdr: Option[MatchDuplicateResult] = None
  ): MatchDuplicateResultV2 = {
    val score = MatchDuplicateScore(md, PerspectiveComplete)
    val wss = score.getWinnerSets
    val places = score.places.flatMap { p =>
      p.teams.map { t =>
        (t.id -> p.place)
      }.toList
    }.toMap
    val r = wss.map { ws =>
      ws.map { tid =>
          score.getTeam(tid).get
        }
        .map { team =>
          DuplicateSummaryEntry(
            team,
            score.teamScores.get(team.id),
            places.get(team.id)
          )
        }
    }

    val (pl, cr, up) = mdr match {
      case Some(r) => (r.played, r.created, r.updated)
      case None =>
        val time = SystemTime.currentTimeMillis()
        val played = if (md.created == 0) time else md.created
        (played, md.created, md.updated)
    }
    new MatchDuplicateResultV2(MatchDuplicateResult.idNul, r, None, None, None, pl, cr, up, "MP")
      .fixup

  }

}
