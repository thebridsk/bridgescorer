package com.github.thebridsk.bridge.client.test

import com.github.thebridsk.bridge.data.js.SystemTimeJs
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.Round
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.client.pages.chicagos.PagePlayers
import com.github.thebridsk.bridge.client.pages.chicagos.ViewPlayersQuintetInternal
import com.github.thebridsk.bridge.data.Hand
import scala.annotation.tailrec

object TestFairRotation {

  val pNorth = "North"
  val pSouth = "South"
  val pEast = "East"
  val pWest = "West"
  val pExtra = "Extra"

  val allPlayers = pNorth::pSouth::pEast::pWest::pExtra::Nil
}

class TestFairRotation extends AnyFlatSpec with Matchers {

  import TestFairRotation._

  SystemTimeJs()

  behavior of "TestFairRotation in bridgescorer-client"

  def checkForDuplicates( pairs: List[(String,String)] ): Unit = {
    @tailrec
    def check( pairs: List[(String,String)] ): Unit = {
      pairs match {
        case Nil =>
        case head :: Nil =>
          withClue( s"All pairs must be with two people, found $head, in $pairs") {
            head._1 must not be head._2
          }

        case head :: rest =>
          rest.find { e =>
            (head._1 == e._1 && head._2 == e._2) || (head._1 == e._2 && head._2 == e._1)
          }.map( f => throw new Exception( s"Duplicate found: $f") )
          withClue( s"All pairs must be with two people, found $head, in $pairs") {
            head._1 must not be head._2
          }
          check(rest)
      }
    }
    check(pairs)
    allPlayers.foreach { p =>
      val pairsWithPlayer = pairs.filter { pair => pair._1 == p || pair._2 == p}
      withClue( s"Each person must appear 4 times, player $p, in $pairs") {
        pairsWithPlayer.length mustBe 4
      }
    }
  }

  it should "test all combinations of sitting out in fair rotation" in {
    val r0 = Round.create("0",
                          pNorth,
                          pSouth,
                          pEast,
                          pWest,
                          North.pos,
                          Nil )
    val mc0 = MatchChicago(MatchChicago.id(1),List("","","",""),Nil,0,false).setPlayers(pNorth, pSouth, pEast, pWest).playChicago5(pExtra).setQuintet(false).addRound(r0)

    val pairs0 = (pNorth,pSouth)::(pEast,pWest)::Nil
    val notYetSittingOut0 = pNorth::pSouth::pEast::pWest::Nil

    def play( mc: MatchChicago, pairs: List[(String,String)], notYetSittingOut: List[String], results: List[List[(String,String)]] ): List[List[(String,String)]] = {
      if (notYetSittingOut.isEmpty) {
        // no more rounds to play, check if there are duplicates in pairs
        println( s"Testing for duplicates: $pairs")
        checkForDuplicates(pairs)
        pairs :: results
      } else {
        val h = Hand.create("0",3,"N","N","N",false,false,true,3)
        val mcn = mc // .addHandToLastRound(h)
        val props = PagePlayers.Props( null, mc, null)
        val state = ViewPlayersQuintetInternal.State(props)

        notYetSittingOut.foldLeft( results ) { case (r,nextSittingOut) =>
          val nextNotYetSittingOut = notYetSittingOut.filter( _ != nextSittingOut)
          val nextState = state.fairRotation( nextSittingOut )
          val nextPairs = (nextState.north,nextState.south)::(nextState.east,nextState.west)::pairs

          play( mcn, nextPairs, nextNotYetSittingOut, r )
        }
      }
    }

    val r = play( mc0, pairs0, notYetSittingOut0, Nil )

    println( s"Number of results: ${r.length}")
  }
}
