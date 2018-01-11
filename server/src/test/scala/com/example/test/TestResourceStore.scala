package com.example.test

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import com.example.data.Board
import com.example.data.Table
import com.example.data.Hand
import com.example.data.bridge.North
import com.example.data.bridge.East
import com.example.data.bridge.South
import com.example.test.backend.BridgeServiceTesting
import com.example.data.MatchDuplicate
import org.scalatest._
import java.net.InetAddress
import com.example.data.DuplicateHand
import com.example.data.bridge.Spades
import com.example.data.bridge.Doubled
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.StatusCodes
import com.example.backend.BridgeServiceInMemory
import com.example.data.RestMessage
import com.example.backend.BridgeNestedResources

class TestResourceStore extends AsyncFlatSpec with MustMatchers {

  val bridgeService = new BridgeServiceInMemory
  val store = bridgeService.duplicates

  val matchdup = BridgeServiceTesting.testingMatch

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
    store.select("M2").read().map { ret =>
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
    store.select(matchdup.id).resourceBoards.select("B1").read().map { ret =>
      ret match {
        case Right(l) =>
          assert( l.id == "B1" )
          assert( l.equalsIgnoreModifyTime( Board.create("B1", false, false, North.pos, List(
                DuplicateHand.create( Hand.create("H1",7,Spades.suit, Doubled.doubled, North.pos,
                                                  false,false,true,7),
                                                  "1", 1, "B1", "T1", "T2"),
                DuplicateHand.create( Hand.create("H2",7,Spades.suit, Doubled.doubled, North.pos,
                                                  false,false,false,1),
                                                  "2", 2, "B1", "T3", "T4")
                )) ))
        case Left(r) => fail("Unable to get Board B1 from store: "+r._2)
      }
    }
  }

  it should "return the hand T1 object when getting hand from board B1 from match M1 from store" in {
    store.select(matchdup.id).resourceBoards.select("B1").resourceHands.select("T1").read().map { ret =>
      ret match {
        case Right(l) =>
          assert( l.id == "T1" )
          assert( l.equalsIgnoreModifyTime(DuplicateHand.create( Hand.create("H1",7,Spades.suit, Doubled.doubled, North.pos,
                                                                 false,false,true,7),
                                                            "1", 1, "B1", "T1", "T2")
                ))
        case Left(r) => fail("Unable to get hand T1 from MatchDuplicate,Board "+matchdup.id+",B1 to store: "+r._2)
      }
    }
  }

  it should "return not found when getting hand from board B1 from match M2 from store" in {
    store.select("M2").resourceBoards.select("B1").resourceHands.select("T1").read().map { ret =>
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
    store.select(matchdup.id).resourceBoards.select("B4").resourceHands.select("T1").read().map { ret =>
      ret match {
        case Right(l) =>
          fail("Unexpected response to get hand H1 from MatchDuplicate,Board M2,B4 to store: "+l)
        case Left(r) =>
          assert( r._1 == StatusCodes.NotFound )
          assert( r._2 == RestMessage(s"Did not find resource /duplicates/${matchdup.id}/boards/B4") )
      }
    }
  }

  it should "return not found when getting hand from board B3 from match M1 from store" in {
    store.select(matchdup.id).resourceBoards.select("B3").resourceHands.select("T1").read().map { ret =>
      ret match {
        case Right(l) =>
          fail("Unexpected response to get hand H1 from MatchDuplicate,Board M2,B3 to store: "+l)
        case Left(r) =>
          assert( r._1 == StatusCodes.NotFound )
          assert( r._2 == RestMessage(s"Did not find resource /duplicates/${matchdup.id}/boards/B3/hands/T1") )
      }
    }
  }

}
