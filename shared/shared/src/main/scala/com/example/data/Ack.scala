package com.example.data

import scala.annotation.meta._
import io.swagger.v3.oas.annotations.media.Schema

/**
  * @author werewolf
  */
@Schema(
  title = "Ack - Acknowledgement of a message over a websocket.",
  description =
    "An ack of a message sent to the server, sent over a websocket interface."
)
case class Ack(
    @Schema(
      description = "The id of the message sent to the server",
      required = true
    )
    id: String
)
