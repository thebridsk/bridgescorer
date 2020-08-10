package com.github.thebridsk.bridge.data

import com.github.thebridsk.bridge.data.SystemTime.Timestamp
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  name = "ImportStoreData",
  title = "ImportStoreData - A store for importing matches",
  description = "An import store"
)
case class ImportStoreData(
    @Schema(description = "The ID", required = true)
    id: String,
    @Schema(
      description =
        "The last modified time, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    date: Timestamp,
    @Schema(
      description = "The number of duplicate matches in store",
      required = true
    )
    duplicatesCount: Int,
    @Schema(
      description = "The number of duplicate result matches in store",
      required = true
    )
    duplicateresultsCount: Int,
    @Schema(
      description = "The number of chicago matches in store",
      required = true
    )
    chicagosCount: Int,
    @Schema(
      description = "The number of rubber matches in store",
      required = true
    )
    rubbersCount: Int
)
