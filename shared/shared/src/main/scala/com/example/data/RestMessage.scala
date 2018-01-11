package com.example.data

import io.swagger.annotations._
import scala.annotation.meta._

@ApiModel(description = "Structure returned for all REST API errors")
case class RestMessage( 
    @(ApiModelProperty @field)(value="A message indicating what the error was")
    msg: String 
)