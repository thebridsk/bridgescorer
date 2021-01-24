package com.github.thebridsk.bridge.client.controller

import com.github.thebridsk.bridge.clientcommon.rest2.RestClientBoardSet
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.client.bridge.action.BridgeDispatcher
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientMovement
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientBoardSetsAndMovements
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxCall
import scala.concurrent.duration.Duration
import com.github.thebridsk.bridge.data.BoardSetsAndMovements
import com.github.thebridsk.bridge.data.rest.JsonSupport
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo
import com.github.thebridsk.bridge.client.bridge.store.BoardSetStore
import com.github.thebridsk.bridge.clientcommon.rest2.Result
import com.github.thebridsk.bridge.clientcommon.rest2.ResultObject
import scala.concurrent.Future

object BoardSetController {
  val logger: Logger = Logger("bridge.BoardSetController")

  def getBoardSets(): Result[Unit] = {
    val r = if (!BridgeDemo.isDemo) {
      logger.fine("getting boardsets")
      RestClientBoardSet
        .list()
        .recordFailure()
        .map(items => {
          logger.fine("got boardsets")
          BridgeDispatcher.updateAllBoardSet(items.toList)
        })
    } else {
      getBoardsetsAndMovements()
    }
    r.onComplete(v => ())
    r
  }

  def getBoardSet(name: BoardSet.Id): Result[Unit] = {
    val r = if (!BridgeDemo.isDemo) {
      RestClientBoardSet
        .get(name)
        .recordFailure()
        .map(item => {
          BridgeDispatcher.updateBoardSet(item)
        })
    } else {
      getBoardsetsAndMovements()
    }
    r.onComplete(v => ())
    r
  }

  def updateBoardSet(bs: BoardSet): Result[Unit] = {
    val r = if (!BridgeDemo.isDemo) {
      RestClientBoardSet
        .update(bs.id, bs)
        .recordFailure()
        .map(item => {
          BridgeDispatcher.updateBoardSet(bs)
        })
    } else {
      getBoardsetsAndMovements {
        BridgeDispatcher.updateBoardSet(bs)
      }
    }
    r.onComplete(v => ())
    r
  }

  def createBoardSet(bs: BoardSet): Result[BoardSet] = {
    val r = if (!BridgeDemo.isDemo) {
      RestClientBoardSet.create(bs).recordFailure().map { item =>
        BridgeDispatcher.createBoardSet(bs)
        bs
      }
    } else {
      new ResultObject[BoardSet](Future {
        getBoardsetsAndMovements {
          BridgeDispatcher.updateBoardSet(bs)
        }
        bs
      })
    }
    r.onComplete(v => ())
    r
  }

  def deleteBoardSet(bs: BoardSet.Id): Result[Unit] = {
    val r = if (!BridgeDemo.isDemo) {
      RestClientBoardSet.delete(bs).recordFailure().map { item =>
        BridgeDispatcher.deleteBoardSet(bs)
      }
    } else {
      getBoardsetsAndMovements {
        BridgeDispatcher.deleteBoardSet(bs)
      }
    }
    r.onComplete(v => ())
    r
  }

  def getMovement(): Result[Unit] = {
    val r = if (!BridgeDemo.isDemo) {
      RestClientMovement
        .list()
        .recordFailure()
        .map(items => {
          BridgeDispatcher.updateAllMovement(items.toList)
        })
    } else {
      getBoardsetsAndMovements()
    }
    r.onComplete(v => ())
    r
  }

  def getMovement(name: Movement.Id): Result[Unit] = {
    val r = if (!BridgeDemo.isDemo) {
      RestClientMovement
        .get(name)
        .recordFailure()
        .map(item => {
          BridgeDispatcher.updateMovement(item)
        })
    } else {
      getBoardsetsAndMovements()
    }
    r.onComplete(v => ())
    r
  }

  def updateMovement(mov: Movement): Result[Unit] = {
    val r = if (!BridgeDemo.isDemo) {
      RestClientMovement
        .update(mov.id, mov)
        .recordFailure()
        .map(item => {
          BridgeDispatcher.updateMovement(mov)
        })
    } else {
      getBoardsetsAndMovements {
        BridgeDispatcher.updateMovement(mov)
      }
    }
    r.onComplete(v => ())
    r
  }

  def createMovement(mov: Movement): Result[Movement] = {
    val r = if (!BridgeDemo.isDemo) {
      RestClientMovement.create(mov).recordFailure().map { item =>
        BridgeDispatcher.createMovement(mov)
        mov
      }
    } else {
      new ResultObject[Movement](Future {
        getBoardsetsAndMovements {
          BridgeDispatcher.updateMovement(mov)
        }
        mov
      })
    }
    r.onComplete(v => ())
    r
  }

  def deleteMovement(mov: Movement.Id): Result[Unit] = {
    val r = if (!BridgeDemo.isDemo) {
      RestClientMovement.delete(mov).recordFailure().map { item =>
        BridgeDispatcher.deleteMovement(mov)
      }
    } else {
      getBoardsetsAndMovements {
        BridgeDispatcher.deleteMovement(mov)
      }
    }
    r.onComplete(v => ())
    r
  }

  def getBoardsetsAndMovements(demoCB: => Unit = {}): Result[Unit] = {
    val r = if (!BridgeDemo.isDemo) {
      RestClientBoardSetsAndMovements
        .list()
        .recordFailure()
        .map(items => {
          items.foreach { bm =>
            BridgeDispatcher
              .updateAllBoardSetAndMovements(bm.boardsets, bm.movements, bm.individualmovements)
          }
        })
    } else {
      if (BoardSetStore.getBoardSets().isEmpty) {
        AjaxCall
          .send(
            method = "GET",
            url = "demo/boardsetsAndMovements.json",
            data = null,
            timeout = Duration("30s"),
            headers = Map[String, String](),
            withCredentials = false,
            responseType = "text/plain"
          )
          .map { wxhr =>
            val r = wxhr.responseText
            import JsonSupport._
            val bm = JsonSupport.readJson[BoardSetsAndMovements](r)
            BridgeDispatcher.updateAllBoardSetAndMovements(
              bm.boardsets,
              bm.movements,
              List.empty
            )
            demoCB
          }
      } else {
        new ResultObject(demoCB)
      }
    }
    r.onComplete(v => ())
    r
  }
}
