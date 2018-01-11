package com.example.manualtest

import utils.main.Main
import scala.reflect.io.Directory
import com.example.backend.BridgeServiceFileStore
import utils.logging.Logger
import com.example.data.RestMessage
import scala.concurrent.ExecutionContext.Implicits.global

object LoadBridgeStore extends Main {

  def execute(): Int = {

    val d = Directory( "store")
    val bs =  new BridgeServiceFileStore( d )

    println("Created a new BridgeServiceFileStore")
    logger.info("Created a new BridgeServiceFileStore")

    bs.duplicates.syncStore.readAll() match {
      case Right(map) =>
        println("Got "+map.size+" entries")
        logger.info("Got "+map.size+" entries")

        map.find{ p => true }.foreach(md => logger.warning("Got an entry "+md))

      case Left((statusCode,restMessage)) =>
        logger.severe("Got error: "+statusCode+": "+restMessage)

    }

    0
  }
}
