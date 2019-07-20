package com.github.thebridsk.bridge.server.service.graphql

import sangria.schema._
import com.github.thebridsk.bridge.server.backend.BridgeService
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.MatchRubber

object Data {

  class ImportBridgeService(
      val id: String
  )

  class ImportBridgeServiceRepo(val bridgeService: BridgeService) {}
}
