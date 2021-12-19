package com.github.thebridsk.bridge.data

import SystemTime.Timestamp

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A table for a duplicate match")
case class Table(
    @Schema(description = "The ID of the table", required = true)
    id: Table.Id,
    @Schema(description = "The name of the table", required = true)
    name: String,
    @Schema(
      description =
        "When the duplicate hand was created, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    created: Timestamp,
    @Schema(
      description =
        "When the duplicate hand was last updated, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    updated: Timestamp
) {

  def equalsIgnoreModifyTime(other: Table): Boolean =
    this == other.copy(created = created, updated = updated)

  def setId(newId: Table.Id, forCreate: Boolean): Table = {
    val time = SystemTime.currentTimeMillis()
    copy(
      id = newId,
      created = if (forCreate) time; else created,
      updated = time
    )
  }

}

trait IdTable

object Table extends HasId[IdTable]("") {
  def create(id: Table.Id, name: String) = new Table(id, name, 0, 0)

}
