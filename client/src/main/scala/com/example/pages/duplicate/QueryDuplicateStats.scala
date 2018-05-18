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

  val fragComparisonStats = """
    |fragment comparisonFields on PlayerComparisonStats {
    |  data {
    |    player
    |    sameside
    |    aggressivegood
    |    aggressivebad
    |    aggressiveneutral
    |    passivegood
    |    passivebad
    |    passiveneutral
    |  }
    |}
    """.stripMargin

  val fragContractStats = """
    |fragment contractFields on DuplicateContractStats {
    |  data {
    |    contract
    |    contractType
    |    histogram {
    |      tricks, counter
    |    }
    |    handsPlayed
    |  }
    |  min
    |  max
    |}
    """.stripMargin

  val fragPlayerStats = """
    |fragment playerFields on DuplicatePlayerStats {
    |  declarer {
    |    player
    |    declarer
    |    contractType
    |    handsPlayed
    |    histogram {
    |      tricks, counter
    |    }
    |  }
    |  defender {
    |    player
    |    declarer
    |    contractType
    |    handsPlayed
    |    histogram {
    |      tricks, counter
    |    }
    |  }
    |  min
    |  max
    |}
    """.stripMargin

  private val queryPlayerStats =
       """
         |    playerStats {
         |      ...playerFields
         |    }
         |""".stripMargin

  private val queryContractStats =
       """
         |    contractStats {
         |      ...contractFields
         |    }
         |""".stripMargin

  private val queryPlayerDoubledStats =
       """
         |    playerDoubledStats {
         |      ...playerFields
         |    }
         |""".stripMargin

  private val queryComparisonStats =
       """
         |    comparisonStats {
         |      ...comparisonFields
         |    }
         |""".stripMargin

  def getDuplicateStats(
      playerStats: Boolean = false,
      contractStats: Boolean = false,
      playerDoubledStats: Boolean = false,
      comparisonStats: Boolean = false
  ) = {
    val query =
       """{
         |  duplicatestats {
         |""".stripMargin +
         (if (playerStats) queryPlayerStats else "") +
         (if (contractStats) queryContractStats else "") +
         (if (playerDoubledStats) queryPlayerDoubledStats else "") +
         (if (comparisonStats) queryComparisonStats else "") +
       """
         |  }
         |}
         |""".stripMargin +
         (if (comparisonStats) fragComparisonStats else "")+
         (if (contractStats) fragContractStats else "")+
         (if (playerStats || playerDoubledStats) fragPlayerStats else "")
    makeQuery(query)
  }

  def duplicateStats() = getDuplicateStats(true,true,true,true)

  def makeQuery( query: String ) = {

    val vars = None
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
