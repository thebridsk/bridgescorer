package com.example.data

import io.swagger.annotations._
import scala.annotation.meta._

/**
 * @author werewolf
 */
@ApiModel(description = "Server version information")
case class ServerVersion(
    @(ApiModelProperty @field)(value="The name of the entity", required=true)
    name: String,
    @(ApiModelProperty @field)(value="The version of the entity", required=true)
    version: String,
    @(ApiModelProperty @field)(value="The build date of the entity", required=true)
    buildDate: String
    )
