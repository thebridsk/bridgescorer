package com.example.service.graphql

import sangria.schema._
import com.example.backend.BridgeService
import com.example.data.MatchDuplicate
import com.example.data.MatchChicago
import com.example.data.MatchRubber

object Data {

  class ImportBridgeService(
                             val id: String
                           )

  class ImportBridgeServiceRepo( val bridgeService: BridgeService ) {


  }
}
