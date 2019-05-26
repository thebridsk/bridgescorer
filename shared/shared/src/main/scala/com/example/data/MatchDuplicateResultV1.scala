package com.example.data

import com.example.data.SystemTime.Timestamp

import scala.annotation.meta._
import com.example.data.bridge.MatchDuplicateScore
import com.example.data.bridge.PerspectiveComplete
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema

@Schema(
    title = "BoardTeamResults - team results on board.",
    description="The results of a team when they played the board."
)
case class BoardTeamResults(
    @Schema(description="The id of the team", required=true)
    team: Id.Team,
    @Schema(description="The number of points the team got playing the board", required=true)
    points: Double )

@Schema(
    title = "BoardResults - the results of a board.",
    description = "The results of a board in a Duplicate results object.")
case class BoardResults(
    @Schema(description="The board", required=true)
    board: Int,
    @ArraySchema(
        minItems=0,
        uniqueItems=true,
        schema=new Schema(
            implementation=classOf[BoardTeamResults],
        ),
        arraySchema = new Schema( description = "The played hands in the round.", required=true)
    )
    points: List[BoardTeamResults] )

@Schema(name="MatchDuplicateResult",
        description = "A hand from a duplicate match.  On input, the place field in DuplicateSummaryEntry is ignored.  If boardresults is specified, then the result field in DuplicateSummaryEntry is also ignored on input."
)
case class MatchDuplicateResultV1 private(
    @Schema(description="The ID of the MatchDuplicate", required=true)
    id: Id.MatchDuplicateResult,
    @Schema(description="The results of the match, a list of winnersets."
                        +"  Each winnerset is a list of DuplicateSummaryEntry objects",
            required=true)
    results: List[List[DuplicateSummaryEntry]],
    @Schema(description="The board scores of the teams, a list of BoardResults objects", required=false)
    boardresults: Option[List[BoardResults]],
    @Schema(description="a comment", required=false, `type`="string")
    comment: Option[String],
    @Schema(description="True if the match is not finished, default is false", required=false, `type`="boolean")
    notfinished: Option[Boolean],
    @Schema(description="when the duplicate match was played", required=true)
    played: Timestamp,
    @Schema(description="When the duplicate match was created, in milliseconds since 1/1/1970 UTC", required=true)
    created: Timestamp,
    @Schema(description="When the duplicate match was last updated, in milliseconds since 1/1/1970 UTC", required=true)
    updated: Timestamp
  ) extends VersionedInstance[MatchDuplicateResult,  MatchDuplicateResultV1,String] {

  def equalsIgnoreModifyTime( other: MatchDuplicateResultV1, throwit: Boolean = false ) = id==other.id &&
                                               equalsInResults(other,throwit)

  def equalsInResults( other: MatchDuplicateResultV1, throwit: Boolean = false ) = {
    if (results.length == other.results.length) {
      results.zip(other.results).map { e =>
        val(left,right) = e
        left.find { t1 =>
          // this function must return true if t1 is NOT in other.team
          val rc = !right.contains(t1)
          if (rc&&throwit) throw new Exception("MatchDuplicateResultV1 other did not have result equal to: "+t1)
          rc
        }.isEmpty
      }.foldLeft(true)((ac,x)=> ac&&x)
    } else {
      if (throwit) throw new Exception("MatchDuplicateResultV1 results don't winner sets: "+results+" "+other.results)
      false
    }
  }

  def setId( newId: Id.MatchDuplicate, forCreate: Boolean, dontUpdateTime: Boolean = false ) = {
    if (dontUpdateTime) {
      copy(id=newId )
    } else {
      val time = SystemTime.currentTimeMillis()
      copy(id=newId, /* created=if (forCreate) time; else created, */ updated=time)
    }
  }

  def copyForCreate(id: Id.MatchDuplicate) = {
    val time = SystemTime.currentTimeMillis()
    copy( id=id, created=time, updated=time )

  }

  @Schema(hidden = true)
  def getTables(): Int = {
    results.flatten.length/2
  }

  @Schema(hidden = true)
  def getBoards(): Int = {
    val nTables = getTables
    val pointsPerBoard = nTables*(nTables-1)
    if (pointsPerBoard == 0) 0
    else getTotalPoints/pointsPerBoard
  }

  @Schema(hidden = true)
  def getTotalPoints(): Int = {
    results.flatten.map{ r => r.result.getOrElse(0.0) }.foldLeft(0.0)((ac,v)=>ac+v).toInt
  }

  def fixPlaces() = {
    val places = results.map { winnerset =>
      val m = winnerset.groupBy(e => e.result.getOrElse(0.0)).map { e =>
        val (points, teams) = e
        points->teams
      }
      val sorted = m.toList.sortWith((e1,e2)=> e1._1>e2._1)
      var place = 1
      sorted.flatMap{ e=>
        val (points, ts) = e
        val fixed = ts.map( e => e.copy(place=Some(place)))
        place += ts.size
        fixed
      }
    }
    copy(results=places)
  }

  @Schema(hidden = true)
  def fixupSummary() = {
    boardresults match {
      case Some(l) =>
        this
      case None =>
        this
    }
  }

  @Schema(hidden = true)
  def fixup() = {
    fixupSummary().fixPlaces()
  }

  @Schema(hidden = true)
  def getWinnerSets: List[List[Id.Team]] = {
    results.map( l => l.map( e => e.team.id))
  }

  @Schema(hidden = true)
  def placeByWinnerSet(winnerset: List[Id.Team]): List[MatchDuplicateScore.Place] = {
    results.find( ws => ws.find( e => !winnerset.contains(e.team.id)).isEmpty) match {
      case Some(rws) =>
        rws.groupBy(e => e.place.getOrElse(0)).map { arg =>
          val (place, list) = arg
          val pts = list.head.result.getOrElse(0.0)
          val teams = list.map(dse => dse.team)
          MatchDuplicateScore.Place(place,pts,teams)
        }.toList
      case None =>
        throw new Exception(s"could not find winner set ${winnerset} in ${results}")
    }
  }

  /**
   * Modify the player names according to the specified name map.
   * The timestamp is not changed.
   * @return None if the names were not changed.  Some() with the modified object
   */
  def modifyPlayers( nameMap: Map[String,String] ) = {
    val (nresults, modified) = results.map { ws =>
      ws.map { t =>
        t.team.modifyPlayers(nameMap) match {
          case Some(nt) => (t.copy( team=nt), true)
          case None => (t, false)
        }
      }.foldLeft( (List[DuplicateSummaryEntry](),false) ) { (ac,v) =>
        (ac._1:::List(v._1), ac._2||v._2)
      }
    }.foldLeft( (List[List[DuplicateSummaryEntry]](),false) ) { (ac,v) =>
      (ac._1:::List(v._1), ac._2||v._2)
    }
    if (modified) {
      Some( copy( results=nresults ))
    } else {
      None
    }
  }

  def convertToCurrentVersion() = {
    val r = results.map { list =>
      list.map { dse =>
        dse.copy( result = dse.result.map( v => v*2 ) )
      }
    }
    val br = boardresults.map { list =>
      list.map { b =>
        b.copy( points = b.points.map( t => t.copy( points = t.points*2) ) )
      }
    }
    (false,MatchDuplicateResultV2(id,r,br,comment,notfinished,played,created,updated,MatchDuplicate.MatchPoints))
  }

  def readyForWrite() = this

}

object MatchDuplicateResultV1 {

  def apply(
    id: Id.MatchDuplicateResult,
    results: List[List[DuplicateSummaryEntry]],
    boardresults: Option[List[BoardResults]],
    comment: Option[String],
    notfinished: Boolean,
    played: Timestamp,
    created: Timestamp,
    updated: Timestamp
  ) = {
      new MatchDuplicateResultV1(id,results,boardresults,comment,Some(notfinished),played,created,updated).fixup()
  }

  def apply(
    id: Id.MatchDuplicateResult,
    results: List[List[DuplicateSummaryEntry]],
    boardresults: List[BoardResults],
    comment: Option[String],
    notfinished: Boolean,
    played: Timestamp,
    created: Timestamp,
    updated: Timestamp
  ) = {
      new MatchDuplicateResultV1(id,results,Option(boardresults),comment,Some(notfinished),played,created,updated).fixup()
  }

  def apply(
    id: Id.MatchDuplicateResult,
    results: List[List[DuplicateSummaryEntry]],
    played: Timestamp,
    created: Timestamp,
    updated: Timestamp
  ) = {
      new MatchDuplicateResultV1(id,results,None,None,None,played,created,updated).fixup()
  }

  def create( id: Id.MatchDuplicateResult = "" ) = {
    val time = SystemTime.currentTimeMillis()
    new MatchDuplicateResultV1(id,List(),None,None,None,time,time,time).fixup()
  }

  def createFrom( md: MatchDuplicate, mdr: Option[MatchDuplicateResult] = None ) = {
    val score = MatchDuplicateScore(md, PerspectiveComplete)
    val wss = score.getWinnerSets()
    val places = score.places.flatMap { p => p.teams.map { t => (t.id->p.place) }.toList }.toMap
    val r = wss.map { ws =>
      ws.map { tid => score.getTeam(tid).get }.map { team =>
        DuplicateSummaryEntry( team, score.teamScores.get( team.id ), places.get( team.id ))
      }
    }

    val (pl,cr,up) = mdr match {
      case Some(r) => (r.played, r.created, r.updated )
      case None =>
        val time = SystemTime.currentTimeMillis()
        val played = if (md.created == 0) time else md.created
        (played,md.created,md.updated)
    }
    new MatchDuplicateResultV1("",r,None,None,None,pl,cr,up).fixup()

  }
}
