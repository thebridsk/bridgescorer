package com.github.thebridsk.bridge.data

import io.swagger.v3.oas.annotations.media.Schema

/**
  * @author werewolf
  */
@Schema(
  title = "ServerVersion - Server version information",
  description = "Server version information"
)
case class ServerVersion(
    @Schema(description = "The name of the entity", required = true)
    name: String,
    @Schema(description = "The version of the entity", required = true)
    version: String,
    @Schema(description = "The build date of the entity", required = true)
    buildDate: String
)
