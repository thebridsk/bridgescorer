package com.github.thebridsk.bridge.server.service.graphql

import com.github.thebridsk.bridge.server.backend.BridgeService

object Data {

  class ImportBridgeService(
      val id: String
  )

  class ImportBridgeServiceRepo(val bridgeService: BridgeService) {}
}
