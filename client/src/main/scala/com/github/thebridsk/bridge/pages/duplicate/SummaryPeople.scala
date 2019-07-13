package com.github.thebridsk.bridge.pages.duplicate

import com.github.thebridsk.bridge.data.DuplicateSummary

object SummaryPeople {

  def apply( summaries: List[DuplicateSummary] ) = new SummaryPeople(Some(summaries))

  def apply( summaries: Option[List[DuplicateSummary]] ) = new SummaryPeople(summaries)

  case class FirstPlaces( firstPlace: Int, total: Int ) {
    override def toString() = firstPlace.toString()+"/"+total.toString()

    def pct = if (total == 0) 0 else firstPlace.toDouble / total * 100
    def pctToString() = if (total == 0) "" else Utils.toPctString(pct)
  }

  case class FirstPlacePoints( firstPlace: Double, total: Int ) {
    override def toString() = firstPlace.toString()+"/"+total.toString()

    def firstPlaceAsString = Utils.toString(firstPlace)
    def pct = if (total == 0) 0 else firstPlace.toDouble / total * 100
    def pctToString() = if (total == 0) "" else Utils.toPctString(pct)
  }

  case class PlayerScores( score: Double, total: Double ) {
    override def toString() = Utils.toPointsString(score)+"/"+total.toString()
    def pct = if (total == 0) 0 else score / total * 100
    def scoreToString() = Utils.toPointsString(score)
    def pctToString() = if (total == 0) "" else Utils.toPctString(pct)
  }

}

class SummaryPeople( osummaries: Option[List[DuplicateSummary]] ) {
  import SummaryPeople._

  def summaries = osummaries.getOrElse(List())

  def isData = osummaries.isDefined

  val allPlayers = summaries.flatMap(_.players()).toSet.toList.sortWith((p1,p2)=>p1<p2)
  val firstPlaces = allPlayers.map { p => (p-> {
    var total = 0
    val fp = summaries.filter { ds =>
      ds.finished && (ds.playerPlaces().get(p) match {
      case Some(place) =>
        total += 1
        place==1
      case _ => false
    })}.length
    FirstPlaces(fp,total)
  })}.toMap
  val playerScores = allPlayers.map { p => (p-> {
    var total = 0.0
    val fp = summaries.filter {_.finished}.map { ds =>
      ds.playerScores().get(p) match {
      case Some(place) =>
        total += ds.boards*(ds.tables-1)
        place
      case _ => 0.0
    }}.foldLeft(0.0)(_ + _)
    PlayerScores(fp,total)
  })}.toMap
  val firstPlacePoints = allPlayers.map { p => (p-> {
    var total = 0
    val fp = summaries.flatMap { ds =>
      if (ds.finished) {
        val playerPlacesMap = ds.playerPlaces()
        val nWinners = playerPlacesMap.filter( e => e._2==1 ).size/2
        playerPlacesMap.get(p) match {
          case Some(place) =>
            total += 1
            (if (place==1) 1.0/nWinners else 0.0)::Nil
          case _ => Nil
        }
      } else {
        Nil
      }
    }.foldLeft(0.0) { (ac,l) => ac+l }
    FirstPlacePoints(fp,total)
  })}.toMap

}
