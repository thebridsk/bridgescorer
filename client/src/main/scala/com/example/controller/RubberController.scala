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
import com.example.rest2.RestResult
import org.scalactic.source.Position
import scala.concurrent.Future
import com.example.rest2.WrapperXMLHttpRequest
import com.example.logger.Alerter
import com.example.graphql.GraphQLClient
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsDefined
import com.example.data.rest.JsonSupport
import play.api.libs.json.Json
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import play.api.libs.json.JsUndefined

object RubberController {
  val logger = Logger("bridge.RubberController")

  class CreateResultMatchRubber(
                                ajaxResult: AjaxResult[WrapperXMLHttpRequest],
                                future: Future[MatchRubber]
                               )(
                                 implicit
                                   pos: Position,
                                   executor: ExecutionContext
                               ) extends CreateResult[MatchRubber](ajaxResult,future) {

    def this( result: RestResult[MatchRubber])(implicit executor: ExecutionContext) = {
      this(result.ajaxResult, result.future )
    }

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

  def getSummary(): Unit = {
    logger.finer("Sending rubbers list request to server")
    import scala.scalajs.js.timers._
    setTimeout(1) { // note the absence of () =>
      RestClientRubber.list().recordFailure().foreach { list => Alerter.tryitWithUnit {
        logger.finer(s"RubberList got ${list.size} entries")
        BridgeDispatcher.updateRubberList(None,list)
      }}
    }
  }

  def getImportSummary( importId: String ): Unit = {
    logger.finer(s"Sending import rubbersummaries ${importId} list request to server")
    import scala.scalajs.js.timers._
    setTimeout(1) { // note the absence of () =>
      GraphQLClient.request(
          """query importRubbers( $importId: ImportId! ) {
             |  import( id: $importId ) {
             |    rubbers {
             |      id
             |      north
             |      south
             |      east
             |      west
             |      dealerFirstHand
             |      hands {
             |        id
             |        hand {
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
             |        honors
             |        honorsPlayer
             |        created
             |        updated
             |      }
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
          Some("importRubbers") ).map { r =>
            r.data match {
              case Some(data) =>
                data \ "import" \ "rubbers" match {
                  case JsDefined( jds ) =>
                    import JsonSupport._
                    Json.fromJson[List[MatchRubber]](jds) match {
                      case JsSuccess(ds,path) =>
                        logger.finer(s"Import(${importId})/rubbers got ${ds.size} entries")
                        BridgeDispatcher.updateRubberList(Some(importId),ds.toArray)
                      case JsError(err) =>
                        logger.warning(s"Import(${importId})/chicagos, JSON error: ${JsError.toJson(err)}")
                    }
                  case _: JsUndefined =>
                    logger.warning(s"error import rubber list ${importId}, did not find import/rubbers field")
                }
              case None =>
                logger.warning(s"error import rubber list ${importId}, ${r.getError()}")
            }
          }.recover {
            case x: Exception =>
                logger.warning(s"exception import rubber list ${importId}", x)
          }.foreach { x => }
    }
  }

  def deleteRubber( id: String) = {
    BridgeDispatcher.deleteRubber(id)
    RestClientRubber.delete(id).recordFailure()
  }

}
