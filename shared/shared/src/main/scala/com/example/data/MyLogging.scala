package com.example.data

import scala.annotation.meta._
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema

/**
 * @author werewolf
 */
@Schema(description = "Logger configuration for the browser")
case class LoggerConfig(
    @ArraySchema(
        minItems=0,
        uniqueItems=true,
        schema=new Schema(
          description="A list of logging level for a logger, syntax: &lt;loggername&gt;=&lt;level}&gt;",
          required=true,
          example="[root]=ALL",
          `type`="string"
        )
    )
    loggers: List[String],
    @ArraySchema(
        minItems=0,
        uniqueItems=true,
        schema=new Schema(
          description="A list of logging level for an appender, syntax: &lt;appendername&gt;=&lt;level}&gt;",
          required=true,
          example="[root]=ALL",
          `type`="string"
        )
    )
    appenders: List[String],
    @Schema(description="A client Id", required=false, `type`="string")
    clientid: Option[String] = None,
    @Schema(description="Use Rest when updating REST objects", required=false, `type`="boolean")
    useRestToServer: Option[Boolean] = None,
    @Schema(description="Use SSE to receive updates from server", required=false, `type`="boolean")
    useSSEFromServer: Option[Boolean] = None
    )
