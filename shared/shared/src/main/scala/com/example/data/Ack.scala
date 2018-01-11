package com.example.data

import io.swagger.annotations._
import scala.annotation.meta._

/**
 * @author werewolf
 */
@ApiModel(description = "An ack of a message sent to the server, sent over a websocket interface.")
case class Ack(
    @(ApiModelProperty @field)(value="The id of the message sent to the server", required=true)
    id: String )
