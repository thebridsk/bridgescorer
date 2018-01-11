package com.example.manualtest

import utils.main.Main
import scala.reflect.io.Path
import org.rogach.scallop._
import utils.main.Converters._
import utils.logging.Logger
import java.util.logging.Level
import com.example.backend.BridgeServiceFileStore
import com.example.backend.BridgeService
import com.example.data.MatchDuplicate
import com.example.backend.BridgeServiceInMemory
import com.example.backend.resource.SyncStore
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object CheckAndFixBoardsInMatches extends Main {

  val optionStore = opt[Path]("store", short='s', descr="The store directory, default=./store", argName="dir", default=Some("./store"))

  def execute(): Int = {
    val bs = optionStore.toOption match {
      case Some(p) =>
        val d = p.toDirectory
        if (!d.isDirectory) {
          if (!d.createDirectory().isDirectory) {
            logger.severe("Unable to create directory for FileStore: "+d)
            return 1
          }
        }
        logger.info("Fixing store "+d)
        new BridgeServiceFileStore( d )
      case None => None
        logger.severe("Must specify a directory for FileStore")
        return 1
    }

    val matchkeys = bs.duplicates.syncStore.readAll() match {
      case Right(result) =>
        Seq( result.keySet.toList: _* )
      case Left(error) =>
        logger.severe("Error reading filestore: "+error._2.msg)
        return 2
    }

    val bridgeServer = new BridgeServiceInMemory()
    logger.info("Keys are "+matchkeys.mkString(", "))
    Await.result( bridgeServer.fillBoards(MatchDuplicate.create("xx")), 30.seconds) match {
      case Right(correct) =>
        matchkeys.flatMap { id => bs.duplicates.syncStore.read(id) match {
          case Right(result) => Some(result)
          case Left(error) =>
            logger.warning("Did not find MatchDuplicate for id "+id+": "+error._2.msg)
            None
          }
        }.foreach {md =>
          val (fixed,msgs) = md.fixVulnerability(correct)
          if (msgs.isEmpty) {
            logger.info("Duplicate match with ID "+md.id+" is correct")
          } else {
            logger.info("Fixing duplicate match with ID "+md.id+", errors were:")
            msgs.foreach { msg => logger.info("  "+msg) }
            bs.duplicates.syncStore.update(fixed.id, fixed)
          }
          }
        0
      case Left((code,msg)) =>
        println( "Did not fill boards: "+code+" "+msg.msg )
        1
    }
  }

}
