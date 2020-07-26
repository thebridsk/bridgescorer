package com.github.thebridsk.bridge.fullserver.test.pages.duplicate


case class Team( teamid: Int, one: String, two: String ) {
  def toStringForScoreboard: String = if (one=="" && two=="") "" else s"${one}\n${two}"
  def toStringForPlayers: String = if (one=="" && two=="") s"${teamid}" else s"${teamid} ${one.trim()} ${two.trim()}"

  def swap: Team = copy( one=two, two=one )

  def blankNames: Team = copy( one="", two="" )
}
