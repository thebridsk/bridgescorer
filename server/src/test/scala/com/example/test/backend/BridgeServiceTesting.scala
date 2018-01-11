package com.example.test.backend

import com.example.data.Board
import com.example.data.Table
import com.example.data.Hand
import com.example.backend.BridgeService
import com.example.backend.BridgeServiceInMemory
import com.example.data.bridge.East
import com.example.data.bridge.North
import com.example.data.MatchDuplicate
import com.example.data.Team
import com.example.data.DuplicateHand
import com.example.data.bridge.Spades
import com.example.data.bridge.Doubled
import com.example.data.bridge.Hearts
import com.example.data.bridge.South
import java.util.Date
import com.example.data.MatchDuplicateV3


object BridgeServiceTesting {
  val testingMatch =
  {

    val teams = Map( "T1"-> Team.create("T1","Nancy","Sam"),
                     "T2"->Team.create("T2","Ellen", "Wayne"),
                     "T3"->Team.create("T3","Norman","Sally"),
                     "T4"->Team.create("T4","Ethan","Wilma"))
    val time = System.currentTimeMillis()
    MatchDuplicateV3("M1", teams.values.toList, List(
        Board.create("B1", false, false, North.pos, List(
            DuplicateHand.create( Hand.create("H1",7,Spades.suit, Doubled.doubled, North.pos,
                                              false,false,true,7),
                                 "1", 1, "B1", "T1", "T2"),
            DuplicateHand.create( Hand.create("H2",7,Spades.suit, Doubled.doubled, North.pos,
                                              false,false,false,1),
                                  "2", 2, "B1", "T3", "T4")
            )),
        Board.create("B2", true, false, East.pos, List(
            DuplicateHand.create( Hand.create("H1",7,Hearts.suit, Doubled.doubled, North.pos,
                                              false,false,true,7),
                                  "1", 1, "B2", "T1", "T2")
            )),
        Board.create("B3", false, true, South.pos, List())
        ), "", "", time, time)

  }
}

/**
 * An implementation of the backend for our service.
 * This is used for testing, and is predefined with
 * resources to facilitate testing.
 * @author werewolf
 */
class BridgeServiceTesting extends BridgeServiceInMemory {

  val storeduplicates = duplicates.syncStore

  storeduplicates.createChild(BridgeServiceTesting.testingMatch)

  override
  def toString() = "BridgeServiceTesting"
}

