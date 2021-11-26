package com.github.thebridsk.bridge.server.manualtest

import com.github.thebridsk.bridge.server.backend.BridgeServiceInMemory
import com.github.thebridsk.bridge.data.BoardSetsAndMovements
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._
import com.github.thebridsk.bridge.data.rest.JsonSupport._
import com.github.thebridsk.bridge.data.rest.JsonSupport
import com.github.thebridsk.utilities.file.FileIO
import scala.concurrent.ExecutionContextExecutor
import com.github.thebridsk.utilities.main.MainNoArgs

object CreateBoardMovementForTest extends MainNoArgs {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  def execute(): Int = {
    val bs = new BridgeServiceInMemory("test")

    val bf = bs.boardSets.readAll().map { r =>
      r match {
        case Right(boardsets) => boardsets.values.toList
        case Left(err)        => List()
      }
    }

    val b = Await.result(bf, Duration("30s"))

    val mf = bs.movements.readAll().map { r =>
      r match {
        case Right(movements) => movements.values.toList
        case Left(err)        => List()
      }
    }

    val m = Await.result(mf, Duration("30s"))

    val imf = bs.individualMovements.readAll().map { r =>
      r match {
        case Right(movements) => movements.values.toList
        case Left(err)        => List()
      }
    }

    val im = Await.result(imf, Duration("30s"))

    val bsm = BoardSetsAndMovements(b, m, im)

    val json = JsonSupport.writeJson(bsm)

    FileIO.writeFileSafe("boardsetAndMovements.json", json)

    0
  }
}
