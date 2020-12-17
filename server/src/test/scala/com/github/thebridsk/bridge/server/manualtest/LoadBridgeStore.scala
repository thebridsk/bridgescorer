package com.github.thebridsk.bridge.server.manualtest

import scala.reflect.io.Directory
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStore
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.data.DifferenceWrappers
import com.github.thebridsk.utilities.main.MainNoArgs

object LoadBridgeStore extends MainNoArgs {

  def execute(): Int = {

    val d = Directory("store")
    val bs = new BridgeServiceFileStore(d)

    println("Created a new BridgeServiceFileStore")
    logger.info("Created a new BridgeServiceFileStore")

    bs.duplicates.syncStore.readAll() match {
      case Right(map) =>
        println("Got " + map.size + " entries")
        logger.info("Got " + map.size + " entries")

        map
          .find { p => true }
          .foreach(md => logger.warning("Got an entry " + md))

        val list = map.values.toList
        val len = list.length
        for (
          i <- 0 until len;
          j <- i until len
        ) {
          val l = list(i)
          val r = list(j)

          import DifferenceWrappers._
          val diff = l.difference("", r)
          logger.info(
            f"""difference between ${l.id} and ${r.id}: same=${diff.percentSame}%.1f%% diff=${diff}"""
          )
        }

      case Left((statusCode, restMessage)) =>
        logger.severe("Got error: " + statusCode + ": " + restMessage)

    }

    0
  }
}
