package com.example.data

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema

/**
 * All BoardSets and Movements
 */
@Schema(name="BoardSetsAndMovements", description = "All board sets and movements")
case class BoardSetsAndMovementsV1(
    @ArraySchema(
        minItems = 0,
        uniqueItems = true,
        schema = new Schema( implementation=classOf[BoardSetV1] )
    )
    boardsets: List[BoardSet],
    @ArraySchema(
        minItems = 0,
        uniqueItems = true,
        schema = new Schema( implementation=classOf[MovementV1] )
    )
    movements: List[Movement]
) {

  def convertToCurrentVersion() = this

}
