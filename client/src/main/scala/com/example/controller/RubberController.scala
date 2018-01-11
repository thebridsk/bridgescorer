package com.example.controller

import utils.logging.Logger
import japgolly.scalajs.react._
import com.example.data.MatchRubber
import com.example.rest2.RestClientRubber
import com.example.bridge.store.RubberStore
import com.example.bridge.action.BridgeDispatcher
import com.example.data.Round
import com.example.data.Hand
import com.example.data.bridge.PlayerPosition
import com.example.data.RubberHand
import com.example.rest2.AjaxResult
import com.example.rest2.Result
import scala.concurrent.ExecutionContext
import com.example.rest2.ResultObject

object RubberController {
  val logger = Logger("bridge.RubberController")

  class CreateResultMatchRubber( result: Result[MatchRubber])(implicit executor: ExecutionContext) extends CreateResult[MatchRubber](result) {

    def updateStore( mc: MatchRubber ): MatchRubber = {
      showMatch(mc)
      logger.info(s"Created new rubber game: ${mc.id}")
      mc
    }

  }

  import scala.concurrent.ExecutionContext.Implicits.global

  def createMatch() = {
    AjaxResult.isEnabled match {
      case Some(true) | None =>
        // enabled - Some(true)
        // mocked - None
        logger.info("Sending create rubber to server")
        val rub = MatchRubber("","","","","","",Nil)
        val r = RestClientRubber.create(rub).recordFailure()
        new CreateResultMatchRubber(r)
      case Some(false) =>
        // disabled
        val created = MatchRubber("R9999","","","","","",Nil)
        logger.info("PageRubber: created new local rubber game: "+created.id)
        showMatch( created )
        new ResultObject(created)
    }
  }

  def showMatch( rub: MatchRubber ) = {
    RubberStore.start(rub.id, rub)
    logger.fine("calling callback with "+rub.id)
  }

  def ensureMatch( rubid: String ) = {
    if (!RubberStore.isMonitoredId(rubid)) {
      val result = RestClientRubber.get(rubid).recordFailure()
      result.foreach( created => {
        logger.info("PageRubber: got rubber game: "+created.id)
        showMatch( created )
      })
      result
    } else {
      new ResultObject( RubberStore.getRubber.get )
    }
  }

  def updateMatch( rub: MatchRubber ) = {
    logger.info("dispatrubng an update to MatchRubber "+rub.id )
    BridgeDispatcher.updateRubber(rub, Some(updateServer))
  }

  def updateServer( rub: MatchRubber ) = {
    RestClientRubber.update(rub.id, rub).recordFailure().foreach( updated => {
      logger.fine("PageRubber: Updated rubber game: "+rub.id)
      // the BridgeDispatcher.updateRubber causes a timing problem.
      // if two updates are done one right after the other, then the second
      // update will be lost.
//      BridgeDispatcher.updateRubber(updated)
    })
  }

  def updateRubberNames( rubid: String, north: String, south: String, east: String, west: String, firstDealer: PlayerPosition ) = {
    BridgeDispatcher.updateRubberNames(rubid, north, south, east, west, firstDealer, Some(updateServer))
  }

  def updateRubberHand( rubid: String, handid: String, hand: RubberHand ) = {
    BridgeDispatcher.updateRubberHand(rubid, handid, hand, Some( updateServer ))
  }

}
