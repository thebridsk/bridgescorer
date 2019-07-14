package com.github.thebridsk.bridge.rest2

import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.LoggerConfig
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.LoggerConfig
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.ServerURL
import com.github.thebridsk.bridge.data.ServerVersion
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.data.RubberHand
import scala.concurrent.duration.Duration
import com.github.thebridsk.bridge.data.RestMessage

import com.github.thebridsk.bridge.data.rest.JsonSupport._
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.LogEntryV2
import com.github.thebridsk.bridge.data.BoardSetsAndMovementsV1
import com.github.thebridsk.bridge.data.BoardSetsAndMovements
import com.github.thebridsk.bridge.data.MatchDuplicateResult

/**
 * @author werewolf
 */

//private object BridgeRestClientImplicitsPrickle {
//  import prickle._
//
//  implicit object ConvertHand extends ResourceConverter[Hand] {
//
//    def toArray( s: String ) = Unpickle[Seq[Hand]].fromString(s).get.toArray
//    def toR( s: String ) = Unpickle[Hand].fromString(s).get
//
//    def toString( r: Hand ) = Pickle.intoString(r)
//  }
//
//  implicit object ConvertChicago extends ResourceConverter[MatchChicago] {
//
//    def toArray( s: String ) = Unpickle[Seq[MatchChicago]].fromString(s).get.toArray
//    def toR( s: String ) = Unpickle[MatchChicago].fromString(s).get
//
//    def toString( r: MatchChicago ) = Pickle.intoString(r)
//  }
//
//}

object Implicits {

  implicit class BooleanStream( val b: Boolean ) extends AnyVal {
    def option[T]( f: =>T ): Option[T] = if (b) Some(f) else None
  }

}

import Implicits._
import com.github.thebridsk.bridge.data.duplicate.suggestion.DuplicateSuggestions
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.data.Round

object RestClientLogEntryV2 extends RestClient[LogEntryV2]("/v1/logger")

object RestClientMovement extends RestClient[Movement]("/v1/rest/movements")
object RestClientBoardSet extends RestClient[BoardSet]("/v1/rest/boardsets")
object RestClientBoardSetsAndMovements extends RestClient[BoardSetsAndMovements]("/v1/rest/boardsetsandmovements")

class RestClientDuplicateBoardHand( parent: RestClientDuplicateBoard, instance: String ) extends RestClient[DuplicateHand]("hands", Some(parent), Some(instance) )
class RestClientDuplicateBoard( parent: RestClient[MatchDuplicate], instance: String ) extends RestClient[Board]("boards", Some(parent), Some(instance) ) {
  def handResource( boardid: String ) = new RestClientDuplicateBoardHand( this, boardid )
}
class RestClientDuplicateTeam( parent: RestClient[MatchDuplicate], instance: String ) extends RestClient[Team]("teams", Some(parent), Some(instance) )

class RestClientChicagoRoundHand( parent: RestClientChicagoRound, instance: String ) extends RestClient[Hand]("hands", Some(parent), Some(instance) )
class RestClientChicagoRound( parent: RestClient[MatchChicago], instance: String ) extends RestClient[Round]("boards", Some(parent), Some(instance) ) {
  def handResource( boardid: String ) = new RestClientChicagoRoundHand( this, boardid )
}

class RestClientRubberHand( parent: RestClient[MatchRubber], instance: String ) extends RestClient[RubberHand]("hands", Some(parent), Some(instance) )

object RestClientNames extends RestClient[String]("/v1/rest/names")
object RestClientChicago extends RestClient[MatchChicago]("/v1/rest/chicagos") {
  def roundResource( rubid: String ) = new RestClientChicagoRound(this,rubid)
}
object RestClientRubber extends RestClient[MatchRubber]("/v1/rest/rubbers") {
  def handResource( rubid: String ) = new RestClientRubberHand(this,rubid)
}
object RestClientDuplicateResult extends RestClient[MatchDuplicateResult]("/v1/rest/duplicateresults") {

  def createDuplicateResult( hand: MatchDuplicateResult,
                             default: Boolean = true,
                             boards: Option[String] = None,
                             movement: Option[String] = None,
                             test: Boolean = false,
                             timeout: Duration = AjaxResult.defaultTimeout ): RestResult[MatchDuplicateResult] = {
    val query = Map[String,String]() ++
                  test.option( "test"->"true" ) ++
                  default.option( "default"->"true") ++
                  boards.map(b=>"boards"->b) ++
                  movement.map(m=>"movements"->m)
    create(hand, query=query.toMap, timeout=timeout)
  }
}
object RestClientDuplicate extends RestClient[MatchDuplicate]("/v1/rest/duplicates") {
  def boardResource( dupid: String ) = new RestClientDuplicateBoard(this, dupid)
  def teamResource( dupid: String ) = new RestClientDuplicateTeam(this, dupid)

  def createMatchDuplicate( hand: MatchDuplicate,
                            default: Boolean = true,
                            boards: Option[String] = None,
                            movement: Option[String] = None,
                            test: Boolean = false,
                            timeout: Duration = AjaxResult.defaultTimeout ): RestResult[MatchDuplicate] = {
    val query = Map[String,String]() ++
                  test.option( "test"->"true" ) ++
                  default.option( "default"->"true") ++
                  boards.map(b=>"boards"->b) ++
                  movement.map(m=>"movements"->m)
    create(hand, query=query.toMap, timeout=timeout)
  }
}
object RestClientDuplicateSummary extends RestClient[DuplicateSummary]("/v1/rest/duplicatesummaries")
object RestClientLoggerConfig extends RestClient[LoggerConfig]("/v1/rest/loggerConfig")
object RestClientServerURL extends RestClient[ServerURL]("/v1/rest/serverurls")
object RestClientServerVersion extends RestClient[ServerVersion]("/v1/rest/serverversion")

object RestClientDuplicateSuggestions extends RestClient[DuplicateSuggestions]("/v1/rest/suggestions")

object RestClientTestBoardsetsAndMovements extends RestClient[BoardSetsAndMovements]("/public/test/boardsetsAndMovements.json")
