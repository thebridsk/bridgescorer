package com.github.thebridsk.bridge.clientcommon.rest2

import com.github.thebridsk.bridge.data.LoggerConfig
import com.github.thebridsk.bridge.data.ServerURL
import com.github.thebridsk.bridge.data.ServerVersion

import com.github.thebridsk.bridge.data.rest.JsonSupport._
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.LogEntryV2
import play.api.libs.json.KeyWrites

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

  implicit class BooleanStream( private val b: Boolean ) extends AnyVal {
    def option[T]( f: =>T ): Option[T] = if (b) Some(f) else None
  }

  implicit val stringKeyWrites: KeyWrites[String] = new KeyWrites[String] {
    def writeKey(key: String): String = key
  }

}

import Implicits._
import scala.concurrent.ExecutionContext.Implicits.global

object RestClientLogEntryV2 extends RestClient[LogEntryV2,String]("/v1/logger")
object RestClientLoggerConfig extends RestClient[LoggerConfig,String]("/v1/rest/loggerConfig")
object RestClientServerURL extends RestClient[ServerURL,String]("/v1/rest/serverurls")
object RestClientServerVersion extends RestClient[ServerVersion,String]("/v1/rest/serverversion")
