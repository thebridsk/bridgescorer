package com.example.data

import io.swagger.annotations._
import scala.annotation.meta._

/**
 * @author werewolf
 */
@ApiModel(description = "Logger configuration for the browser")
case class LoggerConfig(
    @(ApiModelProperty @field)(value="A list of logging level for a logger, syntax: &lt;loggername&gt;=&lt;level}&gt;", required=true, example="[root]=ALL")
    loggers: List[String], 
    @(ApiModelProperty @field)(value="A list of logging level for an appender, syntax: &lt;appendername&gt;=&lt;level}&gt;", required=true, example="[root]=INFO")
    appenders: List[String] 
    )
