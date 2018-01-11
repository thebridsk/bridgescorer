package com.example.manualtest

import utils.main.Main
import com.example.test.selenium.Duplicate5TestPages
import com.example.test.selenium.DuplicateTestPages
import java.io.StringWriter
import java.io.PrintWriter
import utils.logging.Logger

object TestAllHands extends Main {

  val log = Logger("TestAllHands")

  def execute() = {

    try {
      val allHands = Duplicate5TestPages.allHands

      val boards = allHands.getBoardsInTableRound(1, 1)
      log.info( "Boards played on table 1 round 1: "+boards )

      boards.foreach( b => log.info("  "+allHands.getBoard(1, 1, b)))

      boards.foreach{ b =>
        val teamDidNotPlayBoard = allHands.getTeamsThatDontPlayBoard(b)
        log.info( s"Teams that did not play board ${b}: ${teamDidNotPlayBoard}" )

        val teamsPlayBoard = allHands.getTeamsThatPlayBoard(b)
        log.info( s"Teams that play board ${b}: ${teamsPlayBoard}" )

        val hands = allHands.getHandsOnBoards(b)
        log.info( s"Hands on board ${b}:" )
        hands.foreach( h => log.info("  "+h))
      }

      val didnotplay = allHands.getTeamThatDidNotPlay(1, 1)
      log.info( s"""Did not play ${didnotplay}""" )

    } catch {
      case x: Throwable =>
        log.info("Error "+x)
        val sw = new StringWriter
        x.printStackTrace( new PrintWriter(sw))
        val t = sw.toString()
        log.info(t)
    }
    0
  }
}
