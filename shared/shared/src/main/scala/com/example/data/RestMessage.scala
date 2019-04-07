package com.example.data

import scala.annotation.meta._
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Structure returned for all REST API errors")
case class RestMessage(
    @Schema(description="A message indicating what the error was")
    msg: String
)
