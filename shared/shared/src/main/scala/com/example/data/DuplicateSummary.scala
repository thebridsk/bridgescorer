package com.example.data

import com.example.data.SystemTime.Timestamp
import com.example.data.bridge.MatchDuplicateScore
import com.example.data.bridge.PerspectiveComplete

import io.swagger.annotations._
import scala.annotation.meta._

@ApiModel(description = "Details about a team in a match")
case class DuplicateSummaryDetails(
    @(ApiModelProperty @field)(value="The team", required=true)
    team: Id.Team,
    @(ApiModelProperty @field)(value="The number of times the team was declarer", required=true)
    declarer: Int = 0,
    @(ApiModelProperty @field)(value="The number of times the team made the contract as declarer", required=true)
    made: Int = 0,
    @(ApiModelProperty @field)(value="The number of times the team went down as declarer", required=true)
    down: Int = 0,
    @(ApiModelProperty @field)(value="The number of times the team defended the contract", required=true)
    defended: Int = 0,
    @(ApiModelProperty @field)(value="The number of times the team took down the contract as defenders", required=true)
    tookDown: Int = 0,
    @(ApiModelProperty @field)(value="The number of times the team allowed the contract to be made as defenders", required=true)
    allowedMade: Int = 0,
    @(ApiModelProperty @field)(value="The number of times the team passed out a game", required=true)
    passed: Int = 0
  ) {

  def add( v: DuplicateSummaryDetails ) = {
    copy( declarer=declarer+v.declarer, made=made+v.made, down=down+v.down, defended=defended+v.defended, tookDown=tookDown+v.tookDown, allowedMade=allowedMade+v.allowedMade, passed=passed+v.passed)
  }

  def percentMade = made*100.0/declarer
  def percentDown = down*100.0/declarer
  def percentAllowedMade = allowedMade*100.0/defended
  def percentTookDown = tookDown*100.0/defended

  def percentDeclared = declarer*100.0/(declarer+defended+passed)
  def percentDefended = defended*100.0/(declarer+defended+passed)
  def percentPassed = passed*100.0/(declarer+defended+passed)

  def total = declarer+defended+passed
}

object DuplicateSummaryDetails {
  def zero( team: Id.Team ) = new DuplicateSummaryDetails( team )
  def passed( team: Id.Team ) = new DuplicateSummaryDetails( team, passed = 1 )
  def made( team: Id.Team ) = new DuplicateSummaryDetails( team, declarer = 1, made = 1 )
  def down( team: Id.Team ) = new DuplicateSummaryDetails( team, declarer = 1, down = 1 )
  def allowedMade( team: Id.Team ) = new DuplicateSummaryDetails( team, defended = 1, allowedMade = 1 )
  def tookDown( team: Id.Team ) = new DuplicateSummaryDetails( team, defended = 1, tookDown = 1 )
}

@ApiModel(description = "The summary of a duplicate match")
case class DuplicateSummaryEntry(
    @(ApiModelProperty @field)(value="The team", required=true)
    team: Team,
    @(ApiModelProperty @field)(value="The points the team scored", required=false)
    result: Option[Double],
    @(ApiModelProperty @field)(value="The place the team finished in", required=false)
    place: Option[Int],
    @(ApiModelProperty @field)(value="Details about the team", required=false)
    details: Option[DuplicateSummaryDetails] = None,
    @(ApiModelProperty @field)(value="The IMPs the team scored", required=false)
    resultImp: Option[Double] = None,
    @(ApiModelProperty @field)(value="The place using IMPs the team finished in", required=false)
    placeImp: Option[Int] = None
    ) {
  def id = team.id

  def getResultMp = result.getOrElse(0.0)
  def getPlaceMp = place.getOrElse(1)

  def getResultImp = resultImp.getOrElse(0.0)
  def getPlaceImp = placeImp.getOrElse(1)

  def hasImp = resultImp.isDefined&&placeImp.isDefined
  def hasMp = result.isDefined&&place.isDefined
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
    bestMatch: Option[BestMatch] = None,
    @(ApiModelProperty @field)(value="the scoring method used, default is MP", allowableValues="MP, IMP",  required=false)
    scoringmethod: Option[String] = None
    ) {

  def players() = teams.flatMap { t => Seq(t.team.player1, t.team.player2) }.toList
  def playerPlaces() = teams.flatMap{ t => Seq( (t.team.player1->t.getPlaceMp), (t.team.player2->t.getPlaceMp) ) }.toMap
  def playerScores() = teams.flatMap{ t => Seq( (t.team.player1->t.getResultMp), (t.team.player2->t.getResultMp) ) }.toMap
  def playerPlacesImp() = teams.flatMap{ t => Seq( (t.team.player1->t.getPlaceImp), (t.team.player2->t.getPlaceImp) ) }.toMap
  def playerScoresImp() = teams.flatMap{ t => Seq( (t.team.player1->t.getResultImp), (t.team.player2->t.getResultImp) ) }.toMap

  def hasMpScores = teams.headOption.map( t => t.place.isDefined&&t.result.isDefined ).getOrElse(false)
  def hasImpScores = teams.headOption.map( t => t.placeImp.isDefined&&t.resultImp.isDefined ).getOrElse(false)

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

  import MatchDuplicateV3._
  def isMP = scoringmethod.map { sm => sm == MatchPoints }.getOrElse(true)
  def isIMP = scoringmethod.map { sm => sm == InternationalMatchPoints }.getOrElse(false)

}

object DuplicateSummary {
  def create( md: MatchDuplicate ): DuplicateSummary = {
    val score = MatchDuplicateScore(md, PerspectiveComplete)
    val places = score.places.flatMap { p => p.teams.map { t => (t.id->p.place) }.toList }.toMap
    val placesImps = score.placesImps.flatMap { p => p.teams.map { t => (t.id->p.place) }.toList }.toMap
    val details = score.getDetails().map { d => d.team -> d }.toMap
    val t = md.teams.map{ team => DuplicateSummaryEntry(
                                     team,
                                     Some(score.teamScores( team.id )),
                                     Some(places( team.id )),
                                     details.get(team.id),
                                     score.teamImps.get( team.id ),
                                     placesImps.get( team.id )
                                   ) }.toList
    DuplicateSummary( md.id, score.alldone,
                      t,
                      md.boards.size, md.teams.size/2, false,
                      md.created, md.updated, None, md.scoringmethod )
  }

  def create( md: MatchDuplicateResult ): DuplicateSummary = {
    val mdr = md.fixPlaces()
    val boards = mdr.getBoards()
    val tables = mdr.getTables()
    DuplicateSummary( mdr.id,
                      !mdr.notfinished.getOrElse(false),
                      mdr.results.flatten,
                      boards,
                      tables,
                      true,
                      mdr.played,
                      mdr.played,
                      None,
                      Some(md.scoringmethod) )
  }
}
