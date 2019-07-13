package com.github.thebridsk.bridge.test

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import com.github.thebridsk.bridge.data.MatchChicago
import scala.reflect.io.Directory
import scala.reflect.io.Path
import scala.reflect.io.File
import com.github.thebridsk.bridge.backend.BridgeServiceFileStoreConverters
import com.github.thebridsk.bridge.backend.resource.FileIO
import com.github.thebridsk.bridge.data.chicago.ChicagoScoring
import com.github.thebridsk.bridge.data.Round

class TestChicagoScore extends FlatSpec with MustMatchers {

  behavior of "Chicago scoring"

  val testdir = Path("../testdata").toDirectory;

  val prefix = (testdir / "MatchChicago.").toString()

  def getAllGames(): List[File] = {

    testdir.files.map{ f => System.out.println(s"Considering $f"); f}.filter{ f =>
      f.toString().startsWith(prefix)
    }.toList
  }

  val converter = new BridgeServiceFileStoreConverters(true)


  /**
   * Read a resource from the persistent store
   * @param filename
   * @return the result containing the resource or an error
   */
  def read( filename: File ): String = {
    FileIO.readFileSafe( filename.toString() )
  }

  import org.scalatest.prop.TableDrivenPropertyChecks._
  val templates = Table( "MatchChicago", getAllGames(): _* )

  forAll(templates) { filename =>

    var mc: Option[MatchChicago] = None

    it should s"read $filename and convert it to a MatchChicago object" in {
      val s = read(filename)
      val (good, rmc) = converter.matchChicagoJson.parse(s)
      mc = Some(rmc)
    }

    it should s"take the MatchChicago object from $filename and wrap it in a scoring object" in {
      if (mc.isEmpty) fail("Unable to read and convert file")

      mc.get.rounds.zipWithIndex.foreach { e =>
        val (r,i) = e
        System.out.println( s"  Dealer in first hand in $filename in round $i is ${r.dealerFirstRound}" )
      }

      val chicago = ChicagoScoring(mc.get)
    }

  }

}
