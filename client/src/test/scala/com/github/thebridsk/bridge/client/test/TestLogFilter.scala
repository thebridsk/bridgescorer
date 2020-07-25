package com.github.thebridsk.bridge.client.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.clientcommon.logger.LogFilter
import org.scalactic.source.Position
import com.github.thebridsk.bridge.clientcommon
.websocket.{ LogFilter => WLogFilter }

class TestLogFilter extends AnyFlatSpec with Matchers {

  behavior of "TestLogFilter in bridgescorer-client"

  val filterlist = LogFilter.filterlist:::WLogFilter.filterlist

  val goodFilterlist = "Bridge.scala"::
                       "Duplicate.scala"::
                       Nil

  val loggerlist = "comm.xxxx"::
                   "comm.yyyy"::
                   Nil

  val goodLoggerlist = "xx.xxxx"::
                       "bridge.yyyy"::
                       Nil

  def combos( filters: List[String], loggernames: List[String] ) = {
    for ( f <- filters;
          l <- loggernames ) yield {
      (f,l)
    }
  }

  import org.scalatest.prop.TableDrivenPropertyChecks._


//  it should "Not log filterlist*(goodLoggerlist&loggerlist)" in {
  {

    val filter = new LogFilter

    val filtered = Table( ("Filtered filenames", "All logger names"),
                          combos(filterlist, loggerlist:::goodLoggerlist): _* )

    forAll (filtered) { (filename: String, loggername: String) =>
      it should s"1 Not log ${filename} ${loggername}" in {
        val traceMsg = com.github.thebridsk.utilities.logging.TraceMsg(0, loggername, com.github.thebridsk.utilities.logging.LogMsgType, com.github.thebridsk.utilities.logging.Level.FINE)()( Position(filename,null,21) )
        filter.isLogged(traceMsg) mustBe false
      }
    }


  }

//  it should "Not log (goodFilterlist&filterlist)*loggerlist" in {
  {

    val filter = new LogFilter

    val filtered = Table( ("filenames", "filtered logger names"),
                          combos(filterlist:::goodFilterlist, loggerlist): _* )

    forAll (filtered) { (filename: String, loggername: String) =>
      it should s"2 Not log ${filename} ${loggername}" in {
        val traceMsg = com.github.thebridsk.utilities.logging.TraceMsg(0, loggername, com.github.thebridsk.utilities.logging.LogMsgType, com.github.thebridsk.utilities.logging.Level.FINE)()( Position(filename,null,21) )
        filter.isLogged(traceMsg) mustBe false
      }
    }


  }

//  it should "log goodFilterlist*goodloggerlist" in {
  {

    val filter = new LogFilter

    val filtered = Table( ("filenames", "filtered logger names"),
                          combos(goodFilterlist, goodLoggerlist): _* )

    forAll (filtered) { (filename: String, loggername: String) =>
      it should s"3 log ${filename} ${loggername}" in {
        val traceMsg = com.github.thebridsk.utilities.logging.TraceMsg(0, loggername, com.github.thebridsk.utilities.logging.LogMsgType, com.github.thebridsk.utilities.logging.Level.FINE)()( Position(filename,null,21) )
        filter.isLogged(traceMsg) mustBe true
      }
    }


  }

}
