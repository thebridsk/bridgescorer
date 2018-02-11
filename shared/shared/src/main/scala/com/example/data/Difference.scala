package com.example.data

import scala.annotation.tailrec

class Difference(
    val same: Int = 0,
    val changes: Int = 0,
    val differences: List[String] = List()
  ) {

  def copy(
    same: Int = same,
    changes: Int = changes,
    differences: List[String] = differences
  ) = {
    new Difference(same,changes,differences)
  }

  def percentSame = {
    val sum = same+changes
    if (sum == 0) 0
    else same*100.0 / sum
  }

  def add( v: Difference ) = {
    copy(same = same+v.same,
         changes=changes+v.changes,
         differences=differences:::v.differences)
  }

  def incChange( diff: String ) = copy( changes = changes+1, differences=diff::differences )
  def incSame = copy( same = same+1 )

  override
  def toString() = {
    s"""Difference(same=$same,changes=$changes,%=${percentSame},differences=${differences.mkString("[", ",", "]")}"""
  }
}

object SameDifference extends Difference(1,0)
class ChangeDifference( diff: String) extends Difference(0,1,List(diff))

object Difference {

  def apply(
    same: Int = 0,
    changes: Int = 0,
    differences: List[String] = List()
  ) = {
    new Difference(same,changes,differences)
  }

  def unapply( v: Difference ) = (v.same,v.changes,v.differences)

  def compare[T]( me: T, other: T, diff: String ) = {
    if (me == other) SameDifference
    else new ChangeDifference(diff)
  }

  def fold( v: Difference* ) = {
    v.foldLeft(Difference())((ac,v)=>ac.add(v))
  }

  trait DifferenceComparable[I,T,W <: DifferenceComparable[I,T,W]] extends Any {
    def id: I
    def differenceW( prefix: String, other: W ): Difference
    def difference( prefix: String, other: T ): Difference
  }

  def compareList[I,T,W <: DifferenceComparable[I,T,W]](
      me: List[W],
      other: List[W],
      prefix: String
    )(
      implicit
        ordering: Ordering[I]
  ) = {
    import ordering._
    val sortedMe = me.sortWith( (m,o) => m.id < o.id)
    val sortedOther = other.sortWith( (m,o) => m.id < o.id)

    @tailrec
    def compare( m: List[W], o: List[W], diff: Difference ): Difference = {
      if (m.isEmpty) {
        val ol = o.length
        if (ol == 0) diff
        else diff.add( Difference(0,ol,List(prefix+o.map( w => w.id).mkString("(", ",", ")"))) )
      } else if (o.isEmpty) {
        val ml = m.length
        if (ml == 0) diff
        else diff.add( Difference(0,ml,List(prefix+m.map( w => w.id).mkString("(", ",", ")"))) )
      } else {
        val mh = m.head
        val oh = o.head
        if (mh.id == oh.id) {
          compare( m.tail, o.tail, diff.add( mh.differenceW(prefix+"("+mh.id+")", oh) ))
        } else if (mh.id < oh.id) {
          compare( m.tail, o, diff.add( new ChangeDifference( prefix+"("+mh.id+")") ))
        } else {
          compare( m, o.tail, diff.add( new ChangeDifference( prefix+"("+oh.id+")") ))
        }
      }
    }

    compare( sortedMe, sortedOther, Difference() )
  }
}

object DifferenceWrappers {
  import Difference._

  implicit class WrapTeam( val me: Team ) extends AnyVal with DifferenceComparable[Id.Team, Team, WrapTeam] {

    def id = me.id

    def differenceW( prefix: String, other: WrapTeam ) =
      difference( prefix, other.me )

    def difference( prefix: String, other: Team ): Difference = {
      fold(
        compare( me.id, other.id, prefix+".id" ),
        compare( me.player1, other.player1, prefix+".player1" ),
        compare( me.player2, other.player2, prefix+".player2" ),
        compare( me.created, other.created, prefix+".created" ),
        compare( me.updated, other.updated, prefix+".updated" ),
      )
    }

  }

  implicit class WrapHand( val me: Hand ) extends AnyVal with DifferenceComparable[String, Hand, WrapHand] {

    def id = me.id

    def differenceW( prefix: String, other: WrapHand ) =
      difference( prefix, other.me )

    def difference( prefix: String, other: Hand ): Difference = {
      fold(
        compare( me.id, other.id, prefix+".id" ),
        compare( me.contractTricks, other.contractTricks, prefix+".contractTricks" ),
        compare( me.contractSuit, other.contractSuit, prefix+".contractSuit" ),
        compare( me.contractDoubled, other.contractDoubled, prefix+".contractDoubled" ),
        compare( me.declarer, other.declarer, prefix+".declarer" ),
        compare( me.nsVul, other.nsVul, prefix+".nsVul" ),
        compare( me.ewVul, other.ewVul, prefix+".ewVul" ),
        compare( me.madeContract, other.madeContract, prefix+".madeContract" ),
        compare( me.tricks, other.tricks, prefix+".tricks" ),
        compare( me.created, other.created, prefix+".created" ),
        compare( me.updated, other.updated, prefix+".updated" ),
      )
    }
  }

  implicit class WrapDupHand( val me: DuplicateHand ) extends AnyVal with DifferenceComparable[Id.DuplicateHand, DuplicateHand, WrapDupHand] {

    def id = me.id

    def differenceW( prefix: String, other: WrapDupHand ) =
      difference( prefix, other.me )

    def difference( prefix: String, other: DuplicateHand ): Difference = {
      fold(
        compare( me.id, other.id, prefix+".id" ),
        compareList[String, Hand, WrapHand](me.played.map(h=>WrapHand(h)), other.played.map(h=>WrapHand(h)), prefix+".played"),
        compare( me.table, other.table, prefix+".table" ),
        compare( me.round, other.round, prefix+".round" ),
        compare( me.board, other.board, prefix+".board" ),
        compare( me.nsTeam, other.nsTeam, prefix+".nsTeam" ),
        compare( me.nIsPlayer1, other.nIsPlayer1, prefix+".nIsPlayer1" ),
        compare( me.ewTeam, other.ewTeam, prefix+".ewTeam" ),
        compare( me.eIsPlayer1, other.eIsPlayer1, prefix+".eIsPlayer1" ),
        compare( me.created, other.created, prefix+".created" ),
        compare( me.updated, other.updated, prefix+".updated" ),
      )
    }
  }

  implicit class WrapBoard( val me: Board ) extends AnyVal with DifferenceComparable[Id.DuplicateBoard, Board, WrapBoard] {

    def id = me.id

    def differenceW( prefix: String, other: WrapBoard ) =
      difference( prefix, other.me )

    def difference( prefix: String, other: Board ): Difference = {
      fold(
        compare( me.id, other.id, prefix+".id" ),
        compare( me.nsVul, other.nsVul, prefix+".nsVul" ),
        compare( me.ewVul, other.ewVul, prefix+".ewVul" ),
        compare( me.dealer, other.dealer, prefix+".dealer" ),
        compareList[Id.DuplicateHand, DuplicateHand, WrapDupHand](me.hands.map(h=>WrapDupHand(h)), other.hands.map(h=>WrapDupHand(h)), prefix+".hands" ),
        compare( me.created, other.created, prefix+".created" ),
        compare( me.updated, other.updated, prefix+".updated" ),
      )
    }

  }

  implicit class WrapMatchDuplicate( val me: MatchDuplicate ) extends AnyVal with DifferenceComparable[Id.MatchDuplicate, MatchDuplicate, WrapMatchDuplicate] {

    def id = me.id

    def differenceW( prefix: String, other: WrapMatchDuplicate ) =
      difference( prefix, other.me )

    def difference( prefix: String, other: MatchDuplicate ): Difference = {
      fold(
        compare( me.id, other.id, prefix+".id" ),
        compareList[Id.Team, Team, WrapTeam](me.teams.map(h=>WrapTeam(h)), other.teams.map(h=>WrapTeam(h)), prefix+".teams" ),
        compareList[Id.DuplicateBoard, Board, WrapBoard](me.boards.map(h=>WrapBoard(h)), other.boards.map(h=>WrapBoard(h)), prefix+".boards" ),
        compare( me.boardset, other.boardset, prefix+".boardset" ),
        compare( me.movement, other.movement, prefix+".movement" ),
        compare( me.created, other.created, prefix+".created" ),
        compare( me.updated, other.updated, prefix+".updated" ),
      )
    }

  }

  implicit class WrapDuplicateSummaryEntry( val me: DuplicateSummaryEntry ) extends AnyVal with DifferenceComparable[Id.Team, DuplicateSummaryEntry, WrapDuplicateSummaryEntry] {

    def id = me.id

    def differenceW( prefix: String, other: WrapDuplicateSummaryEntry ) =
      difference( prefix, other.me )

    def difference( prefix: String, other: DuplicateSummaryEntry ): Difference = {
      fold(
        compare( me.id, other.id, prefix+".id" ),
        me.team.difference( prefix+".team", other.team ),
        compare( me.result, other.result, prefix+".result" ),
        compare( me.place, other.place, prefix+".place" ),
      )
    }

  }

  implicit class WrapMatchDuplicateDuplicateResult( val me: MatchDuplicateResult ) extends AnyVal with DifferenceComparable[Id.MatchDuplicate, MatchDuplicateResult, WrapMatchDuplicateDuplicateResult] {

    def id = me.id

    def differenceW( prefix: String, other: WrapMatchDuplicateDuplicateResult ) =
      difference( prefix, other.me )

    def difference( prefix: String, other: MatchDuplicateResult ): Difference = {
      fold(
        compare( me.id, other.id, prefix+".id" ),
        me.results.zip(other.results).map { entry =>
          val (mer,or) = entry
          compareList[Id.Team, DuplicateSummaryEntry, WrapDuplicateSummaryEntry](mer.map(h=>WrapDuplicateSummaryEntry(h)), or.map(h=>WrapDuplicateSummaryEntry(h)), prefix+".results" )
        }.foldLeft( Difference() )( (ac,v) => ac.add(v) ),
        compare( me.comment, other.comment, prefix+".comment" ),
        compare( me.notfinished, other.notfinished, prefix+".notfinished" ),
        compare( me.created, other.created, prefix+".created" ),
        compare( me.updated, other.updated, prefix+".updated" ),
      )
    }

  }

}
