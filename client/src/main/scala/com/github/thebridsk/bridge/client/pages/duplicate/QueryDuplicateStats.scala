package com.github.thebridsk.bridge.client.pages.duplicate

import com.github.thebridsk.bridge.data.duplicate.stats.DuplicateStats
import play.api.libs.json.Json
import com.github.thebridsk.bridge.clientcommon.graphql.GraphQLClient
import play.api.libs.json.JsObject
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.rest.JsonSupport
import com.github.thebridsk.bridge.clientcommon.rest2.Result
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientDuplicatePlayerPlaces
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerPlaces
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxResult
import play.api.libs.json.Reads

object QueryDuplicateStats {
  import JsonSupport._
  import scala.concurrent.ExecutionContext.Implicits.global

  val logger: Logger = Logger("bridge.QueryDuplicateStats")

  case class StatResult(duplicatestats: DuplicateStats)

  implicit val StatResultReads: Reads[StatResult] = Json.reads[StatResult]

  val fragComparisonStats: String =
    """
      |fragment comparisonFields on PlayerComparisonStats {
      |  data {
      |    player
      |    stattype
      |    aggressivegood
      |    aggressivebad
      |    aggressiveneutral
      |    passivegood
      |    passivebad
      |    passiveneutral
      |  }
      |}
      |""".stripMargin

  val fragContractStats: String =
    """
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

  val fragPlayerStats: String =
    """
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
      |""".stripMargin

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

  private val queryPlayersOpponentsStats =
    """
      |    playersOpponentsStats {
      |      players {
      |        player
      |        opponents {
      |          player
      |          opponent
      |          matchesPlayed
      |          matchesBeat
      |          matchesTied
      |          totalMP
      |          wonMP
      |        }
      |      }
      |    }
      |""".stripMargin

  def getDuplicateStats(
      playerStats: Boolean = false,
      contractStats: Boolean = false,
      playerDoubledStats: Boolean = false,
      comparisonStats: Boolean = false,
      playersOpponentsStats: Boolean = false
  ): AjaxResult[Either[String, StatResult]] = {
    val query =
      """{
        |  duplicatestats {
        |""".stripMargin +
        (if (playerStats) queryPlayerStats else "") +
        (if (contractStats) queryContractStats else "") +
        (if (playerDoubledStats) queryPlayerDoubledStats else "") +
        (if (comparisonStats) queryComparisonStats else "") +
        (if (playersOpponentsStats) queryPlayersOpponentsStats else "") +
        """
          |  }
          |}
          |""".stripMargin +
        (if (comparisonStats) fragComparisonStats else "") +
        (if (contractStats) fragContractStats else "") +
        (if (playerStats || playerDoubledStats) fragPlayerStats else "")
    makeQuery(query)
  }

  def duplicateStats(): AjaxResult[Either[String, StatResult]] =
    getDuplicateStats(true, true, true, true)

  def makeQuery(query: String): AjaxResult[Either[String, StatResult]] = {

    val vars = None
    val operation = None

    GraphQLClient
      .request(query, vars, operation)
      .map { resp =>
        resp.data match {
          case Some(d: JsObject) =>
            Json.fromJson[StatResult](d) match {
              case JsSuccess(t, path) =>
                Right(t)
              case err: JsError =>
                logger.warning(
                  s"Error processing return data from duplicate stats: ${JsError.toJson(err)}"
                )
                Left("Error processing returned data")
            }
          case _ =>
            logger.warning(s"Error on Imports list: ${resp}")
            Left("Internal error")
        }
      }
      .recover {
        case x: Exception =>
          logger.warning(s"Error on Imports list", x)
          Left("Internal error")
      }

  }

  def queryPlayerStats(
      playerPlacesStats: Boolean = false
  ): Result[PlayerPlaces] = {
    RestClientDuplicatePlayerPlaces.get("")
  }

}
