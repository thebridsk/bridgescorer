package com.example.data

object Id {
  type MatchDuplicateResult = String
  type MatchDuplicate = String
  type MatchChicago = String
  type DuplicateBoard = String
  type DuplicateHand = String
  type Team = String
  type Table = String

  private val idpattern = "[a-zA-Z]*(\\d+)".r
  def idComparer( id1: String, id2: String ): Int = {
    val idpattern(s1) = id1
    val idpattern(s2) = id2

    s1.toInt.compareTo(s2.toInt)
  }

  def duplicateIdToNumber( id: Id.MatchDuplicate ) = id match {
    case idpattern( bn ) => bn
    case _ => id
  }

  def boardIdToBoardNumber( id: Id.DuplicateBoard ) = id match {
    case idpattern( bn ) => bn
    case _ => id
  }
  def teamIdToTeamNumber( id: Id.Team ) = id match {
    case idpattern( bn ) => bn
    case _ => id
  }
  def tableIdToTableNumber( id: Id.Table ) = id match {
    case idpattern( bn ) => bn
    case _ => id
  }

  def genericIdToNumber( id: String ) = id match {
    case idpattern( bn ) => bn
    case _ => id
  }

  def boardNumberToId( n: Int ): Id.DuplicateBoard = {
    "B"+n
  }

  def isValidIdPattern( s: String ): Boolean = s match {
    case idpattern( n ) => true
    case _ => false
  }
}

object IdOrdering {

  trait IdOrderingBase[T] extends Ordering[T] {

    def compare(x: T, y: T): Int = {
      Id.idComparer(x.toString(), y.toString())
    }

  }

  implicit object IDMatchDuplicateOrdering extends IdOrderingBase[Id.MatchDuplicate]

  implicit object IDMatchDuplicateResultOrdering extends IdOrderingBase[Id.MatchDuplicateResult]

  implicit object IDMatchChicagoOrdering extends IdOrderingBase[Id.MatchChicago]

}
