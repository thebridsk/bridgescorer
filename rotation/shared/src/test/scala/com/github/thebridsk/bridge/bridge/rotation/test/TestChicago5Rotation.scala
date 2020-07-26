package com.github.thebridsk.bridge.bridge.rotation.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.rotation.Table
import com.github.thebridsk.bridge.rotation.Table._
import com.github.thebridsk.bridge.rotation.Chicago5Rotation
import org.scalatest.exceptions.TestFailedException

object TestChicago5Rotation {

  /**
   * Get the specified property as either a java property or an environment variable.
   * If both are set, the java property wins.
   * @param name the property name
   * @return the property value wrapped in a Some object.  None property not set.
   */
  def getPropOrEnv( name: String ): Option[String] = sys.props.get(name) match {
    case v: Some[String] =>
//      log.fine("getPropOrEnv: found system property: "+name+"="+v.get)
      v
    case None =>
      sys.env.get(name) match {
        case v: Some[String] =>
//          log.fine("getPropOrEnv: found env var: "+name+"="+v.get)
          v
        case None =>
//          log.fine("getPropOrEnv: did not find system property or env var: "+name)
          None
      }
  }

  /**
   * flag to determine if println is enabled in test cases
   * Configure by setting the System Property ParallelUtils.useSerial or environment variable ParallelUtils.useSerial
   * to "true" or "false".
   * If this property is not set, then the os.name system property is used, on windows or unknown parallel, otherwise serial.
   */
  val enablePrintln: Boolean = {
    getPropOrEnv("TestChicago5RotationPrintln") match {
      case Some(v) =>
        v.toBoolean
      case None =>
        sys.props.getOrElse("os.name", "oops").toLowerCase() match {
          case os: String if (os.contains("win")) => false
          case os: String if (os.contains("mac")) => false
          case os: String if (os.contains("nix")||os.contains("nux")) => false
          case os =>
//            log.severe("Unknown operating system: "+os)
            false
        }
    }
  }

}

class TestChicago5Rotation extends AnyFlatSpec with Matchers {

  def println( s: String = "" ): Unit = {
    if (TestChicago5Rotation.enablePrintln) Predef.println(s)
  }

  behavior of "Table class in bridgescorer-rotation"

  // North at the top:
  //    A
  //  D   B
  //    C
  val defaultTable: Table = Table( north="A", south="C", east="B", west="D", sittingOut="E" )

  it should "return A as the player at north" in {
    defaultTable.find(North) mustBe "A"
    defaultTable.find("A") mustBe Some(North)
  }

  it should "return B as the player at east" in {
    defaultTable.find(East) mustBe "B"
    defaultTable.find("B") mustBe Some(East)
  }

  it should "return C as the player at south" in {
    defaultTable.find(South) mustBe "C"
    defaultTable.find("C") mustBe Some(South)
  }

  it should "return D as the player at west" in {
    defaultTable.find(West) mustBe "D"
    defaultTable.find("D") mustBe Some(West)
  }

  it should "return E as the player sitting out" in {
    defaultTable.find(SittingOut) mustBe "E"
    defaultTable.find("E") mustBe Some(SittingOut)
  }

  // North at the top:
  //    A
  //  D   B
  //    C

  it should "return north's partner, left and right opponents" in {
    defaultTable.partnerOf(North) mustBe Some("C")
    defaultTable.leftOf(North) mustBe Some("B")
    defaultTable.rightOf(North) mustBe Some("D")
  }

  it should "return east's partner, left and right opponents" in {
    defaultTable.partnerOf(East) mustBe Some("D")
    defaultTable.leftOf(East) mustBe Some("C")
    defaultTable.rightOf(East) mustBe Some("A")
  }

  it should "return south's partner, left and right opponents" in {
    defaultTable.partnerOf(South) mustBe Some("A")
    defaultTable.leftOf(South) mustBe Some("D")
    defaultTable.rightOf(South) mustBe Some("B")
  }

  it should "return west's partner, left and right opponents" in {
    defaultTable.partnerOf(West) mustBe Some("B")
    defaultTable.leftOf(West) mustBe Some("A")
    defaultTable.rightOf(West) mustBe Some("C")
  }

  // Table( north="A", south="C", east="B", west="D", sittingOut="E" )
  it should "swap left and partner of north" in {
    val result = Table( north="A", south="B", east="C", west="D", sittingOut="E" )

    defaultTable.swapLeftPartnerOf( North ) mustBe result
  }

  it should "swap left and partner of south" in {
    val result = Table( north="D", south="C", east="B", west="A", sittingOut="E" )

    defaultTable.swapLeftPartnerOf( South ) mustBe result
  }

  it should "swap left and partner of east" in {
    val result = Table( north="A", south="D", east="B", west="C", sittingOut="E" )

    defaultTable.swapLeftPartnerOf( East ) mustBe result
  }

  it should "swap left and partner of west" in {
    val result = Table( north="B", south="C", east="A", west="D", sittingOut="E" )

    defaultTable.swapLeftPartnerOf( West ) mustBe result
  }

  // North at the top:
  //    A
  //  D   B
  //    C
  // Table( north="A", south="C", east="B", west="D", sittingOut="E" )
  it should "swap right and partner of north" in {
    val result = Table( north="A", south="D", east="B", west="C", sittingOut="E" )

    defaultTable.swapRightPartnerOf( North ) mustBe result
  }

  it should "swap right and partner of south" in {
    val result = Table( north="B", south="C", east="A", west="D", sittingOut="E" )

    defaultTable.swapRightPartnerOf( South ) mustBe result
  }

  it should "swap right and partner of east" in {
    val result = Table( north="D", south="C", east="B", west="A", sittingOut="E" )

    defaultTable.swapRightPartnerOf( East ) mustBe result
  }

  it should "swap right and partner of west" in {
    val result = Table( north="A", south="B", east="C", west="D", sittingOut="E" )

    defaultTable.swapRightPartnerOf( West ) mustBe result
  }

  // Table( north="A", south="C", east="B", west="D", sittingOut="E" )
  it should "swap left and partner of and rotate north" in {
    val result = Table( north="E", south="B", east="C", west="D", sittingOut="A" )

    defaultTable.rotateSwapLeftPartnerOf( North ) mustBe result
  }

  it should "swap left and partner of and rotate south" in {
    val result = Table( north="D", south="E", east="B", west="A", sittingOut="C" )

    defaultTable.rotateSwapLeftPartnerOf( South ) mustBe result
  }

  it should "swap left and partner of and rotate east" in {
    val result = Table( north="A", south="D", east="E", west="C", sittingOut="B" )

    defaultTable.rotateSwapLeftPartnerOf( East ) mustBe result
  }

  it should "swap left and partner of and rotate west" in {
    val result = Table( north="B", south="C", east="A", west="E", sittingOut="D" )

    defaultTable.rotateSwapLeftPartnerOf( West ) mustBe result
  }

  // Table( north="A", south="C", east="B", west="D", sittingOut="E" )
  it should "swap right and partner of and rotate north" in {
    val result = Table( north="E", south="D", east="B", west="C", sittingOut="A" )

    defaultTable.rotateSwapRightPartnerOf( North ) mustBe result
  }

  it should "swap right and partner of and rotate south" in {
    val result = Table( north="B", south="E", east="A", west="D", sittingOut="C" )

    defaultTable.rotateSwapRightPartnerOf( South ) mustBe result
  }

  it should "swap right and partner of and rotate east" in {
    val result = Table( north="D", south="C", east="E", west="A", sittingOut="B" )

    defaultTable.rotateSwapRightPartnerOf( East ) mustBe result
  }

  it should "swap right and partner of and rotate west" in {
    val result = Table( north="A", south="B", east="C", west="E", sittingOut="D" )

    defaultTable.rotateSwapRightPartnerOf( West ) mustBe result
  }


  // North at the top:
  //    A
  //  D   B
  //    C
  // Table( north="A", south="C", east="B", west="D", sittingOut="E" )
  it should "rotate clockwise" in {
    val resultN = Table( north="E", south="B", east="D", west="C", sittingOut="A" )
    defaultTable.rotateClockwise(North) mustBe resultN
    val resultS = Table( north="D", south="E", east="A", west="B", sittingOut="C" )
    defaultTable.rotateClockwise(South) mustBe resultS
    val resultE = Table( north="D", south="A", east="E", west="C", sittingOut="B" )
    defaultTable.rotateClockwise(East) mustBe resultE
    val resultW = Table( north="C", south="B", east="A", west="E", sittingOut="D" )
    defaultTable.rotateClockwise(West) mustBe resultW
  }
  it should "rotate counterclockwise" in {
    val resultN = Table( north="E", south="D", east="C", west="B", sittingOut="A" )
    defaultTable.rotateCounterClockwise(North) mustBe resultN
    val resultS = Table( north="B", south="E", east="D", west="A", sittingOut="C" )
    defaultTable.rotateCounterClockwise(South) mustBe resultS
    val resultE = Table( north="C", south="D", east="E", west="A", sittingOut="B" )
    defaultTable.rotateCounterClockwise(East) mustBe resultE
    val resultW = Table( north="B", south="A", east="C", west="E", sittingOut="D" )
    defaultTable.rotateCounterClockwise(West) mustBe resultW
  }

  it should "rotate three players" in {
    defaultTable.rotateClockwise(North).rotateCounterClockwise(North) mustBe defaultTable
    defaultTable.rotateClockwise(South).rotateCounterClockwise(South) mustBe defaultTable
    defaultTable.rotateClockwise(East).rotateCounterClockwise(East) mustBe defaultTable
    defaultTable.rotateClockwise(West).rotateCounterClockwise(West) mustBe defaultTable
  }

  it should "play 5 rounds left" in {
    val result = Chicago5Rotation.playLeft()

    result.size mustBe 5

    println( "For 5 rounds\n"+result.mkString("\n") )
    checkResult(result,1)

  }

  it should "play 5 rounds right" in {
    val result = Chicago5Rotation.playRight()

    result.size mustBe 5

    println( "For 5 rounds\n"+result.mkString("\n") )
    checkResult(result,1)

  }

//  it should "play 10 rounds both" in {
//    val result = Chicago5Rotation.playBoth()
//
//    result.size mustBe 10
//
//    println( "For 10 rounds: "+result )
//    println( "For 10 rounds\n"+result.mkString("\n") )
//    checkResult(result,2)
//
//  }

  it should "play 15 rounds with right first" in {
    val result = Chicago5Rotation.allRotations(true)

    result.size mustBe 15

    println( "For 15 rounds\n"+result.mkString("\n") )
    checkResult(result,3)

  }

  it should "play 15 rounds with playComplete owesn" in {
    val s = Table( north="1", south="2", east="3", west="4", sittingOut="5" )
    val players = s.sittingOut::s.west::s.east::s.south::s.north::Nil
    val result = Chicago5Rotation.playComplete(players, s::Nil)

    result.size mustBe 15

    println( "For 15 rounds\n"+result.mkString("\n") )
    checkResult(result,3)

    (result zip players.tail ::: players ::: players ::: players).zipWithIndex.foreach(e => {
      val ((t, p), i) = e
      println( s"${i}: Next out $p from ${t.find(p).get}" )
    })

  }

  it should "play 15 rounds with playComplete onswe" in {
    val s = Table( north="1", south="2", east="3", west="4", sittingOut="5" )
    val players = s.sittingOut::s.north::s.south::s.west::s.east::Nil
    val result = Chicago5Rotation.playComplete(players, s::Nil)

    result.size mustBe 15

    println( "For 15 rounds\n"+result.mkString("\n") )
    checkResult(result,3)

  }

  it should "play 10 rounds with playComplete maneuverLeft and random" in {
    val s = Table( north="1", south="2", east="3", west="4", sittingOut="5" )
    val players = s.sittingOut::s.north::s.south::s.west::s.east::Nil

    var count = 0
    val failed = (for ( round1 <- players.tail.permutations;
                        round2 <- players.permutations ) yield {
      if (round1.last != round2.head) {
        try {
          count = count + 1
          val maneuvers = (round1:::round2).map( Chicago5Rotation.maneuverLeftStillOthersSwap(_) _)
          val result = Chicago5Rotation.play(maneuvers, s::Nil)

          result.size mustBe 10

          println( "For 10 rounds\n"+result.mkString("\n") )
          checkResult(result,2)
          None
        } catch {
          case x: Exception =>
            println("Caught "+x.getClass.getName )
            Some( round1:::round2 )
        }
      } else {
        None
      }
    }).filter( f => f.isDefined ).map( f => f.get ).toList
    if (failed.size !=0 ) {
      println( "Failed permutations: "+failed.size +"/"+count)
      println( failed.mkString("\n") )
      println()
      fail("Not all permutations worked")
    }
  }

  it should "fail in playing 15 rounds with left first" in {
    val result = Chicago5Rotation.allRotations(false)

    result.size mustBe 15

    println( "For 15 rounds\n"+result.mkString("\n") )
    intercept[TestFailedException] {
      checkResult(result,3)
    }

  }

  it should "work in all player rotations for 5 rounds" in {
    val s = Table( north="1", south="2", east="3", west="4", sittingOut="5" )
    val players = s.west::s.east::s.south::s.north::Nil

    val perms = players.permutations.toList
    val failed = perms.map( list => {
      try {
        val ps = list
        val result = Chicago5Rotation.playLeft(ps, s::Nil)
        checkResult(result,1)
        None
      } catch {
        case x: Exception =>
          println("Caught "+x.getClass.getName )
          Some( list )
      }
    }).filter( f => f.isDefined ).map( f => f.get ).toList
    if (failed.size !=0 ) {
      println( "Failed permutations: "+failed.size +"/"+perms.size)
      println( failed.mkString("\n") )
      println()
      fail("Not all permutations worked")
    }
  }

  it should "work by using rotation scheme for 5 rounds" in {
    val s = Table( north="1", south="2", east="3", west="4", sittingOut="5" )
    val players = s.west::s.east::s.south::s.north::Nil

    val perms = players.permutations.toList
    val failed = perms.map( list => {
      try {
        val ps = list
        import Chicago5Rotation._
        val maneuvers = ps.map(p => maneuverLeftStillOthersSwap(p) _)
        val result = Chicago5Rotation.play(maneuvers, s::Nil)
        checkResult(result,1)
        None
      } catch {
        case x: Exception =>
          println("Caught "+x.getClass.getName )
          Some( list )
      }
    }).filter( f => f.isDefined ).map( f => f.get ).toList
    if (failed.size !=0 ) {
      println( "Failed permutations: "+failed.size +"/"+perms.size)
      println( failed.mkString("\n") )
      println()
      fail("Not all permutations worked")
    }
  }

  it should "work in 4 of the player rotations" in {
    val s = Table( north="1", south="2", east="3", west="4", sittingOut="5" )
    val players = s.west::s.east::s.south::s.north::Nil

    val perms = players.permutations.toList
    val failed = perms.map( list => {
      try {
        val ps = s.sittingOut::list
        val result = Chicago5Rotation.allRotations(true, ps, s::Nil)
        checkResult(result,3)
        None
      } catch {
        case x: Exception =>
          println("Caught "+x.getClass.getName )
          Some( list )
      }
    }).filter( f => f.isDefined ).map( f => f.get ).toList
    if (failed.size !=0 ) {
      println( "Failed permutations: "+failed.size +"/"+perms.size)
      println( failed.mkString("\n") )
      println()
      if (perms.size - failed.size != 4) fail("Did not work with exactly 4 permutations")
    }
  }

  it should "fail in all player rotations with left first" in {
    val s = Table( north="1", south="2", east="3", west="4", sittingOut="5" )
    val players = s.west::s.east::s.south::s.north::Nil

    val perms = players.permutations.toList
    val failed = perms.map( list => {
      try {
        val ps = s.sittingOut::list
        val result = Chicago5Rotation.allRotations(false, ps, s::Nil)
        checkResult(result,3)
        None
      } catch {
        case x: Exception =>
          println("Caught "+x.getClass.getName )
          Some( list )
      }
    }).filter( f => f.isDefined ).map( f => f.get ).toList
    if (failed.size !=0 ) {
      println( "Failed permutations: "+failed.size +"/"+perms.size)
      println( failed.mkString("\n") )
      println()

      if (perms.size != failed.size) fail("Not all permutations failed")
    }
  }

  def checkResult( result: List[Table], expectedCount: Int ): Unit = {
    val players = result.head.players()
    val r = players.flatMap( p1 => {
      players.filter( p2 => p1<p2 ).flatMap( p2 => {
        val count = result.filter( h => h.hasPartnership(p1, p2) ).size
        if (count != expectedCount) (p1,p2,count)::Nil
        else Nil
      })
    })
    if (!r.isEmpty) {
      println( s"Did not get $expectedCount occurences of all pairs\n"+r.mkString("\n") )
      println( result.mkString("\n") )
      fail(s"Did not get $expectedCount occurences of all pairs")
    }
  }
}
