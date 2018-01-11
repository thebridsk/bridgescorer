package com.example.data

import io.swagger.annotations._
import scala.annotation.meta._

/**
 * @author werewolf
 */
@ApiModel(description = "The response to GET /v1/rest/serverurl")
case class ServerURL(
    @(ApiModelProperty @field)(value="The server URLs", required=true)
    serverUrl: List[String]
    )
