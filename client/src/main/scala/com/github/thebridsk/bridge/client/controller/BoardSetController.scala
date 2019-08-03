package com.github.thebridsk.bridge.client.controller

import com.github.thebridsk.bridge.clientcommon.rest2.RestClientBoardSet
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.client.bridge.action.BridgeDispatcher
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientMovement
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientBoardSetsAndMovements
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxResult
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientTestBoardsetsAndMovements
import com.github.thebridsk.bridge.client.Bridge
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxCall
import scala.concurrent.duration.Duration
import com.github.thebridsk.bridge.data.BoardSetsAndMovements
import com.github.thebridsk.bridge.data.rest.JsonSupport
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo

object BoardSetController {
  val logger = Logger("bridge.BoardSetController")

  def getBoardSets(): Unit = {
    if (!BridgeDemo.isDemo) {
      RestClientBoardSet.list().recordFailure().foreach( items => {
        BridgeDispatcher.updateAllBoardSet(items.toList)
      })
    } else {
      getBoardsetsAndMovements()
    }
  }

  def getBoardSet( name: String): Unit = {
    if (!BridgeDemo.isDemo) {
      RestClientBoardSet.get(name).recordFailure().foreach( item => {
        BridgeDispatcher.updateBoardSet(item)
      })
    } else {
      getBoardsetsAndMovements()
    }
  }

  def getMovement(): Unit = {
    if (!BridgeDemo.isDemo) {
      RestClientMovement.list().recordFailure().foreach( items => {
        BridgeDispatcher.updateAllMovement(items.toList)
      })
    } else {
      getBoardsetsAndMovements()
    }
  }

  def getMovement( name: String): Unit = {
    if (!BridgeDemo.isDemo) {
      RestClientMovement.get(name).recordFailure().foreach( item => {
        BridgeDispatcher.updateMovement(item)
      })
    } else {
      getBoardsetsAndMovements()
    }
  }

  def getBoardsetsAndMovements(): Unit = {
    if (!BridgeDemo.isDemo) {
      RestClientBoardSetsAndMovements.list().recordFailure().foreach( items => {
        items.foreach { bm =>
          BridgeDispatcher.updateAllBoardSetAndMovements(bm.boardsets,bm.movements)
        }
      })
    } else {
      AjaxCall.send(
        method = "GET",
        url = "demo/boardsetsAndMovements.json",
        data = null,
        timeout = Duration("30s"),
        headers = Map[String, String](),
        withCredentials = false,
        responseType = "text/plain"
      ).map { wxhr =>
        val r = wxhr.responseText
        import JsonSupport._
        val bm = JsonSupport.readJson[BoardSetsAndMovements](r)
        BridgeDispatcher.updateAllBoardSetAndMovements(bm.boardsets,bm.movements)
      }
    }
  }
}
