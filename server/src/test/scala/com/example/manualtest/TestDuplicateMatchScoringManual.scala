package com.example.manualtest

import utils.main.Main
import com.example.data.Id
import com.example.data.test.TestMatchDuplicate
import com.example.data.MatchDuplicate
import com.example.data.bridge.MatchDuplicateScore
import com.example.data.bridge.BoardScore
import com.example.backend.BridgeService
import com.example.data.bridge.PerspectiveDirector
import com.example.data.bridge.PerspectiveTable
import com.example.data.bridge.PerspectiveComplete
import com.example.backend.BridgeServiceInMemory
import scala.concurrent.Await
import scala.concurrent.duration._

object TestDuplicateMatchScoringManual extends Main {

  def execute() = {
    val m = TestMatchDuplicate.getPlayedMatch("M1")

    check( m, "T1", "T2" )
    check( m, "T3", "T4" )
    println("")
    check( m, "T1", "T3" )
    check( m, "T2", "T4" )
    println("")
    check( m, "T1", "T4" )
    check( m, "T2", "T3" )
    println("")
    checkDirector(m)
    println("")
    checkComplete(m)

    println("")
    Await.result( new BridgeServiceInMemory("test").fillBoards(MatchDuplicate.create("M2")), 30.seconds) match {
      case Right(md) =>
        check(md, "T1", "T2" )
        showTables(md)
        0
      case Left((code,msg)) =>
        println( "Did not fill boards: "+code+" "+msg.msg )
        1
    }
  }

  def teamScores(boards: List[BoardScore], team: Id.Team) = {
    boards.map( board => {
                            val ts = board.scores().get(team).get
                            if (ts.played) {
                              if (ts.hidden) {
                                "played"
                              } else {
                                "%3.1f".format( ts.points )
                              }
                            } else {
                              ""
                            }
                        }  )
  }

  def checkDirector( m: MatchDuplicate ) = {
    println("From Director" )
    val score = MatchDuplicateScore(m, PerspectiveDirector)
    check(m,score)
  }

  def checkComplete( m: MatchDuplicate ) = {
    println("From Complete" )
    val score = MatchDuplicateScore(m, PerspectiveComplete)
    check(m,score)
  }

  def check( m: MatchDuplicate, team1: Id.Team, team2: Id.Team ): Unit = {
    println("From teams "+team1+" and "+team2 )
    val score = MatchDuplicateScore(m, PerspectiveTable(team1, team2))
    check(m,score)
  }

  def check( m: MatchDuplicate, score: MatchDuplicateScore ): Unit = {
    val teams = m.teams.map{t=>t.id}.toList.sortWith((one,two)=> Id.idComparer(one,two)<0)
    val boards = score.boards.values.toList.sortWith((one,two)=> Id.idComparer(one.id,two.id)<0)

    val header = List( List("team","total"), boards.map( b => b.id.toString ).toList ).flatten

    val body = teams.map( team =>
                  List( List(team.toString(), score.teamScores.get(team).get.toString),
                        teamScores(boards,team) ).flatten
                )
    printTable(header,body)
    println("")

    val pheader = List( "Place", "Points", "Players" )
    val pbody = score.places.map { place => List( place.place.toString(), place.score.toString(), place.teams.mkString(", ")) }
    printTable( pheader, pbody)

    println("")

    boards.foreach { b => checkBoard(b,teams) }
  }

  def checkBoard( board: BoardScore, teams: List[Id.Team] ) = {
    println("Board: "+board.id)

    val header = List("NS", "Contract", "By", "Made", "Down", "NSScore", "EWScore", "EW", "MatchPoints")
    val body = teams.map { team => {
      val teamscore = board.scores().get(team).get
      if (teamscore.isNS) {
        // NS
        if (teamscore.played) {
          if (teamscore.hidden) {
            List(teamscore.teamId.toString, "played", "?", "?", "?", "?", "--", teamscore.opponent.get.toString, "?")
          } else {
            val contract = teamscore.contract.get
            List(teamscore.teamId.toString, contract.contract, contract.declarer, contract.made.getOrElse(""), contract.down.getOrElse(""), teamscore.score.toString, "--", teamscore.opponent.get.toString, teamscore.points.toString)
          }
        } else {
          List(teamscore.teamId.toString, "", "", "", "", "", "--", teamscore.opponent.get.toString, "")
        }
      } else {
        // EW
        if (teamscore.played) {
          if (teamscore.hidden) {
            List(teamscore.teamId.toString, "--", "--", "--", "--", "--", "played", "--", "?")
          } else {
            List(teamscore.teamId.toString, "--", "--", "--", "--", "--", teamscore.score.toString, "--", teamscore.points.toString)
          }
        } else {
          List(teamscore.teamId.toString, "--", "--", "--", "--", "--", "", "--", "")
        }
      }
    } }

    printTable( header, body, "%9s " )

    println("")
  }

  def showTables( md: MatchDuplicate ) = {
    MatchDuplicateScore(md,PerspectiveTable("T1","T2")).tables.foreach{ case(table, rounds) =>
      println("")
      println("Table "+table)
      val header = List( "Round", "NS", "EW", "Boards" )
      val body = rounds.map { round => {
        List( round.round.toString(), round.ns.id.toString, round.ew.id.toString, round.boards.map { b => b.id.toString }.mkString(", ") )
      } }
      printTable( header, body )
    }
  }

  def printTable( header: List[String], body: List[List[String]], format: String = "%7s " ) = {
    List(List(header),body).flatten.foreach { row => {
      row.foreach { cell => print( format.format(cell) ) }
      println("")
    } }
  }
}
