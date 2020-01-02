package com.github.thebridsk.bridge.fullserver.test.pages.duplicate


case class Team( teamid: Int, one: String, two: String ) {
  def toStringForScoreboard = if (one=="" && two=="") "" else s"${one}\n${two}"
  def toStringForPlayers = if (one=="" && two=="") s"${teamid}" else s"${teamid} ${one.trim()} ${two.trim()}"

  def swap = copy( one=two, two=one )

  def blankNames = copy( one="", two="" )
}
