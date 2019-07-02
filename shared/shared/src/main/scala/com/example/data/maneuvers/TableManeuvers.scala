package com.example.data.maneuvers

import com.example.data.bridge._

case class TableManeuvers(
    north: String,
    south: String,
    east: String,
    west: String
) {
  import TableManeuvers._

  override def toString() = {
    s"TableManeuvers( $north-$south, $east-$west )"
  }

  def players() = north :: south :: east :: west :: Nil

  def sortedPlayers() = players.sorted

  def isPlayerSpecified(p: String) = p != null && p.length() > 0

  def areAllPlayersValid() = {
    isPlayerSpecified(north) && isPlayerSpecified(south) && isPlayerSpecified(
      east
    ) && isPlayerSpecified(west)
  }

  def areNSPlayersValid() = {
    isPlayerSpecified(north) && isPlayerSpecified(south)
  }

  def areEWPlayersValid() = {
    isPlayerSpecified(east) && isPlayerSpecified(west)
  }

  /**
    * Returns the location of the specified player
    */
  def find(p: String) = {
    p match {
      case `north` => Some(North)
      case `south` => Some(South)
      case `east`  => Some(East)
      case `west`  => Some(West)
      case _       => None
    }
  }

  def find(l: PlayerPosition) = {
    l match {
      case North => north
      case South => south
      case East  => east
      case West  => west
    }
  }

  def isPlayerValid(l: PlayerPosition) = {
    l match {
      case North => isPlayerSpecified(north)
      case South => isPlayerSpecified(south)
      case East  => isPlayerSpecified(east)
      case West  => isPlayerSpecified(west)
    }
  }

  def partnerOfPosition(l: PlayerPosition): PlayerPosition = l match {
    case North => South
    case South => North
    case East  => West
    case West  => East
  }

  def leftOfPosition(l: PlayerPosition): PlayerPosition = l match {
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

  def rightOfPosition(l: PlayerPosition): PlayerPosition = l match {
    case North => West
    case South => East
    case East  => North
    case West  => South
  }

  def setPlayer(l: PlayerPosition, p: String) = {
    l match {
      case North => copy(north = p)
      case South => copy(south = p)
      case East  => copy(east = p)
      case West  => copy(west = p)
    }
  }

  def swap(l1: PlayerPosition, l2: PlayerPosition) = {
    val p1 = find(l1)
    val p2 = find(l2)
    setPlayer(l1, p2).setPlayer(l2, p1)
  }

  def swapWithPartner(pos: PlayerPosition) = {
    val partner = partnerOfPosition(pos)
    swap(pos, partner)
  }

  def swapRightAndPartnerOf(l: PlayerPosition) = {
    swap(rightOfPosition(l), partnerOfPosition(l))
  }

  def swapLeftAndPartnerOf(l: PlayerPosition) = {
    swap(leftOfPosition(l), partnerOfPosition(l))
  }

  def swapRightAndLeftOf(l: PlayerPosition) = {
    swap(rightOfPosition(l), leftOfPosition(l))
  }

}

object TableManeuvers {}
