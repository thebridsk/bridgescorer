package com.github.thebridsk.bridge.data.maneuvers

import com.github.thebridsk.bridge.data.bridge._

case class TableManeuvers(
    north: String,
    south: String,
    east: String,
    west: String
) {

  override def toString(): String = {
    s"TableManeuvers( $north-$south, $east-$west )"
  }

  def players: List[String] = north :: south :: east :: west :: Nil

  def sortedPlayers = players.sorted

  def isPlayerSpecified(p: String): Boolean = p != null && p.length() > 0

  def areAllPlayersValid: Boolean = {
    isPlayerSpecified(north) && isPlayerSpecified(south) && isPlayerSpecified(
      east
    ) && isPlayerSpecified(west)
  }

  def areNSPlayersValid: Boolean = {
    isPlayerSpecified(north) && isPlayerSpecified(south)
  }

  def areEWPlayersValid: Boolean = {
    isPlayerSpecified(east) && isPlayerSpecified(west)
  }

  /**
    * Returns the location of the specified player
    */
  def find(p: String): Option[PlayerPosition] = {
    p match {
      case `north` => Some(North)
      case `south` => Some(South)
      case `east`  => Some(East)
      case `west`  => Some(West)
      case _       => None
    }
  }

  def find(l: PlayerPosition): String = {
    l match {
      case North => north
      case South => south
      case East  => east
      case West  => west
    }
  }

  def isPlayerValid(l: PlayerPosition): Boolean = {
    l match {
      case North => isPlayerSpecified(north)
      case South => isPlayerSpecified(south)
      case East  => isPlayerSpecified(east)
      case West  => isPlayerSpecified(west)
    }
  }

  def partnerOfPosition(l: PlayerPosition): PlayerPosition =
    l match {
      case North => South
      case South => North
      case East  => West
      case West  => East
    }

  def leftOfPosition(l: PlayerPosition): PlayerPosition =
    l match {
      case North => East
      case South => West
      case East  => South
      case West  => North
    }

  def partnerOf(p: String): Option[String] = {
    find(p) match {
      case Some(l) => Some(partnerOf(l))
      case None    => None
    }
  }

  def partnerOf(l: PlayerPosition): String = {
    find(partnerOfPosition(l))
  }

  def leftOf(p: String): Option[String] = {
    find(p) match {
      case Some(l) => Some(leftOf(l))
      case None    => None
    }
  }

  def leftOf(l: PlayerPosition): String = {
    find(leftOfPosition(l))
  }

  def rightOf(p: String): Option[String] = {
    find(p) match {
      case Some(l) => Some(rightOf(l))
      case None    => None
    }
  }

  def rightOf(l: PlayerPosition): String = {
    find(rightOfPosition(l))
  }

  def rightOfPosition(l: PlayerPosition): PlayerPosition =
    l match {
      case North => West
      case South => East
      case East  => North
      case West  => South
    }

  def setPlayer(l: PlayerPosition, p: String): TableManeuvers = {
    l match {
      case North => copy(north = p)
      case South => copy(south = p)
      case East  => copy(east = p)
      case West  => copy(west = p)
    }
  }

  def swap(l1: PlayerPosition, l2: PlayerPosition): TableManeuvers = {
    val p1 = find(l1)
    val p2 = find(l2)
    setPlayer(l1, p2).setPlayer(l2, p1)
  }

  def swapWithPartner(pos: PlayerPosition): TableManeuvers = {
    val partner = partnerOfPosition(pos)
    swap(pos, partner)
  }

  def swapRightAndPartnerOf(l: PlayerPosition): TableManeuvers = {
    swap(rightOfPosition(l), partnerOfPosition(l))
  }

  def swapLeftAndPartnerOf(l: PlayerPosition): TableManeuvers = {
    swap(leftOfPosition(l), partnerOfPosition(l))
  }

  def swapRightAndLeftOf(l: PlayerPosition): TableManeuvers = {
    swap(rightOfPosition(l), leftOfPosition(l))
  }

  def rotateClockwise: TableManeuvers = {
    TableManeuvers(
      north = west,
      south = east,
      east = north,
      west = south
    )
  }

  def rotateCounterClockwise: TableManeuvers = {
    TableManeuvers(
      north = east,
      south = west,
      east = south,
      west = north
    )
  }

  def rotate180: TableManeuvers = {
    TableManeuvers(
      north = south,
      south = north,
      east = west,
      west = east
    )
  }
}

object TableManeuvers {}
