package com.github.thebridsk.bridge.data

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  title = "RestMessage - Structure returned for all REST API errors",
  description = "Structure returned for all REST API errors"
)
case class RestMessage(
    @Schema(
      description = "A message indicating what the error was",
      required = true
    )
    msg: String
)
