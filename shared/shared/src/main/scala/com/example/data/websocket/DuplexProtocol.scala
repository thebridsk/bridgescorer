package com.example.data.websocket

import com.example.data.MatchDuplicate
import com.example.data.Id
import com.example.data.Board
import com.example.data.DuplicateHand
import com.example.data.Team
import play.api.libs.json._
import com.example.data.rest.JsonSupport._
import io.swagger.annotations._
import scala.annotation.meta._

object DuplexProtocol {

//  import Protocol._
  import Protocol.ToBrowserMessage
  import Protocol.ToServerMessage

  // When adding another subclass of Message,
  // be sure to add the serialization and deserialization
  // code in the vals message2Writer and message2Reader
  sealed trait DuplexMessage

  /**
   * For sending data to the server, not expecting a response
   * @param data the data
   */
  case class Send( data: ToServerMessage ) extends DuplexMessage
  /**
   * For sending data to the server, expecting a response or ack
   * @param data the data
   * @param seq a sequence number
   * @param ack true - just an ack is requested, false - a response is requested
   */
  case class Request( data: ToServerMessage, seq: Int, ack: Boolean ) extends DuplexMessage
  /**
   * For a response from the server to a request.  This also serves as the ack.
   * @param data the data
   * @param seq the sequence number from the request
   */
  case class Response( data: ToBrowserMessage, seq: Int ) extends DuplexMessage
  /**
   * For a response from the server to a request.  This also serves as the ack.
   * @param data the error message
   * @param seq the sequence number from the request
   */
  case class ErrorResponse( data: String, seq: Int ) extends DuplexMessage

  /**
   * For a unsolicited data from the server.
   * @param data the data
   */
  case class Unsolicited( data: ToBrowserMessage ) extends DuplexMessage

  /**
   * For sending log entries to the server
   */
  @ApiModel(value="LogEntry", description = "For a log message from the client.")
  case class LogEntryV2(
      @(ApiModelProperty @field)(value="The source position", required=true)
      position: String,
      @(ApiModelProperty @field)(value="The logger name", required=true)
      logger: String,
      @(ApiModelProperty @field)(value="The timestamp, in milleseconds since 1/1/1970", required=true)
      timestamp: Double,
      @(ApiModelProperty @field)(value="The trace level", required=true)
      level: String,
      @(ApiModelProperty @field)(value="The URL of the page", required=true)
      url: String,
      @(ApiModelProperty @field)(value="The message", required=true)
      message: String,
      @(ApiModelProperty @field)(value="The cause", required=true)
      cause: String,
      @(ApiModelProperty @field)(value="The args", required=true)
      args: List[String],
      @(ApiModelProperty @field)(value="A client Id", required=false)
      clientid: Option[String] = None
  ) extends DuplexMessage

  /**
   * For sending log entries to the server
   */
  case class LogEntryS( json: String) extends DuplexMessage

  import com.example.data.rest.JsonSupport._
  import com.example.data.rest.DuplexProtocolJsonSupport._

  def toString(msg: DuplexMessage) = msg.toJson

  def fromString(str: String) = str.parseJson[DuplexMessage]

}
