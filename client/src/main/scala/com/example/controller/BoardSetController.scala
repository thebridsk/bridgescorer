package com.example.controller

import com.example.rest2.RestClientBoardSet
import com.example.data.BoardSet
import com.example.bridge.action.BridgeDispatcher
import utils.logging.Logger
import com.example.data.Movement
import com.example.rest2.RestClientMovement
import scala.concurrent.ExecutionContext.Implicits.global
import com.example.rest2.RestClientBoardSetsAndMovements

object BoardSetController {
  val logger = Logger("bridge.BoardSetController")

  def getBoardSets() = {
    RestClientBoardSet.list().recordFailure().foreach( items => {
      BridgeDispatcher.updateAllBoardSet(items.toList)
    })
  }

  def getBoardSet( name: String) = {
    RestClientBoardSet.get(name).recordFailure().foreach( item => {
      BridgeDispatcher.updateBoardSet(item)
    })
  }

  def getMovement() = {
    RestClientMovement.list().recordFailure().foreach( items => {
      BridgeDispatcher.updateAllMovement(items.toList)
    })
  }

  def getMovement( name: String) = {
    RestClientMovement.get(name).recordFailure().foreach( item => {
      BridgeDispatcher.updateMovement(item)
    })
  }

  def getBoardsetsAndMovements() = {
    RestClientBoardSetsAndMovements.list().recordFailure().foreach( items => {
      items.foreach { bm =>
        BridgeDispatcher.updateAllBoardSetAndMovements(bm.boardsets,bm.movements)
      }
    })
  }
}
