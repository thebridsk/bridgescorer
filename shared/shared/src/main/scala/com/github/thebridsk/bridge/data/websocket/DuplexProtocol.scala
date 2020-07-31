package com.github.thebridsk.bridge.data.websocket

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema
import com.github.thebridsk.bridge.data.SystemTime.Timestamp

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
  case class Send(data: ToServerMessage) extends DuplexMessage

  /**
    * For sending data to the server, expecting a response or ack
    * @param data the data
    * @param seq a sequence number
    * @param ack true - just an ack is requested, false - a response is requested
    */
  case class Request(data: ToServerMessage, seq: Int, ack: Boolean)
      extends DuplexMessage

  /**
    * For a response from the server to a request.  This also serves as the ack.
    * @param data the data
    * @param seq the sequence number from the request
    */
  case class Response(data: ToBrowserMessage, seq: Int) extends DuplexMessage

  /**
    * For a response from the server to a request.  This also serves as the ack.
    * @param data the error message
    * @param seq the sequence number from the request
    */
  case class ErrorResponse(data: String, seq: Int) extends DuplexMessage

  /**
    * For a unsolicited data from the server.
    * @param data the data
    */
  case class Unsolicited(data: ToBrowserMessage) extends DuplexMessage

  /**
    * Complete the stream
    */
  case class Complete(reason: String = "") extends DuplexMessage

  /**
    * The stream failed
    */
  case class Fail(ex: Exception) extends DuplexMessage

  /**
    * For sending log entries to the server
    */
  @Schema(
    name = "LogEntry",
    title = "LogEntry - For a log message from the client.",
    description = "For a log message from the client."
  )
  case class LogEntryV2(
      @Schema(description = "The source position", required = true)
      position: String,
      @Schema(description = "The logger name", required = true)
      logger: String,
      @Schema(
        description = "The timestamp, in milleseconds since 1/1/1970",
        required = true
      )
      timestamp: Timestamp,
      @Schema(description = "The trace level", required = true)
      level: String,
      @Schema(description = "The URL of the page", required = true)
      url: String,
      @Schema(description = "The message", required = true)
      message: String,
      @Schema(description = "The cause", required = true)
      cause: String,
      @ArraySchema(
        minItems = 0,
        schema = new Schema(
          description = "An argument to the message.",
          required = true,
          `type` = "string"
        ),
        arraySchema = new Schema(
          description = "The arguments to formatting the message."
        )
      )
      args: List[String],
      @Schema(description = "A client Id", required = false)
      clientid: Option[String] = None
  ) extends DuplexMessage

  /**
    * For sending log entries to the server
    */
  case class LogEntryS(json: String) extends DuplexMessage

  import com.github.thebridsk.bridge.data.rest.JsonSupport._
  import com.github.thebridsk.bridge.data.rest.DuplexProtocolJsonSupport._

  def toString(msg: DuplexMessage) = msg.toJson

  def fromString(str: String): DuplexMessage = str.parseJson[DuplexMessage]

}
