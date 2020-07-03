package com.github.thebridsk.bridge.data

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema.AccessMode

/**
  * All BoardSets and Movements
  */
@Schema(
  name = "BoardSetsAndMovements",
  title = "BoardSetsAndMovements - All the boardsets and movements.",
  description = "All board sets and movements that the server knows about.",
  accessMode = AccessMode.READ_ONLY
)
case class BoardSetsAndMovementsV1(
    @ArraySchema(
      minItems = 0,
      uniqueItems = true,
      schema = new Schema(implementation = classOf[BoardSetV1]),
      arraySchema = new Schema(
        description = "All the boardsets known to the server.",
        required = true
      )
    )
    boardsets: List[BoardSet],
    @ArraySchema(
      minItems = 0,
      uniqueItems = true,
      schema = new Schema(implementation = classOf[MovementV1]),
      arraySchema = new Schema(
        description = "All the movements known to the server.",
        required = true
      )
    )
    movements: List[Movement]
) {

  def convertToCurrentVersion = this

}
