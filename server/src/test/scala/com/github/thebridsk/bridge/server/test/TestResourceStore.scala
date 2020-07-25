package com.github.thebridsk.bridge.server.test

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.server.test.backend.BridgeServiceTesting
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.bridge.Spades
import com.github.thebridsk.bridge.data.bridge.Doubled
import akka.http.scaladsl.model.StatusCodes
import com.github.thebridsk.bridge.server.backend.BridgeServiceInMemory
import com.github.thebridsk.bridge.data.RestMessage
import com.github.thebridsk.bridge.server.backend.BridgeNestedResources
import com.github.thebridsk.bridge.data.Team

class TestResourceStore extends AsyncFlatSpec with Matchers {

  val bridgeService = new BridgeServiceInMemory("test")
  val store = bridgeService.duplicates

  val matchdup = BridgeServiceTesting.testingMatch

  val team1 = Team.id(1)
  val team2 = Team.id(2)
  val team3 = Team.id(3)
  val team4 = Team.id(4)

  behavior of "BridgeServiceAlternate for duplicate"

  it should "return the same object when adding a MatchDuplicate to the store" in {
    store.createChild(matchdup).map { ret =>
      ret match {
        case Right(l) => assert( l.equalsIgnoreModifyTime(matchdup) )
        case Left(r) => fail("Unable to add MatchDuplicate "+matchdup.id+" to store: "+r._2)
      }
    }
  }

  it should "return the M1 object when getting from store" in {
    store.select(matchdup.id).read().map { ret =>
      ret match {
        case Right(l) => assert( l.equalsIgnoreModifyTime(matchdup) )
        case Left(r) => fail("Unable to query MatchDuplicate "+matchdup.id+" from store: "+r._2)
      }
    }
  }

  it should "return not found when getting M2 from store" in {
    store.select(MatchDuplicate.id(2)).read().map { ret =>
      ret match {
        case Right(l) => fail("did not get not found, got "+l)
        case Left(r) =>
          assert( r._1 == StatusCodes.NotFound )
          assert( r._2 == RestMessage("Did not find resource /duplicates/M2") )
      }
    }
  }

  import BridgeNestedResources._
  it should "return the B1 object when getting boards from match M1 from store" in {
    store.select(matchdup.id).resourceBoards.select(Board.id(1)).read().map { ret =>
      ret match {
        case Right(l) =>
          assert( l.id == Board.id(1) )
          assert( l.equalsIgnoreModifyTime( Board.create(Board.id(1), false, false, North.pos, List(
                DuplicateHand.create( Hand.create("H1",7,Spades.suit, Doubled.doubled, North.pos,
                                                  false,false,true,7),
                                                  Table.id(1), 1, Board.id(1), team1, team2),
                DuplicateHand.create( Hand.create("H2",7,Spades.suit, Doubled.doubled, North.pos,
                                                  false,false,false,1),
                                                  Table.id(2), 2, Board.id(1), team3, team4)
                )) ))
        case Left(r) => fail("Unable to get Board B1 from store: "+r._2)
      }
    }
  }

  it should "return the hand T1 object when getting hand from board B1 from match M1 from store" in {
    store.select(matchdup.id).resourceBoards.select(Board.id(1)).resourceHands.select(team1).read().map { ret =>
      ret match {
        case Right(l) =>
          assert( l.id == team1 )
          assert( l.equalsIgnoreModifyTime(DuplicateHand.create( Hand.create("H1",7,Spades.suit, Doubled.doubled, North.pos,
                                                                 false,false,true,7),
                                                            Table.id(1), 1, Board.id(1), team1, team2)
                ))
        case Left(r) => fail("Unable to get hand T1 from MatchDuplicate,Board "+matchdup.id+",B1 to store: "+r._2)
      }
    }
  }

  it should "return not found when getting hand from board B1 from match M2 from store" in {
    store.select(MatchDuplicate.id(2)).resourceBoards.select(Board.id(1)).resourceHands.select(team1).read().map { ret =>
      ret match {
        case Right(l) =>
          fail("Unexpected response to get hand H1 from MatchDuplicate,Board M2,B1 to store: "+l)
        case Left(r) =>
          assert( r._1 == StatusCodes.NotFound )
          assert( r._2 == RestMessage("Did not find resource /duplicates/M2") )
      }
    }
  }

  it should "return not found when getting hand from board B4 from match M1 from store" in {
    store.select(matchdup.id).resourceBoards.select(Board.id(4)).resourceHands.select(team1).read().map { ret =>
      ret match {
        case Right(l) =>
          fail("Unexpected response to get hand H1 from MatchDuplicate,Board M2,B4 to store: "+l)
        case Left(r) =>
          assert( r._1 == StatusCodes.NotFound )
          assert( r._2 == RestMessage(s"Did not find resource /duplicates/${matchdup.id.id}/boards/B4") )
      }
    }
  }

  it should "return not found when getting hand from board B3 from match M1 from store" in {
    store.select(matchdup.id).resourceBoards.select(Board.id(3)).resourceHands.select(team1).read().map { ret =>
      ret match {
        case Right(l) =>
          fail("Unexpected response to get hand H1 from MatchDuplicate,Board M2,B3 to store: "+l)
        case Left(r) =>
          assert( r._1 == StatusCodes.NotFound )
          assert( r._2 == RestMessage(s"Did not find resource /duplicates/${matchdup.id.id}/boards/B3/hands/T1") )
      }
    }
  }

}
