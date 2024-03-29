package com.github.thebridsk.bridge.server.manualtest

import com.github.thebridsk.utilities.main.Main
import scala.reflect.io.Path
import com.github.thebridsk.bridge.server.backend.BridgeService
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.data.duplicate.stats.PlayersOpponentsStats
import com.github.thebridsk.bridge.server.backend.resource.SyncStore
import scala.concurrent.duration._
import org.rogach.scallop.ScallopOption
import com.github.thebridsk.utilities.main.MainConf

class OpponentStatsConf extends MainConf {

  import com.github.thebridsk.utilities.main.Converters._

  val optionStore: ScallopOption[Path] = opt[Path](
    "store",
    short = 's',
    descr = "The store directory, default=./store",
    argName = "dir",
    default = Some("./store")
  )

}
object OpponentStats extends Main[OpponentStatsConf] {

  val timeout = 120

  import config._

  def execute(): Int = {

    val bs = BridgeService(optionStore.toOption.get)
    val dupstore = new SyncStore(bs.duplicates)

    dupstore.readAll(timeout.second) match {
      case Right(dups) =>
        val stats = PlayersOpponentsStats.stats(dups)

        System.out.println(stats)

      case Left(err) =>
        System.out.println(s"""Error reading MatchDuplicate games: $err""")
    }

    0
  }
}
