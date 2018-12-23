package com.example.data

import com.example.data.SystemTime.Timestamp

import io.swagger.annotations._
import scala.annotation.meta._
import com.example.data.bridge.MatchDuplicateScore
import com.example.data.bridge.PerspectiveComplete

@ApiModel(value="MatchDuplicateResult",
          description = "A hand from a duplicate match."
                        +"  On input, the place field in DuplicateSummaryEntry is ignored."
                        +"  If boardresults is specified, then the result field in DuplicateSummaryEntry is also ignored on input."
         )
case class MatchDuplicateResultV2 private(
    @(ApiModelProperty @field)(value="The ID of the MatchDuplicate", required=true)
    id: Id.MatchDuplicateResult,
    @(ApiModelProperty @field)(value="The results of the match, a list of winnersets."
                                    +"  Each winnerset is a list of DuplicateSummaryEntry objects",
                               required=true)
    results: List[List[DuplicateSummaryEntry]],
    @(ApiModelProperty @field)(value="The board scores of the teams, a list of BoardResults objects", required=false)
    boardresults: Option[List[BoardResults]],
    @(ApiModelProperty @field)(value="a comment", required=false)
    comment: Option[String],
    @(ApiModelProperty @field)(value="True if the match is not finished, default is false", required=false)
    notfinished: Option[Boolean],
    @(ApiModelProperty @field)(value="when the duplicate match was played", required=true)
    played: Timestamp,
    @(ApiModelProperty @field)(value="when the duplicate match was created", required=true)
    created: Timestamp,
    @(ApiModelProperty @field)(value="when the duplicate match was last updated", required=true)
    updated: Timestamp,
    @(ApiModelProperty @field)(value="the scoring method used", allowableValues="MP, IMP",  required=true)
    scoringmethod: String

  ) extends VersionedInstance[MatchDuplicateResult,  MatchDuplicateResultV2,String] {

  def equalsIgnoreModifyTime( other: MatchDuplicateResultV2, throwit: Boolean = false ) = id==other.id &&
                                               equalsInResults(other,throwit)

  def equalsInResults( other: MatchDuplicateResultV2, throwit: Boolean = false ) = {
    if (results.length == other.results.length) {
      results.zip(other.results).map { e =>
        val(left,right) = e
        left.find { t1 =>
          // this function must return true if t1 is NOT in other.team
          val rc = !right.contains(t1)
          if (rc&&throwit) throw new Exception("MatchDuplicateResultV2 other did not have result equal to: "+t1)
          rc
        }.isEmpty
      }.foldLeft(true)((ac,x)=> ac&&x)
    } else {
      if (throwit) throw new Exception("MatchDuplicateResultV2 results don't winner sets: "+results+" "+other.results)
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

  @ApiModelProperty(hidden = true)
  def getTables(): Int = {
    results.flatten.length/2
  }

  @ApiModelProperty(hidden = true)
  def getBoards(): Int = {
    val nTables = getTables
    val pointsPerBoard = nTables*(nTables-1)
    if (pointsPerBoard == 0) 0
    else getTotalPoints/pointsPerBoard
  }

  @ApiModelProperty(hidden = true)
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

  def fixPlacesImp() = {
    val places = results.map { winnerset =>
      val m = winnerset.groupBy(e => e.resultImp.getOrElse(0.0)).map { e =>
        val (points, teams) = e
        points->teams
      }
      val sorted = m.toList.sortWith((e1,e2)=> e1._1>e2._1)
      var place = 1
      sorted.flatMap{ e=>
        val (points, ts) = e
        val fixed = ts.map( e => e.copy(placeImp=Some(place)))
        place += ts.size
        fixed
      }
    }
    copy(results=places)
  }

  @ApiModelProperty(hidden = true)
  def fixupSummary() = {
    boardresults match {
      case Some(l) =>
        this
      case None =>
        this
    }
  }

  @ApiModelProperty(hidden = true)
  def fixup() = {
    fixupSummary().fixPlaces().fixPlacesImp()
  }

  @ApiModelProperty(hidden = true)
  def getWinnerSets: List[List[Id.Team]] = {
    results.map( l => l.map( e => e.team.id))
  }

  @ApiModelProperty(hidden = true)
  def placeByWinnerSet(winnerset: List[Id.Team]): List[MatchDuplicateScore.Place] = {
    results.find( ws => ws.find( e => !winnerset.contains(e.team.id)).isEmpty) match {
      case Some(rws) =>
        rws.groupBy(e => e.place.getOrElse(1)).map { arg =>
          val (place, list) = arg
          val pts = list.head.result.getOrElse(0.0)
          val teams = list.map(dse => dse.team)
          MatchDuplicateScore.Place(place,pts,teams)
        }.toList
      case None =>
        throw new Exception(s"could not find winner set ${winnerset} in ${results}")
    }
  }

  @ApiModelProperty(hidden = true)
  def placeByWinnerSetIMP(winnerset: List[Id.Team]): List[MatchDuplicateScore.Place] = {
    results.find( ws => ws.find( e => !winnerset.contains(e.team.id)).isEmpty) match {
      case Some(rws) =>
        rws.groupBy(e => e.placeImp.getOrElse(1)).map { arg =>
          val (place, list) = arg
          val pts = list.head.resultImp.getOrElse(0.0)
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

  import MatchDuplicateV3._
  def isMP = scoringmethod == MatchPoints
  def isIMP = scoringmethod == InternationalMatchPoints

  def convertToCurrentVersion() =
    this

  def readyForWrite() = this

}

object MatchDuplicateResultV2 {

  def apply(
    id: Id.MatchDuplicateResult,
    results: List[List[DuplicateSummaryEntry]],
    boardresults: Option[List[BoardResults]],
    comment: Option[String],
    notfinished: Boolean,
    scoringmethod: String,
    played: Timestamp,
    created: Timestamp,
    updated: Timestamp
  ) = {
      new MatchDuplicateResultV2(id,results,boardresults,comment,Some(notfinished),played,created,updated,scoringmethod).fixup()
  }

  def apply(
    id: Id.MatchDuplicateResult,
    results: List[List[DuplicateSummaryEntry]],
    boardresults: List[BoardResults],
    comment: Option[String],
    notfinished: Boolean,
    scoringmethod: String,
    played: Timestamp,
    created: Timestamp,
    updated: Timestamp
  ) = {
      new MatchDuplicateResultV2(id,results,Option(boardresults),comment,Some(notfinished),played,created,updated,scoringmethod).fixup()
  }

  def apply(
    id: Id.MatchDuplicateResult,
    results: List[List[DuplicateSummaryEntry]],
    scoringmethod: String,
    played: Timestamp,
    created: Timestamp,
    updated: Timestamp
  ) = {
      new MatchDuplicateResultV2(id,results,None,None,None,played,created,updated,scoringmethod).fixup()
  }

  def create( id: Id.MatchDuplicateResult = "", scoringmethod: String = "MP" ) = {
    val time = SystemTime.currentTimeMillis()
    new MatchDuplicateResultV2(id,List(),None,None,None,time,time,time, scoringmethod).fixup()
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
    new MatchDuplicateResultV2("",r,None,None,None,pl,cr,up,"MP").fixup()

  }
}
