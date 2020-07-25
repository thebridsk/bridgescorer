package com.github.thebridsk.bridge.server.test

import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.server.util.SystemTimeJVM
import org.scalatest.flatspec.AnyFlatSpec
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.chicago.ChicagoScoring
import com.github.thebridsk.bridge.data.Round
import com.github.thebridsk.bridge.data.chicago.ChicagoScoring.Fixture

class TestChicagoScoring extends AnyFlatSpec with Matchers {

  SystemTimeJVM()

  behavior of "a chicago played match"

  val chiid = MatchChicago.id(1)
  var mc = MatchChicago( chiid, ""::""::""::""::Nil, Nil, 0, false )

  it should "allow a ChicagoScoring object to be created" in {
    val cs = ChicagoScoring(mc)

    cs.players mustBe ""::""::""::""::Nil
    cs.rounds.length mustBe 0
  }

  it should "allow the names to be set and scoring should still work" in {
    mc = mc.setPlayers("North"::"South"::"East"::"West"::Nil)
    val cs = ChicagoScoring(mc)

    cs.players mustBe "North"::"South"::"East"::"West"::Nil
    cs.rounds.length mustBe 0

  }

  it should "allow a round to be added and scoring should still work" in {
    val r = Round.create("0", "North", "South", "East", "West", "N", Nil )
    mc = mc.addRound(r)
    val cs = ChicagoScoring(mc)

    cs.players mustBe "North"::"South"::"East"::"West"::Nil
    cs.rounds.length mustBe 1
  }

  it should "allow the conversion to a chicago5 match" in {
    mc = mc.playChicago5("Extra")
    val cs = ChicagoScoring(mc)

    cs.players mustBe "North"::"South"::"East"::"West"::"Extra"::Nil
    cs.rounds.length mustBe 1
  }

  behavior of "a chicago fixture calculation"

  it should "show 2 possible fixtures" in {
    var chi = MatchChicago( chiid, ""::""::""::""::Nil, Nil, 0, false )
    chi = chi.setPlayers("North"::"South"::"East"::"West"::Nil)
    chi = chi.addRound(Round.create("0", "North", "South", "East", "West", "N", Nil ))
    chi = chi.playChicago5("Extra")
    val cs = ChicagoScoring(chi)
    val possible = cs.getNextPossibleFixtures
    possible.size mustBe 4
    possible.keys.toSet mustBe Set("North","South","East","West")
    possible.foreach( e => {
      val (player,pos) = e
      pos.size mustBe 2
//      pos.foreach(f => println("Next "+f))
    })
  }

  it should "find the correct next" in {
    val players = Seq("East","West","North","South","Extra")
    val allfixtures = ChicagoScoring.getAllFixtures(players:_*)
    val pathSoFar = List(
                      Fixture("East","West","North","South","Extra"),
                      Fixture("East","South","Extra","West","North"),
                      Fixture("East","Extra","North","West","South"))

    val paths = ChicagoScoring.calculatePaths(allfixtures, pathSoFar, "East")

    paths.foreach( path => {
      validatePath(path)
    })
  }

  it should "find the correct next after 4 rounds" in {
    val players = Seq("east","west","north","south","extra")
    val allfixtures = ChicagoScoring.getAllFixtures(players:_*)
    val pathSoFar = List(
                      Fixture("east","west","north","south","extra"),
                      Fixture("east","south","extra","west","north"),
                      Fixture("east","extra","north","west","south"),
                      Fixture("extra","north","south","west","east") )

    val allpaths = ChicagoScoring.calculatePaths(allfixtures)

    val found = allpaths.filter(p => p.take(4) == pathSoFar)

    found.size mustBe 1

    val paths = ChicagoScoring.calculatePaths(allfixtures, pathSoFar, "west")

    paths.size mustBe 1

    paths.foreach( path => {
      validatePath(path)
    })
  }

  /**
   * Check if the specified testpath is valid.
   * Validity checks:
   * - every pair of people only occurs once
   * - a player only sits out once
   * @param testpath the path to test.
   * @throws TestFailedException if test fails
   */
  def validatePath( testpath: List[Fixture] ): Unit = {
    testpath.map( f => f.extra ).toSet.size mustBe testpath.size

    testpath.zipWithIndex.map{ case (f1,i1) =>
      testpath.drop(i1+1).zipWithIndex.map{ case (f2,i2) =>
        f2.hasPair(f1.north, f1.south) mustBe false
        f2.hasPair(f1.east, f1.west) mustBe false
      }
    }
  }

  def process( players: Seq[String], pathSoFar: List[Fixture], allfixtures: Seq[Fixture]): Unit = {
//    println("Working on "+pathSoFar)
    withClue ("Fixture path is "+pathSoFar) { validatePath(pathSoFar) }

    val ret = players.flatMap(p => {
      val paths = ChicagoScoring.calculatePaths(allfixtures, pathSoFar, p)
      val nexts = ChicagoScoring.calculateNextPossible(pathSoFar, paths)
      if (nexts.isEmpty) Seq()
      else {
        nexts
      }
    }).foreach( f => {
      val psf = pathSoFar:::List(f)
      process(players,psf,allfixtures)
    })
  }

  it should "walk all fixture paths" in {
    val players = Seq("North", "South", "East", "West", "Extra")
    val allfixtures = ChicagoScoring.getAllFixtures(players:_*)
    val pathSoFar = List( allfixtures.head )
    process(players, pathSoFar, allfixtures)
  }
}
