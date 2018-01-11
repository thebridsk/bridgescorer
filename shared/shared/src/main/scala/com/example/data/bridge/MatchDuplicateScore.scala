package com.example.data.bridge

import com.example.data.MatchDuplicate
import com.example.data.Id
import com.example.data.Team
import com.example.data.DuplicateHand

case class DuplicateException(message: String) extends Exception(message)

sealed trait DuplicateViewPerspective

case object PerspectiveDirector extends DuplicateViewPerspective
case class PerspectiveTable( teamId1: Id.Team, teamId2: Id.Team ) extends DuplicateViewPerspective
case object PerspectiveComplete extends DuplicateViewPerspective

class MatchDuplicateScore private ( duplicate: MatchDuplicate, val perspective: DuplicateViewPerspective ) {
  import MatchDuplicateScore._

  val id = duplicate.id

  val created = duplicate.created
  val updated = duplicate.updated

  val boards = duplicate.boards.map(b=>(b.id->BoardScore(b, perspective))).toMap

  val sortedBoards = boards.values.toList.sortWith((b1,b2)=>Id.idComparer(b1.id, b2.id)<0)

  val alldone = !boards.values.exists( b => !b.allplayed )

  val teams = duplicate.teams

  def getTeam( tid: Id.Team ) = duplicate.getTeam(tid)

  val teamScores = duplicate.teams.map( team => (team.id->{
    var points: Double = 0
    boards.values.foreach { b => { points += {
      b.scores().get(team.id) match {
        case Some(tbs) => tbs.points
        case None => 0
      }
    } } }
    points
  }) ).toMap

  val places = {
    val m = teamScores.groupBy(e => e._2).map { e =>
      val (points, teams) = e
      points->teams.keys.map( tid => getTeam(tid).get ).toList
    }
    val sorted = m.toList.sortWith((e1,e2)=> e1._1>e2._1)
    var place = 1
    sorted.map(e=>{
      val (points, ts) = e
      val p = place
      place += ts.size
      Place(p,points,ts)
    } )
  }

  def placeByWinnerSet( winnerset: List[Id.Team] ) = {
    places.flatMap( p => {
      val pteam = p.teams.filter( t => winnerset.contains(t.id))
      if (pteam.isEmpty) Nil
      else p.copy(teams=pteam)::Nil
    })
  }

  val tables: Map[String, List[Round]] = {
    import scala.collection.mutable
    val tables: mutable.Map[String, mutable.Map[Int, Round]] = mutable.Map()

    boards.foreach{ case(bid,board) =>
      board.board.hands.foreach{ hand => {
        val table = hand.table
        val roundmap = tables.get(table) match {
          case Some(map) => map
          case None =>
            val map = mutable.Map[Int, Round]()
            tables += (table->map)
            map
        }
        val round = hand.round
        val ns = duplicate.getTeam(hand.nsTeam).get
        val ew = duplicate.getTeam(hand.ewTeam).get
        val newround = roundmap.get(round) match {
          case Some(r) =>
            if (ns != r.ns || ew != r.ew) throw DuplicateException("NS and EW don't match for hand on table "+hand.table+" board "+hand.board+" with other hands in round "+round)
            r.copy( boards = (board::r.boards) )
          case None =>
            Round( table, round, ns, ew, List(board))
        }
        roundmap += (round -> newround)
      } }
    }

    tables.map{ case(table,rounds)=>{
      (table -> List( rounds.values.toList.sortWith((r1,r2)=>r1.round.compareTo(r2.round)<0).toList:_* ).map { r => r.copy( boards = r.boards.sortWith((r1,r2)=> Id.idComparer(r1.id.toString, r2.id.toString)<0) ) })
    }}.toMap
  }

  def getRound( table: String, round: Int ) = {
    tables.get(table) match {
      case Some(rounds) =>
        rounds.find { r => r.round == round }
      case None => None
    }
  }

  /**
   * Get all the table Ids in sort order.
   */
  def getTableIds() = {
    tables.keys.map { id => id.asInstanceOf[Id.Table] }.toSet.toList.sortWith { (l,r) => Id.idComparer(l,r)<0 }
  }

  def getBoardSet() = duplicate.boardset

  def getMovement() = duplicate.movement

  /**
   * Get the winner sets.  From each set a winner should be declared.
   * @return a list of the winner sets.  A winner set is a list of team Ids.
   */
  def getWinnerSets(): List[List[Id.Team]] = {
    // key is a team, value are the opponents of key
    val winnersets = scala.collection.mutable.Map[ Id.DuplicateHand, List[Id.DuplicateHand] ]()
    def addTeam( t1: Id.Team, t2: Id.Team ) = {
      val cur = winnersets.get(t1) match {
        case Some(l) => l
        case None => Nil
      }
      val next = if (cur.contains(t2)) cur else t2::cur
      winnersets.put(t1, next)
    }
    def add( h: DuplicateHand ) = {
      addTeam( h.nsTeam, h.ewTeam )
      addTeam( h.ewTeam, h.nsTeam )
    }
    duplicate.boards.foreach(b => b.hands.foreach( h => add(h)))
    def sorter(id1: Id.Team, id2:Id.Team) = Id.idComparer(id1, id2)<0
    val sets = winnersets.values.map( l => l.sortWith( sorter) ).toSeq.distinct.toList
    if (sets.size != 2) {
      List(duplicate.teams.map(t=>t.id).toList.sortWith(sorter))
    } else {
      val List(s1,s2) = sets
      if (s1.filter( k => s2.contains(k) ).isEmpty && s2.filter( k => s1.contains(k) ).isEmpty) {
        // the two lists don't have any common entries
        // check if everyone in a set was at the same position
        val allSamePos = duplicate.boards.map( b => {
          val (ns,ew) = b.hands.map( h => (h.nsTeam,h.ewTeam)).unzip
          if (ns.contains(s1.head)) {
            val s1AllNS = s1.map(p => ns.contains(p)).find( n => !n ).isEmpty
            val s2AllEW = s2.map(p => ew.contains(p)).find( n => !n ).isEmpty
            s1AllNS&&s2AllEW
          } else {
            val s2AllNS = s2.map(p => ns.contains(p)).find( n => !n ).isEmpty
            val s1AllEW = s1.map(p => ew.contains(p)).find( n => !n ).isEmpty
            s2AllNS&&s1AllEW
          }
        }).find( ok => !ok ).isEmpty

        if (allSamePos) sets
        else List(duplicate.teams.map(t=>t.id).toList.sortWith(sorter))
      } else {
        // the two sets have at least one entry in common
        List(duplicate.teams.map(t=>t.id).toList.sortWith(sorter))
      }
    }
  }
}

object MatchDuplicateScore {

  case class TeamScore( team: Id.Team, points: Double )

  case class Round( table: String, round: Int, ns: Team, ew: Team, boards: List[BoardScore] ) {
    def allUnplayedOnTable = boards.find { bs => bs.hasTeamPlayed(ns.id) }.isEmpty
    def complete = boards.filter{bs=>bs.board.getHand(ns.id).isDefined}.find{ bs => !bs.hasTeamPlayed(ns.id) }.isEmpty

    /**
     * @return a tuple 2.  The first is the played boards, the second is the unplayed boards.
     */
    def playedAndUnplayedBoards() = {
      boards.partition { b => b.hasTeamPlayed(ns.id) }
    }

    override
    def toString() = s"""[Round table=${table}, round=${round}, ns=${ns.id}, ew=${ew.id}, boards=${boards.mkString("\n  ","\n  ","")}]"""
  }

  case class Place( place: Int, score: Double, teams: List[Team] )

  def apply( duplicate: MatchDuplicate, perspective: DuplicateViewPerspective ) = new MatchDuplicateScore(duplicate, perspective)
}
