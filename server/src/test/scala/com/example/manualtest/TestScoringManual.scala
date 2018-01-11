package com.example.manualtest

import java.util.logging.Level
import utils.main.Main
import com.example.data.Hand
import com.example.data.bridge._
import com.example.data.bridge.DuplicateBridge._

/**
 * @author werewolf
 */
object TestScoringManual extends Main {
  
  def execute() = {
    showHand( Hand.create("1",4,"N","R","N",true,false,true,5) )
    simpleTest()
    checkallmade()
    
    checkalldown()
    checkalldownwithcontract()
    0
  }

  def showHand( hand: ScoreHand ): Unit = {
    logger.info("Hand: "+hand)
  }
  
  def simpleTest(): Unit = {
    showHand( Hand.create("1",4,"S","N","N",true,false,true,5) )
    showHand( Hand.create("1",4,"N","D","N",true,false,false,5) )
  }
  
  def checkallmade() = {
    println();
    println("made")
    for (suit <- contractSuits;
         ctricks <- 1 to 7) {
      for (tricks <- ctricks to 7) {
        var didsuit = false
        for (vul <- vulnerabilities;
             doubled <- contractDoubling) {
          val hand = ScoreHand("1",ctricks,suit,doubled,North,vul,NotVul,Made,tricks)
          if (!didsuit) {
            val contract = ""+hand.contractTricks.tricks+hand.contractSuit.suit+"   "+hand.tricks
            print(contract)
            didsuit = true
          }
          print("   "+hand.score.ns)
          if (hand.score.ns != -hand.score.ew) print("  scores not equal")
        }
        println()
      }
    }
  }
  
  def checkMajorMinor() = {
    println("Starting to check majors and minors")
    for ((suit1,suit2) <- (Spades,Hearts)::(Diamonds,Clubs)::Nil;
         ctricks <- 1 to 7) {
      for (tricks <- ctricks to 7) {
        var didsuit = false
        for (vul <- vulnerabilities;
             doubled <- contractDoubling) {
          val hand1 = ScoreHand("1",ctricks,suit1,doubled,North,vul,NotVul,Made,tricks)
          val hand2 = ScoreHand("1",ctricks,suit2,doubled,North,vul,NotVul,Made,tricks)
          if (hand1.score.ns != hand2.score.ns) {
            println("Not equal scores: "+hand1.score.ns+" "+hand2.score.ns)
            println("  "+hand1)
            println("  "+hand2)
          }
        }
      }
    }
    println("Done checking majors and minors")
  }

  
  def checkalldown() = {
    println()
    println("down")
    for (ctricks <- 7 to 7) {
      for (tricks <- 1 to 6+ctricks) {
        var didsuit = false
        for (vul <- vulnerabilities;
             doubled <- contractDoubling) {
          val hand1 = ScoreHand("1",ctricks,NoTrump,doubled,North,vul,NotVul,Down,tricks)
          val hand2 = ScoreHand("1",ctricks,Spades,doubled,North,vul,NotVul,Down,tricks)
          val hand3 = ScoreHand("1",ctricks,Hearts,doubled,North,vul,NotVul,Down,tricks)
          val hand4 = ScoreHand("1",ctricks,Diamonds,doubled,North,vul,NotVul,Down,tricks)
          val hand5 = ScoreHand("1",ctricks,Clubs,doubled,North,vul,NotVul,Down,tricks)
          if (!didsuit) {
            val contract = ""+hand5.contractTricks.tricks+hand5.contractSuit.suit+"   "+hand5.tricks
            print(contract)
            didsuit = true
          }
          print("   "+hand5.score.ew)
          if (hand5.score.ns != -hand5.score.ew) print("  ew-ns scores not equal ("+hand5.score.ns+","+hand5.score.ew)
          if (hand5.score.ns != hand1.score.ns
              || hand5.score.ns != hand2.score.ns
              || hand5.score.ns != hand3.score.ns
              || hand5.score.ns != hand4.score.ns
              || hand5.score.ew != hand1.score.ew
              || hand5.score.ew != hand2.score.ew
              || hand5.score.ew != hand3.score.ew
              || hand5.score.ew != hand4.score.ew
              ) {
            print(  "suit score not equal "+hand1.score.ns+","+hand2.score.ns+","+hand3.score.ns+","+hand4.score.ns+","+hand5.score.ns+
                                      "   "+hand1.score.ew+","+hand2.score.ew+","+hand3.score.ew+","+hand4.score.ew+","+hand5.score.ew)
          }
        }
        println()
      }
    }
  }

  def checkalldownwithcontract() = {
    println("Checking for down equals")
    for (tricks <- 1 to 13) {
      var didsuit = false
      for (vul <- vulnerabilities;
           doubled <- contractDoubling) {
        for (ctricks <- Math.max(1, tricks-6) to 7 ) {
          val handRef = ScoreHand("1",ctricks,NoTrump,doubled,North,vul,NotVul,Down,tricks)
          for (suit <- Spades::Hearts::Diamonds::Clubs::Nil)
          {
            val hand = ScoreHand("1",ctricks,suit,doubled,North,vul,NotVul,Down,tricks)
            if (handRef.score.ns != -handRef.score.ew) print("  ew-ns scores not equal ("+handRef.score.ns+","+handRef.score.ew)
            if (handRef.score.ns != hand.score.ns
                || handRef.score.ew != hand.score.ew
                ) {
              println(  "score not equal "+handRef.score.ns+","+hand.score.ns)
              println( "  "+handRef)
              println( "  "+hand)
            }
          }
        }
      }
      println()
    }
  }

}