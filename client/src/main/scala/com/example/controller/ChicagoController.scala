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
import com.example.rest2.AjaxResult
import org.scalactic.source.Position
import com.example.rest2.WrapperXMLHttpRequest
import com.example.graphql.GraphQLClient
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsDefined
import play.api.libs.json.Json
import com.example.data.rest.JsonSupport
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import play.api.libs.json.JsUndefined
import com.example.logger.Alerter
import com.example.data.Id
import com.example.Bridge
import com.example.bridge.store.ChicagoSummaryStore
import scala.util.Success
import scala.util.Failure

object ChicagoController {
  val logger = Logger("bridge.ChicagoController")

  class CreateResultMatchChicago(
                                  ajaxResult: AjaxResult[WrapperXMLHttpRequest],
                                  future: Future[MatchChicago]
                                )(
                                  implicit
                                    pos: Position,
                                    executor: ExecutionContext
                                ) extends CreateResult[MatchChicago](ajaxResult,future) {

    def this( result: RestResult[MatchChicago])(implicit executor: ExecutionContext) = {
      this(result.ajaxResult, result.future )
    }

    def updateStore( mc: MatchChicago ): MatchChicago = {
      showMatch(mc)
      logger.info(s"Created new chicago game: ${mc.id}")
      mc
    }

  }

  import scala.concurrent.ExecutionContext.Implicits.global

  private var currentId = 0

  def createMatch() = {
    logger.info("Sending create chicago to server")
    if (Bridge.isDemo) {
      currentId = currentId + 1
      val chi = MatchChicago(s"C$currentId",List("","","",""),Nil,0,false)
      new CreateResultMatchChicago(null, Future(chi))
    } else {
      val chi = MatchChicago("",List("","","",""),Nil,0,false)
      val result = RestClientChicago.create(chi).recordFailure()
      new CreateResultMatchChicago(result)
    }
  }

  def showMatch( chi: MatchChicago ) = {
    ChicagoStore.start(chi.id, chi)
    logger.fine("calling callback with "+chi.id)
    BridgeDispatcher.updateChicago(chi)
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
    if (!Bridge.isDemo) {
      RestClientChicago.update(chi.id, chi).recordFailure().foreach( updated => {
        logger.fine(s"PageChicago: Updated chicago game: ${chi.id}")
        // the BridgeDispatcher.updateChicago causes a timing problem.
        // if two updates are done one right after the other, then the second
        // update will be lost.
  //      BridgeDispatcher.updateChicago(updated)
      })
    }
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

  def getSummary(error: ()=>Unit): Unit = {
    logger.finer("Sending duplicatesummaries list request to server")
    import scala.scalajs.js.timers._
    setTimeout(1) { // note the absence of () =>
      if (Bridge.isDemo) {
        val x = ChicagoSummaryStore.getChicagoSummary().getOrElse(Array())
        BridgeDispatcher.updateChicagoSummary(None, x)
      } else {
        RestClientChicago.list().recordFailure().onComplete { trylist =>
          Alerter.tryitWithUnit {
            trylist match {
              case Success(list) =>
                logger.finer(s"ChicagoSummary got ${list.size} entries")
                BridgeDispatcher.updateChicagoSummary(None,list)
              case Failure(err) =>
                error()
            }
          }
        }
      }
    }
  }

  def getImportSummary( importId: String, error: ()=>Unit ): Unit = {
    logger.finer(s"Sending import duplicatesummaries ${importId} list request to server")
    import scala.scalajs.js.timers._
    setTimeout(1) { // note the absence of () =>
      GraphQLClient.request(
          """query importDuplicates( $importId: ImportId! ) {
             |  import( id: $importId ) {
             |    chicagos {
             |      id
             |      players
             |      rounds {
             |        id
             |        north
             |        south
             |        east
             |        west
             |        dealerFirstRound
             |        hands {
             |          id
             |          contractTricks
             |          contractSuit
             |          contractDoubled
             |          declarer
             |          nsVul
             |          ewVul
             |          madeContract
             |          tricks
             |          created
             |          updated
             |        }
             |        created
             |        updated
             |      }
             |      gamesPerRound
             |      simpleRotation
             |      created
             |      updated
             |      bestMatch {
             |        id
             |        sameness
             |        differences
             |      }
             |    }
             |  }
             |}
             |""".stripMargin,
          Some(JsObject( Seq( "importId" -> JsString(importId) ) )),
          Some("importDuplicates") ).map { r =>
            r.data match {
              case Some(data) =>
                data \ "import" \ "chicagos" match {
                  case JsDefined( jds ) =>
                    import JsonSupport._
                    Json.fromJson[List[MatchChicago]](jds) match {
                      case JsSuccess(ds,path) =>
                        logger.finer(s"Import(${importId})/chicagos got ${ds.size} entries")
                        BridgeDispatcher.updateChicagoSummary(Some(importId),ds.toArray)
                      case JsError(err) =>
                        logger.warning(s"Import(${importId})/chicagos, JSON error: ${JsError.toJson(err)}")
                        error()
                    }
                  case _: JsUndefined =>
                    logger.warning(s"error import duplicatesummaries ${importId}, did not find import/duplicatesummaries field")
                    error()
                }
              case None =>
                logger.warning(s"error import duplicatesummaries ${importId}, ${r.getError()}")
                error()
            }
          }.recover {
            case x: Exception =>
                logger.warning(s"exception import duplicatesummaries ${importId}", x)
                error()
          }.foreach { x => }
    }
  }

  def deleteChicago( id: Id.MatchChicago) = {
    BridgeDispatcher.deleteChicago(id)
    if (!Bridge.isDemo) RestClientChicago.delete(id).recordFailure()
  }

}
