package com.github.thebridsk.bridge.server.manualtest

import com.github.thebridsk.utilities.main.MainNoArgs
import com.github.thebridsk.bridge.data.chicago.ChicagoScoring
import com.github.thebridsk.bridge.data.chicago.ChicagoScoring.Fixture

object TestFixtures extends MainNoArgs {
  def execute(): Int = {
    import ChicagoScoring._

    val allFixtures =
      ChicagoScoring.getAllFixtures("North", "South", "East", "West", "Extra")
    showFixtures(allFixtures)
    val r = calculatePaths(allFixtures)
    showPaths(r)

    val pathSoFar = List(allFixtures.head)
    val r1 = calculatePaths(allFixtures, pathSoFar, "East")
    showPaths(r1, "With East sitting out: ")
    val n1 = calculateNextPossible(pathSoFar, r1)
    showFixtures(n1.toSeq, "There are " + n1.size + " possible fixtures")

    val pathSoFar2 = n1.head :: pathSoFar
    val r2 = calculatePaths(allFixtures, pathSoFar2, "North")
    showPaths(r2, "With North sitting out: ")
    val n2 = calculateNextPossible(pathSoFar2, r2)
    showFixtures(n2.toSeq, "There are " + n2.size + " possible fixtures")

    0
  }

  def showFixtures(
      allFixtures: Seq[Fixture],
      description: String = "Showing all Fixtures"
  ): Unit = {
    logger.info(description + ": found " + allFixtures.length)
    allFixtures.foreach(f => logger.info("  " + fixtureToString(f)))
  }

  def fixtureToString(f: Fixture): String = {
    f.north + "-" + f.south + " " + f.east + "-" + f.west + " Sit out: " + f.extra
  }

  def showPaths(paths: List[List[Fixture]], desc: String = ""): Unit = {
    if (paths.isEmpty) {
      logger.info(desc + "Did not find any paths")
    } else {
      val f = paths.head.head
      val players = Seq(f.north, f.south, f.east, f.west, f.extra)
      logger.info(desc + "Showing paths for " + players.mkString(", "))
      paths.zipWithIndex.foreach { e =>
        {
          val (list, index) = e
          logger.info("  Path " + index)
          list.zipWithIndex.foreach(el => {
            val (f, ei) = el
            logger.info("    " + (ei + 1) + ": " + fixtureToString(f))
          })
        }
      }
    }
  }

}
