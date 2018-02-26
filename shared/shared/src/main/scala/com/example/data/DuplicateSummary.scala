package com.example.data

import com.example.data.SystemTime.Timestamp
import com.example.data.bridge.MatchDuplicateScore
import com.example.data.bridge.PerspectiveComplete

import io.swagger.annotations._
import scala.annotation.meta._

@ApiModel(description = "The summary of a duplicate match")
case class DuplicateSummaryEntry(
    @(ApiModelProperty @field)(value="The team", required=true)
    team: Team,
    @(ApiModelProperty @field)(value="The points the team scored", required=true)
    result: Double,
    @(ApiModelProperty @field)(value="The place the team finished in", required=true)
    place: Int
    ) {
  def id = team.id
}

@ApiModel(description = "The best match in the main store")
case class BestMatch(
    @(ApiModelProperty @field)(value="How similar the matches are", required=true)
    sameness: Double,
    @(ApiModelProperty @field)(value="The ID of the MatchDuplicate in the main store that is the best match, none if no match", required=true)
    id: Option[Id.MatchDuplicate],
    @(ApiModelProperty @field)(value="The fields that are different", required=true)
    differences: Option[List[String]]
)

object BestMatch {

  def noMatch = new BestMatch( -1, None, None )

  def apply( id: Id.MatchDuplicate, diff: Difference ) = {
    new BestMatch( diff.percentSame, Some(id), Some(diff.differences) )
  }
}

@ApiModel(description = "The summary of duplicate matches")
case class DuplicateSummary(
    @(ApiModelProperty @field)(value="The ID of the MatchDuplicate being summarized", required=true)
    id: Id.MatchDuplicate,
    @(ApiModelProperty @field)(value="True if the match is finished", required=true)
    finished: Boolean,
    @(ApiModelProperty @field)(value="The scores of the teams", required=true)
    teams: List[DuplicateSummaryEntry],
    @(ApiModelProperty @field)(value="The number of boards in the match", required=true)
    boards: Int,
    @(ApiModelProperty @field)(value="The number of tables in the match", required=true)
    tables: Int,
    @(ApiModelProperty @field)(value="True if this is only the results", required=true)
    onlyresult: Boolean,
    @(ApiModelProperty @field)(value="when the duplicate hand was created", required=true)
    created: Timestamp,
    @(ApiModelProperty @field)(value="when the duplicate hand was last updated", required=true)
    updated: Timestamp,
    @(ApiModelProperty @field)(value="the best match in the main store", required=false)
    bestMatch: Option[BestMatch] = None
    ) {

  def players() = teams.flatMap { t => Seq(t.team.player1, t.team.player2) }.toList
  def playerPlaces() = teams.flatMap{ t => Seq( (t.team.player1->t.place), (t.team.player2->t.place) ) }.toMap
  def playerScores() = teams.flatMap{ t => Seq( (t.team.player1->t.result), (t.team.player2->t.result) ) }.toMap

  def idAsDuplicateResultId = id.asInstanceOf[Id.MatchDuplicateResult]

  def containsPair( p1: String, p2: String ) = {
    teams.find { dse =>
      (dse.team.player1==p1&&dse.team.player2==p2) || (dse.team.player1==p2&&dse.team.player2==p1)
    }.isDefined
  }

  /**
   * @return true if all players played in game
   */
  def containsPlayer( name: String* ) = {
    name.find { p =>
      // return true if player did not play
      teams.find { dse =>
        // true if p is one of the players
        dse.team.player1==p || dse.team.player2==p
      }.isEmpty
    }.isEmpty
  }

  /**
   * Modify the player names according to the specified name map.
   * The timestamp is not changed.
   * @return None if the names were not changed.  Some() with the modified object
   */
  def modifyPlayers( nameMap: Map[String,String] ) = {
    val (nteams, modified) = teams.map { t =>
      t.team.modifyPlayers(nameMap) match {
        case Some(nt) => (t.copy( team=nt), true)
        case None => (t, false)
      }
    }.foldLeft( (List[DuplicateSummaryEntry](),false) ) { (ac,v) =>
        (ac._1:::List(v._1), ac._2||v._2)
    }
    if (modified) {
      Some( copy( teams=nteams ))
    } else {
      None
    }
  }
}

object DuplicateSummary {
  def create( md: MatchDuplicate ): DuplicateSummary = {
    val score = MatchDuplicateScore(md, PerspectiveComplete)
    val places = score.places.flatMap { p => p.teams.map { t => (t.id->p.place) }.toList }.toMap
    val t = md.teams.map{ team => DuplicateSummaryEntry( team, score.teamScores( team.id ), places( team.id ) ) }.toList
    DuplicateSummary( md.id, score.alldone,
                      t,
                      md.boards.size, md.teams.size/2, false,
                      md.created, md.updated )
  }

  def create( md: MatchDuplicateResult ): DuplicateSummary = {
    val mdr = md.fixPlaces()
    val boards = mdr.getBoards()
    val tables = mdr.getTables()
    DuplicateSummary( mdr.id, !mdr.notfinished.getOrElse(false), mdr.results.flatten,
                      boards, tables, true,
                      mdr.played, mdr.played )
  }
}
