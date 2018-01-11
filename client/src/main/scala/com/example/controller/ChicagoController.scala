package com.example.controller

import utils.logging.Logger
import japgolly.scalajs.react._
import com.example.data.MatchChicago
import com.example.rest2.RestClientChicago
import com.example.bridge.store.ChicagoStore
import com.example.bridge.action.BridgeDispatcher
import com.example.data.Round
import com.example.data.Hand
import com.example.rest2.RestResult
import com.example.rest2.Result
import com.example.rest2.ResultRecorder
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.concurrent.CanAwait
import scala.util.Try
import scala.concurrent.Future
import com.example.rest2.ResultObject

object ChicagoController {
  val logger = Logger("bridge.ChicagoController")

  class CreateResultMatchChicago( result: Result[MatchChicago])(implicit executor: ExecutionContext) extends CreateResult[MatchChicago](result) {

    def updateStore( mc: MatchChicago ): MatchChicago = {
      showMatch(mc)
      logger.info(s"Created new chicago game: ${mc.id}")
      mc
    }

  }

  import scala.concurrent.ExecutionContext.Implicits.global

  def createMatch() = {
    logger.info("Sending create chicago to server")
    val chi = MatchChicago("",List("","","",""),Nil,0,false)
    val result = RestClientChicago.create(chi).recordFailure()
    new CreateResultMatchChicago(result)
  }

  def showMatch( chi: MatchChicago ) = {
    ChicagoStore.start(chi.id, chi)
    logger.fine("calling callback with "+chi.id)
  }

  def ensureMatch( chiid: String ) = {
    if (!ChicagoStore.isMonitoredId(chiid)) {
      val result = RestClientChicago.get(chiid).recordFailure()
      result.foreach( created=>{
        logger.info(s"PageChicago: got chicago game: ${created.id}")
        showMatch( created )
      })
      result
    } else {
      new ResultObject(ChicagoStore.getChicago.get)
    }
  }

  def updateMatch( chi: MatchChicago ) = {
    logger.info("dispatching an update to MatchChicago "+chi.id )
    BridgeDispatcher.updateChicago(chi, Some(updateServer))
  }

  def updateServer( chi: MatchChicago ) = {
    RestClientChicago.update(chi.id, chi).recordFailure().foreach( updated => {
      logger.fine(s"PageChicago: Updated chicago game: ${chi.id}")
      // the BridgeDispatcher.updateChicago causes a timing problem.
      // if two updates are done one right after the other, then the second
      // update will be lost.
//      BridgeDispatcher.updateChicago(updated)
    })
  }

  def updateChicagoNames( chiid: String, nplayer1: String, nplayer2: String, nplayer3: String, nplayer4: String, extra: Option[String], quintet: Boolean, simpleRotation: Boolean ) = {
    BridgeDispatcher.updateChicagoNames(chiid, nplayer1, nplayer2, nplayer3, nplayer4, extra, quintet, simpleRotation, Some(updateServer))
  }

  def updateChicago5( chiid: String, extraPlayer: String ) = {
    BridgeDispatcher.updateChicago5(chiid, extraPlayer, Some(updateServer))
  }

  def updateChicagoRound( chiid: String, round: Round ) = {
    BridgeDispatcher.updateChicagoRound(chiid, round, Some( updateServer ))
  }

  def updateChicagoHand( chiid: String, roundid: Int, handid: Int, hand: Hand ) = {
    BridgeDispatcher.updateChicagoHand(chiid, roundid, handid, hand, Some( updateServer ))
  }

}
