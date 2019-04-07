package com.example.data

import scala.annotation.meta._
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @author werewolf
 */
@Schema(description = "The response to GET /v1/rest/serverurl")
case class ServerURL(
    @ArraySchema(
        minItems=0,
        uniqueItems=true,
        schema=new Schema(
          description="The server URL",
          required=true,
          `type`="string"
        )
    )
    serverUrl: List[String]
)
