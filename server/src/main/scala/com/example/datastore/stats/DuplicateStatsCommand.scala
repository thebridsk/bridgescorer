package com.example.datastore.stats

import utils.main.Subcommand
import com.example.datastore.DataStoreCommands
import com.example.datastore.ShowCommand
import org.rogach.scallop.ValueConverter
import scala.concurrent.duration._
import org.rogach.scallop._
import com.example.backend.BridgeService
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.Future
import scala.reflect.io.Path
import java.io.PrintStream
import com.example.data.Id
import com.example.data.MatchDuplicate

object DuplicateStatsCommand extends Subcommand("stats") {
  import DataStoreCommands.optionStore
  import ShowCommand.log

  implicit def dateConverter: ValueConverter[Duration] = singleArgConverter[Duration](Duration(_))

  import utils.main.Converters._

  descr("show all player stats in datastore")

  banner(s"""
Show player stats in a datastore

Syntax:
  ${DataStoreCommands.cmdName} show stats options
Options:""")

  val optionOut = opt[Path]("out",
                            short='o',
                            descr="The output csv file, default output to stdout",
                            argName="csv",
                            default=None)
  val optionPercent = toggle("percent",
                             default=Some(false),
                             short='p',
                             descrYes="Output stats as percent",
                             descrNo="Output stats as raw values" )

//  footer(s""" """)

  def await[T]( fut: Future[T] ) = Await.result(fut, 30.seconds)

  def executeSubcommand(): Int = {
    val store = optionStore()
    log.info(s"Using datastore ${store}")
    val bs = BridgeService( store )

    implicit val ps = optionOut.map { p =>
      log.info(s"Writing to ${p}")
      new PrintStream( p.jfile )
    }.getOrElse( new PrintStream(System.out) {
      override
      def close() = {}  // don't close System.out
    })

    import resource._

    managed(ps).foreach { out =>
      await( bs.duplicates.readAll() ) match {
        case Right( dups ) =>
          PlayerStats.statsToCsv( PlayerStats.stats(dups), optionPercent() )
          ContractStats.statsToCsv( ContractStats.stats(dups), optionPercent() )
        case Left(error) =>
          log.severe(s"Error reading datastore: ${error}")
      }
    }

    0
  }

}
