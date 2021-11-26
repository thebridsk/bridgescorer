package com.github.thebridsk.bridge.server.manualtest

import com.github.thebridsk.utilities.main.MainNoArgs
import com.github.thebridsk.bridge.data.bridge.BoardScore

object TestIMPs extends MainNoArgs {

  def execute(): Int = {

    for (i <- 0 to 5000 by 10) {
      val imps = BoardScore.getIMPs(i)
      println(f"$i%4d  $imps%.1f")
    }

    0
  }
}
