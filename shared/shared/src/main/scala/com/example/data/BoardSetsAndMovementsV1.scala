package com.example.data

import io.swagger.annotations.ApiModel

/**
 * All BoardSets and Movements
 */
@ApiModel(value="BoardSetsAndMovements", description = "All board sets and movements")
case class BoardSetsAndMovementsV1( boardsets: List[BoardSet], movements: List[Movement] ) {

  def convertToCurrentVersion() = this

}
