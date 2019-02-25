package com.example.manualtest

import utils.main.Main
import scala.reflect.io.Path
import com.example.backend.BridgeService
import scala.concurrent.ExecutionContext.Implicits.global
import com.example.data.duplicate.stats.PlayersOpponentsStats
import com.example.backend.resource.SyncStore
import scala.concurrent.duration._

object OpponentStats extends Main {

  import utils.main.Converters._

  val optionStore = opt[Path]("store", short='s', descr="The store directory, default=./store", argName="dir", default=Some("./store"))

  val timeout = 120

  def execute() = {

    val bs = BridgeService(optionStore.toOption.get)
    val dupstore = new SyncStore(bs.duplicates)

    dupstore.readAll(timeout.second) match {
      case Right(dups) =>
        val stats = PlayersOpponentsStats.stats( dups )

        System.out.println(stats)

      case Left(err) =>
        System.out.println(s"""Error reading MatchDuplicate games: $err""")
    }

    0
  }
}
