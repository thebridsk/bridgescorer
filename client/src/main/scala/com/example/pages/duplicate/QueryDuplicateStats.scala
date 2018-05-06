package com.example.pages.duplicate

import com.example.data.duplicate.stats.DuplicateStats
import play.api.libs.json.Json
import com.example.graphql.GraphQLClient
import play.api.libs.json.JsObject
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import utils.logging.Logger
import com.example.data.rest.JsonSupport

object QueryDuplicateStats {
  import JsonSupport._
  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger("bridge.QueryDuplicateStats")

  case class StatResult( duplicatestats: DuplicateStats )

  implicit val StatResultReads = Json.reads[StatResult]

  def duplicateStats() = {

    val vars = None
    val query =
       """{
         |  duplicatestats {
         |    playerStats {
         |      declarer {
         |        player
         |        declarer
         |        contractType
         |        handsPlayed
         |        histogram {
         |          tricks, counter
         |        }
         |      }
         |      defender {
         |        player
         |        declarer
         |        contractType
         |        handsPlayed
         |        histogram {
         |          tricks, counter
         |        }
         |      }
         |      min
         |      max
         |    }
         |    contractStats {
         |      data {
         |        contract
         |        contractType
         |        histogram {
         |          tricks, counter
         |        }
         |        handsPlayed
         |      }
         |      min
         |      max
         |    }
         |    playerDoubledStats {
         |      declarer {
         |        player
         |        declarer
         |        contractType
         |        handsPlayed
         |        histogram {
         |          tricks, counter
         |        }
         |      }
         |      defender {
         |        player
         |        declarer
         |        contractType
         |        handsPlayed
         |        histogram {
         |          tricks, counter
         |        }
         |      }
         |      min
         |      max
         |    }
         |  }
         |}
         |""".stripMargin
    val operation = None

    GraphQLClient.request(query, vars, operation ).map { resp =>
      resp.data match {
        case Some( d: JsObject ) =>
          Json.fromJson[StatResult](d) match {
            case JsSuccess( t, path ) =>
              Right(t)
            case err: JsError =>
              logger.warning( s"Error processing return data from duplicate stats: ${JsError.toJson(err)}" )
              Left("Error processing returned data")
          }
        case _ =>
          logger.warning( s"Error on Imports list: ${resp}")
          Left("Internal error")
      }
    }.recover {
      case x: Exception =>
        logger.warning( s"Error on Imports list", x)
        Left("Internal error")
    }

  }
}
