package com.github.thebridsk.bridge.manualtest

import com.github.thebridsk.utilities.main.Main
import com.github.thebridsk.bridge.backend.BridgeServiceInMemory
import com.github.thebridsk.bridge.data.BoardSetsAndMovements
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._
import com.github.thebridsk.bridge.data.rest.JsonSupport._
import com.github.thebridsk.bridge.data.rest.JsonSupport
import com.github.thebridsk.bridge.backend.resource.FileIO

object CreateBoardMovementForTest extends Main {

  implicit val ec = ExecutionContext.global

  def execute() = {
    val bs = new BridgeServiceInMemory("test")

    val bf = bs.boardSets.readAll().map{ r =>
      r match {
        case Right(boardsets) => boardsets.values.toList
        case Left(err) => List()
      }
    }

    val b = Await.result(bf, Duration("30s"))

    val mf = bs.movements.readAll().map{ r =>
      r match {
        case Right(movements) => movements.values.toList
        case Left(err) => List()
      }
    }

    val m = Await.result(mf, Duration("30s"))

    val bsm = BoardSetsAndMovements(b,m)

    val json = JsonSupport.writeJson(bsm)

    FileIO.writeFileSafe("boardsetAndMovements.json", json)

    0
  }
}