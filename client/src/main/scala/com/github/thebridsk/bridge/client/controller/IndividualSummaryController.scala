package com.github.thebridsk.bridge.client.controller

import com.github.thebridsk.bridge.clientcommon.rest2.AjaxCall
import scala.concurrent.duration._
import com.github.thebridsk.bridge.clientcommon.BridgeExecutionContext.global
import scala.util.Success
import com.github.thebridsk.bridge.data.rest.JsonSupport
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.client.bridge.action.BridgeDispatcher
import scala.util.Failure
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo
import com.github.thebridsk.bridge.client.bridge.store.IndividualDuplicateSummaryStore
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientIndividualDuplicateSummary
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import com.github.thebridsk.bridge.clientcommon.graphql.GraphQLClient
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsDefined
import play.api.libs.json.Json
import com.github.thebridsk.bridge.data.bridge.individual.DuplicateSummary
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import play.api.libs.json.JsUndefined
import com.github.thebridsk.utilities.logging.Logger

object IndividualSummaryController {

  val logger: Logger = Logger("bridge.IndividualSummaryController")

  private def getDemoSummary(error: () => Unit): Unit = {
    val x = AjaxCall.send(
      method = "GET",
      url = "demo/demoMatchDuplicates.json",
      data = null,
      timeout = Duration("30s"),
      headers = Map[String, String](),
      withCredentials = false,
      responseType = "text/plain"
    )
    x.onComplete { t =>
      t match {
        case Success(wxhr) =>
          val r = wxhr.responseText
          import JsonSupport._
          val bm = JsonSupport.readJson[List[MatchDuplicate]](r)
//          logger.info(s"Controller.getDemoSummary got json = ${bm}")
          BridgeDispatcher.updateIndividualDuplicateSummaryDemoMatch(None, bm)
        case Failure(err) =>
          error()
      }
    }
  }

  def getSummary(error: () => Unit /* = ()=>{} */ ): Unit = {
    logger.finer("Sending duplicatesummaries list request to server")
    import scala.scalajs.js.timers._
    setTimeout(1) { // note the absence of () =>
      if (BridgeDemo.isDemo) {
        IndividualDuplicateSummaryStore.getDuplicateSummary match {
          case Some(list) =>
            BridgeDispatcher.updateIndividualDuplicateSummary(None, list)
          case None =>
            getDemoSummary(error)
        }
      } else {
        RestClientIndividualDuplicateSummary.list().recordFailure().onComplete {
          trylist =>
            trylist match {
              case Success(list) =>
                Alerter.tryitWithUnit {
                  logger.finer(s"DuplicateSummary got ${list.size} entries")
                  BridgeDispatcher.updateIndividualDuplicateSummary(None, list.toList)
                }
              case Failure(err) =>
                error()
            }
        }
      }
    }
  }

  def getImportSummary(importId: String, error: () => Unit): Unit = {
    logger.finer(
      s"Sending import duplicatesummaries ${importId} list request to server"
    )
    throw new UnsupportedOperationException("getImportSummary need to be implemented")
    import scala.scalajs.js.timers._
    setTimeout(1) { // note the absence of () =>
      GraphQLClient
        .request(
          """query importDuplicates( $importId: ImportId! ) {
            |  import( id: $importId ) {
            |    duplicatesummaries {
            |      id
            |      finished
            |      teams {
            |        id
            |        team {
            |          id
            |          player1
            |          player2
            |          created
            |          updated
            |        }
            |        result
            |        place
            |      }
            |      boards
            |      tables
            |      onlyresult
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
          Some(JsObject(Seq("importId" -> JsString(importId)))),
          Some("importDuplicates")
        )
        .map { r =>
          r.data match {
            case Some(data) =>
              data \ "import" \ "duplicatesummaries" match {
                case JsDefined(jds) =>
                  import JsonSupport._
                  Json.fromJson[List[DuplicateSummary]](jds) match {
                    case JsSuccess(ds, path) =>
                      logger.finer(
                        s"Import(${importId})/DuplicateSummary got ${ds.size} entries"
                      )
                      BridgeDispatcher.updateIndividualDuplicateSummary(
                        Some(importId),
                        ds
                      )
                    case JsError(err) =>
                      logger.warning(
                        s"Import(${importId})/DuplicateSummary, JSON error: ${JsError.toJson(err)}"
                      )
                      error()
                  }
                case _: JsUndefined =>
                  logger.warning(
                    s"error import duplicatesummaries ${importId}, did not find import/duplicatesummaries field"
                  )
                  error()
              }
            case None =>
              logger.warning(
                s"error import duplicatesummaries ${importId}, ${r.getError()}"
              )
              error()
          }
        }
        .recover {
          case x: Exception =>
            logger.warning(
              s"exception import duplicatesummaries ${importId}",
              x
            )
            error()
        }
        .foreach { x => }
    }
  }

}
