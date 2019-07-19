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
import scala.concurrent.ExecutionContext.Implicits.global

object RestClientLogEntryV2 extends RestClient[LogEntryV2]("/v1/logger")
object RestClientLoggerConfig extends RestClient[LoggerConfig]("/v1/rest/loggerConfig")
object RestClientServerURL extends RestClient[ServerURL]("/v1/rest/serverurls")
object RestClientServerVersion extends RestClient[ServerVersion]("/v1/rest/serverversion")
