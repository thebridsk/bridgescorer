package com.github.thebridsk.bridge.data

import scala.annotation.meta._
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

/**
  * @author werewolf
  */
@Schema(
  title = "ServerURL - The list of URLs for the server",
  description =
    "The list of URLs that can be used to connect to the server.  The response to GET /v1/rest/serverurl."
)
case class ServerURL(
    @ArraySchema(
      minItems = 0,
      uniqueItems = true,
      schema = new Schema(
        description = "A server URL",
        required = true,
        `type` = "string"
      ),
      arraySchema = new Schema(
        description =
          "All the server URLs that can be used to connect to the server.",
        required = true
      )
    )
    serverUrl: List[String]
)
