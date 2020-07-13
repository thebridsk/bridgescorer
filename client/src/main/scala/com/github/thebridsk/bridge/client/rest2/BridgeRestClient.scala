package com.github.thebridsk.bridge.clientcommon.rest2

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
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerPlaces
import com.github.thebridsk.bridge.data.DuplicatePicture
import org.scalactic.source.Position
import org.scalajs.dom.raw.File
import org.scalajs.dom.raw.FormData

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


import com.github.thebridsk.bridge.clientcommon.rest2.Implicits._
import com.github.thebridsk.bridge.data.duplicate.suggestion.DuplicateSuggestions
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.data.Round
import com.github.thebridsk.bridge.data.Id

object RestClientLogEntryV2 extends RestClient[LogEntryV2,String]("/v1/logger")

object RestClientMovement extends RestClient[Movement,String]("/v1/rest/movements")
object RestClientBoardSet extends RestClient[BoardSet,String]("/v1/rest/boardsets")
object RestClientBoardSetsAndMovements extends RestClient[BoardSetsAndMovements,String]("/v1/rest/boardsetsandmovements")

class RestClientDuplicateBoardHand( parent: RestClientDuplicateBoard, instance: String ) extends RestClient[DuplicateHand,Team.Id]("hands", Some(parent), Some(instance) )
class RestClientDuplicateBoard( parent: RestClient[MatchDuplicate,_], instance: String ) extends RestClient[Board,String]("boards", Some(parent), Some(instance) ) {
  def handResource( boardid: Id.DuplicateBoard ) = new RestClientDuplicateBoardHand( this, boardid )
}
class RestClientDuplicateBoardHandPicture( parent: RestClientDuplicateBoardPicture, instance: Id.DuplicateBoard ) extends RestClient[DuplicatePicture,Team.Id]("hands", Some(parent), Some(instance) ) {
  /**
   * @param id the north player that played the pictured hand
   * @param file the File object.  This must have the filename set, only the extension is important.
   * @param query
   * @param headers
   * @param timeout
   */
  def putPicture(
      id: Team.Id,
      file: File,
      query: Map[String, String] = Map.empty,
      headers: Map[String, String] = Map.empty,
      timeout: Duration = AjaxResult.defaultTimeout
  ): RestResult[Unit] = {
    val formData = new FormData
    formData.append("picture", file)

    AjaxResult.put(getURL(id,query), data=formData, timeout=timeout, headers=headers).recordFailure()
  }
}
class RestClientDuplicateBoardPicture( parent: RestClient[MatchDuplicate,_], instance: Id.MatchDuplicate ) extends RestClient[DuplicatePicture,Id.MatchDuplicate]("pictures", Some(parent), Some(instance) ) {
  def handResource( boardid: Id.DuplicateBoard ) = new RestClientDuplicateBoardHandPicture( this, boardid )
}
class RestClientDuplicateTeam( parent: RestClient[MatchDuplicate,_], instance: String ) extends RestClient[Team,Team.Id]("teams", Some(parent), Some(instance) )

class RestClientChicagoRoundHand( parent: RestClientChicagoRound, instance: String ) extends RestClient[Hand,String]("hands", Some(parent), Some(instance) )
class RestClientChicagoRound( parent: RestClient[MatchChicago,_], instance: String ) extends RestClient[Round,String]("boards", Some(parent), Some(instance) ) {
  def handResource( boardid: String ) = new RestClientChicagoRoundHand( this, boardid )
}

class RestClientRubberHand( parent: RestClient[MatchRubber,_], instance: String ) extends RestClient[RubberHand,String]("hands", Some(parent), Some(instance) )

object RestClientNames extends RestClient[String,String]("/v1/rest/names")
object RestClientChicago extends RestClient[MatchChicago,String]("/v1/rest/chicagos") {
  def roundResource( rubid: String ) = new RestClientChicagoRound(this,rubid)
}
object RestClientRubber extends RestClient[MatchRubber,String]("/v1/rest/rubbers") {
  def handResource( rubid: String ) = new RestClientRubberHand(this,rubid)
}
object RestClientDuplicateResult extends RestClient[MatchDuplicateResult,String]("/v1/rest/duplicateresults") {

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
object RestClientDuplicate extends RestClient[MatchDuplicate,String]("/v1/rest/duplicates") {
  def boardResource( dupid: String ) = new RestClientDuplicateBoard(this, dupid)
  def teamResource( dupid: String ) = new RestClientDuplicateTeam(this, dupid)
  def pictureResource( dupid: String ) = new RestClientDuplicateBoardPicture(this,dupid)

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
object RestClientDuplicateSummary extends RestClient[DuplicateSummary,String]("/v1/rest/duplicatesummaries")
object RestClientLoggerConfig extends RestClient[LoggerConfig,String]("/v1/rest/loggerConfig")
object RestClientServerURL extends RestClient[ServerURL,String]("/v1/rest/serverurls")
object RestClientServerVersion extends RestClient[ServerVersion,String]("/v1/rest/serverversion")

object RestClientDuplicateSuggestions extends RestClient[DuplicateSuggestions,String]("/v1/rest/suggestions")

object RestClientTestBoardsetsAndMovements extends RestClient[BoardSetsAndMovements,String]("/public/test/boardsetsAndMovements.json")

object RestClientDuplicatePlayerPlaces extends RestClient[PlayerPlaces,String]("/v1/rest/duplicateplaces")
